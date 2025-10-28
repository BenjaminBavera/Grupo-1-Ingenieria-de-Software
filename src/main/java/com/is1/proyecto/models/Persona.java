package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("persona")
public class Persona extends Model{
    
    public String getName(){
        return getString("nombre");
    }

    public void setName(String nombre){
        set("nombre", nombre);
    }

    public String getLastName(){
        return getString("apellido");
    }

    public void setLastName(String apellido){
        set("apellido", apellido);
    }

    public int getTelefono(){
        return getInteger("telefono");   
    }    

    public void setTelefono(int tel){
        set("telefono", tel);
    }

    public int getDNI(){
        return getInteger("dni");
    }

    public void setDNI(int dni){
        set("dni", dni);
    }
}
