package com.is1.proyecto.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.ProfesorMateria;
import com.is1.proyecto.models.Usuario;
import com.is1.proyecto.models.Profesor;
import org.javalite.activejdbc.Base;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.get;
import static spark.Spark.post;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.mindrot.jbcrypt.BCrypt;

public class ProfesorController {

    // Instancia estática y final de ObjectMapper para la serialización/deserialización JSON.
    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void registrarRutas(){
        get("/registrarProfesor", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Crea un mapa para pasar datos a la plantilla.
            String currentUsername = req.session().attribute("username");
            Boolean loggedIn = req.session().attribute("loggedIn");
            String rol = req.session().attribute("rol");
            // Validar sesión
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
                return null;
            }
            // Validar rol (Solo Admins)
            if (!"administrador".equals(rol)) {
                res.redirect("/login?error=No tienes permisos para gestionar usuarios.");
                return null;
            }
            // Pasamos el nombre de usuario para que el Mustache lo salude
            model.put("username", currentUsername);
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
            // 1. Ahora necesitamos credenciales de acceso, además de los datos físicos
            String username = req.queryParams("username");
            String password = req.queryParams("password");
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String correo = req.queryParams("correo");
            String dni = req.queryParams("dni");
            String telefono = req.queryParams("telefono"); // Agregamos teléfono que está en la DB

            // Validaciones básicas (Asegurate de que no falten los nuevos campos)
            if (username == null || username.isEmpty() || password == null || password.isEmpty() ||
                    nombre == null || nombre.isEmpty() || apellido == null || apellido.isEmpty() ||
                    correo == null || correo.isEmpty() || dni == null || dni.isEmpty()) {

                res.status(400);
                res.redirect("/registrarProfesor?error=Todos los campos obligatorios son requeridos.");
                return "";
            }

            // Validar formato de correo (Mantenemos tu lógica)
            if (!correo.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                res.redirect("/registrarProfesor?error=Correo invalido.");
                return "";
            }

            try {
                // INICIAMOS TRANSACCIÓN
                Base.openTransaction();

                // 1. Verificar si el username, DNI o correo ya existen
                if (Usuario.findFirst("username = ?", username) != null) {
                    throw new Exception("El nombre de usuario ya está en uso.");
                }
                if (Usuario.findFirst("dni = ?", dni) != null) {
                    throw new Exception("El DNI ya está registrado.");
                }
                if (Profesor.findFirst("correo = ?", correo) != null) {
                    throw new Exception("El correo ya está registrado.");
                }

                // 2. Creamos la identidad padre (Usuario)
                Usuario user = new Usuario();
                user.setUsername(username);
                user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt())); // Hasheado por seguridad
                user.setName(nombre);
                user.setApellido(apellido);
                user.setDNI(Integer.parseInt(dni));
                user.setTelefono(telefono); // Puede ser nulo
                user.setRol("profesor"); // Asignamos el rol estrictamente
                user.saveIt();

                // 3. Creamos el hijo (Profesor) vinculándolo al ID del Usuario recién creado
                Profesor pro = new Profesor();
                pro.set("usuario_id", user.getId()); // ¡CLAVE FORÁNEA!
                pro.set("correo", correo);
                pro.saveIt();

                // CONFIRMAMOS TRANSACCIÓN
                Base.commitTransaction();

                res.status(201);
                String mensaje = "Profesor " + nombre + " registrado exitosamente!";
                String mensajeCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8.toString());
                res.redirect("/registrarProfesor?message=" + mensajeCodificado);
                return "";

            } catch (Exception e) {
                // SI ALGO FALLA, DESHACEMOS TODO
                Base.rollbackTransaction();
                System.err.println("Error al registrar el profesor: " + e.getMessage());
                e.printStackTrace();

                // Pasamos el mensaje de la excepción para que el usuario sepa qué falló (ej: "El DNI ya está registrado")
                String errorMsg = URLEncoder.encode("Error: " + e.getMessage(), StandardCharsets.UTF_8.toString());
                res.redirect("/registrarProfesor?error=" + errorMsg);
                return "";
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

        // GET: Mostrar el formulario para vincular profesores a materias
        get("/vincularProfesores", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            // Validaciones de sesión y rol de administrador (igual que arriba)
            Boolean loggedIn = req.session().attribute("loggedIn");
            String rol = req.session().attribute("rol");
            if (loggedIn == null || !loggedIn || !"administrador".equals(rol)) {
                res.redirect("/login?error=Acceso denegado.");
                return null;
            }

            // Atajar mensajes de éxito o error
            String successMessage = req.queryParams("successMessage");
            if (successMessage != null) model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("errorMessage");
            if (errorMessage != null) model.put("errorMessage", errorMessage);

            // PASO CLAVE: Buscar datos para llenar los dropdowns
            List<Materia> listaMaterias = Materia.findAll();
            // Buscamos en la tabla Usuario a los que tienen rol 'profesor'
            List<Usuario> listaProfesores = Usuario.where("rol = ?", "profesor");

            model.put("materias", listaMaterias);
            model.put("profesores", listaProfesores);

            return new ModelAndView(model, "vincular_profesores.mustache");
        }, new MustacheTemplateEngine());

        // POST: Procesar la vinculación
        post("/vincularProfesores", (req, res) -> {
            try {
                // 1. Obtener datos del formulario
                // Nota: El form nos manda el ID del Usuario, no el del Profesor directamente
                String usuarioIdStr = req.queryParams("profesor_id");
                String materiaIdStr = req.queryParams("materia_id");
                String cargo = req.queryParams("cargo");

                // Validar nulos
                if (usuarioIdStr == null || materiaIdStr == null || cargo == null) {
                    res.redirect("/vincularProfesores?errorMessage=Todos los campos son obligatorios.");
                    return null;
                }

                int usuarioId = Integer.parseInt(usuarioIdStr);
                int materiaId = Integer.parseInt(materiaIdStr);

                // 2. Buscar el registro "hijo" (Profesor) asociado a ese Usuario
                Profesor profReal = Profesor.findFirst("usuario_id = ?", usuarioId);

                if (profReal == null) {
                    res.redirect("/vincularProfesores?errorMessage=Error interno: No se encontró el registro físico del profesor.");
                    return null;
                }

                // 3. Crear y guardar la relación en la base de datos
                ProfesorMateria vinculo = new ProfesorMateria();
                vinculo.set("profesor_id", profReal.getId());
                vinculo.set("materia_id", materiaId);
                vinculo.set("cargo", cargo);
                vinculo.saveIt(); // Si ya está vinculado, ActiveJDBC tirará DBException (por la primary key compuesta)

                // 4. Redirigir con éxito
                res.redirect("/vincularProfesores?successMessage=Profesor vinculado a la materia exitosamente.");

            } catch (org.javalite.activejdbc.DBException e) {
                System.err.println("Error de BD: " + e.getMessage());
                res.redirect("/vincularProfesores?errorMessage=Este profesor ya está vinculado a esta materia.");
            } catch (Exception e) {
                System.err.println("Error general: " + e.getMessage());
                res.redirect("/vincularProfesores?errorMessage=Error interno al procesar la solicitud.");
            }
            return null;
        });
    }
}
