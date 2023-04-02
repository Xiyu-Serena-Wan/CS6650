import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class GetThread extends Thread {
    private final int NUM_REQUESTS = 5;
    private final String[] API_URLS = {
//            "http://3.95.160.156:8080/RedisServletA/matches/",
//            "http://3.95.160.156:8080/RedisServletA/stats/"
            "http://localhost:8080/RedisServlet/matches/",
            "http://localhost:8080/RedisServlet/stats/"
    };
    private List<Long> latencies = new ArrayList<Long>();
    private Double sumLatency = 0.;
    private Long minLatency;
    private Long maxLatency;
    private Double meanLatency;

    public static void main(String[] args) throws IOException, InterruptedException {
        GetThread getThread = new GetThread();
        getThread.start();

    }
    @Override
    public void run() {
        for (int j = 0; j < 60; j++) {
            try {
                for (int i = 0; i < NUM_REQUESTS; i++) {
                    String apiUrl = API_URLS[new Random().nextInt(API_URLS.length)];

                    String requestData = generateRandomData();

                    // Send the GET request and record the latency
                    long startTime = System.currentTimeMillis();
                    sendGetRequest(apiUrl, requestData);
                    long endTime = System.currentTimeMillis();
                    latencies.add(endTime - startTime);
                    sumLatency += endTime - startTime;

                    Thread.sleep(1000 / NUM_REQUESTS);
                }

                meanLatency = sumLatency / latencies.size();
                minLatency = Collections.min(latencies);
                maxLatency = Collections.max(latencies);
                System.out.println(latencies);
                System.out.println("Min latency = " + minLatency + "ms");
                System.out.println("Mean latency = " + meanLatency + "ms");
                System.out.println("Max latency = " + maxLatency + "ms");

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void sendGetRequest(String apiUrl, String requestData) {
        try {
            URL url = new URL(apiUrl + requestData);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateRandomData() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(0,50000) + 1);
    }
}
