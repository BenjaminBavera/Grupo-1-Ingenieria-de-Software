package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("usuario")
public class Usuario extends Model{

    public String getName(){
        return getString("nombre");
    }

    public void setName(String nombre){
        set("nombre", nombre);
    }

    public String getApellido(){
        return getString("apellido");
    }

    public void setApellido(String apellido){
        set("apellido", apellido);
    }

    public String getTelefono(){
        return getString("telefono");
    }    

    public void setTelefono(String tel){
        set("telefono", tel);
    }

    public int getDNI(){
        return getInteger("dni");
    }

    public void setDNI(int dni){
        set("dni", dni);
    }

    public String getPassword(){
        return getString("password");
    }

    public void setPassword(String password){
        set("password", password);
    }

    public String getRol(){
        return getString("rol");
    }

    public void setRol(String rol){
        set("rol", rol);
    }

    public String getUsername(){
        return getString("username");
    }

    public void setUsername(String username){
        set("username", username);
    }
}
