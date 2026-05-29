package logica.nucleo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class LectorArchivo {

    private LectorArchivo() {}

    public static String leer(String ruta) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(ruta));
        return limpiar(new String(bytes, StandardCharsets.UTF_8));
    }

    public static String limpiar(String codigo) {
        String normalizado = codigo.replace("\r\n", "\n").replace("\r", "\n");
        StringBuilder sb = new StringBuilder(normalizado.length());
        for (char c : normalizado.toCharArray()) {
            if (c == '\n' || c == '\t' || (c >= 32 && c < 127)) sb.append(c);
        }
        return sb.toString();
    }
}