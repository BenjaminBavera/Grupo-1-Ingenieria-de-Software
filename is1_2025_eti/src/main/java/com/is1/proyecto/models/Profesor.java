package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("profesor") // Esta anotación asocia explícitamente el modelo 'Teacher' con la tabla 'teacher' en la DB.
public class Profesor extends Model {

    // ActiveJDBC mapea automáticamente las columnas de la tabla 'users'
    // (como 'id', 'name', 'password', etc.) a los atributos de esta clase.
    // No necesitas declarar los campos (id, name, password) aquí como variables de instancia,
    // ya que la clase Model base se encarga de la interacción con la base de datos.

    // Opcional: Puedes agregar métodos getters y setters si prefieres un acceso más tipado,
    // aunque los métodos genéricos de Model (getString(), set(), getInteger(), etc.) ya funcionan.

    public String getNombre() {
        return getString("nombre"); // Obtiene el valor de la columna 'nombre'
    }

    public void setNombre(String nombre) {
        set("nombre", nombre); // Establece el valor para la columna 'nombre'
    }

    public String getApellido() {
        return getString("apellido");
    }

    public void setApellido(String apellido){
        set("apellido", apellido);
    }

    public int getTelefono(){
        return getInteger("telefono");   
    }    

    public void setTelefono(int tel){
        set("telefono", tel);
    }

    public String getCorreo(){
        return getString("correo");
    }

    public void setCorreo(String correo){
        set("mail", correo);
    }

    public int getDNI(){
        return getInteger("dni");
    }

    public void setDNI(int dni){
        set("dni", dni);
    }

}