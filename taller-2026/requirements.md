# Especificación del Proyecto: Sistema de Gestión Estudiantil
## 1. Problema que se quiere resolver
La Universidad presenta ineficiencias en la gestión académica debido a la fragmentación de la información y procesos manuales. Los desafíos críticos detectados son:
* **Inconsistencia de datos:** El uso de planillas y sistemas aislados genera duplicidad y errores.
* **Gestión de Correlatividades:** La falta de validación automatizada provoca inscripciones que no cumplen con los planes de estudio.
* **Seguimiento del Progreso:** Dificultad para obtener una visión clara del avance de los estudiantes en tiempo real.
  
## 2. Usuarios del sistema
* **Administradores (Oficina de Alumnos):** Control total de legajos, carreras, planes de estudio y asignación docente.
* **Estudiantes:** Acceso a su historia académica, consulta de horarios e inscripciones.
* **Docentes:** Registro de calificaciones, gestión de sus comisiones asignadas y consultas de listado de alumnos.

## 3. Funcionalidades principales
* **Administración Académica:** Alta, Baja y Modificación de alumnos, docentes, materias y planes.
* **Validación de Correlativas:** Control automático de requisitos previos antes de confirmar una inscripción.
* **Gestión de Cursadas y Notas:** Registro de la actividad académica y cálculo de promedios.
* **Consultas de Avance:** Visualización del plan de estudios y materias aprobadas/pendientes.

## 5. Tamaño del equipo
El proyecto es llevado adelante por un equipo de **3 integrantes**

## 6. Tecnologías elegidas y justificación
* **Lenguaje:** **Java**. Seleccionado por su robustez, manejo de excepciones y para aprovechar el paradigma orientado a objetos. La estructura de paquetes refleja el uso de herencia (clase Persona como base de Estudiante y Profesor) para evitar la duplicación de atributos comunes y facilitar el mantenimiento.
* **Base de Datos:** **SQLite**. Se eligió por su **portabilidad "zero-configuration"**. Al ser un motor *serverless* basado en un único archivo, facilita enormemente el trabajo colaborativo en el equipo de 3, permitiendo compartir el estado de la base de datos a través del repositorio sin necesidad de instalar motores pesados como MySQL o PostgreSQL en cada equipo.
* **Motor de Plantillas:** **Mustache**. Se seleccionó para la capa de presentación. Al ser un sistema de plantillas logic-less, nos obligó a mantener una separación limpia entre la lógica de negocio en Java y la visualización, facilitando la creación de interfaces dinámicas como el dashboard y los formularios de registro.
* **Patrón de Diseño:** **Singleton**. Implementado en la clase DBConfigSingleton para gestionar la conexión a SQLite. Esto garantiza que exista una única instancia de la conexión a la base de datos en toda la ejecución, optimizando el uso de recursos y evitando bloqueos en el archivo de la base de datos.
* **Control de Versiones:** **Git/GitHub**. Indispensable para coordinar el trabajo de los 3 integrantes.

## 7. Plazo estimado
El ciclo de vida del proyecto se planifica para un ciclo de **un cuatrimestre (aprox. 4 meses)**, con entregas parciales enfocadas en la evolución del análisis hacia la implementación final.
