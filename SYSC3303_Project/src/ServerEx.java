
import java.util.Scanner;

public class ServerEx  implements Runnable{
	Server sr;
	private final Scanner scanner;
    
	public ServerEx(Server s) {
		sr=s;
		this.scanner = new Scanner(System.in);
    }
	
	@Override
    public void run() {
		String entry;
		while(sr.isActive()) {
			System.out.println("Enter 'exit' to terminate server");
            entry = scanner.next();
            if (entry.equalsIgnoreCase("exit")) {
                sr.stopReceiving();
                System.exit(1);
            } else if (entry.equals("cd")) {
            	System.out.println("Enter new directory");
            	sr.setDir(scanner.nextLine());
            }
        }
    }
}
