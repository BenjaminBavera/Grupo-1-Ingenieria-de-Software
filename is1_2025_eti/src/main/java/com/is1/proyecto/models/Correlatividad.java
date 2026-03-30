package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("correlatividad")
public class Correlatividad extends Model {

    public int getMateriaId() {
        return getInteger("materia_id");
    }

    public int getCorrelativaId() {
        return getInteger("correlativa_id");
    }

    public String getTipo() {
        return getString("tipo");
    }
}
