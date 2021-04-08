import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.component.jms.JmsConsumer;

import javax.jms.*;
import java.io.File;
import java.io.FileWriter;


/**
 * @author Hazel
 */
public class ReceiverNo2 {

    //默认连接用户名
    private static final String USERNAME = ActiveMQConnection.DEFAULT_USER;
    //默认连接密码
    private static final String PASSWORD = ActiveMQConnection.DEFAULT_PASSWORD;
    //默认连接地址
    private static final String BROKEURL = ActiveMQConnection.DEFAULT_BROKER_URL;

    private static final String RECEIVERNAME="ReceiverNo2";

    public static void main(String[] args) throws Exception{

        //连接工厂
        ConnectionFactory connectionFactory;
        //连接
        Connection connection = null;

        Session session;//会话 接受或者发送消息的线程
        Destination destination;//消息的目的地

        MessageConsumer messageConsumer;//消息的消费者
        MessageProducer messageProducer;//消息的生产者


        //实例化连接工厂
        connectionFactory = new ActiveMQConnectionFactory(ReceiverNo2.USERNAME, ReceiverNo2.PASSWORD, ReceiverNo2.BROKEURL);

        try {
            //通过连接工厂获取连接
            connection = connectionFactory.createConnection();
            //启动连接
            connection.start();
            //创建session
            session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            //创建一个连接HelloWorld的消息队列
            destination = session.createTopic("HelloWAM");
            //点对点消息队列，如果要创建发布订阅模式，是要改成session.createTopic("HelloWAM");，整个类其他地方都不用变




            //创建消息消费者
            messageConsumer = session.createConsumer(destination);
            String[] fileHead;
            String fileName;

            //读取消息
            while(true){

                TextMessage textMessage = (TextMessage)messageConsumer.receive(10000);

                if(textMessage==null) {
                    continue;
                }

                if(textMessage.getText().contains(ReceiverNo2.RECEIVERNAME+",Server,")){
                    fileHead = textMessage.getText().split(",");
                    fileName=fileHead[2].toString();
                    System.out.println("Receiver:"+ReceiverNo2.RECEIVERNAME+",fileName:"+fileName);

                    String path = "D:\\my"+ReceiverNo2.RECEIVERNAME+"File\\" + fileName+".txt";
                    File file = new File(path);
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                    FileWriter fw = new FileWriter(file, false);

                    while (true) {
                        textMessage = (TextMessage) messageConsumer.receive(10000);
                        //System.out.println("Accept msg : " + textMessage.getText());
                        if (textMessage.getText().contains("fileEnd")) {
                            break;
                        }
                        fw.write(textMessage.getText() + '\n');
                    }
                    fw.flush();
                    fw.close();
                    System.out.println("文件"+fileName+"已接收!");
                    break;
                }
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }


    }
}