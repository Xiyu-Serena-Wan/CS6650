import com.opencsv.CSVWriter;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Part1 {
    //    private static final long TIMESLEEP = 1;
    final static private int NUMTHREADS = 500; // 500
    private static final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(100000))
            .build();

    private static final String CSV_FILE_PATH
            = "./Part1_write "+ NUMTHREADS +".csv";


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
                for (int j = 0; j < 1000; j++) { // 1000
                    int d = ThreadLocalRandom.current().nextInt(0,2);
                    int swiper = ThreadLocalRandom.current().nextInt(0,5000) + 1;
                    int swipee = ThreadLocalRandom.current().nextInt(0,1000000) + 1;
                    int length = ThreadLocalRandom.current().nextInt(256) + 1;
                    String generatedString = RandomStringUtils.random(length, true, true);
                    HttpRequest request = HttpRequest.newBuilder()
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    "{\"swipe\"=" + direction[d] + // d.nextInt(2)
                                            "," + "\"swiper\"= " + swiper + // swiper.nextInt(5000) % 5000
                                            ",\n" + "\"swipee\"= " + swipee + // swipee.nextInt(1000000) % .1000000
                                            ",\n" + "comment=" + generatedString +
                                            "}"))
//                            .uri(URI.create("http://localhost:8080/HW2_war_exploded/servlet/"))
//                            .uri(URI.create("http://34.201.21.28:8080/HW2_war/servlet"))
//                            .uri(URI.create("http://18.205.3.46:8080/HW2_war/servlet"))
                            .uri(URI.create("http://hw2-1566521256.us-east-1.elb.amazonaws.com:8080/HW2_war/servlet"))
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
        completed.await();
        Long end_time = new Timestamp(System.currentTimeMillis()).getTime();
        int last_time = Math.toIntExact(end_time - start_time);

        System.out.println("time spent = " + last_time + " ms when tomcat max num of thread = 200, " +
                "client thread num = " + NUMTHREADS
                + ", " + "successful request number = " + succeed
                + ", " + "failed request number = " + fail
                + ", " + "request per second = " + succeed.floatValue() / last_time * 1000
                + ", " + "start time = " + start_time
                + ", " + "end time = " + end_time
        );

        // x -> x.stream()
//        List<String[]> merged = myList.stream().flatMap(Collection::stream).sorted(new Comparator<String[]>() {
//            @Override
//            public int compare(String[] o1, String[] o2) {
//                return o1[0].compareTo(o2[0]);
//            }
//        }).collect(Collectors.toList());
//         Now sort by address instead of name (default).
//        String[] headerRecord = {"Start Timestamp", "Request Type", "Latency", "Response Code"};
//        writer.writeNext(headerRecord);
//        writer.writeAll(merged);
//         closing writer connection
//        writer.close();




    }

}