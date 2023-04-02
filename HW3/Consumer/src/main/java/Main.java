import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {

  public static void main(String[] args) throws IOException, TimeoutException {
    for (int i = 0; i < 6; i++) {
      Thread t1 = new Thread(new Connection());
      t1.start();
    }
  }
}
