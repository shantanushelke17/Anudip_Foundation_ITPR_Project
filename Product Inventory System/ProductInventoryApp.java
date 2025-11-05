import java.sql.*;
import java.util.*;

/**
 * Product Inventory System - JDBC CLI
 * ------------------------------------
 * A simple command-line CRUD app using Java + JDBC + MySQL.
 *
 * Table: product(id INT PRIMARY KEY, name VARCHAR(50), quantity INT, price DECIMAL(10,2))
 *
 * How to run:
 *   1) Ensure MySQL is running and you created the schema using schema.sql
 *   2) Compile:
 *        Windows:
 *          javac -cp ".;path\to\mysql-connector-j.jar" ProductInventoryApp.java
 *        macOS/Linux:
 *          javac -cp ".:path/to/mysql-connector-j.jar" ProductInventoryApp.java
 *   3) Run:
 *        Windows:
 *          java -cp ".;path\to\mysql-connector-j.jar" ProductInventoryApp
 *        macOS/Linux:
 *          java -cp ".:path/to/mysql-connector-j.jar" ProductInventoryApp
 */
public class ProductInventoryApp {

    // ====== UPDATE THESE WITH YOUR DB DETAILS ======
    private static final String URL  = "jdbc:mysql://localhost:3306/inventorydb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";       // your MySQL username
    private static final String PASS = "12345";   // your MySQL password
    // ===============================================

    private final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        ProductInventoryApp app = new ProductInventoryApp();
        app.run();
    }

    private void run() {
        System.out.println("=== Product Inventory System (JDBC CLI) ===");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC driver not found. Add mysql-connector-j.jar to classpath.");
            return;
        }

        while (true) {
            printMenu();
            int choice = readInt("Enter your choice: ");
            try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
                switch (choice) {
                    case 1 -> insertProduct(con);
                    case 2 -> listProducts(con);
                    case 3 -> findProductById(con);
                    case 4 -> updateProduct(con);
                    case 5 -> deleteProduct(con);
                    case 6 -> listLowStock(con);
                    case 7 -> adjustStock(con);
                    case 8 -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid choice. Try again.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("""

                ====== MENU ======
                1. Add Product
                2. Display All Products
                3. Find Product by ID
                4. Update Product (name/quantity/price)
                5. Delete Product
                6. List Low-Stock Products (<= threshold)
                7. Adjust Stock (+/- quantity)
                8. Exit
                """);
    }

    // ---------- CRUD IMPLEMENTATION ----------

    private void insertProduct(Connection con) throws SQLException {
        int id = readInt("Enter Product ID (int): ");
        sc.nextLine(); // consume newline
        System.out.print("Enter Product Name: ");
        String name = sc.nextLine().trim();
        int qty = readInt("Enter Quantity (int): ");
        double price = readDouble("Enter Price (decimal): ");

        String sql = "INSERT INTO product (id, name, quantity, price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setInt(3, qty);
            ps.setDouble(4, price);
            int rows = ps.executeUpdate();
            System.out.println(rows + " record(s) inserted.");
        } catch (SQLIntegrityConstraintViolationException dup) {
            System.out.println("Insert failed: ID already exists.");
        }
    }

    private void listProducts(Connection con) throws SQLException {
        String sql = "SELECT id, name, quantity, price FROM product ORDER BY id";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("%-6s %-30s %-10s %-10s%n", "ID", "Name", "Quantity", "Price");
            System.out.println("--------------------------------------------------------------");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-6d %-30s %-10d %-10.2f%n",
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"));
            }
            if (!any) System.out.println("(no products found)");
        }
    }

    private void findProductById(Connection con) throws SQLException {
        int id = readInt("Enter Product ID to find: ");
        String sql = "SELECT id, name, quantity, price FROM product WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.printf("ID: %d%nName: %s%nQuantity: %d%nPrice: %.2f%n",
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("quantity"),
                            rs.getDouble("price"));
                } else {
                    System.out.println("No product found with ID " + id);
                }
            }
        }
    }

    private void updateProduct(Connection con) throws SQLException {
        int id = readInt("Enter Product ID to update: ");
        System.out.println("What would you like to update?");
        System.out.println("1) Name  2) Quantity  3) Price  4) Name+Quantity+Price");
        int opt = readInt("Choose option: ");
        sc.nextLine(); // consume newline

        String sql;
        switch (opt) {
            case 1 -> {
                System.out.print("New Name: ");
                String name = sc.nextLine().trim();
                sql = "UPDATE product SET name=? WHERE id=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setInt(2, id);
                    ack(ps.executeUpdate());
                }
            }
            case 2 -> {
                int qty = readInt("New Quantity: ");
                sql = "UPDATE product SET quantity=? WHERE id=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, qty);
                    ps.setInt(2, id);
                    ack(ps.executeUpdate());
                }
            }
            case 3 -> {
                double price = readDouble("New Price: ");
                sql = "UPDATE product SET price=? WHERE id=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setDouble(1, price);
                    ps.setInt(2, id);
                    ack(ps.executeUpdate());
                }
            }
            case 4 -> {
                System.out.print("New Name: ");
                String name = sc.nextLine().trim();
                int qty = readInt("New Quantity: ");
                double price = readDouble("New Price: ");
                sql = "UPDATE product SET name=?, quantity=?, price=? WHERE id=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setInt(2, qty);
                    ps.setDouble(3, price);
                    ps.setInt(4, id);
                    ack(ps.executeUpdate());
                }
            }
            default -> System.out.println("Invalid option.");
        }
    }

    private void deleteProduct(Connection con) throws SQLException {
        int id = readInt("Enter Product ID to delete: ");
        String sql = "DELETE FROM product WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ack(ps.executeUpdate());
        }
    }

    private void listLowStock(Connection con) throws SQLException {
        int threshold = readInt("Show products with quantity <= threshold. Enter threshold: ");
        String sql = "SELECT id, name, quantity, price FROM product WHERE quantity <= ? ORDER BY quantity ASC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-6s %-30s %-10s %-10s%n", "ID", "Name", "Quantity", "Price");
                System.out.println("--------------------------------------------------------------");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%-6d %-30s %-10d %-10.2f%n",
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("quantity"),
                            rs.getDouble("price"));
                }
                if (!any) System.out.println("(no products meet the threshold)");
            }
        }
    }

    private void adjustStock(Connection con) throws SQLException {
        int id = readInt("Enter Product ID to adjust: ");
        int delta = readInt("Enter quantity change (+ to add, - to remove): ");

        String sql = "UPDATE product SET quantity = quantity + ? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, id);
            ack(ps.executeUpdate());
        }
    }

    private void ack(int rows) {
        if (rows == 0) System.out.println("No rows affected (ID may not exist).");
        else System.out.println("Success! Rows affected: " + rows);
    }

    // ---------- INPUT HELPERS ----------

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid decimal number.");
            }
        }
    }
}
