@echo off
REM Script para generar el lexer con JFlex
REM Ejecutar desde la raiz del proyecto

echo Generando LexerJoda.java con JFlex...
java -jar JFlex.jar src/logica/lexico/Joda.flex

IF %ERRORLEVEL% == 0 (
    echo Lexer generado correctamente en src/logica/lexico/LexerJoda.java
) ELSE (
    echo Error al generar el lexer. Verifique JFlex.jar y el archivo Joda.flex
)
pause