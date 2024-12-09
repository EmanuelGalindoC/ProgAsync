package progasync;

public class NotificationSystem {
    public static void main(String[] args) throws InterruptedException {
        ScalableEventDispatcher dispatcher = new ScalableEventDispatcher(10);

        for (int i = 1; i <= 50; i++) {
            String userId = "user" + i;

            dispatcher.registerListener(userId, "email", event ->
                    System.out.println(userId + " (Email): " + event.getMessage()));
            dispatcher.registerListener(userId, "sms", event ->
                    System.out.println(userId + " (SMS): " + event.getMessage()));
        }

        EventProducer producer = new EventProducer(dispatcher);

        for (int i = 1; i <= 50; i++) {
            String userId = "user" + i;

            for (int j = 0; j < 10; j++) {
                producer.produceEvent(userId, "email", "Email notification " + j + " for " + userId);
                producer.produceEvent(userId, "sms", "SMS notification " + j + " for " + userId);
            }
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }
}
