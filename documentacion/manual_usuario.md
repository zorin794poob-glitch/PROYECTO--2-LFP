# Manual de usuario — PromptZal 3

## 1. Inicio
Abra la carpeta del proyecto en Apache NetBeans y ejecute la clase `com.promptzal.Main`. También puede compilar con `javac` siguiendo el README. La ventana tiene una barra de acciones, un editor a la izquierda y pestañas de prompt y errores a la derecha.

## 2. Crear, abrir y guardar
- **Nuevo:** limpia el editor.
- **Abrir .pz:** permite elegir un archivo PromptZal.
- **Guardar:** guarda el texto con extensión `.pz`.

## 3. Compilar
Escriba o abra un programa y pulse **Compilar**. El sistema ejecuta las fases disponibles y muestra el prompt generado. Si hay errores, aparecerán en la pestaña de errores con tipo y posición.

## 4. Guardar el prompt
Pulse **Guardar prompt** y seleccione una ruta `.md` o `.txt`. El resultado incluye directivas, contexto de cada agente ejecutado, variables, instrucciones y resultados solicitados.

## 5. Reportes
- **Reporte errores:** HTML con los errores o indicación explícita de que no existen.
- **Tabla símbolos:** HTML con identificadores, tipo, ámbito y línea.
- **Reporte AST:** HTML con árbol textual indentado.

## 6. Ejemplo
Abra `ejemplos/analista.pz`, pulse Compilar y revise el prompt. El programa de ejemplo carga `ventas.csv`, pregunta por tendencias, resume el resultado y exporta `resumen`.

## 7. Diagnóstico básico
- Si aparece `Identificador no declarado`, compruebe que la variable se definió antes en el mismo agente.
- Si `EJECUTAR` no encuentra un agente, verifique que el nombre coincida.
- Si `EXPORTAR` falla, asegúrese de que la variable sea resultado o variable de un agente ejecutado.
- Si hay un error sintáctico, compruebe llaves, flechas `->`, conectores y el orden contexto/variables/comandos.

## 8. Capturas para la entrega
Las capturas dependen de la ejecución en el equipo del estudiante; no se incluyen capturas ficticias. Tome capturas reales de: interfaz con el ejemplo abierto, prompt generado, panel de errores y cada reporte HTML abierto en el navegador. Insértelas en este manual antes de entregar.
