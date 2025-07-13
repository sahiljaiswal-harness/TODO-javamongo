package todo;

import com.google.gson.*;
import com.mongodb.client.*;
import org.bson.Document;

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public class TodoApp {


    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/todos", new TodoHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server running on http://localhost:8000");
    }

    static class TodoHandler implements HttpHandler {
        private final TodoRepository repository;
        private final Gson gson;

        public TodoHandler() {
            MongoCollection<Document> collection = MongoConnection.connect().getCollection("todo");
            GsonBuilder builder = new GsonBuilder();

            // Register custom serializers and deserializers for Instant
            builder.registerTypeAdapter(Instant.class,
                    (JsonDeserializer<Instant>) (json, typeOfT, context) -> Instant
                            .parse(json.getAsJsonPrimitive().getAsString()));

            builder.registerTypeAdapter(Instant.class,
                    (JsonSerializer<Instant>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()));
            
        
            gson = builder.create();
            repository = new TodoRepository(collection, gson);
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
                        case "PATCH" -> handleUpdate(exchange, id);
                        case "DELETE" -> handleDelete(exchange, id);
                        default -> sendError(exchange, 405, "Method Not Allowed");
                    }
                } else {
                    sendError(exchange, 404, "Not Found");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal Server Error");
            }
        }

        private void handleList(HttpExchange exchange) throws IOException {
            // Parse query params
            String query = exchange.getRequestURI().getQuery();
            Boolean completed = null;
            String search = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        if (pair[0].equals("completed")) {
                            String val = pair[1].toLowerCase();
                            if (val.equals("true") || val.equals("false")) {
                                completed = Boolean.parseBoolean(val);
                            } else {
                                sendError(exchange, 400, "Invalid value for 'completed'. Use true or false.");
                                return;
                            }
                        } else if (pair[0].equals("search")) {
                            search = pair[1];
                        } else {
                            sendError(exchange, 400, "Unknown query parameter: '" + pair[0] + "'.");
                            return;
                        }
                    } else {
                        sendError(exchange, 400, "Malformed query parameter: '" + param + "'.");
                        return;
                    }
                }
            }
            
            String cacheKey = "todos:list" + (completed != null ? ":completed=" + completed : "") + (search != null ? ":search=" + search : "");
            String cached = RedisClient.get(cacheKey);
            if (cached != null) {
                System.out.println("Serving from Redis cache");
                sendJson(exchange, gson.fromJson(cached, List.class), 200, true);
                return;
            }
            // Always retrieve from DB and cache the full result (before search filtering)
            List<TodoItem> dbItems = repository.listTodosWithQuery(completed);
            String dbJson = gson.toJson(dbItems);
            RedisClient.setex(cacheKey, 60, dbJson); // cache list for 60 seconds
            // Now filter in-memory for search
            List<TodoItem> items = dbItems;
            if (search != null) {
                String searchLower = search.toLowerCase();
                items = new ArrayList<>();
                for (TodoItem item : dbItems) {
                    if ((item.title != null && item.title.toLowerCase().contains(searchLower)) ||
                        (item.description != null && item.description.toLowerCase().contains(searchLower))) {
                        items.add(item);
                    }
                }
            }

            RedisClient.setex(cacheKey, 60, gson.toJson(items)); 
            // Update cache for each item by id
            for (TodoItem item : dbItems) {
                String itemKey = "todo:" + item.id;
                RedisClient.setex(itemKey, 60, gson.toJson(item)); // cache item for 60 seconds
            }
            sendJson(exchange, items, 200, false);
        }

        private void handleCreate(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            TodoItem item = gson.fromJson(body, TodoItem.class);
            item = repository.createTodo(item);
            RedisClient.delete("todos:list");
            sendJson(exchange, item, 201, false);
        }

        private void handleGet(HttpExchange exchange, UUID id) throws IOException {
            String cacheKey = "todo:" + id.toString();
            String cached = RedisClient.get(cacheKey);
            if (cached != null) {
                sendJson(exchange, gson.fromJson(cached, TodoItem.class), 200, true);
                return;
            }
            TodoItem item = repository.getTodoById(id);
            if (item == null) {
                sendError(exchange, 404, "Not Found");
                return;
            }
            String json = gson.toJson(item);
            RedisClient.setex(cacheKey, 60, json); // cache item for 60 seconds
            sendJson(exchange, item, 200, false);
        }

        private void handleUpdate(HttpExchange exchange, UUID id) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            TodoItem update = gson.fromJson(body, TodoItem.class);
            TodoItem updated = repository.updateTodo(id, update);
            if (updated == null) {
                sendError(exchange, 404, "Not Found");
                return;
            }
            RedisClient.delete("todo:" + id.toString());
            RedisClient.delete("todos:list");
            sendJson(exchange, updated, 200, false);
        }

        private void handleDelete(HttpExchange exchange, UUID id) throws IOException {
            boolean deleted = repository.softDeleteTodo(id);
            if (!deleted) {
                sendError(exchange, 404, "Not Found");
                return;
            }
            RedisClient.delete("todo:" + id.toString());
            RedisClient.delete("todos:list");
            sendJson(exchange, null, 204, false);
        }

        private void sendJson(HttpExchange exchange, Object obj, int status, boolean isCache) throws IOException {
            String json = gson.toJson(obj);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            if (isCache) {
                exchange.getResponseHeaders().add("X-Cache", "HIT");
            }
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void sendError(HttpExchange exchange, int status, String msg) throws IOException {
            String json = gson.toJson(Collections.singletonMap("error", msg));
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

    }
}
