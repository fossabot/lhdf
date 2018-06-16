package de.lheinrich.lhdf.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;

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

public class FileTools {

    public static String loadConfigFile(String fileName) {
        try {
            var realFilePath = new File(fileName).toPath();
            if (Files.exists(realFilePath)) {
                return new String(Files.readAllBytes(realFilePath), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return loadResourceFile(fileName);
    }

    public static String loadResourceFile(String fileName) {
        return loadResourceFile(fileName, true);
    }

    public static String loadResourceFile(String fileName, boolean printError) {
        var result = new StringBuilder();

        var classLoader = FileTools.class.getClassLoader();
        var file = new File(classLoader.getResource(fileName).getFile());

        try (var scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();
                result.append(line).append("\n");
            }
        } catch (IOException ex) {
            if (printError)
                ex.printStackTrace();
        }

        return result.toString();
    }
}
