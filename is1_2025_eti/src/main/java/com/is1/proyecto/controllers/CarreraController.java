package com.is1.proyecto.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Plan;
import com.is1.proyecto.models.PlanMateria;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.get;
import static spark.Spark.post;

public class CarreraController {
    // Instancia estática y final de ObjectMapper para la serialización/deserialización JSON.
    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void registrarRutas(){
        get("/crearCarrera", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String currentUsername = req.session().attribute("username");
            Boolean loggedIn = req.session().attribute("loggedIn");
            String rol = req.session().attribute("rol");
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                res.redirect("/?error=Debes iniciar sesión para acceder a esta página.");
                return null;
            }
            if (!"administrador".equals(rol)) {
                res.redirect("/dashboard?error=No tienes permisos para esta sección.");
                return null;
            }
            model.put("username", currentUsername);
            // --- LA MAGIA NUEVA: Traemos los datos de la BD ---
            model.put("carreras", Carrera.findAll());
            model.put("planes", Plan.findAll());
            model.put("materias", Materia.findAll());
            // --------------------------------------------------
            String successMessage = req.queryParams("message");
            if (successMessage != null) model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null) model.put("errorMessage", errorMessage);
            return new ModelAndView(model, "crear_carrera.mustache");
        }, new MustacheTemplateEngine());

        post("/crearCarrera/new", (req, res) -> {
            // 1. Capturar el dato del formulario
            String nombre = req.queryParams("nombre");
            // 2. Seguridad: Verificar que solo el Admin pueda crear carreras
            String rol = req.session().attribute("rol");
            if (!"administrador".equals(rol)) {
                res.redirect("/dashboard?error=Permiso denegado. Solo administradores.");
                return "";
            }
            // 3. Validación de campos vacíos
            if (nombre == null || nombre.trim().isEmpty()) {
                res.redirect("/crearCarrera?error=El nombre de la carrera es obligatorio.");
                return "";
            }
            try {
                // 4. Regla de Negocio: Evitar carreras duplicadas
                if (Carrera.findFirst("nombre = ?", nombre.trim()) != null) {
                    throw new Exception("Ya existe una carrera registrada con ese nombre.");
                }
                // 5. Guardar en Base de Datos
                Carrera carrera = new Carrera();
                carrera.set("nombre", nombre.trim()); // .trim() saca espacios en blanco accidentales
                carrera.saveIt();
                // 6. Redirigir con éxito
                String mensaje = "La carrera '" + nombre + "' se creó correctamente.";
                String msgEncoded = URLEncoder.encode(mensaje, StandardCharsets.UTF_8.toString());
                res.redirect("/crearCarrera?message=" + msgEncoded);
            } catch (Exception e) {
                // Manejo de errores
                System.err.println("Error al crear carrera: " + e.getMessage());
                String errorEncoded = URLEncoder.encode("Error: " + e.getMessage(), StandardCharsets.UTF_8.toString());
                res.redirect("/crearCarrera?error=" + errorEncoded);
            }
            return "";
        });

        //NO TOQUEN ESTO!!!!!!!!! ME FALTA HACER EL JSON PARA CARRERA
        /** NO TOCAR!!!! TENGO QUE CAMBIAR PROFESOR POR CARRERA
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
         **/
    }
}
