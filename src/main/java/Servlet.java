import com.google.gson.Gson;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

@WebServlet(name = "Servlet", value = "/Servlet")
public class Servlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        Gson gson = new Gson();

        try {
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = request.getReader().readLine()) != null) {
                sb.append(s);
            }

            Swipe swipe = (Swipe) gson.fromJson(sb.toString(), Swipe.class);

            Status status = new Status();
            if (isSwipeValid(swipe) == 200) {
                status.setSuccess(true);
                status.setDescription("Write successful");
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
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private Integer isSwipeValid(Swipe swipe) {
//        System.out.println(swipe.getSwipe());
//        System.out.println(swipe.getSwiper());
//        System.out.println(swipe.getSwipee());
//        System.out.println(swipe.getComment());
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