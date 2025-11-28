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
    
    import java.util.List;
    
    public class MenuApp {
    
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
            connectionDuClient();
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
    
        if (reponse.equals("y")) {
            connexionClient();
        } else {
            inscriptionOuPas();
        }
    
        pause();
        menuPrincipal();
    }


        private static void connexionClient() {
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

            if (mailExisteDansBDD(mail_temp)) {
                mail = mail_temp;
                System.out.println("On vous a connecté avec ce mail : " + mail);
                questionDonnees();
            } else {
                System.out.println("Vous n'êtes pas inscrit. Voulez-vous vous inscrire avec ce mail ? (y/n)");

                String repInscription = yesNoQuestion();   // <-- ceci fonctionnera enfin

                if (repInscription.equals("y")) {
                    mail = mail_temp;
                    insererClient(mail_temp);
                    System.out.println("Voulez vous entrer vos données personelles ?");
                    String reponse = yesNoQuestion();
                    if (reponse.equals("n")) {
                        System.out.println("Pas de souci, vous pourrez les entrer plus tard." +
                                "On vous etes actuellementconnecté avec ce mail : " + mail);
                        return;
                    }
                    creerCompteComplet(mail);
                    System.out.println("On vous a inscrit avec ce mail : " + mail);

                } else {
                    System.out.println("Pas de souci ! Vous pourrez vous inscrire plus tard.");
                }
            }
        }




    // -----------------------------------------------------------
    // ----------------------- 2) INSCRIPTION ---------------------
    // -----------------------------------------------------------
    
        private static void inscriptionOuPas() {
    
            System.out.println("Voulez-vous vous inscrire ? (y/n)");
            String reponse = yesNoQuestion();
            if (reponse.equals("n")) {
                System.out.println("Pas de souci, vous pourrez vous inscrire plus tard.");
                return;
            }
                System.out.println("Entrez votre mail :");
                String mail_temp = Question();

                // ---------- 1) mail n'existe pas : inscription ----------
                if (!mailExisteDansBDD(mail_temp)) {
                    mail = mail_temp;
                    insererClient(mail);   // INSERT COMPLET
                    System.out.println("Vous avez créé votre compte.");

                    questionDonnees();     // <---- correct

                    return;
                }

                // ---------- 2) mail existe déjà ----------
                System.out.println("Cette adresse existe déjà. Est-ce vous ? (y/n)");
                if (yesNoQuestion().equals("y")) {
                    mail = mail_temp;
                    System.out.println("On vous a connecté avec ce mail : " + mail);
                    questionDonnees();     // <---- il manquait ici !
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

        private static void creerCompteComplet(String mail_temp) {

            int newId = genererNouvelIdClient();
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
            OracleDB db = new OracleDB();
            try {
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
                    if (adresseExisteDeja(addr)) {
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
            } catch (Exception e) {
                try {
                    db.getConnection().rollback();
                } catch (Exception e1) {}

                System.out.println("Erreur lors de la création du compte :");
                e.printStackTrace();
            }
        }
    
    
        private static int genererNouvelIdClient() {
            String sql = "SELECT NVL(MAX(IdClient), 0) + 1 AS newId FROM Client";
            OracleDB db = new OracleDB();
            try {
                Statement stmt = db.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(sql);
    
                rs.next();
                int newId = rs.getInt("newId");
    
                stmt.close();
                db.getConnection().commit();
                return newId;
            } catch (Exception e) {
                try {
                    db.getConnection().rollback();
                } catch (Exception e1) {}
                e.printStackTrace();
                return -1;
            } finally {
                db.close();
            }

        }
    
        // ------------------- 5) Ajouter un client -------------------
        private static void insererClient(String mail_temp) {

            int newId = genererNouvelIdClient();
            if (newId < 0) {
                System.out.println("Impossible de créer un nouvel ID client.");
                return;
            }

            id_client = String.valueOf(newId);

            String sqlClient = "INSERT INTO Client (IdClient) VALUES (" + id_client + ")";
            OracleDB db = new OracleDB();
            try {
                Statement stmt = db.getConnection().createStatement();
                stmt.executeUpdate(sqlClient);

                stmt.close();

                System.out.println("Client ajouté (IdClient = " + id_client + ")");

            } catch (Exception e) {
                System.out.println("Erreur lors de l'insertion du client :");
                e.printStackTrace();
            } finally {
                db.close();
            }
        }



        // ------------ 6) Ajouter Adresse_Livraison ------------------
        private static void insererAdresseLivraisonParDefaut(int idClient, String mail_temp) {
    
            String sql = "INSERT INTO Adresse_Livraison (AdresseLiv, IdClient) "
                    + "VALUES ('" + mail_temp + "', " + idClient + ")";
            OracleDB db = new OracleDB();
            try {
                Statement stmt = db.getConnection().createStatement();
                stmt.executeUpdate(sql);
    
                stmt.close();

                System.out.println("Adresse de livraison ajoutée pour le client.");
    
            } catch (Exception e) {
                System.out.println("Erreur lors de l'ajout de l'adresse :");
                e.printStackTrace();
            } finally {
                db.close();

            }
        }
    
    
    
    
    // -----------------------------------------------------------
    // -------------------- 3) Vérifier Mail ----------------------
    // -----------------------------------------------------------
    
        private static boolean mailExisteDansBDD(String mail_temp) {
            String sql = "SELECT 1 FROM Adresse_Livraison WHERE AdresseLiv = '" + mail_temp + "'";
            OracleDB db = new OracleDB();
            try {
                Statement stmt = db.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(sql);
    
                boolean existe = rs.next();
    
                stmt.close();

                db.getConnection().commit();
                return existe;
            } catch (Exception e) {
                try {
                    db.getConnection().rollback();
                } catch (Exception e1) {}
                e.printStackTrace();
                return false;
            } finally {
                db.close();
            }
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
                    choix = Integer.parseInt(input);
                } catch (Exception e) {
                    choix = -1;
                }

                switch (choix) {

                    case 1:
                        menuCatalogue();
                        break;

                    case 2:
                        afficherDate();
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
                        if (!Objects.equals(id_client, "")) {
                            questionDonnees();
                        } else {
                            connectionDuClient();
                        }
                        break;

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
        public static void afficherDate() {
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
            menuPrincipal();
        }


        // ============================
        // ANONYMISATION
        // ============================
        public static void questionDonnees() {

            clearScreen();

            if (compteIncomplet) {
                System.out.println("Voulez-vous entrer vos informations personnelles ? (y/n)");
                String rep = yesNoQuestion();

                if (rep.equals("y")) {
                    creerCompteComplet(mail);
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
                    anonymiserClient();
                } else {
                    System.out.println("Merci pour votre confiance.");
                }
            }

            pause();
        }


        private static void anonymiserClient() {
            OracleDB db = new OracleDB();
            try {
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
            } catch (Exception e) {
                try {
                    db.getConnection().rollback();
                } catch (SQLException e1) {}
                System.out.println("Erreur lors de l'anonymisation :");
                e.printStackTrace();
            }
            finally {
                db.close();
            }
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

        private static boolean adresseExisteDeja(String addr) {
            String sql = "SELECT 1 FROM Adresse_Livraison WHERE AdresseLiv = '" + addr
                    + "' AND IdClient = " + id_client;

            OracleDB db = new OracleDB();
            try {
                Statement stmt = db.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                boolean exists = rs.next();

                rs.close();
                stmt.close();

                db.getConnection().commit();
                return exists;

            } catch (Exception e) {
                try {
                    db.getConnection().rollback();
                } catch (SQLException e1) {}
                System.out.println("Erreur lors de la vérification d'adresse :");
                e.printStackTrace();
                return true; // Par sécurité
            } finally {
                db.close();
            }
        }
    }