# Corrección: `duplicate class: promptzal.lexer.Lexer`

El lexer se genera automáticamente desde `src/main/jflex/PromptZal.jflex` por el plugin Maven de JFlex. No se debe crear una segunda clase `Lexer.java` en `src/main/java`.

1. En NetBeans, busca `class Lexer` en todos los archivos del proyecto.
2. Si existe `src/main/java/promptzal/lexer/Lexer.java`, confirma que es el archivo manual duplicado y elimínalo.
3. Conserva `PromptZal.jflex`, `Token.java` y `TipoToken.java`.
4. Ejecuta `mvn clean package` o usa **Clean and Build**.

Esta copia del ZIP no incluye una clase `Lexer.java` manual y fija explícitamente el directorio de salida de JFlex en `target/generated-sources/jflex`.
