package jdbc;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class MenuApp {

    static String id_client = "";
    static String mail = "";
    static boolean compteIncomplet = true;
    private static int nombreAlertePeremption;
    private static final int MARGE_TOLERE_EXPIRATION = 10;
    private static final String STATUT_RECUPERE = "Récupérée | Livrée";
    private static final Map<Integer, Double> prixBaseArticles = new HashMap<>();
    private static final DateTimeFormatter HEURE_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String SQL_DATE_PEREMPTION = "CASE WHEN REGEXP_LIKE(lp.date_peremption, '^[0-9]{2}-[A-Z]{3}-[0-9]{2}$') THEN TO_DATE(lp.date_peremption, 'DD-MON-RR', 'NLS_DATE_LANGUAGE=AMERICAN') WHEN REGEXP_LIKE(lp.date_peremption, '^[0-9]{4}-[0-9]{2}-[0-9]{2}$') THEN TO_DATE(lp.date_peremption, 'YYYY-MM-DD') ELSE NULL END";
    static Scanner scanner = new Scanner(System.in);
    static List<String> requetes = List.of(
            "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Légumes'",
            "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Fruits' ",
            "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Produits Laitiers' ",
            "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Boulangerie' ",
            "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Boissons' ",
            "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Épicerie' ",
            "SELECT ar.idArticle, c.type, c.capacite, c.unite, c.caractere, ar.prixttc FROM Contenant c INNER JOIN Article ar ON c.idArticle = ar.idArticle");

    static class LigneCommandeSaisie {
        final int idArticle;
        final String nomProduit;
        final double quantite;
        final double prixUnitaire;
        final double sousTotal;
        final String quantiteAffichage;

        LigneCommandeSaisie(int idArticle, String nomProduit, double quantite, double prixUnitaire, String quantiteAffichage) {
            this.idArticle = idArticle;
            this.nomProduit = nomProduit;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
            this.sousTotal = quantite * prixUnitaire;
            this.quantiteAffichage = quantiteAffichage;
        }
    }

    private static void mettreAJourInformationsPersonnelles() {
        clearScreen();
        System.out.println("===== Modifier mes informations =====");
        if (id_client.isEmpty()) {
            System.out.println("Vous devez être connecté(e).");
            pause();
            return;
        }
        OracleDB db = new OracleDB();
        Connection conn = db.getConnection();
        if (conn == null) {
            System.out.println("Connexion indisponible.");
            db.close();
            pause();
            return;
        }
        boolean transactionActive = false;
        boolean previousAutoCommit = true;
        try {
            int clientId = Integer.parseInt(id_client);
            String nomActuel;
            String prenomActuel;
            String telActuel;
            String emailActuel;

            String sqlInfo = "SELECT NomC, PrenomC, NumTelC, EmailC FROM Client_Non_Oublie WHERE IdClient = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlInfo)) {
                ps.setInt(1, clientId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Aucune information enregistrée. Complétez d'abord votre profil.");
                        compteIncomplet = true;
                        return;
                    }
                    nomActuel = rs.getString("NomC");
                    prenomActuel = rs.getString("PrenomC");
                    telActuel = rs.getString("NumTelC");
                    emailActuel = rs.getString("EmailC");
                }
            }

            List<String> adressesActuelles = new ArrayList<>();
            String sqlAdresse = "SELECT AdresseLiv FROM Adresse_Livraison WHERE IdClient = ? ORDER BY AdresseLiv";
            try (PreparedStatement ps = conn.prepareStatement(sqlAdresse)) {
                ps.setInt(1, clientId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        adressesActuelles.add(rs.getString(1));
                    }
                }
            }

            if (adressesActuelles.isEmpty()) {
                System.out.println("Aucune adresse enregistrée.");
            } else {
                System.out.println("Adresses enregistrées :");
                for (String adresse : adressesActuelles) {
                    System.out.println("- " + adresse);
                }
            }

            System.out.print("Nom (" + nomActuel + ") : ");
            String nom = scanner.nextLine().trim();
            if (nom.isEmpty()) {
                nom = nomActuel;
            }

            System.out.print("Prénom (" + prenomActuel + ") : ");
            String prenom = scanner.nextLine().trim();
            if (prenom.isEmpty()) {
                prenom = prenomActuel;
            }

            System.out.print("Téléphone (" + telActuel + ") : ");
            String telephone = scanner.nextLine().trim();
            if (telephone.isEmpty()) {
                telephone = telActuel;
            }

            System.out.print("Email (" + emailActuel + ") : ");
            String nouvelEmail = scanner.nextLine().trim();
            if (nouvelEmail.isEmpty()) {
                nouvelEmail = emailActuel;
            }

            if (!nouvelEmail.equalsIgnoreCase(emailActuel) && mailExisteDansBDD(db, nouvelEmail)) {
                System.out.println("Email déjà utilisé.");
                return;
            }

            List<String> nouvellesAdresses = null;
            System.out.println("Souhaitez-vous remplacer vos adresses de livraison ? (y/n)");
            if (yesNoQuestion().equals("y")) {
                nouvellesAdresses = saisirAdresses();
            }

            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            transactionActive = true;

            String updateSql = "UPDATE Client_Non_Oublie SET NomC = ?, PrenomC = ?, NumTelC = ?, EmailC = ? WHERE IdClient = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, nom);
                ps.setString(2, prenom);
                ps.setString(3, telephone);
                ps.setString(4, nouvelEmail);
                ps.setInt(5, clientId);
                ps.executeUpdate();
            }

            if (nouvellesAdresses != null && !nouvellesAdresses.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Adresse_Livraison WHERE IdClient = ?")) {
                    ps.setInt(1, clientId);
                    ps.executeUpdate();
                }
                for (String adresse : nouvellesAdresses) {
                    enregistrerAdresse(conn, clientId, adresse);
                }
            }

            conn.commit();
            transactionActive = false;
            conn.setAutoCommit(previousAutoCommit);
            mail = nouvelEmail;
            compteIncomplet = false;
            System.out.println("Informations mises à jour.");
        } catch (NumberFormatException e) {
            System.out.println("Identifiant client invalide.");
        } catch (SQLException e) {
            if (transactionActive) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                }
            }
            System.out.println("Impossible de mettre à jour vos informations : " + e.getMessage());
        } finally {
            db.close();
            pause();
        }
    }

    private static void supprimerDonneesClient() {
        clearScreen();
        System.out.println("===== Supprimer mes données =====");
        if (id_client.isEmpty()) {
            System.out.println("Vous devez être connecté(e).");
            pause();
            return;
        }
        OracleDB db = new OracleDB();
        try {
            System.out.println("Cette action est définitive. Confirmez-vous la suppression ? (y/n)");
            if (yesNoQuestion().equals("y")) {
                anonymiserClient(db);
            } else {
                System.out.println("Suppression annulée.");
            }
        } catch (SQLException e) {
            System.out.println("Impossible de supprimer vos données : " + e.getMessage());
        } finally {
            db.close();
            pause();
        }
    }

    private static void consulterCommandesClient() {
        clearScreen();
        System.out.println("===== Mes commandes =====");
        if (id_client.isEmpty()) {
            System.out.println("Vous devez être connecté(e).");
            pause();
            return;
        }
        OracleDB db = new OracleDB();
        Connection conn = db.getConnection();
        if (conn == null) {
            System.out.println("Connexion indisponible.");
            db.close();
            pause();
            return;
        }
        List<Integer> commandesAnnulables = new ArrayList<>();
        try {
            int clientId = Integer.parseInt(id_client);
            String sql = "SELECT IdCommande, date_Com, Heure_Com, Prix_Total, Statut, Mode_Paiement, Mode_Recuperation FROM Commande WHERE IdClient = ? ORDER BY date_Com DESC, Heure_Com DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, clientId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean hasRow = false;
                    System.out.printf("%-10s %-12s %-8s %-15s %-20s %-20s %-10s%n",
                            "IdCmd", "Date", "Heure", "Statut", "ModePaiement", "ModeRecup", "Total");
                    while (rs.next()) {
                        hasRow = true;
                        int idCommande = rs.getInt("IdCommande");
                        java.sql.Date dateCom = rs.getDate("date_Com");
                        String heure = rs.getString("Heure_Com");
                        String statut = rs.getString("Statut");
                        String modePaiement = rs.getString("Mode_Paiement");
                        String modeRecup = rs.getString("Mode_Recuperation");
                        double total = rs.getDouble("Prix_Total");
                        if ("En Préparation".equalsIgnoreCase(statut)) {
                            commandesAnnulables.add(idCommande);
                        }
                        System.out.printf("%-10d %-12s %-8s %-15s %-20s %-20s %-10.2f%n",
                                idCommande,
                                dateCom != null ? dateCom.toString() : "",
                                heure != null ? heure : "",
                                statut,
                                modePaiement,
                                modeRecup,
                                total);
                    }
                    if (!hasRow) {
                        System.out.println("Aucune commande enregistrée.");
                        return;
                    }
                }
            }

            if (commandesAnnulables.isEmpty()) {
                System.out.println("\nAucune commande n'est annulable pour le moment.");
                return;
            }

            System.out.println("\nSouhaitez-vous annuler une commande en préparation ? (y/n)");
            if (yesNoQuestion().equals("y")) {
                System.out.print("ID de la commande : ");
                int idCommande = scanner.nextInt();
                scanner.nextLine();
                if (!commandesAnnulables.contains(idCommande)) {
                    System.out.println("Cette commande n'est pas annulable.");
                } else {
                    annulerCommandeEnPreparation(conn, idCommande);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Identifiant client invalide.");
        } catch (SQLException e) {
            System.out.println("Impossible d'afficher vos commandes : " + e.getMessage());
        } finally {
            db.close();
            pause();
        }
    }

    private static void annulerCommandeEnPreparation(Connection conn, int idCommande) {
        boolean previousAutoCommit = true;
        try {
            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE Commande SET Statut = 'Annulée' WHERE IdCommande = ? AND Statut = 'En Préparation'")) {
                ps.setInt(1, idCommande);
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    conn.rollback();
                    System.out.println("Commande déjà traitée, annulation impossible.");
                } else {
                    conn.commit();
                    System.out.println("Commande " + idCommande + " annulée.");
                }
            }
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
            System.out.println("Impossible d'annuler la commande : " + e.getMessage());
        } finally {
            try {
                conn.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    private static String yesNoQuestion() {
        while (true) {
            String reponse = scanner.nextLine().trim().toLowerCase();
            if (reponse.equals("y") || reponse.equals("n")) {
                return reponse;
            }
            System.out.print("Répondez y/n : ");
        }
    }

    private static String demanderValeurNonVide(String message) {
        while (true) {
            System.out.print(message);
            String valeur = scanner.nextLine().trim();
            if (!valeur.isEmpty()) {
                return valeur;
            }
            System.out.println("Valeur obligatoire.");
        }
    }

    public static void main(String[] args) {
        choisirInterface();
    }

    private static void choisirInterface() {
        while (true) {
            clearScreen();
            System.out.println("===== CHOIX D'INTERFACE =====");
            System.out.println("1. Interface Utilisateur");
            System.out.println("2. Interface Épicier");
            System.out.println("0. Quitter");
            System.out.print("Votre choix : ");
            int choix = scanner.nextInt();
            scanner.nextLine();

            switch (choix) {
                case 1 -> menuUtilisateur();
                case 2 -> menuEpicier();
                case 0 -> {
                    System.out.println("Au revoir !");
                    return;
                }
                default -> {
                    System.out.println("Choix invalide !");
                    pause();
                }
            }
        }
    }

    public static void connectionDuClient() {
        System.out.println("Avez-vous un compte ? (y/n)");
        String reponse = yesNoQuestion();
        OracleDB db = new OracleDB();
        try {
            if (reponse.equals("y")) {
                connexionClient(db);
            } else {
                System.out.println("Souhaitez-vous vous inscrire ? (y/n)");
                if (yesNoQuestion().equals("y")) {
                    inscriptionClient(db);
                } else {
                    System.out.println("Pas de souci, vous pourrez vous inscrire plus tard.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur d'authentification : " + e.getMessage());
            try {
                db.getConnection().rollback();
            } catch (SQLException ignored) {
            }
        } finally {
            db.close();
        }
        pause();
    }

    private static void connexionClient(OracleDB db) throws SQLException {
        while (true) {
            String email = demanderValeurNonVide("Entrez votre email : ");
            Integer clientId = trouverClientParMail(db, email);
            if (clientId != null) {
                id_client = String.valueOf(clientId);
                mail = email;
                compteIncomplet = false;
                System.out.println("Connexion réussie.");
                System.out.println("Souhaitez-vous mettre à jour vos informations personnelles ? (y/n)");
                if (yesNoQuestion().equals("y")) {
                    questionDonnees();
                }
                return;
            }
            System.out.println("Aucun compte trouvé pour cet email. Voulez-vous vous inscrire avec cet email ? (y/n)");
            if (yesNoQuestion().equals("y")) {
                creerCompteComplet(db, email);
                return;
            }
            System.out.println("Voulez-vous essayer avec un autre email ? (y/n)");
            if (yesNoQuestion().equals("n")) {
                return;
            }
        }
    }

    private static void inscriptionClient(OracleDB db) throws SQLException {
        while (true) {
            String email = demanderValeurNonVide("Entrez votre email : ");
            if (mailExisteDansBDD(db, email)) {
                System.out.println("Email déjà utilisé. Voulez-vous vous connecter ? (y/n)");
                if (yesNoQuestion().equals("y")) {
                    connexionClient(db);
                    return;
                }
            } else {
                creerCompteComplet(db, email);
                return;
            }
        }
    }

    private static void creerCompteComplet(OracleDB db, String email) throws SQLException {
        Connection conn = db.getConnection();
        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            int newId = genererNouvelIdClient(conn);
            insererClient(conn, newId);
            String nom = demanderValeurNonVide("Entrez votre nom : ");
            String prenom = demanderValeurNonVide("Entrez votre prénom : ");
            String telephone = demanderValeurNonVide("Entrez votre numéro de téléphone : ");
            List<String> adresses = saisirAdresses();
            for (String adresse : adresses) {
                enregistrerAdresse(conn, newId, adresse);
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Client_Non_Oublie (IdClient, NomC, PrenomC, NumTelC, EmailC) VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, newId);
                ps.setString(2, nom);
                ps.setString(3, prenom);
                ps.setString(4, telephone);
                ps.setString(5, email);
                ps.executeUpdate();
            }
            conn.commit();
            id_client = String.valueOf(newId);
            mail = email;
            compteIncomplet = false;
            System.out.println("Compte créé. Votre identifiant est " + id_client + ".");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    private static int genererNouvelIdClient(Connection conn) throws SQLException {
        String sql = "SELECT NVL(MAX(IdClient), 0) + 1 FROM Client";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 1;
    }

    private static void insererClient(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Client (IdClient) VALUES (?)")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static void enregistrerAdresse(Connection conn, int idClient, String adresse) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Adresse_Livraison (AdresseLiv, IdClient) VALUES (?, ?)")) {
            ps.setString(1, adresse);
            ps.setInt(2, idClient);
            ps.executeUpdate();
        }
    }

    private static List<String> saisirAdresses() {
        List<String> adresses = new ArrayList<>();
        while (true) {
            String adresse = demanderValeurNonVide("Adresse : ");
            if (adresses.contains(adresse)) {
                System.out.println("Adresse déjà ajoutée.");
            } else {
                adresses.add(adresse);
            }
            System.out.print("Ajouter une autre adresse ? (y/n) : ");
            if (yesNoQuestion().equals("n") && !adresses.isEmpty()) {
                return adresses;
            }
        }
    }

    private static boolean mailExisteDansBDD(OracleDB db, String email) throws SQLException {
        return trouverClientParMail(db, email) != null;
    }

    private static Integer trouverClientParMail(OracleDB db, String email) throws SQLException {
        String sql = "SELECT IdClient FROM Client_Non_Oublie WHERE LOWER(EmailC) = LOWER(?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }

    // ============================
    // MENUS PRINCIPAUX
    // ============================
    private static void rafraichirNombreAlertes() {
        archiverLotsPerimes();
        LocalDate dateCourante = LocalDate.now();
        LocalDate limite = dateCourante.plusDays(MARGE_TOLERE_EXPIRATION);

        try {
            OracleDB db = new OracleDB();
            String sql = "SELECT COUNT(*) " +
                    "FROM Lot_Produit lp " +
                    "WHERE " + SQL_DATE_PEREMPTION + " IS NOT NULL " +
                    "AND " + SQL_DATE_PEREMPTION + " <= TO_DATE(?, 'YYYY-MM-DD')";

            PreparedStatement pstmt = db.getConnection().prepareStatement(sql);
            pstmt.setString(1, limite.toString());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                nombreAlertePeremption = rs.getInt(1);
            }

            rs.close();
            pstmt.close();
            db.close();
        } catch (Exception e) {
            System.out.println("Impossible de rafraîchir les alertes de péremption pour le moment.");
        }
    }

    /** Déplace les lots périmés vers la table Perte puis les retire du stock. */
    private static void archiverLotsPerimes() {
        String sqlPerimes = "SELECT lp.IdArticle, lp.date_reception, lp.Qte_dispo, a.Unite " +
                "FROM Lot_Produit lp " +
                "JOIN Art_Pdt a ON a.IdArticle = lp.IdArticle " +
                "WHERE " + SQL_DATE_PEREMPTION + " IS NOT NULL " +
                "AND " + SQL_DATE_PEREMPTION + " < CURRENT_DATE";

        OracleDB db = null;
        try {
            db = new OracleDB();
            Connection conn = db.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psSelect = conn.prepareStatement(sqlPerimes);
                 ResultSet rs = psSelect.executeQuery()) {

                while (rs.next()) {
                    int idArticle = rs.getInt("IdArticle");
                    Date dateReceptionSql = rs.getDate("date_reception");
                    LocalDate dateReception = dateReceptionSql.toLocalDate();
                    int qte = rs.getInt("Qte_dispo");
                    String unite = rs.getString("Unite");

                    int newPerteId = genererNouvellePerteId(conn);
                    try (PreparedStatement psPerte = conn.prepareStatement(
                            "INSERT INTO Perte(IdPerte, datePerte, naturePerte, typePerte, qtePerdue, unite) " +
                                    "VALUES (?, CURRENT_DATE, 'Peremption', 'Article', ?, ?)")) {
                        psPerte.setInt(1, newPerteId);
                        psPerte.setInt(2, qte);
                        psPerte.setString(3, unite);
                        psPerte.executeUpdate();
                    }

                    try (PreparedStatement psDel = conn.prepareStatement(
                            "DELETE FROM Lot_Produit WHERE IdArticle = ? AND date_reception = ?")) {
                        psDel.setInt(1, idArticle);
                        psDel.setDate(2, java.sql.Date.valueOf(dateReception));
                        psDel.executeUpdate();
                    }
                }
            }
            conn.commit();
        } catch (Exception e) {
            try {
                if (db != null && db.getConnection() != null) {
                    db.getConnection().rollback();
                }
            } catch (SQLException ignored) {
            }
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    private static int genererNouvellePerteId(Connection conn) throws SQLException {
        String sql = "SELECT NVL(MAX(IdPerte), 0) + 1 FROM Perte";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 1;
    }

    /** Applique des remises progressives (50% à 90%) selon la proximité de la date de péremption. */
    private static void appliquerRemisesProgressives() {
        LocalDate dateCourante = LocalDate.now();
        LocalDate limite = dateCourante.plusDays(MARGE_TOLERE_EXPIRATION);

        String sql = "SELECT lp.IdArticle, ar.prixTTC, MIN(" + SQL_DATE_PEREMPTION + ") AS peremption " +
                "FROM Lot_Produit lp " +
                "JOIN Article ar ON ar.IdArticle = lp.IdArticle " +
                "WHERE " + SQL_DATE_PEREMPTION + " IS NOT NULL " +
                "AND " + SQL_DATE_PEREMPTION + " <= TO_DATE(?, 'YYYY-MM-DD') " +
                "GROUP BY lp.IdArticle, ar.prixTTC";

        OracleDB db = null;
        try {
            db = new OracleDB();
            Connection conn = db.getConnection();

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, limite.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int idArticle = rs.getInt("IdArticle");
                        java.sql.Date peremptionSql = rs.getDate("peremption");
                        if (peremptionSql == null) {
                            continue;
                        }
                        LocalDate peremptionDate = peremptionSql.toLocalDate();
                        long joursRestants = ChronoUnit.DAYS.between(dateCourante, peremptionDate);
                        long joursClampe = Math.max(0, Math.min(MARGE_TOLERE_EXPIRATION, joursRestants));

                        // Remise progresse de 50% (10 jours) à 90% (le jour J ou après)
                        double facteurPrix = 0.1 + 0.4 * (joursClampe / (double) MARGE_TOLERE_EXPIRATION);

                        double prixActuel = rs.getDouble("prixTTC");
                        double prixBase = prixBaseArticles.computeIfAbsent(idArticle, k -> prixActuel);
                        double prixCible = arrondirDeuxDecimales(prixBase * facteurPrix);

                        if (prixActuel - prixCible > 0.009) {
                            try (PreparedStatement psUpdate = conn.prepareStatement("UPDATE Article SET prixTTC = ? WHERE IdArticle = ?")) {
                                psUpdate.setDouble(1, prixCible);
                                psUpdate.setInt(2, idArticle);
                                psUpdate.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Impossible d'appliquer les remises progressives.");
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    private static double arrondirDeuxDecimales(double valeur) {
        return Math.round(valeur * 100.0) / 100.0;
    }

    public static void menuUtilisateur() {
        while (true) {
            rafraichirNombreAlertes();
            clearScreen();
            System.out.println("===== INTERFACE UTILISATEUR =====");
            System.out.println("1. Consulter nos catalogues");
            System.out.println("2. Soldes / Offres (" + nombreAlertePeremption + ")");

            if (id_client.isEmpty()) {
                System.out.println("3. Passer une commande (connexion requise)");
                System.out.println("4. Inscription / Connexion");
                System.out.println("0. Retour");
            } else if (compteIncomplet) {
                System.out.println("3. Passer une commande (désactivé : compte incomplet)");
                System.out.println("4. Gérer mes informations");
                System.out.println("5. Déconnexion");
                System.out.println("0. Retour");
            } else {
                System.out.println("3. Passer une commande");
                System.out.println("4. Gérer mes informations");
                System.out.println("5. Déconnexion");
                System.out.println("0. Retour");
            }

            System.out.print("Votre choix : ");
            int choix = scanner.nextInt();
            scanner.nextLine();

            switch (choix) {
                case 1 -> menuCatalogue();
                case 2 -> afficherSoldesOffres();
                case 3 -> {
                    if (id_client.isEmpty()) {
                        System.out.println("Vous devez être connecté(e) pour passer une commande.");
                        pause();
                    } else if (compteIncomplet) {
                        System.out.println("Impossible de passer commande tant que vos données personnelles sont incomplètes.");
                        pause();
                    } else {
                        Passer_Commande();
                    }
                }
                case 4 -> {
                    if (id_client.isEmpty()) {
                        connectionDuClient();
                    } else {
                        questionDonnees();
                    }
                }
                case 5 -> {
                    if (!id_client.isEmpty()) {
                        deconnecterClient();
                    } else {
                        System.out.println("Choix invalide !");
                        pause();
                    }
                }
                case 0 -> {
                    return;
                }
                default -> {
                    System.out.println("Choix invalide !");
                    pause();
                }
            }
        }
    }

    public static void menuEpicier() {
        while (true) {
            rafraichirNombreAlertes();
            clearScreen();
            System.out.println("===== INTERFACE ÉPICIER =====");
            System.out.println("1. Consulter nos catalogues");
            System.out.println("2. Alertes de péremption (" + nombreAlertePeremption + ")");
            System.out.println("3. Clôturer / mettre à jour une commande");
            System.out.println("4. Déclarer une perte");
            System.out.println("5. Menu de modification des informations ");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");

            int choix = scanner.nextInt();
            scanner.nextLine();

            switch (choix) {
                case 1 -> menuCatalogue();
                case 2 -> afficherAlertesPeremption();
                case 3 -> cloturerCommande(false);
                case 4 -> declarerPerte();
                case 5 -> menuModification();
                case 0 -> {
                    return;
                }
                default -> {
                    System.out.println("Choix invalide !");
                    pause();
                }
            }
        }
    }

    @Deprecated
    public static void menuPrincipal() {
        menuUtilisateur();
    }

    // ============================
    // SOUS-MENU : CATALOGUE
    // ============================
    public static void menuCatalogue() {
        int choix = -1;
        clearScreen();
        System.out.println("===== CATALOGUES =====");
        System.out.println("1. Légumes");
        System.out.println("2. Fruits");
        System.out.println("3. Produits Laitiers");
        System.out.println("4. Boulangerie");
        System.out.println("5. Boissons");
        System.out.println("6. Épicerie");
        System.out.println("7. Contenants");
        System.out.println("0. Retour");
        System.out.print("Votre choix : ");
        while (choix != 0) {

            choix = scanner.nextInt();
            OracleDB db = new OracleDB();

            switch (choix) {
                case 1:
                    clearScreen();
                    db.runQuery(requetes.get(0));
                    db.close();
                    pause();
                    break;
                case 2:
                    clearScreen();
                    db.runQuery(requetes.get(1));
                    db.close();
                    pause();
                    break;
                case 3:
                    clearScreen();
                    db.runQuery(requetes.get(2));
                    db.close();
                    pause();
                    break;
                case 4:
                    clearScreen();
                    db.runQuery(requetes.get(3));
                    db.close();
                    pause();
                    break;
                case 5:
                    clearScreen();
                    db.runQuery(requetes.get(4));
                    db.close();
                    pause();
                    break;
                case 6:
                    clearScreen();
                    db.runQuery(requetes.get(5));
                    db.close();
                    pause();
                    break;
                case 7:
                    clearScreen();
                    db.runQuery(requetes.get(6));
                    db.close();
                    pause();
                    break;
                case 0:
                    return; // retour au menu principal
                default:
                    System.out.println("Choix invalide !");
                    pause();
            }
        }
    }

    // ============================
    // SOUS-MENU : PASSER UNE COMMANDE
    // ============================


    public static void Passer_Commande() {

        clearScreen();
        System.out.println("===== Passer une commande =====");

        if (id_client.isEmpty()) {
            System.out.println("Vous devez être connecté(e) pour passer une commande.");
            pause();
            return;
        }
        if (compteIncomplet) {
            System.out.println("Complétez vos informations personnelles avant de passer commande.");
            pause();
            return;
        }

        List<LigneCommandeSaisie> commande = new ArrayList<>();
        OracleDB db = new OracleDB();

        String[] categories = {
                "Légumes", "Fruits", "Produits Laitiers", "Boulangerie",
                "Boissons", "Épicerie", "Contenants"
        };

        boolean continuerCatalogues = true;
        double totalCommande = 0.0;

        while (continuerCatalogues) {

            System.out.println("===== CATALOGUES =====");
            for (int i = 0; i < categories.length; i++) {
                System.out.println((i + 1) + ". " + categories[i]);
            }
            System.out.println("0. Terminer la commande et voir le récapitulatif");
            System.out.print("Choisissez la catégorie : ");

            int choix = scanner.nextInt();
            scanner.nextLine();

            if (choix == 0) break;
            if (choix < 0 || choix > categories.length) {
                System.out.println("Choix invalide. Réessayez.");
                continue;
            }

            String categorieChoisie = categories[choix - 1];

            clearScreen();
            String listArticlesSql;

            if (categorieChoisie.equalsIgnoreCase("Contenants")) {
                listArticlesSql =
                        "SELECT c.idArticle, c.type AS nom_produit, c.capacite, c.unite, c.caractere, ar.prixttc " +
                                "FROM Contenant c " +
                                "JOIN Article ar ON ar.idArticle = c.idArticle";
            } else {
                listArticlesSql =
                        "SELECT ar.idArticle, p.nom_produit, p.description, a.MODE_CONDITIONNEMENT, a.UNITE, a.QUANTITE_UNITAIRE, ar.prixttc " +
                                "FROM Art_Pdt a " +
                                "JOIN Article ar ON a.IDARTICLE = ar.IDARTICLE " +
                                "JOIN Produit p ON p.IDPRODUIT = a.IDPRODUIT " +
                                "WHERE p.categorie = '" + categorieChoisie + "'";
            }

            db.runQuery(listArticlesSql);
            System.out.println("\n(Entrez 0 pour revenir au choix de catégorie)");

            while (true) {

                System.out.print("\nChoisissez l'ID de l'article (ou 0 pour revenir au catalogue) : ");
                int idArticle = scanner.nextInt();
                scanner.nextLine();

                if (idArticle == 0) break;

                String qArticle;
                if (categorieChoisie.equalsIgnoreCase("Contenants")) {
                    qArticle =
                            "SELECT ar.idArticle, ar.prixttc, c.type AS nom_produit, 'UNITE' AS MODE_CONDITIONNEMENT " +
                                    "FROM Contenant c " +
                                    "JOIN Article ar ON ar.idArticle = c.idArticle " +
                                    "WHERE ar.idArticle = " + idArticle;
                } else {
                    qArticle =
                            "SELECT ar.idArticle, ar.prixttc, p.nom_produit, a.MODE_CONDITIONNEMENT " +
                                    "FROM Article ar " +
                                    "JOIN Art_Pdt a ON a.IDARTICLE = ar.IDARTICLE " +
                                    "JOIN Produit p ON p.IDPRODUIT = a.IDPRODUIT " +
                                    "WHERE ar.idArticle = " + idArticle;
                }

                String nomProduit = "";
                double prixUnitaire = 0.0;
                String mode = "";

                try {
                    OracleDB.SimpleQuery q = db.new SimpleQuery(qArticle);
                    ResultSet rs = q.executeSelect();
                    if (rs.next()) {
                        prixUnitaire = rs.getDouble("prixttc");
                        nomProduit = rs.getString("nom_produit");
                        mode = rs.getString("MODE_CONDITIONNEMENT");
                    }
                    rs.close();
                } catch (SQLException e) {
                    System.out.println("Erreur lecture article/produit : " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }

                if (nomProduit == null || nomProduit.isEmpty()) {
                    System.out.println("Aucun article trouvé pour cet idArticle=" + idArticle + ". Réessayez.");
                    continue;
                }

                double quantiteDouble = 0.0;
                int quantiteInt = 0;
                String qteAffichage;
                boolean isVrac = mode != null && mode.equalsIgnoreCase("Vrac");

                if (isVrac) {
                    System.out.print("Choisissez la quantité  : ");
                    try {
                        quantiteDouble = scanner.nextDouble();
                        scanner.nextLine();
                        if (quantiteDouble <= 0) {
                            System.out.println("Quantité invalide, doit être > 0.");
                            continue;
                        }
                    } catch (Exception ex) {
                        scanner.nextLine();
                        System.out.println("Entrée invalide. Utilisez une valeur décimale (ex: 1.5).");
                        continue;
                    }
                    qteAffichage = String.format("%.2f", quantiteDouble);
                } else {
                    System.out.print("Nombre d'articles (entier) : ");
                    try {
                        quantiteInt = scanner.nextInt();
                        scanner.nextLine();
                        if (quantiteInt <= 0) {
                            System.out.println("Quantité invalide, doit être > 0.");
                            continue;
                        }
                    } catch (Exception ex) {
                        scanner.nextLine();
                        System.out.println("Entrée invalide. Utilisez un entier (ex: 2).");
                        continue;
                    }
                    qteAffichage = String.valueOf(quantiteInt);
                }

                double quantitePourCalcul = isVrac ? quantiteDouble : (double) quantiteInt;
                LigneCommandeSaisie ligne = new LigneCommandeSaisie(idArticle, nomProduit, quantitePourCalcul, prixUnitaire, qteAffichage);

                commande.add(ligne);
                totalCommande += ligne.sousTotal;

                System.out.println("Ajoute : " + nomProduit + " x" + qteAffichage + " (sous-total: " + String.format("%.2f", ligne.sousTotal) + " €)");
            }

            System.out.println("\nVoulez-vous ajouter des produits d'une autre catégorie ? (O/N) : ");
            String rep = scanner.nextLine().trim();
            if (!rep.equalsIgnoreCase("O")) {
                continuerCatalogues = false;
            }
            clearScreen();
        }

        clearScreen();
        if (commande.isEmpty()) {
            System.out.println("Aucun article sélectionné.");
            db.close();
            pause();
            return;
        }

        System.out.println("===== RÉCAPITULATIF GLOBAL DE VOTRE COMMANDE =====");
        System.out.printf("%-8s %-40s %12s %12s %12s%n", "ID_ART", "NOM_PRODUIT", "QTE", "PU (€)", "SOUS-TOTAL (€)");
        System.out.println("------------------------------------------------------------------------------------------");

        for (LigneCommandeSaisie l : commande) {
            String nom = l.nomProduit;
            System.out.printf("%-8s %-40s %12s %12s %12s%n",
                    l.idArticle,
                    nom.length() > 40 ? nom.substring(0, 37) + "..." : nom,
                    l.quantiteAffichage,
                    String.format("%.2f", l.prixUnitaire),
                    String.format("%.2f", l.sousTotal));
        }

        System.out.println("------------------------------------------------------------------------------------------");
        System.out.printf("%-72s %12s%n", "TOTAL PRODUITS", String.format("%.2f €", totalCommande));

        System.out.println("\nChoisissez le mode de paiement :");
        System.out.println("1 - Paiement en Ligne");
        System.out.println("2 - Paiement en Boutique");
        System.out.print("Votre choix (1/2) : ");
        int choixPaiement = scanner.nextInt();
        scanner.nextLine();

        String modePaiement = choixPaiement == 1 ? "Paiement en Ligne" : "Paiement en Boutique";

        System.out.println("\nChoisissez le mode de récupération :");
        System.out.println("1 - Retrait en Boutique");
        System.out.println("2 - Livraison à Domicile");
        System.out.print("Votre choix (1/2) : ");
        int choixRecuperation = scanner.nextInt();
        scanner.nextLine();
        String modeRecuperation = choixRecuperation == 2 ? "Livraison à Domicile" : "Retrait en Boutique";

        int idClient;
        try {
            idClient = Integer.parseInt(id_client);
        } catch (NumberFormatException e) {
            System.out.println("Identifiant client invalide.");
            db.close();
            pause();
            return;
        }

        Connection conn = db.getConnection();
        if (conn == null) {
            System.out.println("Connexion indisponible.");
            db.close();
            pause();
            return;
        }

        if (!clientExiste(conn, idClient)) {
            System.out.println("Client introuvable.");
            db.close();
            pause();
            return;
        }

        double fraisLivraison = 0.0;
        String adresseLivraison = null;
        if ("Livraison à Domicile".equals(modeRecuperation)) {
            fraisLivraison = calculFraisLivraison(totalCommande);
            System.out.printf("\nFrais de livraisons : %.2f €%n", fraisLivraison);
            try {
                adresseLivraison = choisirAdresseLivraison(conn, idClient);
            } catch (SQLException e) {
                System.out.println("Erreur adresse de livraison : " + e.getMessage());
                db.close();
                pause();
                return;
            }
            if (adresseLivraison == null || adresseLivraison.isBlank()) {
                System.out.println("Adresse requise pour la livraison.");
                db.close();
                pause();
                return;
            }
        }

        executerCommande(db, commande, totalCommande, fraisLivraison, modePaiement, modeRecuperation, "En Préparation", idClient, adresseLivraison);

        double totalAvecLivraison = totalCommande + fraisLivraison;
        if (fraisLivraison > 0) {
            System.out.printf("%-72s %12s%n", "FRAIS DE LIVRAISON", String.format("%.2f €", fraisLivraison));
            System.out.printf("%-72s %12s%n", "TOTAL À PAYER", String.format("%.2f €", totalAvecLivraison));
        }

        System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
        scanner.nextLine();

        db.close();
        pause();
    }

    private static boolean clientExiste(Connection conn, int idClient) {
        String sql = "SELECT 1 FROM Client WHERE IdClient = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Erreur vérification client : " + e.getMessage());
            return false;
        }
    }

    private static double calculFraisLivraison(double totalCommande) {
        if (totalCommande > 50) {
            System.out.println("\nBonne nouvelle ! Livraison offerte au-delà de 50 euros.");
            return 0.0;
        }

        System.out.println("\nType de livraison :");
        System.out.println("1- Standard (2 à 5 jours ouvrés)");
        System.out.println("2- Express (1 à 2 jours)");
        System.out.println("3- Premium (quelques heures)");
        System.out.print("Choisissez le type de livraison : ");
        int choix = scanner.nextInt();
        scanner.nextLine();
        return switch (choix) {
            case 1 -> 0.07 * totalCommande;
            case 2 -> 0.2 * totalCommande;
            case 3 -> 0.5 * totalCommande;
            default -> {
                System.out.println("Choix invalide.");
                yield calculFraisLivraison(totalCommande);
            }
        };
    }

    private static String choisirAdresseLivraison(Connection conn, int idClient) throws SQLException {
        List<String> adresses = new ArrayList<>();
        String sql = "SELECT AdresseLiv FROM Adresse_Livraison WHERE IdClient = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    adresses.add(rs.getString(1));
                }
            }
        }
        if (adresses.isEmpty()) {
            System.out.println("Aucune adresse enregistrée.");
            return saisirNouvelleAdresse(conn, idClient);
        }
        while (true) {
            System.out.println("Sélectionnez l'adresse de livraison :");
            for (int i = 0; i < adresses.size(); i++) {
                System.out.println((i + 1) + " - " + adresses.get(i));
            }
            System.out.println((adresses.size() + 1) + " - Ajouter une nouvelle adresse");
            System.out.print("Votre choix : ");
            int choix = scanner.nextInt();
            scanner.nextLine();
            if (choix >= 1 && choix <= adresses.size()) {
                return adresses.get(choix - 1);
            }
            if (choix == adresses.size() + 1) {
                return saisirNouvelleAdresse(conn, idClient);
            }
            System.out.println("Choix invalide.");
        }
    }

    private static String saisirNouvelleAdresse(Connection conn, int idClient) throws SQLException {
        while (true) {
            System.out.print("Nouvelle adresse : ");
            String adresse = scanner.nextLine().trim();
            if (adresse.isEmpty()) {
                System.out.println("Adresse vide.");
                continue;
            }
            String sql = "INSERT INTO Adresse_Livraison (AdresseLiv, IdClient) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, adresse);
                ps.setInt(2, idClient);
                ps.executeUpdate();
                return adresse;
            } catch (SQLException e) {
                System.out.println("Impossible d'enregistrer l'adresse : " + e.getMessage());
                String state = e.getSQLState();
                if (state == null || !state.equals("23000")) {
                    throw e;
                }
            }
        }
    }

    private static void executerCommande(OracleDB db, List<LigneCommandeSaisie> lignes, double totalProduits, double fraisLivraison, String modePaiement, String modeRecuperation, String statutInitial, int idClient, String adresseLivraison) {
        Connection conn = db.getConnection();
        if (conn == null) {
            System.out.println("Connexion indisponible.");
            return;
        }
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDate dateCourante = maintenant.toLocalDate();
        String heureCourante = maintenant.toLocalTime().format(HEURE_FORMAT);
        double totalGeneral = totalProduits + fraisLivraison;
        boolean livraison = "Livraison à Domicile".equals(modeRecuperation);
        try {
            conn.setAutoCommit(false);
            int idCommande = genererNouvelleCommandeId(conn);
            for (LigneCommandeSaisie ligne : lignes) {
                double disponible = stockDisponible(conn, ligne.idArticle);
                if (disponible < ligne.quantite) {
                    throw new IllegalStateException("Stock insuffisant pour " + ligne.nomProduit + ", il reste : " + disponible);
                }
            }

            String sqlCommande = "INSERT INTO Commande (IdCommande, date_Com, Heure_Com, Prix_Total, Statut, Mode_Paiement, Mode_Recuperation, IdClient) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlCommande)) {
                ps.setInt(1, idCommande);
                ps.setDate(2, java.sql.Date.valueOf(dateCourante));
                ps.setString(3, heureCourante);
                ps.setDouble(4, totalGeneral);
                ps.setString(5, statutInitial);
                ps.setString(6, modePaiement);
                ps.setString(7, modeRecuperation);
                ps.setInt(8, idClient);
                ps.executeUpdate();
            }
            String sqlLigne = "INSERT INTO Ligne_Commande (IdCommande, IdLC, IdArticle, QteCom, PU, Sous_Total) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlLigne)) {
                int idLC = 1;
                for (LigneCommandeSaisie ligne : lignes) {
                    ps.setInt(1, idCommande);
                    ps.setInt(2, idLC++);
                    ps.setInt(3, ligne.idArticle);
                    ps.setDouble(4, ligne.quantite);
                    ps.setDouble(5, ligne.prixUnitaire);
                    ps.setDouble(6, ligne.sousTotal);
                    ps.executeUpdate();
                }
            }
            if (livraison) {
                String sqlLivraison = "INSERT INTO Est_Livrée_A (IdCommande, AdresseLiv) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlLivraison)) {
                    ps.setInt(1, idCommande);
                    ps.setString(2, adresseLivraison);
                    ps.executeUpdate();
                }
            }
            conn.commit();
            System.out.println("Commande créée sous le numéro " + idCommande + ".");
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.out.println("Erreur rollback : " + ex.getMessage());
            }
            System.out.println("Échec de la commande : " + e.getMessage());
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Erreur restauration auto-commit : " + e.getMessage());
            }
        }
    }

    private static int genererNouvelleCommandeId(Connection conn) throws SQLException {
        String sql = "SELECT NVL(MAX(IdCommande), 0) + 1 FROM Commande";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 1;
    }

    private static void reserverStockCommande(Connection conn, int idCommande) throws SQLException {
        ajusterStockCommande(conn, idCommande, true);
    }

    private static void restituerStockCommande(Connection conn, int idCommande) throws SQLException {
        ajusterStockCommande(conn, idCommande, false);
    }

    private static void ajusterStockCommande(Connection conn, int idCommande, boolean retirer) throws SQLException {
        String sqlLignes =
                "SELECT IdArticle, SUM(QteCom) AS QteTotale " +
                        "FROM Ligne_Commande " +
                        "WHERE IdCommande = ? " +
                        "GROUP BY IdArticle";
        try (PreparedStatement psLignes = conn.prepareStatement(sqlLignes)) {
            psLignes.setInt(1, idCommande);
            try (ResultSet rsLignes = psLignes.executeQuery()) {
                while (rsLignes.next()) {
                    int idArt = rsLignes.getInt("IdArticle");
                    double qte = rsLignes.getDouble("QteTotale");
                    ajusterStockArticle(conn, idArt, qte, retirer);
                }
            }
        }
    }

    private static void ajusterStockArticle(Connection conn, int idArticle, double quantite, boolean retirer) throws SQLException {
        String tableLots = trouverTableLots(conn, idArticle);
        String sqlLots = "SELECT date_reception, Qte_dispo FROM " + tableLots + " WHERE IdArticle = ? ORDER BY date_reception";
        List<java.sql.Date> dates = new ArrayList<>();
        List<Double> qtes = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sqlLots)) {
            ps.setInt(1, idArticle);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dates.add(rs.getDate("date_reception"));
                    qtes.add(rs.getDouble("Qte_dispo"));
                }
            }
        }

        if (dates.isEmpty()) {
            throw new SQLException("Aucun lot pour l'article " + idArticle);
        }

        double reste = quantite;

        if (retirer) {
            for (int i = 0; i < dates.size() && reste > 0; i++) {
                double dispo = qtes.get(i);
                double preleve = Math.min(dispo, reste);
                try (PreparedStatement psUpd = conn.prepareStatement(
                        "UPDATE " + tableLots + " SET Qte_dispo = Qte_dispo - ? WHERE IdArticle = ? AND date_reception = ?")) {
                    psUpd.setDouble(1, preleve);
                    psUpd.setInt(2, idArticle);
                    psUpd.setDate(3, dates.get(i));
                    psUpd.executeUpdate();
                }
                reste -= preleve;
            }
            if (reste > 0.0001) {
                throw new SQLException("Stock insuffisant pour l'article " + idArticle);
            }
        } else {
            // on réinjecte la quantité sur le lot le plus ancien
            try (PreparedStatement psUpd = conn.prepareStatement(
                    "UPDATE " + tableLots + " SET Qte_dispo = Qte_dispo + ? WHERE IdArticle = ? AND date_reception = ?")) {
                psUpd.setDouble(1, quantite);
                psUpd.setInt(2, idArticle);
                psUpd.setDate(3, dates.get(0));
                psUpd.executeUpdate();
            }
        }
    }

    private static String trouverTableLots(Connection conn, int idArticle) throws SQLException {
        String sqlProduit = "SELECT COUNT(*) FROM Lot_Produit WHERE IdArticle = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlProduit)) {
            ps.setInt(1, idArticle);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return "Lot_Produit";
                }
            }
        }
        String sqlContenant = "SELECT COUNT(*) FROM Lot_Contenant WHERE IdArticle = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlContenant)) {
            ps.setInt(1, idArticle);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return "Lot_Contenant";
                }
            }
        }
        throw new SQLException("Aucun lot pour l'article " + idArticle);
    }

    private static boolean stockSuffisant(Connection conn, int idArticle, double quantite) throws SQLException {
        return stockDisponible(conn, idArticle) > quantite;
    }

    private static double stockDisponible(Connection conn, int idArticle) throws SQLException {
        return sommeStock(conn, "Lot_Produit", idArticle) + sommeStock(conn, "Lot_Contenant", idArticle);
    }

    private static double sommeStock(Connection conn, String table, int idArticle) throws SQLException {
        String sql = "SELECT NVL(SUM(Qte_dispo), 0) FROM " + table + " WHERE IdArticle = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idArticle);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0;
    }


    // ============================
    // ALERTES DE PEREMPTION / SOLDES
    // ============================
    private static void afficherAlertes(boolean pourClient) {
        clearScreen();
        rafraichirNombreAlertes();
        appliquerRemisesProgressives();
        LocalDate dateCourante = LocalDate.now();
        LocalDate limite = dateCourante.plusDays(MARGE_TOLERE_EXPIRATION);

        String titre = pourClient ? "===== SOLDES / OFFRES =====" : "===== ALERTES DE PÉREMPTION =====";
        System.out.println(titre);
        if (nombreAlertePeremption == 0) {
            System.out.println("Aucun produit concerné pour le moment.");
            pause();
            return;
        }

        String sqlBientot = "SELECT p.nom_produit, lp.Qte_dispo, a.Unite, ar.prixttc, " +
                "TO_CHAR(" + SQL_DATE_PEREMPTION + ", 'YYYY-MM-DD') AS peremption " +
                "FROM Lot_Produit lp " +
                "JOIN Art_Pdt a ON a.IdArticle = lp.IdArticle " +
                "JOIN Produit p ON p.IdProduit = a.IdProduit " +
                "JOIN Article ar ON ar.idArticle = a.idArticle " +
                "WHERE " + SQL_DATE_PEREMPTION + " IS NOT NULL " +
                "AND " + SQL_DATE_PEREMPTION + " BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD') " +
                "ORDER BY peremption";

        String sqlPerimes = "SELECT p.nom_produit, lp.Qte_dispo, a.Unite, ar.prixttc, " +
                "TO_CHAR(" + SQL_DATE_PEREMPTION + ", 'YYYY-MM-DD') AS peremption " +
                "FROM Lot_Produit lp " +
                "JOIN Art_Pdt a ON a.IdArticle = lp.IdArticle " +
                "JOIN Produit p ON p.IdProduit = a.IdProduit " +
                "JOIN Article ar ON ar.idArticle = a.idArticle " +
                "WHERE " + SQL_DATE_PEREMPTION + " IS NOT NULL " +
                "AND " + SQL_DATE_PEREMPTION + " < TO_DATE(?, 'YYYY-MM-DD') " +
                "ORDER BY peremption";

        try {
            OracleDB db = new OracleDB();
            Connection conn = db.getConnection();
            afficherSectionAlertes(conn,
                    "Produits périmant dans les 7 prochains jours",
                    sqlBientot,
                    dateCourante.toString(),
                    limite.toString());
            afficherSectionAlertes(conn,
                    "Produits déjà périmés",
                    sqlPerimes,
                    dateCourante.toString());
            if (pourClient) {
                System.out.println("\nLes réductions sont appliquées automatiquement sur ces articles.");
            } else {
                System.out.println("\nLes ajustements de prix et les pertes sont gérés automatiquement (SystemeEpicerie).");
            }
            db.close();
        } catch (Exception e) {
            System.out.println("Impossible d'afficher les alertes de péremption.");
        }

        pause();
    }

    private static void afficherSectionAlertes(Connection conn, String sectionTitre, String sql, String... params) throws SQLException {
        System.out.println("\n" + sectionTitre);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                afficherTableAlertes(rs);
            }
        }
    }

    private static void afficherTableAlertes(ResultSet rs) throws SQLException {
        System.out.printf("%-40s %-12s %-10s %-14s %-10s%n", "Produit", "Quantité", "Unité", "Péremption", "Prix TTC");
        System.out.println("--------------------------------------------------------------------------------");
        boolean hasRow = false;
        while (rs.next()) {
            hasRow = true;
            String nom = rs.getString("nom_produit");
            double quantite = rs.getDouble("Qte_dispo");
            String unite = rs.getString("Unite");
            String peremption = rs.getString("peremption");
            double prix = rs.getDouble("prixttc");
            System.out.printf("%-40s %-12s %-10s %-14s %-10s%n",
                    tronquerNomProduit(nom),
                    String.format("%.2f", quantite),
                    unite,
                    peremption,
                    String.format("%.2f", prix));
        }
        if (!hasRow) {
            System.out.println("Aucun article.");
        }
    }

    private static String tronquerNomProduit(String nom) {
        if (nom == null) {
            return "";
        }
        return nom.length() > 40 ? nom.substring(0, 37) + "..." : nom;
    }

    public static void afficherSoldesOffres() {
        afficherAlertes(true);
    }

    public static void afficherAlertesPeremption() {
        afficherAlertes(false);
    }

    // ============================
    // GESTION PROFIL
    // ============================
    public static void questionDonnees() {
        clearScreen();
        if (id_client.isEmpty()) {
            System.out.println("Vous devez être connecté(e).");
            return;
        }
        OracleDB db = new OracleDB();
        try {
            if (compteIncomplet) {
                System.out.println("Vos informations sont incomplètes. Souhaitez-vous les renseigner maintenant ? (y/n)");
                if (yesNoQuestion().equals("y")) {
                    String email = mail.isEmpty() ? demanderValeurNonVide("Entrez votre email : ") : mail;
                    creerCompteComplet(db, email);
                } else {
                    System.out.println("Vous pourrez compléter votre profil plus tard.");
                }
                return;
            }
        } catch (SQLException e) {
            System.out.println("Impossible de mettre à jour vos informations : " + e.getMessage());
            return;
        } finally {
            db.close();
        }

        while (true) {
            clearScreen();
            System.out.println("===== MON COMPTE =====");
            System.out.println("1. Modifier mes informations personnelles");
            System.out.println("2. Supprimer mes données");
            System.out.println("3. Consulter mes commandes");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");
            int choix = scanner.nextInt();
            scanner.nextLine();

            switch (choix) {
                case 1 -> mettreAJourInformationsPersonnelles();
                case 2 -> supprimerDonneesClient();
                case 3 -> consulterCommandesClient();
                case 0 -> {
                    return;
                }
                default -> {
                    System.out.println("Choix invalide !");
                    pause();
                }
            }
        }
    }

    private static void anonymiserClient(OracleDB db) throws SQLException {
        Connection conn = db.getConnection();
        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Client_Non_Oublie WHERE IdClient = ?")) {
                ps.setInt(1, Integer.parseInt(id_client));
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Adresse_Livraison WHERE IdClient = ?")) {
                ps.setInt(1, Integer.parseInt(id_client));
                ps.executeUpdate();
            }
            conn.commit();
            id_client = "";
            mail = "";
            compteIncomplet = true;
            System.out.println("Vos informations ont été supprimées.");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    private static void deconnecterClient() {
        id_client = "";
        mail = "";
        compteIncomplet = true;
        System.out.println("Vous êtes maintenant déconnecté(e).");
        pause();
    }



    // ============================
    // CLOTURER UNE COMMANDE
    // ============================

    public static void cloturerCommande(boolean restreintAuClient) {
        clearScreen();
        System.out.println("===== Clôturer une commande =====");

        if (restreintAuClient && Objects.equals(id_client, "")) {
            System.out.println("Vous n'êtes pas connecté(e).");
            pause();
            return;
        }

        OracleDB db = new OracleDB();
        Connection conn = null;

        try {
            conn = db.getConnection();
            conn.setAutoCommit(false);

            String sqlListe =
                    "SELECT IdCommande, date_Com, Statut, Mode_Paiement, Mode_Recuperation, Prix_Total, IdClient " +
                            "FROM Commande " +
                            "WHERE Statut IN ('En Préparation', 'Prête', 'En Livraison')";
            if (restreintAuClient) {
                sqlListe += " AND IdClient = ?";
            }

            PreparedStatement psListe = conn.prepareStatement(sqlListe);
            if (restreintAuClient) {
                psListe.setInt(1, Integer.parseInt(id_client));
            }
            ResultSet rsListe = psListe.executeQuery();

            boolean hasRow = false;
            System.out.printf("%-10s %-12s %-10s %-20s %-20s %-22s %-10s%n",
                    "IdCmd", "Date", "Client", "Statut", "ModePaiement", "ModeRecuperation", "Total");
            while (rsListe.next()) {
                hasRow = true;
                int idCmd = rsListe.getInt("IdCommande");
                java.sql.Date dateCom = rsListe.getDate("date_Com");
                String statut = rsListe.getString("Statut");
                String modePaiement = rsListe.getString("Mode_Paiement");
                String modeRecup = rsListe.getString("Mode_Recuperation");
                double total = rsListe.getDouble("Prix_Total");
                int idClientCommande = rsListe.getInt("IdClient");

                System.out.printf("%-10d %-12s %-10s %-20s %-20s %-22s %-10.2f%n",
                        idCmd, dateCom, idClientCommande, statut, modePaiement, modeRecup, total);
            }

            rsListe.close();
            psListe.close();

            if (!hasRow) {
                System.out.println("Aucune commande à mettre à jour.");
                conn.rollback();
                db.close();
                pause();
                return;
            }

            System.out.print("\nEntrez l'ID de la commande à clôturer (0 pour annuler) : ");
            int idCommande = scanner.nextInt();
            scanner.nextLine();
            if (idCommande == 0) {
                conn.rollback();
                db.close();
                return;
            }

            String sqlCmd =
                    "SELECT Statut, Mode_Paiement, Mode_Recuperation, IdClient " +
                            "FROM Commande WHERE IdCommande = ?";
            PreparedStatement psCmd = conn.prepareStatement(sqlCmd);
            psCmd.setInt(1, idCommande);
            ResultSet rsCmd = psCmd.executeQuery();

            if (!rsCmd.next()) {
                System.out.println("Aucune commande avec cet ID.");
                rsCmd.close();
                psCmd.close();
                conn.rollback();
                db.close();
                pause();
                return;
            }

            int idClientCommande = rsCmd.getInt("IdClient");
            if (restreintAuClient && idClientCommande != Integer.parseInt(id_client)) {
                System.out.println("Cette commande ne vous appartient pas.");
                rsCmd.close();
                psCmd.close();
                conn.rollback();
                db.close();
                pause();
                return;
            }

            String statutActuel = rsCmd.getString("Statut");
            String modePaiement = rsCmd.getString("Mode_Paiement");
            String modeRecup = rsCmd.getString("Mode_Recuperation");

            rsCmd.close();
            psCmd.close();

            System.out.println("\nCommande " + idCommande + " :");
            System.out.println("Client              : " + idClientCommande);
            System.out.println("Statut actuel       : " + statutActuel);
            System.out.println("Mode de paiement    : " + modePaiement);
            System.out.println("Mode de recuperation: " + modeRecup);

            System.out.println("\nQue voulez-vous faire ?");
            System.out.println("1 - Marquer comme 'Prête'");
            System.out.println("2 - Marquer comme 'En Livraison'");
            System.out.println("3 - Marquer comme '" + STATUT_RECUPERE + "'");
            System.out.println("4 - Annuler la commande");
            System.out.print("Votre choix : ");
            int choixAction = scanner.nextInt();
            scanner.nextLine();

            String nouveauStatut;
            switch (choixAction) {
                case 1 -> nouveauStatut = "Prête";
                case 2 -> nouveauStatut = "En Livraison";
                case 3 -> nouveauStatut = STATUT_RECUPERE;
                case 4 -> nouveauStatut = "Annulée";
                default -> {
                    System.out.println("Action invalide.");
                    conn.rollback();
                    db.close();
                    pause();
                    return;
                }
            }

            boolean stockReserveDeja = "Prête".equals(statutActuel)
                    || "En Livraison".equals(statutActuel)
                    || STATUT_RECUPERE.equals(statutActuel);

            boolean besoinReservation = ("Prête".equals(nouveauStatut)
                    || "En Livraison".equals(nouveauStatut)
                    || STATUT_RECUPERE.equals(nouveauStatut))
                    && !stockReserveDeja;

            boolean demandeAnnulation = "Annulée".equals(nouveauStatut);

            if (demandeAnnulation) {
                if (restreintAuClient && !"En Préparation".equals(statutActuel)) {
                    System.out.println("Vous ne pouvez plus annuler cette commande.");
                    conn.rollback();
                    db.close();
                    pause();
                    return;
                }
                if (!restreintAuClient && STATUT_RECUPERE.equals(statutActuel)) {
                    System.out.println("Commande deja livree, annulation impossible.");
                    conn.rollback();
                    db.close();
                    pause();
                    return;
                }
            }

            if (besoinReservation) {
                try {
                    reserverStockCommande(conn, idCommande);
                } catch (SQLException ex) {
                    System.out.println("\nStock insuffisant pour finaliser la commande : " + ex.getMessage());
                    conn.rollback();
                    db.close();
                    pause();
                    return;
                }
            }

            String sqlUpdate =
                    "UPDATE Commande SET Statut = ? WHERE IdCommande = ?";
            PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
            psUpdate.setString(1, nouveauStatut);
            psUpdate.setInt(2, idCommande);
            psUpdate.executeUpdate();
            psUpdate.close();

            if (demandeAnnulation && stockReserveDeja) {
                restituerStockCommande(conn, idCommande);
            }

            conn.commit();
            System.out.println("\nCommande " + idCommande + " mise à jour avec le statut : " + nouveauStatut);

        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ignored) {
            }
            System.out.println("\nErreur SQL lors de la clôture : " + e.getMessage());
        } finally {
            db.close();
        }

        System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
        scanner.nextLine();
    }


    // ============================
    // DECLARER UNE PERTE
    // ============================
    public static void declarerPerte() {
        clearScreen();
        System.out.println("===== Déclarer une perte =====");

        OracleDB db = new OracleDB();
        Connection conn = null;

        try {
            conn = db.getConnection();
            conn.setAutoCommit(false);

            System.out.print("ID de l'article concerné (0 pour annuler) : ");
            int idArticle = scanner.nextInt();
            scanner.nextLine();
            if (idArticle == 0) {
                conn.rollback();
                db.close();
                return;
            }

            String sqlStock = "SELECT SUM(lp.Qte_dispo) AS stock_total, MIN(a.Unite) AS Unite " +
                    "FROM Lot_Produit lp " +
                    "JOIN Art_Pdt a ON a.IdArticle = lp.IdArticle " +
                    "WHERE lp.IdArticle = ? " +
                    "GROUP BY lp.IdArticle";
            PreparedStatement psStock = conn.prepareStatement(sqlStock);
            psStock.setInt(1, idArticle);
            ResultSet rsStock = psStock.executeQuery();

            if (!rsStock.next()) {
                System.out.println("Aucun stock trouvé pour cet article.");
                rsStock.close();
                psStock.close();
                conn.rollback();
                db.close();
                pause();
                return;
            }

            double stockTotal = rsStock.getDouble("stock_total");
            String unite = rsStock.getString("Unite");

            rsStock.close();
            psStock.close();

            if (stockTotal <= 0) {
                System.out.println("Stock nul pour cet article.");
                conn.rollback();
                db.close();
                pause();
                return;
            }

            System.out.println("Stock disponible : " + stockTotal + " " + unite);
            System.out.print("Quantité à déclarer en perte : ");
            double qtePerdue = scanner.nextDouble();
            scanner.nextLine();

            if (qtePerdue <= 0) {
                System.out.println("Quantité invalide.");
                conn.rollback();
                db.close();
                pause();
                return;
            }
            if (qtePerdue > stockTotal) {
                System.out.println("Quantité demandée supérieure au stock, ajustement à " + stockTotal);
                qtePerdue = stockTotal;
            }

            System.out.print("Nature de la perte (vol, casse, etc.) : ");
            String naturePerte = scanner.nextLine().trim();
            if (naturePerte.isEmpty()) {
                naturePerte = "Non précisée";
            }

            int idPerte = SystemeEpicerie.genererNouvellePerteId(db);

            String insertPerte = "INSERT INTO Perte(IdPerte, datePerte, naturePerte, typePerte, qtePerdue, unite) " +
                    "VALUES (?, CURRENT_DATE, ?, 'Article', ?, ?)";
            PreparedStatement psPerte = conn.prepareStatement(insertPerte);
            psPerte.setInt(1, idPerte);
            psPerte.setString(2, naturePerte);
            psPerte.setDouble(3, qtePerdue);
            psPerte.setString(4, unite);
            psPerte.executeUpdate();
            psPerte.close();

            String sqlLots = "SELECT date_reception, Qte_dispo " +
                    "FROM Lot_Produit WHERE IdArticle = ? ORDER BY date_reception";
            PreparedStatement psLots = conn.prepareStatement(sqlLots);
            psLots.setInt(1, idArticle);
            ResultSet rsLots = psLots.executeQuery();

            double restant = qtePerdue;
            while (rsLots.next() && restant > 0) {
                double qteLot = rsLots.getDouble("Qte_dispo");
                java.sql.Date dateReception = rsLots.getDate("date_reception");
                double aRetirer = Math.min(qteLot, restant);

                String sqlUpdateLot = "UPDATE Lot_Produit SET Qte_dispo = Qte_dispo - ? WHERE IdArticle = ? AND date_reception = ?";
                PreparedStatement psUpdateLot = conn.prepareStatement(sqlUpdateLot);
                psUpdateLot.setDouble(1, aRetirer);
                psUpdateLot.setInt(2, idArticle);
                psUpdateLot.setDate(3, dateReception);
                psUpdateLot.executeUpdate();
                psUpdateLot.close();

                restant -= aRetirer;
            }
            rsLots.close();
            psLots.close();

            conn.commit();
            System.out.println("Perte déclarée (ID " + idPerte + ") pour l'article " + idArticle + ".");
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ignored) {
            }
            System.out.println("Erreur lors de la déclaration de perte : " + e.getMessage());
        } finally {
            db.close();
        }

        System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
        scanner.nextLine();
    }


    // ============================
    // UTILITAIRES
    // ============================
    public static void pause() {
        System.out.println("\nAppuyez sur ENTER pour quitter...");
        try { System.in.read(); } catch (Exception e) {}
    }

    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            System.out.println("Impossible de nettoyer l’écran.");
        }
    }

    public static int demanderEntier(String message) {
        while (true) {
            System.out.print(message);
            String rep = scanner.nextLine().trim();

            try {
                return Integer.parseInt(rep);
            }
            catch (NumberFormatException e) {
                System.out.println(" Veuillez entrer un nombre valide.");
            }
        }
    }



    public static void menuModification() {

        while (true) {
            clearScreen();

            System.out.println("===== MENU MODIFICATION / ADMINISTRATION =====");
            System.out.println("1. Modifier les PRODUITS");
            System.out.println("2. Modifier les PRODUCTEURS");
            System.out.println("3. Modifier les CONTENANTS");
            System.out.println("4. Modifier les LOTS d'ARTICLES de PRODUITS");
            System.out.println("5. Modifier les LOTS de CONTENANTS");
            System.out.println("6. Modifier les ARTICLES de PRODUITS");
            System.out.println("0. Retour au menu principal");
            System.out.print("Votre choix : ");

            String input = scanner.nextLine().trim();
            int choix;

            try {
                choix = Integer.parseInt(input);
            } catch (Exception e) {
                choix = -1;
            }

            switch (choix) {

                case 1:
                    modProduit();   // <-- tu l’as déjà
                    break;

                case 2:
                    modProd();      // <-- tu l’as déjà
                    break;

                case 3:
                    modContenant(); // <-- je peux te le coder si tu veux
                    break;

                case 4:
                    modLotProduit(); // <-- je te le code si tu veux
                    break;

                case 5:
                    modLotContenant(); // <-- je te le code si tu veux
                    break;
                case 6:
                    modArticleProd();
                    break;

                case 0:
                    return; // <-- retour propre au menu principal

                default:
                    System.out.println("Choix invalide, veuillez réessayer.");
                    pause();
            }
        }
    }

    public static void modProduit() {
        clearScreen();
        System.out.println("Voulez vous ajouter un produit ? (y/n)");
        String reponse = yesNoQuestion();
        if (reponse.equals("y")) {
            ajoutProduit();
        }
        System.out.println("Voulez vous supprimer un produit de votre liste ? (y/n)");
        String suppProd = yesNoQuestion();
        if (suppProd.equals("y")) {
            suppProduit();
        }
        pause();
    }

    public static void ajoutProduit() {
        clearScreen();
        System.out.println("Enregistrement d'un produit (obligatoire).");

        OracleDB db = new OracleDB();

        try {
            Connection conn = db.getConnection();
            conn.setAutoCommit(false);

            // Génération ID produit
            String sql = "SELECT NVL(MAX(idProduit), 0) + 1 AS newId FROM Produit";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            rs.next();
            int newIdProduit = rs.getInt("newId");
            rs.close();
            stmt.close();

            // Saisie produit
            String nomProduit = demanderValeurNonVide("Nom du produit : ");
            String categorie = demanderValeurNonVide("Categorie : ");
            String description = demanderValeurNonVide("Description : ");
            String carSpe = demanderValeurNonVide("Caractéristiques spéciales : ");

            System.out.println("Producteurs existants :");
            db.runQuery("SELECT email_producteur, nom FROM Producteur");

            // Email producteur
            String mailProd = "";
            while (mailProd.isEmpty()) {
                mailProd = demanderValeurNonVide("Email du producteur : ");
                PreparedStatement checkProd = conn.prepareStatement(
                        "SELECT 1 FROM Producteur WHERE email_producteur = ?"
                );
                checkProd.setString(1, mailProd);
                ResultSet rp = checkProd.executeQuery();

                if (!rp.next()) {
                    System.out.println("Producteur introuvable.");
                    mailProd = "";
                }

                rp.close();
                checkProd.close();
            }

            // Insertion produit
            String sqlInsert = """
            INSERT INTO Produit(IdProduit, nom_Produit, categorie, description, CaracteristiquesSpe, email_Producteur)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

            PreparedStatement insert = conn.prepareStatement(sqlInsert);
            insert.setInt(1, newIdProduit);
            insert.setString(2, nomProduit);
            insert.setString(3, categorie);
            insert.setString(4, description);
            insert.setString(5, carSpe);
            insert.setString(6, mailProd);
            insert.executeUpdate();
            insert.close();

            // Obligation d'ajouter au moins une période
            boolean auMoinsUnePeriode = false;

            while (!auMoinsUnePeriode) {

                System.out.println("Enregistrement d'une période de disponibilité obligatoire.");

                String dateDeb = demanderValeurNonVide("Date début (YYYY-MM-DD) : ");
                String dateFin = demanderValeurNonVide("Date fin (YYYY-MM-DD) : ");

                // Vérifier validité dateDeb < dateFin
                PreparedStatement testDates = conn.prepareStatement(
                        "SELECT CASE WHEN TO_DATE(?, 'YYYY-MM-DD') < TO_DATE(?, 'YYYY-MM-DD') THEN 1 ELSE 0 END AS ok FROM dual"
                );
                testDates.setString(1, dateDeb);
                testDates.setString(2, dateFin);
                ResultSet rtest = testDates.executeQuery();
                rtest.next();

                if (rtest.getInt("ok") == 0) {
                    System.out.println("Intervalle de dates invalide.");
                    rtest.close();
                    testDates.close();
                    continue;
                }

                rtest.close();
                testDates.close();

                // Vérifier si saison existe
                PreparedStatement checkSeason = conn.prepareStatement(
                        "SELECT 1 FROM Saisonalite WHERE date_debut = TO_DATE(?, 'YYYY-MM-DD') AND date_fin = TO_DATE(?, 'YYYY-MM-DD')"
                );
                checkSeason.setString(1, dateDeb);
                checkSeason.setString(2, dateFin);
                ResultSet rsSeason = checkSeason.executeQuery();

                boolean saisonExiste = rsSeason.next();
                rsSeason.close();
                checkSeason.close();

                // Si elle n’existe pas, on la crée
                if (!saisonExiste) {
                    PreparedStatement insSeason = conn.prepareStatement(
                            "INSERT INTO Saisonalite(date_debut, date_fin) VALUES (TO_DATE(?, 'YYYY-MM-DD'), TO_DATE(?, 'YYYY-MM-DD'))"
                    );
                    insSeason.setString(1, dateDeb);
                    insSeason.setString(2, dateFin);
                    insSeason.executeUpdate();
                    insSeason.close();
                }

                // Insert dans Est_Disponible
                PreparedStatement insED = conn.prepareStatement("""
                INSERT INTO Est_Disponible(IdProduit, date_debut, date_fin)
                VALUES (?, TO_DATE(?, 'YYYY-MM-DD'), TO_DATE(?, 'YYYY-MM-DD'))
            """);
                insED.setInt(1, newIdProduit);
                insED.setString(2, dateDeb);
                insED.setString(3, dateFin);
                insED.executeUpdate();
                insED.close();

                auMoinsUnePeriode = true;

                System.out.println("Période enregistrée.");
                System.out.println("Ajouter une autre période ? (y/n)");
                if (yesNoQuestion().equals("y")) {
                    auMoinsUnePeriode = true;
                } else {
                    break;
                }
            }

            conn.commit();
            System.out.println("Produit et disponibilités enregistrés.");
        }
        catch (Exception e) {
            System.out.println("Erreur lors de l'enregistrement du produit.");
            try { db.getConnection().rollback(); } catch (Exception ignored) {}
        }

        db.close();
        pause();
    }

    public static void suppProduit() {
        clearScreen();
        OracleDB db = new OracleDB();

        try {
            Connection conn = db.getConnection();
            conn.setAutoCommit(false);

            db.runQuery("SELECT IdProduit, nom_Produit FROM Produit");

            int idProduit = demanderEntier("Id du produit à supprimer : ");

            // Vérifier existence produit
            PreparedStatement chkProd = conn.prepareStatement(
                    "SELECT 1 FROM Produit WHERE IdProduit = ?"
            );
            chkProd.setInt(1, idProduit);
            ResultSet rs = chkProd.executeQuery();

            if (!rs.next()) {
                System.out.println("Ce produit n'existe pas.");
                rs.close();
                chkProd.close();
                db.close();
                pause();
                return;
            }
            rs.close();
            chkProd.close();

            // Vérifier si des articles de ce produit apparaissent dans des commandes
            PreparedStatement chkCmd = conn.prepareStatement("""
                SELECT 1
                FROM Ligne_Commande
                WHERE IdArticle IN (
                    SELECT IdArticle
                    FROM Art_Pdt
                    WHERE IdProduit = ?
                )
            """);
            chkCmd.setInt(1, idProduit);
            ResultSet rcmd = chkCmd.executeQuery();

            if (rcmd.next()) {
                System.out.println("Impossible de supprimer ce produit : au moins un article associé apparaît dans des commandes.");
                rcmd.close();
                chkCmd.close();
                db.close();
                pause();
                return;
            }
            rcmd.close();
            chkCmd.close();

            // Supprimer Est_Disponible (optionnel car ON DELETE CASCADE, mais on le fait clairement)
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Est_Disponible WHERE IdProduit = ?"
            )) {
                ps.setInt(1, idProduit);
                ps.executeUpdate();
            }

            // Récupérer les articles liés à ce produit
            PreparedStatement getArt = conn.prepareStatement(
                    "SELECT IdArticle FROM Art_Pdt WHERE IdProduit = ?"
            );
            getArt.setInt(1, idProduit);
            ResultSet rArt = getArt.executeQuery();

            ArrayList<Integer> articles = new ArrayList<>();
            while (rArt.next()) {
                articles.add(rArt.getInt(1));
            }
            rArt.close();
            getArt.close();

            // Pour chaque article lié : supprimer toute la partie interne
            for (int idArt : articles) {

                // Subit
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Subit WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }

                // Donne_Lieu_A
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Donne_Lieu_A WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }

                // Lot_Produit
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Lot_Produit WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }

                // Lot_Contenant (au cas où, même si normalement ce sont les contenants)
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Lot_Contenant WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }

                // Contenant
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Contenant WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }

                // Art_Pdt
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Art_Pdt WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }

                // Article
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Article WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }
            }

            // Enfin, suppression du produit lui-même
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Produit WHERE IdProduit = ?"
            )) {
                ps.setInt(1, idProduit);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("Produit supprimé.");

        } catch (SQLException e) {
            System.out.println("Erreur pendant la suppression du produit : " + e.getMessage());
            try {
                db.getConnection().rollback();
            } catch (SQLException ignored) {
            }
        }

        db.close();
        pause();
    }



    public static void modProd() {
        clearScreen();
        System.out.println("Voulez vous ajouter un producteur ? (y/n)");
        String reponse = yesNoQuestion();
        if (reponse.equals("y")) {
            ajoutProd();
        }
        System.out.println("Voulez vous supprimer un producteur de votre liste ? (y/n)");
        String suppProd = yesNoQuestion();
        if (suppProd.equals("y")) {
            suppProd();
        }
        pause();
    }

    public static void ajoutProd() {
        clearScreen();
        OracleDB db = new OracleDB();

        try {
            db.getConnection().setAutoCommit(false);
            System.out.println("Voici les producteurs :");
            db.runQuery("SELECT * FROM Producteur");

            String mailProd = "";

            while (mailProd.equals("")) {
                mailProd = demanderValeurNonVide("Quel est l'email du producteur à ajouter ?");

                String sqlCheck = "SELECT 1 FROM Producteur WHERE email_producteur = ?";
                PreparedStatement ps = db.getConnection().prepareStatement(sqlCheck);
                ps.setString(1, mailProd);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    System.out.println("Ce mail existe déjà. Continuer ? (y/n)");
                    if (yesNoQuestion().equals("n")) {
                        rs.close();
                        ps.close();
                        db.close();
                        return;
                    }
                    System.out.println("Veuillez entrer un email non utilisé.");
                    mailProd = "";
                }

                rs.close();
                ps.close();
            }

            String nomProd = demanderValeurNonVide("Nom du producteur :");

            String numProd = demanderValeurNonVide("Numéro de téléphone :");

            String addrProd = demanderValeurNonVide("Adresse :");

            String GeoLoc = demanderValeurNonVide("Coordonnées géographiques :");

            String sqlInsert = """
            INSERT INTO Producteur(email_producteur, nom, numTel, address, GeoLoc)
            VALUES (?, ?, ?, ?, ?)
        """;

            PreparedStatement smt = db.getConnection().prepareStatement(sqlInsert);
            smt.setString(1, mailProd);
            smt.setString(2, nomProd);
            smt.setString(3, numProd);
            smt.setString(4, addrProd);
            smt.setString(5, GeoLoc);

            smt.executeUpdate();
            db.getConnection().commit();
            smt.close();

            System.out.println("Producteur ajouté !");
        }
        catch (SQLException e) {
            System.out.println("Erreur. Producteur non ajouté.");
            try { db.getConnection().rollback(); } catch (SQLException ignored) {}
        }

        db.close();
    }

    static void suppProd() {
        clearScreen();
        OracleDB db = new OracleDB();

        try {
            Connection conn = db.getConnection();
            conn.setAutoCommit(false);

            db.runQuery("SELECT email_producteur, nom FROM Producteur");

            String mailProd = "";

            // Boucle pour demander un email existant ou abandonner
            while (mailProd.isEmpty()) {
                mailProd = demanderValeurNonVide("Entrer le mail du producteur à enlever : ");

                String sqlCheck = "SELECT 1 FROM Producteur WHERE email_producteur = ?";
                PreparedStatement chk = conn.prepareStatement(sqlCheck);
                chk.setString(1, mailProd);
                ResultSet rs = chk.executeQuery();

                if (!rs.next()) {
                    System.out.println("Ce producteur n'existe pas.");
                    rs.close();
                    chk.close();

                    System.out.println("Voulez-vous réessayer ? (y/n)");
                    if (yesNoQuestion().equals("n")) {
                        db.close();
                        return;
                    }
                    mailProd = "";
                } else {
                    rs.close();
                    chk.close();
                }
            }

            // Récupérer tous les articles liés à ce producteur via ses produits
            PreparedStatement getArt = conn.prepareStatement("""
                SELECT a.IdArticle
                FROM Article a
                JOIN Art_Pdt ap ON ap.IdArticle = a.IdArticle
                JOIN Produit p ON p.IdProduit = ap.IdProduit
                WHERE p.email_producteur = ?
            """);
            getArt.setString(1, mailProd);
            ResultSet rArt = getArt.executeQuery();

            ArrayList<Integer> articles = new ArrayList<>();
            while (rArt.next()) {
                articles.add(rArt.getInt(1));
            }
            rArt.close();
            getArt.close();

            // Vérifier si au moins un de ces articles est utilisé dans des commandes
            PreparedStatement chkCmd = conn.prepareStatement(
                    "SELECT 1 FROM Ligne_Commande WHERE IdArticle = ?"
            );

            for (int idArt : articles) {
                chkCmd.setInt(1, idArt);
                ResultSet rc = chkCmd.executeQuery();
                if (rc.next()) {
                    System.out.println("Impossible de supprimer ce producteur : au moins un de ses articles apparaît dans des commandes.");
                    rc.close();
                    chkCmd.close();
                    db.close();
                    pause();
                    return;
                }
                rc.close();
            }
            chkCmd.close();

            // Supprimer Est_Disponible pour ses produits
            try (PreparedStatement ps = conn.prepareStatement("""
                DELETE FROM Est_Disponible
                WHERE IdProduit IN (
                    SELECT IdProduit FROM Produit WHERE email_producteur = ?
                )
            """)) {
                ps.setString(1, mailProd);
                ps.executeUpdate();
            }

            // Supprimer tout ce qui est interne pour chaque article
            for (int idArt : articles) {

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Subit WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Donne_Lieu_A WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Lot_Produit WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Lot_Contenant WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Contenant WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Art_Pdt WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Article WHERE IdArticle = ?"
                )) {
                    ps.setInt(1, idArt);
                    ps.executeUpdate();
                }
            }

            // Supprimer les produits du producteur
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Produit WHERE email_producteur = ?"
            )) {
                ps.setString(1, mailProd);
                ps.executeUpdate();
            }

            // Supprimer ses activités Exerce
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Exerce WHERE email_producteur = ?"
            )) {
                ps.setString(1, mailProd);
                ps.executeUpdate();
            }

            // Enfin, supprimer le producteur lui-même
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Producteur WHERE email_producteur = ?"
            )) {
                ps.setString(1, mailProd);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("Producteur supprimé avec toutes ses données internes.");

        } catch (SQLException e) {
            System.out.println("Erreur pendant la suppression du producteur : " + e.getMessage());
            try {
                db.getConnection().rollback();
            } catch (SQLException ignored) {
            }
        }

        db.close();
        pause();
    }



    public static void modArticleProd() {
        clearScreen();
        System.out.println("Voulez vous ajouter un article produit ? (y/n)");
        String reponse = yesNoQuestion();
        if (reponse.equals("y")) {
            ajoutArticleProduit();
        }
        System.out.println("Voulez vous supprimer un article produit  de votre liste ? (y/n)");
        String suppProd = yesNoQuestion();
        if (suppProd.equals("y")) {
            suppArticleProduit();
        }
        pause();
    }

    public static void ajoutArticleProduit() {
        clearScreen();
        OracleDB db = new OracleDB();

        try {
            db.getConnection().setAutoCommit(false);

            System.out.println("Liste des produits disponibles :");
            db.runQuery("SELECT IdProduit, nom_Produit FROM Produit");

            // 1) Choix produit
            int idProduit;
            while (true) {
                idProduit = demanderEntier("Id du produit : ");

                PreparedStatement ps = db.getConnection().prepareStatement(
                        "SELECT 1 FROM Produit WHERE IdProduit = ?"
                );
                ps.setInt(1, idProduit);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    rs.close();
                    ps.close();
                    break;
                }

                rs.close();
                ps.close();

                System.out.println("Ce produit n'existe pas. Voulez-vous réessayer ? (y/n)");
                if (yesNoQuestion().equals("n")) {
                    db.close();
                    return;
                }
            }

            // 2) Génération IdArticle
            String sqlId = "SELECT NVL(MAX(IdArticle), 0) + 1 AS newId FROM Article";
            ResultSet rsId = db.getConnection().createStatement().executeQuery(sqlId);
            rsId.next();
            int newIdArticle = rsId.getInt("newId");
            rsId.close();

            // 3) Mode conditionnement
            String mode = "";
            while (!(mode.equals("Vrac") || mode.equals("Pré-Conditionné"))) {
                mode = demanderValeurNonVide("Mode (Vrac / Pré-Conditionné) : ").trim();
                if (!(mode.equals("Vrac") || mode.equals("Pré-Conditionné"))) {
                    System.out.println("Valeur invalide, veuillez saisir Vrac ou Pré-Conditionné.");
                }
            }

            // 4) Quantité unitaire
            double quantite = -1;
            while (quantite <= 0) {
                try {
                    quantite = Double.parseDouble(
                            demanderValeurNonVide("Quantité unitaire (> 0) : ")
                    );
                } catch (Exception e) {
                    quantite = -1;
                }
                if (quantite <= 0) {
                    System.out.println("La quantité doit être un nombre positif.");
                }
            }

            // 5) Unité
            String unite = "";
            while (!(unite.equals("g") || unite.equals("L") || unite.equals("unité"))) {
                unite = demanderValeurNonVide("Unité (g / L / unité) : ").trim();
                if (!(unite.equals("g") || unite.equals("L") || unite.equals("unité"))) {
                    System.out.println("Unité invalide.");
                }
            }

            // 6) Délai disponibilité
            int delai = -1;
            while (delai <= 0) {
                delai = demanderEntier("Délai de disponibilité (> 0 jours) : ");
                if (delai <= 0) {
                    System.out.println("Le délai doit être strictement positif.");
                }
            }

            // 7) Type dispo
            String typeDispo = "";
            while (!(typeDispo.equals("Sur Commande") ||
                    typeDispo.equals("En Stock") ||
                    typeDispo.equals("Sur Commande et En Stock"))) {

                System.out.println("Types possibles :");
                System.out.println("1) En Stock");
                System.out.println("2) Sur Commande");
                System.out.println("3) Sur Commande et En Stock");

                int choix = demanderEntier("Votre choix : ");

                switch (choix) {
                    case 1 -> typeDispo = "En Stock";
                    case 2 -> typeDispo = "Sur Commande";
                    case 3 -> typeDispo = "Sur Commande et En Stock";
                    default -> System.out.println("Choix invalide.");
                }
            }

            // 8) Prix (Achat + TTC) / sans contrainte TTC > achat, juste > 0
            double prixAchat = -1;
            while (prixAchat <= 0) {
                try {
                    prixAchat = Double.parseDouble(
                            demanderValeurNonVide("Prix d'achat (>0) : ")
                    );
                } catch (Exception e) {
                    prixAchat = -1;
                }
                if (prixAchat <= 0) {
                    System.out.println("Prix d'achat invalide.");
                }
            }

            double prixTTC = -1;
            while (prixTTC <= 0) {
                try {
                    prixTTC = Double.parseDouble(
                            demanderValeurNonVide("Prix TTC (>0) : ")
                    );
                } catch (Exception e) {
                    prixTTC = -1;
                }
                if (prixTTC <= 0) {
                    System.out.println("Prix TTC invalide.");
                }
            }

            // INSERT Article
            PreparedStatement pA = db.getConnection().prepareStatement(
                    "INSERT INTO Article(IdArticle, prixAchat, prixTTC) VALUES (?, ?, ?)"
            );
            pA.setInt(1, newIdArticle);
            pA.setDouble(2, prixAchat);
            pA.setDouble(3, prixTTC);
            pA.executeUpdate();
            pA.close();

            // INSERT Art_Pdt
            PreparedStatement psAdd = db.getConnection().prepareStatement("""
                INSERT INTO Art_Pdt(IdArticle, IdProduit, Mode_Conditionnement,
                                    Quantite_Unitaire, Unite, Delai_Dispo, Type_Dispo)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """);
            psAdd.setInt(1, newIdArticle);
            psAdd.setInt(2, idProduit);
            psAdd.setString(3, mode);
            psAdd.setDouble(4, quantite);
            psAdd.setString(5, unite);
            psAdd.setInt(6, delai);
            psAdd.setString(7, typeDispo);
            psAdd.executeUpdate();
            psAdd.close();

            db.getConnection().commit();
            System.out.println("Article-produit ajouté.");

        } catch (Exception e) {
            System.out.println("Erreur lors de l'ajout d'un article-produit : " + e.getMessage());
            try {
                db.getConnection().rollback();
            } catch (Exception ignore) {}
        }

        db.close();
        pause();
    }

    public static void suppArticleProduit() {
        clearScreen();
        OracleDB db = new OracleDB();

        try {
            db.getConnection().setAutoCommit(false);

            System.out.println("Articles-produit existants :");
            db.runQuery("SELECT IdArticle, IdProduit, Mode_Conditionnement FROM Art_Pdt");

            int idArticle = demanderEntier("Entrez l'IdArticle à supprimer : ");

            // Vérifier existence dans Art_Pdt
            PreparedStatement check = db.getConnection().prepareStatement(
                    "SELECT 1 FROM Art_Pdt WHERE IdArticle = ?"
            );
            check.setInt(1, idArticle);
            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                System.out.println("Cet article-produit n'existe pas.");
                rs.close();
                check.close();
                db.close();
                pause();
                return;
            }
            rs.close();
            check.close();

            // Vérifier utilisation dans des commandes
            PreparedStatement checkCmd = db.getConnection().prepareStatement(
                    "SELECT 1 FROM Ligne_Commande WHERE IdArticle = ?"
            );
            checkCmd.setInt(1, idArticle);
            ResultSet rsCmd = checkCmd.executeQuery();

            if (rsCmd.next()) {
                System.out.println("Impossible de supprimer cet article : il figure dans des commandes.");
                rsCmd.close();
                checkCmd.close();
                db.close();
                pause();
                return;
            }
            rsCmd.close();
            checkCmd.close();

            // Vérifier présence de lots
            PreparedStatement checkLot = db.getConnection().prepareStatement(
                    "SELECT 1 FROM Lot_Produit WHERE IdArticle = ?"
            );
            checkLot.setInt(1, idArticle);
            ResultSet rsLot = checkLot.executeQuery();

            if (rsLot.next()) {
                System.out.println("Impossible : cet article possède encore des lots de produit.");
                System.out.println("Supprime d'abord les lots de ce produit.");
                rsLot.close();
                checkLot.close();
                db.close();
                pause();
                return;
            }
            rsLot.close();
            checkLot.close();

            // Vérifier s'il est un Contenant
            PreparedStatement checkCont = db.getConnection().prepareStatement(
                    "SELECT 1 FROM Contenant WHERE IdArticle = ?"
            );
            checkCont.setInt(1, idArticle);
            ResultSet rsCont = checkCont.executeQuery();

            if (rsCont.next()) {
                System.out.println("Cet article correspond à un contenant.");
                System.out.println("Utilisez la fonction de suppression de contenant.");
                rsCont.close();
                checkCont.close();
                db.close();
                pause();
                return;
            }
            rsCont.close();
            checkCont.close();

            // Suppression Art_Pdt
            PreparedStatement delAP = db.getConnection().prepareStatement(
                    "DELETE FROM Art_Pdt WHERE IdArticle = ?"
            );
            delAP.setInt(1, idArticle);
            delAP.executeUpdate();
            delAP.close();

            // Suppression Article
            PreparedStatement delA = db.getConnection().prepareStatement(
                    "DELETE FROM Article WHERE IdArticle = ?"
            );
            delA.setInt(1, idArticle);
            delA.executeUpdate();
            delA.close();

            db.getConnection().commit();
            System.out.println("Article-produit supprimé.");

        } catch (SQLException e) {
            System.out.println("Erreur suppression article-produit : " + e.getMessage());
            try {
                db.getConnection().rollback();
            } catch (Exception ignored) {
            }
        }

        db.close();
        pause();
    }



    public static void modLotProduit() {
        clearScreen();
        System.out.println("Voulez vous ajouter un lot produit ? (y/n)");
        String reponse = yesNoQuestion();
        if (reponse.equals("y")) {
            ajoutLotProduit();
        }
        System.out.println("Voulez vous modifier la quantité d'un lot produit ? (y/n)");
        String modrep = yesNoQuestion();
        if (modrep.equals("y")) {
            modificationLotProduit();
        }
        System.out.println("Voulez vous supprimer un article lot  de votre liste ? (y/n)");
        String suppProd = yesNoQuestion();
        if (suppProd.equals("y")) {
            suppLotProduit();
        }
        pause();
    }

    public static void ajoutLotProduit() {
        clearScreen();
        OracleDB db = new OracleDB();

        try {
            db.getConnection().setAutoCommit(false);
            System.out.println("Articles disponibles :");
            db.runQuery("SELECT * FROM Article");

            int idArticle = demanderEntier("IdArticle du lot : ");

            // Vérifier si Article existe
            PreparedStatement check = db.getConnection().prepareStatement(
                    "SELECT 1 FROM Article WHERE IdArticle = ?"
            );
            check.setInt(1, idArticle);
            ResultSet rc = check.executeQuery();

            if (!rc.next()) {
                System.out.println("Cet article n'existe pas.");
                rc.close();
                check.close();
                db.close();
                return;
            }
            rc.close();
            check.close();

            double qte = -1;
            while (qte <= 0) {
                try {
                    qte = Double.parseDouble(
                            demanderValeurNonVide("Quantité disponible : ")
                    );
                } catch (Exception e) {
                    qte = -1;
                }
                if (qte <= 0) System.out.println("Quantité invalide.");
            }

            String dateRec = demanderValeurNonVide("Date réception (YYYY-MM-DD) : ");
            String datePer = demanderValeurNonVide("Date péremption (YYYY-MM-DD) : ");

            String type = "";
            while (!type.equals("DLC") && !type.equals("DLUO")) {
                type = demanderValeurNonVide("Type date (DLC / DLUO) : ");
            }

            String sql = """
            INSERT INTO Lot_Produit(IdArticle, date_reception, date_peremption, type_date, Qte_dispo)
            VALUES (?, TO_DATE(?, 'YYYY-MM-DD'), TO_DATE(?, 'YYYY-MM-DD'), ?, ?)
        """;

            PreparedStatement ps = db.getConnection().prepareStatement(sql);
            ps.setInt(1, idArticle);
            ps.setString(2, dateRec);
            ps.setString(3, datePer);
            ps.setString(4, type);
            ps.setDouble(5, qte);

            ps.executeUpdate();
            ps.close();
            db.getConnection().commit();

            System.out.println("Lot produit ajouté !");
        }
        catch (Exception e) {
            System.out.println("Erreur ajout lot produit : " + e.getMessage());
            try { db.getConnection().rollback(); } catch (Exception ignore) {}
        }

        db.close();
        pause();
    }



    public static void modificationLotProduit() {
        clearScreen();
        OracleDB db = new OracleDB();

        try {
            db.getConnection().setAutoCommit(false);
            db.runQuery("SELECT IdArticle, TO_CHAR(date_reception,'YYYY-MM-DD') AS dateRec, Qte_dispo FROM Lot_Produit");

            int idArticle = demanderEntier("IdArticle du lot à modifier : ");
            String dateRec = demanderValeurNonVide("Date réception (YYYY-MM-DD) : ");

            PreparedStatement check = db.getConnection().prepareStatement("""
            SELECT 1 FROM Lot_Produit
            WHERE IdArticle = ?
            AND date_reception = TO_DATE(?, 'YYYY-MM-DD')
        """);
            check.setInt(1, idArticle);
            check.setString(2, dateRec);
            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                System.out.println("Lot introuvable.");
                rs.close(); check.close(); db.close();
                pause();
                return;
            }
            rs.close();
            check.close();

            double newQte = -1;
            while (newQte <= 0) {
                try {
                    newQte = Double.parseDouble(
                            demanderValeurNonVide("Nouvelle quantité : ")
                    );
                } catch (Exception e) {
                    newQte = -1;
                }
                if (newQte <= 0) System.out.println("Quantité invalide.");
            }

            PreparedStatement upd = db.getConnection().prepareStatement("""
            UPDATE Lot_Produit
            SET Qte_dispo = ?
            WHERE IdArticle = ?
            AND date_reception = TO_DATE(?, 'YYYY-MM-DD')
        """);
            upd.setDouble(1, newQte);
            upd.setInt(2, idArticle);
            upd.setString(3, dateRec);
            upd.executeUpdate();
            upd.close();

            db.getConnection().commit();
            System.out.println("Lot modifié !");
        }
        catch (Exception e) {
            System.out.println("Erreur modification lot produit : " + e.getMessage());
            try { db.getConnection().rollback(); } catch (Exception ignore) {}
        }

        db.close();
        pause();
    }


    public static void suppLotProduit() {
        clearScreen();
        OracleDB db = new OracleDB();

        try {
            db.getConnection().setAutoCommit(false);
            db.runQuery("SELECT IdArticle, TO_CHAR(date_reception,'YYYY-MM-DD') AS dateRec, Qte_dispo FROM Lot_Produit");

            int idArt = demanderEntier("IdArticle du lot à supprimer : ");
            String dateRec = demanderValeurNonVide("Date réception (YYYY-MM-DD) : ");

            PreparedStatement check = db.getConnection().prepareStatement("""
                SELECT 1 FROM Lot_Produit
                WHERE IdArticle = ?
                AND date_reception = TO_DATE(?, 'YYYY-MM-DD')
            """);
            check.setInt(1, idArt);
            check.setString(2, dateRec);
            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                System.out.println("Aucun lot ne correspond.");
                rs.close();
                check.close();
                db.close();
                pause();
                return;
            }
            rs.close();
            check.close();

            PreparedStatement del = db.getConnection().prepareStatement("""
                DELETE FROM Lot_Produit
                WHERE IdArticle = ?
                AND date_reception = TO_DATE(?, 'YYYY-MM-DD')
            """);
            del.setInt(1, idArt);
            del.setString(2, dateRec);
            del.executeUpdate();
            del.close();

            db.getConnection().commit();
            System.out.println("Lot supprimé.");

        } catch (Exception e) {
            System.out.println("Erreur suppression lot produit : " + e.getMessage());
            try {
                db.getConnection().rollback();
            } catch (Exception ignore) {}
        }

        db.close();
        pause();
    }




    public static void modContenant() {
        clearScreen();
        System.out.println("Voulez vous ajouter un contenant ? (y/n)");
        String reponse = yesNoQuestion();
        if (reponse.equals("y")) {
            ajoutContenant();
        }
        System.out.println("Voulez vous supprimer un contenant de votre liste ? (y/n)");
        String suppProd = yesNoQuestion();
        if (suppProd.equals("y")) {
            suppContenant();
        }
        pause();
    }

    public static void ajoutContenant() {
        clearScreen();
        OracleDB db = new OracleDB();

        try {
            db.getConnection().setAutoCommit(false);
            System.out.println("Liste des contenants existants :");
            db.runQuery("SELECT * FROM Contenant");

            // === Génération de IdArticle ===
            String sqlID = "SELECT NVL(MAX(IdArticle),0)+1 AS newId FROM Article";
            ResultSet rsID = db.getConnection().createStatement().executeQuery(sqlID);
            rsID.next();
            int newId = rsID.getInt("newId");
            rsID.close();

            // === Saisie ===
            System.out.println("Création d'un nouveau contenant…");

            String type = demanderValeurNonVide("Type (bouteille / bocal / sachet...) : ");

            String unite = "";
            while (!unite.equals("L") && !unite.equals("g")) {
                unite = demanderValeurNonVide("Capacité : choisir parmi {L, g}");
                if (!unite.equals("L") && !unite.equals("g")) {
                    System.out.println("Valeur invalide. L'unité doit être 'L' ou 'g'.");
                }
            }

            int capacite = -1;
            while (capacite < 0) {
                String capaciteStr = demanderValeurNonVide("La capacité (ex : 75 , 500 ) : ");
                try {
                    capacite = Integer.parseInt(capaciteStr);
                } catch (Exception e) {
                } finally {
                    if (capacite < 0) {
                        System.out.println("Valeur de capacité invalide.");
                        System.out.println("Veuillez entrer un valeur correcte de capacité");
                    } else {
                        capacite = Integer.parseInt(capaciteStr);
                    }
                }
            }

            String caractere = "";
            while (!caractere.equals("Réutilisable") && !caractere.equals("Jetable")) {
                caractere = demanderValeurNonVide("Caractère (Réutilisable / Jetable) : ");
                if (!caractere.equals("Réutilisable") && !caractere.equals("Jetable")) {
                    System.out.println("Choisir : Réutilisable ou Jetable");
                }
            }

            String prixAchatStr = demanderValeurNonVide("Prix d'achat : ");
            double prixAchat = Double.parseDouble(prixAchatStr);

            String ttcStr = demanderValeurNonVide("Prix TTC : ");
            double prixTTC = Double.parseDouble(ttcStr);

            if (prixTTC <= prixAchat) {
                System.out.println("ERREUR : prixTTC doit être > prixAchat !");
                db.close();
                return;
            }

            // === INSERT Article ===
            String sqlArticle = "INSERT INTO Article(IdArticle, prixAchat, prixTTC) VALUES (?,?,?)";
            PreparedStatement pa = db.getConnection().prepareStatement(sqlArticle);
            pa.setInt(1, newId);
            pa.setDouble(2, prixAchat);
            pa.setDouble(3, prixTTC);
            pa.executeUpdate();
            pa.close();

            // === INSERT Contenant ===
            String sqlCont = """
            INSERT INTO Contenant(IdArticle, Type, Capacite, Unite, Caractere)
            VALUES (?, ?, ?, ?, ?)
        """;
            PreparedStatement pc = db.getConnection().prepareStatement(sqlCont);
            pc.setInt(1, newId);
            pc.setString(2, type);
            pc.setInt(3, capacite);
            pc.setString(4, unite);
            pc.setString(5, caractere);
            pc.executeUpdate();
            pc.close();

            db.getConnection().commit();
            System.out.println("Contenant ajouté avec succès !");
        }
        catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout du contenant.");
            try { db.getConnection().rollback(); } catch (SQLException ign) {}
        }

        db.close();
    }

    public static void suppContenant() {
        clearScreen();
        OracleDB db = new OracleDB();

        try {
            db.getConnection().setAutoCommit(false);

            System.out.println("Liste des contenants :");
            db.runQuery("SELECT * FROM Contenant");

            int idArticle;

            while (true) {
                idArticle = demanderEntier("Entrez l'IdArticle du contenant à supprimer : ");

                // Vérifier existence
                PreparedStatement ps = db.getConnection().prepareStatement(
                        "SELECT 1 FROM Contenant WHERE IdArticle = ?"
                );
                ps.setInt(1, idArticle);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    rs.close();
                    ps.close();
                    break; // ok
                }

                rs.close();
                ps.close();

                System.out.println("Ce contenant n'existe pas.");
                System.out.println("Voulez-vous réessayer ? (y/n)");
                if (yesNoQuestion().equals("n")) {
                    db.close();
                    return;
                }
            }

            // Vérifier si l'article du contenant est utilisé dans des commandes
            PreparedStatement checkCmd = db.getConnection().prepareStatement(
                    "SELECT 1 FROM Ligne_Commande WHERE IdArticle = ?"
            );
            checkCmd.setInt(1, idArticle);
            ResultSet rsCmd = checkCmd.executeQuery();

            if (rsCmd.next()) {
                System.out.println("Impossible de supprimer ce contenant : son article est utilisé dans des commandes.");
                rsCmd.close();
                checkCmd.close();
                db.close();
                pause();
                return;
            }
            rsCmd.close();
            checkCmd.close();

            // Supprimer lots de contenant
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "DELETE FROM Lot_Contenant WHERE IdArticle = ?"
            )) {
                ps.setInt(1, idArticle);
                ps.executeUpdate();
            }

            // Supprimer le contenant
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "DELETE FROM Contenant WHERE IdArticle = ?"
            )) {
                ps.setInt(1, idArticle);
                ps.executeUpdate();
            }

            // Supprimer l'article associé
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "DELETE FROM Article WHERE IdArticle = ?"
            )) {
                ps.setInt(1, idArticle);
                ps.executeUpdate();
            }

            db.getConnection().commit();
            System.out.println("Contenant supprimé.");

        } catch (SQLException e) {
            System.out.println("Erreur suppression contenant : " + e.getMessage());
            try {
                db.getConnection().rollback();
            } catch (Exception ign) {
            }
        }

        db.close();
        pause();
    }



    public static void modLotContenant() {
        clearScreen();
        System.out.println("Voulez vous ajouter un lot de contenant ? (y/n)");
        String reponse = yesNoQuestion();
        if (reponse.equals("y")) {
            ajoutLotContenant();
        }
        System.out.println("Voulez vous supprimer un lot de contenant de votre liste ? (y/n)");
        String suppProd = yesNoQuestion();
        if (suppProd.equals("y")) {
            suppLotContenant();
        }
        pause();
    }

    public static void ajoutLotContenant() {
        clearScreen();
        OracleDB db = new OracleDB();

        try {
            db.getConnection().setAutoCommit(false);
            db.runQuery("SELECT * FROM Contenant");

            int idArticle = demanderEntier("IdArticle du contenant : ");

            PreparedStatement check = db.getConnection().prepareStatement(
                    "SELECT 1 FROM Contenant WHERE IdArticle = ?"
            );
            check.setInt(1, idArticle);
            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                System.out.println("Ce contenant n'existe pas.");
                rs.close(); check.close(); db.close();
                return;
            }
            rs.close();
            check.close();

            int qte = demanderEntier("Quantité du lot : ");
            String dateRec = demanderValeurNonVide("Date réception (YYYY-MM-DD) : ");

            PreparedStatement ins = db.getConnection().prepareStatement("""
            INSERT INTO Lot_Contenant(IdArticle, date_reception, Qte_dispo)
            VALUES (?, TO_DATE(?, 'YYYY-MM-DD'), ?)
        """);
            ins.setInt(1, idArticle);
            ins.setString(2, dateRec);
            ins.setInt(3, qte);
            ins.executeUpdate();
            ins.close();

            db.getConnection().commit();
            System.out.println("Lot de contenant ajouté !");
        }
        catch (Exception e) {
            System.out.println("Erreur ajout lot contenant : " + e.getMessage());
            try { db.getConnection().rollback(); } catch (Exception ignore) {}
        }

        db.close();
        pause();
    }


    public static void suppLotContenant() {
        clearScreen();
        OracleDB db = new OracleDB();

        try {
            db.getConnection().setAutoCommit(false);
            db.runQuery("SELECT IdArticle, TO_CHAR(date_reception,'YYYY-MM-DD') AS dateRec, Qte_dispo FROM Lot_Contenant");

            int idArt = demanderEntier("IdArticle du lot contenant à supprimer : ");
            String dateRec = demanderValeurNonVide("Date réception (YYYY-MM-DD) : ");

            PreparedStatement check = db.getConnection().prepareStatement("""
                SELECT 1 FROM Lot_Contenant
                WHERE IdArticle = ?
                AND date_reception = TO_DATE(?, 'YYYY-MM-DD')
            """);
            check.setInt(1, idArt);
            check.setString(2, dateRec);

            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                System.out.println("Aucun lot de contenant trouvé.");
                rs.close();
                check.close();
                db.close();
                pause();
                return;
            }

            rs.close();
            check.close();

            PreparedStatement del = db.getConnection().prepareStatement("""
                DELETE FROM Lot_Contenant
                WHERE IdArticle = ?
                AND date_reception = TO_DATE(?, 'YYYY-MM-DD')
            """);
            del.setInt(1, idArt);
            del.setString(2, dateRec);
            del.executeUpdate();
            del.close();

            db.getConnection().commit();
            System.out.println("Lot de contenant supprimé.");

        } catch (Exception e) {
            System.out.println("Erreur suppression lot contenant : " + e.getMessage());
            try {
                db.getConnection().rollback();
            } catch (Exception ignore) {}
        }

        db.close();
        pause();
    }


}
