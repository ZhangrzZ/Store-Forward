import javax.jms.*;
import java.util.concurrent.*;

/**
 * @author Ruzhen Zhang
 */
public class Watchdog {

    private final static ScheduledExecutorService scheduler= Executors.newScheduledThreadPool(1);
    private static MessageProducer producer = null;
    private static MessageConsumer consumer = null;
    public static boolean isReceiverRunning = false;
    public Watchdog(Session session) throws JMSException{
        Destination topic = session.createTopic("Watchdog");
        producer = session.createProducer(topic);
        consumer = session.createConsumer(topic);
    }
    public void startSenderWatchdog(Session session){

        Runnable runnable = () -> {
            TextMessage textMessage = null;
            try {
                textMessage = session.createTextMessage("PING");
                textMessage.setStringProperty("ID", Chatroom.aIdentifier);
                producer.send(textMessage);
            } catch (JMSException e) {
                System.out.println("Stop sending PING.");
                scheduler.shutdown();
            }
        };
        //延迟执行时间（秒）
        long delay = 0;
        //执行的时间间隔（秒）
        long period = 1;
        scheduler.scheduleAtFixedRate(runnable, delay, period, TimeUnit.SECONDS);
    }
    public void startReceiver() {
        if (!isReceiverRunning) {
            isReceiverRunning = true;
            Runnable runnable = new WatchdogReceiver(consumer);
            Thread t = new Thread(runnable);
            t.start();
        }
    }
}
