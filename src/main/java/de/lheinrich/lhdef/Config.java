package de.lheinrich.lhdef;

import java.util.HashMap;
import java.util.Map;


public class Config {

    private Map<String, String> config = new HashMap<>();

    public Config(String config, String backup) {
        for (var line : config.split(System.lineSeparator())) {
            if (line.trim().equals("")) {
                continue;
            }
            var keyValue = line.split("=");
            this.config.put(keyValue[0], keyValue[1]);
        }

        for (var line : backup.split(System.lineSeparator())) {
            if (line.trim().equals("")) {
                continue;
            }
            var keyValue = line.split("=");
            if (!this.config.containsKey(keyValue[0])) {
                this.config.put(keyValue[0], keyValue[1]);
            }
        }
    }

    public String get(String id) {
        return config.get(id);
    }

    public Boolean getBoolean(String id) {
        return Boolean.valueOf(config.get(id));
    }

    public Integer getInt(String id) {
        return Integer.valueOf(config.get(id));
    }

    public Double getDouble(String id) {
        return Double.valueOf(config.get(id));
    }

    public Long getLong(String id) {
        return Long.valueOf(config.get(id));
    }

    public Float getFloat(String id) {
        return Float.valueOf(config.get(id));
    }
}
