import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;

public class WatchdogReceiver implements Runnable{
    private MessageConsumer consumer = null;
    public WatchdogReceiver(MessageConsumer consumer) {
        this.consumer = consumer;
    }
    public void run() {
        System.out.println("Watchdog线程已启动");
        while (Watchdog.isReceiverRunning) {
            try {
                TextMessage message = (TextMessage) consumer.receive(3000);
                if (message == null) {
                    System.out.println("[INFO]Target Disconnected.");
                    Watchdog.isReceiverRunning = false;
                    return;
                }
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }
}
