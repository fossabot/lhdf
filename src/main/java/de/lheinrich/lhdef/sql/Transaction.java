package de.lheinrich.lhdef.sql;

import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

public class Transaction {

    private final MySQL mysql;
    private final List<Map.Entry<String, Object[]>> statements = new ArrayList<>();

    public Transaction(MySQL mysql) {
        this.mysql = mysql;
    }

    public void add(String query, Object... args) {
        try {
            this.statements.add(new AbstractMap.SimpleEntry(query, args));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void commit() {
        try {
            for (var entry : statements) {
                var statement = this.mysql.prepareTransactionStatement(entry.getKey(), entry.getValue());
                statement.executeUpdate();
            }

            this.mysql.getConnection().commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
