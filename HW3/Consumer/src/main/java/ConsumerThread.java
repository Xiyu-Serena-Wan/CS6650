import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsumerThread implements Runnable{
  private final Connection connection;
  private final String QUEUE_NAME;
  private final JedisPool jedisPool;
  private final Gson gson = new Gson();

  public ConsumerThread(Connection connection, String QUEUE_NAME, JedisPool jedisPool) {
    this.connection = connection;
    this.QUEUE_NAME = QUEUE_NAME;
    this.jedisPool = jedisPool;
  }

  @Override
  public void run() {
      try {
        final Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println("Wait for the messages.");
        channel.basicQos(10);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

          Swipe swipe = gson.fromJson(message, Swipe.class);
          Jedis jedis = null;
          try {
            jedis = jedisPool.getResource();
            storeSwipe(swipe, jedis);
          } catch (Exception exception) {
            exception.printStackTrace();
          } finally {
            try {
              if (null != jedis) {
                jedis.close();
              }
            } catch (Exception jedisE) {
              jedisE.printStackTrace();
            }
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            System.out.println("Thread = " + Thread.currentThread() + " gets " + message);
          }
        };
        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});
      } catch (IOException ex) {
        ex.printStackTrace();
        Logger.getLogger(ConsumerThread.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

  void storeSwipe(Swipe swipe, Jedis jedis) {
    String swiperAndSwipeeId = "swiper:" + swipe.getSwiper() + "swipee:" + swipe.getSwipee() + "swipe:" + swipe.getSwipe();
    jedis.hmset(swiperAndSwipeeId,
            new HashMap<>() {{
              put("swiper", swipe.getSwiper());
              put("swipee", swipe.getSwipee());
              put("swipe", swipe.getSwipe());
            }});
  }
}
