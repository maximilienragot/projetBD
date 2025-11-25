package jdbc;

public class Main {
    public static void main(String[] args) {

        OracleDB db = new OracleDB();   // → Connexion Oracle

        System.out.println("\n--- TEST : SELECT simple ---");
        db.runQuery("SELECT * FROM Producteur");  // → Change avec ta table

        db.close();
    }
}
 
    

