import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.component.jms.JmsConsumer;

import javax.jms.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;


/**
 * @author Hazel
 */
public class Server {

    //默认连接用户名
    private static final String USERNAME = ActiveMQConnection.DEFAULT_USER;
    //默认连接密码
    private static final String PASSWORD = ActiveMQConnection.DEFAULT_PASSWORD;
    //默认连接地址
    private static final String BROKEURL = ActiveMQConnection.DEFAULT_BROKER_URL;

    private static final String [] RECEIVERNAME={"ReceiverNo1","ReceiverNo2"};
    private static final List receiverList = Arrays.asList(RECEIVERNAME);


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
        connectionFactory = new ActiveMQConnectionFactory(Server.USERNAME, Server.PASSWORD, Server.BROKEURL);

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

            String filePath = null;
            String[] fileHead;
            String receiver, fileName;

            //读取消息
            while (true) {
                TextMessage textMessage = (TextMessage) messageConsumer.receive(10000);
                //System.out.println(textMessage.getText());

                if(textMessage==null) {
                    continue;
                }

                //判断文件头，提取receiver & fileName
                if(textMessage.getText().contains(",Sender,")){
                    fileHead = textMessage.getText().split(",");
                    receiver=fileHead[0].toString();
                    fileName=fileHead[2].toString();
                    System.out.println("Receiver:"+receiver+",FileName:"+fileName);
                }
                else{
                    continue;
                }

                //判断文件是否存在
                if (Server.receiverList.contains(receiver)) {
                    filePath = "D:\\myTestFile\\" + fileName+".txt";
                    File file = new File(filePath);

                    //文件不存在，向sender发送接收文件请求，收取并存储文件
                    if (!file.exists()) {
                        connection = connectionFactory.createConnection();
                        connection.start();
                        session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
                        destination = session.createTopic("HelloWAM");
                        messageProducer = session.createProducer(destination);

                        String msg = "Sender,Server,noFound";
                        TextMessage message = session.createTextMessage(msg);
                        //System.out.println("发送消息:"+msg);
                        messageProducer.send(message);
                        session.commit();

                        file.getParentFile().mkdirs();
                        file.createNewFile();
                        FileWriter fw = new FileWriter(file, false);

                        while(true){
                            textMessage = (TextMessage)messageConsumer.receive(10000);
                            //System.out.println(textMessage.getText());

                            if(textMessage.getText().contains("Server,Sender,"+fileName)){
                                while(true){
                                    textMessage = (TextMessage)messageConsumer.receive(10000);
                                    //System.out.println("Accept msg : "+textMessage.getText());
                                    if(textMessage.getText().contains("fileEnd")) {
                                        break;
                                    }
                                    fw.write(textMessage.getText()+'\n');
                                }
                                fw.flush();
                                fw.close();
                                System.out.println("文件"+fileName+"存储完毕!");
                                break;
                            }
                        }
                    }
                    //文件存在，向sender发送不需发送文件信息
                    else {
                        messageProducer = session.createProducer(destination);
                        String msg = "Sender,Server,Exist";
                        TextMessage message = session.createTextMessage(msg);
                        //System.out.println("发送消息:"+msg);
                        messageProducer.send(message);
                        session.commit();
                    }

                    //将存储的文件发往receiver
                    BufferedReader reader = null;
                    reader = new BufferedReader(new FileReader(file));
                    String tempString = null;

                    connection = connectionFactory.createConnection();
                    connection.start();
                    session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
                    destination = session.createTopic("HelloWAM");
                    messageProducer = session.createProducer(destination);

                    String msg = receiver+",Server,"+fileName;
                    TextMessage message = session.createTextMessage(msg);
                    //System.out.println("发送消息:"+msg);
                    messageProducer.send(message);
                    while ((tempString = reader.readLine()) != null) {
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

                }
            }

        } catch (JMSException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}