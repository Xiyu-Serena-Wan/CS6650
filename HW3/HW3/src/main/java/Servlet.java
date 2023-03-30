import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@WebServlet(name = "Servlet", value = "/Servlet")
public class Servlet extends HttpServlet {
    private static final String EXCHANGE_NAME = "logs";
    private final ConnectionFactory factory = new ConnectionFactory();
    private Channel channel;
    private SwipeDao swipeDao;

    @Override
    public void init() {
//        factory.setHost("localhost");
        factory.setHost("ec2-52-202-113-159.compute-1.amazonaws.com");
        factory.setPort(5672);
        factory.setUsername("admin"); // create a admin
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        try {
            Connection connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        String host = getServletContext().getInitParameter("jdbcHost");
        String port = "3306";
        String jdbcURL = getServletContext().getInitParameter("jdbcURL");
        String schema = "Swipes";
        String url = jdbcURL + host + ":" + port + "/" + schema + "?serverTimezone=UTC";
        String jdbcUsername = getServletContext().getInitParameter("jdbcUsername");
        String jdbcPassword = getServletContext().getInitParameter("jdbcPassword");

        swipeDao = new SwipeDao(url, jdbcUsername, jdbcPassword);

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
                    channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes(StandardCharsets.UTF_8));
                    System.out.println(" [.] Got '" + message + "', then write to database");
//                    swipeDao.create(new Swipe(swipe.getSwipe(), swipe.getSwiper(), swipe.getSwipee(), swipe.getComment()));
                    System.out.println(" Successfully write");
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                status.setDescription("Write successful");
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
            throws IOException{
        processRequest(request, response);
//        doGet(request, response);
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
//        System.out.println(action);

        if (action.startsWith("/matches")){
            match(request, response);
        } else if (action.startsWith("/stats")) {
            stats(request, response);
        }
    }

    private void match(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Map for storing messages.
        Map<String, String> messages = new HashMap<>();
        request.setAttribute("messages", messages);

        List<String> users = new ArrayList<>();

        String action = request.getServletPath();
        String[] pathParts = action.split("/");
        String userID = pathParts[pathParts.length - 1];

        if (userID == null || userID.trim().isEmpty()) {
            messages.put("success", "Please enter a valid name.");
        } else {
            // Retrieve BlogUsers, and store as a message.
            try {
                users = swipeDao.getSwipesFromMatches(userID);
            } catch (SQLException e) {
                e.printStackTrace();
                throw new IOException(e);
            }
            messages.put("success", "Displaying results for " + userID);
            // Save the previous search term, so it can be used as the default
            // in the input box when rendering FindUsers.jsp.
            messages.put("previousFirstName", userID);
        }
        request.setAttribute("swipes", users);

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>The matches are: " + users + "</h1>");
        out.println("</body></html>");
    }

    private void stats(HttpServletRequest request, HttpServletResponse response)
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
            // Retrieve BlogUsers, and store as a message.
            try {
                like = swipeDao.getStatsFromMatches(userID);
            } catch (SQLException e) {
                e.printStackTrace();
                throw new IOException(e);
            }
            messages.put("success", "Displaying results for " + userID);
            // Save the previous search term, so it can be used as the default
            // in the input box when rendering FindUsers.jsp.
            messages.put("previousFirstName", userID);
        }
        request.setAttribute("stats", like);

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>The stats is: " + like + "</h1>");
        out.println("</body></html>");
    }

}