package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("teachers") // Esta anotación asocia explícitamente el modelo 'Teacher' con la tabla 'teacher' en la DB.
public class Teacher extends User {

    // ActiveJDBC mapea automáticamente las columnas de la tabla 'users'
    // (como 'id', 'name', 'password', etc.) a los atributos de esta clase.
    // No necesitas declarar los campos (id, name, password) aquí como variables de instancia,
    // ya que la clase Model base se encarga de la interacción con la base de datos.

    // Opcional: Puedes agregar métodos getters y setters si prefieres un acceso más tipado,
    // aunque los métodos genéricos de Model (getString(), set(), getInteger(), etc.) ya funcionan.

    public String getName() {
        return getString("name"); // Obtiene el valor de la columna 'name'
    }

    public void setName(String name) {
        set("name", name); // Establece el valor para la columna 'name'
    }

    public String getPassword() {
        return getString("password"); // Obtiene el valor de la columna 'password'
    }

    public void setPassword(String password) {
        set("password", password); // Establece el valor para la columna 'password'
    }

    public String getLastName() {
        return getString("lastname");
    }

    public void setLastname(String lastname){
        set("lastname", lastname);
    }

    public String getEmail(){
        return getString("email");
    }

    public void setEmail(String email){
        set("email", email);
    }

    public int getDNI(){
        return getInteger("dni");
    }

    public void setDNI(int dni){
        set("dni", dni);
    }

}