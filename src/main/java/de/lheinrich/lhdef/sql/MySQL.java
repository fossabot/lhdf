package de.lheinrich.lhdef.sql;

import java.sql.*;

/*
 * Copyright (c) 2018 Lennart Heinrich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public class MySQL {

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private Connection connection;
    private Connection transactionConnection;

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
            this.transactionConnection = DriverManager.getConnection(
                    "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?autoReconnect=true&useSSL=false&allowMultiQueries=true",
                    this.username, this.password);
            this.transactionConnection.setAutoCommit(false);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            this.connection.close();
            this.transactionConnection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isConnected() {
        try {
            return this.connection != null && !this.connection.isClosed() && this.transactionConnection != null && !this.transactionConnection.isClosed();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public Connection getConnection() {
        return connection;
    }

    public Connection getTransactionConnection() {
        return connection;
    }

    private PreparedStatement prepareStatement(String query, Object... args) throws SQLException {
        var preparedStatement = getConnection().prepareStatement(query);
        for (var i = 1; i <= args.length; i++)
            preparedStatement.setObject(i, args[i - 1]);

        return preparedStatement;
    }

    protected PreparedStatement prepareTransactionStatement(String query, Object... args) throws SQLException {
        var preparedStatement = getTransactionConnection().prepareStatement(query);
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