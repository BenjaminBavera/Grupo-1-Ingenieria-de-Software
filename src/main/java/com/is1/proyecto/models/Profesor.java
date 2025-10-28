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

    public String getName() {
        return getString("nombre"); // Obtiene el valor de la columna 'name'
    }

    public void setName(String name) {
        set("nombre", name); // Establece el valor para la columna 'name'
    }

    public String getLastName() {
        return getString("apellido");
    }

    public void setLastname(String lastname){
        set("apellido", lastname);
    }

    public int getTelefono(){
        return getInteger("telefono");   
    }    

    public void setTelefono(int tel){
        set("telefono", tel);
    }

    public String getEmail(){
        return getString("mail");
    }

    public void setEmail(String email){
        set("mail", email);
    }

    public int getDNI(){
        return getInteger("dni");
    }

    public void setDNI(int dni){
        set("dni", dni);
    }

}