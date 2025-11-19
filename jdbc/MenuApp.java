package jdbc;
import java.util.Scanner;

public class MenuApp {

    static Scanner scanner = new Scanner(System.in);

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
            System.out.println("2. Passer une commande");
            System.out.println("3. ");
            System.out.println("0. Quitter");
            System.out.print("Votre choix : ");

            choix = scanner.nextInt();

            switch (choix) {
                case 1:
                    menuCatalogue();
                    break;
                case 2:
                    menuCalculatrice();
                    break;
                case 3:
                    afficherDate();
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
            System.out.println("3. Viande");
            System.out.println("4. Boulangerie");
            System.out.println("5. Pâtisserie");
            System.out.println("6. Biscuits");
            System.out.println("7. Laitier");
            System.out.println("8. Contenants");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");

            choix = scanner.nextInt();
            OracleDB db = new OracleDB();

            switch (choix) {
                case 1:
                    clearScreen();
                    db.runQuery("SELECT p.nom, p.description, a.forme, a.unite, a.quantite , a.disponibilite FROM Produit p INNER JOIN Article_prod a ON p.id_produit = a.id_produit WHERE p.categorie = 'Légumes'");  
                    db.close();
                    pause();
                    break;
                case 2:
                    clearScreen();
                    db.runQuery("SELECT p.nom, p.description, a.forme, a.unite, a.quantite , a.disponibilite FROM Produit p INNER JOIN Article_prod a ON p.id_produit = a.id_produit WHERE p.categorie = 'Fruits' ");  
                    db.close();
                    pause();
                    break;
                case 3:
                    clearScreen();
                    db.runQuery("SELECT p.nom, p.description, a.forme, a.unite, a.quantite , a.disponibilite FROM Produit p INNER JOIN Article_prod a ON p.id_produit = a.id_produit WHERE p.categorie = 'Viande' ");  
                    db.close();
                    pause();
                    break;
                case 4:
                    clearScreen();
                    db.runQuery("SELECT p.nom, p.description, a.forme, a.unite, a.quantite , a.disponibilite FROM Produit p INNER JOIN Article_prod a ON p.id_produit = a.id_produit WHERE p.categorie = 'Boulangerie' ");  
                    db.close();
                    pause();
                    break;
                case 5:
                    clearScreen();
                    db.runQuery("SELECT p.nom, p.description, a.forme, a.unite, a.quantite , a.disponibilite FROM Produit p INNER JOIN Article_prod a ON p.id_produit = a.id_produit WHERE p.categorie = 'Pâtisserie' ");  
                    db.close();
                    pause();
                    break;
                case 6:
                    clearScreen();
                    db.runQuery("SELECT p.nom, p.description, a.forme, a.unite, a.quantite , a.disponibilite FROM Produit p INNER JOIN Article_prod a ON p.id_produit = a.id_produit WHERE p.categorie = 'Biscuits' ");  
                    db.close();
                    pause();
                    break;
                case 7:
                    clearScreen();
                    db.runQuery("SELECT p.nom, p.description, a.forme, a.unite, a.quantite , a.disponibilite FROM Produit p INNER JOIN Article_prod a ON p.id_produit = a.id_produit WHERE p.categorie = 'Laitier' ");  
                    db.close();
                    pause();
                    break;    
                
                case 8:
                    clearScreen();
                    db.runQuery("SELECT p.nom, p.description, a.forme, a.unite, a.quantite , a.disponibilite FROM Produit p INNER JOIN Article_prod a ON p.id_produit = a.id_produit WHERE p.categorie = 'Laitier' ");  
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
    // SOUS-MENU : CALCULATRICE
    // ============================
    public static void menuCalculatrice() {
        clearScreen();
        System.out.println("===== CALCULATRICE =====");

        System.out.print("Entrez un premier nombre : ");
        int a = scanner.nextInt();

        System.out.print("Entrez un deuxième nombre : ");
        int b = scanner.nextInt();

        System.out.println("Résultat : " + (a + b));

        pause(); // retour automatique au menu précédent
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
