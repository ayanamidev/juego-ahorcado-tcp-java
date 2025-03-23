package com.liceolapaz.net.DAO;

import com.liceolapaz.net.entidades.Jugador;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class JugadorDAO {

    private static final StandardServiceRegistry sr =
            new StandardServiceRegistryBuilder().configure().build();

    private static final SessionFactory sf =
            new MetadataSources(sr).buildMetadata().buildSessionFactory();

    public static void verificarYCrearJugador(String nombre) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            // Buscar si ya existe el jugador por nombre
            Jugador jugador = session.createQuery(
                            "FROM Jugador WHERE nombre = :nombre", Jugador.class)
                    .setParameter("nombre", nombre)
                    .uniqueResult();

            if (jugador == null) {
                // Si no existe, lo creamos
                jugador = new Jugador();
                jugador.setNombre(nombre);
                session.persist(jugador);
                System.out.println("Jugador nuevo añadido: " + nombre);
            } else {
                System.out.println("Jugador ya existe: " + nombre);
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.out.println("Error al verificar o crear jugador: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    public static Jugador ObtenerJugador(String nombre){
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();
        Jugador jugador = session.createQuery(
                        "FROM Jugador WHERE nombre = :nombre", Jugador.class)
                .setParameter("nombre", nombre)
                .uniqueResult();

        return jugador;
    }

    public static void actualizarPuntuacionJugador(String nombre, int puntos) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            // Buscar el jugador por nombre
            Jugador jugador = session.createQuery(
                            "FROM Jugador WHERE nombre = :nombre", Jugador.class)
                    .setParameter("nombre", nombre)
                    .uniqueResult();

            if (jugador != null) {
                // Actualizar la puntuación total del jugador
                int nuevaPuntuacion = jugador.getPuntuacionTotal() + puntos;
                jugador.setPuntuacionTotal(nuevaPuntuacion);
                session.merge(jugador); // Actualizar la entidad en la base de datos
                System.out.println("Puntuación actualizada para el jugador " + nombre + ": " + nuevaPuntuacion);
            } else {
                System.out.println("Jugador no encontrado: " + nombre);
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.out.println("Error al actualizar la puntuación del jugador: " + e.getMessage());
        } finally {
            session.close();
        }
    }
    public static int obtenerPuntuacion(String nombre) {
        Session session = sf.openSession();
        int puntuacion = 0;

        try {
            Jugador jugador = session.createQuery(
                            "FROM Jugador WHERE nombre = :nombre", Jugador.class)
                    .setParameter("nombre", nombre)
                    .uniqueResult();

            if (jugador != null) {
                puntuacion = jugador.getPuntuacionTotal();
            }

        } catch (Exception e) {
            System.out.println("Error al obtener puntuación: " + e.getMessage());
        } finally {
            session.close();
        }

        return puntuacion;
    }

}
