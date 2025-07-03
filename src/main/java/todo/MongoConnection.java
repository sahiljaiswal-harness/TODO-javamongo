package todo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoConnection {
    public static MongoDatabase connect() {
        String uri = System.getenv("MONGO_URI");
        MongoClient client = MongoClients.create(uri);
        return client.getDatabase("apps");
    }
}
