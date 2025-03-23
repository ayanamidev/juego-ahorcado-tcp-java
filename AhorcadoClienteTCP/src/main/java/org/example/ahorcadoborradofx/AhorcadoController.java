package org.example.ahorcadoborradofx;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.Socket;
import java.util.Optional;

public class AhorcadoController {

    // Configuración de conexión
    private static final String SERVIDOR = "localhost";
    private static final int PUERTO = 65000;
    public Label contadorLabel;
    public Label saliendo;
    public VBox pantallaSaliendo;
    public ProgressIndicator ruedaCarga;
    public Label mensajeSaliendo;
    public Button botonPuntuacion;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private boolean esPartidaSolo = false;
    private String oponente;
    private Timeline contadorTiempo;

    @FXML private VBox contenedorPrincipal;

    @FXML private VBox pantallaNombre, pantallaModo, pantallaJuego;

    @FXML private TextField nombreField;
    @FXML private Label estadoLabel;
    @FXML private Button botonEnviarNombre;

    @FXML private Label modoLabel;
    @FXML private Button botonSolo, botonEsperar;

    @FXML private Label palabraLabel, intentosLabel, turnoLabel, estado, feedback, oponenteLabel, puntuacionTotal;
    @FXML private TextField letraField;
    @FXML private Button botonCancelar, botonReconectar, enviar;


    @FXML
    public void initialize() {
        conectarServidor();
        mostrarSolo(pantallaNombre);
    }

    // ---------------------- Manejo de botones ----------------------

    @FXML
    public void onEnviarNombre() {
        String nombre = nombreField.getText().trim();
        if (!nombre.isEmpty()) {
            enviar(nombre);
            nombreField.setDisable(true);
            botonEnviarNombre.setDisable(true);
        }
    }

    @FXML
    public void onJugarSolo() {
        enviar("solo");
        deshabilitarModo();
        esPartidaSolo = true;
    }

    @FXML
    public void onEsperar() {
        enviar("esperar");
        deshabilitarModo();
        esPartidaSolo = false;
        modoLabel.setText("Esperando a otro jugador...");

    }

    @FXML
    public void onCancelar() {
        enviar("cancelar");
        cerrarConexion();
        resetUI();
        conectarServidor();
    }

    @FXML
    public void onCancelar2() {
        enviar("cancelar");
    }

    @FXML
    public void onReconectar() {
        cerrarConexion();
        resetUI();
        estadoLabel.setText("Reconectando...");
        conectarServidor();
        mostrarSolo(pantallaNombre);
    }

    public void enviarLetra(ActionEvent event) {
        String letra = letraField.getText().trim();
        if (!letra.isEmpty()) {
            enviar(letra);
            letraField.clear();
        }
    }

    public void mostrarPuntuacion(ActionEvent event) {
        enviar("puntuacion");
    }

    public void salir(ActionEvent event) {
        System.exit(0);
    }

    public void setStage(Stage stage) {
        stage.setOnCloseRequest(event -> {
            Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
            alerta.setTitle("Confirmación");
            alerta.setHeaderText("¿Estás seguro de que quieres cancelar la partida?");
            alerta.setContentText("Perderás tu progreso actual.");
            Optional<ButtonType> resultado = alerta.showAndWait();
            if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                enviar("cancelar");
            } else {
                event.consume();
            }
        });
    }

    // ---------------------- Comunicación ----------------------

    private void conectarServidor() {
        new Thread(() -> {
            try {
                socket = new Socket(SERVIDOR, PUERTO);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String mensaje;
                while ((mensaje = in.readLine()) != null) {
                    String finalMensaje = mensaje;
                    Platform.runLater(() -> {
                        try {
                            procesarMensaje(finalMensaje);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

            } catch (IOException e) {
                Platform.runLater(() -> estadoLabel.setText("No se pudo conectar con el servidor."));
            }
        }).start();
    }

    private void enviar(String mensaje) {
        if (out != null) {
            out.println(mensaje);
        }
    }

    private void cerrarConexion() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    // ---------------------- UI Utils ----------------------

    private void mostrarSolo(Node nodoVisible) {
        for (Node node : contenedorPrincipal.getChildren()) {
            node.setVisible(false);
            node.setManaged(false);
        }
        nodoVisible.setVisible(true);
        nodoVisible.setManaged(true);
    }

    private void ocultarNodos(Node... nodos) {
        for (Node nodo : nodos) {
            nodo.setVisible(false);
            nodo.setManaged(false);
        }
    }

    private void activarNodos(Node... nodos) {
        for (Node nodo : nodos) {
            nodo.setVisible(true);
            nodo.setManaged(true);
        }
    }

    private void deshabilitarModo() {
        botonSolo.setDisable(true);
        botonEsperar.setDisable(true);
        botonPuntuacion.setDisable(true);

    }

    private void resetUI() {
        nombreField.setDisable(false);
        nombreField.clear();
        botonEnviarNombre.setDisable(false);
        botonSolo.setDisable(false);
        botonEsperar.setDisable(false);
        botonPuntuacion.setDisable(false);


        esPartidaSolo = false;

        ocultarNodos(oponenteLabel, palabraLabel, intentosLabel, turnoLabel, letraField, feedback, puntuacionTotal);
        estado.setText("");
    }

    // ---------------------- Procesamiento de mensajes ----------------------

    private void procesarMensaje(String mensaje) throws InterruptedException {
        if (mensaje.startsWith("SOLICITUD_NOMBRE")) {
            estadoLabel.setText("Introduce tu nombre:");
            mostrarSolo(pantallaNombre);

        } else if (mensaje.startsWith("SOLICITUD_MODO_JUEGO")) {
            modoLabel.setText("¿Quieres jugar solo o esperar a otro jugador?");
            mostrarSolo(pantallaModo);
            activarNodos(botonSolo, botonEsperar);

        } else if (mensaje.startsWith("PARTIDA_EN_CURSO")) {
            modoLabel.setText("Actualmente hay una partida en curso. Espera...");
            esPartidaSolo = true;
            mostrarSolo(pantallaModo);

        } else if (mensaje.startsWith("PARTIDA_INICIADA")) {
            iniciarPartida(false, mensaje);
            turnoLabel.setText("Es tu turno.");
            activarNodos(turnoLabel);

        } else if (mensaje.startsWith("PARTIDA_SOLO_INICIADA")) {
            iniciarPartida(true, mensaje);

        } else if (mensaje.startsWith("ESTADO")) {
            String[] partes = mensaje.split(";");
            palabraLabel.setText(partes[1].split(":")[1]);
            intentosLabel.setText("Intentos restantes: " + partes[2].split(":")[1]);
            activarNodos(palabraLabel, intentosLabel);
            mostrarSolo(pantallaJuego);

        } else if (mensaje.startsWith("TURNO")) {
            if (!esPartidaSolo){
                turnoLabel.setText("Es tu turno.");
                enviar.setDisable(false);

            }
            iniciarContador();
            activarNodos(letraField, botonCancelar, contadorLabel);
            letraField.setDisable(false);
            letraField.requestFocus();
            letraField.applyCss();
            letraField.layout();

        } else if (mensaje.startsWith("ESPERA_TURNO")) {
            turnoLabel.setText("Espera tu turno...");

            if (contadorTiempo != null) {
                contadorTiempo.stop();
            }

            ocultarNodos(contadorLabel, botonCancelar);
            enviar.setDisable(true);
            letraField.setDisable(true);
            letraField.applyCss();
            letraField.layout();

        } else if (mensaje.startsWith("SOLICITUD_LETRA")) {
            activarNodos(letraField);

            letraField.applyCss();
            letraField.layout();

        } else if (mensaje.startsWith("ACIERTO")) {
            feedback.setText("¡Correcto!");
            activarNodos(feedback);

        } else if (mensaje.startsWith("FALLO")) {
            feedback.setText("Incorrecto.");
            activarNodos(feedback);

        } else if (mensaje.startsWith("PALABRA")) {
            palabraLabel.setText(mensaje.split(";")[1]);
            activarNodos(palabraLabel);

        } else if (mensaje.startsWith("INTENTOS_RESTANTES")) {
            intentosLabel.setText("Intentos restantes: " + mensaje.split(":")[1]);
            activarNodos(intentosLabel);

        } else if (mensaje.startsWith("FIN_PARTIDA")) {
            mostrarFinPartida(mensaje);

        } else if (mensaje.startsWith("ENTRADA_INVALIDA")) {
            estado.setText("Entrada inválida, intenta otra letra.");
            activarNodos(estado);

        } else if (mensaje.startsWith("JUGADOR_DESCONECTADO") || mensaje.startsWith("PARTIDA_CANCELADA")) {
            mostrarFinAbrupto(mensaje);

        } else if (mensaje.startsWith("PUNTUACION_TOTAL")) {
            puntuacionTotal.setText("Puntuación Total: "+mensaje.split(";")[1]);
            activarNodos(puntuacionTotal);
        }
    }

    private void iniciarPartida(boolean solo, String mensaje) {
        if (!solo) {
            oponente = mensaje.split(":")[1];
            oponenteLabel.setText("Oponente: " + oponente);
            activarNodos(oponenteLabel);
        }
        estado.setText("Ingresa una letra: ");
        activarNodos(estado, enviar, botonCancelar);
        mostrarSolo(pantallaJuego);
    }

    private void mostrarFinPartida(String mensaje) {
        if (mensaje.contains("GANADOR")) {
            mensajeSaliendo.setText("¡Has ganado! La palabra es: " + mensaje.split("PALABRA:")[1]);
        } else if (mensaje.contains("PERDEDOR")) {
            mensajeSaliendo.setText("Has perdido. La palabra es: " + mensaje.split("PALABRA:")[1]);
        } else {
            mensajeSaliendo.setText("Ninguno adivinó, la palabra es: " + mensaje.split("PALABRA:")[1]);
        }

        activarNodos(mensajeSaliendo, saliendo);
        mostrarCarga(true);
        mostrarSolo(pantallaSaliendo);
        ocultarNodos(letraField, enviar, intentosLabel, turnoLabel, palabraLabel, feedback, botonCancelar);

        PauseTransition pausa = new PauseTransition(Duration.seconds(5));
        pausa.setOnFinished(e -> {
            mostrarCarga(false);
            cerrarConexion();
            resetUI();
            conectarServidor();
        });
        pausa.play();
    }

    private void mostrarFinAbrupto(String mensaje) {
        if (mensaje.startsWith("JUGADOR_DESCONECTADO")) {
            mensajeSaliendo.setText("Un jugador se ha desconectado de la partida.");
        } else {
            mensajeSaliendo.setText("Se ha cancelado la partida.");
        }
        activarNodos(mensajeSaliendo,saliendo);
        mostrarCarga(true);
        mostrarSolo(pantallaSaliendo);
        ocultarNodos(oponenteLabel,letraField, enviar, intentosLabel, turnoLabel, palabraLabel, estado, feedback, botonCancelar, contadorLabel);


        PauseTransition pausa = new PauseTransition(Duration.seconds(5));

        pausa.setOnFinished(e -> {
            mostrarCarga(false);
            cerrarConexion();
            resetUI();
            conectarServidor();
        });
        pausa.play();
    }
    private void iniciarContador() {
        if (contadorTiempo != null) {
            contadorTiempo.stop();
        }

        contadorLabel.setText("15");
        activarNodos(contadorLabel);

        contadorTiempo = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    int segundosRestantes = Integer.parseInt(contadorLabel.getText()) - 1;
                    contadorLabel.setText(String.valueOf(segundosRestantes));
                    contadorLabel.setVisible(true);
                    contadorLabel.setManaged(true);


                })
        );
        contadorTiempo.setCycleCount(15);
        contadorTiempo.play();
    }
    private void mostrarCarga(boolean mostrar) {
        ruedaCarga.setVisible(mostrar);
        ruedaCarga.setManaged(mostrar);
    }


}
