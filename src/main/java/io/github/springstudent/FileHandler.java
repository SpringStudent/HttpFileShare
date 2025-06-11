package io.github.springstudent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;

/**
 * @author ZhouNing
 * @date 2025/6/9 14:39
 **/
public class FileHandler implements HttpHandler {

    private DownloadListener downloadListener;

    public FileHandler(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String[] parts = uri.getPath().split("/");
        if (parts.length != 3) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        String id = parts[2];
        FileInfo fileInfo = FileRegistry.get(id);
        if (fileInfo == null || !fileInfo.getFile().exists()) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
        String fileName = URLEncoder.encode(fileInfo.getFile().getName(), "UTF-8").replaceAll("\\+", "%20");
        exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        exchange.sendResponseHeaders(200, fileInfo.getFile().length());
        try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(fileInfo.getFile())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
        fileInfo.addDownloadCount();
        if(downloadListener != null) {
            downloadListener.onDownload(id, fileInfo.getDownloadCount());
        }
    }
}
