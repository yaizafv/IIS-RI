package uo.ri.cws.application.persistence.util.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import uo.ri.util.jdbc.ConnectionProperties;

public class Jdbc {

    private static final ThreadLocal<
            Connection> threadConnection = new ThreadLocal<>();

    private static final String URL = ConnectionProperties
            .getProperty("DB_URL");
    private static final String USER = ConnectionProperties
            .getProperty("DB_USER");
    private static final String PASS = ConnectionProperties
            .getProperty("DB_PASS");

    public static Connection createThreadConnection() throws SQLException {

        Connection con = DriverManager.getConnection(URL, USER, PASS);
        threadConnection.set(con);
        return con;
    }

    public static Connection getCurrentConnection() {
        Connection connection = threadConnection.get();
        if (connection == null) {
            throw new IllegalStateException("No JDBC connection is associated with this thread");
        }
        return connection;
    }

}
