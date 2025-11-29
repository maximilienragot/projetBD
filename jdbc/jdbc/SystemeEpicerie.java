package jdbc;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class SystemeEpicerie {

    private static final int MARGE_TOLERE_EXPIRATION = 7;
    private static final double REDUCTION = 0.5;

    // Chaque entrée : [nomProduit, qte, joursRestants, typeDate]
    private static final ArrayList<String[]> produitBientotPerime = new ArrayList<>();
    private static final ArrayList<String> lotsDejaReduits = new ArrayList<>();
    private static int nombreAlertePeremption;


    public static void main(String[] args) {

        while (true) {
            mettreAJourProduitsBientotPerimes();
            try {
                Thread.sleep(30_000); // 30 secondes
            } catch (InterruptedException ignored) {
                return;
            }
        }
    }



    // =====================================================
    //   GENERER NOUVELLE PERTE
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

        try {

            // =====================================================
            // 1️⃣ Sélection des lots bientôt périmés
            // =====================================================
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

                // ID unique du lot
                String lotKey = idArticle + "-" + dateReception;

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

                // =====================================================
                // 2️⃣ Réduction appliquée UNE SEULE FOIS
                // =====================================================
                if (!lotsDejaReduits.contains(lotKey)) {

                    reduirePrixArticle(db, idArticle);
                    lotsDejaReduits.add(lotKey);
                }
            }

            rs.close();
            pstmt.close();
            db.getConnection().commit();

        } catch (Exception e) {
            try { db.getConnection().rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
        }

        // =====================================================
        // 3️⃣ Lots déjà périmés → PERTE + suppression
        // =====================================================
        try {

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
            ResultSet rsMajPerime = stmt.executeQuery(sqlPerimes);

            while (rsMajPerime.next()) {

                int idArticle = rsMajPerime.getInt("IdArticle");
                LocalDate dateReception = rsMajPerime.getDate("date_reception").toLocalDate();
                int qte = rsMajPerime.getInt("Qte_dispo");
                String unite = rsMajPerime.getString("Unite");

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

                // supprimer le lot
                String deleteLot = """
                    DELETE FROM Lot_Produit
                    WHERE IdArticle = ? AND date_reception = ?
                    """;

                PreparedStatement psDel = db.getConnection().prepareStatement(deleteLot);
                psDel.setInt(1, idArticle);
                psDel.setDate(2, java.sql.Date.valueOf(dateReception));
                psDel.executeUpdate();
                psDel.close();
            }

            rsMajPerime.close();
            stmt.close();
            db.getConnection().commit();

        } catch (Exception e) {
            try { db.getConnection().rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
        }

        db.close();
    }

    // =====================================================
    //  RÉDUCTION UNIQUE SUR UN ARTICLE
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
    // GETTERS
    // =====================================================
    public static ArrayList<String[]> getProduitBientotPerime() {
        return produitBientotPerime;
    }

    public static int getNombreAlertePeremption() {
        return nombreAlertePeremption;
    }
}
