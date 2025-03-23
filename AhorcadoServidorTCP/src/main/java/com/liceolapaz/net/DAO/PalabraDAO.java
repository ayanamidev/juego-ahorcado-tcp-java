package com.liceolapaz.net.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liceolapaz.net.entidades.Palabra;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.io.InputStream;
import java.util.List;
import java.util.Random;

public class PalabraDAO {
    private static final StandardServiceRegistry sr = new StandardServiceRegistryBuilder().configure().build();
    private static final SessionFactory sf = new MetadataSources(sr).buildMetadata().buildSessionFactory();

    public static String obtenerPalabra() {
        Session session = sf.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            long count = (long) session.createQuery("SELECT COUNT(p) FROM Palabra p").uniqueResult();

            if (count == 0) {
                System.out.println("La tabla 'palabra' está vacía. Insertando datos desde JSON...");
                List<Palabra> palabras = cargarPalabrasDesdeJSON("palabras-ahorcado.json");
                for (Palabra palabra : palabras) {
                    session.persist(palabra);
                }
                transaction.commit();
                count = palabras.size();
                transaction = session.beginTransaction();
            } else {
                transaction.commit();
            }

            if (count > 0) {
                Random random = new Random();
                int idAleatorio = random.nextInt((int) count) + 1;
                Palabra palabra = session.get(Palabra.class, idAleatorio);
                return (palabra != null) ? palabra.getPalabra() : "No encontrada";
            }
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return "No encontrada";
    }

    private static List<Palabra> cargarPalabrasDesdeJSON(String nombreArchivo) {
        try {
            InputStream inputStream = PalabraDAO.class.getClassLoader().getResourceAsStream(nombreArchivo);
            if (inputStream == null) {
                throw new RuntimeException("No se encontró el archivo JSON: " + nombreArchivo);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(inputStream, new TypeReference<List<Palabra>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error al cargar el JSON: " + e.getMessage(), e);
        }
    }
}
