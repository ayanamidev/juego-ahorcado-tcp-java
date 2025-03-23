package com.liceolapaz.net.DAO;

import com.liceolapaz.net.entidades.Palabra;
import com.liceolapaz.net.entidades.Partida;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class PartidaDAO {

    private static final StandardServiceRegistry sr =
            new StandardServiceRegistryBuilder().configure().build();
    private static final SessionFactory sf =
            new MetadataSources(sr).buildMetadata().buildSessionFactory();

    public static Partida registrarPartida(String palabraTexto, boolean acertado) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            // Buscar la palabra en la tabla
            Palabra palabra = session.createQuery(
                            "FROM Palabra WHERE palabra = :texto", Palabra.class)
                    .setParameter("texto", palabraTexto)
                    .uniqueResult();


            // Crear objeto Partida
            Partida partida = new Partida();
            partida.setAcertado(acertado);
            partida.setPalabra(palabra);

            // Guardar en la base de datos
            session.persist(partida);
            tx.commit();

            System.out.println("✅ Partida registrada con palabra: '" + palabraTexto + "', acertado: " + acertado);
            return  partida;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.out.println("❌ Error al registrar la partida: " + e.getMessage());
        } finally {
            session.close();
        }
        return null;
    }
}
