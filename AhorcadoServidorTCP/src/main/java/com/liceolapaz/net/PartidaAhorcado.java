package com.liceolapaz.net;

import com.liceolapaz.net.DAO.JugadorDAO;
import com.liceolapaz.net.DAO.JugadorPartidaDAO;
import com.liceolapaz.net.DAO.PalabraDAO;
import com.liceolapaz.net.DAO.PartidaDAO;
import com.liceolapaz.net.entidades.Jugador;
import com.liceolapaz.net.entidades.Partida;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PartidaAhorcado implements Runnable {
    private final Socket jugador1, jugador2;
    private final String nombreJ1, nombreJ2;
    private final String palabra;
    private final char[] progreso;
    private int intentosJ1, intentosJ2;
    private int timeoutsJ1 = 0, timeoutsJ2 = 0;
    private PrintWriter salida1, salida2;

    private final BlockingQueue<String> colaJ1 = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> colaJ2 = new LinkedBlockingQueue<>();

    private boolean partidaFinalizada = false;

    public PartidaAhorcado(Socket jugador1, Socket jugador2, String nombreJ1, String nombreJ2) {
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.nombreJ1 = nombreJ1;
        this.nombreJ2 = nombreJ2;

        this.palabra = PalabraDAO.obtenerPalabra();
        System.out.println(palabra);
        this.progreso = new char[palabra.length()];
        Arrays.fill(progreso, '_');

        int maxIntentos = palabra.length() / 2;
        this.intentosJ1 = maxIntentos;
        this.intentosJ2 = maxIntentos;
    }

    @Override
    public void run() {
        try {
            BufferedReader in1 = new BufferedReader(new InputStreamReader(jugador1.getInputStream()));
            salida1 = new PrintWriter(jugador1.getOutputStream(), true);
            new Thread(() -> escucharJugador(in1, colaJ1, nombreJ1)).start();

            BufferedReader in2;
            if (jugador2 != null) {
                in2 = new BufferedReader(new InputStreamReader(jugador2.getInputStream()));
                salida2 = new PrintWriter(jugador2.getOutputStream(), true);
                new Thread(() -> escucharJugador(in2, colaJ2, nombreJ2)).start();
            } else {
                in2 = null;
            }

            boolean turnoJugador1 = jugador2 == null || new Random().nextBoolean();

            if (jugador2 == null) {
                salida1.println("PARTIDA_SOLO_INICIADA");
                salida1.println("ESTADO;PALABRA:" + String.valueOf(progreso) + ";INTENTOS_RESTANTES:" + intentosJ1);
            } else {
                salida1.println("PARTIDA_INICIADA;OPONENTE:" + nombreJ2);
                salida2.println("PARTIDA_INICIADA;OPONENTE:" + nombreJ1);
                salida1.println("ESTADO;PALABRA:" + String.valueOf(progreso) + ";INTENTOS_RESTANTES:" + intentosJ1);
                salida2.println("ESTADO;PALABRA:" + String.valueOf(progreso) + ";INTENTOS_RESTANTES:" + intentosJ2);
            }

            while (!partidaFinalizada && !ganado() && (intentosJ1 > 0 || (jugador2 != null && intentosJ2 > 0))) {
                PrintWriter salidaActual = turnoJugador1 ? salida1 : salida2;
                PrintWriter salidaEspera = turnoJugador1 ? salida2 : salida1;
                BlockingQueue<String> colaActual = turnoJugador1 ? colaJ1 : colaJ2;
                String nombreActual = turnoJugador1 ? nombreJ1 : nombreJ2;
                boolean esJ1 = turnoJugador1;

                salidaActual.println("TURNO;JUGADOR:" + nombreActual);
                if (jugador2 != null) salidaEspera.println("ESPERA_TURNO");
                salidaActual.println("SOLICITUD_LETRA");

                String mensaje = colaActual.poll(15, TimeUnit.SECONDS);

                if (mensaje == null) {
                    if (esJ1) timeoutsJ1++;
                    else timeoutsJ2++;

                    if ((esJ1 && timeoutsJ1 >= 2) || (!esJ1 && timeoutsJ2 >= 2)) {
                        salidaActual.println("PARTIDA_CANCELADA");
                        if (jugador2 != null) salidaEspera.println("PARTIDA_CANCELADA");
                        cancelarPartida(nombreActual + " no respondió dos veces seguidas.");
                        return;
                    } else {
                        salidaActual.println("FALLO");
                        if (esJ1) intentosJ1--;
                        else intentosJ2--;
                        salidaActual.println("INTENTOS_RESTANTES:" + (esJ1 ? intentosJ1 : intentosJ2));
                        if (jugador2 != null) turnoJugador1 = !turnoJugador1;
                        continue;
                    }
                }

                if (mensaje.equalsIgnoreCase("cancelar")) {
                    cancelarPartida(nombreActual);
                    return;
                }

                char letra = mensaje.toLowerCase().charAt(0);
                boolean acierto = actualizarProgreso(letra);
                salidaActual.println(acierto ? "ACIERTO" : "FALLO");

                if (!acierto) {
                    if (esJ1) intentosJ1--;
                    else intentosJ2--;
                    salidaActual.println("INTENTOS_RESTANTES:" + (esJ1 ? intentosJ1 : intentosJ2));
                    if (jugador2 != null) turnoJugador1 = !turnoJugador1;
                }

                salida1.println("PALABRA;" + String.valueOf(progreso));
                if (jugador2 != null) salida2.println("PALABRA;" + String.valueOf(progreso));

                if (ganado()) {
                    int puntos = palabra.length() < 10 ? 1 : 2;
                    salidaActual.println("FIN_PARTIDA;RESULTADO:GANADOR;PALABRA:" + palabra);
                    JugadorDAO.actualizarPuntuacionJugador(nombreActual, puntos);
                    Jugador jugador = JugadorDAO.ObtenerJugador(nombreActual);
                    Partida partida = PartidaDAO.registrarPartida(palabra, true);
                    JugadorPartidaDAO.registrarJugadorPartida(jugador, partida, puntos);
                    if (jugador2 != null) {
                        (esJ1 ? salida2 : salida1).println("FIN_PARTIDA;RESULTADO:PERDEDOR;PALABRA:" + palabra);
                        Jugador perdedor = JugadorDAO.ObtenerJugador(esJ1 ? nombreJ2 : nombreJ1);
                        JugadorPartidaDAO.registrarJugadorPartida(perdedor, partida, 0);
                    }
                    cerrarConexiones();
                    partidaFinalizada = true;
                    return;
                }

                // Si ambos jugadores se quedan sin intentos (empate) o si es un jugador solo y pierde
                if ((intentosJ1 <= 0 && intentosJ2 <= 0) || (jugador2 == null && intentosJ1 <= 0)) {

                    // Registrar la partida como no ganada
                    Partida partida = PartidaDAO.registrarPartida(palabra, false);
                    Jugador jugador = JugadorDAO.ObtenerJugador(nombreJ1);
                    JugadorPartidaDAO.registrarJugadorPartida(jugador, partida, 0);

                    if (jugador2 == null) {
                        // Modo individual: solo jugador 1 ha perdido
                        salida1.println("FIN_PARTIDA;PERDEDOR;PALABRA:" + palabra);
                    } else {
                        // Ambos jugadores perdieron = empate
                        salida1.println("FIN_PARTIDA;EMPATE;PALABRA:" + palabra);
                        salida2.println("FIN_PARTIDA;EMPATE;PALABRA:" + palabra);

                        // Registrar también al segundo jugador
                        Jugador jugadorDos = JugadorDAO.ObtenerJugador(nombreJ2);
                        JugadorPartidaDAO.registrarJugadorPartida(jugadorDos, partida, 0);
                    }

                    cerrarConexiones();
                    partidaFinalizada = true;
                    return;
                }

            }

        } catch (Exception e) {
            System.out.println("Error en la partida: " + e.getMessage());
            cancelarPartida("error");
        }
    }

    private void escucharJugador(BufferedReader lector, BlockingQueue<String> cola, String nombre) {
        try {
            String linea;
            while ((linea = lector.readLine()) != null) {
                cola.put(linea);
            }
        } catch (Exception e) {
            System.out.println(nombre + " se ha desconectado.");
            cola.offer("cancelar");
        }
    }

    private void cancelarPartida(String nombre) {
        if (partidaFinalizada) return;
        System.out.println("La partida ha sido cancelada por " + nombre);
        partidaFinalizada = true;

        if (salida1 != null) salida1.println("PARTIDA_CANCELADA");
        if (salida2 != null) salida2.println("PARTIDA_CANCELADA");

        cerrarConexiones();
    }

    private boolean actualizarProgreso(char letra) {
        boolean acierto = false;
        for (int i = 0; i < palabra.length(); i++) {
            if (palabra.charAt(i) == letra && progreso[i] == '_') {
                progreso[i] = letra;
                acierto = true;
            }
        }
        return acierto;
    }

    private boolean ganado() {
        return palabra.equals(String.valueOf(progreso));
    }

    private void cerrarConexiones() {
        try {
            if (jugador1 != null && !jugador1.isClosed()) jugador1.close();
            if (jugador2 != null && !jugador2.isClosed()) jugador2.close();
        } catch (IOException ignored) {}
    }
}
