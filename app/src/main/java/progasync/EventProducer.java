package progasync;

public class EventProducer {
    private final ScalableEventDispatcher dispatcher;

    public EventProducer(ScalableEventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void produceEvent(String userId, String type, String message) throws InterruptedException {
        Event event = new Event(type, message, userId);
        dispatcher.dispatchEvent(event);
    }
}
