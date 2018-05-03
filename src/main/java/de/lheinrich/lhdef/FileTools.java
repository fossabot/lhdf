package de.lheinrich.lhdef;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
        String jarFolder;

        try {
            jarFolder = new File(FileTools.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath().replace('\\', '/');
            stream = FileTools.class.getResourceAsStream(resourceName);

            if (stream == null) {
                throw new IOException("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            var readBytes = stream.available();
            var buffer = new byte[readBytes];
            stream.read(buffer, 0, readBytes);

            fileContent = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException ex) {
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
