PRAGMA foreign_keys = ON; --Habilita las claves foráneas

-- Elimina la tabla 'users' si ya existe para asegurar un inicio limpio
DROP TABLE IF EXISTS users;

-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    name TEXT NOT NULL UNIQUE,          -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    password TEXT NOT NULL           -- Contraseña hasheada (TEXT es el tipo de cadena recomendado para SQLite)
);

CREATE TABLE teachers(
    id INTEGER PRIMARY KEY,
    lastname TEXT NOT NULL,
    email TEXT NOT NULL,
    dni INTEGER NOT NULL UNIQUE

    CONSTRAINT fk_teacher_user FOREIGN KEY (id) REFERENCES users(id)
);