PRAGMA foreign_keys = ON; --Habilita las claves foráneas

-- Elimina la tabla 'users' si ya existe para asegurar un inicio limpio
DROP TABLE IF EXISTS users;

DROP TABLE IF EXISTS persona;
DROP TABLE IF EXISTS estudiante;
DROP TABLE IF EXISTS profesor;

-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    name TEXT NOT NULL UNIQUE,          -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    password TEXT NOT NULL           -- Contraseña hasheada (TEXT es el tipo de cadena recomendado para SQLite)
);

-- Tabla persona
CREATE TABLE persona (
    dni INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL,
    apellido TEXT NOT NULL,
    telefono TEXT 
);

-- Tabla profesor
CREATE TABLE profesor (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dni INTEGER NOT NULL,
    correo TEXT NOT NULL,
    FOREIGN KEY (dni) REFERENCES persona(dni)
);

-- Tabla estudiante
CREATE TABLE estudiante (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dni INTEGER NOT NULL,
    anioIngreso INTEGER NOT NULL,
    nivel TEXT CHECK(nivel IN ('principiante', 'avanzado')),
    FOREIGN KEY (dni) REFERENCES persona(dni)
);
