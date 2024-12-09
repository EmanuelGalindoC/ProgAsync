package progasync;

@FunctionalInterface
public interface EventListener {
    void onEvent(Event event);
}
