package com.is1.proyecto.controllers;

import com.is1.proyecto.models.Materia;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import static spark.Spark.post;

public class MateriaController {

    public static void registrarRutas() {

        post("/crearMateria/new", (req, res) -> {
            String planId = req.queryParams("plan_id");
            String nombre = req.queryParams("nombre");
            String anioCursado = req.queryParams("anio_cursado");
            String cuatrimestre = req.queryParams("cuatrimestre");

            String rol = req.session().attribute("rol");
            if (!"administrador".equals(rol)) {
                res.redirect("/dashboard?error=Permiso denegado.");
                return "";
            }

            try {
                Materia materia = new Materia();
                materia.set("plan_id", Integer.parseInt(planId));
                materia.set("nombre", nombre.trim());
                materia.set("anio_cursado", Integer.parseInt(anioCursado));
                materia.set("cuatrimestre", Integer.parseInt(cuatrimestre));
                materia.saveIt();

                String msg = URLEncoder.encode("Materia creada y vinculada con éxito.", StandardCharsets.UTF_8.toString());
                res.redirect("/crearCarrera?message=" + msg);
            } catch (Exception e) {
                String err = URLEncoder.encode("Error al crear materia: " + e.getMessage(), StandardCharsets.UTF_8.toString());
                res.redirect("/crearCarrera?error=" + err);
            }
            return "";
        });

        post("/eliminarMateria", (req, res) -> {
            String materiaId = req.queryParams("materia_id");

            String rol = req.session().attribute("rol");
            if (!"administrador".equals(rol)) {
                res.redirect("/dashboard?error=Permiso denegado.");
                return "";
            }

            try {
                Materia materia = Materia.findById(materiaId);
                if (materia != null) {
                    materia.delete();
                    String msg = URLEncoder.encode("Materia eliminada.", StandardCharsets.UTF_8.toString());
                    res.redirect("/crearCarrera?message=" + msg);
                } else {
                    res.redirect("/crearCarrera?error=No se encontró la materia.");
                }
            } catch (Exception e) {
                res.redirect("/crearCarrera?error=Error al eliminar la materia.");
            }
            return "";
        });
    }
}