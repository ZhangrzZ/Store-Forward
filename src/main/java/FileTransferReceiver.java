import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.BlobMessage;
import org.apache.activemq.command.ActiveMQQueue;

import javax.jms.*;
import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileTransferReceiver extends Thread{
    private static MessageConsumer consumer;
    public FileTransferReceiver(ActiveMQSession session) throws JMSException{
        Queue queue = new ActiveMQQueue("FileTransfer");
        consumer = session.createConsumer(queue);

    }
    public void run(){
        System.out.println("接收文件线程已启动");
        while (true) {
            try {
                Message message = consumer.receive();
                if (message instanceof BlobMessage && message.getStringProperty("TO").equals(Chatroom.aIdentifier)) {
                        String fileName = message.getStringProperty("FILE.NAME");
                        System.out.println("接收到来自" + message.getStringProperty("FROM") + "的文件");
                        message.getLongProperty("FILE.SIZE");
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("请指定文件保存地址");
                        fileChooser.setSelectedFile(new File(fileName));
                        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                            File file = fileChooser.getSelectedFile();
                            OutputStream os = new FileOutputStream(file);
                            System.out.println("" + fileName);
                            InputStream inputStream = ((BlobMessage) message).getInputStream();
                            byte[] buff = new byte[256];
                            int len;
                            while ((len = inputStream.read(buff)) > 0)
                                os.write(buff, 0, len);
                            os.close();
                            System.out.println("" + fileName);
                        }
                }
            } catch (Exception e) {
                System.out.println("Connection closed.");
                return;
            }
        }
    }
}
