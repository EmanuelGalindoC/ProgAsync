package progasync;
import java.util.concurrent.*;
public class ScalableEventDispatcher {
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, EventListener>> userListeners = new ConcurrentHashMap<>();
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final ExecutorService workerPool;
    private volatile boolean running = true;

    public ScalableEventDispatcher(int numWorkers) {
        this.workerPool = Executors.newFixedThreadPool(numWorkers);
        startWorkerThreads();
    }

    public void registerListener(String userId, String eventType, EventListener listener) {
        userListeners
                .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(eventType, listener);
    }

    private void processEvent(Event event) {
        ConcurrentHashMap<String, EventListener> userSpecificListeners = userListeners.get(event.getUserId());
        if (userSpecificListeners != null) {
            EventListener listener = userSpecificListeners.get(event.getType());
            if (listener != null) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    System.err.println("Error processing event for user " + event.getUserId() + ": " + e.getMessage());
                }
            }
        }
    }

    private void startWorkerThreads() {
        for (int i = 0; i < ((ThreadPoolExecutor) workerPool).getCorePoolSize(); i++) {
            workerPool.execute(() -> {
                try {
                    while (running || !eventQueue.isEmpty()) {
                        Event event = eventQueue.poll(1, TimeUnit.SECONDS);
                        if (event != null) {
                            processEvent(event);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    public void dispatchEvent(Event event) throws InterruptedException {
        System.out.println(event.getMessage());
    }
}
