package de.lheinrich.lhdef.sql;

import java.sql.*;

public class MySQL {

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private Connection connection;

    public MySQL(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public void connect() {
        try {
            this.connection = DriverManager.getConnection(
                    "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?autoReconnect=true&useSSL=false&allowMultiQueries=true",
                    this.username, this.password);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            this.connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isConnected() {
        try {
            return this.connection != null && !this.connection.isClosed();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public Connection getConnection() {
        return connection;
    }

    protected PreparedStatement prepareStatement(String query, Object... args) throws SQLException {
        var preparedStatement = getConnection().prepareStatement(query);
        for (var i = 1; i <= args.length; i++)
            preparedStatement.setObject(i, args[i - 1]);

        return preparedStatement;
    }

    public ResultSet doQuery(String query, Object... args) {
        try {
            ResultSet resultSet = prepareStatement(query, args).executeQuery();
            return resultSet;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void doUpdate(String query, Object... args) {
        try {
            PreparedStatement preparedStatement = prepareStatement(query, args);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}