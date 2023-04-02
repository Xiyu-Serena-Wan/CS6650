import com.rabbitmq.client.ConnectionFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class Connection implements Runnable{

  private final static String QUEUE_NAME = "queue";
  private final static String RABBIT_HOST = "44.201.115.219";
  private final static int RABBIT_PORT = 5672;
  private final static String REDIS_HOST = "18.233.160.235";
  private final static int REDIS_PORT = 6379;
  private final static int RABBIT_POOL_SIZE = 40;
  private final static int REDIS_POOL_SIZE = 1000;
  static ConnectionFactory factory;
  static com.rabbitmq.client.Connection connection;
  private final JedisPool jedisPool;

  public Connection() throws IOException, TimeoutException {
    factory = new ConnectionFactory();
    factory.setUsername("guest1");
    factory.setPassword("guest1");
    factory.setVirtualHost("/");
    factory.setHost(RABBIT_HOST);
    factory.setPort(RABBIT_PORT);
    factory.setConnectionTimeout(6000);
    JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
    jedisPoolConfig.setMaxTotal(REDIS_POOL_SIZE);
    jedisPoolConfig.setMaxIdle(5);
    jedisPoolConfig.setMaxWaitMillis(120000);

    connection = factory.newConnection();
    jedisPool = new JedisPool(jedisPoolConfig, REDIS_HOST, REDIS_PORT);
  }

    @Override
    public void run() {
      ExecutorService channelPool = Executors.newFixedThreadPool(RABBIT_POOL_SIZE);
      for (int i = 0; i < RABBIT_POOL_SIZE; i++) {
        channelPool.execute(new ConsumerThread(connection, QUEUE_NAME, jedisPool));
      }
    }
}
