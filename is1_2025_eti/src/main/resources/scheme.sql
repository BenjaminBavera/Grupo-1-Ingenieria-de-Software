PRAGMA foreign_keys = ON; --Habilita las claves foráneas

-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE IF NOT EXISTS users(
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    name TEXT NOT NULL UNIQUE,          -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    password TEXT NOT NULL           -- Contraseña hasheada (TEXT es el tipo de cadena recomendado para SQLite)
);

-- Tabla persona
CREATE TABLE IF NOT EXISTS persona(
    dni INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL,
    apellido TEXT NOT NULL,
    telefono TEXT 
);

-- Tabla profesor
CREATE TABLE IF NOT EXISTS profesor (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dni INTEGER NOT NULL,
    correo TEXT NOT NULL UNIQUE,
    CONSTRAINT fk_profesor_persona FOREIGN KEY (dni) REFERENCES persona(dni) ON DELETE CASCADE
);

-- Tabla estudiante
CREATE TABLE IF NOT EXISTS estudiante (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dni INTEGER NOT NULL,
    anioIngreso INTEGER NOT NULL,
    nivel TEXT CHECK(nivel IN ('principiante', 'avanzado')),
    CONSTRAINT fk_estudiante_persona FOREIGN KEY (dni) REFERENCES persona(dni) ON DELETE CASCADE
);

-- Tabla carrera
CREATE TABLE IF NOT EXISTS carrera (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    codigo INTEGER UNIQUE,
    plan_vigente_id INTEGER,
    FOREIGN KEY (plan_vigente_id) REFERENCES plan(id)
);

-- Tabla plan
CREATE TABLE IF NOT EXISTS plan(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    año INTEGER NOT NULL,
    carrera_id INTEGER NOT NULL,
    FOREIGN KEY (carrera_id) REFERENCES carrera(id) ON DELETE CASCADE
);

-- Tabla materia
CREATE TABLE IF NOT EXISTS materia(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    codigo INTEGER UNIQUE
);

-- Tabla de la relacion N a N Plan-Materia
CREATE TABLE IF NOT EXISTS plan_materia (
    plan_id INTEGER,
    materia_id INTEGER,
    PRIMARY KEY (plan_id, materia_id),

    FOREIGN KEY (plan_id) REFERENCES plan(id) ON DELETE CASCADE,
    FOREIGN KEY (materia_id) REFERENCES materia(id) ON DELETE CASCADE
);

-- Tabla correlatividad (relacion recursiva de materia)
CREATE TABLE IF NOT EXISTS correlatividad(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    materia_id INTEGER NOT NULL,
    correlativa_id INTEGER NOT NULL,
    tipo TEXT CHECK(tipo IN ('regular', 'aprobada')),

    FOREIGN KEY (materia_id) REFERENCES materia(id) ON DELETE CASCADE,
    FOREIGN KEY (correlativa_id) REFERENCES materia(id) ON DELETE CASCADE
);