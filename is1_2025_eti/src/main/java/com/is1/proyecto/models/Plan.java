package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("plan")
public class Plan extends Model {

    public int getAnio() {
        return getInteger("anio");
    }

    public void setAño(int año) {
        set("año", año);
    }

    public int getCarreraId() {
        return getInteger("carrera_id");
    }

    public void setCarreraId(int carreraId) {
        set("carrera_id", carreraId);
    }
}
