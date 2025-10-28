PRAGMA foreign_keys = ON; --Habilita las claves foráneas

-- Elimina la tabla 'users' si ya existe para asegurar un inicio limpio
DROP TABLE IF EXISTS users;

-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    name TEXT NOT NULL UNIQUE,          -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    password TEXT NOT NULL           -- Contraseña hasheada (TEXT es el tipo de cadena recomendado para SQLite)
);

CREATE TABLE persona(
    nombre TEXT NOT NULL,
    apellido TEXT NOT NULL,
    dni INTEGER NOT NULL UNIQUE,
    telefono INTEGER NOT NULL
);

CREATE TABLE profesor(
    dni INTEGER NOT NULL,
    mail TEXT NOT NULL

    CONSTRAINT fk_teacher_person FOREIGN KEY (dni) REFERENCES person(dni)
);

CREATE TABLE estudiante(
    dni INTEGER NOT NULL,
    añoIngreso INTEGER NOT NULL,
    nivel ENUM(principiante, avanzado) 

    CONSTRAINT fk_student FOREIGN KEY (dni) REFERENCES person(dni)
)