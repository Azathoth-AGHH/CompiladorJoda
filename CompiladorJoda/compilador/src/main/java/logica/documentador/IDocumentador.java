package logica.documentador;

import java.util.List;
import logica.lexico.Token;

public interface IDocumentador {
    String documentar(List<Token> tokens, String codigoFuente);
}