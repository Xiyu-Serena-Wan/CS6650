import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@WebServlet(name = "Servlet", value = "/Servlet")
public class Servlet extends HttpServlet {
    private static final String EXCHANGE_NAME = "logs";
    private final ConnectionFactory factory = new ConnectionFactory();
    private Connection connection;
    private Channel channel;

    @Override
    public void init() throws ServletException {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
        factory.setHost("ec2-3-84-168-223.compute-1.amazonaws.com");
        factory.setPort(5672);
        factory.setUsername("admin"); // create a admin
        factory.setPassword("guest");
        factory.setVirtualHost("/");
//        Connection connection = null;
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
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
                    System.out.println(" [x] Requesting (" + message + ")");
                    channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes("UTF-8"));
                    System.out.println(" [.] Got '" + message + "'");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                status.setDescription("Write successful");
                // send it to the queue
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
            throws IOException {
        processRequest(request, response);
    }

    private Integer isSwipeValid(Swipe swipe) {
        if (!(swipe.getSwipe().equals("left") || swipe.getSwipe().equals("right")))
            return 400;
        int swiper = Integer.parseInt(swipe.getSwiper());
        int swipee = Integer.parseInt(swipe.getSwipee());
        if (swiper >= 1 && swiper <= 5000
                && swipee >= 1 && swipee <= 1000000
                && swipe.getComment().length() <= 256) return 200;
        else if (swiper < 1 || swiper > 5000 || swipee < 1 || swipee > 1000000) return 404;
        else return 400;
    }
}