package com.is1.proyecto.controllers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.Usuario;
import com.is1.proyecto.models.Estudiante;
import org.javalite.activejdbc.Base;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.get;
import static spark.Spark.post;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.mindrot.jbcrypt.BCrypt;


public class EstudianteController {
    // Instancia estática y final de ObjectMapper para la serialización/deserialización JSON.
    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void registrarRutas(){
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


        post("/registrarEstudiante/new", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dni = req.queryParams("dni");
            String telefono = req.queryParams("telefono");

            if (username == null || username.isEmpty() || password == null || password.isEmpty() ||
                    nombre == null || nombre.isEmpty() || apellido == null || apellido.isEmpty() || dni == null || dni.isEmpty()) {

                res.status(400);
                res.redirect("/registrarEstudiante?error=Todos los campos obligatorios son requeridos.");
                return "";
            }

            try {
                Base.openTransaction();

                // Verificaciones
                if (Usuario.findFirst("username = ?", username) != null) {
                    throw new Exception("El nombre de usuario ya está en uso.");
                }
                if (Usuario.findFirst("dni = ?", dni) != null) {
                    throw new Exception("El DNI ya está registrado.");
                }

                // 1. Crear Padre (Usuario)
                Usuario user = new Usuario();
                user.setUsername(username);
                user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
                user.setName(nombre);
                user.setApellido(apellido);
                user.setDNI(Integer.parseInt(dni));
                user.setTelefono(telefono);
                user.setRol("estudiante");
                user.saveIt();

                // 2. Crear Hijo (Estudiante)
                Estudiante est = new Estudiante();
                est.set("usuario_id", user.getId());
                est.set("anioIngreso", LocalDate.now().getYear()); // Calculamos automático
                est.set("nivel", "principiante"); // Por defecto según el CHECK de SQLite
                est.saveIt();

                Base.commitTransaction();

                res.status(201);
                String mensaje = "Estudiante " + nombre + " registrado exitosamente!";
                String mensajeCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8.toString());
                res.redirect("/crearCarrera?message=" + mensajeCodificado);
                return "";

            } catch (Exception e) {
                Base.rollbackTransaction();
                System.err.println("Error al registrar el estudiante: " + e.getMessage());
                String errorMsg = URLEncoder.encode("Error: " + e.getMessage(), StandardCharsets.UTF_8.toString());
                res.redirect("/registrarEstudiante?error=" + errorMsg);
                return "";
            }
        });
        // POST: Endpoint para añadir estudiantes (API que devuelve JSON, no HTML).
        // Advertencia: Esta ruta tiene un propósito diferente a las de formulario HTML.
        post("/add_estudiante", (req, res) -> {
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
    }
}
