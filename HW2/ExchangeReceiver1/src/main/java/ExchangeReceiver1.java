import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ExchangeReceiver1 {

    private static final String EXCHANGE_NAME = "logs";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
        factory.setHost("ec2-3-84-168-223.compute-1.amazonaws.com");
        factory.setPort(5672);
        factory.setUsername("admin"); // create a admin
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");
//        channel.queueDeclare(queueName, false, false, false, null);
//        channel.queuePurge(queueName);

        channel.basicQos(1);

        System.out.println(" [x] Awaiting RPC requests");
        AtomicInteger count = new AtomicInteger();
        Map<String, List<String>> messages_right = new HashMap<>();
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
//                    .Builder()
//                    .correlationId(delivery.getProperties().getCorrelationId())
//                    .build();

//            String response = "";
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [.] Received message (" + message + ")");
//            response += message;
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
                };


//            } finally {
//                channel.basicPublish("", delivery.getProperties().getReplyTo(), null, response.getBytes(StandardCharsets.UTF_8));
                // replyProps
//                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
//                System.out.println("count: " + count);
//                if (count.intValue() == 100){
//                    System.out.println(messages_right);
//                }
            }
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
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