package logica.lexico;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Fachada del analizador lexico.
 * Usa el lexer generado por JFlex (LexerJoda) para tokenizar
 * el codigo fuente y retorna la lista de tokens.
 */
public class AnalizadorLexico {

    /**
     * Analiza el codigo fuente y retorna todos los tokens encontrados.
     * Los tokens de tipo COMENTARIO se incluyen en la lista (el documentador
     * puede filtrarlos si es necesario).
     *
     * @param codigoFuente texto completo del programa .joda
     * @return lista de tokens (incluyendo ERROR y EOF)
     */
    public List<Token> analizar(String codigoFuente) {
        List<Token> tokens = new ArrayList<>();
        try {
            LexerJoda lexer = new LexerJoda(new StringReader(codigoFuente));
            Token t;
            while ((t = lexer.yylex()) != null) {
                tokens.add(t);
                if (t.getTipo() == Token.Tipo.EOF) {
                    break;
                }
            }
            // Aseguramos un EOF al final si el lexer no lo genero
            if (tokens.isEmpty() || tokens.get(tokens.size() - 1).getTipo() != Token.Tipo.EOF) {
                tokens.add(new Token(Token.Tipo.EOF, "", 0));
            }
        } catch (Exception e) {
            tokens.add(new Token(Token.Tipo.ERROR,
                    "Error interno del lexer: " + limpiar(e.getMessage()), 0));
        }
        return tokens;
    }

    /** Elimina caracteres especiales no ASCII para evitar simbolos ? en consola. */
    private String limpiar(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[^\\x20-\\x7E]", "?");
    }
}