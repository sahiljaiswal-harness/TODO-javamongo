package todo;

import com.google.gson.*;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public class TodoApp {

    static class TodoItem {
        UUID id;
        String title;
        String description;
        boolean completed;
        boolean deleted;
        Instant createdAt;
        Instant updatedAt;
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/todos", new TodoHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server running on http://localhost:8000");
    }

    static class TodoHandler implements HttpHandler {
        private final MongoCollection<Document> collection;
        private final Gson gson;

        public TodoHandler() {
            collection = MongoConnection.connect().getCollection("todo");

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Instant.class,
                    (JsonDeserializer<Instant>) (json, typeOfT, context) -> Instant
                            .parse(json.getAsJsonPrimitive().getAsString()));
            builder.registerTypeAdapter(Instant.class,
                    (JsonSerializer<Instant>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()));
            gson = builder.create();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            try {
                if (method.equalsIgnoreCase("GET") && parts.length == 2) {
                    handleList(exchange);
                } else if (method.equalsIgnoreCase("POST") && parts.length == 2) {
                    handleCreate(exchange);
                } else if (parts.length == 3) {
                    UUID id = UUID.fromString(parts[2]);
                    switch (method) {
                        case "GET" -> handleGet(exchange, id);
                        case "PUT" -> handleUpdate(exchange, id);
                        case "DELETE" -> handleDelete(exchange, id);
                        default -> sendResponse(exchange, 405, "Method Not Allowed");
                    }
                } else {
                    sendResponse(exchange, 404, "Not Found");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal Server Error");
            }
        }

        private void handleList(HttpExchange exchange) throws IOException {
            List<TodoItem> items = new ArrayList<>();
            for (Document doc : collection.find(Filters.eq("deleted", false))) {
                items.add(gson.fromJson(doc.toJson(), TodoItem.class));
            }
            sendJson(exchange, items, 200);
        }

        private void handleCreate(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            TodoItem item = gson.fromJson(body, TodoItem.class);
            item.id = UUID.randomUUID();
            item.createdAt = Instant.now();
            item.updatedAt = item.createdAt;
            item.deleted = false;

            Document doc = Document.parse(gson.toJson(item));
            collection.insertOne(doc);

            sendJson(exchange, item, 201);
        }

        private void handleGet(HttpExchange exchange, UUID id) throws IOException {
            Document doc = collection.find(Filters.and(
                    Filters.eq("id", id.toString()),
                    Filters.eq("deleted", false))).first();

            if (doc == null) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }

            TodoItem item = gson.fromJson(doc.toJson(), TodoItem.class);
            sendJson(exchange, item, 200);
        }

        private void handleUpdate(HttpExchange exchange, UUID id) throws IOException {
            Document doc = collection.find(Filters.eq("id", id.toString())).first();
            if (doc == null || doc.getBoolean("deleted", false)) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }

            TodoItem existing = gson.fromJson(doc.toJson(), TodoItem.class);

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            TodoItem update = gson.fromJson(body, TodoItem.class);

            existing.title = update.title != null ? update.title : existing.title;
            existing.description = update.description != null ? update.description : existing.description;
            existing.completed = update.completed;
            existing.updatedAt = Instant.now();

            Document updatedDoc = Document.parse(gson.toJson(existing));
            collection.replaceOne(Filters.eq("id", id.toString()), updatedDoc);

            sendResponse(exchange, 200, gson.toJson(existing));
        }

        private void handleDelete(HttpExchange exchange, UUID id) throws IOException {
            Document doc = collection.find(Filters.eq("id", id.toString())).first();
            if (doc == null || doc.getBoolean("deleted", false)) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }

            TodoItem todo = gson.fromJson(doc.toJson(), TodoItem.class);

            todo.deleted = true;
            todo.updatedAt = Instant.now();

            String updatedJson = gson.toJson(todo);
            Document updatedDoc = Document.parse(updatedJson);

            collection.replaceOne(Filters.eq("id", id.toString()), updatedDoc);
            sendResponse(exchange, 204, "");
        }

        private void sendJson(HttpExchange exchange, Object obj, int status) throws IOException {
            String json = gson.toJson(obj);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void sendResponse(HttpExchange exchange, int status, String msg) throws IOException {
            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
