package com.is1.proyecto.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Plan;
import com.is1.proyecto.models.PlanMateria;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.get;
import static spark.Spark.post;

public class MateriaController {

    // Instancia estática y final de ObjectMapper para la serialización/deserialización JSON.
    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void registrarRutas(){

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

    }
}
