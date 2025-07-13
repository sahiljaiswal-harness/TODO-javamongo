package todo;

import java.time.Instant;
import java.util.UUID;

public class TodoItem {
    public UUID id;
    public String title;
    public String description;
    public boolean completed;
    public boolean deleted;
    public Instant createdAt;
    public Instant updatedAt;
}
