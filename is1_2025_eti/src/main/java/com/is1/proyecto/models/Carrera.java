package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("carrera")
public class Carrera extends Model {

    public String getNombre() {
        return getString("nombre");
    }

    public void setNombre(String nombre) {
        set("nombre", nombre);
    }

    public int getCodigo() {
        return getInteger("codigo");
    }

    public void setCodigo(int codigo) {
        set("codigo", codigo);
    }

    public Integer getPlanVigenteId() {
        return getInteger("plan_vigente_id");
    }

    public void setPlanVigenteId(Integer planId) {
        set("plan_vigente_id", planId);
    }
}
