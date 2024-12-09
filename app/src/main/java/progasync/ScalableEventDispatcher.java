package progasync;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ScalableEventDispatcher {
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, EventListener>> userListeners = new ConcurrentHashMap<>();
    private final BlockingQueue<Event> eventQueue;
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(2);
    private final ExecutorService workerPool;
    private volatile boolean running = true;

    // Retry settings
    private final int maxRetries = 3;
    private final long retryDelayMillis = 1000;

    public ScalableEventDispatcher(int numWorkers, int queueCapacity) {
        this.eventQueue = new ArrayBlockingQueue<>(queueCapacity);
        this.workerPool = Executors.newFixedThreadPool(numWorkers);
        startWorkerThreads();
    }

    public void registerListener(String userId, String eventType, EventListener listener) {
        userListeners
                .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(eventType, listener);
    }

    public void unregisterListener(String userId, String eventType) {
        ConcurrentHashMap<String, EventListener> userSpecificListeners = userListeners.get(userId);
        if (userSpecificListeners != null) {
            userSpecificListeners.remove(eventType);
            if (userSpecificListeners.isEmpty()) {
                userListeners.remove(userId);
            }
        }
    }

    public void dispatchEvent(Event event) throws InterruptedException {
        eventQueue.put(event);
    }

    private void startWorkerThreads() {
        for (int i = 0; i < ((ThreadPoolExecutor) workerPool).getCorePoolSize(); i++) {
            workerPool.execute(() -> {
                try {
                    while (running || !eventQueue.isEmpty()) {
                        Event event = eventQueue.poll(1, TimeUnit.SECONDS);
                        if (event != null) {
                            processEvent(event, new AtomicInteger(0));
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    private void processEvent(Event event, AtomicInteger retryCount) {
        ConcurrentHashMap<String, EventListener> userSpecificListeners = userListeners.get(event.getUserId());
        if (userSpecificListeners != null) {
            EventListener listener = userSpecificListeners.get(event.getType());
            if (listener != null) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    System.err.println("Error processing event for user " + event.getUserId() + ": " + e.getMessage());
                    handleRetry(event, retryCount);
                }
            }
        }
    }

    private void handleRetry(Event event, AtomicInteger retryCount) {
        int currentRetry = retryCount.incrementAndGet();
        if (currentRetry <= maxRetries) {
            System.out.println("Retrying event for user " + event.getUserId() + " (Attempt " + currentRetry + ")");
            retryExecutor.schedule(() -> processEvent(event, retryCount), retryDelayMillis, TimeUnit.MILLISECONDS);
        } else {
            System.err.println("Max retry attempts reached for event: " + event.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        workerPool.shutdown();
        retryExecutor.shutdown();
        try {
            while (!eventQueue.isEmpty()) {
                Event event = eventQueue.poll();
                if (event != null) {
                    processEvent(event, new AtomicInteger(0));
                }
            }
            if (!workerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
            if (!retryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                retryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            retryExecutor.shutdownNow();
        }
    }
}