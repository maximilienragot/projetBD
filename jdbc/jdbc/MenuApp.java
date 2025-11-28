package jdbc;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

import jdbc.OracleDB.SimpleQuery;

import java.util.List;

public class MenuApp {

    static String id_client = "";
    private static int nombreAlertePeremption;
    private static boolean clientOublie = true;
    private static final int MARGE_TOLERE_EXPIRATION = 10;
    static Scanner scanner = new Scanner(System.in);
    static List<String> requetes = List.of(
            "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Légumes'",
            "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Fruits' ",
            "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Produits Laitiers' ",
            "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Boulangerie' ",
            "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Boissons' ",
            "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Épicerie' ",
            "SELECT ar.idArticle, c.type, c.capacite, c.unite, c.caractere, ar.prixttc FROM Contenant c INNER JOIN Article ar ON c.idArticle = ar.idArticle");

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
        String id_client_temp = "";
        System.out.println("Bonjour, voulez vous vous connecter ? (y/n)\n");
        String reponse = scanner.next();
        while (!(Objects.equals(reponse, "y") || Objects.equals(reponse, "n"))) {
            System.out.println("reponse incorrecte ! ");
            reponse = scanner.next();
        }
        if (reponse.equals("y")) {
            System.out.println(" Veuillez entrer un id_client.");
            id_client_temp = scanner.next();
            while (id_client_temp.isEmpty()) {
                clearScreen();
                System.out.println("id_client non valide ! Voulez vous vous inscrire ? (y/n)");
                String reponseContInscription =  "";
                while (!(Objects.equals(reponseContInscription, "y" ) || Objects.equals(reponseContInscription, "n" ))) {
                    System.out.println("reponse incorrecte ! ");
                    reponseContInscription = scanner.next();
                }
                if (reponseContInscription.equals("n")) {
                    return;
                }
                id_client_temp = scanner.next();

            }
            try {
                OracleDB db = new OracleDB();
                Statement stmt = db.getConnection().createStatement();

                String sql = "SELECT * FROM Client WHERE idclient = '" + id_client_temp + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    sql = "SELECT * FROM Client_non_oublie WHERE idclient = '" + id_client_temp + "'";
                    rs = stmt.executeQuery(sql);
                    if (rs.next()) {
                        clientOublie = true;
                    }
                    id_client = id_client_temp;
                } else {
                    System.out.println(" Vous n'êtes pas inscrits, vous voulez vous vous inscrire ? (y/n/giveup)");
                    String reponseInscription = scanner.next();
                    while (!(Objects.equals(reponseInscription, "y") || Objects.equals(reponseInscription, "n")
                            || Objects.equals(reponseInscription, "giveup"))) {
                        System.out.println("reponse incorrecte ! ");
                        reponseInscription = scanner.next();
                    }
                    if (reponseInscription.equals("y")) {
                        // ajouter client
                        id_client = id_client_temp;
                        System.out.println("On vous a inscrit avec cet identifiant:" + id_client);
                        questionAnonymisation();
                    } else {
                        System.out.println("Pas de souci ! Vous pourrez vous inscrire plus tard");
                    }
                }
                stmt.close();
                db.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Pas de souci ! Vous pourrez vous inscrire plus tard");
        }
        pause();
    }

    // ============================
    // MENUS PRINCIPAUX
    // ============================
    private static void rafraichirNombreAlertes() {
        LocalDate dateCourante = LocalDate.now();
        LocalDate limite = dateCourante.plusDays(MARGE_TOLERE_EXPIRATION);

        try {
            OracleDB db = new OracleDB();
            String sql = "SELECT COUNT(*) " +
                    "FROM Lot_Produit " +
                    "WHERE REGEXP_LIKE(date_peremption, '^[0-9]{2}-[A-Z]{3}-[0-9]{2}$') " +
                    "AND TO_DATE(date_peremption, 'DD-MON-RR', 'NLS_DATE_LANGUAGE=AMERICAN') <= TO_DATE(?, 'YYYY-MM-DD')";

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

    public static void menuUtilisateur() {
        while (true) {
            rafraichirNombreAlertes();
            clearScreen();
            System.out.println("===== INTERFACE UTILISATEUR =====");
            System.out.println("1. Consulter nos catalogues");
            System.out.println("2. Soldes / Offres (" + nombreAlertePeremption + ")");

            if (Objects.equals(id_client, "")) {
                System.out.println("3. Passer une commande (connexion requise)");
                System.out.println("4. Inscription/connection");
                System.out.println("0. Retour");
            } else if (clientOublie) {
                System.out.println("3. Passer une commande (désactivé : anonymat)");
                System.out.println("4. Anonymité");
                System.out.println("5. Deconnection");
                System.out.println("0. Retour");
            } else {
                System.out.println("3. Passer une commande");
                System.out.println("4. Anonymité");
                System.out.println("5. Deconnection");
                System.out.println("0. Retour");
            }

            System.out.print("Votre choix : ");
            int choix = scanner.nextInt();
            scanner.nextLine();

            switch (choix) {
                case 1 -> menuCatalogue();
                case 2 -> afficherSoldesOffres();
                case 3 -> {
                    if (Objects.equals(id_client, "")) {
                        System.out.println("Vous devez être connecté(e) pour passer une commande.");
                        pause();
                    } else if (clientOublie) {
                        System.out.println("Impossible de passer commande en mode anonyme.");
                        pause();
                    } else {
                        Passer_Commande();
                    }
                }
                case 4 -> {
                    if (!Objects.equals(id_client, "")) {
                        questionAnonymisation();
                    } else {
                        connectionDuClient();
                    }
                }
                case 5 -> {
                    if (!id_client.isEmpty()) {
                        id_client = "";
                        clientOublie = true;
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
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");

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

        // liste globale : chaque ligne = [idArticle, nomProduit, quantite (string), pu, sousTotal]
        List<List<String>> commande = new ArrayList<>();

        OracleDB db = new OracleDB();

        // tableau des catégories (mappage simple)
        String[] categories = {
                "Légumes", "Fruits", "Produits Laitiers", "Boulangerie",
                "Boissons", "Épicerie", "Contenants"
        };

        boolean continuerCatalogues = true;
        double totalCommande = 0.0; // on cumule ici pour éviter parse plus tard

        while (continuerCatalogues) {

            // --- Choix de la catégorie ---
            System.out.println("===== CATALOGUES =====");
            for (int i = 0; i < categories.length; i++) {
                System.out.println((i + 1) + ". " + categories[i]);
            }
            System.out.println("0. Terminer la commande et voir le récapitulatif");
            System.out.print("Choisissez la catégorie : ");

            int choix = scanner.nextInt();
            scanner.nextLine();

            if (choix == 0) break; // fin de la saisie -> on sort des catalogues
            if (choix < 0 || choix > categories.length) {
                System.out.println("Choix invalide. Réessayez.");
                continue;
            }

            String categorieChoisie = categories[choix - 1];

            // --- Afficher les articles disponibles pour la catégorie (avec idArticle !) ---
            clearScreen();
            String listArticlesSql;

            if (categorieChoisie.equalsIgnoreCase("Contenants")) {

                // --- CAS SPÉCIAL CONTENANTS ---
                listArticlesSql =
                        "SELECT c.idArticle, c.type AS nom_produit, c.capacite, c.unite, c.caractere, ar.prixttc " +
                                "FROM Contenant c " +
                                "JOIN Article ar ON ar.idArticle = c.idArticle";

            } else {

                // --- CAS NORMAL PRODUITS ---
                listArticlesSql =
                        "SELECT ar.idArticle, p.nom_produit, p.description, a.MODE_CONDITIONNEMENT, a.UNITE, a.QUANTITE_UNITAIRE, ar.prixttc " +
                                "FROM Art_Pdt a " +
                                "JOIN Article ar ON a.IDARTICLE = ar.IDARTICLE " +
                                "JOIN Produit p ON p.IDPRODUIT = a.IDPRODUIT " +
                                "WHERE p.categorie = '" + categorieChoisie + "'";
            }


            // affiche un tableau (utilise ta méthode d'affichage)
            db.runQuery(listArticlesSql);
            System.out.println("\n(Entrez 0 pour revenir au choix de catégorie)");

            // --- Ajouter des produits dans la catégorie courante ---
            while (true) {

                System.out.print("\nChoisissez l'ID de l'article (ou 0 pour revenir au catalogue) : ");
                int idArticle = scanner.nextInt();
                scanner.nextLine();

                if (idArticle == 0) break; // retourne au choix de catégorie

                // Récupérer nomProduit, prixTTC et mode conditionnement pour cet idArticle
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
                        // colonne par alias/nom de table
                        prixUnitaire = rs.getDouble("prixttc");
                        nomProduit = rs.getString("nom_produit");
                        mode = rs.getString("MODE_CONDITIONNEMENT");
                    }
                    rs.close();
                } catch (SQLException e) {
                    System.out.println("Erreur lecture article/produit : " + e.getMessage());
                    e.printStackTrace();
                    continue; // revenir au choix de produita
                }

                if (nomProduit == null || nomProduit.isEmpty()) {
                    System.out.println("Aucun article trouvé pour cet idArticle=" + idArticle + ". Réessayez.");
                    continue;
                }

                // Demander la quantité selon le mode
                double quantiteDouble = 0.0;
                int quantiteInt = 0;
                String qteAffichage;
                boolean isVrac = mode != null && mode.equalsIgnoreCase("Vrac");

                if (isVrac) {
                    // quantité décimale autorisée (ex: kg)
                    System.out.print("Choisissez la quantité  : ");
                    // lire un double en gérant les erreurs
                    try {
                        quantiteDouble = scanner.nextDouble();
                        scanner.nextLine();
                        if (quantiteDouble <= 0) {
                            System.out.println("Quantité invalide, doit être > 0.");
                            continue;
                        }
                    } catch (Exception ex) {
                        scanner.nextLine(); // consommer la ligne incorrecte
                        System.out.println("Entrée invalide. Utilisez une valeur décimale (ex: 1.5).");
                        continue;
                    }
                    qteAffichage = String.format("%.2f", quantiteDouble);
                } else {
                    // article pré-conditionné -> nombre d'articles (entier)
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
                double sousTotal = prixUnitaire * quantitePourCalcul;

                // Stocker la ligne dans 'commande' (on stocke quantite en format lisible)
                List<String> ligne = new ArrayList<>();
                ligne.add(String.valueOf(idArticle));                       // 0 : idArticle
                ligne.add(nomProduit);                                      // 1 : nom
                ligne.add(qteAffichage);                                    // 2 : qté (affichage)
                ligne.add(String.format("%.2f", prixUnitaire));             // 3 : PU
                ligne.add(String.format("%.2f", sousTotal));                // 4 : sous-total

                commande.add(ligne);

                // cumuler le total correctement ici
                totalCommande += sousTotal;

                System.out.println("→ Ajouté : " + nomProduit + " x" + qteAffichage + " (sous-total: " + String.format("%.2f", sousTotal) + " €)");
            }

            // Après la catégorie, demander si l'utilisateur veut continuer avec d'autres catalogues
            System.out.println("\nVoulez-vous ajouter des produits d'une autre catégorie ? (O/N) : ");
            String rep = scanner.nextLine().trim();
            if (!rep.equalsIgnoreCase("O")) {
                continuerCatalogues = false;
            }
            clearScreen();
        } // fin boucle catalogues

        // on a fini la saisie : afficher récapitulatif global
        clearScreen();
        System.out.println("===== RÉCAPITULATIF GLOBAL DE VOTRE COMMANDE =====");
        System.out.printf("%-8s %-40s %12s %12s %12s%n", "ID_ART", "NOM_PRODUIT", "QTE", "PU (€)", "SOUS-TOTAL (€)");
        System.out.println("------------------------------------------------------------------------------------------");

        for (List<String> l : commande) {
            String idArt = l.get(0);
            String nom = l.get(1);
            String qte = l.get(2);
            String pu = l.get(3);
            String st = l.get(4);

            System.out.printf("%-8s %-40s %12s %12s %12s%n",
                    idArt,
                    nom.length() > 40 ? nom.substring(0, 37) + "..." : nom,
                    qte,
                    pu,
                    st);
        }

        System.out.println("------------------------------------------------------------------------------------------");
        System.out.printf("%-72s %12s%n", "TOTAL", String.format("%.2f €", totalCommande));

        // Demander le mode de paiement (1 ou 2)
        System.out.println("\nChoisissez le mode de paiement :");
        System.out.println("1 - Paiement en Ligne");
        System.out.println("2 - Paiement en Boutique");
        System.out.print("Votre choix (1/2) : ");
        int choixPaiement = scanner.nextInt();
        scanner.nextLine();

        String modePaiement = "PAIEMENT EN BOUTIQUE";
        if (choixPaiement == 1) modePaiement = "PAIEMENT EN LIGNE";

        System.out.println("\nMode de paiement choisi : " + modePaiement);

        // Choisir le mode de recuperation
        System.out.println("\nChoisissez le mode de recuperation :");
        System.out.println("1 - Livraison a Domicile");
        System.out.println("2 - Retrait en Magasin");
        System.out.print("Votre choix (1/2) : ");
        int choixRecup = scanner.nextInt();
        scanner.nextLine();
        String modeRecuperation = "Retrait en Magasin";
        if (choixRecup == 1) {
            modeRecuperation = "Livraison a Domicile";
        }

        // Enregistrement en base de la commande et de ses lignes
        Connection conn = null;
        try {
            conn = db.getConnection();
            conn.setAutoCommit(false);

            // Générer un nouvel IdCommande (simplifié : MAX + 1)
            int idCommande = 0;
            PreparedStatement psId = conn.prepareStatement("SELECT NVL(MAX(IdCommande), 0) + 1 FROM Commande");
            ResultSet rsId = psId.executeQuery();
            if (rsId.next()) {
                idCommande = rsId.getInt(1);
            }
            rsId.close();
            psId.close();

            // Insertion de la commande
            PreparedStatement psCmd = conn.prepareStatement(
                    "INSERT INTO Commande (IdCommande, date_Com, Heure_Com, Prix_Total, Statut, Mode_Paiement, Mode_Recuperation, IdClient) " +
                            "VALUES (?, SYSDATE, SYSDATE, ?, 'En Préparation', ?, ?, ?)");
            psCmd.setInt(1, idCommande);
            psCmd.setDouble(2, totalCommande);
            psCmd.setString(3, modePaiement);
            psCmd.setString(4, modeRecuperation);
            psCmd.setInt(5, Integer.parseInt(id_client));
            psCmd.executeUpdate();
            psCmd.close();

            // Insertion des lignes de commande
            PreparedStatement psLig = conn.prepareStatement(
                    "INSERT INTO Ligne_Commande (IdCommande, IdLC, IdArticle, QteCom, PU, Sous_Total) VALUES (?, ?, ?, ?, ?, ?)");
            int idLC = 1;
            for (List<String> l : commande) {
                int idArt = Integer.parseInt(l.get(0));
                double qte = Double.parseDouble(l.get(2).replace(',', '.'));
                double pu = Double.parseDouble(l.get(3).replace(',', '.'));
                double st = Double.parseDouble(l.get(4).replace(',', '.'));

                psLig.setInt(1, idCommande);
                psLig.setInt(2, idLC++);
                psLig.setInt(3, idArt);
                psLig.setDouble(4, qte);
                psLig.setDouble(5, pu);
                psLig.setDouble(6, st);
                psLig.executeUpdate();
            }
            psLig.close();

            conn.commit();
            System.out.println("\nCommande enregistree avec l'ID : " + idCommande);
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ignored) {
                // ignore
            }
            System.out.println("Erreur lors de l'enregistrement de la commande : " + e.getMessage());
        } finally {
            db.close();
        }

        System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
        scanner.nextLine();

        pause();
    }


    // ============================
    // ALERTES DE PEREMPTION / SOLDES
    // ============================
    private static void afficherAlertes(boolean pourClient) {
        clearScreen();
        rafraichirNombreAlertes();
        LocalDate dateCourante = LocalDate.now();
        LocalDate limite = dateCourante.plusDays(MARGE_TOLERE_EXPIRATION);

        String titre = pourClient ? "===== SOLDES / OFFRES =====" : "===== ALERTES DE PÉREMPTION =====";
        System.out.println(titre);
        if (nombreAlertePeremption == 0) {
            System.out.println("Aucun produit concerné pour le moment.");
            pause();
            return;
        }

        System.out.println("Produits concernés jusqu'au " + limite + " :");
        String sql = "SELECT p.nom_produit, lp.Qte_dispo, lp.date_peremption, a.Unite, ar.prixttc " +
                "FROM Lot_Produit lp " +
                "JOIN Art_Pdt a ON a.IdArticle = lp.IdArticle " +
                "JOIN Produit p ON p.IdProduit = a.IdProduit " +
                "JOIN Article ar ON ar.idArticle = a.idArticle " +
                "WHERE REGEXP_LIKE(lp.date_peremption, '^[0-9]{2}-[A-Z]{3}-[0-9]{2}$') " +
                "AND TO_DATE(lp.date_peremption, 'DD-MON-RR', 'NLS_DATE_LANGUAGE=AMERICAN') <= TO_DATE('" + limite + "', 'YYYY-MM-DD') " +
                "ORDER BY lp.date_peremption";

        try {
            OracleDB db = new OracleDB();
            db.runQuery(sql);
            if (pourClient) {
                System.out.println("\nCes articles sont proposés en offre car leur date approche.");
                System.out.println("Les prix affichés tiennent compte des réductions appliquées automatiquement.");
            } else {
                System.out.println("\nLes ajustements de prix à l'approche de la date limite sont pris en compte automatiquement (SystemeEpicerie).");
            }
            db.close();
        } catch (Exception e) {
            System.out.println("Impossible d'afficher les alertes de péremption.");
        }

        pause();
    }

    public static void afficherSoldesOffres() {
        afficherAlertes(true);
    }

    public static void afficherAlertesPeremption() {
        afficherAlertes(false);
    }

    // ============================
    // ANONYMISATION
    // ============================
    public static void questionAnonymisation() {
        clearScreen();
        if (clientOublie) {
            System.out.println("Voulez vous entrer vos informations personnelles ? (y/n)");
            String reponse = scanner.next();
            if (Objects.equals(reponse, "y")) {
                clientOublie = false;
                System.out.println("Merci, vous n'êtes plus en mode anonyme.");
            } else if (Objects.equals(reponse, "n")) {
                System.out.println("Pas de soucis, vous pourrez les ajouter plus tard. ");
            }
            pause();
            return;
        }

        System.out.println("Voulez vous supprimer vos informations personnelles ? (y/n)");
        String reponse = scanner.next();
        if (Objects.equals(reponse, "y")) {
            clientOublie = true;
            System.out.println("Vos informations ont été retirées. Vous êtes en mode anonyme.");
        } else if (Objects.equals(reponse, "n")) {
            System.out.println("Merci pour votre confiance");
        }
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
            System.out.println("3 - Marquer comme 'Recuperee | Livree'");
            System.out.println("4 - Annuler la commande");
            System.out.print("Votre choix : ");
            int choixAction = scanner.nextInt();
            scanner.nextLine();

            String nouveauStatut;
            switch (choixAction) {
                case 1 -> nouveauStatut = "Prête";
                case 2 -> nouveauStatut = "En Livraison";
                case 3 -> nouveauStatut = "Recuperee | Livree";
                case 4 -> nouveauStatut = "Annulée";
                default -> {
                    System.out.println("Action invalide.");
                    conn.rollback();
                    db.close();
                    pause();
                    return;
                }
            }

            if (!"Annulée".equals(nouveauStatut)) {
                String sqlStockProbleme =
                        "SELECT lc.IdArticle " +
                                "FROM Ligne_Commande lc " +
                                "JOIN Lot_Produit lp ON lp.IdArticle = lc.IdArticle " +
                                "WHERE lc.IdCommande = ? " +
                                "GROUP BY lc.IdArticle " +
                                "HAVING SUM(lp.Qte_dispo) < SUM(lc.QteCom)";

                PreparedStatement psStock = conn.prepareStatement(sqlStockProbleme);
                psStock.setInt(1, idCommande);
                ResultSet rsStock = psStock.executeQuery();

                if (rsStock.next()) {
                    int idArtProbleme = rsStock.getInt("IdArticle");
                    System.out.println("\nStock insuffisant pour l'article " + idArtProbleme +
                            ". Impossible de clôturer cette commande.");
                    rsStock.close();
                    psStock.close();
                    conn.rollback();
                    db.close();
                    pause();
                    return;
                }
                rsStock.close();
                psStock.close();
            }

            String sqlUpdate =
                    "UPDATE Commande SET Statut = ? WHERE IdCommande = ?";
            PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
            psUpdate.setString(1, nouveauStatut);
            psUpdate.setInt(2, idCommande);
            psUpdate.executeUpdate();
            psUpdate.close();

            if ("Recuperee | Livree".equals(nouveauStatut)) {
                String sqlLignes =
                        "SELECT IdArticle, SUM(QteCom) AS QteTotale " +
                                "FROM Ligne_Commande " +
                                "WHERE IdCommande = ? " +
                                "GROUP BY IdArticle";
                PreparedStatement psLignes = conn.prepareStatement(sqlLignes);
                psLignes.setInt(1, idCommande);
                ResultSet rsLignes = psLignes.executeQuery();

                while (rsLignes.next()) {
                    int idArt = rsLignes.getInt("IdArticle");
                    double qte = rsLignes.getDouble("QteTotale");

                    String sqlMajLot =
                            "UPDATE Lot_Produit " +
                                    "SET Qte_dispo = Qte_dispo - ? " +
                                    "WHERE IdArticle = ?";
                    PreparedStatement psMajLot = conn.prepareStatement(sqlMajLot);
                    psMajLot.setDouble(1, qte);
                    psMajLot.setInt(2, idArt);
                    psMajLot.executeUpdate();
                    psMajLot.close();
                }

                rsLignes.close();
                psLignes.close();
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
}
