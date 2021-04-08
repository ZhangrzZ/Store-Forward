import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.BlobMessage;
import org.apache.activemq.ActiveMQSession;

import javax.jms.*;
import javax.swing.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;


public class Chatroom {

    private final static String brokerURL = "tcp://localhost:61616";
    private final static String prompt = String.join("\n",
            "--- Welcome to Lab1: Chatroom ---",
            "|1. Chat with a target			|",
            "|2. Send broadcast message		|",
            "|3. File transfer				|",
            "|4. Quit						|",
            "---------------------------------");
    public static String aIdentifier = "";
    public static Scanner input = null;
    private static ActiveMQSession session = null;
    private static Destination msgDestination = null;
    private static Watchdog watchdog = null;
    public static void main(String[] args) throws Exception{
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(
                ActiveMQConnectionFactory.DEFAULT_USER,
                ActiveMQConnectionFactory.DEFAULT_PASSWORD,
                brokerURL
        );

        ActiveMQConnection connection = (ActiveMQConnection) factory.createConnection();
        connection.start();
        session = (ActiveMQSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        System.out.println("成功连接中间件ActiveMQ Broker@" + brokerURL);
        msgDestination = session.createTopic("Message");
        Receiver receiver = new Receiver(session, msgDestination);
        receiver.start();
        FileTransferReceiver fileTransferReceiver = new FileTransferReceiver(session);
        fileTransferReceiver.start();
        Thread.yield();
        input = new Scanner(System.in);
        System.out.println("输入本机标识：");
        aIdentifier = input.next();
        watchdog = new Watchdog(session);
        watchdog.startSenderWatchdog(session);
        boolean sym = true;
        while (sym) {
            System.out.println(prompt);
            switch (input.nextInt()) {
                case 1 -> chat(false);
                case 2 -> chat(true);
                case 3 -> sendFile();
                case 4 -> sym = false;
            }
        }
        session.close();
        connection.close();
        System.out.println("Bye.");
    }
    public static void chat(boolean broadcast) {
        String aTarget, msg;
        int i = 0;
        try {
            MessageProducer producer = session.createProducer(msgDestination);
            if (!broadcast) {
                System.out.println("Input target ID:");
                aTarget = input.next();
            } else {
                aTarget = "ALL";
            }
            watchdog.startReceiver();
            Thread.sleep(300L);
            System.out.print("Input [OVER] to stop.\n>>");
            while (!(msg = input.next()).equals("OVER")) {
                i++;
                TextMessage textMessage = session.createTextMessage(msg);
                textMessage.setStringProperty("TO", aTarget);
                textMessage.setStringProperty("FROM", aIdentifier);
                producer.send(textMessage);
                System.out.printf("[%s] Sent #%d %s%n", new SimpleDateFormat("MM/dd HH:mm:ss").format(new Date()), i, broadcast ? "BROADCAST" : "");
                System.out.print(">>");
            }
            producer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void sendFile() throws JMSException{
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("请选择要传送的文件");
        System.out.println("请输入目标主机ID：");
        String target = input.next();
        if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
            return;
        File file = fileChooser.getSelectedFile();
        BlobMessage blobMessage = session.createBlobMessage(file);
        blobMessage.setStringProperty("FROM", Chatroom.aIdentifier);
        blobMessage.setStringProperty("TO", target);
        blobMessage.setStringProperty("FILE.NAME", file.getName());
        blobMessage.setLongProperty("FILE.SIZE", file.length());
        System.out.println("开始发送文件：" + file.getName() + "，文件大小：" + file.length() + "字节");
        //producer.send(blobMessage);
        System.out.println("文件发送完成，已上传至FTP。");
    }
}
