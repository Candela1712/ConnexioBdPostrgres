package com.iticbcn.banc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;
 
 
public class Main {
    public static void main(String[] args) {
        Properties p = new Properties();


        try {
            p.load(Files.newBufferedReader(Path.of("banc","src","main","resources","conn.properties")));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        HikariConfig hkcfg = new HikariConfig();
        hkcfg.setJdbcUrl(p.getProperty("DB_URL"));
        hkcfg.setUsername(p.getProperty("DB_USER"));
        hkcfg.setPassword(p.getProperty("DB_PASSWORD"));
        hkcfg.setMaximumPoolSize(Integer.parseInt(p.getProperty("DB_MAX_POOL", "5")));
 
        //iniciem el datasource i controlem excepcions
        try (HikariDataSource hkds = new HikariDataSource(hkcfg)) {
            //connectem la base de dades
            try (Connection cnn = hkds.getConnection()){
                
                //Mostrar els moviments
                for (String resultat : readAllStatements(cnn)) {
                    System.out.println("moviment amb id: " + resultat);
                }

                //Exercici 1. moviments amb import
                System.out.println();
                readStatementsByImport(cnn, 10.0);

                //Exercici 3 insertar client nou
                System.out.println();
                Scanner sc = new Scanner(System.in);
                List<String> dades = new ArrayList<>();

                System.out.print("Nom: ");
                dades.add(sc.nextLine());
                System.out.print("Cognoms: ");
                dades.add(sc.nextLine());
                System.out.print("ID fiscal: ");
                dades.add(sc.nextLine());

                insertClient(cnn, dades);
                
                String query = "SELECT * FROM statements";
                PreparedStatement ps = cnn.prepareStatement(query);


            } catch (SQLException e) {
                System.out.println("No connectat!");
                e.printStackTrace();
            }

        } catch (PoolInitializationException e) {  
            System.out.println("No s'ha pogut connectar a la base de dades: " + e.getMessage());
        }
 
 
    }
    //per mostrar tots els registres
    public static List<String> readAllStatements(Connection conn) throws SQLException {
        List<String> resultatQuery = new ArrayList<>();
        String query = "SELECT * FROM statements";

        try (PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String resultat = String.join(";",
                        rs.getString("id_statement"),
                        rs.getString("tipus_moviment"),
                        rs.getString("import"),
                        rs.getString("divisa"));
                resultatQuery.add(resultat);
            }
        }
        return resultatQuery;
    }

    //Ex 1 statements amb import
    public static void readStatementsByImport(Connection conn, double quantitat) throws SQLException {
        String query = "SELECT * FROM statements WHERE import >= ?";

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setDouble(1, quantitat);

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                String header = String.join(";", 
                        meta.getColumnName(1),
                        meta.getColumnName(3),
                        meta.getColumnName(8),
                        meta.getColumnName(10));
                System.out.println(header);

                while (rs.next()) {
                    String linia = String.join(";",
                            rs.getString("id_statement"),
                            rs.getString("tipus_moviment"),
                            rs.getString("import"),
                            rs.getString("divisa"));
                    System.out.println(linia);
                }
            }
        }
    }


    // Ex 2 comprovar si el client existeix

    public static boolean clientExists(Connection conn, String idFiscal) throws SQLException {
        String query = "SELECT 1 FROM clients WHERE id_fiscal = ?";

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, idFiscal);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Exercici 2: inserta client utilitzant  commit / rollback
    public static void insertClient(Connection conn, List<String> dades) throws SQLException {
        String nom = dades.get(0);
        String cognoms = dades.get(1);
        String idFiscal = dades.get(2);

        if (clientExists(conn, idFiscal)) {
            System.out.println("El client ja existeix.");
            return;
        }

        String insert = "INSERT INTO clients (nom, cognoms, id_fiscal) VALUES (?,?,?)";
        boolean autoCommitOriginal = conn.getAutoCommit();

        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            conn.setAutoCommit(false);
            ps.setString(1, nom);
            ps.setString(2, cognoms);
            ps.setString(3, idFiscal);
            ps.executeUpdate();
            conn.commit();
            System.out.println("Client inserit correctament.");
        } catch (SQLException e) {
            conn.rollback();
            System.out.println("Error, s'ha fet rollback: " + e.getMessage());
        } finally {
            conn.setAutoCommit(autoCommitOriginal);
        }
    }
}

