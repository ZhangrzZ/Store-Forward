import javax.jms.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Receiver extends Thread{
    private MessageConsumer consumer;
    public Receiver(Session session, Destination topic) throws Exception{
        this.consumer = session.createConsumer(topic);
    }
    public void run() {
        System.out.println("接收线程启动。");
        while(true){
            TextMessage message = null;
            try {
                message = (TextMessage)consumer.receive();
                if (message.getStringProperty("TO").equals(Chatroom.aIdentifier)) {
                    System.out.printf("[%s] Recv from %s:%n", new SimpleDateFormat("MM/dd HH:mm:ss").format(new Date()), message.getStringProperty("FROM"));
                    System.out.println(message.getText());
                } else if (message.getStringProperty("TO").equals("ALL") && !message.getStringProperty("FROM").equals(Chatroom.aIdentifier)) {
                    System.out.printf("[%s] Recv broadcast from %s:%n", new SimpleDateFormat("MM/dd HH:mm:ss").format(new Date()), message.getStringProperty("FROM"));
                    System.out.println(message.getText());
                }
            } catch (Exception e) {
                System.out.println("JMS Closed.");
                return;
            }
        }
    }
}
