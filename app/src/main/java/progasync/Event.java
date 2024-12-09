package progasync;

public class Event {
    private final String type;
    private final String message;
    private final String userId; 

    public Event(String type, String message, String userId) {
        this.type = type;
        this.message = message;
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getUserId() {
        return userId;
    }
}