package progasync;

public class NotificationSystem {
    public static void main(String[] args) {
        ScalableEventDispatcher dispatcher = new ScalableEventDispatcher(10, 20);

        for (int i = 1; i <= 50; i++) {
            String userId = "user" + i;

            dispatcher.registerListener(userId, "email", event -> {
                if (Math.random() < 0.2) {
                    throw new RuntimeException("Simulated failure for " + event.getMessage());
                }
                System.out.println(userId + " (Email): " + event.getMessage());
            });

            dispatcher.registerListener(userId, "sms", event ->
                    System.out.println(userId + " (SMS): " + event.getMessage()));
        }

        EventProducer producer = new EventProducer(dispatcher);

        for (int i = 1; i <= 50; i++) {
            String userId = "user" + i;

            for (int j = 0; j < 10; j++) {
                try {
                    producer.produceEvent(userId, "email", "Email notification " + j + " for " + userId);
                    producer.produceEvent(userId, "sms", "SMS notification " + j + " for " + userId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        dispatcher.shutdown();
    }
}