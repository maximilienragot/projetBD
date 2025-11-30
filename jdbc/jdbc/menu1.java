    package jdbc;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;
    import java.sql.SQLException;
    import java.sql.Statement;
    import java.time.LocalDate;
    import java.util.ArrayList;
    import java.util.Objects;
    import java.util.Scanner;
    
    import jdbc.OracleDB.SimpleQuery;
    import oracle.jdbc.internal.OraclePreparedStatement;

    import java.util.List;

    import static java.lang.Integer.parseInt;

    public class menu1 {

        static String id_client = "";
        static String mail = "";
        static boolean compteIncomplet = true;
        private static int nombreAlertePeremption;
        private static int MARGE_TOLERE_EXPIRATION = 7;
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
            menuPrincipal();
        }

        public static String yesNoQuestion() {
            String reponse = scanner.nextLine().trim();
            while (!(Objects.equals(reponse, "y") || Objects.equals(reponse, "n") || reponse.isEmpty())) {
                System.out.println("reponse incorrecte ! Répondez <y> ou <n>. ");
                reponse = scanner.nextLine().trim();
            }
            return reponse;
        }

        public static String Question() {
            String reponse = scanner.nextLine().trim();
            while (reponse.isEmpty()) {
                System.out.println("reponse incorrecte ! Répondez <y> ou <n>. ");
                reponse = scanner.nextLine().trim();
            }
            return reponse;
        }




    // -----------------------------------------------------------
    // ------------------------- 1) CONNEXION ---------------------
    // -----------------------------------------------------------
        public static void connectionDuClient() {

        System.out.println("Avez-vous un compte ? (y/n)");
        String reponse = yesNoQuestion();
        OracleDB db =  new OracleDB();
        try {
            if (reponse.equals("y")) {
                connexionClient(db);
            } else {
                inscriptionOuPas(db);
            }
        }  catch (Exception e) {
            try {db.getConnection().rollback();
            } catch (Exception e2){}

        }
    }


        private static void connexionClient(OracleDB db) throws SQLException {
                String mail_temp;

                while (true) {
                    System.out.println("Veuillez entrer un mail :");
                    mail_temp = scanner.next();   // lit le mail (un mot)

                    if (!mail_temp.isEmpty()) break;

                    clearScreen();
                    System.out.println("Mail non valide ! Voulez-vous toujours vous connecter ? (y/n)");
                    String reponse = yesNoQuestion();
                    if (reponse.equals("n")) {
                        System.out.println("Pas de souci, vous pourrez vous connecter plus tard.");
                        return;
                    }
                }

                if (mailExisteDansBDD(db, mail_temp)) {
                    mail = mail_temp;
                    System.out.println("On vous a connecté avec ce mail : " + mail);
                    questionDonnees(db);
                } else {
                    System.out.println("Vous n'êtes pas inscrit. Voulez-vous vous inscrire avec ce mail ? (y/n)");

                    String repInscription = yesNoQuestion();   // <-- ceci fonctionnera enfin

                    if (repInscription.equals("y")) {
                        mail = mail_temp;
                        insererClient(db);
                        System.out.println("Voulez vous entrer vos données personelles ?");
                        String reponse = yesNoQuestion();
                        if (reponse.equals("n")) {
                            System.out.println("Pas de souci, vous pourrez les entrer plus tard." +
                                    "On vous etes actuellementconnecté avec ce mail : " + mail);
                            return;
                        }
                        creerCompteComplet(db, mail);
                        System.out.println("On vous a inscrit avec ce mail : " + mail);

                    } else {
                        System.out.println("Pas de souci ! Vous pourrez vous inscrire plus tard.");
                    }
                }
        }




    // -----------------------------------------------------------
    // ----------------------- 2) INSCRIPTION ---------------------
    // -----------------------------------------------------------

        private static void inscriptionOuPas(OracleDB db) throws SQLException {

            System.out.println("Voulez-vous vous inscrire ? (y/n)");
            String reponse = yesNoQuestion();
            if (reponse.equals("n")) {
                System.out.println("Pas de souci, vous pourrez vous inscrire plus tard.");
                return;
            }
                System.out.println("Entrez votre mail :");
                String mail_temp = Question();

                // ---------- 1) mail n'existe pas : inscription ----------
                if (!mailExisteDansBDD(db, mail_temp)) {
                    mail = mail_temp;
                    insererClient(db);   // INSERT COMPLET
                    System.out.println("Vous avez créé votre compte.");

                    questionDonnees(db);     // <---- correct

                    return;
                }

                // ---------- 2) mail existe déjà ----------
                System.out.println("Cette adresse existe déjà. Est-ce vous ? (y/n)");
                if (yesNoQuestion().equals("y")) {
                    mail = mail_temp;
                    System.out.println("On vous a connecté avec ce mail : " + mail);
                    questionDonnees(db);     // <---- il manquait ici !
                    return;
                }

                // ---------- 3) mail existe mais ce n'est pas lui ----------
                System.out.println("Voulez-vous toujours vous inscrire ? (y/n)");
                if (yesNoQuestion().equals("n")) {
                    System.out.println("Pas de souci, vous pourrez vous inscrire plus tard.");
                    return;
                }
                connectionDuClient();
        }

        private static void creerCompteComplet (OracleDB db, String mail_temp) throws SQLException {

            int newId = genererNouvelIdClient(db);
            if (newId < 0) {
                System.out.println("Impossible de générer un IdClient.");
                return;
            }

            // ⚠ IMPORTANT : on stocke l'id dans la variable globale
            id_client = String.valueOf(newId);

            // Lire infos utilisateur
            System.out.print("Entrez votre nom : ");
            String nom = scanner.nextLine().trim();

            System.out.print("Entrez votre prénom : ");
            String prenom = scanner.nextLine().trim();

            System.out.print("Entrez votre numéro de téléphone : ");
            String numTel = scanner.nextLine().trim();
                Statement stmt = db.getConnection().createStatement();

                // 1) INSERT dans Client
                stmt.executeUpdate("INSERT INTO Client (IdClient) VALUES (" + id_client + ")");

                // 2) MULTIPLES ADRESSES AVEC ANTI-DOUBLON
                System.out.println("Entrez vos adresses (au moins 1).");

                List<String> adresses = new ArrayList<>();
                boolean stop = false;

                while (!stop) {
                    System.out.print("Adresse : ");
                    String addr = scanner.nextLine().trim();

                    if (addr.isEmpty()) {
                        System.out.println("Adresse vide invalide !");
                        continue;
                    }

                    // Vérification doublon LOCAL
                    if (adresses.contains(addr)) {
                        System.out.println("⚠ Cette adresse est déjà entrée !");
                        continue;
                    }

                    // Vérification doublon dans la BDD
                    if (adresseExisteDeja(db, addr)) {
                        System.out.println("⚠ Cette adresse existe déjà dans votre compte !");
                        continue;
                    }

                    // Ajout en liste
                    adresses.add(addr);

                    System.out.print("Ajouter une autre adresse ? (y/n) : ");
                    if (yesNoQuestion().equals("n")) {
                        stop = true;
                    }
                }

                // Insertion BDD de toutes les adresses
                for (String addr : adresses) {
                    String sqlAddr = "INSERT INTO Adresse_Livraison (AdresseLiv, IdClient) VALUES ('"
                            + addr + "', " + id_client + ")";
                    stmt.executeUpdate(sqlAddr);
                }

                // 3) INSERT dans Client_Non_Oublie
                String sqlCNO =
                        "INSERT INTO Client_Non_Oublie (IdClient, NomC, PrenomC, NumTelC, EmailC) VALUES ("
                                + id_client + ", '"
                                + nom + "', '"
                                + prenom + "', '"
                                + numTel + "', '"
                                + mail_temp + "')";

                stmt.executeUpdate(sqlCNO);

                stmt.close();
                db.close();

                mail = mail_temp;
                compteIncomplet = false;

                System.out.println("Votre compte complet a été créé avec succès !");
                db.getConnection().commit();
        }


        private static int genererNouvelIdClient(OracleDB db) throws SQLException{
            String sql = "SELECT NVL(MAX(IdClient), 0) + 1 AS newId FROM Client";
                Statement stmt = db.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                rs.next();
                int newId = rs.getInt("newId");

                stmt.close();
                return newId;
        }

        // ------------------- 5) Ajouter un client -------------------
        private static void insererClient(OracleDB db) throws SQLException {

            int newId = genererNouvelIdClient(db);
            if (newId < 0) {
                System.out.println("Impossible de créer un nouvel ID client.");
                return;
            }

            id_client = String.valueOf(newId);

            String sqlClient = "INSERT INTO Client (IdClient) VALUES (" + id_client + ")";
                Statement stmt = db.getConnection().createStatement();
                stmt.executeUpdate(sqlClient);

                stmt.close();

                System.out.println("Client ajouté (IdClient = " + id_client + ")");
        }



    // -----------------------------------------------------------
    // -------------------- 3) Vérifier Mail ----------------------
    // -----------------------------------------------------------

        private static boolean mailExisteDansBDD(OracleDB db, String mail_temp) throws SQLException{
            String sql = "SELECT 1 FROM Adresse_Livraison WHERE AdresseLiv = '" + mail_temp + "'";
                Statement stmt = db.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                boolean existe = rs.next();

                stmt.close();

                db.getConnection().commit();
                return existe;
        }



        // ============================
        // MENU PRINCIPAL
        // ============================
        public static void menuPrincipal() {
            while (true) {
                clearScreen();

                nombreAlertePeremption = SystemeEpicerie.getNombreAlertePeremption();

                System.out.println("===== MENU PRINCIPAL =====");
                System.out.println("1. Consulter nos catalogues");
                System.out.println("2. Alertes de peremption (" + nombreAlertePeremption + ")");

                if (Objects.equals(id_client, "")) {
                    System.out.println("3. Passer une commande");
                    System.out.println("4. Vous ne pouvez pas clôturer de commande car vous n'êtes pas connecté(e).");
                    System.out.println("5. Inscription / Connexion");

                } else if (compteIncomplet) {
                    System.out.println("3. Passer une commande");
                    System.out.println("4. Impossible de clôturer : données personnelles incomplètes");
                    System.out.println("5. Anonymité");
                    System.out.println("6. Déconnexion");

                } else {
                    System.out.println("3. Passer une commande");
                    System.out.println("4. Clôturer une commande");
                    System.out.println("5. Anonymité");
                    System.out.println("6. Déconnexion");
                }

                System.out.println("0. Quitter");
                System.out.print("Votre choix : ");

                // --- Lecture sécurisée ---
                String input = scanner.nextLine().trim();
                int choix;
                try {
                    choix = parseInt(input);
                } catch (Exception e) {
                    choix = -1;
                }

                switch (choix) {

                    case 1:
                        menuCatalogue();
                        break;

                    case 2:
                        afficherProduitsBientotPerime();
                        break;

                    case 3:
                        if (!(compteIncomplet || Objects.equals(id_client, ""))) {
                            Passer_Commande();
                        } else {
                            System.out.println("Vous devez être connecté et complet pour commander.");
                            pause();
                        }
                        break;

                    case 4:
                        if (Objects.equals(id_client, "")) {
                            System.out.println("Impossible : vous n'êtes pas connecté(e).");
                        } else if (compteIncomplet) {
                            System.out.println("Impossible : informations personnelles manquantes.");
                        } else {
                            System.out.println("Clôture de commande à implémenter...");
                        }
                        pause();
                        break;

                    case 5:
                        OracleDB db = new OracleDB();
                        try {
                            if (!Objects.equals(id_client, "")) {
                                questionDonnees(db);
                            } else {
                                connectionDuClient();
                            }
                        } catch (Exception e) {
                            System.out.println("Probleme de connection, veuillez réessayer plus tard.");
                        } finally {
                            break;
                        }

                    case 6:
                        if (!id_client.isEmpty()) {
                            id_client = "";
                            mail = "";
                            compteIncomplet = true;
                            System.out.println("Vous êtes maintenant déconnecté.");
                        }
                        pause();
                        break;

                    case 0:
                        System.out.println("Au revoir !");
                        return;  // <-- ON QUITTE PROPREMENT

                    default:
                        System.out.println("Choix invalide !");
                        pause();
                }
            }
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
                        db.runQuery(requetes.getFirst());
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
                        continue; // revenir au choix de produit
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

            // (Ici on arrête : insertion en base / création commande sera fait dans l'étape suivante)
            System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
            scanner.nextLine();

            db.close();
            pause();
        }


        // ============================
        // DATE
        // ============================
        public static void afficherProduitsBientotPerime() {
            clearScreen();
            LocalDate dateCourante = LocalDate.now();
            LocalDate limite = dateCourante.plusDays(MARGE_TOLERE_EXPIRATION);

            System.out.println("===== ALERTES DE PÉREMPTION =====");
            System.out.println("Nous sommes le : " + dateCourante + "\n");

            ArrayList<String[]> liste = SystemeEpicerie.getProduitBientotPerime();
            nombreAlertePeremption = SystemeEpicerie.getNombreAlertePeremption();

            if (nombreAlertePeremption == 0) {
                System.out.println("Aucun produit n'est bientôt périmé.\n");
            } else {

                System.out.println("Produits bientôt périmés dans les " + MARGE_TOLERE_EXPIRATION + " jours :\n");

                System.out.printf("%-30s %-10s %-15s %-10s\n",
                        "Produit", "Quantité", "Jours restants", "Type");
                System.out.println("-----------------------------------------------------------------------");

                for (String[] prod : liste) {
                    String nom = prod[0];
                    String quantite = prod[1];
                    String jours = prod[2];
                    String typeDate = prod[3];

                    System.out.printf("%-30s %-10s %-15s %-10s\n",
                            nom,
                            quantite,
                            jours,
                            typeDate);
                }

                System.out.println("-----------------------------------------------------------------------");
                System.out.println("Total : " + nombreAlertePeremption + " lots concernés.\n");
            }

            pause();
        }


        // ============================
        // ANONYMISATION
        // ============================
        public static void questionDonnees(OracleDB db) throws SQLException {

            clearScreen();

            if (compteIncomplet) {
                System.out.println("Voulez-vous entrer vos informations personnelles ? (y/n)");
                String rep = yesNoQuestion();

                if (rep.equals("y")) {
                    creerCompteComplet(db, mail);
                    compteIncomplet = false;
                    System.out.println("Merci, vos informations ont été enregistrées.");
                } else {
                    System.out.println("Pas de souci, vous pourrez les ajouter plus tard.");
                }
            }
            else {
                System.out.println("Voulez-vous supprimer (anonymiser) vos informations personnelles ? (y/n)");
                String rep = yesNoQuestion();

                if (rep.equals("y")) {
                    anonymiserClient(db);
                } else {
                    System.out.println("Merci pour votre confiance.");
                }
            }

            pause();
        }


        private static void anonymiserClient(OracleDB db) throws SQLException{
                Statement stmt = db.getConnection().createStatement();

                // Supprime les données personnelles
                String sql1 =
                        "DELETE FROM Client_Non_Oublie WHERE IdClient = " + id_client;
                stmt.executeUpdate(sql1);

                // Supprime toutes les adresses du client
                String sql2 =
                        "DELETE FROM Adresse_Livraison WHERE IdClient = " + id_client;
                stmt.executeUpdate(sql2);

                stmt.close();
                db.close();

                compteIncomplet = true;
                mail = null;
                id_client = "";

                System.out.println("Vos données personnelles ont été anonymisées.");
                db.getConnection().commit();
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

        private static boolean adresseExisteDeja(OracleDB db, String addr) throws SQLException {
            String sql = "SELECT 1 FROM Adresse_Livraison WHERE AdresseLiv = '" + addr
                    + "' AND IdClient = " + id_client;

                Statement stmt = db.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                boolean exists = rs.next();

                rs.close();
                stmt.close();

                db.getConnection().commit();
                return exists;
        }



        public static void menuModification() {

            while (true) {
                clearScreen();

                System.out.println("===== MENU MODIFICATION / ADMINISTRATION =====");
                System.out.println("1. Modifier les PRODUITS");
                System.out.println("2. Modifier les PRODUCTEURS");
                System.out.println("3. Modifier les CONTENANTS");
                System.out.println("4. Modifier les LOTS de PRODUITS");
                System.out.println("5. Modifier les LOTS de CONTENANTS");
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
            System.out.println("Voulez vous enregistrer un produit ? (y/n)");
            if (yesNoQuestion().equals("n")) return;

            OracleDB db = new OracleDB();

            try {
                // --- Génération ID produit ---
                String sql = "SELECT NVL(MAX(idProduit), 0) + 1 AS newId FROM Produit";
                Statement stmt = db.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                rs.next();

                int newIdProduit = rs.getInt("newId");
                rs.close();
                stmt.close();

                // --- Saisie informations produit ---
                System.out.print("Entrez le nom du produit : ");
                String nomProduit = Question();

                System.out.print("Entrez la catégorie : ");
                String categorie = Question();

                System.out.print("Entrez la description : ");
                String description = Question();

                System.out.print("Entrez la caractéristique spéciale : ");
                String carSpe = Question();

                System.out.println("Voici les producteurs :");
                db.runQuery("SELECT * FROM Producteur");

                // --- Vérification producteur ---
                System.out.print("Entrez l'email du producteur : ");
                String mailProd = "";

                while (mailProd.equals("")) {
                    mailProd = Question();

                    String verif = "SELECT 1 FROM Producteur WHERE email_producteur = ?";
                    PreparedStatement ps = db.getConnection().prepareStatement(verif);
                    ps.setString(1, mailProd);

                    ResultSet rs1 = ps.executeQuery();

                    if (!rs1.next()) {
                        System.out.println("Producteur introuvable. Continuer ? (y/n)");
                        if (yesNoQuestion().equals("n")) {
                            rs1.close();
                            ps.close();
                            db.close();
                            return;
                        }
                        System.out.println("Veuillez entrer un email valide.");
                        mailProd = "";
                    }

                    rs1.close();
                    ps.close();
                }

                // --- Insertion produit ---
                String sqlInsert = """
            INSERT INTO Produit(IdProduit, nom_Produit, categorie, description, CaracteristiquesSpe, email_Producteur)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

                PreparedStatement insert = db.getConnection().prepareStatement(sqlInsert);
                insert.setInt(1, newIdProduit);
                insert.setString(2, nomProduit);
                insert.setString(3, categorie);
                insert.setString(4, description);
                insert.setString(5, carSpe);
                insert.setString(6, mailProd);

                insert.executeUpdate();
                db.getConnection().commit();
                insert.close();

                System.out.println("Produit enregistré !");
            }
            catch (SQLException e) {
                System.out.println("Erreur pendant l'enregistrement.");
                try { db.getConnection().rollback(); } catch (SQLException ignored) {}
            }

            db.close();
        }

        public static void suppProduit() {
            clearScreen();
            OracleDB db = new OracleDB();
            try {
                String sql = """
                    SELECT * FROM Produit
                    """;
                db.runQuery(sql);
                System.out.println("Entrer l'identifiant du produit à enlever.");
                String idProduit = "";
                while ( idProduit.equals("")) {
                    idProduit = Question();
                    sql = """
                    SELECT * FROM Produit WHERE idProduit = ?
                    """;
                    PreparedStatement ps1 = db.getConnection().prepareStatement(sql);
                    ps1.setString(1, idProduit);
                    ResultSet rs1 = ps1.executeQuery();
                    if (!rs1.next()) {
                        String prod = """
                        SELECT * FROM Produit
                        """;
                        db.runQuery(prod);
                        System.out.println("Attention, cet identifiant n'existe pas.");
                        System.out.println("Voulez vous continuez a supprimer un produit ? (y/n)");
                        String suppProduit = Question();
                        if (suppProduit.equals("n")) {
                            return;
                        } else {
                            System.out.println("Veuillez entrez un identifiant de produit existant.");
                            idProduit = "";
                        }
                    }
                }
                sql = """ 
                DELETE FROM Produit
                WHERE idProduit = ?
                """;
                PreparedStatement ps = db.getConnection().prepareStatement(sql);
                ps.setString(1, idProduit);
                ps.execute();
                db.getConnection().commit();
            } catch (SQLException se) {
                System.out.println("Il y a un problème. Le produit n'a pas été supprimé de la liste.");
                try {
                    db.getConnection().rollback();
                }
                catch (SQLException se2) {}
            }
            db.close();
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
                System.out.println("Voici les producteurs :");
                db.runQuery("SELECT * FROM Producteur");

                System.out.println("Quel est l'email du producteur à ajouter ?");
                String mailProd = "";

                while (mailProd.equals("")) {
                    mailProd = Question();

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

                System.out.println("Nom du producteur :");
                String nomProd = Question();

                System.out.println("Numéro de téléphone :");
                String numProd = Question();

                System.out.println("Adresse :");
                String addrProd = Question();

                System.out.println("Coordonnées géographiques :");
                String GeoLoc = Question();

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

        public static void suppProd() {
            clearScreen();
            OracleDB db = new OracleDB();
            try {
            String sql = """
                    SELECT * FROM Producteur
                    """;
            db.runQuery(sql);
            System.out.println("Entrer le mail du producteur à enlever.");
            String mailProd = "";
            while ( mailProd.equals("")) {
                mailProd = Question();
                sql = """
                    SELECT * FROM Producteur WHERE email_producteur = ?
                    """;
                PreparedStatement ps1 = db.getConnection().prepareStatement(sql);
                ps1.setString(1, mailProd);
                ResultSet rs1 = ps1.executeQuery();
                if (!rs1.next()) {
                    String prod = """
                        SELECT * FROM Producteur
                        """;
                    db.runQuery(prod);
                    System.out.println("Attention, ce mail n'existe pas.");
                    System.out.println("Voulez vous continuez a supprimer un producteur ? (y/n)");
                    String suppProd = Question();
                    if (suppProd.equals("n")) {
                        return;
                    } else {
                        System.out.println("Veuillez entrez un mail existant.");
                        mailProd = "";
                    }
                }
            }
            sql = """ 
                DELETE FROM Producteur
                WHERE email_producteur = ?
                """;
            PreparedStatement ps = db.getConnection().prepareStatement(sql);
            ps.setString(1, mailProd);
            ps.execute();
            db.getConnection().commit();
    } catch (SQLException se) {
        System.out.println("Il y a un problème. Votre producteur n'a pas été supprimé.");
        try {
            db.getConnection().rollback();
        }
        catch (SQLException se2) {}
    }
            db.close();
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
                System.out.println("Voici les articles produits existants :");
                db.runQuery("SELECT * FROM Produit");

                System.out.println("Entrez l'id du produit pour lequel créer un article :");
                int idProduit = -1;

                while (idProduit < 0) {
                    String rep = Question();
                    try { idProduit = Integer.parseInt(rep); }
                    catch (Exception e) { idProduit = -1; }

                    // vérifier existence produit
                    String sqlCheck = "SELECT 1 FROM Produit WHERE idProduit = ?";
                    PreparedStatement ps = db.getConnection().prepareStatement(sqlCheck);
                    ps.setInt(1, idProduit);

                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        System.out.println("Ce produit n'existe pas ! Continuer ? (y/n)");
                        if (yesNoQuestion().equals("n")) {
                            rs.close(); ps.close(); db.close();
                            return;
                        }
                        System.out.println("Entrez un indentifiant de produit valide :");
                        idProduit = -1;
                    }

                    rs.close();
                    ps.close();
                }

                // génération idArticle
                String sqlId = "SELECT NVL(MAX(idArticle), 0) + 1 AS newId FROM Article";
                ResultSet rsId = db.getConnection().createStatement().executeQuery(sqlId);
                rsId.next();
                int newIdArticle = rsId.getInt("newId");
                rsId.close();

                System.out.print("Mode conditionnement : ");
                String mode = Question();

                System.out.print("Unité : ");
                String unite = Question();

                System.out.print("Quantité unitaire : ");
                String qte = Question();

                // insertion Article
                String sqlArt = "INSERT INTO Article(idArticle, prixttc) VALUES (?, ?)";
                PreparedStatement pArt = db.getConnection().prepareStatement(sqlArt);
                System.out.print("Prix TTC : ");
                double prix = Double.parseDouble(Question());
                pArt.setInt(1, newIdArticle);
                pArt.setDouble(2, prix);
                pArt.executeUpdate();
                pArt.close();

                // insertion Art_Pdt
                String sql = """
            INSERT INTO Art_Pdt(idArticle, idProduit, mode_conditionnement, unite, quantite_unitaire)
            VALUES (?, ?, ?, ?, ?)
        """;

                PreparedStatement psAdd = db.getConnection().prepareStatement(sql);
                psAdd.setInt(1, newIdArticle);
                psAdd.setInt(2, idProduit);
                psAdd.setString(3, mode);
                psAdd.setString(4, unite);
                psAdd.setString(5, qte);

                psAdd.executeUpdate();
                psAdd.close();

                db.getConnection().commit();

                System.out.println("Article-produit ajouté !");
            }
            catch (Exception e) {
                System.out.println("Erreur de connexion. Aucun article de produit n'a été ajouté");
                try { db.getConnection().rollback(); } catch (Exception ignore) {}
            }

            db.close();
        }

        public static void suppArticleProduit() {
            clearScreen();
            OracleDB db = new OracleDB();

            try {
                System.out.println("Voici les articles produits existants :");
                db.runQuery("SELECT * FROM Art_Pdt");
                System.out.println("Entrez indentifiant d'article de produit à supprimer :");
                String id = "";
                while (id.equals("")) {
                    id = Question();

                    String sqlCheck = "SELECT 1 FROM Art_Pdt WHERE idArticle = ?";
                    PreparedStatement ps = db.getConnection().prepareStatement(sqlCheck);
                    ps.setString(1, id);
                    ResultSet rs = ps.executeQuery();

                    if (!rs.next()) {
                        System.out.println("Cet article n'existe pas. Continuer ? (y/n)");
                        if (yesNoQuestion().equals("n")) {
                            rs.close(); ps.close(); db.close();
                            return;
                        }
                        id = "";
                    }

                    rs.close();
                    ps.close();
                }

                // supprimer Art_Pdt
                PreparedStatement ps1 = db.getConnection().prepareStatement(
                        "DELETE FROM Art_Pdt WHERE idArticle = ?"
                );
                ps1.setString(1, id);
                ps1.executeUpdate();
                ps1.close();

                // supprimer Article correspondant
                PreparedStatement ps2 = db.getConnection().prepareStatement(
                        "DELETE FROM Article WHERE idArticle = ?"
                );
                ps2.setString(1, id);
                ps2.executeUpdate();
                ps2.close();

                db.getConnection().commit();
                System.out.println("L'article de produit a été supprimé !");
            }
            catch (Exception e) {
                System.out.println("Erreur de connexion. L'article de produit n'a pas été supprimé.");
                try { db.getConnection().rollback(); } catch (Exception ignore) {}
            }

            db.close();
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
                System.out.println("Articles disponibles :");
                db.runQuery("SELECT * FROM Article");

                System.out.println("Entrez l'indentifiant de l'article pour créer un lot :");
                int idArticle = Integer.parseInt(Question());

                System.out.print("Quantité en stock : ");
                double qte = Double.parseDouble(Question());

                System.out.print("Date de péremption (YYYY-MM-DD) : ");
                String date = Question();

                String sql = """
            INSERT INTO Lot_Produit(idArticle, quantite, date_peremption)
            VALUES (?, ?, TO_DATE(?, 'YYYY-MM-DD'))
        """;

                PreparedStatement ps = db.getConnection().prepareStatement(sql);
                ps.setInt(1, idArticle);
                ps.setDouble(2, qte);
                ps.setString(3, date);

                ps.executeUpdate();
                ps.close();

                db.getConnection().commit();
                System.out.println("Lot ajouté !");
            }
            catch (Exception e) {
                System.out.println("Erreur de connexion. Le lot n'a pas été ajouté.");
                try { db.getConnection().rollback(); } catch (Exception ignore) {}
            }

            db.close();
        }

        public static void modificationLotProduit() {
            clearScreen();
            OracleDB db = new OracleDB();

            System.out.println("Voulez-vous modifier la quantité d'un lot de produit ? (y/n):");
            String reponse = yesNoQuestion();
            if (reponse.equals("n")) {
                db.close();
                return;
            }

            try {
                // Afficher tous les lots
                System.out.println("Voici les lots de produits :");
                db.runQuery("SELECT IdArticle, date_reception, Qte_dispo FROM Lot_Produit");

                // ========== CHOIX ID ARTICLE ==========
                String idArticleStr = "";
                while (idArticleStr.isEmpty()) {
                    System.out.println("\nEntrez l'identifiant de l'article concerné :");
                    idArticleStr = Question();

                    String sqlVerifArticle = """
                SELECT DISTINCT IdArticle FROM Lot_Produit WHERE IdArticle = ?
            """;

                    PreparedStatement ps = db.getConnection().prepareStatement(sqlVerifArticle);
                    ps.setInt(1, Integer.parseInt(idArticleStr));
                    ResultSet rs = ps.executeQuery();

                    if (!rs.next()) {
                        System.out.println(" Cet IdArticle ne correspond à aucun lot.");
                        idArticleStr = "";
                    }

                    rs.close();
                    ps.close();
                }

                int idArticle = Integer.parseInt(idArticleStr);

                // ========== CHOIX DATE RECEPTION ==========
                String dateReceptionStr = "";
                while (dateReceptionStr.isEmpty()) {
                    System.out.println("Entrez la date de réception du lot (format DD-MM-YYYY) :");
                    dateReceptionStr = Question();

                    String sqlVerifDate = """
                SELECT * FROM Lot_Produit
                WHERE IdArticle = ? 
                AND date_reception = TO_DATE(?, 'DD-MM-YYYY')
            """;

                    PreparedStatement ps = db.getConnection().prepareStatement(sqlVerifDate);
                    ps.setInt(1, idArticle);
                    ps.setString(2, dateReceptionStr);
                    ResultSet rs = ps.executeQuery();

                    if (!rs.next()) {
                        System.out.println(" Aucun lot ne correspond à cette date pour cet article.");
                        dateReceptionStr = "";
                    }

                    rs.close();
                    ps.close();
                }

                // ========== NOUVELLE QUANTITÉ (FLOAT) ==========
                String nouvelleQteStr = "";
                double nouvelleQte = 0;

                while (nouvelleQteStr.isEmpty()) {
                    System.out.println("Entrez la nouvelle quantité disponible (nombre > 0, décimal accepté) :");
                    nouvelleQteStr = Question();

                    try {
                        nouvelleQte = Double.parseDouble(nouvelleQteStr);
                        if (nouvelleQte <= 0) {
                            System.out.println(" La quantité doit être un nombre positif.");
                            nouvelleQteStr = "";
                        }
                    } catch (Exception e) {
                        System.out.println(" Ce n'est pas un nombre valide.");
                        nouvelleQteStr = "";
                    }
                }

                // ========== MISE À JOUR ==========
                String sqlUpdate = """
            UPDATE Lot_Produit
            SET Qte_dispo = ?
            WHERE IdArticle = ?
              AND date_reception = TO_DATE(?, 'DD-MM-YYYY')
        """;

                PreparedStatement psUpdate = db.getConnection().prepareStatement(sqlUpdate);
                psUpdate.setDouble(1, nouvelleQte);
                psUpdate.setInt(2, idArticle);
                psUpdate.setString(3, dateReceptionStr);

                psUpdate.executeUpdate();
                psUpdate.close();

                db.getConnection().commit();
                System.out.println("✔ Quantité du lot modifiée avec succès !");

            } catch (Exception e) {
                try { db.getConnection().rollback(); } catch (Exception ignored) {}
                System.out.println(" ERREUR : la quantité du lot n'a pas été modifiée.");
                e.printStackTrace();
            }

            db.close();
            pause();
        }

        public static void suppLotProduit() {
            clearScreen();
            OracleDB db = new OracleDB();

            try {
                db.runQuery("SELECT * FROM Lot_Produit");

                System.out.println("Entrez l'identifiant du lot de produit à supprimer :");
                int idLot = Integer.parseInt(Question());

                PreparedStatement ps = db.getConnection().prepareStatement(
                        "DELETE FROM Lot_Produit WHERE idLotProduit = ?"
                );
                ps.setInt(1, idLot);
                ps.executeUpdate();
                ps.close();

                db.getConnection().commit();

                System.out.println("Lot supprimé !");
            }
            catch (Exception e) {
                System.out.println("Erreur de connexion.Le lot n'a pas été supprimé.");
                try { db.getConnection().rollback(); } catch (Exception ignore) {}
            }

            db.close();
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

                System.out.print("Type (bouteille / bocal / sachet...) : ");
                String type = Question();

                System.out.println("Capacité : choisir parmi {L, g}");
                String unite = "";
                while (!unite.equals("L") && !unite.equals("g")) {
                    unite = Question();
                    if (!unite.equals("L") && !unite.equals("g")) {
                        System.out.println("Valeur invalide. L'unité doit être 'L' ou 'g'.");
                    }
                }

                System.out.print("La capacité (ex : 75 , 500 ) : ");
                int capacite = -1;
                while (capacite < 0) {
                    String capaciteStr = Question();
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

                System.out.print("Caractère (Réutilisable / Jetable) : ");
                String caractere = "";
                while (!caractere.equals("Réutilisable") && !caractere.equals("Jetable")) {
                    caractere = Question();
                    if (!caractere.equals("Réutilisable") && !caractere.equals("Jetable")) {
                        System.out.println("Choisir : Réutilisable ou Jetable");
                    }
                }

                System.out.print("Prix d'achat : ");
                double prixAchat = Double.parseDouble(Question());

                System.out.print("Prix TTC : ");
                double prixTTC = Double.parseDouble(Question());

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
                db.runQuery("SELECT * FROM Contenant");

                System.out.println("Entrez l'IdArticle du contenant à supprimer :");
                String id = "";

                while (id.equals("")) {
                    id = Question();

                    String sqlCheck = "SELECT 1 FROM Contenant WHERE IdArticle = ?";
                    PreparedStatement ps = db.getConnection().prepareStatement(sqlCheck);
                    ps.setString(1, id);
                    ResultSet rs = ps.executeQuery();

                    if (!rs.next()) {
                        System.out.println("Ce contenant n'existe pas. Continuer ? (y/n)");
                        if (yesNoQuestion().equals("n")) {
                            rs.close(); ps.close(); db.close();
                            return;
                        }
                        id = "";
                    }

                    rs.close();
                    ps.close();
                }

                // DELETE Contenant (cascade supprimera Article si défini ON DELETE CASCADE)
                PreparedStatement psDel = db.getConnection().prepareStatement(
                        "DELETE FROM Contenant WHERE IdArticle = ?"
                );
                psDel.setString(1, id);
                psDel.executeUpdate();
                psDel.close();

                db.getConnection().commit();
                System.out.println("Contenant supprimé.");
            }
            catch (SQLException e) {
                System.out.println("Erreur suppression contenant.");
                try { db.getConnection().rollback(); } catch (SQLException ign) {}
            }

            db.close();
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
                System.out.println("Voici les contenants :");
                db.runQuery("SELECT * FROM Contenant");

                System.out.println("Entrez l'IdArticle pour créer un lot :");
                int idArticle = -1;

                while (idArticle < 0) {
                    try {
                        idArticle = Integer.parseInt(Question());
                    } catch (Exception e) {
                        idArticle = -1;
                    }

                    PreparedStatement ps = db.getConnection().prepareStatement(
                            "SELECT 1 FROM Contenant WHERE IdArticle = ?"
                    );
                    ps.setInt(1, idArticle);
                    ResultSet rs = ps.executeQuery();

                    if (!rs.next()) {
                        System.out.println("Ce contenant n'existe pas. Continuer ? (y/n)");
                        if (yesNoQuestion().equals("n")) {
                            rs.close(); ps.close(); db.close();
                            return;
                        }
                        idArticle = -1;
                    }

                    rs.close();
                    ps.close();
                }

                System.out.println("Quantité disponible dans le lot :");
                int qte = Integer.parseInt(Question());

                System.out.println("Date de réception (YYYY-MM-DD) :");
                String dateRec = Question();

                String sqlInsert = """
            INSERT INTO Lot_Contenant(IdArticle, date_reception, Qte_dispo)
            VALUES (?, TO_DATE(?, 'YYYY-MM-DD'), ?)
        """;

                PreparedStatement psAdd = db.getConnection().prepareStatement(sqlInsert);
                psAdd.setInt(1, idArticle);
                psAdd.setString(2, dateRec);
                psAdd.setInt(3, qte);

                psAdd.executeUpdate();
                psAdd.close();

                db.getConnection().commit();
                System.out.println("Lot de contenant ajouté !");
            }
            catch (SQLException e) {
                System.out.println("Erreur ajout lot contenant.");
                try { db.getConnection().rollback(); } catch (SQLException ign) {}
            }

            db.close();
        }

        public static void suppLotContenant() {
            clearScreen();
            OracleDB db = new OracleDB();

            try {
                db.runQuery("SELECT * FROM Lot_Contenant");

                System.out.println("Entrez l'identifiant de l'article du lot à supprimer :");
                int idArticle = Integer.parseInt(Question());

                System.out.println("Entrez la date de réception du lot (YYYY-MM-DD) :");
                String dateRec = Question();

                PreparedStatement psCheck = db.getConnection().prepareStatement("""
            SELECT 1 FROM Lot_Contenant
            WHERE IdArticle = ?
            AND date_reception = TO_DATE(?, 'YYYY-MM-DD')
        """);
                psCheck.setInt(1, idArticle);
                psCheck.setString(2, dateRec);

                ResultSet rs = psCheck.executeQuery();
                if (!rs.next()) {
                    System.out.println("Aucun lot avec ces informations !");
                    rs.close();
                    psCheck.close();
                    db.close();
                    return;
                }

                rs.close();
                psCheck.close();

                PreparedStatement psDel = db.getConnection().prepareStatement("""
            DELETE FROM Lot_Contenant
            WHERE IdArticle = ?
            AND date_reception = TO_DATE(?, 'YYYY-MM-DD')
        """);
                psDel.setInt(1, idArticle);
                psDel.setString(2, dateRec);
                psDel.executeUpdate();
                psDel.close();

                db.getConnection().commit();
                System.out.println("Lot supprimé.");
            }
            catch (SQLException e) {
                System.out.println("Erreur de connexion. Le lot du contenant.");
                try { db.getConnection().rollback(); } catch (SQLException ign) {}
            }

            db.close();
        }
    }
