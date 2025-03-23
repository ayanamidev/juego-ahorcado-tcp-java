package com.liceolapaz.net.DAO;

import com.liceolapaz.net.entidades.Jugador;
import com.liceolapaz.net.entidades.JugadorPartida;
import com.liceolapaz.net.entidades.Partida;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class JugadorPartidaDAO {

    private static final StandardServiceRegistry sr =
            new StandardServiceRegistryBuilder().configure().build();

    private static final SessionFactory sf =
            new MetadataSources(sr).buildMetadata().buildSessionFactory();

    public static void registrarJugadorPartida(Jugador jugador, Partida partida, int puntuacion) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            JugadorPartida jugadorPartida = new JugadorPartida();
            jugadorPartida.setJugador(jugador);
            jugadorPartida.setPartida(partida);
            jugadorPartida.setPuntuacionPartida(puntuacion);

            session.persist(jugadorPartida);
            tx.commit();

            System.out.println("Registro creado en JugadorPartida: " +
                    jugador.getNombre() + ", partida ID: " + partida.getId() + ", puntos: " + puntuacion);
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.out.println("Error al registrar JugadorPartida: " + e.getMessage());
        } finally {
            session.close();
        }
    }
}
