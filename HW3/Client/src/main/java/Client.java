import com.opencsv.CSVWriter;
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
import java.util.stream.Collectors;

public class Client {
    final static private int NUMTHREADS = 100; // 500
    private static final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(100000))
            .build();

    private static final String CSV_FILE_PATH
            = "./Part1_write "+ NUMTHREADS +".csv";
    private static List<Long> latencies = new ArrayList<Long>();

    private static Double sumLatency = 0.;
    private static Long minLatency;
    private static Long maxLatency;
    private static Double meanLatency;

    private static final int REQUESTS_PER_SECOND = 5;
    private static final int NUM_REQUESTS = 5;
    private static final String[] API_URLS = {
            "http://44.200.208.165:8080/HW3_war/matches/",
            "http://44.200.208.165:8080/HW3_war/stats/"
    };

    public static void main(String[] args) throws IOException, InterruptedException {

        // first create file object for file placed at location
        // specified by filepath
//        File file = new File(CSV_FILE_PATH);

        // create FileWriter object with file as parameter
//        FileWriter outfile = new FileWriter(file);

        // create CSVWriter with ';' as separator
//        CSVWriter writer = new CSVWriter(outfile, ',',
//                CSVWriter.NO_QUOTE_CHARACTER,
//                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
//                CSVWriter.DEFAULT_LINE_END);

        // create a List which contains Data
//        ArrayList<ArrayList<String[]>> myList = new ArrayList<>();
//        for (int i = 0; i < NUMTHREADS; i++) {
//            myList.add(new ArrayList<>());
//        }


        AtomicInteger succeed = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);
        CountDownLatch completed = new CountDownLatch(NUMTHREADS);
        String[] direction = {"left", "right"};
        Long start_time = new Timestamp(System.currentTimeMillis()).getTime();
//        String[][] csv = new String[200000][5];
        for(int i = 0; i < NUMTHREADS; i++){
            ArrayList<String[]> data = new ArrayList<>(); // Arrays.asList(new String[250000][])
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
                            .uri(URI.create("http://44.200.208.165:8080/HW3_war/swipe/"))
//                            .uri(URI.create("http://34.201.21.28:8080/HW2_war/servlet"))
//                            .uri(URI.create("http://hw2-1566521256.us-east-1.elb.amazonaws.com:8080/HW2_war/servlet"))
                            .build();

                    HttpResponse<String> response = null;
//                    Long pre_send = new Timestamp(System.currentTimeMillis()).getTime();
                    for (int k = 0; k < 5; k++) {
                        try {
                            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()); //
//                            System.out.println(response.body());
//                    Thread.sleep(TIMESLEEP);
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
//                    Long end_send = new Timestamp(System.currentTimeMillis()).getTime();
//                    Long latency = end_send - pre_send;
//
                    assert response != null;
//                    String[] rowdata = {String.valueOf(pre_send),
//                            "POST", String.valueOf(latency), String.valueOf(response.statusCode())}; // response.statusCode()

//                    if (data.get((int) (pre_send - start_time)) == null) {
//                        data.set((int) (pre_send - start_time), rowdata);
//                    }
//                    data.add(rowdata);

                }
                completed.countDown();
            };
//            myList.set(i, data);
            new Thread(thread).start();
        }

        while (completed.getCount() > 0){
//            System.out.println(completed.getCount());
            try {
                for (int i = 0; i < NUM_REQUESTS; i++) {
                    // Choose a random API URL
                    String apiUrl = API_URLS[new Random().nextInt(API_URLS.length)];

                    // Generate random data for the request. it has to be one of the existing users.
                    String requestData = generateRandomData();

                    // Send the GET request and record the latency
                    long startTime = System.currentTimeMillis();
                    sendGetRequest(apiUrl, requestData);
                    long endTime = System.currentTimeMillis();
                    latencies.add(endTime - startTime);
                    sumLatency += endTime - startTime;

                    // Sleep for the appropriate amount of time to achieve the desired requests per second
                    Thread.sleep(1000 / REQUESTS_PER_SECOND);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        completed.await();
        Long end_time = new Timestamp(System.currentTimeMillis()).getTime();
        int last_time = Math.toIntExact(end_time - start_time);

        // Print out the min, mean, and max latencies
        meanLatency = sumLatency / latencies.size();
        minLatency = Collections.min(latencies);
        maxLatency = Collections.max(latencies);
//        System.out.println(latencies);
        System.out.println("GetThread: Min latency = " + minLatency + "ms");
        System.out.println("GetThread: Mean latency = " + meanLatency + "ms");
        System.out.println("GetThread: Max latency = " + maxLatency + "ms");

        System.out.println("time spent = " + last_time + " ms when tomcat max num of thread = 200, " +
                "client thread num = " + NUMTHREADS
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

    private static String generateRandomData() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(0,5000) + 1);
    }

}