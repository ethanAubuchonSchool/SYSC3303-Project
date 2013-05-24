
import java.util.Scanner;

public class ServerEx  implements Runnable{
Server sr;
    public ServerEx(Server s) {
    
    sr=s;
    
        this.scanner = new Scanner(System.in);}


 private final Scanner scanner;

    @Override
    public void run() {
        String entry;

        while(sr.isActive()) {
            System.out.println("Enter 'exit' to terminate server");
            entry = scanner.next();
            if (entry.equalsIgnoreCase("exit")) {
                sr.stopReceiving();
                System.exit(1);
            }
        }
    }


}
