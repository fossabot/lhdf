package de.lheinrich.lhdf.sql;

import de.lheinrich.lhdf.tools.FileTools;

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

public class ClientSQL {

    protected String sqlHost;
    protected int sqlPort;
    protected String sqlDatabase;
    protected String sqlUsername;
    protected String sqlPassword;
    private String sqlConnectionString;
    private Connection sqlConnection;
    private Connection sqlTransactionConnection;

    public void loginSQL(DriverSQL driverSQL, String host, int port, String database, String username, String password) {
        this.sqlHost = host;
        this.sqlPort = port;
        this.sqlDatabase = database;
        this.sqlUsername = username;
        this.sqlPassword = password;
        this.sqlConnectionString = "jdbc:" + driverSQL.getDriver() + "://" + this.sqlHost + ":" + this.sqlPort + "/" + this.sqlDatabase + "?autoReconnect=true&useSSL=false&allowMultiQueries=true";
    }

    public void connectSQL() {
        try {
            this.sqlConnection = DriverManager.getConnection(sqlConnectionString, this.sqlUsername, this.sqlPassword);
            this.sqlTransactionConnection = DriverManager.getConnection(sqlConnectionString, this.sqlUsername, this.sqlPassword);
            this.sqlTransactionConnection.setAutoCommit(false);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void disconnectSQL() {
        try {
            this.sqlConnection.close();
            this.sqlTransactionConnection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isConnectedSQL() {
        try {
            return this.sqlConnection != null && !this.sqlConnection.isClosed() && this.sqlTransactionConnection != null && !this.sqlTransactionConnection.isClosed();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public Connection getSqlConnection() {
        return this.sqlConnection;
    }

    public Connection getSqlTransactionConnection() {
        return this.sqlTransactionConnection;
    }

    private PreparedStatement prepareStatementSQL(String query, Object... args) throws SQLException {
        var preparedStatement = getSqlConnection().prepareStatement(query);
        for (var i = 1; i <= args.length; i++)
            preparedStatement.setObject(i, args[i - 1]);

        return preparedStatement;
    }

    protected PreparedStatement prepareTransactionStatementSQL(Connection connection, String query, Object... args) throws SQLException {
        var preparedStatement = connection.prepareStatement(query);
        for (var i = 1; i <= args.length; i++)
            preparedStatement.setObject(i, args[i - 1]);

        return preparedStatement;
    }

    public ResultSet doQuerySQL(String query, Object... args) {
        try {
            ResultSet resultSet = prepareStatementSQL(query, args).executeQuery();
            return resultSet;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void doUpdateSQL(String query, Object... args) {
        try {
            PreparedStatement preparedStatement = prepareStatementSQL(query, args);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static String getQuery(String name) {
        return FileTools.loadResourceFile("sql/" + name + ".sql");
    }
}