package com.liceolapaz.net.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "palabra")
public class Palabra {
    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "palabra", nullable = false)
    private String palabra;

    // Constructor sin argumentos requerido por Hibernate
    public Palabra() {
    }

    public Palabra(int id, String palabra) {
        this.id = id;
        this.palabra = palabra;
    }

    public Palabra(String palabra) {
        this.palabra = palabra;
    }

    // Getters y setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPalabra() {
        return palabra;
    }

    public void setPalabra(String palabra) {
        this.palabra = palabra;
    }
}