// TFTPSim.java
// This class is the beginnings of an error simulator for TFTP server 
// based on UDP/IP. The simulator receives a read or write packet from a client and
// passes it on to the server.  
// One socket (68) is used to receive from the client, and another to send/receive


import java.io.*;
import java.net.*;
import java.util.*;

public class TFTPSim {
	
	public static enum Mode { OFF, ON};
	
	public static final int TIP = 2;
	public static final int PACKET = 1;
	public static final int DATA = 1;
	public static final int ACK = 2;
	public static final int REQ = 3;

	// UDP datagram packets and sockets used to send / receive
	private DatagramSocket socket;
	private Mode mode;
	private byte packetType;
	private int blockNumber;
	private int errorDetail;
	private Error error;
	
	public TFTPSim()
	{
	   try {
	      // Construct a datagram socket and bind it to port 68 on the local host machine.
	      // This socket will be used to receive UDP Datagram packets from clients.
	      socket = new DatagramSocket(68);
	   } catch (SocketException se) {
	      se.printStackTrace();
	      System.exit(1);
	   }
	}
	
	
	/**
	 * Forms a datagram packet to pass to thread
	 * @return
	 * @throws UnknownHostException
	 */
	public DatagramPacket FormPacket() throws UnknownHostException
	{
		byte data[] = new byte[516];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		try {
			System.out.println("Waiting for packet from client on port "+socket.getPort());
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Recieved Packet");
		return packet;
	
	}
	
	
	
	/**
	 * A menu that asks the user to pick either a Packet error or a TID error
	 */
	public void setupErrorMode() {
		Scanner scanner = new Scanner (System.in);
		int input;
		
		System.out.println("Which type of error do you wish to generate? (select by number):");
		System.out.println("0) No Error");
		System.out.println("4) Packet Error");
		System.out.println("5) TID Error");
		System.out.println("Choose: ");
		
		for(;;) {
			input = scanner.nextInt();
			if(input==0) {
				this.error = new Error();
				scanner.close();
				return;
			}
			if(input==4) {
				packetError(scanner);
				break;
			} else if(input==5) {
				tipError(scanner);
				break;
			}
			System.out.println("Invalid option.  Please try again:");
		}
		scanner.close();
	}
	
	
	/**
	 * Used to select packet error details
	 * @param scanner - an input scanner passed into the method to eliminate the need for a new scanner
	 */
	private void packetError(Scanner scanner) {
		//Select Packet Type
		System.out.println("Packet Error:");
		System.out.println();
		System.out.println("Packet type to corrupt:");
		System.out.println("1) DATA");
		System.out.println("2) ACK");
		System.out.println("3) REQ");
		for(;;) {
			this.packetType = scanner.nextByte();
			if (this.packetType!=DATA || this.packetType!=ACK || this.packetType!=REQ) break;
			System.out.println("Invalid block selection.  Please try again:");
		} 
		
		System.out.println();
		System.out.println();
		
		if (this.packetType == REQ) {
			System.out.println();
			System.out.println("Select type of error you wish to generate in the request packet:");
			System.out.println("1) No Starting Zero");
			System.out.println("2) Invalid Op Code");
			System.out.println("3) No File Name");
			System.out.println("4) No Zero After Filename");
			System.out.println("5) No Mode");
			System.out.println("6) No Zero After Mode");
			System.out.println("7) Data After Zero");
			
			for(;;) {
				this.errorDetail = scanner.nextInt();
				if (this.errorDetail>0 || this.errorDetail<=7) break;
				System.out.println("Invalid option.  Please try again:");
			}
			error = new Error(TIP, this.packetType, new BlockNumber(this.blockNumber),this.errorDetail);
			
			return;
		}
		
		//Select Block Number
		System.out.println("Please select a block number to trigger the error: ");
		for(;;) {
			this.blockNumber = scanner.nextInt();
			if (this.blockNumber>0) break;
			System.out.println("Invalid block number selection.  Please try again:");
		}
		
		System.out.println();
		System.out.println("Select type of error you wish to generate in the data packet:");
		System.out.println("1) No Starting Zero");
		System.out.println("2) Invalid Op Code");
		System.out.println("3) Invalid Block Number");
		
		if (this.packetType == DATA) {
			System.out.println("4) Too Much Data");
		} else if (this.packetType == ACK) {
			System.out.println("4) Add Data to ACK");
		}
		for(;;) {
			this.errorDetail = scanner.nextInt();
			if (this.errorDetail>0 || this.errorDetail<=4) break;
			System.out.println("Invalid option.  Please try again:");
		}
		error = new Error(TIP, this.packetType, new BlockNumber(this.blockNumber),this.errorDetail);
	}
	
	
	/**
	 * TIP error detail menu
	 * @param scanner - an input scanner passed into the method to eliminate the need for a new scanner
	 */
	private void tipError(Scanner scanner) {
		System.out.println("TIP Error:");
		System.out.println();
		System.out.println("Packet type to initiate error:");
		System.out.println("1) DATA");
		System.out.println("2) ACK");
		for(;;) {
			this.packetType = scanner.nextByte();
			if (this.packetType!=DATA || this.packetType!=ACK) break;
			System.out.println("Invalid block selection.  Please try again:");
		} 
		
		System.out.println();
		System.out.println();
		
		System.out.println("Please select a block number to trigger the error: ");
		for(;;) {
			this.blockNumber = scanner.nextInt();
			if (this.blockNumber>0 || (this.blockNumber==0 && this.packetType == (byte) 1)) break;
			System.out.println("Invalid block number selection.  Please try again:");
		} 
		error = new Error(TIP, this.packetType, new BlockNumber(this.blockNumber));
	}
	
	
	
	public static void main( String args[] ) throws UnknownHostException
	{
		TFTPSim s = new TFTPSim();
		Scanner scanner = new Scanner (System.in);
		String input;
		
		for(;;) {
			System.out.println("Do you wish to use the simulator? (y/n):");
			input = scanner.next();
			
			if(input.equals('y')||input.equals('Y')) {
				s.mode = Mode.ON;
				break;
			} else if(input.equals('n')||input.equals('N')) {
				s.mode = Mode.OFF;
				break;
			}
			System.out.println("Invalid option.  Please try again:");
		}
		scanner.close();
		
		for(;;){
			Thread connect = new Thread ( new TFTPSimManager(s.FormPacket(),s.error));
			if(s.mode==Mode.ON) s.setupErrorMode();
			else s.error = new Error();
	        connect.start();
		}
	}
}




