package de.lheinrich.lhdef;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Database {

    private int lastMap;
    private final File dbFile;
    private final boolean isMasterDatabase;
    private final Map<String, String> values = new TreeMap<>();
    private static final Map<File, Database> DATABASES;

    static {
        DATABASES = new HashMap<>();
    }

    public Database(File dbFile) {
        this.dbFile = dbFile.getAbsoluteFile();
        if (DATABASES.containsKey(dbFile)) {
            isMasterDatabase = false;
        } else {
            isMasterDatabase = true;
            initialize();
        }
    }

    private void initialize() {
        try {
            if (!dbFile.exists()) {
                Files.write(dbFile.toPath(), "".getBytes(), StandardOpenOption.CREATE);
            }
            var dbContent = new String(Files.readAllBytes(dbFile.toPath()), StandardCharsets.UTF_8).replace(System.lineSeparator(), "");
            for (String line : dbContent.split("-")) {
                if (line.trim().equals("")) {
                    continue;
                }

                var keyValue = line.split("#", 2);
                values.put(new String(Base64.getDecoder().decode(keyValue[0]), StandardCharsets.UTF_8), new String(Base64.getDecoder().decode(keyValue[1]), StandardCharsets.UTF_8));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        write();
    }

    private void write() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    doWrite();
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }).start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            doWrite();
        }));
    }

    private void doWrite() {
        if (values.hashCode() != lastMap) {
            var builder = new StringBuilder();
            values.entrySet().iterator().forEachRemaining((entry) -> {
                builder.append(Base64.getEncoder().encodeToString(entry.getKey().getBytes()));
                builder.append("#");
                builder.append(Base64.getEncoder().encodeToString(entry.getValue().getBytes()));
                builder.append("-");
            });

            writeToFile(builder.toString());
            lastMap = values.hashCode();
        }
    }

    private void writeToFile(String content) {
        try {
            Files.write(dbFile.toPath(), content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void remove(String key) {
        if (isMasterDatabase) {
            values.remove(key);
        } else {
            DATABASES.get(dbFile).remove(key);
        }
    }

    public void update(String key, Object value) {
        if (isMasterDatabase) {
            values.remove(key);
            values.put(key, String.valueOf(value));
        } else {
            DATABASES.get(dbFile).update(key, value);
        }
    }

    public String getString(String key) {
        if (isMasterDatabase) {
            return values.get(key);
        } else {
            return DATABASES.get(dbFile).getString(key);
        }
    }

    public int getInteger(String key) {
        if (isMasterDatabase) {
            return Integer.valueOf(values.get(key));
        } else {
            return DATABASES.get(dbFile).getInteger(key);
        }
    }

    public long getLong(String key) {
        if (isMasterDatabase) {
            return Long.valueOf(values.get(key));
        } else {
            return DATABASES.get(dbFile).getLong(key);
        }
    }

    public boolean getBoolean(String key) {
        if (isMasterDatabase) {
            return Boolean.valueOf(values.get(key));
        } else {
            return DATABASES.get(dbFile).getBoolean(key);
        }
    }

    public double getDouble(String key) {
        if (isMasterDatabase) {
            return Double.valueOf(values.get(key));
        } else {
            return DATABASES.get(dbFile).getDouble(key);
        }
    }

    public Set<String> getKeys() {
        if (isMasterDatabase) {
            return values.keySet();
        } else {
            return DATABASES.get(dbFile).getKeys();
        }
    }

    public Collection<String> getValues() {
        if (isMasterDatabase) {
            return values.values();
        } else {
            return DATABASES.get(dbFile).getValues();
        }
    }

    public Map<String, String> getFull() {
        if (isMasterDatabase) {
            return values;
        } else {
            return DATABASES.get(dbFile).getFull();
        }
    }
}
