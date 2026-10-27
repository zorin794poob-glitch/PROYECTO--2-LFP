# Casos de uso — PromptZal 3

## Actores
- **Programador/usuario:** escribe y compila archivos PromptZal.
- **Sistema de archivos:** origen y destino de archivos `.pz`, `.md`, `.txt` y reportes HTML.

## CU-01 — Crear o editar programa
**Precondición:** aplicación abierta. **Flujo:** el usuario escribe código en el editor; puede iniciar un archivo nuevo. **Postcondición:** código disponible en el editor.

## CU-02 — Abrir archivo `.pz`
**Flujo:** seleccionar Abrir, elegir un archivo `.pz`; el contenido aparece en el editor. **Alternativas:** si ocurre un error de lectura, se muestra un mensaje.

## CU-03 — Guardar programa
**Flujo:** pulsar Guardar; si no existe ruta, seleccionar destino; se escribe el contenido con extensión `.pz`.

## CU-04 — Compilar programa
**Precondición:** texto disponible. **Flujo:** el sistema ejecuta análisis léxico, parser descendente recursivo, construcción de AST, validación semántica y generación del prompt. Muestra el prompt y errores de la compilación. **Alternativas:** los errores léxicos y sintácticos se acumulan; el parser intenta recuperarse usando tokens de sincronización.

## CU-05 — Exportar prompt
**Flujo:** pulsar Guardar prompt y elegir archivo `.md` o `.txt`. **Postcondición:** el prompt generado queda guardado.

## CU-06 — Generar reporte de errores
**Flujo:** compilar y seleccionar Reporte errores; guardar HTML. El reporte incluye tipo, descripción, fila, columna y, cuando corresponda, token encontrado y esperado. Si no hay errores, lo indica explícitamente.

## CU-07 — Generar tabla de símbolos
**Flujo:** compilar y seleccionar Tabla símbolos; guardar HTML con nombre, tipo, ámbito y línea.

## CU-08 — Generar reporte AST
**Flujo:** compilar y seleccionar Reporte AST; guardar una representación textual indentada del árbol en HTML.

## CU-09 — Validar semántica
**Reglas:** no duplicar agentes; resolver referencias de `EJECUTAR`; verificar identificadores dentro del ámbito del agente; permitir como destino una variable de resultado; verificar que `EXPORTAR` nombre una variable perteneciente a un agente ejecutado. Los identificadores iniciales de `GENERAR` y unidades después de números se tratan como excepciones.
