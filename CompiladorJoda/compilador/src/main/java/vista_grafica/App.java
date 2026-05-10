package vista_grafica;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Punto de entrada de la aplicacion JavaFX del Compilador JODA.
 * Usa StageStyle.UNDECORATED para quitar la barra de titulo del SO
 * y mostrar la barra de titulo personalizada del FXML.
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/vista_grafica/VistaPrincipal.fxml")
        );
        Parent root = loader.load();

        // Pasar el Stage al controlador ANTES de mostrar la ventana
        Controlador controlador = loader.getController();
        controlador.setStage(primaryStage);

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(
            getClass().getResource("/vista_grafica/estilos.css").toExternalForm()
        );

        // Sin decoraciones nativas del sistema operativo
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("Compilador JODA v2.0");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
