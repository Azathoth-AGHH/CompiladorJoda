package vista_grafica;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Subscription;

import logica.documentador.DocumentadorLinea;
import logica.lexico.Token;
import logica.nucleo.CompiladorJoda;
import logica.nucleo.ResultadoCompilacion;
import logica.semantico.EntradaTablaSimbolos;

public class Controlador {

    // ===================================================================
    // PALABRAS RESERVADAS JODA
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

    private static final List<String> ALL_KEYWORDS = new ArrayList<>();
    static {
        ALL_KEYWORDS.addAll(KEYWORDS);
        ALL_KEYWORDS.addAll(TYPES);
        ALL_KEYWORDS.addAll(BUILTINS);
        ALL_KEYWORDS.addAll(Arrays.asList("true", "false"));
        Collections.sort(ALL_KEYWORDS);
    }

    // ===================================================================
    // COLORES INLINE
    // ===================================================================
    private static final String COLOR_KEYWORD  = "-fx-fill: #ff7b72; -fx-font-weight: bold;";
    private static final String COLOR_TYPE     = "-fx-fill: #79c0ff; -fx-font-weight: bold;";
    private static final String COLOR_BUILTIN  = "-fx-fill: #d2a8ff; -fx-font-weight: bold;";
    private static final String COLOR_STRING   = "-fx-fill: #a5d6ff;";
    private static final String COLOR_NUMBER   = "-fx-fill: #79c0ff;";
    private static final String COLOR_BOOLEAN  = "-fx-fill: #ff9a3c; -fx-font-weight: bold;";
    private static final String COLOR_COMMENT  = "-fx-fill: #6e7681; -fx-font-style: italic;";
    private static final String COLOR_OPERATOR = "-fx-fill: #ffa657;";
    private static final String COLOR_BRACKET  = "-fx-fill: #e6edf3; -fx-font-weight: bold;";
    private static final String COLOR_DEFAULT  = "-fx-fill: #e6edf3;";

    // ===================================================================
    // PATRON REGEX PARA RESALTADO
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
    private boolean panelLateralVisible              = true;
    private boolean panelInferiorVisible             = true;
    private double  posicionDivisorGuardada          = 0.75;
    private double  posicionDivisorVerticalGuardada  = 0.65;

    // ===================================================================
    // COMPONENTES FXML
    // ===================================================================
    @FXML private HBox   barraTitulo;
    @FXML private Label  labelEstado;
    @FXML private Label  labelArchivo;
    @FXML private Button btnMinimizar, btnMaximizar, btnCerrar;
    @FXML private Button btnToggleLateral, btnToggleInferior;

    @FXML private TextArea areaNumeros;
    @FXML private TextArea areaEditor;
    @FXML private HBox     editorHBox;

    @FXML private SplitPane splitCentral;
    @FXML private SplitPane splitVertical;
    @FXML private VBox      panelLateral;

    @FXML private VBox   contenedorInferior;
    @FXML private Button btnColapsarInferior;

    @FXML private TableView<EntradaTablaSimbolos>           tablaSimbolos;
    @FXML private TableColumn<EntradaTablaSimbolos, String> colNombre, colTipo, colCategoria, colLinea, colValor;

    @FXML private TabPane  tabPaneResultados;
    @FXML private Tab      tabSalida, tabErrores, tabTokens, tabDocTecnica, tabDocDescriptiva;
    @FXML private TextArea areaSalida, areaErrores, areaTokens, areaDocTecnica, areaDocDescriptiva;

    // ===================================================================
    // EDITOR
    // ===================================================================
    private InlineCssTextArea codeArea;

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
    // REEMPLAZAR TextArea POR InlineCssTextArea
    // ===================================================================
    private void reemplazarEditorConCodeArea() {
        areaNumeros.setVisible(false);
        areaNumeros.setManaged(false);

        codeArea = new InlineCssTextArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setWrapText(false);

        codeArea.setStyle(
            "-fx-background-color: #0d1117;" +
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 13.5px;"
        );

        codeArea.getStylesheets().add(
            getClass().getResource("/vista_grafica/editor-joda.css").toExternalForm()
        );
        codeArea.getStylesheets().add(
            getClass().getResource("/vista_grafica/Estilos.css").toExternalForm()
        );

        // Resaltado reactivo: 80ms despues del ultimo cambio
        resaltadoSub = codeArea.multiPlainChanges()
            .successionEnds(java.time.Duration.ofMillis(80))
            .subscribe(cambio -> Platform.runLater(() -> {
                String txt = codeArea.getText();
                if (!txt.isEmpty()) aplicarResaltadoInline(txt);
            }));

        // Marcar como modificado cuando el usuario escribe
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.equals(oldText)) modificado = true;
        });

        codeArea.addEventFilter(KeyEvent.KEY_PRESSED,  this::manejarTeclaPresionada);
        codeArea.addEventFilter(KeyEvent.KEY_RELEASED, this::manejarTeclaLiberada);
        codeArea.addEventFilter(KeyEvent.KEY_TYPED,    this::manejarTeclaTipada);

        HBox.setHgrow(codeArea, Priority.ALWAYS);
        VBox.setVgrow(codeArea, Priority.ALWAYS);
        codeArea.setMaxWidth(Double.MAX_VALUE);
        codeArea.setMaxHeight(Double.MAX_VALUE);

        editorHBox.getChildren().remove(areaEditor);
        editorHBox.getChildren().add(codeArea);
    }

    // ===================================================================
    // RESALTADO DE SINTAXIS INLINE
    // ===================================================================
    private void aplicarResaltadoInline(String texto) {
        codeArea.setStyle(0, texto.length(), COLOR_DEFAULT);

        Matcher m = PATTERN.matcher(texto);
        while (m.find()) {
            String estilo;
            if      (m.group("COMMENT")  != null) estilo = COLOR_COMMENT;
            else if (m.group("STRING")   != null) estilo = COLOR_STRING;
            else if (m.group("KEYWORD")  != null) estilo = COLOR_KEYWORD;
            else if (m.group("TYPE")     != null) estilo = COLOR_TYPE;
            else if (m.group("BUILTIN")  != null) estilo = COLOR_BUILTIN;
            else if (m.group("BOOLEAN")  != null) estilo = COLOR_BOOLEAN;
            else if (m.group("NUMBER")   != null) estilo = COLOR_NUMBER;
            else if (m.group("OPERATOR") != null) estilo = COLOR_OPERATOR;
            else if (m.group("BRACKET")  != null) estilo = COLOR_BRACKET;
            else                                   estilo = COLOR_DEFAULT;

            codeArea.setStyle(m.start(), m.end(), estilo);
        }
    }

    // ===================================================================
    // TECLADO INTELIGENTE
    // ===================================================================
    private void manejarTeclaPresionada(KeyEvent e) {

        if (autocompletePopup != null && autocompletePopup.isShowing()) {
            switch (e.getCode()) {
                case ESCAPE:
                    ocultarPopup();
                    e.consume();
                    return;
                case DOWN:
                    int bajar = autocompleteList.getSelectionModel().getSelectedIndex();
                    int maxBajar = autocompleteList.getItems().size() - 1;
                    if (bajar < maxBajar) {
                        autocompleteList.getSelectionModel().select(bajar + 1);
                        autocompleteList.scrollTo(bajar + 1);
                    }
                    e.consume();
                    return;
                case UP:
                    int subir = autocompleteList.getSelectionModel().getSelectedIndex();
                    if (subir > 0) {
                        autocompleteList.getSelectionModel().select(subir - 1);
                        autocompleteList.scrollTo(subir - 1);
                    }
                    e.consume();
                    return;
                case TAB:
                    e.consume();
                    return;
                case ENTER:
                    String selEnter = autocompleteList.getSelectionModel().getSelectedItem();
                    if (selEnter != null) {
                        e.consume();
                        aplicarAutocompletar(selEnter);
                        return;
                    }
                    break;
                default:
                    break;
            }
        }

        if (e.getCode() == KeyCode.ESCAPE) {
            ocultarPopup();
            return;
        }

        if (e.getCode() == KeyCode.ENTER) {
            manejarEnter(e);
            return;
        }

        if (e.getCode() == KeyCode.TAB) {
            codeArea.insertText(codeArea.getCaretPosition(), "    ");
            e.consume();
        }
    }

    private void manejarTeclaLiberada(KeyEvent e) {
        if (e.getCode() == KeyCode.TAB
                && autocompletePopup != null
                && autocompletePopup.isShowing()) {
            String sel = autocompleteList.getSelectionModel().getSelectedItem();
            if (sel != null) aplicarAutocompletar(sel);
            e.consume();
        }
    }

    private void manejarTeclaTipada(KeyEvent e) {
        switch (e.getCharacter()) {
            case "{":
                e.consume();
                insertarPareja("{", "}");
                break;
            case "(":
                e.consume();
                insertarPareja("(", ")");
                break;
            case "[":
                e.consume();
                insertarPareja("[", "]");
                break;
            case "\"":
                e.consume();
                insertarPareja("\"", "\"");
                break;
            default:
                break;
        }
        Platform.runLater(this::actualizarPopupAutocompletado);
    }

    private void insertarPareja(String apertura, String cierre) {
        int pos = codeArea.getCaretPosition();
        codeArea.insertText(pos, apertura + cierre);
        codeArea.moveTo(pos + 1);
    }

    private void manejarEnter(KeyEvent e) {
        e.consume();

        int    pos     = codeArea.getCaretPosition();
        int    parr    = codeArea.getCurrentParagraph();
        String linea   = codeArea.getParagraph(parr).getText();
        String trimmed = linea.stripTrailing();

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

        autocompleteList.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                String sel = autocompleteList.getSelectionModel().getSelectedItem();
                if (sel != null) aplicarAutocompletar(sel);
            }
        });

        autocompleteList.setOnMousePressed(ev -> {
            String sel = autocompleteList.getSelectionModel().getSelectedItem();
            if (sel != null) aplicarAutocompletar(sel);
        });
    }

    private void actualizarPopupAutocompletado() {
        String parcial = palabraActual();
        if (parcial == null || parcial.length() < 2) {
            ocultarPopup();
            return;
        }

        List<String> sugerencias = filtrar(parcial);
        if (sugerencias.isEmpty() ||
            (sugerencias.size() == 1 && sugerencias.get(0).equals(parcial))) {
            ocultarPopup();
            return;
        }

        autocompleteList.getItems().setAll(sugerencias);
        autocompleteList.getSelectionModel().selectFirst();
        autocompleteList.scrollTo(0);

        Optional<Bounds> bo = codeArea.getCaretBounds();
        if (bo.isPresent() && stage != null) {
            Bounds b = bo.get();
            autocompletePopup.show(stage, b.getMinX(), b.getMaxY() + 2);
            codeArea.requestFocus();
        }
    }

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

    private void aplicarAutocompletar(String seleccion) {
        ocultarPopup();
        String parcial = palabraActual();
        if (parcial == null) parcial = "";

        int pos    = codeArea.getCaretPosition();
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

    private String snippet(String p) {
        switch (p) {
            case "entry":  return "entry {\n    $CURSOR$\n\t}";
            case "if":     return "if ($CURSOR$) {\n    \n\t}";
            case "else":   return "else {\n    $CURSOR$\n\t}";
            case "loop":   return "loop ($CURSOR$) {\n    \n\t}";
            case "select": return "select ($CURSOR$) {\n    case : \n\t}";
            case "define": return "define $CURSOR$";
            case "out":    return "out($CURSOR$);";
            case "input":  return "input($CURSOR$);";
            case "object": return "object $CURSOR$ {\n    \n\t}";
            case "method": return "method $CURSOR$() {\n    \n\t}";
            case "return": return "return $CURSOR$;";
            default:       return p;
        }
    }

    private void ocultarPopup() {
        if (autocompletePopup != null && autocompletePopup.isShowing())
            autocompletePopup.hide();
    }

    // ===================================================================
    // ACCESO AL TEXTO
    // ===================================================================
    private String getText() {
        return codeArea != null ? codeArea.getText() : "";
    }

    private void setText(String texto) {
        if (codeArea == null) return;
        codeArea.replaceText(texto);
        Platform.runLater(() -> {
            String t = codeArea.getText();
            if (!t.isEmpty()) aplicarResaltadoInline(t);
        });
    }

    private void cargarCodigoInicial() {
        String codigoDemo =
            "\n// Programa de ejemplo JODA\n" +
            "entry {\n" +
            "}\n";

        codeArea.replaceText(codigoDemo);
        modificado = false;

        codeArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    String t = codeArea.getText();
                    if (!t.isEmpty()) aplicarResaltadoInline(t);
                    modificado = false;
                });
            }
        });

        if (codeArea.getScene() != null) {
            Platform.runLater(() -> {
                String t = codeArea.getText();
                if (!t.isEmpty()) aplicarResaltadoInline(t);
                modificado = false;
            });
        }
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
    @FXML public void accionCerrar() {
        if (modificado && !confirmarDescarte()) return;
        stage.close();
    }

    @FXML public void accionMinimizar() {
        stage.setIconified(true);
    }

    @FXML public void accionMaximizar() {
        boolean max = stage.isMaximized();
        stage.setMaximized(!max);
        btnMaximizar.setText(max ? "□" : "❐");
    }

    @FXML public void onTitleBarPressed(MouseEvent e) {
        if (stage.isMaximized()) {
            stage.setMaximized(false);
            btnMaximizar.setText("□");
        }
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
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            resizeDireccion = "";
            scene.setCursor(Cursor.DEFAULT);
        });
    }

    private String calcDireccion(double x, double y, double w, double h) {
        boolean n=y<BORDE_RESIZE, s=y>h-BORDE_RESIZE, o=x<BORDE_RESIZE, este=x>w-BORDE_RESIZE;
        if (y<42 && !n) return "";
        if (n&&o) return "NO"; if (n&&este) return "NE"; if (s&&o) return "SO"; if (s&&este) return "SE";
        if (n) return "N"; if (s) return "S"; if (este) return "E"; if (o) return "O";
        return "";
    }

    private Cursor cursorDir(String d) {
        switch(d) {
            case "N":  return Cursor.N_RESIZE;  case "S":  return Cursor.S_RESIZE;
            case "E":  return Cursor.E_RESIZE;  case "O":  return Cursor.W_RESIZE;
            case "NE": return Cursor.NE_RESIZE; case "NO": return Cursor.NW_RESIZE;
            case "SE": return Cursor.SE_RESIZE; case "SO": return Cursor.SW_RESIZE;
            default:   return Cursor.DEFAULT;
        }
    }

    // ===================================================================
    // PANELES COLAPSABLES
    // ===================================================================
    @FXML
    public void togglePanelLateral() {
        if (panelLateralVisible) {
            posicionDivisorGuardada = splitCentral.getDividerPositions()[0];
            animarDivisor(posicionDivisorGuardada, 1.0, true);
            btnToggleLateral.setText("▶ Tabla");
            panelLateralVisible = false;
        } else {
            animarDivisor(splitCentral.getDividerPositions()[0],
                          posicionDivisorGuardada, false);
            btnToggleLateral.setText("◀ Tabla");
            panelLateralVisible = true;
        }
    }

    private void animarDivisor(double desde, double hasta, boolean col) {
        SplitPane.Divider div = splitCentral.getDividers().get(0);
        Timeline tl = new Timeline(
            new KeyFrame(javafx.util.Duration.ZERO,
                new KeyValue(div.positionProperty(), desde)),
            new KeyFrame(javafx.util.Duration.millis(200),
                new KeyValue(div.positionProperty(), hasta))
        );
        tl.setOnFinished(ev -> {
            panelLateral.setVisible(!col);
            panelLateral.setManaged(!col);
            if (!col) div.setPosition(posicionDivisorGuardada);
        });
        tl.play();
    }

    @FXML
    public void togglePanelInferior() {
        if (splitVertical == null) return;

        if (panelInferiorVisible) {
            posicionDivisorVerticalGuardada = splitVertical.getDividerPositions()[0];
            animarDivisorVertical(posicionDivisorVerticalGuardada, 0.98);
            btnToggleInferior.setText("▴ Panel");
            btnColapsarInferior.setText("▴");
            panelInferiorVisible = false;
            Platform.runLater(() -> {
                tabPaneResultados.setVisible(false);
                tabPaneResultados.setManaged(false);
            });
        } else {
            animarDivisorVertical(splitVertical.getDividerPositions()[0],
                                  posicionDivisorVerticalGuardada);
            btnToggleInferior.setText("▾ Panel");
            btnColapsarInferior.setText("▾");
            panelInferiorVisible = true;
            tabPaneResultados.setVisible(true);
            tabPaneResultados.setManaged(true);
        }
    }

    private void animarDivisorVertical(double desde, double hasta) {
        SplitPane.Divider div = splitVertical.getDividers().get(0);
        Timeline tl = new Timeline(
            new KeyFrame(javafx.util.Duration.ZERO,
                new KeyValue(div.positionProperty(), desde)),
            new KeyFrame(javafx.util.Duration.millis(220),
                new KeyValue(div.positionProperty(), hasta))
        );
        tl.play();
    }

    // ===================================================================
    // ACCIONES DE ARCHIVO
    // ===================================================================
    @FXML public void accionNuevo() {
        if (modificado && !confirmarDescarte()) return;
        setText("// Nuevo archivo JODA\nentry {\n    \n}\n");
        archivoActual = null;
        modificado = false;
        limpiarResultados();
        setEstado("Nuevo archivo");
        labelArchivo.setText("Sin archivo");
    }

    @FXML public void accionAbrir() {
        if (modificado && !confirmarDescarte()) return;
        File f = crearFC("Abrir archivo JODA").showOpenDialog(getVentana());
        if (f == null) return;
        try {
            setText(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            archivoActual = f;
            modificado = false;
            limpiarResultados();
            setEstado("Abierto: " + f.getName());
            labelArchivo.setText(f.getAbsolutePath());
        } catch (IOException ex) {
            mostrarError("No se pudo abrir:\n" + ex.getMessage());
        }
    }

    @FXML public void accionGuardar() {
        if (archivoActual == null) { accionGuardarComo(); return; }
        guardar(archivoActual);
    }

    @FXML public void accionGuardarComo() {
        File f = crearFC("Guardar archivo JODA").showSaveDialog(getVentana());
        if (f == null) return;
        if (!f.getName().endsWith(".joda")) f = new File(f.getAbsolutePath() + ".joda");
        guardar(f);
        archivoActual = f;
        labelArchivo.setText(f.getAbsolutePath());
    }

    private void guardar(File f) {
        try (FileWriter fw = new FileWriter(f, StandardCharsets.UTF_8)) {
            fw.write(getText());
            modificado = false;
            setEstado("Guardado: " + f.getName());
        } catch (IOException ex) {
            mostrarError("No se pudo guardar:\n" + ex.getMessage());
        }
    }

    // ===================================================================
    // COMPILAR Y EJECUTAR
    // ===================================================================
    @FXML
    public void accionCompilar() {
        String codigo = getText();
        if (codigo == null || codigo.trim().isEmpty()) {
            mostrarError("El editor esta vacio.");
            return;
        }
        setEstado("Compilando...");
        limpiarResultados();
        if (!panelInferiorVisible) togglePanelInferior();

        // Siempre crear archivo temporal con el contenido ACTUAL del editor
        File arc;
        try {
            arc = File.createTempFile("joda_", ".joda");
            arc.deleteOnExit();
            try (FileWriter fw = new FileWriter(arc, StandardCharsets.UTF_8)) {
                fw.write(codigo);
            }
        } catch (IOException ex) {
            mostrarError("Error archivo temporal:\n" + ex.getMessage());
            return;
        }

        final File archivoFinal = arc;

        new Thread(() -> {
            CompiladorJoda compilador = new CompiladorJoda();

            compilador.setInputCallback(prompt -> {
                String[] resultado = {""};
                java.util.concurrent.CountDownLatch latch =
                    new java.util.concurrent.CountDownLatch(1);

                Platform.runLater(() -> {
                    TextInputDialog dialogo = new TextInputDialog();
                    dialogo.setTitle("Entrada de datos - JVM-J");
                    dialogo.setHeaderText(null);
                    dialogo.setGraphic(null);

                    DialogPane pane = dialogo.getDialogPane();
                    pane.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #161b22, #0d1117);" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 20;" +
                        "-fx-text-fill: #e6edf3;"
                    );

                    TextField txt = dialogo.getEditor();
                    txt.setStyle(
                        "-fx-background-color: #0d1117;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-prompt-text-fill: #6e7681;" +
                        "-fx-highlight-fill: #264f78;" +
                        "-fx-highlight-text-fill: white;" +
                        "-fx-border-color: #388bfd;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-family: 'Consolas';" +
                        "-fx-font-size: 15px;" +
                        "-fx-padding: 10;"
                    );

                    Label tituloInput = new Label("Ingresa el valor para: " + prompt);
                    tituloInput.setStyle(
                        "-fx-text-fill: #e6edf3;" +
                        "-fx-font-size: 17px;" +
                        "-fx-font-family: 'Consolas';"
                    );

                    VBox contenido = new VBox(15);
                    contenido.getChildren().addAll(tituloInput, txt);
                    pane.setContent(contenido);

                    Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
                    okBtn.setText("Aceptar");
                    okBtn.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #2ea043, #238636);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-padding: 8 18 8 18;" +
                        "-fx-cursor: hand;"
                    );

                    Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
                    cancelBtn.setText("Cancelar");
                    cancelBtn.setStyle(
                        "-fx-background-color: #21262d;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 8 18 8 18;" +
                        "-fx-cursor: hand;"
                    );

                    pane.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);

                    java.util.Optional<String> respuesta = dialogo.showAndWait();
                    resultado[0] = respuesta.orElse("0");
                    latch.countDown();
                });

                try {
                    latch.await();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                return resultado[0];
            });

            ResultadoCompilacion res =
                compilador.compilarYEjecutar(archivoFinal.getAbsolutePath());
            Platform.runLater(() -> mostrarResultados(res));
        }).start();
    }

    @FXML public void accionLimpiar() {
        limpiarResultados();
        setEstado("Resultados limpiados");
    }

    // ===================================================================
    // MOSTRAR RESULTADOS
    // ===================================================================
    private void mostrarResultados(ResultadoCompilacion r) {
        variablesDeclaradas.clear();
        if (r.getTablaSimbolos() != null)
            for (EntradaTablaSimbolos e : r.getTablaSimbolos())
                if (!variablesDeclaradas.contains(e.getNombre()))
                    variablesDeclaradas.add(e.getNombre());

        if (r.tieneErrores()) {
            int linea = detectarPrimeraLinea(r);
            if (linea > 0) resaltarLinea(linea);
        }

        // ---- ERRORES Y ADVERTENCIAS ----
        StringBuilder sb = new StringBuilder();
        boolean hay = false;
        List<String> eL = r.getErroresLexicos();
        if (eL != null && !eL.isEmpty()) {
            hay = true;
            sb.append("=== ERRORES LEXICOS ===\n");
            for (String e : eL) sb.append("  ").append(e).append("\n");
            sb.append("\n");
        }
        List<String> eS = r.getErroresSintacticos();
        if (eS != null && !eS.isEmpty()) {
            hay = true;
            sb.append("=== ERRORES SINTACTICOS ===\n");
            for (String e : eS) sb.append("  ").append(e).append("\n");
            sb.append("\n");
        }
        List<String> eSem = r.getErroresSemanticos();
        if (eSem != null && !eSem.isEmpty()) {
            hay = true;
            sb.append("=== ERRORES SEMANTICOS ===\n");
            for (String e : eSem) sb.append("  ").append(e).append("\n");
            sb.append("\n");
        }
        List<String> adv = r.getAdvertenciasSemanticas();
        if (adv != null && !adv.isEmpty()) {
            sb.append("=== ADVERTENCIAS ===\n");
            for (String a : adv) sb.append("  ").append(a).append("\n");
            sb.append("\n");
        }
        if (!hay && (adv == null || adv.isEmpty()))
            sb.append("No se detectaron errores ni advertencias.\n");
        areaErrores.setText(sb.toString());

        // ---- DETERMINAR FASE DE ERROR ----
        boolean hayErrorLexico     = eL != null && !eL.isEmpty();
        boolean hayErrorSintactico = eS != null && !eS.isEmpty();

        List<String> todosLosErrores = new ArrayList<>();
        if (hayErrorLexico)     todosLosErrores.addAll(eL);
        if (hayErrorSintactico) todosLosErrores.addAll(eS);

        if (hayErrorLexico || hayErrorSintactico) {

            StringBuilder ts = new StringBuilder();
            ts.append(String.format("%-6s  %-28s  %s%n", "LINEA", "TIPO DE TOKEN", "LEXEMA"));
            ts.append("-".repeat(70)).append("\n");
            for (String err : todosLosErrores) {
                int lineaErr = numLinea(err);
                String lineaStr = lineaErr > 0 ? String.valueOf(lineaErr) : "?";
                ts.append(String.format("%-6s  %-28s  %s%n", lineaStr, "*** ERROR ***", err));
            }
            areaTokens.setText(ts.toString());

            StringBuilder dt = new StringBuilder();
            dt.append("=== DOCUMENTACION TECNICA ===\n");
            dt.append("Narrativa descriptiva del flujo de tokens identificados:\n\n");
            for (String err : todosLosErrores) {
                int lineaErr = numLinea(err);
                dt.append("\n--- Linea ").append(lineaErr > 0 ? lineaErr : "?").append(" ---\n");
                dt.append("  [*** ERROR ***                  ] ");
                dt.append("Lexema: '").append(err, 0, Math.min(err.length(), 20)).append("'");
                dt.append(" -> ").append(err).append("\n");
            }
            dt.append("\n=== FIN DE DOCUMENTACION ===\n");
            areaDocTecnica.setText(dt.toString());

            if (hayErrorSintactico) {
                areaDocDescriptiva.setText(
                    "=== DOCUMENTACION DESCRIPTIVA NO DISPONIBLE ===\n\n" +
                    "Se encontraron errores sintacticos. La documentacion\n" +
                    "descriptiva requiere un AST valido para generarse.\n\n" +
                    "=== ERRORES SINTACTICOS DETECTADOS ===\n" +
                    String.join("\n", eS));
            }

        } else {

            if (r.getTokens() != null && !r.getTokens().isEmpty()) {
                StringBuilder ts = new StringBuilder();
                ts.append(String.format("%-6s  %-28s  %s%n", "LINEA", "TIPO DE TOKEN", "LEXEMA"));
                ts.append("-".repeat(70)).append("\n");
                for (Token t : r.getTokens()) {
                    if (t.getTipo() == Token.Tipo.T_FIN_ARCHIVO) continue;
                    ts.append(String.format("%-6d  %-28s  '%s'%n",
                        t.getLinea(), t.getTipo().name(), t.getLexema()));
                }
                areaTokens.setText(ts.toString());
            }

            if (r.getDocumentacion() != null && !r.getDocumentacion().isEmpty()) {
                areaDocTecnica.setText(r.getDocumentacion());
            }

            if (r.getTokens() != null && r.getCodigoFuente() != null) {
                DocumentadorLinea dl = new DocumentadorLinea();
                List<String> lineas = dl.documentarPorLinea(r.getTokens(), r.getCodigoFuente());
                StringBuilder ds = new StringBuilder();
                for (String l : lineas) ds.append(l).append("\n");
                areaDocDescriptiva.setText(ds.toString());
            }
        }

        // ---- SALIDA DE EJECUCION ----
        StringBuilder out = new StringBuilder();
        out.append(r.isExitoCompilacion()
            ? "=== RESULTADO DE EJECUCION JVM-J ===\n\n"
            : "=== COMPILACION DETENIDA ===\n\n");
        List<String> sal = r.getSalidasEjecucion();
        if (sal != null && !sal.isEmpty())
            for (String s : sal) out.append(s).append("\n");
        else if (r.isExitoCompilacion())
            out.append("(Sin salida en consola)\n");
        areaSalida.setText(out.toString());

        // ---- TABLA DE SIMBOLOS ----
        if (r.getTablaSimbolos() == null)
            tablaSimbolos.setItems(FXCollections.emptyObservableList());
        else
            tablaSimbolos.setItems(FXCollections.observableArrayList(r.getTablaSimbolos()));

        // ---- FOCO DE PESTANA ----
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
            for (String msg : g) { int l = numLinea(msg); if (l > 0) return l; }
        }
        return -1;
    }

    private int numLinea(String msg) {
        int idx = msg.toLowerCase().indexOf("linea ");
        if (idx < 0) return -1;
        int s = idx + 6;
        StringBuilder n = new StringBuilder();
        while (s < msg.length() && Character.isDigit(msg.charAt(s))) n.append(msg.charAt(s++));
        try { return n.length() > 0 ? Integer.parseInt(n.toString()) : -1; }
        catch (NumberFormatException ex) { return -1; }
    }

    private void resaltarLinea(int num) {
        String txt = getText();
        if (txt == null || txt.isEmpty()) return;
        String[] ls = txt.split("\n", -1);
        if (num > ls.length) return;
        int ini = 0;
        for (int i = 0; i < num - 1; i++) ini += ls[i].length() + 1;
        codeArea.selectRange(ini, ini + ls[num - 1].length());
        codeArea.requestFocus();
    }

    // ===================================================================
    // UTILIDADES
    // ===================================================================
    private void limpiarResultados() {
        areaSalida.clear();
        areaErrores.clear();
        areaTokens.clear();
        areaDocTecnica.clear();
        areaDocDescriptiva.clear();
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
        return a.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }

    private FileChooser crearFC(String titulo) {
        FileChooser fc = new FileChooser();
        fc.setTitle(titulo);
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Archivos JODA (*.joda)", "*.joda"),
            new FileChooser.ExtensionFilter("Todos los archivos (*.*)", "*.*")
        );
        return fc;
    }

    private Window getVentana() { return codeArea.getScene().getWindow(); }
}