package de.lheinrich.lhdef.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Transaction {

    private final MySQL mysql;
    private final List<PreparedStatement> statements = new ArrayList<>();

    public Transaction(MySQL mysql) {
        this.mysql = mysql;
    }

    public void add(String query, Object... args) {
        try {
            this.statements.add(this.mysql.prepareStatement(query, args));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void commit() {
        try {
            this.mysql.getConnection().setAutoCommit(false);

            for (PreparedStatement update : statements)
                update.executeUpdate();

            this.mysql.getConnection().commit();
            this.mysql.getConnection().setAutoCommit(true);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
