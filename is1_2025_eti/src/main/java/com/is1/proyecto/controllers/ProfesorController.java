package com.is1.proyecto.controllers;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.Profesor;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.get;
import static spark.Spark.post;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ProfesorController {

    // Instancia estática y final de ObjectMapper para la serialización/deserialización JSON.
    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void registrarRutas(){
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
                String mensaje = "Profesor " + nombre + " registrado exitosamente!";
                String mensajeCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8.toString());
                res.redirect("/registrarProfesor?message=" + mensajeCodificado);
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
    }
}
