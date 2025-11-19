package jdbc;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OracleDB {

    private static final String URL  = "jdbc:oracle:thin:@//localhost:1521/XE";
    private static final String USER = "SYSTEM";
    private static final String PASS = "0000";

    private Connection conn;

    public OracleDB() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            this.conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("Connexion Oracle réussie ✔");
        } catch (Exception e) {
            System.out.println("Échec connexion Oracle ❌");
            e.printStackTrace();
        }
    }

    public void runQuery(String sql) {
    try {
        if (conn == null) {
            System.out.println("❌ Impossible d'exécuter la requête : pas de connexion.");
            return;
        }

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        // --- Stockage temporaire ---
        List<String[]> rows = new ArrayList<>();
        String[] header = new String[colCount];
        int[] widths = new int[colCount];

        // --- Récupération des noms des colonnes ---
        for (int i = 1; i <= colCount; i++) {
            header[i - 1] = meta.getColumnName(i);
            widths[i - 1] = header[i - 1].length();
        }
        rows.add(header);

        // --- Lecture des données ---
        while (rs.next()) {
            String[] line = new String[colCount];
            for (int i = 1; i <= colCount; i++) {
                String val = String.valueOf(rs.getObject(i));
                line[i - 1] = val;
                widths[i - 1] = Math.max(widths[i - 1], val.length());
            }
            rows.add(line);
        }

        // --- Construction des bordures ---
        String border = "+";
        for (int w : widths) {
            border += "-".repeat(w + 2) + "+";
        }

        // --- Affichage ---
        System.out.println(border);
        printRow(header, widths);
        System.out.println(border);

        for (int i = 1; i < rows.size(); i++) {
            printRow(rows.get(i), widths);
        }
        System.out.println(border);

        rs.close();
        stmt.close();

    } catch (Exception e) {
        e.printStackTrace();
    }
}

private void printRow(String[] values, int[] widths) {
    StringBuilder sb = new StringBuilder("|");
    for (int i = 0; i < values.length; i++) {
        sb.append(" ")
          .append(String.format("%-" + widths[i] + "s", values[i]))
          .append(" |");
    }
    System.out.println(sb.toString());
}


    public void close() {
        try {
            if (conn != null) conn.close();
            System.out.println("Connexion fermée.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
