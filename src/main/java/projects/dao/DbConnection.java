package recipes.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import projects.exception.DbException;

public class DbConnection {
    private static final String HOST = "localhost";
    private static final String PASSWORD = "Andr0!ds";
    private static final int PORT = 3306;
    private static final String SCHEMA = "projects";
    private static final String USER = "projects";

    public static Connection getConnection() {
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false", HOST, PORT, SCHEMA);

        try {
            Connection conn = DriverManager.getConnection(url, USER, PASSWORD);
            System.out.println("Successfully obtained connection");
            return conn;
        } catch (SQLException e) {
            System.err.println("Error getting connection");
            throw new DbException(e);
        }
    }
}