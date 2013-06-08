import java.io.IOException;
import java.net.*;
import java.util.Scanner;

//import javax.swing.*;


public class Server {
	public static final int WELL_KNOWN_PORT = 69;
	public static final int BUFFER_SIZE = 512+4;
	private boolean acceptIncoming;
	private String directory;
	
	

	//public static final byte DEFAULT = 0;//Default Server will be type 0

	
	private DatagramSocket wellKnown;

	//private byte threadMode;
	
	public Server() {
		try {
			wellKnown = new DatagramSocket(WELL_KNOWN_PORT);
			this.acceptIncoming = true;
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	
	public DatagramPacket recieveTFTP() {
		byte data[] = new byte[BUFFER_SIZE];
		DatagramPacket incomingPacket = new DatagramPacket(data, data.length);
		try {
			wellKnown.receive(incomingPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Recieved Packet");
		return incomingPacket;
	}
	
	public void setDir(String s) {
		this.directory = s;
	}
	
	public static void main(String [] args) {
		Server s = new Server();
		Scanner in = new Scanner(System.in);
		
		System.out.println("Please enter a directory (blank for default): ");
		s.setDir(in.nextLine());
				
		(new Thread(new ServerEx(s))).start();
		
		for(;;) {
			Thread st = new Thread(new ServerThread(s.recieveTFTP(), s.directory));
			st.start();
		}
	}
	protected void stopReceiving() {
            this.acceptIncoming = false;
	}
	/**
	 * @return the isActive
	 */
	protected boolean isActive() {
            return acceptIncoming;
	}
} 
