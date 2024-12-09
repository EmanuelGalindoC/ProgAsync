package progasync;

public class ScalableEventDispatcher {
    public void dispatchEvent(Event event) throws InterruptedException {
        System.out.println(event.getMessage());
    }
}
