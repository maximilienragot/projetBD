package jdbc;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OracleDB {

    private static final String URL = "jdbc:oracle:thin:@oracle1.ensimag.fr:1521:oracle1";
    private static final String USER = "touatia";
    private static final String PASS = "touatia";

    private Connection conn;
    public Connection getConnection() {
    return this.conn;
}


    public OracleDB() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            this.conn = DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** ========== AFFICHAGE DE TABLEAU ========== */
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

            List<String[]> rows = new ArrayList<>();
            String[] header = new String[colCount];
            int[] widths = new int[colCount];

            for (int i = 1; i <= colCount; i++) {
                header[i - 1] = meta.getColumnName(i);
                widths[i - 1] = header[i - 1].length();
            }
            rows.add(header);

            while (rs.next()) {
                String[] line = new String[colCount];
                for (int i = 1; i <= colCount; i++) {
                    String val = String.valueOf(rs.getObject(i));
                    line[i - 1] = val;
                    widths[i - 1] = Math.max(widths[i - 1], val.length());
                }
                rows.add(line);
            }

            String border = "+";
            for (int w : widths) {
                border += "-".repeat(w + 2) + "+";
            }

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

    /** Affichage d'une ligne */
    private void printRow(String[] values, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < values.length; i++) {
            sb.append(" ")
              .append(String.format("%-" + widths[i] + "s", values[i]))
              .append(" |");
        }
        System.out.println(sb.toString());
    }

    /** ========== SIMPLE QUERY ========== */
    public class SimpleQuery {
        private String query;

        public SimpleQuery(String query) {
            this.query = query;
        }

        public ResultSet executeSelect() throws SQLException {
            PreparedStatement ps = conn.prepareStatement(query);
            return ps.executeQuery();
        }

        public int executeUpdate() throws SQLException {
            PreparedStatement ps = conn.prepareStatement(query);
            return ps.executeUpdate();
        }
    }

    public void close() {
        try {
            if (conn != null) conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
