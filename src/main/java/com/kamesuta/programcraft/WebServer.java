package com.kamesuta.programcraft;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class WebServer {
    private HttpServer server;
    private final TextResourceLoader loader;
    private final ScriptExecuteAction scriptExecuteAction;

    public interface TextResourceLoader {
        String loadTextResource(String path) throws IOException;
    }

    public interface ScriptExecuteAction {
        void executeScript(String machineName, String script);
    }

    private static class RequestData {
        public String name;
        public String code;
    }

    public WebServer(TextResourceLoader loader, ScriptExecuteAction scriptExecuteAction) {
        this.loader = loader;
        this.scriptExecuteAction = scriptExecuteAction;
    }

    public void start() throws IOException {
        // Create an HttpServer instance on port 8000
        server = HttpServer.create(new InetSocketAddress(8000), 0);

        // Execute API
        server.createContext("/execute", exchange -> {
            // Form POST name=? code=?

            // Get the request method
            String method = exchange.getRequestMethod();
            if (!method.equals("POST")) {
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(405, 0);
                exchange.getResponseBody().write("Method not allowed".getBytes());
                exchange.getResponseBody().close();
                exchange.close();
                return;
            }

            // Parse POST query
            String query = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            // Parse json
            RequestData json = new Gson().fromJson(query, RequestData.class);
            if (json == null || json.name == null || json.code == null) {
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(400, 0);
                exchange.getResponseBody().write("Bad request".getBytes());
                exchange.getResponseBody().close();
                exchange.close();
                return;
            }

            // Execute the script
            scriptExecuteAction.executeScript(json.name, json.code);

            // Send the response
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write("OK".getBytes());

            // Close the exchange
            exchange.getResponseBody().close();
            exchange.close();
        });

        // Define a context that serves files from the current directory
        server.createContext("/", exchange -> {
            // Handle requests here (simple file serving)

            // Get the request path
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            // Load the file from the resources directory
            String response;
            try {
                response = loader.loadTextResource("www" + path);
            } catch (IOException e) {
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().write("Not found".getBytes());
                exchange.getResponseBody().close();
                exchange.close();
                return;
            }
            // Convert the response to bytes
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

            // Set the response headers
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, bytes.length);

            // Write the response
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();

            // Close the exchange
            exchange.close();
        });

        // Start the server
        server.start();
        System.out.println("Server started on port 8000");
    }

    public void stop() {
        if (server == null) {
            return;
        }

        // Stop the server
        server.stop(0);
        System.out.println("Server stopped");
    }
}
