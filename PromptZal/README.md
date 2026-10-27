# PromptZal – Proyecto 2 (LFP, USAC CUNOC)

Compilador de PromptZal: léxico con **JFlex**, sintáctico **descendente recursivo escrito a mano**,
AST, tabla de símbolos, validación semántica, generación del promptzal e IDE en **Java Swing**.

## Abrir en NetBeans
1. `Archivo > Abrir proyecto...` y elegir la carpeta `PromptZal` (proyecto Maven).
2. `Clean and Build` (necesita internet la primera vez: Maven descarga el plugin de JFlex,
   que genera `target/generated-sources/jflex/promptzal/lexer/Lexer.java` desde `src/main/jflex/PromptZal.jflex`).
3. `Run` (clase principal `promptzal.Main`). Requiere JDK 11 o superior.

## Estructura
- `src/main/jflex/PromptZal.jflex` – especificación léxica (JFlex).
- `parser/Parser.java` – un método por no terminal; recuperación en modo pánico.
- `ast/Ast.java` – nodos del AST. `semantic/` – tabla de símbolos y validaciones.
- `generator/GeneradorPrompt.java` – promptzal. `report/Reportes.java` – reportes HTML y AST.
- `gui/` – IDE (editor con resaltado, errores, símbolos, AST, tokens, promptzal).
- `ejemplos/` – programas `.pz`, promptzal generado y reportes de muestra.

## Pendiente de tu parte
Repositorio Git con commits distribuidos, manuales técnico y de usuario en PDF (con capturas),
y estar listo para la comprobación de conocimiento. Revisa y entiende cada clase antes de entregar.
