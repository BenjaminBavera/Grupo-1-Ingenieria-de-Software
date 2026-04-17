package com.is1.proyecto.controllers;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.Estudiante;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.get;
import static spark.Spark.post;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
                String mensaje = "Estudiante " + nombre + " registrado exitosamente!";
                String mensajeCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8.toString());
                res.redirect("/registrarEstudiante?message=" + mensajeCodificado);
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
