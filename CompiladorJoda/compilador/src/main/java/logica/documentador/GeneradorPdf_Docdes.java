package logica.documentador;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/*
GeneradorPdf_Docdes

Genera un PDF con la documentacion descriptiva linea por linea
producida por DocumentadorLinea.

Solo debe invocarse cuando la compilacion es exitosa (sin errores).
Uso:
    GeneradorPdf_Docdes gen = new GeneradorPdf_Docdes();
    String ruta = gen.generar(lineasDocumentacion, nombreArchivo);
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
     * El PDF se guarda en la misma carpeta donde se encuentra el JAR ejecutable.
     * Si no se puede determinar la ubicacion del JAR (por ejemplo, al correr
     * desde el IDE), se usa el directorio de trabajo actual como fallback.
     */
    public String generar(List<String> lineasDoc, String archivoJoda) throws IOException {

        String nombreBase  = extraerNombreBase(archivoJoda);
        String carpetaJar  = obtenerCarpetaJar();
        String rutaSalida  = carpetaJar + File.separator
                             + "DocDescriptiva_" + nombreBase + ".pdf";

        try (PDDocument documento = new PDDocument()) {

            // ---- PAGINA 1: PORTADA ----
            agregarPortada(documento, nombreBase, archivoJoda);

            // ---- PAGINAS DE CONTENIDO ----
            agregarPaginasContenido(documento, lineasDoc, nombreBase);

            documento.save(rutaSalida);
        }

        return rutaSalida;
    }

    // ---------------------------------------------------------------
    // DETECCION DE LA CARPETA DEL JAR
    // ---------------------------------------------------------------

    /**
     * Devuelve la ruta absoluta de la carpeta que contiene el JAR en ejecucion.
     *
     * Estrategia:
     *   1. Intenta obtener la ubicacion real del JAR mediante CodeSource.
     *   2. Si falla (IDE, tests, rutas con caracteres especiales), usa user.dir
     *      como fallback seguro.
     *
     * Esto garantiza que el PDF siempre aparezca junto al ejecutable,
     * independientemente del directorio de trabajo desde el que se lance la JVM.
     */
    private String obtenerCarpetaJar() {
        try {
            // getProtectionDomain().getCodeSource().getLocation() devuelve la
            // URL del JAR (o de la carpeta de clases si se corre desde el IDE).
            java.net.URL ubicacion = GeneradorPdf_Docdes.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation();

            // Convertir URL -> URI -> File para manejar correctamente espacios
            // y caracteres especiales en la ruta (ej: "Mi Proyecto" en el path).
            File archivoJar = new File(ubicacion.toURI());

            // Si la ubicacion apunta al JAR mismo, subimos al directorio padre.
            // Si apunta a una carpeta (IDE), la usamos directamente.
            if (archivoJar.isFile()) {
                return archivoJar.getParent();
            } else {
                return archivoJar.getAbsolutePath();
            }

        } catch (URISyntaxException | SecurityException | NullPointerException e) {
            // Fallback: usar el directorio de trabajo actual.
            // Ocurre en entornos con SecurityManager restrictivo o en algunos
            // servidores de aplicaciones.
            System.err.println("Advertencia GeneradorPdf: no se pudo determinar "
                    + "la carpeta del JAR, usando directorio de trabajo. Causa: "
                    + e.getMessage());
            return System.getProperty("user.dir");
        }
    }

    // ---------------------------------------------------------------
    // PORTADA
    // ---------------------------------------------------------------

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
            dibujarTexto(cs, "COMPILADOR JODA",
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                    22f, COLOR_ACENTO,
                    centrarX("COMPILADOR JODA",
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 22f, ancho),
                    yTitulo);

            yTitulo -= 28;
            dibujarTexto(cs, "Documentacion Descriptiva",
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                    16f, COLOR_TEXTO_CLARO,
                    centrarX("Documentacion Descriptiva",
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16f, ancho),
                    yTitulo);

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
            dibujarTexto(cs, labelArchivo,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                    11f, COLOR_TEXTO_TENUE,
                    centrarX(labelArchivo,
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11f, ancho),
                    yTitulo);

            yTitulo -= 18;
            dibujarTexto(cs, nombreBase + ".joda",
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                    13f, COLOR_VERDE,
                    centrarX(nombreBase + ".joda",
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13f, ancho),
                    yTitulo);

            // ---- Fecha y hora ----
            yTitulo -= 50;
            String fechaHora = "Generado: "
                    + LocalDateTime.now()
                                   .format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss"));
            dibujarTexto(cs, fechaHora,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                    10f, COLOR_TEXTO_TENUE,
                    centrarX(fechaHora,
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10f, ancho),
                    yTitulo);

            // ---- Estado ----
            yTitulo -= 25;
            String estadoTxt = "Estado: COMPILACION EXITOSA";
            dibujarTexto(cs, estadoTxt,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                    11f, COLOR_VERDE,
                    centrarX(estadoTxt,
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11f, ancho),
                    yTitulo);

            // ---- Pie de portada ----
            dibujarTexto(cs, "JVM-J  |  Joint Object-Deployment Assembly",
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                    8f, COLOR_TEXTO_TENUE,
                    centrarX("JVM-J  |  Joint Object-Deployment Assembly",
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8f, ancho),
                    25f);
        }
    }

    // ---------------------------------------------------------------
    // PAGINAS DE CONTENIDO
    // ---------------------------------------------------------------

    private void agregarPaginasContenido(PDDocument doc,
                                          List<String> lineasDoc,
                                          String nombreBase) throws IOException {

        // Filtrar y preparar las lineas utiles
        List<String> lineasUtiles = new ArrayList<>();
        for (String linea : lineasDoc) {
            if (linea == null) continue;
            String trim = linea.trim();
            if (trim.isEmpty()) continue;
            if (trim.startsWith("=== DOCUMENTACION")) continue;
            if (trim.startsWith("=== FIN"))           continue;
            lineasUtiles.add(linea);
        }

        PDRectangle tamPagina    = PDRectangle.A4;
        float ancho              = tamPagina.getWidth();
        float alto               = tamPagina.getHeight();
        float areaUtil           = ancho - MARGEN_IZQ - MARGEN_DER;
        float yMaxContenido      = alto - MARGEN_SUP - 30;
        float yMinContenido      = MARGEN_INF + 20;

        PDPage               paginaActual = null;
        PDPageContentStream  cs           = null;
        int  numeroPagina = 1;
        float y           = 0;
        int   filaIdx     = 0;

        for (String linea : lineasUtiles) {

            // Nueva pagina si es necesario
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
                y  = iniciarPagina(cs, ancho, alto, numeroPagina, nombreBase);
            }

            // Color alternado de fila
            Color colorFila = (filaIdx % 2 == 0) ? COLOR_CUERPO_FILA_PAR : COLOR_CUERPO_FILA_IMPAR;
            cs.setNonStrokingColor(colorFila);
            cs.addRect(MARGEN_IZQ, y - 2, areaUtil, ALTO_LINEA);
            cs.fill();

            boolean esLinea = linea.matches("Linea \\d+.*");

            Color       colorTexto;
            PDType1Font fuente;
            float       tamFuente;

            if (esLinea) {
                colorTexto = COLOR_ACENTO;
                fuente     = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                tamFuente  = TAM_SUBTITULO;
                if (filaIdx > 0) {
                    cs.setStrokingColor(COLOR_LINEA_SEP);
                    cs.setLineWidth(0.4f);
                    cs.moveTo(MARGEN_IZQ, y + ALTO_LINEA - 1);
                    cs.lineTo(ancho - MARGEN_DER, y + ALTO_LINEA - 1);
                    cs.stroke();
                }
            } else {
                colorTexto = COLOR_TEXTO_CLARO;
                fuente     = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                tamFuente  = TAM_CUERPO;
            }

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

    // ---------------------------------------------------------------
    // CABECERA Y PIE DE PAGINA
    // ---------------------------------------------------------------

    private float iniciarPagina(PDPageContentStream cs,
                                 float ancho, float alto,
                                 int numeroPagina, String nombreBase) throws IOException {
        // Fondo
        cs.setNonStrokingColor(COLOR_FONDO_ENCABEZADO);
        cs.addRect(0, 0, ancho, alto);
        cs.fill();

        // Banda de cabecera
        cs.setNonStrokingColor(new Color(22, 27, 34));
        cs.addRect(0, alto - 38, ancho, 38);
        cs.fill();

        // Linea de acento bajo la cabecera
        cs.setNonStrokingColor(COLOR_ACENTO);
        cs.addRect(0, alto - 40, ancho, 2);
        cs.fill();

        // Titulo en cabecera
        dibujarTexto(cs,
                "JODA  |  Doc. Descriptiva  |  " + nombreBase + ".joda",
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                TAM_CABECERA, COLOR_ACENTO, MARGEN_IZQ, alto - 24);

        // Numero de pagina (derecha)
        String pag  = "Pagina " + numeroPagina;
        float  xPag = ancho - MARGEN_DER
                - anchoTexto(pag, new PDType1Font(Standard14Fonts.FontName.HELVETICA), TAM_CABECERA);
        dibujarTexto(cs, pag,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                TAM_CABECERA, COLOR_TEXTO_TENUE, xPag, alto - 24);

        return alto - MARGEN_SUP;
    }

    private void cerrarPagina(PDPageContentStream cs,
                               PDDocument doc, PDPage pagina,
                               int numeroPagina, String nombreBase,
                               float ancho, float alto) throws IOException {
        // Linea de acento sobre el pie
        cs.setNonStrokingColor(COLOR_ACENTO);
        cs.addRect(0, MARGEN_INF - 4, ancho, 1);
        cs.fill();

        // Texto del pie
        String pie  = "Compilador JODA v2.2  |  JVM-J: JODA Virtual Machine  |  Compilacion exitosa";
        float  xPie = centrarX(pie,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA), TAM_CABECERA, ancho);
        dibujarTexto(cs, pie,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                TAM_CABECERA, COLOR_TEXTO_TENUE, xPie, MARGEN_INF - 14);

        cs.close();
    }

    // ---------------------------------------------------------------
    // UTILIDADES DE DIBUJO
    // ---------------------------------------------------------------

    private void dibujarTexto(PDPageContentStream cs, String texto,
                               PDType1Font fuente, float tamano,
                               Color color, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(fuente, tamano);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        // Sanitizar: eliminar caracteres no Latin-1 que PDFBox Type1 no soporta
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
        return (anchoPagina - anchoTexto(texto, fuente, tamano)) / 2f;
    }

    private String truncarSiNecesario(String texto, PDType1Font fuente,
                                       float tamano, float anchoMax) throws IOException {
        if (anchoTexto(texto, fuente, tamano) <= anchoMax) return texto;
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