package com.is1.proyecto.controllers;

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

public class PlanController {

    // Instancia estática y final de ObjectMapper para la serialización/deserialización JSON.
    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void registrarRutas(){
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


    }
}
