package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

// Importaciones necesarias para la aplicación Spark
import java.util.HashMap; // Utilidad para serializar/deserializar objetos Java a/desde JSON.
import java.util.List;
import java.util.Map; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).

import com.is1.proyecto.models.*;
import org.javalite.activejdbc.Base; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.
import org.mindrot.jbcrypt.BCrypt; // Utilidad para hashear y verificar contraseñas de forma segura.

import com.fasterxml.jackson.databind.ObjectMapper; // Representa un modelo de datos y el nombre de la vista a renderizar.
import com.is1.proyecto.config.DBConfigSingleton; // Motor de plantillas Mustache para Spark.

import spark.ModelAndView; // Modelo de ActiveJDBC que representa la tabla 'users'.
import static spark.Spark.after; // Modelo de ActiveJDBC que representa la tabla 'profesor'.
import static spark.Spark.before; // Modelo de ActiveJDBC que representa la tabla 'persona'.
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.post;
import spark.template.mustache.MustacheTemplateEngine;



/**
 * Clase principal de la aplicación Spark.
 * Configura las rutas, filtros y el inicio del servidor web.
 */
public class App {

    // Instancia estática y final de ObjectMapper para la serialización/deserialización JSON.
    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Método principal que se ejecuta al iniciar la aplicación.
     * Aquí se configuran todas las rutas y filtros de Spark.
     */
    public static void main(String[] args) {
        port(8080); // Configura el puerto en el que la aplicación Spark escuchará las peticiones (por defecto es 4567).

        // Obtener la instancia única del singleton de configuración de la base de datos.
        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();

        // --- Filtro 'before' para gestionar la conexión a la base de datos ---
        // Este filtro se ejecuta antes de cada solicitud HTTP.
        before((req, res) -> {
            try {
                // VERIFICACIÓN CLAVE: Solo abre la conexión si no hay una ya abierta en este hilo.
                if (!Base.hasConnection()) {
                    // Abre una conexión a la base de datos utilizando las credenciales del singleton.
                    Base.open(dbConfig.getDriver(), dbConfig.getDbUrl(), dbConfig.getUser(), dbConfig.getPass());
                }
                System.out.println(req.url());

            } catch (Exception e) {
                // Si ocurre un error al abrir la conexión, se registra y se detiene la solicitud
                // con un código de estado 500 (Internal Server Error) y un mensaje JSON.
                System.err.println("Error al abrir conexión con ActiveJDBC: " + e.getMessage());
                halt(500, "{\"error\": \"Error interno del servidor: Fallo al conectar a la base de datos.\"}" + e.getMessage());
            }
        });

        // --- Filtro 'after' para cerrar la conexión a la base de datos ---
        // Este filtro se ejecuta después de que cada solicitud HTTP ha sido procesada.
        after((req, res) -> {
            try {
                // VERIFICACIÓN CLAVE: Solo cierra la conexión si efectivamente hay una abierta.
                if (Base.hasConnection()) {
                    // Cierra la conexión a la base de datos para liberar recursos.
                    Base.close();
                }
            } catch (Exception e) {
                // Si ocurre un error al cerrar la conexión, se registra.
                System.err.println("Error al cerrar conexión con ActiveJDBC: " + e.getMessage());
            }
        });

        // --- Rutas GET para renderizar formularios y páginas HTML ---

        // GET: Muestra el formulario de creación de cuenta.
        // Soporta la visualización de mensajes de éxito o error pasados como query parameters.
        get("/user/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Crea un mapa para pasar datos a la plantilla.

            // Obtener y añadir mensaje de éxito de los query parameters (ej. ?message=Cuenta creada!)
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            // Obtener y añadir mensaje de error de los query parameters (ej. ?error=Campos vacíos)
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            // Renderiza la plantilla 'user_form.mustache' con los datos del modelo.
            return new ModelAndView(model, "user_form.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta para mostrar el dashboard (panel de control) del usuario.
        // Requiere que el usuario esté autenticado.
        get("/dashboard", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla del dashboard.

            // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
            String currentUsername = req.session().attribute("currentUserUsername");
            Boolean loggedIn = req.session().attribute("loggedIn");

            // 1. Verificar si el usuario ha iniciado sesión.
            // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
            // significa que el usuario no está logueado o su sesión expiró.
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
                // Redirige al login con un mensaje de error.
                res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
                return null; // Importante retornar null después de una redirección.
            }

            // 2. Si el usuario está logueado, añade el nombre de usuario al modelo para la plantilla.
            model.put("username", currentUsername);

            // 3. Renderiza la plantilla del dashboard con el nombre de usuario.
            return new ModelAndView(model, "dashboard.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta para cerrar la sesión del usuario.
        get("/logout", (req, res) -> {
            // Invalida completamente la sesión del usuario.
            // Esto elimina todos los atributos guardados en la sesión y la marca como inválida.
            // La cookie JSESSIONID en el navegador también será gestionada para invalidarse.
            req.session().invalidate();

            System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /login.");

            // Redirige al usuario a la página de login con un mensaje de éxito.
            res.redirect("/");

            return null; // Importante retornar null después de una redirección.
        });

        // GET: Muestra el formulario de inicio de sesión (login).
        // Nota: Esta ruta debería ser capaz de leer también mensajes de error/éxito de los query params
        // si se la usa como destino de redirecciones. (Tu código de /user/create ya lo hace, aplicar similar).
        get("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }
            return new ModelAndView(model, "login.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta de alias para el formulario de creación de cuenta.
        // En una aplicación real, probablemente querrías unificar con '/user/create' para evitar duplicidad.
        get("/user/new", (req, res) -> {
            return new ModelAndView(new HashMap<>(), "user_form.mustache"); // No pasa un modelo específico, solo el formulario.
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.


        // --- Rutas POST para manejar envíos de formularios y APIs ---

        // POST: Maneja el envío del formulario de creación de nueva cuenta.
        post("/user/new", (req, res) -> {
            String name = req.queryParams("name");
            String password = req.queryParams("password");

            // Validaciones básicas: campos no pueden ser nulos o vacíos.
            if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
                res.status(400); // Código de estado HTTP 400 (Bad Request).
                // Redirige al formulario de creación con un mensaje de error.
                res.redirect("/user/create?error=Nombre y contraseña son requeridos.");
                return ""; // Retorna una cadena vacía ya que la respuesta ya fue redirigida.
            }

            try {
                // Intenta crear y guardar la nueva cuenta en la base de datos.
                User ac = new User(); // Crea una nueva instancia del modelo User.
                // Hashea la contraseña de forma segura antes de guardarla.
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                ac.set("name", name); // Asigna el nombre de usuario.
                ac.set("password", hashedPassword); // Asigna la contraseña hasheada.
                ac.saveIt(); // Guarda el nuevo usuario en la tabla 'users'.

                res.status(201); // Código de estado HTTP 201 (Created) para una creación exitosa.
                // Redirige al formulario de creación con un mensaje de éxito.
                res.redirect("/user/create?message=Cuenta creada exitosamente para " + name + "!");
                return ""; // Retorna una cadena vacía.

            } catch (Exception e) {
                // Si ocurre cualquier error durante la operación de DB (ej. nombre de usuario duplicado),
                // se captura aquí y se redirige con un mensaje de error.
                System.err.println("Error al registrar la cuenta: " + e.getMessage());
                e.printStackTrace(); // Imprime el stack trace para depuración.
                res.status(500); // Código de estado HTTP 500 (Internal Server Error).
                res.redirect("/user/create?error=Error interno al crear la cuenta. Intente de nuevo.");
                return ""; // Retorna una cadena vacía.
            }
        });


        // POST: Maneja el envío del formulario de inicio de sesión.
        post("/login", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla de login o dashboard.

            String username = req.queryParams("username");
            String plainTextPassword = req.queryParams("password");

            // Validaciones básicas: campos de usuario y contraseña no pueden ser nulos o vacíos.
            if (username == null || username.isEmpty() || plainTextPassword == null || plainTextPassword.isEmpty()) {
                res.status(400); // Bad Request.
                model.put("errorMessage", "El nombre de usuario y la contraseña son requeridos.");
                return new ModelAndView(model, "login.mustache"); // Renderiza la plantilla de login con error.
            }

            // Busca la cuenta en la base de datos por el nombre de usuario.
            User ac = User.findFirst("name = ?", username);

            // Si no se encuentra ninguna cuenta con ese nombre de usuario.
            if (ac == null) {
                res.status(401); // Unauthorized.
                model.put("errorMessage", "Usuario o contraseña incorrectos."); // Mensaje genérico por seguridad.
                return new ModelAndView(model, "login.mustache"); // Renderiza la plantilla de login con error.
            }

            // Obtiene la contraseña hasheada almacenada en la base de datos.
            String storedHashedPassword = ac.getString("password");

            // Compara la contraseña en texto plano ingresada con la contraseña hasheada almacenada.
            // BCrypt.checkpw hashea la plainTextPassword con el salt de storedHashedPassword y compara.
            if (BCrypt.checkpw(plainTextPassword, storedHashedPassword)) {
                // Autenticación exitosa.
                res.status(200); // OK.

                // --- Gestión de Sesión ---
                req.session(true).attribute("currentUserUsername", username); // Guarda el nombre de usuario en la sesión.
                req.session().attribute("userId", ac.getId()); // Guarda el ID de la cuenta en la sesión (útil).
                req.session().attribute("loggedIn", true); // Establece una bandera para indicar que el usuario está logueado.

                System.out.println("DEBUG: Login exitoso para la cuenta: " + username);
                System.out.println("DEBUG: ID de Sesión: " + req.session().id());


                model.put("username", username); // Añade el nombre de usuario al modelo para el dashboard.
                // Renderiza la plantilla del dashboard tras un login exitoso.
                return new ModelAndView(model, "dashboard.mustache");
            } else {
                // Contraseña incorrecta.
                res.status(401); // Unauthorized.
                System.out.println("DEBUG: Intento de login fallido para: " + username);
                model.put("errorMessage", "Usuario o contraseña incorrectos."); // Mensaje genérico por seguridad.
                return new ModelAndView(model, "login.mustache"); // Renderiza la plantilla de login con error.
            }
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta POST.


        // POST: Endpoint para añadir usuarios (API que devuelve JSON, no HTML).
        // Advertencia: Esta ruta tiene un propósito diferente a las de formulario HTML.
        post("/add_users", (req, res) -> {
            res.type("application/json"); // Establece el tipo de contenido de la respuesta a JSON.

            // Obtiene los parámetros 'name' y 'password' de la solicitud.
            String name = req.queryParams("name");
            String password = req.queryParams("password");

            // --- Validaciones básicas ---
            if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
                res.status(400); // Bad Request.
                return objectMapper.writeValueAsString(Map.of("error", "Nombre y contraseña son requeridos."));
            }

            try {
                // --- Creación y guardado del usuario usando el modelo ActiveJDBC ---
                User newUser = new User(); // Crea una nueva instancia de tu modelo User.
                // ¡ADVERTENCIA DE SEGURIDAD CRÍTICA!
                // En una aplicación real, las contraseñas DEBEN ser hasheadas (ej. con BCrypt)
                // ANTES de guardarse en la base de datos, NUNCA en texto plano.
                // (Nota: El código original tenía la contraseña en texto plano aquí.
                // Se recomienda usar `BCrypt.hashpw(password, BCrypt.gensalt())` como en la ruta '/user/new').
                newUser.set("name", name); // Asigna el nombre al campo 'name'.
                newUser.set("password", password); // Asigna la contraseña al campo 'password'.
                newUser.saveIt(); // Guarda el nuevo usuario en la tabla 'users'.

                res.status(201); // Created.
                // Devuelve una respuesta JSON con el mensaje y el ID del nuevo usuario.
                return objectMapper.writeValueAsString(Map.of("message", "Usuario '" + name + "' registrado con éxito.", "id", newUser.getId()));

            } catch (Exception e) {
                // Si ocurre cualquier error durante la operación de DB, se captura aquí.
                System.err.println("Error al registrar usuario: " + e.getMessage());
                e.printStackTrace(); // Imprime el stack trace para depuración.
                res.status(500); // Internal Server Error.
                return objectMapper.writeValueAsString(Map.of("error", "Error interno al registrar usuario: " + e.getMessage()));
            }
        });

        get("/registrarProfesor", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Crea un mapa para pasar datos a la plantilla.
            // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
            String currentUsername = req.session().attribute("currentUserUsername");
            Boolean loggedIn = req.session().attribute("loggedIn");
            // 1. Verificar si el usuario ha iniciado sesión.
            // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
            // significa que el usuario no está logueado o su sesión expiró.
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                System.out.println("DEBUG: Acceso no autorizado a /registrarProfesor. Redirigiendo a /login.");
                // Redirige al login con un mensaje de error.
                res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
                return null; // Importante retornar null después de una redirección.
            }
            // Obtener y añadir mensaje de éxito de los query parameters (ej. ?message=Cuenta creada!)
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }
            // Obtener y añadir mensaje de error de los query parameters (ej. ?error=Campos vacíos)
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }
            // Renderiza la plantilla 'registrarProfesor.mustache' con los datos del modelo.
            return new ModelAndView(model, "registrarProfesor.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.


         // POST: Maneja el envío del formulario de creación de un profesor nuevo.
        post("/registrarProfesor/new", (req, res) -> {
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String correo = req.queryParams("correo");
            String dni = req.queryParams("dni");

            // Validaciones básicas: campos no pueden ser nulos o vacíos.
            if (nombre == null || nombre.isEmpty() || apellido == null || apellido.isEmpty() || correo == null || correo.isEmpty() || dni == null || dni.isEmpty()) {
                res.status(400); // Código de estado HTTP 400 (Bad Request).
                // Redirige al formulario de creación con un mensaje de error.
                res.redirect("/registrarProfesor?error=Nombre y apellido son requeridos.");
                return ""; // Retorna una cadena vacía ya que la respuesta ya fue redirigida.
            }

            // Validar formato de correo
            if (!correo.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                res.status(400); // Código de estado HTTP 400 (Bad Request).
                res.redirect("/registrarProfesor?error=Correo invalido.");
                return "";
            }
            // Verificar si la persona ya existe
                Persona personaExistente = Persona.findFirst("dni = ?", dni);
                if (personaExistente != null) {
                    res.redirect("/registrarProfesor?error=El DNI ya esta registrado.");
                    return "";
                }
                // Verificar si el correo del profesor ya existe
                Profesor profesorExistente = Profesor.findFirst("correo = ?", correo);
                if (profesorExistente != null) {
                    res.redirect("/registrarProfesor?error=El correo ya esta registrado.");
                    return "";
                }

            try {
                // Intenta crear y guardar el nuevo profesor en la base de datos.
                Persona per = new Persona();
                Profesor pro = new Profesor(); // Crea una nueva instancia del modelo Profesor.

                per.set("nombre", nombre); // Asigna el nombre de persona.
                per.set("apellido", apellido); // Asigna el apellido de persona.
                per.set("dni", dni); // Asigna el dni de persona.
                per.saveIt(); // Guarda la nueva persona en la tabla 'persona'.

                pro.set("dni", dni); //Asigna la fk dni de profesor.
                pro.set("correo", correo); // Asigna el correo de profesor. 
                pro.saveIt(); // Guarda el nuevo profesor en la tabla 'profesor'.

                res.status(201); // Código de estado HTTP 201 (Created) para una creación exitosa.
                // Redirige al formulario de creación con un mensaje de éxito.
                res.redirect("/registrarProfesor?message=Profesor "+ nombre + " "+ apellido + " registrado exitosamente!");
                return ""; // Retorna una cadena vacía.

            } catch (Exception e) {
                // Si ocurre cualquier error durante la operación de DB (ej. nombre de usuario duplicado),
                // se captura aquí y se redirige con un mensaje de error.
                System.err.println("Error al registrar el profesor: " + e.getMessage());
                e.printStackTrace(); // Imprime el stack trace para depuración.
                res.redirect("/registrarProfesor?error=Error interno al registrar profesor. Intente de nuevo.");
                return ""; // Retorna una cadena vacía.
            }
        });
        // POST: Endpoint para añadir profesores (API que devuelve JSON, no HTML).
        // Advertencia: Esta ruta tiene un propósito diferente a las de formulario HTML.
        post("/add_profesor", (req, res) -> {
            res.type("application/json"); // Establece el tipo de contenido de la respuesta a JSON.

            // Obtiene los parámetros 'name' y 'password' de la solicitud.
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String correo = req.queryParams("correo");
            String dni = req.queryParams("dni");

            // --- Validaciones de nombre y apellido ---
            if (nombre == null || nombre.isEmpty() || apellido == null || apellido.isEmpty()) {
                res.status(400); // Bad Request.
                return objectMapper.writeValueAsString(Map.of("error", "Nombre y apellido son requeridos."));
            }
            // --- Validacion de correo---
            if (correo == null || correo.isEmpty() || !correo.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                res.status(400); // Bad Request.
                return objectMapper.writeValueAsString(Map.of("error", "Correo invalido."));
            }

            try {
                // --- Creación y guardado del usuario usando el modelo ActiveJDBC ---
                Profesor newProfesor = new Profesor(); // Crea una nueva instancia de tu modelo User.
                
                newProfesor.set("nombre", nombre); // Asigna el nombre al campo 'nombre'.
                newProfesor.set("apellido", apellido); // Asigna la contraseña al campo 'apellido'.
                newProfesor.set("correo", correo);
                newProfesor.set("dni", dni);
                newProfesor.saveIt(); // Guarda el nuevo usuario en la tabla 'profesor'.

                res.status(201); // Created.
                // Devuelve una respuesta JSON con el mensaje y el ID del nuevo usuario.
                return objectMapper.writeValueAsString(Map.of("message", "Profesor '" + nombre + "' registrado con éxito."));

            } catch (Exception e) {
                // Si ocurre cualquier error durante la operación de DB, se captura aquí.
                System.err.println("Error al registrar profesor: " + e.getMessage());
                e.printStackTrace(); // Imprime el stack trace para depuración.
                res.status(500); // Internal Server Error.
                return objectMapper.writeValueAsString(Map.of("error", "Error interno al registrar profesor: " + e.getMessage()));
            }
        });

        get("/registrarEstudiante", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Crea un mapa para pasar datos a la plantilla.
            // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
            String currentUsername = req.session().attribute("currentUserUsername");
            Boolean loggedIn = req.session().attribute("loggedIn");
            // 1. Verificar si el usuario ha iniciado sesión.
            // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
            // significa que el usuario no está logueado o su sesión expiró.
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                System.out.println("DEBUG: Acceso no autorizado a /registrarEstudiante. Redirigiendo a /login.");
                // Redirige al login con un mensaje de error.
                res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
                return null; // Importante retornar null después de una redirección.
            }
            // Obtener y añadir mensaje de éxito de los query parameters (ej. ?message=Cuenta creada!)
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }
            // Obtener y añadir mensaje de error de los query parameters (ej. ?error=Campos vacíos)
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }
            // Renderiza la plantilla 'registrarEstudiante.mustache' con los datos del modelo.
            return new ModelAndView(model, "registrarEstudiante.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.


        // POST: Maneja el envío del formulario de creación de un estudiante nuevo.
        post("/registrarEstudiante/new", (req, res) -> {
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dni = req.queryParams("dni");

            // Validaciones básicas: campos no pueden ser nulos o vacíos.
            if (nombre == null || nombre.isEmpty() || apellido == null || apellido.isEmpty() || dni == null || dni.isEmpty()) {
                res.status(400); // Código de estado HTTP 400 (Bad Request).
                // Redirige al formulario de creación con un mensaje de error.
                res.redirect("/registrarEstudiante?error=Nombre y apellido son requeridos.");
                return ""; // Retorna una cadena vacía ya que la respuesta ya fue redirigida.
            }
            // Verificar si la persona ya existe
            Persona personaExistente = Persona.findFirst("dni = ?", dni);
            if (personaExistente != null) {
                res.redirect("/registrarEstudiante?error=El DNI ya esta registrado.");
                return "";
            }

            try {
                // Intenta crear y guardar el nuevo estudiante en la base de datos.
                Persona per = new Persona();
                Estudiante est = new Estudiante(); // Crea una nueva instancia del modelo Profesor.

                per.set("nombre", nombre); // Asigna el nombre de persona.
                per.set("apellido", apellido); // Asigna el apellido de persona.
                per.set("dni", dni); // Asigna el dni de persona.
                per.saveIt(); // Guarda la nueva persona en la tabla 'persona'.

                est.set("dni", dni); //Asigna la fk dni de profesor.
                est.saveIt(); // Guarda el nuevo profesor en la tabla 'profesor'.

                res.status(201); // Código de estado HTTP 201 (Created) para una creación exitosa.
                // Redirige al formulario de creación con un mensaje de éxito.
                res.redirect("/registrarEstudiante?message=Estudiante "+ nombre + " "+ apellido + " registrado exitosamente!");
                return ""; // Retorna una cadena vacía.

            } catch (Exception e) {
                // Si ocurre cualquier error durante la operación de DB (ej. nombre de usuario duplicado),
                // se captura aquí y se redirige con un mensaje de error.
                System.err.println("Error al registrar el estudiante: " + e.getMessage());
                e.printStackTrace(); // Imprime el stack trace para depuración.
                res.redirect("/registrarEstudiante?error=Error interno al registrar estudiante. Intente de nuevo.");
                return ""; // Retorna una cadena vacía.
            }
        });
        // POST: Endpoint para añadir profesores (API que devuelve JSON, no HTML).
        // Advertencia: Esta ruta tiene un propósito diferente a las de formulario HTML.
        post("/add_profesor", (req, res) -> {
            res.type("application/json"); // Establece el tipo de contenido de la respuesta a JSON.

            // Obtiene los parámetros 'name' y 'password' de la solicitud.
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dni = req.queryParams("dni");

            // --- Validaciones de nombre y apellido ---
            if (nombre == null || nombre.isEmpty() || apellido == null || apellido.isEmpty()) {
                res.status(400); // Bad Request.
                return objectMapper.writeValueAsString(Map.of("error", "Nombre y apellido son requeridos."));
            }

            try {
                // --- Creación y guardado del usuario usando el modelo ActiveJDBC ---
                Estudiante newEstudiante = new Estudiante(); // Crea una nueva instancia de tu modelo User.

                newEstudiante.set("nombre", nombre); // Asigna el nombre al campo 'nombre'.
                newEstudiante.set("apellido", apellido); // Asigna el apellido al campo 'apellido'.
                newEstudiante.set("dni", dni);
                newEstudiante.saveIt(); // Guarda el nuevo usuario en la tabla 'estudiante'.

                res.status(201); // Created.
                // Devuelve una respuesta JSON con el mensaje y el ID del nuevo usuario.
                return objectMapper.writeValueAsString(Map.of("message", "Estudiante '" + nombre + "' registrado con éxito."));

            } catch (Exception e) {
                // Si ocurre cualquier error durante la operación de DB, se captura aquí.
                System.err.println("Error al registrar estudiante: " + e.getMessage());
                e.printStackTrace(); // Imprime el stack trace para depuración.
                res.status(500); // Internal Server Error.
                return objectMapper.writeValueAsString(Map.of("error", "Error interno al registrar estudiante: " + e.getMessage()));
            }
        });

        get("/crearMateria", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            // 1. Manejo de mensajes de éxito/error enviados por redirecciones previas
            String successMessage = req.queryParams("successMessage");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            String errorMessage = req.queryParams("errorMessage");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            // 2. Obtener los datos de la base de datos usando ActiveJDBC
            List<Plan> listaPlanes = Plan.findAll();
            List<Materia> listaMaterias = Materia.findAll();

            // 3. Pasar las listas al modelo Mustache
            // Las claves ("planes" y "materias") deben coincidir EXACTAMENTE
            // con las etiquetas que usaste en tu crear_materia.mustache
            model.put("planes", listaPlanes);
            model.put("materias", listaMaterias);

            return new ModelAndView(model, "crear_materia.mustache");
        }, new MustacheTemplateEngine());

        post("/crearMateria", (req, res) -> {
            try {
                // 1. Obtenemos los datos
                String nombre = req.queryParams("nombre");
                String codigoStr = req.queryParams("codigo");

                // 2. Validación básica para evitar nulos
                if (nombre == null || nombre.trim().isEmpty() || codigoStr == null || codigoStr.trim().isEmpty()) {
                    res.redirect("/crearMateria?errorMessage=Todos los campos son obligatorios.");
                    return null;
                }

                int codigo = Integer.parseInt(codigoStr);

                // 3. Intentamos guardar en la base de datos
                Materia m = new Materia();
                m.set("nombre", nombre);
                m.set("codigo", codigo);
                m.saveIt(); // Si el código ya existe, esto lanza una DBException

                // 4. Si todo sale bien, redirigimos con éxito
                // (Lo mando a /crearMateria para que el usuario vea la lista actualizada ahí mismo)
                res.redirect("/crearMateria?successMessage=Materia '" + nombre + "' creada exitosamente.");

            } catch (org.javalite.activejdbc.DBException e) {
                // Atajamos el error de SQLite (casi seguro es por el código UNIQUE duplicado)
                System.err.println("Error de Base de Datos al crear materia: " + e.getMessage());
                res.redirect("/crearMateria?errorMessage=El código de materia ya existe en el sistema.");

            } catch (NumberFormatException e) {
                // Atajamos el error si en vez de un número ingresan letras en el código
                System.err.println("Error de formato numérico: " + e.getMessage());
                res.redirect("/crearMateria?errorMessage=El código debe ser un número válido.");

            } catch (Exception e) {
                // Atajamos cualquier otro error imprevisto
                System.err.println("Error general al crear materia: " + e.getMessage());
                res.redirect("/crearMateria?errorMessage=Error interno al intentar guardar la materia.");
            }

            return null; // Siempre retornamos null después de un redirect en Spark
        });

        post("/vincularPlanMateria", (req,res) ->{
            int planID = Integer.parseInt(req.queryParams("plan_id"));
            int materiaID = Integer.parseInt(req.queryParams("materia_id"));

            PlanMateria pm = new PlanMateria();
            pm.set("plan_id", planID);
            pm.set("materia_id", materiaID);
            pm.saveIt();

            res.redirect("/dashboard?successMessage=Materia vinculada al plan");
            return null;
        });

        get("/inscripcion", (req,res) -> {
//            // 1. Verificar que haya iniciado sesión
//            Boolean loggedIn = req.session().attribute("loggedIn");
//            if (loggedIn == null || !loggedIn) {
//                res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
//                return null;
//            }
//
//            // 2. Verificar que el rol sea ESPECÍFICAMENTE "estudiante"
//            String rolUsuario = req.session().attribute("rol");
//            // Asumimos que guardaste el rol en minúsculas en la BD
//            if (rolUsuario == null || !rolUsuario.equals("estudiante")) {
//                System.out.println("DEBUG: Intento de acceso denegado a /inscripcion por rol: " + rolUsuario);
//                // Lo mandamos al dashboard con un mensaje de error
//                res.redirect("/dashboard?error=Acceso denegado. Esta sección es exclusiva para estudiantes.");
//                return null;
//            }

            Map<String, Object> model = new HashMap<>();

            List<Materia> materias = Materia.findAll();
            model.put("materias", materias);

            return new ModelAndView(model, "inscripcion.mustache");
        }, new MustacheTemplateEngine());

        post("/inscribir", (req, res) ->{
            int materiaID = Integer.parseInt(req.queryParams("materia_id"));
            int estudianteID = req.session().attribute("userID");

            EstudianteMateria inscripcion = new EstudianteMateria();
            inscripcion.set("estudiante_id", estudianteID);
            inscripcion.set("materia_id", materiaID);

            res.redirect("/inscripcion?successMessage=Inscripción exitosa");
            return null;
        });

        get("/dashboardEstudiante", (req, res)->{
           Map<String, Object> model = new HashMap<>();

           String currentUsername = req.session().attribute("currentUsername");
           model.put("username", currentUsername);

           return new ModelAndView(model, "dashboard_estudiante.mustache");
        }, new MustacheTemplateEngine());

        get("/crearPlan", (req,res)->{
            Map<String,Object> model= new HashMap<>();
            List<Carrera> carreras = Carrera.findAll();
            model.put("carreras", carreras);
            return new ModelAndView(model, "crear_plan.mustache");
        }, new MustacheTemplateEngine());

        post("/crearPlan" , (req,res)->{
           try{
               int anio = Integer.parseInt(req.queryParams("anio"));
               int carreraID = Integer.parseInt(req.queryParams("carrera_id"));

               Plan nuevoPlan = new Plan();
               nuevoPlan.set("año", anio);
               nuevoPlan.set("carrera_id", carreraID);
               nuevoPlan.saveIt();

               res.redirect("/crearPlan?successMessage=Plan creado exitosamente.");
           } catch (Exception e) {
               System.err.println("Error al crear plan: " + e.getMessage());
               res.redirect("/crearPlan?errorMessage=Error al crear el plan. Verifica los datos");
           }
           return null;
        });

        // GET: Ruta para mostrar el dashboard (panel de control) del usuario.
        // Requiere que el usuario esté autenticado.
        get("/dashboardCarrera", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla del dashboard.

            // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
            String currentUsername = req.session().attribute("currentUserUsername");
            Boolean loggedIn = req.session().attribute("loggedIn");

            // 1. Verificar si el usuario ha iniciado sesión.
            // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
            // significa que el usuario no está logueado o su sesión expiró.
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
                // Redirige al login con un mensaje de error.
                res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
                return null; // Importante retornar null después de una redirección.
            }

            // 2. Si el usuario está logueado, añade el nombre de usuario al modelo para la plantilla.
            model.put("username", currentUsername);

            // 3. Renderiza la plantilla del dashboard con el nombre de usuario.
            return new ModelAndView(model, "dashboard_carrera.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.


    } // Fin del método main
} // Fin de la clase App