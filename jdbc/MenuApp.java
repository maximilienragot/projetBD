package jdbc;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

import jdbc.OracleDB.SimpleQuery;

import java.util.List;

public class MenuApp {

    static Scanner scanner = new Scanner(System.in);
    static  List<String> requetes = List.of(
    "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Légumes'",
    "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Fruits' ",
    "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Produits Laitiers' ",
    "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Boulangerie' ",
    "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Boissons' ",
    "SELECT p.IDPRODUIT, p.nom_produit, p.description, p.categorie, p.CARACTERISTIQUESSPE, a.MODE_CONDITIONNEMENT , a.unitE, a.QUANTITE_UNITAIRE, ar.prixttc FROM Produit p INNER JOIN Art_Pdt a ON p.IDPRODUIT = a.IDPRODUIT INNER JOIN ARTICLE ar ON ar.idArticle = a.idArticle WHERE p.categorie = 'Épicerie' " ,
    "SELECT ar.idArticle, c.type, c.capacite, c.unite, c.caractere, ar.prixttc FROM Contenant c INNER JOIN Article ar ON c.idArticle = ar.idArticle");

    public static void main(String[] args) {
        menuPrincipal();
    }

    // ============================
    // MENU PRINCIPAL
    // ============================
    public static void menuPrincipal() {
        int choix = -1;

        while (choix != 0) {
            clearScreen();
            System.out.println("===== MENU PRINCIPAL =====");
            System.out.println("1. Consulter nos catalogues");
            System.out.println("2. Alertes de peremption");
            System.out.println("3. Passer une commande");
            System.out.println("3. Cloturer une commande");
            System.out.println("0. Quitter");
            System.out.print("Votre choix : ");

            choix = scanner.nextInt();

            switch (choix) {
                case 1:
                    menuCatalogue();
                    break;
                case 2:
                    afficherDate();
                    break;
                case 3:
                    Passer_Commande();
                    break;
                case 0:
                    System.out.println("Au revoir !");
                    break;
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

        while (choix != 0) {
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
            System.out.println((i+1) + ". " + categories[i]);
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
        System.out.println("Nous sommes le : " + java.time.LocalDate.now());
        pause();
    }

    // ============================
    // UTILITAIRES
    // ============================
    public static void pause() {
        System.out.println("\nAppuyez sur ENTER pour quitter...");
        try { System.in.read(); } catch (Exception e) {}
    }

    public static void clearScreen() {
        // Efface la console (fonctionne surtout sous cmd/powershell)
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
