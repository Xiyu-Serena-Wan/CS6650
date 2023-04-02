import org.apache.commons.lang3.RandomStringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {
    final static private int NUM_THREADS = 100; // 500
    private static final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(100000))
            .build();

    private static final List<Long> latencies = new ArrayList<>();
    private static Double sumLatency = 0.;
    private static final int  REQUESTS_PER_SECOND = 5;
    private static final String[] API_URLS = {
//            "http://3.83.167.40:8080/RedisServletA/matches/",
//            "http://3.83.167.40:8080/RedisServletA/stats/",
            "http://localhost:8080/RedisServlet/matches/",
            "http://localhost:8080/RedisServlet/stats/"
    };

    public static void main(String[] args) throws InterruptedException {

        AtomicInteger succeed = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);
        CountDownLatch completed = new CountDownLatch(NUM_THREADS);
        String[] direction = {"left", "right"};
        Long start_time = new Timestamp(System.currentTimeMillis()).getTime();
        for(int i = 0; i < NUM_THREADS; i++){
            Runnable thread = () -> {
                for (int j = 0; j < 5000; j++) { // 1000
                    int d = ThreadLocalRandom.current().nextInt(0,2);
                    int swiper = ThreadLocalRandom.current().nextInt(0,50000) + 1;
                    int swipee = ThreadLocalRandom.current().nextInt(0,50000) + 1;
                    int length = ThreadLocalRandom.current().nextInt(255) + 1;
                    String generatedString = RandomStringUtils.random(length, true, true);
                    HttpRequest request = HttpRequest.newBuilder()
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    "{\"swipe\"=" + direction[d] + // d.nextInt(2)
                                            "," + "\"swiper\"= " + swiper + // swiper.nextInt(5000) % 5000
                                            ",\n" + "\"swipee\"= " + swipee + // swipee.nextInt(1000000) % .1000000
                                            ",\n" + "comment=" + generatedString +
                                            "}"))
//                            .uri(URI.create("http://3.83.167.40:8080/RedisServletA/swipe/"))
                            .uri(URI.create("http://localhost:8080/RedisServlet/swipe/"))
                            .build();

                    HttpResponse<String> response = null;
                    for (int k = 0; k < 5; k++) {
                        try {
                            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()); //
                            succeed.addAndGet(1);
                            break;
                        } catch (IOException | InterruptedException e) {
                            if (k < 4) continue;
                            else {
                                fail.addAndGet(1);
                                throw new RuntimeException(e);

                            }
                        }
                    }

                    assert response != null;

                }
                completed.countDown();
            };
            new Thread(thread).start();
        }

        while (completed.getCount() > 0){
            System.out.println(completed.getCount());
            try {
                for (int i = 0; i < REQUESTS_PER_SECOND; i++) {
                    String apiUrl = API_URLS[new Random().nextInt(API_URLS.length)];
                    String requestData = generateRandomData();

                    long startTime = System.currentTimeMillis();
                    sendGetRequest(apiUrl, requestData);
                    long endTime = System.currentTimeMillis();

                    latencies.add(endTime - startTime);
                    sumLatency += endTime - startTime;

                    Thread.sleep(1000 / REQUESTS_PER_SECOND);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        completed.await();
        Long end_time = new Timestamp(System.currentTimeMillis()).getTime();
        int last_time = Math.toIntExact(end_time - start_time);

        double meanLatency = sumLatency / latencies.size();
        Long minLatency = Collections.min(latencies);
        Long maxLatency = Collections.max(latencies);
        System.out.println(latencies);
        System.out.println("Min latency = " + minLatency + "ms");
        System.out.println("Mean latency = " + meanLatency + "ms");
        System.out.println("Max latency = " + maxLatency + "ms");

        System.out.println("time spent = " + last_time + " ms when tomcat max num of thread = 200, " +
                "client thread num = " + NUM_THREADS
                + ", " + "successful request number = " + succeed
                + ", " + "failed request number = " + fail
                + ", " + "request per second = " + succeed.floatValue() / last_time * 1000
                + ", " + "start time = " + start_time
                + ", " + "end time = " + end_time
        );
    }

    private static void sendGetRequest(String apiUrl, String requestData) {
        try {
            URL url = new URL(apiUrl + requestData);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
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

    private static String generateRandomData() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(0,5000) + 1);
    }

}