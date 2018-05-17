package de.lheinrich.lhdf.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
        var fileContent = "";
        var resourceName = "/" + fileName;
        InputStream stream = null;
        OutputStream resStreamOut = null;

        try {
            stream = FileTools.class.getResourceAsStream(resourceName);

            if (stream == null) {
                throw new IOException("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            var readBytes = stream.available();
            var buffer = new byte[readBytes];
            stream.read(buffer, 0, readBytes);

            fileContent = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
                if (resStreamOut != null) {
                    resStreamOut.close();
                }
            } catch (IOException ex) {
            }
        }
        return fileContent;
    }
}
