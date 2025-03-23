package com.liceolapaz.net.entidades;

import jakarta.persistence.*;

@Entity
@Table(name = "Jugador_Partida")
public class JugadorPartida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "idPartida", nullable = false)
    private Partida partida;

    @ManyToOne
    @JoinColumn(name = "idJugador", nullable = false)
    private Jugador jugador;

    @Column(name = "puntuaciónPartida")
    private int puntuacionPartida = 0;

    public JugadorPartida() {
    }

    public JugadorPartida(Partida partida, Jugador jugador, int puntuacionPartida) {
        this.partida = partida;
        this.jugador = jugador;
        this.puntuacionPartida = puntuacionPartida;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public void setJugador(Jugador jugador) {
        this.jugador = jugador;
    }

    public int getPuntuacionPartida() {
        return puntuacionPartida;
    }

    public void setPuntuacionPartida(int puntuacionPartida) {
        this.puntuacionPartida = puntuacionPartida;
    }
}