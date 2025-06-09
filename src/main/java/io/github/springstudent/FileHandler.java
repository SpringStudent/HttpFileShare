package io.github.springstudent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;

/**
 * @author ZhouNing
 * @date 2025/6/9 14:39
 **/
public class FileHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String[] parts = uri.getPath().split("/");
        if (parts.length != 3) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        String id = parts[2];
        File file = FileRegistry.get(id);
        if (file == null || !file.exists()) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        exchange.getResponseHeaders().add("Content-Type", Files.probeContentType(file.toPath()));
        String fileName = URLEncoder.encode(file.getName(), "UTF-8").replaceAll("\\+", "%20");
        exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        exchange.sendResponseHeaders(200, file.length());
        try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }
}
