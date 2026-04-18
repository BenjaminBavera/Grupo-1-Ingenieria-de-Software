package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("materia")
public class Materia extends Model {

    public String getNombre() {
        return getString("nombre");
    }

    public void setNombre(String nombre) {
        set("nombre", nombre);
    }

    public int getAnioCursado() {
        return getInteger("anio_cursado");
    }

    public void setAnioCursado(int anio) {
        set("anio_cursado", anio);
    }

    public int getCuatrimestre() {
        return getInteger("cuatrimestre");
    }

    public void setCuatrimestre(int cuatrimestre) {
        set("cuatrimestre", cuatrimestre);
    }

    // La clave foránea que la une al Plan
    public int getPlanId() {
        return getInteger("plan_id");
    }

    public void setPlanId(int planId) {
        set("plan_id", planId);
    }
}