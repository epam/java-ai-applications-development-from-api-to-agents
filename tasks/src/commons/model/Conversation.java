package commons.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Conversation {

    private final String id;
    private final List<Message> messages = new ArrayList<>();

    public Conversation() {
        this.id = UUID.randomUUID().toString();
    }

    public Conversation(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }
}
