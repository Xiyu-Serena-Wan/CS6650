import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ExchangeReceiver1 {

    private static final String EXCHANGE_NAME = "logs";
    protected static SwipeDao swipeDao;

    public static void main(String[] argv) throws Exception {

        Gson gson = new Gson();

        Properties prop = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			InputStream in = classLoader.getResourceAsStream("b.properties");
//        InputStream in = new BufferedInputStream(new FileInputStream("b.properties"));
        prop.load(in);     ///加载属性列表
        String host = prop.getProperty("ipAddress");
        String port = "3306";
        String schema = "Swipes";
        String url = "jdbc:mysql://" + host + ":" + port + "/" + schema + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String user = prop.getProperty("user");
        String password = prop.getProperty("password");
        swipeDao = new SwipeDao(url, user, password);


        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
        factory.setHost("ec2-52-202-113-159.compute-1.amazonaws.com");
        factory.setPort(5672);
        factory.setUsername("admin"); // create a admin
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        channel.basicQos(1);

        System.out.println(" [x] Awaiting RPC requests");
        AtomicInteger count = new AtomicInteger();
        Map<String, List<String>> messages_right = new HashMap<>();
        Long start_time = new Timestamp(System.currentTimeMillis()).getTime();
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {

            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [.] Received message (" + message + ")");

            count.addAndGet(1);

            List<String> res = ExchangeReceiver1.parser(message);

            String index = res.get(0);
            String dire = res.get(1);
            String value = res.get(2);
            if (dire.equals("right")){
                if (messages_right.containsKey(index)){
                    if (messages_right.get(index).size() < 101)
                        messages_right.get(index).add(value);
                } else {
                    messages_right.put(index, new ArrayList<>());
                    messages_right.get(index).add(value);
                }
            }
            Swipe swipe = gson.fromJson(message, Swipe.class);
            try {
                swipeDao.create(new Swipe(swipe.getSwipe(), swipe.getSwiper(), swipe.getSwipee(), swipe.getComment()));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            System.out.println(" Successfully write");

        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
        Long end_time = new Timestamp(System.currentTimeMillis()).getTime();
        int last_time = Math.toIntExact(end_time - start_time);
        System.out.println("time spent = " + last_time);
    }

    private static List<String> parser(String message){
        List<String> res = new ArrayList(){};
        Gson gson = new Gson();
        Swipe obj = gson.fromJson(message, Swipe.class);
        res.add(obj.getSwiper());
        res.add(obj.getSwipe()); // left or right
        res.add(obj.getSwipee());
        return res;
    }
}