import com.google.gson.Gson;
import com.rabbitmq.client.*;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import java.util.*;
import java.util.concurrent.TimeoutException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

@WebServlet(name = "RedisServlet", value = "/RedisServlet")
public class RedisServlet extends HttpServlet {
    private static final String QUEUE_NAME = "queue";
    private final ConnectionFactory factory = new ConnectionFactory();
    private Channel channel;
    private final static String REDIS_HOST = "18.233.160.235";
    private final static int REDIS_PORT = 6379;
    private final static int REDIS_POOL_SIZE = 1000;
    private JedisPool jedisPool;
    private final static String RABBIT_HOST = "44.201.115.219";
    private final static int RABBIT_PORT = 5672;

    @Override
    public void init() {
//        factory.setHost("localhost");
        factory.setHost(RABBIT_HOST);
        factory.setPort(RABBIT_PORT);
        factory.setUsername("guest1"); // create a admin
        factory.setPassword("guest1");
        factory.setVirtualHost("/");

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(REDIS_POOL_SIZE);
        jedisPoolConfig.setMaxIdle(5);
        jedisPoolConfig.setMaxWaitMillis(120000);

        try {
            Connection connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            jedisPool = new JedisPool(jedisPoolConfig, REDIS_HOST, REDIS_PORT);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        Gson gson = new Gson();

        try {
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = request.getReader().readLine()) != null) {
                sb.append(s);
            }

            Swipe swipe = gson.fromJson(sb.toString(), Swipe.class);
            String message = sb.toString();
            Status status = new Status();
            if (isSwipeValid(swipe) == 200) {
                status.setSuccess(true);
                try {
                    channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
                    System.out.println(" Successfully write");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                response.setStatus(HttpServletResponse.SC_OK);
            } else if (isSwipeValid(swipe) == 404){
                status.setSuccess(false);
                status.setDescription("User not found");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);

            } else {
                status.setSuccess(false);
                status.setDescription("Invalid inputs");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            response.getOutputStream().print(gson.toJson(status));
            response.getOutputStream().flush();
        } catch (Exception ex) {
            ex.printStackTrace();
            Status status = new Status();
            status.setSuccess(false);
            status.setDescription(ex.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getOutputStream().print(gson.toJson(status));
            response.getOutputStream().flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException{
        processRequest(request, response);
    }

    private Integer isSwipeValid(Swipe swipe) {
        if (!(swipe.getSwipe().equals("left") || swipe.getSwipe().equals("right")))
            return 400;
        int swiper = Integer.parseInt(swipe.getSwiper());
        int swipee = Integer.parseInt(swipe.getSwipee());
        if (swiper >= 1 && swiper <= 50000
                && swipee >= 1 && swipee <= 50000
                && swipe.getComment().length() <= 256) return 200;
        else if (swiper < 1 || swiper > 50000 || swipee < 1 || swipee > 50000) return 404;
        else return 400;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getServletPath();
        Jedis jedis = jedisPool.getResource();
        if (action.startsWith("/matches")){
            match(request, response, jedis);
        } else if (action.startsWith("/stats")) {
            stats(request, response, jedis);
        }
    }

    private void match(HttpServletRequest request, HttpServletResponse response, Jedis jedis)
            throws IOException {
        // Map for storing messages.
        Map<String, String> messages = new HashMap<>();
        request.setAttribute("messages", messages);

        String action = request.getServletPath();
        String[] pathParts = action.split("/");
        String userID = pathParts[pathParts.length - 1];
        List<String> users = new ArrayList<>();

        if (userID == null || userID.trim().isEmpty()) {
            messages.put("success", "Please enter a valid name.");
        } else {
            users = getSwipesFromMatches(userID, jedis, users);
            messages.put("success", "Displaying results for " + userID);
            messages.put("previousFirstName", userID);
        }
        request.setAttribute("swipes", users);

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>The matches are: " + users + "</h1>");
        out.println("</body></html>");
    }

    private void stats(HttpServletRequest request, HttpServletResponse response, Jedis jedis)
            throws IOException {
        // Map for storing messages.
        Map<String, String> messages = new HashMap<>();
        request.setAttribute("messages", messages);

        Map<String, Integer> like = new HashMap<>();

        String action = request.getServletPath();
        String[] pathParts = action.split("/");
        String userID = pathParts[pathParts.length - 1];

        if (userID == null || userID.trim().isEmpty()) {
            messages.put("success", "Please enter a valid name.");
        } else {
            like = getStatsFromMatches(userID, jedis);
            messages.put("success", "Displaying results for " + userID);
            messages.put("previousFirstName", userID);
        }
        request.setAttribute("stats", like);

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>The stats is: " + like + "</h1>");
        out.println("</body></html>");
    }

    public List<String> getSwipesFromMatches(String userID, Jedis jedis, List<String> users) {
        String pattern = "swiper:" + userID + "*swipe:right";
        ScanParams params = new ScanParams().match(pattern);
        ScanResult<String> scanResult = jedis.scan("0", params);

        for (String key : scanResult.getResult()) {
            users.add(jedis.hget(key, "swipee"));
        }
        return users;
    }

    public Map<String, Integer> getStatsFromMatches(String userID, Jedis jedis) {
        Map<String, Integer> likes = new HashMap<>();

        String left_pattern = "swiper:" + userID + "*swipe:left";
        ScanParams left_params = new ScanParams().match(left_pattern);
        ScanResult<String> left_scanResult = jedis.scan("0", left_params);
        Integer left = left_scanResult.getResult().size();
        likes.put("left", left);

        String right_pattern = "swiper:" + userID + "*swipe:right";
        ScanParams right_params = new ScanParams().match(right_pattern);
        ScanResult<String> right_scanResult = jedis.scan("0", right_params);
        Integer right = right_scanResult.getResult().size();
        likes.put("right", right);

        return likes;
    }

}