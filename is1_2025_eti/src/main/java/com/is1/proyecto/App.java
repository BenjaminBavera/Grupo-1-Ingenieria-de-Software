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

//Para "encodear" el mensaje y que sea seguro viajar por la URL
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.is1.proyecto.controllers.*; // Importar todos los controladores



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
            Map<String, Object> model = new HashMap<>();
            String username = req.session().attribute("username");
            Boolean loggedIn = req.session().attribute("loggedIn");
            String rol = req.session().attribute("rol");

            // Validación de sesión
            if (loggedIn == null || !loggedIn || username == null) {
                res.redirect("/?error=Inicia sesión primero");
                return null;
            }
            // Validación de Rol (solo permitimos al admin aquí)
            if (!"administrador".equals(rol)) {
                res.redirect("/?error=No tienes permisos de administrador");
                return null;
            }
            model.put("username", username);
            // Asegúrate de que el nombre del archivo coincida con el que tenías para el admin
            return new ModelAndView(model, "dashboard.mustache");
        }, new MustacheTemplateEngine());

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

        // GET: Muestra el formulario para registrar un nuevo Administrador
        get("/crearAdmin", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            // 1. Validar la sesión y el rol actual
            String currentUsername = req.session().attribute("username");
            Boolean loggedIn = req.session().attribute("loggedIn");
            String rol = req.session().attribute("rol");
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                res.redirect("/?error=Debes iniciar sesión para acceder a esta página.");
                return null;
            }
            // ¡SEGURIDAD! Solo un administrador puede crear otro administrador
            if (!"administrador".equals(rol)) {
                System.out.println("DEBUG: Intento de acceso denegado a /crearAdmin por rol: " + rol);
                // Lo mandamos a su panel correspondiente con un reto
                res.redirect("/dashboard?error=No tienes permisos para registrar administradores.");
                return null;
            }
            // 2. Capturar mensajes de éxito o error que vienen del POST
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }
            // 3. Renderizar la plantilla HTML
            return new ModelAndView(model, "crear_admin.mustache");
        }, new MustacheTemplateEngine());

        // --- Rutas POST para manejar envíos de formularios y APIs ---
        //Metodo post para crear un admin
        post("/admin/new", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dni = req.queryParams("dni");
            String telefono = req.queryParams("telefono");

            // Validaciones
            if (username == null || username.isEmpty() || password == null || password.isEmpty() ||
                    nombre == null || nombre.isEmpty() || apellido == null || apellido.isEmpty() || dni == null || dni.isEmpty()) {

                res.status(400);
                res.redirect("/crearAdmin?error=Todos los campos obligatorios son requeridos.");
                return "";
            }

            try {
                // Verificar duplicados
                if (Usuario.findFirst("username = ?", username) != null) {
                    throw new Exception("El nombre de usuario ya está en uso.");
                }
                if (Usuario.findFirst("dni = ?", dni) != null) {
                    throw new Exception("El DNI ya está registrado.");
                }

                // Crear la entidad
                Usuario admin = new Usuario();
                admin.setUsername(username);
                admin.setPassword(BCrypt.hashpw(password, BCrypt.gensalt())); // Hasheamos!
                admin.setName(nombre);
                admin.setApellido(apellido);
                admin.setDNI(Integer.parseInt(dni));
                admin.setTelefono(telefono);

                // ¡LA CLAVE! Forzamos el rol desde el backend, el usuario no elige.
                admin.setRol("administrador");

                admin.saveIt(); // Guardamos en DB

                res.status(201);
                String mensaje = "Administrador " + nombre + " registrado exitosamente!";
                String mensajeCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8.toString());
                // Redirigimos de vuelta al formulario (o al dashboard, como prefieras)
                res.redirect("/crearAdmin?message=" + mensajeCodificado);
                return "";

            } catch (Exception e) {
                System.err.println("Error al registrar el admin: " + e.getMessage());
                String errorMsg = URLEncoder.encode("Error: " + e.getMessage(), StandardCharsets.UTF_8.toString());
                res.redirect("/crearAdmin?error=" + errorMsg);
                return "";
            }
        });

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
            Map<String, Object> model = new HashMap<>();
            String username = req.queryParams("username");
            String plainTextPassword = req.queryParams("password");
            // Validaciones básicas
            if (username == null || username.isEmpty() || plainTextPassword == null || plainTextPassword.isEmpty()) {
                res.status(400); // Bad Request.
                model.put("errorMessage", "El nombre de usuario y la contraseña son requeridos.");
                return new ModelAndView(model, "login.mustache");
            }
            // 1. Buscamos usando el NUEVO modelo y la columna correcta ('username')
            Usuario ac = Usuario.findFirst("username = ?", username);
            // Si no se encuentra ninguna cuenta con ese nombre de usuario.
            if (ac == null) {
                res.status(401); // Unauthorized.
                model.put("errorMessage", "Usuario o contraseña incorrectos.");
                return new ModelAndView(model, "login.mustache");
            }
            // Usamos el getter que creaste en el paso anterior
            String storedHashedPassword = ac.getPassword();
            // Comparamos las contraseñas
            if (BCrypt.checkpw(plainTextPassword, storedHashedPassword)) {
                res.status(200); // OK.
                // --- 2. Gestión de Sesión Mejorada ---
                req.session(true).attribute("username", ac.getUsername());
                req.session().attribute("usuario_id", ac.getId());
                req.session().attribute("loggedIn", true);
                // ¡LA CLAVE DE LA ARQUITECTURA NUEVA! Guardamos el rol en sesión
                String rol = ac.getRol();
                req.session().attribute("rol", rol);
                System.out.println("DEBUG: Login exitoso para la cuenta: " + username + " | Rol: " + rol);
                // --- 3. Redirección Basada en Roles (Patrón PRG) ---
                // Dependiendo de quién se logueó, lo mandamos a su pantalla correspondiente
                switch (rol) {
                    case "administrador":
                        res.redirect("/dashboard"); // Usamos la ruta que ya tenías armada
                        break;
                    case "profesor":
                        res.redirect("/dashboardProfesor");
                        break;
                    case "estudiante":
                        res.redirect("/dashboardEstudiante");
                        break;
                    default:
                        // Si por algún motivo tiene un rol inválido en la DB
                        res.redirect("/login?error=Rol de usuario no reconocido.");
                }
                return null; // En Spark, después de un redirect SIEMPRE retornamos null
            } else {
                // Contraseña incorrecta.
                res.status(401); // Unauthorized.
                System.out.println("DEBUG: Intento de login fallido para: " + username);
                model.put("errorMessage", "Usuario o contraseña incorrectos.");
                return new ModelAndView(model, "login.mustache");
            }
        }, new MustacheTemplateEngine());

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

        registrarRutas();

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

        // GET: Ruta para mostrar el dashboard (panel de control) del usuario.
        // Requiere que el usuario esté autenticado.
        get("/gestionUsuario", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla del dashboard.

            String currentUsername = req.session().attribute("username");
            Boolean loggedIn = req.session().attribute("loggedIn");
            String rol = req.session().attribute("rol");

            // Validar sesión
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                res.redirect("/?error=Debes iniciar sesión para acceder a esta página.");
                return null;
            }
            // Validar rol (Solo Admins)
            if (!"administrador".equals(rol)) {
                res.redirect("/dashboard?error=No tienes permisos para gestionar usuarios.");
                return null;
            }
            // Pasamos el nombre de usuario para que el Mustache lo salude
            model.put("username", currentUsername);
            // 3. Renderiza la plantilla del dashboard con el nombre de usuario.
            return new ModelAndView(model, "dashboard_gestUsuario.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

    } // Fin del método main

    public static void registrarRutas(){
        ProfesorController.registrarRutas();
        EstudianteController.registrarRutas();
        MateriaController.registrarRutas();
        PlanController.registrarRutas();
        CarreraController.registrarRutas();
    }
} // Fin de la clase App