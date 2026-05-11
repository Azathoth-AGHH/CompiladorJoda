package vista_grafica;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import logica.documentador.DocumentadorLinea;
import logica.lexico.Token;
import logica.nucleo.CompiladorJoda;
import logica.nucleo.ResultadoCompilacion;
import logica.semantico.EntradaTablaSimbolos;

/**
 * Controlador principal del Compilador JODA v2.0.
 *
 * Funcionalidades de ventana:
 *   - Cerrar, Minimizar, Maximizar / Restaurar (barra de titulo personalizada).
 *   - Arrastre de la ventana desde la barra de titulo.
 *   - Redimensionado por los 8 bordes y esquinas de la ventana.
 *
 * Funcionalidades de paneles:
 *   - Panel lateral (Tabla de Simbolos): colapsable con animacion suave.
 *   - Panel inferior (Resultados):       colapsable con animacion suave.
 *   - Ambos paneles tienen boton de toggle en la toolbar Y en su propia cabecera.
 */
public class Controlador {

    // ===================================================================
    // REFERENCIAS DE VENTANA
    // ===================================================================
    private Stage  stage;
    private double dragOffsetX, dragOffsetY;

    // Para redimensionado por bordes
    private static final int BORDE_RESIZE = 6; // px de zona activa en cada borde
    private double resizeInicioX, resizeInicioY;
    private double resizeInicioW, resizeInicioH;
    private double resizeInicioStageX, resizeInicioStageY;
    private String resizeDireccion = "";        // "N","S","E","O","NE","NO","SE","SO"

    // ===================================================================
    // ESTADO DE PANELES COLAPSABLES
    // ===================================================================
    private boolean panelLateralVisible  = true;
    private boolean panelInferiorVisible = true;

    // Posicion del divisor del SplitPane antes de colapsar el panel lateral
    private double posicionDivisorGuardada = 0.75;

    // Alto del panel inferior antes de colapsarlo
    private double altoInferiorGuardado = 260;

    // Alto fijo de la cabecera del panel inferior (siempre visible)
    private static final double ALTO_CABECERA = 30;

    // ===================================================================
    // COMPONENTES FXML
    // ===================================================================

    @FXML private HBox   barraTitulo;
    @FXML private Label  labelEstado;
    @FXML private Label  labelArchivo;
    @FXML private Button btnMinimizar;
    @FXML private Button btnMaximizar;
    @FXML private Button btnCerrar;
    @FXML private Button btnToggleLateral;
    @FXML private Button btnToggleInferior;

    @FXML private TextArea areaNumeros;
    @FXML private TextArea areaEditor;

    @FXML private SplitPane splitCentral;
    @FXML private VBox      panelLateral;

    @FXML private VBox   contenedorInferior;
    @FXML private Button btnColapsarInferior;

    @FXML private TableView<EntradaTablaSimbolos>          tablaSimbolos;
    @FXML private TableColumn<EntradaTablaSimbolos, String> colNombre;
    @FXML private TableColumn<EntradaTablaSimbolos, String> colTipo;
    @FXML private TableColumn<EntradaTablaSimbolos, String> colCategoria;
    @FXML private TableColumn<EntradaTablaSimbolos, String> colLinea;
    @FXML private TableColumn<EntradaTablaSimbolos, String> colValor;

    @FXML private TabPane tabPaneResultados;
    @FXML private Tab     tabSalida, tabErrores, tabTokens, tabDocTecnica, tabDocDescriptiva;
    @FXML private TextArea areaSalida, areaErrores, areaTokens, areaDocTecnica, areaDocDescriptiva;

    // ===================================================================
    // ESTADO INTERNO
    // ===================================================================
    private File    archivoActual = null;
    private boolean modificado    = false;

    // ===================================================================
    // INICIALIZACION
    // ===================================================================

    /**
     * Recibe el Stage desde App.java.
     * DEBE llamarse despues de load() y antes de show().
     */
    public void setStage(Stage stage) {
        this.stage = stage;
        // Configurar resize por bordes una vez que la Scene exista
        stage.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) configurarResizePorBordes();
        });
    }

    @FXML
    public void initialize() {
        configurarTablaSimbolos();
        configurarSincronizacionNumeros();
        insertar("");
        setEstado("Listo");
    }

    private void configurarTablaSimbolos() {
        colNombre   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colTipo     .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTipoDato().name()));
        colCategoria.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoria().name()));
        colLinea    .setCellValueFactory(d -> new SimpleStringProperty(
                        String.valueOf(d.getValue().getLineaDeclaracion())));
        colValor    .setCellValueFactory(d -> {
            Object val = d.getValue().getValor();
            return new SimpleStringProperty(val != null ? val.toString() : "(null)");
        });
    }

    private void configurarSincronizacionNumeros() {
        areaEditor.textProperty().addListener((obs, oldText, newText) -> {
            modificado = true;
            actualizarNumeros(newText);
        });
        areaEditor.scrollTopProperty().addListener((obs, oldVal, newVal) ->
            areaNumeros.setScrollTop(newVal.doubleValue())
        );
    }

    private void actualizarNumeros(String texto) {
        int lineas = texto.isEmpty() ? 1 : texto.split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lineas; i++) sb.append(i).append("\n");
        areaNumeros.setText(sb.toString());
    }

    // ===================================================================
    // CONTROL DE VENTANA: cerrar, minimizar, maximizar
    // ===================================================================

    /** Cierra la aplicacion, pidiendo confirmacion si hay cambios sin guardar. */
    @FXML
    public void accionCerrar() {
        if (modificado && !confirmarDescarte()) return;
        stage.close();
    }

    /** Minimiza la ventana a la barra de tareas. */
    @FXML
    public void accionMinimizar() {
        stage.setIconified(true);
    }

    /**
     * Alterna entre maximizado y restaurado.
     * Actualiza el icono del boton para reflejar el estado actual.
     */
    @FXML
    public void accionMaximizar() {
        boolean estaMax = stage.isMaximized();
        stage.setMaximized(!estaMax);
        btnMaximizar.setText(estaMax ? "□" : "❐");
    }

    // ===================================================================
    // ARRASTRE DE VENTANA DESDE LA BARRA DE TITULO
    // ===================================================================

    @FXML
    public void onTitleBarPressed(MouseEvent e) {
        // Si estaba maximizado, restaurar antes de empezar a arrastrar
        if (stage.isMaximized()) {
            stage.setMaximized(false);
            btnMaximizar.setText("□");
        }
        dragOffsetX = e.getScreenX() - stage.getX();
        dragOffsetY = e.getScreenY() - stage.getY();
    }

    @FXML
    public void onTitleBarDragged(MouseEvent e) {
        if (stage.isMaximized()) return;
        stage.setX(e.getScreenX() - dragOffsetX);
        stage.setY(e.getScreenY() - dragOffsetY);
    }

    // ===================================================================
    // REDIMENSIONADO POR BORDES (resize)
    // ===================================================================

    /**
     * Instala los listeners de mouse en la Scene para detectar los 8 bordes/esquinas
     * de la ventana sin decoraciones y permitir redimensionarla.
     */
    private void configurarResizePorBordes() {
        Scene scene = stage.getScene();
        if (scene == null) return;

        // Mover mouse: cambiar cursor segun zona
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            resizeDireccion = calcularDireccion(e.getX(), e.getY(), scene.getWidth(), scene.getHeight());
            scene.setCursor(cursoresParaDireccion(resizeDireccion));
        });

        // Presionar: guardar estado inicial
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!resizeDireccion.isEmpty()) {
                resizeInicioX      = e.getScreenX();
                resizeInicioY      = e.getScreenY();
                resizeInicioW      = stage.getWidth();
                resizeInicioH      = stage.getHeight();
                resizeInicioStageX = stage.getX();
                resizeInicioStageY = stage.getY();
            }
        });

        // Arrastrar: aplicar resize
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (resizeDireccion.isEmpty()) return;
            double dx = e.getScreenX() - resizeInicioX;
            double dy = e.getScreenY() - resizeInicioY;
            double minW = stage.getMinWidth();
            double minH = stage.getMinHeight();

            double nuevoX = resizeInicioStageX;
            double nuevoY = resizeInicioStageY;
            double nuevoW = resizeInicioW;
            double nuevoH = resizeInicioH;

            // Borde derecho / Este
            if (resizeDireccion.contains("E")) {
                nuevoW = Math.max(minW, resizeInicioW + dx);
            }
            // Borde izquierdo / Oeste
            if (resizeDireccion.contains("O")) {
                double candidatoW = resizeInicioW - dx;
                if (candidatoW >= minW) {
                    nuevoW = candidatoW;
                    nuevoX = resizeInicioStageX + dx;
                }
            }
            // Borde inferior / Sur
            if (resizeDireccion.contains("S")) {
                nuevoH = Math.max(minH, resizeInicioH + dy);
            }
            // Borde superior / Norte
            if (resizeDireccion.contains("N")) {
                double candidatoH = resizeInicioH - dy;
                if (candidatoH >= minH) {
                    nuevoH = candidatoH;
                    nuevoY = resizeInicioStageY + dy;
                }
            }

            stage.setX(nuevoX);
            stage.setY(nuevoY);
            stage.setWidth(nuevoW);
            stage.setHeight(nuevoH);
        });

        // Soltar: resetear direccion
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            resizeDireccion = "";
            scene.setCursor(Cursor.DEFAULT);
        });
    }

    /**
     * Calcula en que borde/esquina esta el mouse segun su posicion (x,y)
     * dentro de la ventana de tamano (w,h).
     * Retorna combinaciones de "N","S","E","O" o cadena vacia si no esta en borde.
     */
    private String calcularDireccion(double x, double y, double w, double h) {
        boolean norte = y < BORDE_RESIZE;
        boolean sur   = y > h - BORDE_RESIZE;
        boolean oeste = x < BORDE_RESIZE;
        boolean este  = x > w - BORDE_RESIZE;

        // La barra de titulo es zona de arrastre, no de resize
        boolean enBarraTitulo = y < 42;
        if (enBarraTitulo && !norte) return "";

        if (norte && oeste) return "NO";
        if (norte && este)  return "NE";
        if (sur   && oeste) return "SO";
        if (sur   && este)  return "SE";
        if (norte) return "N";
        if (sur)   return "S";
        if (este)  return "E";
        if (oeste) return "O";
        return "";
    }

    /** Mapea una direccion de resize al cursor de JavaFX correspondiente. */
    private Cursor cursoresParaDireccion(String dir) {
        switch (dir) {
            case "N":  return Cursor.N_RESIZE;
            case "S":  return Cursor.S_RESIZE;
            case "E":  return Cursor.E_RESIZE;
            case "O":  return Cursor.W_RESIZE;
            case "NE": return Cursor.NE_RESIZE;
            case "NO": return Cursor.NW_RESIZE;
            case "SE": return Cursor.SE_RESIZE;
            case "SO": return Cursor.SW_RESIZE;
            default:   return Cursor.DEFAULT;
        }
    }

    // ===================================================================
    // COLAPSAR / EXPANDIR PANEL LATERAL (Tabla de Simbolos)
    // ===================================================================

    /**
     * Alterna la visibilidad del panel lateral con animacion suave.
     * Cuando se colapsa: guarda la posicion del divisor y lo mueve a 1.0 (100%).
     * Cuando se expande: restaura la posicion guardada.
     */
    @FXML
    public void togglePanelLateral() {
        if (panelLateralVisible) {
            // Guardar y colapsar
            posicionDivisorGuardada = splitCentral.getDividerPositions()[0];
            animarDivisor(splitCentral.getDividerPositions()[0], 1.0, true);
            btnToggleLateral.setText("▶ Tabla");
            panelLateralVisible = false;
        } else {
            // Expandir y restaurar
            animarDivisor(splitCentral.getDividerPositions()[0], posicionDivisorGuardada, false);
            btnToggleLateral.setText("◀ Tabla");
            panelLateralVisible = true;
        }
    }

    /**
     * Anima la posicion del divisor del SplitPane entre dos valores.
     * Al terminar la animacion, oculta o muestra el panel lateral segun sea el caso.
     */
    private void animarDivisor(double desde, double hasta, boolean colapsando) {
        // Propiedad animable del divisor
        SplitPane.Divider divisor = splitCentral.getDividers().get(0);

        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(divisor.positionProperty(), desde)),
            new KeyFrame(Duration.millis(200),
                new KeyValue(divisor.positionProperty(), hasta))
        );

        tl.setOnFinished(e -> {
            panelLateral.setVisible(!colapsando);
            panelLateral.setManaged(!colapsando);
            if (!colapsando) {
                // Forzar posicion correcta despues de mostrar
                divisor.setPosition(posicionDivisorGuardada);
            }
        });

        tl.play();
    }

    // ===================================================================
    // COLAPSAR / EXPANDIR PANEL INFERIOR (Resultados)
    // ===================================================================

    /**
     * Alterna la visibilidad del panel inferior con animacion suave.
     * La cabecera siempre permanece visible para poder expandir de nuevo.
     * Botones sincronizados: el de la toolbar y el de la cabecera del panel.
     */
    @FXML
    public void togglePanelInferior() {
        if (panelInferiorVisible) {
            // Guardar alto actual y colapsar
            altoInferiorGuardado = contenedorInferior.getHeight();
            if (altoInferiorGuardado < 80) altoInferiorGuardado = 260;
            animarAltoPanel(altoInferiorGuardado, ALTO_CABECERA, true);
            btnToggleInferior.setText("▴ Panel");
            btnColapsarInferior.setText("▴");
            panelInferiorVisible = false;
        } else {
            // Expandir
            animarAltoPanel(ALTO_CABECERA, altoInferiorGuardado, false);
            btnToggleInferior.setText("▾ Panel");
            btnColapsarInferior.setText("▾");
            panelInferiorVisible = true;
        }
    }

    /**
     * Anima la altura del panel inferior entre dos valores.
     * Usa prefHeight y maxHeight para forzar el tamano en el layout de BorderPane.
     */
    private void animarAltoPanel(double desde, double hasta, boolean colapsando) {
        contenedorInferior.setPrefHeight(desde);
        contenedorInferior.setMaxHeight(desde);

        // Mostrar el tabPane durante la animacion de expansion
        if (!colapsando) {
            tabPaneResultados.setVisible(true);
            tabPaneResultados.setManaged(true);
        }

        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(contenedorInferior.prefHeightProperty(), desde),
                new KeyValue(contenedorInferior.maxHeightProperty(), desde)),
            new KeyFrame(Duration.millis(220),
                new KeyValue(contenedorInferior.prefHeightProperty(), hasta),
                new KeyValue(contenedorInferior.maxHeightProperty(), hasta))
        );

        tl.setOnFinished(e -> {
            if (colapsando) {
                // Ocultar el tabPane para que no consuma espacio ni sea clickeable
                tabPaneResultados.setVisible(false);
                tabPaneResultados.setManaged(false);
                contenedorInferior.setPrefHeight(ALTO_CABECERA);
                contenedorInferior.setMaxHeight(ALTO_CABECERA);
            } else {
                contenedorInferior.setPrefHeight(Region.USE_COMPUTED_SIZE);
                contenedorInferior.setMaxHeight(Region.USE_PREF_SIZE);
            }
        });

        tl.play();
    }

    // ===================================================================
    // ACCIONES DE ARCHIVO
    // ===================================================================

    @FXML
    public void accionNuevo() {
        if (modificado && !confirmarDescarte()) return;
        areaEditor.clear();
        archivoActual = null;
        modificado = false;
        limpiarResultados();
        setEstado("Nuevo archivo");
        labelArchivo.setText("Sin archivo");
    }

    @FXML
    public void accionAbrir() {
        if (modificado && !confirmarDescarte()) return;
        FileChooser fc = crearFileChooser("Abrir archivo JODA");
        File archivo = fc.showOpenDialog(getVentana());
        if (archivo == null) return;
        try {
            byte[] bytes = Files.readAllBytes(archivo.toPath());
            areaEditor.setText(new String(bytes, StandardCharsets.UTF_8));
            archivoActual = archivo;
            modificado = false;
            limpiarResultados();
            setEstado("Abierto: " + archivo.getName());
            labelArchivo.setText(archivo.getAbsolutePath());
        } catch (IOException e) {
            mostrarAlertaError("No se pudo abrir el archivo:\n" + e.getMessage());
        }
    }

    @FXML
    public void accionGuardar() {
        if (archivoActual == null) { accionGuardarComo(); return; }
        guardarEnArchivo(archivoActual);
    }

    @FXML
    public void accionGuardarComo() {
        FileChooser fc = crearFileChooser("Guardar archivo JODA");
        File archivo = fc.showSaveDialog(getVentana());
        if (archivo == null) return;
        if (!archivo.getName().endsWith(".joda"))
            archivo = new File(archivo.getAbsolutePath() + ".joda");
        guardarEnArchivo(archivo);
        archivoActual = archivo;
        labelArchivo.setText(archivo.getAbsolutePath());
    }

    private void guardarEnArchivo(File archivo) {
        try (FileWriter fw = new FileWriter(archivo, StandardCharsets.UTF_8)) {
            fw.write(areaEditor.getText());
            modificado = false;
            setEstado("Guardado: " + archivo.getName());
        } catch (IOException e) {
            mostrarAlertaError("No se pudo guardar:\n" + e.getMessage());
        }
    }

    // ===================================================================
    // COMPILAR Y EJECUTAR
    // ===================================================================

    @FXML
    public void accionCompilar() {
        String codigo = areaEditor.getText();
        if (codigo == null || codigo.trim().isEmpty()) {
            mostrarAlertaError("El editor esta vacio. Escribe o abre un archivo JODA.");
            return;
        }

        setEstado("Compilando...");
        limpiarResultados();

        // Si el panel inferior esta colapsado, expandirlo para mostrar resultados
        if (!panelInferiorVisible) togglePanelInferior();

        // Guardar en temporal si no hay archivo vigente o hay cambios
        File archivoCompilacion = archivoActual;
        if (archivoCompilacion == null || modificado) {
            try {
                archivoCompilacion = File.createTempFile("joda_", ".joda");
                archivoCompilacion.deleteOnExit();
                try (FileWriter fw = new FileWriter(archivoCompilacion, StandardCharsets.UTF_8)) {
                    fw.write(codigo);
                }
            } catch (IOException e) {
                mostrarAlertaError("Error al crear archivo temporal:\n" + e.getMessage());
                return;
            }
        }

        final File archivoFinal = archivoCompilacion;
        new Thread(() -> {
            CompiladorJoda compilador = new CompiladorJoda();
            ResultadoCompilacion resultado = compilador.compilarYEjecutar(archivoFinal.getAbsolutePath());
            Platform.runLater(() -> mostrarResultados(resultado));
        }).start();
    }

    @FXML
    public void accionLimpiar() {
        limpiarResultados();
        setEstado("Resultados limpiados");
    }

    // ===================================================================
    // MOSTRAR RESULTADOS EN LOS PANELES
    // ===================================================================

    private void mostrarResultados(ResultadoCompilacion resultado) {
        quitarResaltadoErrores();

        if (resultado.tieneErrores()) {
            int primeraLinea = detectarPrimeraLineaError(resultado);
            if (primeraLinea > 0) resaltarLineaError(primeraLinea);
        }

        mostrarErroresEnPanel(resultado);
        mostrarTokensEnPanel(resultado.getTokens());

        if (resultado.getDocumentacion() != null)
            areaDocTecnica.setText(resultado.getDocumentacion());

        if (resultado.getTokens() != null && resultado.getCodigoFuente() != null)
            mostrarDocDescriptiva(resultado.getTokens(), resultado.getCodigoFuente());

        mostrarSalidaEjecucion(resultado);
        mostrarTablaSimbolos(resultado.getTablaSimbolos());

        if (!resultado.tieneErrores() && resultado.isExitoCompilacion()) {
            setEstado("Compilacion y ejecucion exitosa.");
            tabPaneResultados.getSelectionModel().select(tabSalida);
        } else {
            setEstado("Se detectaron errores.");
            tabPaneResultados.getSelectionModel().select(tabErrores);
        }
    }

    private void mostrarErroresEnPanel(ResultadoCompilacion resultado) {
        StringBuilder sb = new StringBuilder();
        boolean hayErrores = false;

        List<String> errL = resultado.getErroresLexicos();
        if (errL != null && !errL.isEmpty()) {
            hayErrores = true;
            sb.append("=== ERRORES LEXICOS ===\n");
            for (String e : errL) sb.append("  ").append(e).append("\n");
            sb.append("\n");
        }
        List<String> errS = resultado.getErroresSintacticos();
        if (errS != null && !errS.isEmpty()) {
            hayErrores = true;
            sb.append("=== ERRORES SINTACTICOS ===\n");
            for (String e : errS) sb.append("  ").append(e).append("\n");
            sb.append("\n");
        }
        List<String> errSem = resultado.getErroresSemanticos();
        if (errSem != null && !errSem.isEmpty()) {
            hayErrores = true;
            sb.append("=== ERRORES SEMANTICOS ===\n");
            for (String e : errSem) sb.append("  ").append(e).append("\n");
            sb.append("\n");
        }
        List<String> adv = resultado.getAdvertenciasSemanticas();
        if (adv != null && !adv.isEmpty()) {
            sb.append("=== ADVERTENCIAS ===\n");
            for (String a : adv) sb.append("  ").append(a).append("\n");
            sb.append("\n");
        }
        if (!hayErrores && (adv == null || adv.isEmpty()))
            sb.append("No se detectaron errores ni advertencias.\n");

        areaErrores.setText(sb.toString());
    }

    private void mostrarTokensEnPanel(List<Token> tokens) {
        if (tokens == null) { areaTokens.setText("(Sin tokens)"); return; }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s  %-28s  %s%n", "LINEA", "TIPO DE TOKEN", "LEXEMA"));
        sb.append("-".repeat(70)).append("\n");
        for (Token t : tokens) {
            if (t.getTipo() == Token.Tipo.T_FIN_ARCHIVO) continue;
            sb.append(String.format("%-6d  %-28s  '%s'%n",
                t.getLinea(), t.getTipo().name(), t.getLexema()));
        }
        areaTokens.setText(sb.toString());
    }

    private void mostrarDocDescriptiva(List<Token> tokens, String codigoFuente) {
        DocumentadorLinea docLinea = new DocumentadorLinea();
        List<String> lineas = docLinea.documentarPorLinea(tokens, codigoFuente);
        StringBuilder sb = new StringBuilder();
        for (String l : lineas) sb.append(l).append("\n");
        areaDocDescriptiva.setText(sb.toString());
    }

    private void mostrarSalidaEjecucion(ResultadoCompilacion resultado) {
        StringBuilder sb = new StringBuilder();
        sb.append(resultado.isExitoCompilacion()
            ? "=== RESULTADO DE EJECUCION JVM-J ===\n\n"
            : "=== COMPILACION DETENIDA - SIN EJECUCION ===\n\n");
        List<String> salidas = resultado.getSalidasEjecucion();
        if (salidas != null && !salidas.isEmpty())
            for (String s : salidas) sb.append(s).append("\n");
        else if (resultado.isExitoCompilacion())
            sb.append("(El programa no produjo salida en consola)\n");
        areaSalida.setText(sb.toString());
    }

    private void mostrarTablaSimbolos(List<EntradaTablaSimbolos> simbolos) {
        if (simbolos == null) {
            tablaSimbolos.setItems(FXCollections.emptyObservableList());
            return;
        }
        ObservableList<EntradaTablaSimbolos> datos = FXCollections.observableArrayList(simbolos);
        tablaSimbolos.setItems(datos);
    }

    // ===================================================================
    // RESALTADO DE ERRORES EN EL EDITOR
    // ===================================================================

    private int detectarPrimeraLineaError(ResultadoCompilacion resultado) {
        List<List<String>> grupos = List.of(
            resultado.getErroresLexicos()     != null ? resultado.getErroresLexicos()     : List.of(),
            resultado.getErroresSintacticos() != null ? resultado.getErroresSintacticos() : List.of(),
            resultado.getErroresSemanticos()  != null ? resultado.getErroresSemanticos()  : List.of()
        );
        for (List<String> grupo : grupos)
            for (String msg : grupo) {
                int linea = extraerNumeroLinea(msg);
                if (linea > 0) return linea;
            }
        return -1;
    }

    private int extraerNumeroLinea(String mensaje) {
        int idx = mensaje.toLowerCase().indexOf("linea ");
        if (idx < 0) return -1;
        int start = idx + 6;
        StringBuilder num = new StringBuilder();
        while (start < mensaje.length() && Character.isDigit(mensaje.charAt(start)))
            num.append(mensaje.charAt(start++));
        try { return num.length() > 0 ? Integer.parseInt(num.toString()) : -1; }
        catch (NumberFormatException e) { return -1; }
    }

    private void resaltarLineaError(int numLinea) {
        String texto = areaEditor.getText();
        if (texto == null || texto.isEmpty()) return;
        String[] lineas = texto.split("\n", -1);
        if (numLinea > lineas.length) return;
        int posInicio = 0;
        for (int i = 0; i < numLinea - 1; i++) posInicio += lineas[i].length() + 1;
        int posFin = posInicio + lineas[numLinea - 1].length();
        areaEditor.selectRange(posInicio, posFin);
        areaEditor.requestFocus();
        areaEditor.getStyleClass().remove("code-editor-error");
        areaEditor.getStyleClass().add("code-editor-error");
    }

    private void quitarResaltadoErrores() {
        areaEditor.getStyleClass().remove("code-editor-error");
    }

    // ===================================================================
    // UTILIDADES
    // ===================================================================

    private void limpiarResultados() {
        areaSalida.clear(); areaErrores.clear(); areaTokens.clear();
        areaDocTecnica.clear(); areaDocDescriptiva.clear();
        tablaSimbolos.setItems(FXCollections.emptyObservableList());
        quitarResaltadoErrores();
    }

    private void setEstado(String texto) { labelEstado.setText(texto); }

    private void mostrarAlertaError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error - Compilador JODA");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private boolean confirmarDescarte() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cambios sin guardar");
        alert.setHeaderText("Hay cambios sin guardar en el archivo actual.");
        alert.setContentText("Deseas descartar los cambios y continuar?");
        return alert.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }

    private FileChooser crearFileChooser(String titulo) {
        FileChooser fc = new FileChooser();
        fc.setTitle(titulo);
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Archivos JODA (*.joda)", "*.joda"),
            new FileChooser.ExtensionFilter("Todos los archivos (*.*)", "*.*")
        );
        return fc;
    }

    private Window getVentana() { return areaEditor.getScene().getWindow(); }

    private void insertar(String codigo) {
        areaEditor.setText(codigo.isEmpty()
            ?
              "entry {\n" +
              "}\n"
            : codigo);
        modificado = false;
    }
}
