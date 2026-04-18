package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("plan")
public class Plan extends Model {

    public int getAnio() {
        return getInteger("anio");
    }

    public void setAnio(int anio) {
        set("anio", anio);
    }

    public boolean isVigente() {
        return getBoolean("es_vigente");
    }

    public void setVigente(boolean vigente) {
        set("es_vigente", vigente);
    }

    // La clave foránea que lo une a Carrera
    public int getCarreraId() {
        return getInteger("carrera_id");
    }

    public void setCarreraId(int carreraId) {
        set("carrera_id", carreraId);
    }

    public String getCarreraNombre() {
        Carrera carreraPadre = Carrera.findById(this.getCarreraId());
        if (carreraPadre != null) {
            return carreraPadre.getString("nombre");
        }
        return "Carrera Desconocida";
    }
}