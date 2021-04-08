import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;


public class Sender {

    //默认连接用户名
    private static final String USERNAME = ActiveMQConnection.DEFAULT_USER;
    //默认连接密码
    private static final String PASSWORD = ActiveMQConnection.DEFAULT_PASSWORD;
    //默认连接地址
    private static final String BROKEURL = ActiveMQConnection.DEFAULT_BROKER_URL;

    public static void main(String[] args) throws Exception{

        ConnectionFactory connectionFactory;
        Connection connection = null;
        Session session;
        Destination destination;
        MessageConsumer messageConsumer;//消息的消费者
        MessageProducer messageProducer;//消息的生产者


        //连接工程
        connectionFactory = new ActiveMQConnectionFactory(USERNAME,PASSWORD,BROKEURL);

        try {
            while(true){
                //连接
                connection = connectionFactory.createConnection();
                connection.start();
            /*
            createSession参数取值
            * 1、为true表示启用事务
            * 2、消息的确认模式
            * AUTO_ACKNOWLEDGE  自动签收
            * CLIENT_ACKNOWLEDGE 客户端自行调用acknowledge方法签收
            * DUPS_OK_ACKNOWLEDGE 不是必须签收，消费可能会重复发送
            * 在第二次重新传送消息的时候，消息
               头的JmsDelivered会被置为true标示当前消息已经传送过一次，
               客户端需要进行消息的重复处理控制。
            * */

                //会话
                session = connection.createSession(true,Session.AUTO_ACKNOWLEDGE);
                //点对点消息队列，如果要创建发布订阅模式，是要改成session.createTopic("HelloWAM");，整个类其他地方都不用变
                destination = session.createTopic("HelloWAM");
                //消息生产者


                Scanner s = new Scanner(System.in);
                System.out.print("输入传输对象：");
                String receiver = s.nextLine();
                //String receiver="ReceiverNo1";
                System.out.print("输入文件名：");
                String fileName = s.nextLine();
                //String fileName="test#1";
                String filePath="D:\\myFile\\"+fileName+".txt";
                String fileHead=receiver+",Sender,"+fileName;
                System.out.println("fileHead:"+fileHead);

                //String fileName = "D:\\myFile\\test#1.txt";
                File file = new File(filePath);
                BufferedReader reader = null;


                messageProducer = session.createProducer(destination);

                TextMessage message = session.createTextMessage(fileHead);
                messageProducer.send(message);
                session.commit();

                messageConsumer = session.createConsumer(destination);
                //读取消息
                while(true){
                    TextMessage textMessage = (TextMessage)messageConsumer.receive(10000);
                    //System.out.println("Accept msg : "+textMessage.getText());



                    if(textMessage.getText().contains("Sender,Server,Exist")){
                        System.out.println("file exist.");
                        break;
                    }else if(textMessage.getText().contains("Sender,Server,noFound")){
                        System.out.println("file noFound.");
                        System.out.println("向Server发送文件。");

                        reader = new BufferedReader(new FileReader(file));
                        String tempString = null;
                        // 一次读入一行，直到读入null为文件结束

                        connection = connectionFactory.createConnection();
                        connection.start();
                        session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
                        destination = session.createTopic("HelloWAM");
                        messageProducer = session.createProducer(destination);

                        String msg = "Server,Sender,"+fileName;
                        message = session.createTextMessage(msg);
                        //System.out.println("发送消息:"+msg);
                        messageProducer.send(message);
                        while ((tempString = reader.readLine()) != null) {
                            // 显示行号
                            msg = tempString;
                            message = session.createTextMessage(msg);
                            //System.out.println("发送消息:"+msg);
                            messageProducer.send(message);
                        }
                        msg = "fileEnd";
                        message = session.createTextMessage(msg);
                        //System.out.println("发送消息:"+msg);
                        messageProducer.send(message);
                        session.commit();

                        reader.close();
                        break;
                    }
                }
            }
        } catch (JMSException e) {
        }finally {
        }


    }
}
