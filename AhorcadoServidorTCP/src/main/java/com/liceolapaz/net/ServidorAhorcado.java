package com.liceolapaz.net;

import com.liceolapaz.net.DAO.JugadorDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServidorAhorcado {
    private static final int PUERTO = 65000;
    private static final BlockingQueue<JugadorPendiente> colaClientes = new LinkedBlockingQueue<>();
    private static volatile boolean partidaEnCurso = false;

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor Ahorcado iniciado en el puerto " + PUERTO);

            new Thread(() -> {
                while (true) {
                    try {
                        if (!partidaEnCurso && !colaClientes.isEmpty()) {
                            procesarSiguiente();
                        }
                        Thread.sleep(200);
                    } catch (Exception e) {
                        System.out.println("Error en el gestor de partidas: " + e.getMessage());
                    }
                }
            }).start();

            while (true) {
                Socket cliente = servidor.accept();
                new Thread(new ManejadorCliente(cliente)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class JugadorPendiente {
        Socket socket;
        String nombre;
        String modo;
        BufferedReader in;
        PrintWriter out;

        JugadorPendiente(Socket socket, String nombre, String modo, BufferedReader in, PrintWriter out) {
            this.socket = socket;
            this.nombre = nombre;
            this.modo = modo.toLowerCase();
            this.in = in;
            this.out = out;
        }
    }

    private static class ManejadorCliente implements Runnable {
        private final Socket socket;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                out.println("SOLICITUD_NOMBRE");
                String nombre = in.readLine();


                if (nombre == null || nombre.equalsIgnoreCase("cancelar")) {
                    socket.close();
                    return;
                }
                JugadorDAO.verificarYCrearJugador(nombre);

                String modo;
                while (true) {
                    out.println("SOLICITUD_MODO_JUEGO");
                     modo = in.readLine();

                    if (modo == null || modo.equalsIgnoreCase("cancelar")) {
                        out.println("PARTIDA_CANCELADA");
                        socket.close();
                        return;
                    }

                    if (modo.equalsIgnoreCase("puntuacion")) {
                        int puntos = JugadorDAO.obtenerPuntuacion(nombre);
                        out.println("PUNTUACION_TOTAL;" + puntos);
                        System.out.println(puntos);
                        continue; // vuelve a preguntar el modo
                    }

                    if (modo.equalsIgnoreCase("solo") || modo.equalsIgnoreCase("esperar")) {
                        break; // ya tenemos un modo válido
                    }

                    out.println("MODO_INVALIDO");
                }


                synchronized (ServidorAhorcado.class) {
                    if (partidaEnCurso) {
                        out.println("PARTIDA_EN_CURSO");
                    }
                }

                JugadorPendiente jugador = new JugadorPendiente(socket, nombre, modo, in, out);

                // Mientras espera, leemos si llega "cancelar"
                // Espera activa controlada antes de añadir a la cola
                new Thread(() -> {
                    try {
                        while (!socket.isClosed() && !jugador.in.ready()) {
                            Thread.sleep(50);
                        }
                        if (!socket.isClosed() && jugador.in.ready()) {
                            String linea = jugador.in.readLine();
                            if (linea != null && linea.equalsIgnoreCase("cancelar")) {
                                System.out.println(nombre + " canceló antes de emparejar.");
                                out.println("PARTIDA_CANCELADA");
                                socket.close();
                            }
                        }
                    } catch (Exception e) {
                       e.printStackTrace();
                    }
                }).start();

                colaClientes.put(jugador);

            } catch (IOException | InterruptedException e) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }


    private static void procesarSiguiente() {
        try {
            // Buscar si hay alguien que quiere jugar solo
            JugadorPendiente jugadorSolo = null;
            int size = colaClientes.size();
            for (int i = 0; i < size; i++) {
                JugadorPendiente j = colaClientes.poll();
                if (j == null || j.socket.isClosed()) continue;

                if (j.modo.equals("solo")) {
                    jugadorSolo = j;
                    break;
                } else {
                    colaClientes.offer(j);
                }
            }

            if (jugadorSolo != null) {
                partidaEnCurso = true;
                jugadorSolo.out.println("¡Empieza tu partida en solitario!");
                JugadorPendiente finalJugadorSolo = jugadorSolo;
                new Thread(() -> {
                    new PartidaAhorcado(finalJugadorSolo.socket, null, finalJugadorSolo.nombre, null).run();
                    partidaEnCurso = false;
                }).start();
                return;
            }

            // Si no hay "solo", intentamos emparejar dos "esperar"
            JugadorPendiente jugador1 = null;
            JugadorPendiente jugador2 = null;

            size = colaClientes.size();
            for (int i = 0; i < size && (jugador1 == null || jugador2 == null); i++) {
                JugadorPendiente j = colaClientes.poll();
                if (j == null || j.socket.isClosed()) continue;

                if (j.modo.equals("esperar")) {
                    if (jugador1 == null) {
                        jugador1 = j;
                    } else {
                        jugador2 = j;
                    }
                } else {
                    colaClientes.offer(j);
                }
            }

            if (jugador1 != null && jugador2 != null) {
                partidaEnCurso = true;
                jugador1.out.println("¡Emparejado con " + jugador2.nombre + "!");
                jugador2.out.println("¡Emparejado con " + jugador1.nombre + "!");
                System.out.println("Iniciando partida entre " + jugador1.nombre + " y " + jugador2.nombre);

                JugadorPendiente finalJugador = jugador1;
                JugadorPendiente finalJugador1 = jugador2;
                new Thread(() -> {
                    new PartidaAhorcado(
                            finalJugador.socket,
                            finalJugador1.socket,
                            finalJugador.nombre,
                            finalJugador1.nombre
                    ).run();
                    partidaEnCurso = false;
                }).start();
            } else {
                // Devolver jugadores a la cola si no se pudo emparejar
                if (jugador1 != null) colaClientes.offer(jugador1);
                if (jugador2 != null) colaClientes.offer(jugador2);
            }

        } catch (Exception e) {
            System.out.println("Error al procesar siguiente cliente: " + e.getMessage());
        }


    }

}
