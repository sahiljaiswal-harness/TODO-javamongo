package todo;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import java.time.Instant;
import java.util.*;
import org.bson.conversions.Bson;


public class TodoRepository {
    private final MongoCollection<Document> collection;
    private final com.google.gson.Gson gson;

    public TodoRepository(MongoCollection<Document> collection, com.google.gson.Gson gson) {
        this.collection = collection;
        this.gson = gson;
    }

    public List<TodoItem> listTodos() {
        List<TodoItem> items = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("deleted", false))) {
            items.add(gson.fromJson(doc.toJson(), TodoItem.class));
        }
        return items;
    }

    // New: Support queries for completed status and search
    public List<TodoItem> listTodosWithQuery(Boolean completed) {
        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("deleted", false));
        if (completed != null) {
            filters.add(Filters.eq("completed", completed));
        }
        Bson filter = filters.size() == 1 ? filters.get(0) : Filters.and(filters);
        List<TodoItem> items = new ArrayList<>();
        for (Document doc : collection.find(filter)) {
            items.add(gson.fromJson(doc.toJson(), TodoItem.class));
        }
        return items;
    }

    public TodoItem createTodo(TodoItem item) {
        item.id = UUID.randomUUID();
        item.createdAt = Instant.now();
        item.updatedAt = item.createdAt;
        item.deleted = false;
        Document doc = Document.parse(gson.toJson(item));
        collection.insertOne(doc);
        return item;
    }

    public TodoItem getTodoById(UUID id) {
        Document doc = collection.find(Filters.and(
                Filters.eq("id", id.toString()),
                Filters.eq("deleted", false))).first();
        if (doc == null) return null;
        return gson.fromJson(doc.toJson(), TodoItem.class);
    }

    public TodoItem updateTodo(UUID id, TodoItem update) {
        Document doc = collection.find(Filters.eq("id", id.toString())).first();
        if (doc == null || doc.getBoolean("deleted", false)) return null;
        TodoItem existing = gson.fromJson(doc.toJson(), TodoItem.class);
        if (update.title != null) existing.title = update.title;
        if (update.description != null) existing.description = update.description;
        existing.completed = update.completed;
        existing.updatedAt = Instant.now();
        Document updatedDoc = Document.parse(gson.toJson(existing));
        collection.replaceOne(Filters.eq("id", id.toString()), updatedDoc);
        return existing;
    }

    public boolean softDeleteTodo(UUID id) {
        Document doc = collection.find(Filters.eq("id", id.toString())).first();
        if (doc == null || doc.getBoolean("deleted", false)) return false;
        TodoItem todo = gson.fromJson(doc.toJson(), TodoItem.class);
        todo.deleted = true;
        todo.updatedAt = Instant.now();
        Document updatedDoc = Document.parse(gson.toJson(todo));
        collection.replaceOne(Filters.eq("id", id.toString()), updatedDoc);
        return true;
    }

}

