package de.lheinrich.lhdf.tools;

import java.util.HashMap;
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
