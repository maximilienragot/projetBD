package jdbc;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class SystemeEpicerie {

    private static final int MARGE_TOLERE_EXPIRATION = 7;
    private static final double REDUCTION = 0.5;

    private static final ArrayList<String[]> produitBientotPerime = new ArrayList<>();
    private static int nombreAlertePeremption;



    public static void main(String[] args) {

        while (true) {

            mettreAJourProduitsBientotPerimes();

            try {
                Thread.sleep(30000); // 30 secondes
            } catch (InterruptedException ignored) {}
        }
    }


    // =====================================================
    //   GETTERS
    // =====================================================
    public static ArrayList<String[]> getProduitBientotPerime() {
        return produitBientotPerime;
    }

    public static int getNombreAlertePeremption() {
        return nombreAlertePeremption;
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

            Statement stmt = db.getConnection().createStatement();

            // -----------------------------------------
            // 1️⃣ LOTS BIENTÔT PÉRIMÉS
            // -----------------------------------------

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
                WHERE lp.date_peremption > CURRENT_DATE
                  AND lp.date_peremption <= ?
                """;

            PreparedStatement pstmt = db.getConnection().prepareStatement(sqlBientot);
            pstmt.setDate(1, java.sql.Date.valueOf(limite));

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

                String nom = rs.getString("nom_produit");
                int qte = rs.getInt("Qte_dispo");
                String typeDate = rs.getString("type_date");

                LocalDate datePeremption = rs.getDate("date_peremption").toLocalDate();
                long joursRestants = ChronoUnit.DAYS.between(aujourdHui, datePeremption);

                produitBientotPerime.add(new String[]{
                        nom,
                        String.valueOf(qte),
                        String.valueOf(joursRestants),
                        typeDate
                });

                nombreAlertePeremption++;
            }

            rs.close();
            pstmt.close();



            // -----------------------------------------
            // 2️⃣ RÉDUCTION DES PRIX /2
            // -----------------------------------------
            reduirePrixLotsPerimes(db, limite);



            // -----------------------------------------
            // 3️⃣ LOTS DÉJÀ PÉRIMÉS → PERTE
            // -----------------------------------------

            String sqlPerimes = """
                SELECT lp.IdArticle,
                       lp.date_reception,
                       lp.Qte_dispo,
                       a.Unite
                FROM Lot_Produit lp
                JOIN Art_Pdt a ON a.IdArticle = lp.IdArticle
                WHERE lp.date_peremption < CURRENT_DATE
                """;

            rs = stmt.executeQuery(sqlPerimes);

            while (rs.next()) {

                int idArticle = rs.getInt("IdArticle");
                Date dateReceptionSQL = rs.getDate("date_reception");
                LocalDate dateReception = dateReceptionSQL.toLocalDate();
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

            rs.close();
            stmt.close();


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { db.close(); } catch (Exception ignored) {}
        }
    }



    // =====================================================
    //  RÉDUCTION DES PRIX TTC
    // =====================================================
    private static void reduirePrixLotsPerimes(OracleDB db, LocalDate limite) throws SQLException {

        String sql = """
            UPDATE Article ar
            SET ar.prixTTC = ar.prixTTC * ?
            WHERE ar.IdArticle IN (
                SELECT DISTINCT lp.IdArticle
                FROM Lot_Produit lp
                WHERE lp.date_peremption <= ?
            )
            """;

        PreparedStatement ps = db.getConnection().prepareStatement(sql);

        ps.setDouble(1, REDUCTION);
        ps.setDate(2, java.sql.Date.valueOf(limite));

        ps.executeUpdate();
        ps.close();
    }

}
