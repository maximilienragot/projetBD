package jdbc;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class SystemeEpicerie {

    private static final int MARGE_TOLERE_EXPIRATION = 7;
    private static final double REDUCTION = 0.9; // réduction appliquée à chaque mise à jour

    // Chaque entrée : [nomProduit, qte, joursRestants, typeDate]
    private static final ArrayList<String[]> produitBientotPerime = new ArrayList<>();
    private static int nombreAlertePeremption;

    public static void main(String[] args) {

        while (true) {
            try {
                mettreAJourProduitsBientotPerimes();
            } catch (Exception e) {
                System.out.println("Problème lors de la mise à jour des produits en solde.");
                e.printStackTrace();
            }

            try {
                Thread.sleep(10_000);
            } catch (InterruptedException ignored) {
                System.out.println("Mise à jour interrompue.");
            }
        }
    }

    // =====================================================
    //       GENERER NOUVELLE PERTE
    // =====================================================
    private static int genererNouvellePerteId(OracleDB db) throws SQLException {

        String sql = "SELECT NVL(MAX(IdPerte), 0) + 1 AS newId FROM Perte";

        Statement stmt = db.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        rs.next();

        int id = rs.getInt("newId");

        rs.close();
        stmt.close();

        return id;
    }

    // =====================================================
    //  MISE À JOUR PRINCIPALE
    // =====================================================
    private static void mettreAJourProduitsBientotPerimes() {

        produitBientotPerime.clear();
        nombreAlertePeremption = 0;

        LocalDate aujourdHui = LocalDate.now();
        LocalDate limite = aujourdHui.plusDays(MARGE_TOLERE_EXPIRATION);

        OracleDB db = new OracleDB();

        try { db.getConnection().setAutoCommit(false); } catch (Exception ignored) {}

        // =====================================================
        //     1) LOTS BIENTÔT PÉRIMÉS
        // =====================================================
        try {

            String sqlBientot = """
            SELECT lp.IdArticle,
                   lp.date_reception,
                   lp.date_peremption,
                   lp.type_date,
                   lp.Qte_dispo,
                   p.nom_produit,
                   a.Unite
            FROM Lot_Produit lp
            JOIN Art_Pdt a ON a.IdArticle = lp.IdArticle
            JOIN Produit p ON p.IdProduit = a.IdProduit
            WHERE lp.date_peremption <= ?
            """;

            PreparedStatement pstmt = db.getConnection().prepareStatement(sqlBientot);
            pstmt.setDate(1, java.sql.Date.valueOf(limite));

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int idArticle = rs.getInt("IdArticle");
                LocalDate dateReception = rs.getDate("date_reception").toLocalDate();
                LocalDate datePeremption = rs.getDate("date_peremption").toLocalDate();

                String nom = rs.getString("nom_produit");
                int qte = rs.getInt("Qte_dispo");
                String typeDate = rs.getString("type_date");

                long joursRestants = ChronoUnit.DAYS.between(aujourdHui, datePeremption);

                produitBientotPerime.add(new String[]{
                        nom,
                        String.valueOf(qte),
                        String.valueOf(joursRestants),
                        typeDate
                });

                nombreAlertePeremption++;

                // appliquer réduction
                reduirePrixArticle(db, idArticle);
            }

            rs.close();
            pstmt.close();

            db.getConnection().commit();

        } catch (Exception e) {
            try { db.getConnection().rollback(); } catch (Exception ignored) {}
            e.printStackTrace();
        }

        // =====================================================
        //     2) LOTS PÉRIMÉS → PERTE + SUPPRESSION
        // =====================================================
        try {

            db.getConnection().setAutoCommit(false);

            String sqlPerimes = """
            SELECT lp.IdArticle,
                   lp.date_reception,
                   lp.Qte_dispo,
                   a.Unite
            FROM Lot_Produit lp
            JOIN Art_Pdt a ON a.IdArticle = lp.IdArticle
            WHERE lp.date_peremption < CURRENT_DATE
            """;

            Statement stmt = db.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sqlPerimes);

            while (rs.next()) {

                int idArticle = rs.getInt("IdArticle");
                LocalDate dateReception = rs.getDate("date_reception").toLocalDate();
                int qte = rs.getInt("Qte_dispo");
                String unite = rs.getString("Unite");

                int idPerte = genererNouvellePerteId(db);

                String insertPerte = """
                INSERT INTO Perte(IdPerte, datePerte, naturePerte, typePerte, qtePerdue, unite)
                VALUES (?, CURRENT_DATE, 'Peremption', 'Article', ?, ?)
                """;

                PreparedStatement psPerte = db.getConnection().prepareStatement(insertPerte);
                psPerte.setInt(1, idPerte);
                psPerte.setInt(2, qte);
                psPerte.setString(3, unite);
                psPerte.executeUpdate();
                psPerte.close();

                String deleteLot = """
                DELETE FROM Lot_Produit
                WHERE IdArticle = ? 
                AND date_reception = ?
                """;

                PreparedStatement psDel = db.getConnection().prepareStatement(deleteLot);
                psDel.setInt(1, idArticle);
                psDel.setDate(2, java.sql.Date.valueOf(dateReception));
                psDel.executeUpdate();
                psDel.close();
            }

            rs.close();
            stmt.close();

            db.getConnection().commit();

        } catch (Exception e) {
            try { db.getConnection().rollback(); } catch (Exception ignored) {}
            e.printStackTrace();
        }

        db.close();
    }

    // =====================================================
    //     RÉDUCTION DU PRIX
    // =====================================================
    private static void reduirePrixArticle(OracleDB db, int idArticle) throws SQLException {

        String sql = """
            UPDATE Article
            SET prixTTC = prixTTC * ?
            WHERE IdArticle = ?
            """;

        PreparedStatement ps = db.getConnection().prepareStatement(sql);
        ps.setDouble(1, REDUCTION);
        ps.setInt(2, idArticle);
        ps.executeUpdate();
        ps.close();
    }

    // =====================================================
    //     GETTERS
    // =====================================================
    public static ArrayList<String[]> getProduitBientotPerime() {
        return produitBientotPerime;
    }

    public static int getNombreAlertePeremption() {
        return nombreAlertePeremption;
    }
}
