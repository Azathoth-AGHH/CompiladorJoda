/* ============================================================
   Archivo de especificacion JFlex para el lenguaje JODA
   Compatible con JFlex 1.9+
   Compilar con: java -jar JFlex.jar src/logica/lexico/Joda.flex
   ============================================================ */

package logica.lexico;

%%

%class LexerJoda
%unicode
%line
%column
%type Token

%{
    /* Metodo auxiliar para construir un token con numero de linea real */
    private Token token(Token.Tipo tipo, String lexema) {
        return new Token(tipo, lexema, yyline + 1);
    }
%}

/* ---- Definiciones de patrones ---- */

Espacio        = [ \t\r\n]+
ComentarioL    = "//"[^\r\n]*
Entero         = [0-9]+
Decimal        = [0-9]+"."[0-9]+
Cadena         = \"[^\"]*\"
Identificador  = [a-z][a-zA-Z0-9_]*

%%

/* ---- Ignorar espacios y comentarios ---- */
{Espacio}       { /* ignorar */ }
{ComentarioL}   { return token(Token.Tipo.COMENTARIO, yytext()); }

/* ---- Palabras reservadas - estructura ---- */
"entry"         { return token(Token.Tipo.PR_ENTRY,    yytext()); }
"object"        { return token(Token.Tipo.PR_OBJECT,   yytext()); }
"method"        { return token(Token.Tipo.PR_METHOD,   yytext()); }

/* ---- Palabras reservadas - definicion ---- */
"define"        { return token(Token.Tipo.PR_DEFINE,   yytext()); }
"int"           { return token(Token.Tipo.PR_INT,      yytext()); }
"dec"           { return token(Token.Tipo.PR_DEC,      yytext()); }
"string"        { return token(Token.Tipo.PR_STRING,   yytext()); }
"bool"          { return token(Token.Tipo.PR_BOOL,     yytext()); }
"void"          { return token(Token.Tipo.PR_VOID,     yytext()); }

/* ---- Palabras reservadas - control ---- */
"if"            { return token(Token.Tipo.PR_IF,       yytext()); }
"else"          { return token(Token.Tipo.PR_ELSE,     yytext()); }
"select"        { return token(Token.Tipo.PR_SELECT,   yytext()); }
"case"          { return token(Token.Tipo.PR_CASE,     yytext()); }
"loop"          { return token(Token.Tipo.PR_LOOP,     yytext()); }

/* ---- Palabras reservadas - I/O ---- */
"out"           { return token(Token.Tipo.PR_OUT,      yytext()); }
"input"         { return token(Token.Tipo.PR_INPUT,    yytext()); }

/* ---- Palabras reservadas - valores ---- */
"true"          { return token(Token.Tipo.PR_TRUE,     yytext()); }
"false"         { return token(Token.Tipo.PR_FALSE,    yytext()); }
"new"           { return token(Token.Tipo.PR_NEW,      yytext()); }
"return"        { return token(Token.Tipo.PR_RETURN,   yytext()); }

/* ---- Literales ---- */
{Decimal}       { return token(Token.Tipo.LIT_DECIMAL, yytext()); }
{Entero}        { return token(Token.Tipo.LIT_ENTERO,  yytext()); }
{Cadena}        { return token(Token.Tipo.LIT_CADENA,  yytext()); }

/* ---- Identificadores (despues de las palabras reservadas) ---- */
{Identificador} { return token(Token.Tipo.IDENTIFICADOR, yytext()); }

/* ---- Operadores relacionales (primero los de 2 chars) ---- */
"=="            { return token(Token.Tipo.OP_IGUAL,        yytext()); }
"!="            { return token(Token.Tipo.OP_DIFERENTE,    yytext()); }
">="            { return token(Token.Tipo.OP_MAYOR_IGUAL,  yytext()); }
"<="            { return token(Token.Tipo.OP_MENOR_IGUAL,  yytext()); }
">"             { return token(Token.Tipo.OP_MAYOR,        yytext()); }
"<"             { return token(Token.Tipo.OP_MENOR,        yytext()); }

/* ---- Operadores logicos ---- */
"&&"            { return token(Token.Tipo.OP_AND,          yytext()); }
"||"            { return token(Token.Tipo.OP_OR,           yytext()); }
"!"             { return token(Token.Tipo.OP_NOT,          yytext()); }

/* ---- Operadores aritmeticos ---- */
"+"             { return token(Token.Tipo.OP_SUMA,         yytext()); }
"-"             { return token(Token.Tipo.OP_RESTA,        yytext()); }
"*"             { return token(Token.Tipo.OP_MULTIPLICACION, yytext()); }
"/"             { return token(Token.Tipo.OP_DIVISION,     yytext()); }
"%"             { return token(Token.Tipo.OP_MODULO,       yytext()); }

/* ---- Operadores de asignacion e incremento ---- */
"++"            { return token(Token.Tipo.OP_INCREMENTO,   yytext()); }
"--"            { return token(Token.Tipo.OP_DECREMENTO,   yytext()); }
"="             { return token(Token.Tipo.OP_ASIGNACION,   yytext()); }

/* ---- Delimitadores ---- */
";"             { return token(Token.Tipo.DEL_PUNTO_COMA,  yytext()); }
"{"             { return token(Token.Tipo.DEL_LLAVE_A,     yytext()); }
"}"             { return token(Token.Tipo.DEL_LLAVE_C,     yytext()); }
"("             { return token(Token.Tipo.DEL_PAREN_A,     yytext()); }
")"             { return token(Token.Tipo.DEL_PAREN_C,     yytext()); }
"["             { return token(Token.Tipo.DEL_CORCHETE_A,  yytext()); }
"]"             { return token(Token.Tipo.DEL_CORCHETE_C,  yytext()); }
","             { return token(Token.Tipo.DEL_COMA,        yytext()); }
"."             { return token(Token.Tipo.DEL_PUNTO,       yytext()); }

/* ---- Error lexico ---- */
[^]             { return token(Token.Tipo.ERROR, yytext()); }