package com.is1.proyecto.models;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("estudiante")
public class Estudiante extends Model {
    public int getUsuarioId() { return getInteger("usuario_id"); }
    public void setUsuarioId(int usuarioId) { set("usuario_id", usuarioId); }

    public int getAnioIngreso() { return getInteger("anioIngreso"); }
    public void setAnioIngreso(int anio) { set("anioIngreso", anio); }

    public String getNivel() { return getString("nivel"); }
    public void setNivel(String nivel) { set("nivel", nivel); }
}