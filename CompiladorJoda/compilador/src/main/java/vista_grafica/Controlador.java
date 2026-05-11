package vista_grafica;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import logica.documentador.DocumentadorLinea;
import logica.lexico.Token;
import logica.nucleo.CompiladorJoda;
import logica.nucleo.ResultadoCompilacion;
import logica.semantico.EntradaTablaSimbolos;

/**
 * Controlador principal del Compilador JODA v2.2.
 *
 * Compatible con VistaPrincipal.fxml ORIGINAL (sin modificarlo).
 * En initialize() se reemplaza el TextArea del editor por un CodeArea
 * de RichTextFX dentro del mismo HBox (editorHBox), manteniendo el
 * TextArea de numeros de linea oculto (RichTextFX provee los suyos).
 *
 * Caracteristicas del editor inteligente:
 *   - Resaltado de sintaxis en tiempo real (keywords, tipos, strings, numeros, etc.)
 *   - Indentacion automatica al presionar Enter (detecta apertura de bloque con '{')
 *   - Cierre automatico de pares: {} () [] ""
 *   - Autocompletado con popup: keywords JODA + variables declaradas
 *   - Snippets de codigo para palabras clave estructurales
 *   - Tab inserta 4 espacios
 */
public class Controlador {

    // ===================================================================
    // PALABRAS RESERVADAS JODA PARA RESALTADO
    // ===================================================================
    private static final List<String> KEYWORDS = Arrays.asList(
        "entry", "if", "else", "loop", "select", "case", "return", "new"
    );
    private static final List<String> TYPES = Arrays.asList(
        "int", "dec", "string", "bool", "void"
    );
    private static final List<String> BUILTINS = Arrays.asList(
        "define", "out", "input", "object", "method"
    );

    // Lista unificada para autocompletado
    private static final List<String> ALL_KEYWORDS = new ArrayList<>();
    static {
        ALL_KEYWORDS.addAll(KEYWORDS);
        ALL_KEYWORDS.addAll(TYPES);
        ALL_KEYWORDS.addAll(BUILTINS);
        ALL_KEYWORDS.addAll(Arrays.asList("true", "false"));
        Collections.sort(ALL_KEYWORDS);
    }

    // ===================================================================
    // PATRON REGEX PARA RESALTADO
    // Orden importa: COMMENT y STRING primero para no colorear su interior
    // ===================================================================
    private static final Pattern PATTERN;
    static {
        String kw  = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
        String typ = "\\b(" + String.join("|", TYPES)    + ")\\b";
        String blt = "\\b(" + String.join("|", BUILTINS) + ")\\b";
        String bol = "\\b(true|false)\\b";
        String str = "\"([^\"\\\\]|\\\\.)*\"";
        String num = "\\b\\d+(\\.\\d+)?\\b";
        String com = "//[^\n]*";
        String op  = "(\\+\\+|--|==|!=|>=|<=|&&|\\|\\||[+\\-*/%=!<>&|])";
        String br  = "[{}()\\[\\]]";

        PATTERN = Pattern.compile(
            "(?<COMMENT>"   + com + ")"
          + "|(?<STRING>"   + str + ")"
          + "|(?<KEYWORD>"  + kw  + ")"
          + "|(?<TYPE>"     + typ + ")"
          + "|(?<BUILTIN>"  + blt + ")"
          + "|(?<BOOLEAN>"  + bol + ")"
          + "|(?<NUMBER>"   + num + ")"
          + "|(?<OPERATOR>" + op  + ")"
          + "|(?<BRACKET>"  + br  + ")"
        );
    }

    // ===================================================================
    // VENTANA
    // ===================================================================
    private Stage  stage;
    private double dragOffsetX, dragOffsetY;
    private static final int BORDE_RESIZE = 6;
    private double resizeInicioX, resizeInicioY, resizeInicioW, resizeInicioH;
    private double resizeInicioStageX, resizeInicioStageY;
    private String resizeDireccion = "";

    // ===================================================================
    // PANELES
    // ===================================================================
    private boolean panelLateralVisible  = true;
    private boolean panelInferiorVisible = true;
    private double  posicionDivisorGuardada = 0.75;
    private double  altoInferiorGuardado    = 260;
    private static final double ALTO_CABECERA = 30;

    // ===================================================================
    // COMPONENTES FXML
    // Los TextArea areaNumeros y areaEditor se declaran @FXML para que
    // JavaFX los inyecte, pero el editor (areaEditor) sera reemplazado
    // en initialize() por el CodeArea de RichTextFX.
    // ===================================================================
    @FXML private HBox   barraTitulo;
    @FXML private Label  labelEstado;
    @FXML private Label  labelArchivo;
    @FXML private Button btnMinimizar, btnMaximizar, btnCerrar;
    @FXML private Button btnToggleLateral, btnToggleInferior;

    @FXML private TextArea areaNumeros;   // Se oculta; RichTextFX pone sus propios numeros
    @FXML private TextArea areaEditor;    // Se reemplaza en initialize() por el CodeArea
    @FXML private HBox     editorHBox;    // Contenedor padre de areaNumeros + areaEditor

    @FXML private SplitPane splitCentral;
    @FXML private VBox      panelLateral;

    @FXML private VBox   contenedorInferior;
    @FXML private Button btnColapsarInferior;

    @FXML private TableView<EntradaTablaSimbolos>           tablaSimbolos;
    @FXML private TableColumn<EntradaTablaSimbolos, String> colNombre, colTipo, colCategoria, colLinea, colValor;

    @FXML private TabPane  tabPaneResultados;
    @FXML private Tab      tabSalida, tabErrores, tabTokens, tabDocTecnica, tabDocDescriptiva;
    @FXML private TextArea areaSalida, areaErrores, areaTokens, areaDocTecnica, areaDocDescriptiva;

    // ===================================================================
    // EDITOR RICHTEXTFX
    // ===================================================================
    private CodeArea     codeArea;
    @SuppressWarnings("unused")
    private Subscription resaltadoSub;

    // Popup de autocompletado
    private Popup            autocompletePopup;
    private ListView<String> autocompleteList;

    // ===================================================================
    // ESTADO
    // ===================================================================
    private File               archivoActual       = null;
    private boolean            modificado          = false;
    private final List<String> variablesDeclaradas = new ArrayList<>();

    // ===================================================================
    // INICIALIZACION
    // ===================================================================
    public void setStage(Stage stage) {
        this.stage = stage;
        stage.showingProperty().addListener((obs, was, isShowing) -> {
            if (isShowing) configurarResizePorBordes();
        });
    }

    @FXML
    public void initialize() {
        configurarTablaSimbolos();
        reemplazarEditorConCodeArea();
        configurarAutocompletado();
        cargarCodigoInicial();
        setEstado("Listo");
    }

    // ===================================================================
    // REEMPLAZAR TextArea POR CodeArea DE RICHTEXTFX
    // ===================================================================
    /**
     * Elimina el TextArea original del editor del HBox y lo sustituye
     * por un CodeArea de RichTextFX con numeros de linea integrados.
     * El TextArea de numeros (areaNumeros) se oculta porque RichTextFX
     * provee los suyos propios a traves de LineNumberFactory.
     */
    private void reemplazarEditorConCodeArea() {
        areaNumeros.setVisible(false);
        areaNumeros.setManaged(false);

        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        // Aplicar CSS al CodeArea directamente
        codeArea.getStylesheets().add(
            getClass().getResource("/vista_grafica/editor-joda.css").toExternalForm()
        );
        codeArea.getStyleClass().add("code-area");

        // CLAVE: también agregarlo a la escena cuando esté disponible
        codeArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && !newScene.getStylesheets().contains(
                    getClass().getResource("/vista_grafica/editor-joda.css").toExternalForm())) {
                newScene.getStylesheets().add(
                    getClass().getResource("/vista_grafica/editor-joda.css").toExternalForm()
                );
            }
        });

    resaltadoSub = codeArea.multiPlainChanges()
        .successionEnds(java.time.Duration.ofMillis(150))
        .subscribe(cambio -> Platform.runLater(() -> {
            String txt = codeArea.getText();
            if (!txt.isEmpty()) {
                codeArea.setStyleSpans(0, calcularEstilos(txt));
            }
        }));

    codeArea.textProperty().addListener((obs, o, n) -> modificado = true);
    codeArea.addEventFilter(KeyEvent.KEY_PRESSED, this::manejarTeclaPresionada);
    codeArea.addEventFilter(KeyEvent.KEY_TYPED,   this::manejarTeclaTipada);

    HBox.setHgrow(codeArea, Priority.ALWAYS);
    VBox.setVgrow(codeArea, Priority.ALWAYS);
    codeArea.setMaxWidth(Double.MAX_VALUE);
    codeArea.setMaxHeight(Double.MAX_VALUE);

    editorHBox.getChildren().remove(areaEditor);
    editorHBox.getChildren().add(codeArea);
}


    // ===================================================================
    // CALCULAR ESTILOS (resaltado de sintaxis)
    // ===================================================================
    private StyleSpans<Collection<String>> calcularEstilos(String texto) {
        Matcher m = PATTERN.matcher(texto);
        StyleSpansBuilder<Collection<String>> sb = new StyleSpansBuilder<>();
        int ultimo = 0;

        while (m.find()) {
            String cls;
            if      (m.group("COMMENT")  != null) cls = "comment";
            else if (m.group("STRING")   != null) cls = "string";
            else if (m.group("KEYWORD")  != null) cls = "keyword";
            else if (m.group("TYPE")     != null) cls = "type";
            else if (m.group("BUILTIN")  != null) cls = "builtin";
            else if (m.group("BOOLEAN")  != null) cls = "boolean";
            else if (m.group("NUMBER")   != null) cls = "number";
            else if (m.group("OPERATOR") != null) cls = "operator";
            else if (m.group("BRACKET")  != null) cls = "bracket";
            else                                   cls = null;

            sb.add(Collections.emptyList(), m.start() - ultimo);
            sb.add(cls != null ? Collections.singleton(cls) : Collections.emptyList(),
                   m.end() - m.start());
            ultimo = m.end();
        }
        sb.add(Collections.emptyList(), texto.length() - ultimo);
        return sb.create();
    }

    // ===================================================================
    // TECLADO INTELIGENTE
    // ===================================================================

    /** KEY_PRESSED: Enter con indentacion, Tab, navegacion del popup. */
    private void manejarTeclaPresionada(KeyEvent e) {

        // Escape cierra el popup
        if (e.getCode() == KeyCode.ESCAPE) {
            ocultarPopup();
            return;
        }

        // Navegacion en el popup con flechas y confirmacion con Enter/Tab
        if (autocompletePopup != null && autocompletePopup.isShowing()) {
            if (e.getCode() == KeyCode.DOWN) {
                int i = autocompleteList.getSelectionModel().getSelectedIndex();
                autocompleteList.getSelectionModel().select(i + 1);
                e.consume(); return;
            }
            if (e.getCode() == KeyCode.UP) {
                int i = autocompleteList.getSelectionModel().getSelectedIndex();
                autocompleteList.getSelectionModel().select(Math.max(0, i - 1));
                e.consume(); return;
            }
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.TAB) {
                String sel = autocompleteList.getSelectionModel().getSelectedItem();
                if (sel != null) { aplicarAutocompletar(sel); e.consume(); return; }
            }
        }

        // Enter: indentacion automatica
        if (e.getCode() == KeyCode.ENTER) {
            manejarEnter(e);
            return;
        }

        // Tab: insertar 4 espacios
        if (e.getCode() == KeyCode.TAB) {
            codeArea.insertText(codeArea.getCaretPosition(), "    ");
            e.consume();
        }
    }

    /** KEY_TYPED: cierre automatico de pares y trigger del autocompletado. */
    private void manejarTeclaTipada(KeyEvent e) {
        switch (e.getCharacter()) {
            case "{":  insertarCierre("}");  e.consume(); break;
            case "(":  insertarCierre(")");  e.consume(); break;
            case "[":  insertarCierre("]");  e.consume(); break;
            case "\"": insertarCierre("\""); e.consume(); break;
        }
        Platform.runLater(this::actualizarPopupAutocompletado);
    }

    /**
     * Inserta el caracter de cierre y deja el cursor entre ambos caracteres.
     */
    private void insertarCierre(String cierre) {
        int pos = codeArea.getCaretPosition();
        codeArea.insertText(pos, cierre);
        codeArea.moveTo(pos);
    }

    /**
     * Calcula la indentacion de la linea actual y la replica en la nueva linea.
     * Si la linea termina en '{' agrega 4 espacios adicionales.
     */
    private void manejarEnter(KeyEvent e) {
        e.consume();

        int    pos     = codeArea.getCaretPosition();
        int    parr    = codeArea.getCurrentParagraph();
        String linea   = codeArea.getParagraph(parr).getText();
        String trimmed = linea.stripTrailing();

        // Contar espacios de indentacion al inicio
        int espacios = 0;
        for (char c : linea.toCharArray()) {
            if      (c == ' ')  espacios++;
            else if (c == '\t') espacios += 4;
            else break;
        }

        String base  = " ".repeat(espacios);
        String extra = trimmed.endsWith("{") ? "    " : "";

        codeArea.insertText(pos, "\n" + base + extra);
    }

    // ===================================================================
    // AUTOCOMPLETADO
    // ===================================================================
    private void configurarAutocompletado() {
        autocompleteList  = new ListView<>();
        autocompletePopup = new Popup();
        autocompletePopup.setAutoHide(true);
        autocompletePopup.setHideOnEscape(true);

        VBox caja = new VBox(autocompleteList);
        caja.getStyleClass().add("autocomplete-popup");
        autocompletePopup.getContent().add(caja);

        autocompleteList.setPrefWidth(230);
        autocompleteList.setMaxHeight(180);

        // Click doble confirma la seleccion
        autocompleteList.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                String sel = autocompleteList.getSelectionModel().getSelectedItem();
                if (sel != null) aplicarAutocompletar(sel);
            }
        });
    }

    /** Calcula sugerencias y muestra el popup debajo del cursor. */
    private void actualizarPopupAutocompletado() {
        String parcial = palabraActual();
        if (parcial == null || parcial.length() < 2) { ocultarPopup(); return; }

        List<String> sugerencias = filtrar(parcial);
        if (sugerencias.isEmpty() ||
            (sugerencias.size() == 1 && sugerencias.get(0).equals(parcial))) {
            ocultarPopup(); return;
        }

        autocompleteList.getItems().setAll(sugerencias);
        autocompleteList.getSelectionModel().selectFirst();

        Optional<Bounds> bo = codeArea.getCaretBounds();
        if (bo.isPresent() && stage != null) {
            Bounds b = bo.get();
            autocompletePopup.show(stage, b.getMinX(), b.getMaxY() + 2);
        }
    }

    /** Texto que se esta escribiendo justo antes del cursor. */
    private String palabraActual() {
        int pos = codeArea.getCaretPosition();
        if (pos == 0) return null;
        String txt = codeArea.getText();
        int ini = pos - 1;
        while (ini >= 0 && (Character.isLetterOrDigit(txt.charAt(ini)) || txt.charAt(ini) == '_'))
            ini--;
        ini++;
        return (ini >= pos) ? null : txt.substring(ini, pos);
    }

    /** Filtra keywords y variables que comienzan con el prefijo dado. */
    private List<String> filtrar(String prefijo) {
        String pre = prefijo.toLowerCase();
        List<String> res = new ArrayList<>();
        for (String kw : ALL_KEYWORDS)
            if (kw.startsWith(pre) && !kw.equals(prefijo)) res.add(kw);
        for (String var : variablesDeclaradas)
            if (var.startsWith(pre) && !res.contains(var) && !var.equals(prefijo)) res.add(var);
        Collections.sort(res);
        return res;
    }

    /**
     * Reemplaza la palabra parcial con el snippet de la seleccion.
     * El marcador $CURSOR$ indica donde debe quedar el cursor.
     */
    private void aplicarAutocompletar(String seleccion) {
        ocultarPopup();
        String parcial = palabraActual();
        if (parcial == null) parcial = "";

        int pos   = codeArea.getCaretPosition();
        int inicio = pos - parcial.length();
        String snippet = snippet(seleccion);

        codeArea.replaceText(inicio, pos, snippet);

        int marcador = snippet.indexOf("$CURSOR$");
        if (marcador >= 0) {
            String limpio = snippet.replace("$CURSOR$", "");
            codeArea.replaceText(inicio, inicio + snippet.length(), limpio);
            codeArea.moveTo(inicio + marcador);
        }
    }

    /** Genera snippets de codigo para palabras clave estructurales. */
    private String snippet(String p) {
        switch (p) {
            case "entry":  return "entry {\n    $CURSOR$\n}";
            case "if":     return "if ($CURSOR$) {\n    \n}";
            case "else":   return "else {\n    $CURSOR$\n}";
            case "loop":   return "loop ($CURSOR$) {\n    \n}";
            case "select": return "select ($CURSOR$) {\n    case : \n}";
            case "define": return "define $CURSOR$";
            case "out":    return "out($CURSOR$);";
            case "input":  return "input($CURSOR$);";
            case "object": return "object $CURSOR$ {\n    \n}";
            case "method": return "method $CURSOR$() {\n    \n}";
            case "return": return "return $CURSOR$;";
            default:       return p;
        }
    }

    private void ocultarPopup() {
        if (autocompletePopup != null && autocompletePopup.isShowing())
            autocompletePopup.hide();
    }

    // ===================================================================
    // ACCESO AL TEXTO DEL EDITOR
    // ===================================================================
    private String getText() {
        return codeArea != null ? codeArea.getText() : "";
    }

    private void setText(String texto) {
        if (codeArea == null) return;
        codeArea.replaceText(texto);
        Platform.runLater(() -> {
            String t = codeArea.getText();
            if (!t.isEmpty()) codeArea.setStyleSpans(0, calcularEstilos(t));
        });
    }


    private void cargarCodigoInicial() {
        String codigoDemo = "// Programa de ejemplo JODA\nentry {\n    define int a = 2;\n    out(\"Hola\" + a);\n}\n";
        codeArea.replaceText(codigoDemo);
        modificado = false;

    // Forzar resaltado después de que la escena esté lista
        Platform.runLater(() -> Platform.runLater(() -> {
            String t = codeArea.getText();
            if (!t.isEmpty()) {
                codeArea.setStyleSpans(0, calcularEstilos(t));
            }
        }));
}

    // ===================================================================
    // TABLA DE SIMBOLOS
    // ===================================================================
    private void configurarTablaSimbolos() {
        colNombre   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colTipo     .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTipoDato().name()));
        colCategoria.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoria().name()));
        colLinea    .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getLineaDeclaracion())));
        colValor    .setCellValueFactory(d -> {
            Object v = d.getValue().getValor();
            return new SimpleStringProperty(v != null ? v.toString() : "(null)");
        });
    }

    // ===================================================================
    // CONTROL DE VENTANA
    // ===================================================================
    @FXML public void accionCerrar()    { if (modificado && !confirmarDescarte()) return; stage.close(); }
    @FXML public void accionMinimizar() { stage.setIconified(true); }
    @FXML public void accionMaximizar() {
        boolean max = stage.isMaximized();
        stage.setMaximized(!max);
        btnMaximizar.setText(max ? "□" : "❐");
    }

    @FXML public void onTitleBarPressed(MouseEvent e) {
        if (stage.isMaximized()) { stage.setMaximized(false); btnMaximizar.setText("□"); }
        dragOffsetX = e.getScreenX() - stage.getX();
        dragOffsetY = e.getScreenY() - stage.getY();
    }
    @FXML public void onTitleBarDragged(MouseEvent e) {
        if (!stage.isMaximized()) {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        }
    }

    // ===================================================================
    // RESIZE
    // ===================================================================
    private void configurarResizePorBordes() {
        Scene scene = stage.getScene();
        if (scene == null) return;
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            resizeDireccion = calcDireccion(e.getX(), e.getY(), scene.getWidth(), scene.getHeight());
            scene.setCursor(cursorDir(resizeDireccion));
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!resizeDireccion.isEmpty()) {
                resizeInicioX = e.getScreenX(); resizeInicioY = e.getScreenY();
                resizeInicioW = stage.getWidth(); resizeInicioH = stage.getHeight();
                resizeInicioStageX = stage.getX(); resizeInicioStageY = stage.getY();
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (resizeDireccion.isEmpty()) return;
            double dx = e.getScreenX()-resizeInicioX, dy = e.getScreenY()-resizeInicioY;
            double mW = stage.getMinWidth(), mH = stage.getMinHeight();
            double nx = resizeInicioStageX, ny = resizeInicioStageY;
            double nw = resizeInicioW, nh = resizeInicioH;
            if (resizeDireccion.contains("E")) nw = Math.max(mW, resizeInicioW+dx);
            if (resizeDireccion.contains("O")) { double cw=resizeInicioW-dx; if(cw>=mW){nw=cw;nx=resizeInicioStageX+dx;} }
            if (resizeDireccion.contains("S")) nh = Math.max(mH, resizeInicioH+dy);
            if (resizeDireccion.contains("N")) { double ch=resizeInicioH-dy; if(ch>=mH){nh=ch;ny=resizeInicioStageY+dy;} }
            stage.setX(nx); stage.setY(ny); stage.setWidth(nw); stage.setHeight(nh);
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> { resizeDireccion=""; scene.setCursor(Cursor.DEFAULT); });
    }

    private String calcDireccion(double x, double y, double w, double h) {
        boolean n=y<BORDE_RESIZE, s=y>h-BORDE_RESIZE, o=x<BORDE_RESIZE, este=x>w-BORDE_RESIZE;
        if (y<42 && !n) return "";
        if (n&&o) return "NO"; if (n&&este) return "NE"; if (s&&o) return "SO"; if (s&&este) return "SE";
        if (n) return "N"; if (s) return "S"; if (este) return "E"; if (o) return "O";
        return "";
    }
    private Cursor cursorDir(String d) {
        switch(d){case "N":return Cursor.N_RESIZE;case "S":return Cursor.S_RESIZE;
        case "E":return Cursor.E_RESIZE;case "O":return Cursor.W_RESIZE;
        case "NE":return Cursor.NE_RESIZE;case "NO":return Cursor.NW_RESIZE;
        case "SE":return Cursor.SE_RESIZE;case "SO":return Cursor.SW_RESIZE;
        default:return Cursor.DEFAULT;}
    }

    // ===================================================================
    // PANELES COLAPSABLES
    // ===================================================================
    @FXML public void togglePanelLateral() {
        if (panelLateralVisible) {
            posicionDivisorGuardada = splitCentral.getDividerPositions()[0];
            animarDivisor(posicionDivisorGuardada, 1.0, true);
            btnToggleLateral.setText("▶ Tabla"); panelLateralVisible = false;
        } else {
            animarDivisor(splitCentral.getDividerPositions()[0], posicionDivisorGuardada, false);
            btnToggleLateral.setText("◀ Tabla"); panelLateralVisible = true;
        }
    }

    private void animarDivisor(double desde, double hasta, boolean col) {
        SplitPane.Divider div = splitCentral.getDividers().get(0);
        Timeline tl = new Timeline(
            new KeyFrame(javafx.util.Duration.ZERO,        new KeyValue(div.positionProperty(), desde)),
            new KeyFrame(javafx.util.Duration.millis(200), new KeyValue(div.positionProperty(), hasta))
        );
        tl.setOnFinished(ev -> {
            panelLateral.setVisible(!col); panelLateral.setManaged(!col);
            if (!col) div.setPosition(posicionDivisorGuardada);
        });
        tl.play();
    }

    @FXML public void togglePanelInferior() {
        if (panelInferiorVisible) {
            altoInferiorGuardado = contenedorInferior.getHeight();
            if (altoInferiorGuardado < 80) altoInferiorGuardado = 260;
            animarAltoPanel(altoInferiorGuardado, ALTO_CABECERA, true);
            btnToggleInferior.setText("▴ Panel"); btnColapsarInferior.setText("▴"); panelInferiorVisible = false;
        } else {
            animarAltoPanel(ALTO_CABECERA, altoInferiorGuardado, false);
            btnToggleInferior.setText("▾ Panel"); btnColapsarInferior.setText("▾"); panelInferiorVisible = true;
        }
    }

    private void animarAltoPanel(double desde, double hasta, boolean col) {
        contenedorInferior.setPrefHeight(desde); contenedorInferior.setMaxHeight(desde);
        if (!col) { tabPaneResultados.setVisible(true); tabPaneResultados.setManaged(true); }
        Timeline tl = new Timeline(
            new KeyFrame(javafx.util.Duration.ZERO,
                new KeyValue(contenedorInferior.prefHeightProperty(), desde),
                new KeyValue(contenedorInferior.maxHeightProperty(),  desde)),
            new KeyFrame(javafx.util.Duration.millis(220),
                new KeyValue(contenedorInferior.prefHeightProperty(), hasta),
                new KeyValue(contenedorInferior.maxHeightProperty(),  hasta))
        );
        tl.setOnFinished(ev -> {
            if (col) {
                tabPaneResultados.setVisible(false); tabPaneResultados.setManaged(false);
                contenedorInferior.setPrefHeight(ALTO_CABECERA); contenedorInferior.setMaxHeight(ALTO_CABECERA);
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
    @FXML public void accionNuevo() {
        if (modificado && !confirmarDescarte()) return;
        setText("// Programa de ejemplo JODA\nentry {\n    \n}\n");
        archivoActual = null; modificado = false;
        limpiarResultados(); setEstado("Nuevo archivo"); labelArchivo.setText("Sin archivo");
    }

    @FXML public void accionAbrir() {
        if (modificado && !confirmarDescarte()) return;
        File f = crearFC("Abrir archivo JODA").showOpenDialog(getVentana());
        if (f == null) return;
        try {
            setText(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            archivoActual = f; modificado = false;
            limpiarResultados();
            setEstado("Abierto: " + f.getName()); labelArchivo.setText(f.getAbsolutePath());
        } catch (IOException ex) { mostrarError("No se pudo abrir:\n" + ex.getMessage()); }
    }

    @FXML public void accionGuardar() { if (archivoActual==null){accionGuardarComo();return;} guardar(archivoActual); }

    @FXML public void accionGuardarComo() {
        File f = crearFC("Guardar archivo JODA").showSaveDialog(getVentana());
        if (f == null) return;
        if (!f.getName().endsWith(".joda")) f = new File(f.getAbsolutePath()+".joda");
        guardar(f); archivoActual = f; labelArchivo.setText(f.getAbsolutePath());
    }

    private void guardar(File f) {
        try (FileWriter fw = new FileWriter(f, StandardCharsets.UTF_8)) {
            fw.write(getText()); modificado = false; setEstado("Guardado: "+f.getName());
        } catch (IOException ex) { mostrarError("No se pudo guardar:\n"+ex.getMessage()); }
    }

    // ===================================================================
    // COMPILAR Y EJECUTAR
    // ===================================================================
    @FXML public void accionCompilar() {
        String codigo = getText();
        if (codigo == null || codigo.trim().isEmpty()) {
            mostrarError("El editor esta vacio."); return;
        }
        setEstado("Compilando...");
        limpiarResultados();
        if (!panelInferiorVisible) togglePanelInferior();

        File arc = archivoActual;
        if (arc == null || modificado) {
            try {
                arc = File.createTempFile("joda_",".joda");
                arc.deleteOnExit();
                try (FileWriter fw = new FileWriter(arc, StandardCharsets.UTF_8)) { fw.write(codigo); }
            } catch (IOException ex) { mostrarError("Error archivo temporal:\n"+ex.getMessage()); return; }
        }

        final File archivoFinal = arc;
        new Thread(() -> {
            ResultadoCompilacion res = new CompiladorJoda().compilarYEjecutar(archivoFinal.getAbsolutePath());
            Platform.runLater(() -> mostrarResultados(res));
        }).start();
    }

    @FXML public void accionLimpiar() { limpiarResultados(); setEstado("Resultados limpiados"); }

    // ===================================================================
    // MOSTRAR RESULTADOS
    // ===================================================================
    private void mostrarResultados(ResultadoCompilacion r) {
        // Actualizar variables declaradas para autocompletado
        variablesDeclaradas.clear();
        if (r.getTablaSimbolos() != null)
            for (EntradaTablaSimbolos e : r.getTablaSimbolos())
                if (!variablesDeclaradas.contains(e.getNombre()))
                    variablesDeclaradas.add(e.getNombre());

        if (r.tieneErrores()) {
            int linea = detectarPrimeraLinea(r);
            if (linea > 0) resaltarLinea(linea);
        }

        // Panel de errores
        StringBuilder sb = new StringBuilder();
        boolean hay = false;
        List<String> eL = r.getErroresLexicos();
        if (eL!=null&&!eL.isEmpty()){hay=true;sb.append("=== ERRORES LEXICOS ===\n");for(String e:eL)sb.append("  ").append(e).append("\n");sb.append("\n");}
        List<String> eS = r.getErroresSintacticos();
        if (eS!=null&&!eS.isEmpty()){hay=true;sb.append("=== ERRORES SINTACTICOS ===\n");for(String e:eS)sb.append("  ").append(e).append("\n");sb.append("\n");}
        List<String> eSem = r.getErroresSemanticos();
        if (eSem!=null&&!eSem.isEmpty()){hay=true;sb.append("=== ERRORES SEMANTICOS ===\n");for(String e:eSem)sb.append("  ").append(e).append("\n");sb.append("\n");}
        List<String> adv = r.getAdvertenciasSemanticas();
        if (adv!=null&&!adv.isEmpty()){sb.append("=== ADVERTENCIAS ===\n");for(String a:adv)sb.append("  ").append(a).append("\n");sb.append("\n");}
        if (!hay&&(adv==null||adv.isEmpty())) sb.append("No se detectaron errores ni advertencias.\n");
        areaErrores.setText(sb.toString());

        // Tokens
        if (r.getTokens() != null) {
            StringBuilder ts = new StringBuilder();
            ts.append(String.format("%-6s  %-28s  %s%n","LINEA","TIPO DE TOKEN","LEXEMA"));
            ts.append("-".repeat(70)).append("\n");
            for (Token t : r.getTokens()) {
                if (t.getTipo()==Token.Tipo.T_FIN_ARCHIVO) continue;
                ts.append(String.format("%-6d  %-28s  '%s'%n",t.getLinea(),t.getTipo().name(),t.getLexema()));
            }
            areaTokens.setText(ts.toString());
        }

        if (r.getDocumentacion()!=null) areaDocTecnica.setText(r.getDocumentacion());
        if (r.getTokens()!=null && r.getCodigoFuente()!=null) {
            DocumentadorLinea dl = new DocumentadorLinea();
            List<String> lineas = dl.documentarPorLinea(r.getTokens(), r.getCodigoFuente());
            StringBuilder ds = new StringBuilder();
            for (String l : lineas) ds.append(l).append("\n");
            areaDocDescriptiva.setText(ds.toString());
        }

        // Salida de ejecucion
        StringBuilder out = new StringBuilder();
        out.append(r.isExitoCompilacion() ? "=== RESULTADO DE EJECUCION JVM-J ===\n\n" : "=== COMPILACION DETENIDA ===\n\n");
        List<String> sal = r.getSalidasEjecucion();
        if (sal!=null&&!sal.isEmpty()) for(String s:sal) out.append(s).append("\n");
        else if (r.isExitoCompilacion()) out.append("(Sin salida en consola)\n");
        areaSalida.setText(out.toString());

        // Tabla de simbolos
        if (r.getTablaSimbolos()==null) tablaSimbolos.setItems(FXCollections.emptyObservableList());
        else tablaSimbolos.setItems(FXCollections.observableArrayList(r.getTablaSimbolos()));

        if (!r.tieneErrores() && r.isExitoCompilacion()) {
            setEstado("Compilacion y ejecucion exitosa.");
            tabPaneResultados.getSelectionModel().select(tabSalida);
        } else {
            setEstado("Se detectaron errores.");
            tabPaneResultados.getSelectionModel().select(tabErrores);
        }
    }

    // ===================================================================
    // RESALTADO DE LINEA CON ERROR
    // ===================================================================
    private int detectarPrimeraLinea(ResultadoCompilacion r) {
        for (List<String> g : Arrays.asList(
            r.getErroresLexicos()     != null ? r.getErroresLexicos()     : Collections.<String>emptyList(),
            r.getErroresSintacticos() != null ? r.getErroresSintacticos() : Collections.<String>emptyList(),
            r.getErroresSemanticos()  != null ? r.getErroresSemanticos()  : Collections.<String>emptyList()
        )) {
            for (String msg : g) { int l = numLinea(msg); if (l>0) return l; }
        }
        return -1;
    }


    
    private int numLinea(String msg) {
        int idx = msg.toLowerCase().indexOf("linea ");
        if (idx<0) return -1;
        int s = idx+6;
        StringBuilder n = new StringBuilder();
        while (s<msg.length()&&Character.isDigit(msg.charAt(s))) n.append(msg.charAt(s++));
        try { return n.length()>0 ? Integer.parseInt(n.toString()) : -1; }
        catch (NumberFormatException ex) { return -1; }
    }

    private void resaltarLinea(int num) {
        String txt = getText();
        if (txt==null||txt.isEmpty()) return;
        String[] ls = txt.split("\n",-1);
        if (num>ls.length) return;
        int ini = 0;
        for (int i=0;i<num-1;i++) ini += ls[i].length()+1;
        codeArea.selectRange(ini, ini+ls[num-1].length());
        codeArea.requestFocus();
    }

    // ===================================================================
    // UTILIDADES
    // ===================================================================
    private void limpiarResultados() {
        areaSalida.clear(); areaErrores.clear(); areaTokens.clear();
        areaDocTecnica.clear(); areaDocDescriptiva.clear();
        tablaSimbolos.setItems(FXCollections.emptyObservableList());
    }

    private void setEstado(String t) { labelEstado.setText(t); }

    private void mostrarError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error - Compilador JODA");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean confirmarDescarte() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Cambios sin guardar");
        a.setHeaderText("Hay cambios sin guardar.");
        a.setContentText("¿Deseas descartar los cambios y continuar?");
        return a.showAndWait().filter(r -> r==ButtonType.OK).isPresent();
    }

    private FileChooser crearFC(String titulo) {
        FileChooser fc = new FileChooser();
        fc.setTitle(titulo);
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Archivos JODA (*.joda)","*.joda"),
            new FileChooser.ExtensionFilter("Todos los archivos (*.*)","*.*")
        );
        return fc;
    }

    private Window getVentana() { return codeArea.getScene().getWindow(); }
}