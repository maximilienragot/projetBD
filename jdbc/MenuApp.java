package jdbc;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    private static final Map<Integer, LocalDate> dernierRabais = new HashMap<>();
    private static final boolean ANSI_ENABLED = !System.getProperty("os.name").toLowerCase().contains("windows")
            || "ON".equalsIgnoreCase(System.getenv("ENABLE_ANSI"));
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
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
        printHeader("MODIFIER MES INFORMATIONS");
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
        printHeader("SUPPRIMER MES DONNEES");
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
        printHeader("MES COMMANDES");
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

    private static String color(String text, String code) {
        return ANSI_ENABLED ? code + text + ANSI_RESET : text;
    }

    private static void printHeader(String title) {
        String bar = "==============================================";
        System.out.println(color(bar, ANSI_CYAN));
        System.out.println(color(" " + title, ANSI_BOLD + ANSI_CYAN));
        System.out.println(color(bar, ANSI_CYAN));
    }

    private static void choisirInterface() {
        while (true) {
            clearScreen();
            printHeader("CHOIX D'INTERFACE");
            System.out.println(color("1. Interface Utilisateur", ANSI_GREEN));
            System.out.println(color("2. Interface Épicier", ANSI_GREEN));
            System.out.println(color("0. Quitter", ANSI_YELLOW));
            System.out.print(color("Votre choix : ", ANSI_CYAN));
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

                        if (dateCourante.equals(dernierRabais.get(idArticle))) {
                            continue;
                        }

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
                        dernierRabais.put(idArticle, dateCourante);
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
            printHeader("INTERFACE UTILISATEUR");
            System.out.println(color("1. Consulter nos catalogues", ANSI_GREEN));
            System.out.println(color("2. Soldes / Offres (" + nombreAlertePeremption + ")", ANSI_GREEN));

            if (id_client.isEmpty()) {
                System.out.println(color("3. Passer une commande (connexion requise)", ANSI_YELLOW));
                System.out.println(color("4. Inscription / Connexion", ANSI_GREEN));
                System.out.println(color("0. Retour", ANSI_YELLOW));
            } else if (compteIncomplet) {
                System.out.println(color("3. Passer une commande (désactivé : compte incomplet)", ANSI_RED));
                System.out.println(color("4. Gérer mes informations", ANSI_GREEN));
                System.out.println(color("5. Déconnexion", ANSI_YELLOW));
                System.out.println(color("0. Retour", ANSI_YELLOW));
            } else {
                System.out.println(color("3. Passer une commande", ANSI_GREEN));
                System.out.println(color("4. Gérer mes informations", ANSI_GREEN));
                System.out.println(color("5. Déconnexion", ANSI_YELLOW));
                System.out.println(color("0. Retour", ANSI_YELLOW));
            }

            System.out.print(color("Votre choix : ", ANSI_CYAN));
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
            printHeader("INTERFACE EPICIER");
            System.out.println(color("1. Consulter nos catalogues", ANSI_GREEN));
            System.out.println(color("2. Alertes de péremption (" + nombreAlertePeremption + ")", ANSI_GREEN));
            System.out.println(color("3. Clôturer / mettre à jour une commande", ANSI_GREEN));
            System.out.println(color("4. Déclarer une perte", ANSI_YELLOW));
            System.out.println(color("0. Retour", ANSI_YELLOW));
            System.out.print(color("Votre choix : ", ANSI_CYAN));

            int choix = scanner.nextInt();
            scanner.nextLine();

            switch (choix) {
                case 1 -> menuCatalogue();
                case 2 -> afficherAlertesPeremption();
                case 3 -> cloturerCommande(false);
                case 4 -> declarerPerte();
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
        printHeader("CATALOGUES");
        System.out.println(color("1. Légumes", ANSI_GREEN));
        System.out.println(color("2. Fruits", ANSI_GREEN));
        System.out.println(color("3. Produits Laitiers", ANSI_GREEN));
        System.out.println(color("4. Boulangerie", ANSI_GREEN));
        System.out.println(color("5. Boissons", ANSI_GREEN));
        System.out.println(color("6. Épicerie", ANSI_GREEN));
        System.out.println(color("7. Contenants", ANSI_GREEN));
        System.out.println(color("0. Retour", ANSI_YELLOW));
        System.out.print(color("Votre choix : ", ANSI_CYAN));
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
        printHeader("PASSER UNE COMMANDE");

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
        printHeader(pourClient ? "SOLDES / OFFRES" : "ALERTES DE PEREMPTION");
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
        printHeader("CLOTURER UNE COMMANDE");

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
        printHeader("DECLARER UNE PERTE");

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
        System.out.println(color("\nAppuyez sur ENTER pour quitter...", ANSI_CYAN));
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
}
