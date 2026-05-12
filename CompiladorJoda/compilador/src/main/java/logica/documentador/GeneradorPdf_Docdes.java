package logica.documentador;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * GeneradorPdf_Docdes
 *
 * Genera un PDF con la documentacion descriptiva linea por linea
 * producida por DocumentadorLinea.
 *
 * Solo debe invocarse cuando la compilacion es exitosa (sin errores).
 *
 * Uso:
 *   GeneradorPdf_Docdes gen = new GeneradorPdf_Docdes();
 *   String ruta = gen.generar(lineasDocumentacion, nombreArchivo);
 */
public class GeneradorPdf_Docdes {

    // ---- Constantes de diseno ----
    private static final float MARGEN_IZQ      = 50f;
    private static final float MARGEN_DER      = 50f;
    private static final float MARGEN_SUP      = 60f;
    private static final float MARGEN_INF      = 50f;
    private static final float ALTO_LINEA      = 14f;
    private static final float TAM_TITULO      = 16f;
    private static final float TAM_SUBTITULO   = 11f;
    private static final float TAM_CUERPO      = 9.5f;
    private static final float TAM_CABECERA    = 8f;

    // Colores inspirados en el tema oscuro de la GUI
    private static final Color COLOR_FONDO_ENCABEZADO = new Color(13, 17, 23);    // #0d1117
    private static final Color COLOR_ACENTO           = new Color(88, 166, 255);  // #58a6ff azul
    private static final Color COLOR_TEXTO_CLARO      = new Color(230, 237, 243); // #e6edf3
    private static final Color COLOR_TEXTO_TENUE      = new Color(139, 148, 158); // #8b949e
    private static final Color COLOR_LINEA_SEP        = new Color(48, 54, 61);    // #30363d
    private static final Color COLOR_VERDE            = new Color(63, 185, 80);   // #3fb950
    private static final Color COLOR_CUERPO_FILA_PAR  = new Color(22, 27, 34);   // #161b22
    private static final Color COLOR_CUERPO_FILA_IMPAR= new Color(28, 33, 40);   // #1c2128

    /**
     * Genera el PDF de documentacion descriptiva.
     *
     * @param lineasDoc    Lista de strings producida por DocumentadorLinea.documentarPorLinea()
     * @param archivoJoda  Nombre o ruta del archivo fuente .joda (para la portada)
     * @return             Ruta absoluta del PDF generado
     * @throws IOException Si no se puede escribir el archivo
     */
    public String generar(List<String> lineasDoc, String archivoJoda) throws IOException {

        // Determinar ruta de salida junto al archivo fuente (o en directorio temporal)
        String nombreBase = extraerNombreBase(archivoJoda);
        String timestamp  = LocalDateTime.now()
                               .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String rutaSalida = System.getProperty("java.io.tmpdir")
                            + File.separator
                            + "DocDescriptiva_" + nombreBase + "_" + timestamp + ".pdf";

        try (PDDocument documento = new PDDocument()) {

            // ---- PAGINA 1: PORTADA ----
            agregarPortada(documento, nombreBase, archivoJoda);

            // ---- PAGINAS DE CONTENIDO ----
            agregarPaginasContenido(documento, lineasDoc, nombreBase);

            documento.save(rutaSalida);
        }

        return rutaSalida;
    }

    // -----------------------------------------------------------------------
    // PORTADA
    // -----------------------------------------------------------------------
    private void agregarPortada(PDDocument doc, String nombreBase, String rutaOriginal)
            throws IOException {

        PDPage pagina = new PDPage(PDRectangle.A4);
        doc.addPage(pagina);

        float ancho  = pagina.getMediaBox().getWidth();
        float alto   = pagina.getMediaBox().getHeight();

        try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {

            // Fondo completo oscuro
            cs.setNonStrokingColor(COLOR_FONDO_ENCABEZADO);
            cs.addRect(0, 0, ancho, alto);
            cs.fill();

            // Banda de acento superior
            cs.setNonStrokingColor(COLOR_ACENTO);
            cs.addRect(0, alto - 8, ancho, 8);
            cs.fill();

            // Banda de acento inferior
            cs.addRect(0, 0, ancho, 8);
            cs.fill();

            // ---- Titulo principal ----
            float yTitulo = alto - 120;
            dibujarTexto(cs, "COMPILADOR JODA", PDType1Font.HELVETICA_BOLD,
                         22f, COLOR_ACENTO, centrarX("COMPILADOR JODA", PDType1Font.HELVETICA_BOLD, 22f, ancho), yTitulo);

            yTitulo -= 28;
            dibujarTexto(cs, "Documentacion Descriptiva", PDType1Font.HELVETICA_BOLD,
                         16f, COLOR_TEXTO_CLARO,
                         centrarX("Documentacion Descriptiva", PDType1Font.HELVETICA_BOLD, 16f, ancho), yTitulo);

            // Linea separadora central
            yTitulo -= 20;
            cs.setStrokingColor(COLOR_ACENTO);
            cs.setLineWidth(1.5f);
            cs.moveTo(MARGEN_IZQ * 2, yTitulo);
            cs.lineTo(ancho - MARGEN_IZQ * 2, yTitulo);
            cs.stroke();

            // ---- Datos del archivo ----
            yTitulo -= 40;
            String labelArchivo = "Archivo fuente:";
            dibujarTexto(cs, labelArchivo, PDType1Font.HELVETICA_BOLD,
                         11f, COLOR_TEXTO_TENUE,
                         centrarX(labelArchivo, PDType1Font.HELVETICA_BOLD, 11f, ancho), yTitulo);

            yTitulo -= 18;
            dibujarTexto(cs, nombreBase + ".joda", PDType1Font.HELVETICA_BOLD,
                         13f, COLOR_VERDE,
                         centrarX(nombreBase + ".joda", PDType1Font.HELVETICA_BOLD, 13f, ancho), yTitulo);

            // ---- Fecha y hora ----
            yTitulo -= 50;
            String fechaHora = "Generado: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss"));
            dibujarTexto(cs, fechaHora, PDType1Font.HELVETICA,
                         10f, COLOR_TEXTO_TENUE,
                         centrarX(fechaHora, PDType1Font.HELVETICA, 10f, ancho), yTitulo);

            // ---- Estado ----
            yTitulo -= 25;
            String estadoTxt = "Estado: COMPILACION EXITOSA";
            dibujarTexto(cs, estadoTxt, PDType1Font.HELVETICA_BOLD,
                         11f, COLOR_VERDE,
                         centrarX(estadoTxt, PDType1Font.HELVETICA_BOLD, 11f, ancho), yTitulo);

            // ---- Pie de portada ----
            dibujarTexto(cs, "JVM-J  |  Joint Object-Deployment Assembly",
                         PDType1Font.HELVETICA, 8f, COLOR_TEXTO_TENUE,
                         centrarX("JVM-J  |  Joint Object-Deployment Assembly",
                                  PDType1Font.HELVETICA, 8f, ancho), 25f);
        }
    }

    // -----------------------------------------------------------------------
    // PAGINAS DE CONTENIDO
    // -----------------------------------------------------------------------
    private void agregarPaginasContenido(PDDocument doc,
                                          List<String> lineasDoc,
                                          String nombreBase) throws IOException {

        // Filtrar y preparar las lineas utiles
        List<String> lineasUtiles = new ArrayList<>();
        for (String linea : lineasDoc) {
            if (linea == null) continue;
            String trim = linea.trim();
            if (trim.isEmpty()) continue;
            // Excluir la linea de encabezado y pie del DocumentadorLinea
            if (trim.startsWith("=== DOCUMENTACION")) continue;
            if (trim.startsWith("=== FIN"))           continue;
            lineasUtiles.add(linea);
        }

        PDRectangle tamPagina = PDRectangle.A4;
        float ancho  = tamPagina.getWidth();
        float alto   = tamPagina.getHeight();
        float areaUtil = ancho - MARGEN_IZQ - MARGEN_DER;
        float yMaxContenido = alto - MARGEN_SUP - 30; // deja espacio para cabecera
        float yMinContenido = MARGEN_INF + 20;        // deja espacio para pie

        PDPage paginaActual = null;
        PDPageContentStream cs = null;
        int numeroPagina = 1;
        float y = 0;
        int filaIdx = 0;  // para alternar color de fila

        for (int i = 0; i < lineasUtiles.size(); i++) {
            String linea = lineasUtiles.get(i);

            // Si no hay pagina activa o se agoto el espacio, crear nueva
            if (paginaActual == null || y < yMinContenido) {
                if (cs != null) {
                    cerrarPagina(cs, doc, paginaActual, numeroPagina, nombreBase, ancho, alto);
                    cs = null;
                    numeroPagina++;
                    filaIdx = 0;
                }
                paginaActual = new PDPage(tamPagina);
                doc.addPage(paginaActual);
                cs = new PDPageContentStream(doc, paginaActual);
                y = iniciarPagina(cs, ancho, alto, numeroPagina, nombreBase);
            }

            // Pintar fila con color alternado
            Color colorFila = (filaIdx % 2 == 0)
                ? COLOR_CUERPO_FILA_PAR
                : COLOR_CUERPO_FILA_IMPAR;
            cs.setNonStrokingColor(colorFila);
            cs.addRect(MARGEN_IZQ, y - 2, areaUtil, ALTO_LINEA);
            cs.fill();

            // Determinar si la linea es un encabezado de linea numerada
            boolean esLinea = linea.matches("Linea \\d+.*");

            // Color y fuente segun tipo
            Color colorTexto;
            PDType1Font fuente;
            float tamFuente;

            if (esLinea) {
                colorTexto = COLOR_ACENTO;
                fuente     = PDType1Font.HELVETICA_BOLD;
                tamFuente  = TAM_SUBTITULO;
                // Separador sutil antes de cada bloque de linea
                if (filaIdx > 0) {
                    cs.setStrokingColor(COLOR_LINEA_SEP);
                    cs.setLineWidth(0.4f);
                    cs.moveTo(MARGEN_IZQ, y + ALTO_LINEA - 1);
                    cs.lineTo(ancho - MARGEN_DER, y + ALTO_LINEA - 1);
                    cs.stroke();
                }
            } else {
                colorTexto = COLOR_TEXTO_CLARO;
                fuente     = PDType1Font.HELVETICA;
                tamFuente  = TAM_CUERPO;
            }

            // Truncar si es demasiado larga para la pagina
            String textoMostrar = truncarSiNecesario(linea, fuente, tamFuente, areaUtil);

            dibujarTexto(cs, textoMostrar, fuente, tamFuente, colorTexto, MARGEN_IZQ + 4, y + 3);

            y -= ALTO_LINEA;
            filaIdx++;
        }

        // Cerrar ultima pagina
        if (cs != null) {
            cerrarPagina(cs, doc, paginaActual, numeroPagina, nombreBase, ancho, alto);
        }
    }

    // -----------------------------------------------------------------------
    // CABECERA Y PIE DE PAGINA DE CONTENIDO
    // -----------------------------------------------------------------------

    /**
     * Dibuja la cabecera de una pagina de contenido y retorna la Y de inicio del cuerpo.
     */
    private float iniciarPagina(PDPageContentStream cs,
                                  float ancho, float alto,
                                  int numeroPagina, String nombreBase) throws IOException {
        // Fondo completo
        cs.setNonStrokingColor(COLOR_FONDO_ENCABEZADO);
        cs.addRect(0, 0, ancho, alto);
        cs.fill();

        // Banda superior de cabecera
        cs.setNonStrokingColor(new Color(22, 27, 34));
        cs.addRect(0, alto - 38, ancho, 38);
        cs.fill();

        // Linea de acento bajo la cabecera
        cs.setNonStrokingColor(COLOR_ACENTO);
        cs.addRect(0, alto - 40, ancho, 2);
        cs.fill();

        // Titulo en cabecera
        dibujarTexto(cs, "JODA  |  Doc. Descriptiva  |  " + nombreBase + ".joda",
                     PDType1Font.HELVETICA_BOLD, TAM_CABECERA, COLOR_ACENTO,
                     MARGEN_IZQ, alto - 24);

        // Numero de pagina (derecha)
        String pag = "Pagina " + numeroPagina;
        float xPag = ancho - MARGEN_DER - anchoTexto(pag, PDType1Font.HELVETICA, TAM_CABECERA);
        dibujarTexto(cs, pag, PDType1Font.HELVETICA, TAM_CABECERA, COLOR_TEXTO_TENUE, xPag, alto - 24);

        // Devolver Y donde empieza el cuerpo
        return alto - MARGEN_SUP;
    }

    /**
     * Dibuja el pie de pagina y cierra el ContentStream.
     */
    private void cerrarPagina(PDPageContentStream cs,
                               PDDocument doc, PDPage pagina,
                               int numeroPagina, String nombreBase,
                               float ancho, float alto) throws IOException {
        // Linea de acento sobre el pie
        cs.setNonStrokingColor(COLOR_ACENTO);
        cs.addRect(0, MARGEN_INF - 4, ancho, 1);
        cs.fill();

        // Texto del pie
        String pie = "Compilador JODA v2.2  |  JVM-J: JODA Virtual Machine  |  Compilacion exitosa";
        float xPie = centrarX(pie, PDType1Font.HELVETICA, TAM_CABECERA, ancho);
        dibujarTexto(cs, pie, PDType1Font.HELVETICA, TAM_CABECERA, COLOR_TEXTO_TENUE, xPie, MARGEN_INF - 14);

        cs.close();
    }

    // -----------------------------------------------------------------------
    // UTILIDADES DE DIBUJO
    // -----------------------------------------------------------------------

    private void dibujarTexto(PDPageContentStream cs, String texto,
                               PDType1Font fuente, float tamano,
                               Color color, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(fuente, tamano);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        // Sanitizar: eliminar caracteres no ASCII que PDFBox no puede renderizar con Type1
        String seguro = texto.replaceAll("[^\u0000-\u00FF]", "?")
                             .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        cs.showText(seguro);
        cs.endText();
    }

    private float anchoTexto(String texto, PDType1Font fuente, float tamano) throws IOException {
        return fuente.getStringWidth(texto) / 1000 * tamano;
    }

    private float centrarX(String texto, PDType1Font fuente, float tamano, float anchoPagina)
            throws IOException {
        float tw = anchoTexto(texto, fuente, tamano);
        return (anchoPagina - tw) / 2f;
    }

    private String truncarSiNecesario(String texto, PDType1Font fuente,
                                       float tamano, float anchoMax) throws IOException {
        if (anchoTexto(texto, fuente, tamano) <= anchoMax) return texto;
        // Truncar con elipsis
        while (texto.length() > 4 && anchoTexto(texto + "...", fuente, tamano) > anchoMax) {
            texto = texto.substring(0, texto.length() - 1);
        }
        return texto + "...";
    }

    private String extraerNombreBase(String ruta) {
        if (ruta == null || ruta.isEmpty()) return "programa";
        String nombre = new File(ruta).getName();
        int punto = nombre.lastIndexOf('.');
        return (punto > 0) ? nombre.substring(0, punto) : nombre;
    }
}