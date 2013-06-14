import java.io.*;
import java.net.*;

//define a TFTPSimManager class;
public class TFTPSimManager  implements Runnable
{
	public static final int MESSAGE_SIZE = 512;
	public static final int BUFFER_SIZE = MESSAGE_SIZE+4;
	public static final byte MAX_BLOCK_NUM = 127;
	public static final byte DATA = 3;
	public static final byte ACK = 4;
	public static final int REQ = 3;
	
	public static final int TIP = 2;
	public static final int PACKET = 1;
	public static final int NETWORK = 0;
	
	public static final int DELAY = 8;
	public static final int DELETE = 9;
	public static final int DUPLICATE = 10;


	
	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket incomingPacket, outgoingPacket;
	private DatagramSocket socket;
	private boolean exitNext;
	private int clientPort,serverPort;
	private InetAddress clientIP, serverIP;
	
	//Data for error generation
	private int errorType;
	private byte[] comparitorA;
	private int errorDetail;
	private byte packetType;

	
	public TFTPSimManager( DatagramPacket dp, Error e ) {
	  	// Get a reference to the data inside the received datagram.
	    incomingPacket = dp;
	    serverPort = 69;
	    try {
			serverIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			System.out.println("IP error.");
			System.exit(1);
		}
	    clientIP = dp.getAddress();
	    exitNext = false;
	    
	    //Comparitor is used to check for block numbers and op codes
	    //Because a request error can check for read or write request
	    //two comparitors are needed
	    this.comparitorA = new byte[4];
	    this.comparitorA[0] = 0;
	    this.comparitorA[1] = e.getBlockType();
	    this.comparitorA[2] = e.getBlockNumber().getCurrent()[0];
	    this.comparitorA[3] = e.getBlockNumber().getCurrent()[1];
	    //If error is to be made in REQ packet then comparitorA is set to RRQ and comparitorB is set to WRQ
	    if (this.comparitorA[1] == 1) {//Both comparitors are set to DATA
	    	this.comparitorA[1] = 3;
	    } else if (this.comparitorA[1] == 2) {//Both comparitors are set to ACK
	    	this.comparitorA[1] = 4;
	    }
	    //Sets error values
	    this.errorType = e.getErrorType();
	    this.packetType = e.getBlockType();
	    this.errorDetail = e.getErrorDetail();	    
	}

	
	public void run() {
		try {
			byte temp[];
			//  Construct  sendPacket to be sent to the server (to port 69)
			clientPort = incomingPacket.getPort();
			
			//Checks if this is the block that the user requested an error in
			//If so it creates the error and returns a data array
			temp = findError(outgoingPacket = new DatagramPacket(incomingPacket.getData(),incomingPacket.getLength(),serverIP,serverPort));
			
			//Prints out data contents
			for(int i = 0; i < temp.length; i++) System.out.print(temp[i]);
			System.out.println();
			
			//Forms outgoing packet with data array from error generator
			outgoingPacket = new DatagramPacket(temp,temp.length,serverIP,serverPort);
			System.out.println("Recieved Packet from client");
			socket = new DatagramSocket();
			//Sends data to server
			socket.send(outgoingPacket);
			System.out.println("Forwarded packet to server");
			//Checks if that was the last packet
			//If so, thread ends
			if(checkForEnd(outgoingPacket.getData()))
			{
				System.out.println("Closing simulator thread");
				System.out.println("Select type of error you wish to generate in the request packet:");
				System.out.println("1) No Starting Zero");
				System.out.println("2) Invalid Op Code");
				System.out.println("3) No File Name");
				System.out.println("4) No Zero After Filename");
				System.out.println("5) No Mode");
				System.out.println("6) Invalid Mode");
				System.out.println("7) No Zero After Mode");
				System.out.println("8) Data After Zero");
				return;
			}
			
			//Loops until thread ends 
			for(;;) {
				//forwardPacket receives a packet, checks if an error needs to be made,
				//forwards the packet (with or without error).
				//it then returns true if it was the final packet
				if(forwardPacket()) 
				{
					System.out.println("Closing simulator thread");
					System.out.println("Select type of error you wish to generate in the request packet:");
					System.out.println("1) No Starting Zero");
					System.out.println("2) Invalid Op Code");
					System.out.println("3) No File Name");
					System.out.println("4) No Zero After Filename");
					System.out.println("5) No Mode");
					System.out.println("6) Invalid Mode");
					System.out.println("7) No Zero After Mode");
					System.out.println("8) Data After Zero");
					return;//thread closes
				}
			}
			
			
			
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		  
	}
  
	//This sets a flag letting the thread know when the last data packet has been forwarded
	//Upon forwarding the ack packet with a matching block number, it returns true to let the thread know to close
	//Otherwise it returns false
	private boolean checkForEnd(byte data[]) {
		if(data[0]==0&&data[1]==DATA) {
			int i;
			for(i = 4; i < data.length; i++) {
				if(data[i] == 0) {
					exitNext = true;
					return false;
				}
			}
		} else if(data[0]==0 && data[1]==ACK && exitNext) {
			return true;
		} else if(data[0]==0 && data[1]==5) {
			return true;
		}
	  
		return false;
	}
  
	private boolean forwardPacket() {
		byte data[] = new byte[BUFFER_SIZE];
		int outgoingPort;
		InetAddress outgoingIP;
		try {
			//Recieves a packet
			incomingPacket = new DatagramPacket(data,BUFFER_SIZE,InetAddress.getLocalHost(),clientPort);
			socket.receive(incomingPacket);
			//If packet is from the client port it is sent to the server
			if(incomingPacket.getPort()==this.clientPort) {
				System.out.println("Recieved packet from client");
				outgoingPort = this.serverPort;
				outgoingIP = this.serverIP;
			//If it is from the server or an unknown port before the server port is set it is sent to the client
			} else if (this.serverPort == 69 || incomingPacket.getPort()==this.serverPort) {
				if (this.serverPort == 69) this.serverPort = incomingPacket.getPort();
				System.out.println("Recieved packet from server");
				outgoingPort = this.clientPort;
				outgoingIP = this.clientIP;
			} else {//TIP error
				System.out.println("Error with port number");
				System.exit(1);
				outgoingPort = 0; // Won't be reached but is used to stop errors in eclipse
				outgoingIP = this.serverIP;
			}
			//Checks if an error needs to be made
			//returns appropriate byte array
			byte temp[] = findError(outgoingPacket = new DatagramPacket(incomingPacket.getData(),incomingPacket.getLength(),outgoingIP,outgoingPort));
			//if temp is null, pack is "deleted" and no message is sent
			if(temp == null) {
				System.out.println("Packet Deleted");
			} else {
				//Prints out array to be sent
				for(int i = 0; i < temp.length; i++) System.out.print(temp[i]);
				System.out.println();
				//Packs and forwards data
				outgoingPacket = new DatagramPacket(temp,temp.length,outgoingIP,outgoingPort);
				socket.send(outgoingPacket);
				System.out.print("Forwarded packet to ");
				if(outgoingPacket.getPort()==this.clientPort) System.out.println("client port: "+outgoingPacket.getPort());
				else if(outgoingPacket.getPort()==this.serverPort) System.out.println("server port: "+outgoingPacket.getPort());
				else System.out.println("unknown");
			}
			//returns whether or not the end has been reached
			return checkForEnd(incomingPacket.getData());
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
			
		//Never reached
		//Used to suppress eclipse compile errors
		return false; 	
	}
	
	//finds if error packet is needed
	//if so, returns error data
	//else returns original data
	private byte[] findError(DatagramPacket packet) {
		byte temp[] = new byte[2];
		System.arraycopy(packet.getData(), 2, temp, 0, 2);
		if(this.packetType == 1 || this.packetType == 2) {
			for (int i = 0; i < 4; i++) {
				if(packet.getData()[i] != this.comparitorA[i]) return packet.getData();
			}
			return makeError(packet);
		} else if (this.packetType == 3) {
			if(packet.getData()[0]==0 && (packet.getData()[1]==1 || packet.getData()[1]==2)) return makeError(packet);
		}
		return packet.getData();
	}
	
	//Causes the error
	private byte[] makeError(DatagramPacket packet) {
		System.out.println();
		System.out.println("Error being generated.");
		System.out.println();
		byte[] block = new byte[BUFFER_SIZE];
		if (this.errorType == PACKET) { // Generates a packet error
			System.arraycopy(packet.getData(), 0, block, 0, packet.getLength());
			byte temp[];
			if(this.packetType == REQ) {
				int i;
				boolean set;
				switch (this.errorDetail) {
					case 1:// No starting zero
						block[0]++;
						break;
						
					case 2:// invalid op code
						block[1]=-2;
						break;
						
					case 3://No file name
						temp = new byte[4];
						System.arraycopy(block, 0, temp, 0, temp.length);
						block = temp;
						break;
						
					case 4: //No zero after filename
						for(i = 4; i < block.length; i++) {
							if (block[i]==0) break;
						}
						temp = new byte[i];
						System.arraycopy(block, 0, temp, 0, temp.length);
						block = temp;
						break;
						
					case 5: //No mode
						for(i = 4; i < block.length; i++) {
							if (block[i]==0) break;
						}
						temp = new byte[i+1];
						System.arraycopy(block, 0, temp, 0, temp.length);
						temp[temp.length-1] = 0;
						block = temp;
						break;
						
					case 6: // change mode
						for(i = 1; i < block.length; i++) {
							if (block[i] == 0) break;
						}
						String fake = new String("fake");
						byte fakeMode[] = new byte[i + fake.getBytes().length + 2];
						System.arraycopy(fake.getBytes(), 0, fakeMode, i+1, fake.getBytes().length);
						fakeMode[fakeMode.length-1] = 0;
						block = fakeMode;
						break;
						
					case 7: //no closing zero
						set = false;
						for(i = 4; i < block.length; i++) {
							if (block[i]==0) {
								if (set) break;
								else set = true;
							}
						}
						temp = new byte[i+1];
						System.arraycopy(block, 0, temp, 0, temp.length);
						block = temp;
						break;
						
					case 8: //data after closing zero
						set = false;
						for(i = 4; i < block.length; i++) {
							if (block[i]==0) {
								if (set) break;
								else set = true;
							}
						}
						temp = new byte[i+3];
						System.arraycopy(block, 0, temp, 0, temp.length);
						temp[temp.length-2] = 0;
						temp[temp.length-1] = 7;
						block = temp;
						break;
						
					default:
						System.out.println("Error: invalid error details.");
						break;
				}
				
				return block;
				
			} else if (this.packetType == 1) {//DATA
				switch (this.errorDetail) {
					case 1:// no starting 0
						block[0]++;
						break;
						
					case 2:// invalid opcode
						block[1]++;
						break;
						
					case 3://invalid block number
						temp = new byte[2];
						System.arraycopy(block, 2, temp, 0, 2);
						BlockNumber bn = new BlockNumber(temp);
						System.arraycopy(bn.getNext(), 0, block, 2, 2);
						break;
						
					case 4://no data
						temp = new byte[BUFFER_SIZE + 3];
						temp[BUFFER_SIZE] = 1;
						temp[BUFFER_SIZE+1] = 1;
						temp[BUFFER_SIZE+2] = 1;
						block = temp;
						break;
						
					default:
						System.out.println("Error: invalid error details.");
						break;
				}
				
				return block;
			} else if (this.packetType == 2) {//ACK
				switch (this.errorDetail) {
					case 1:// no starting zero
						block[0]++;
						break;
						
					case 2:// invalid opcode
						block[1]=-3;
						break;
						
					case 3:// invalid block number
						temp = new byte[2];
						System.arraycopy(block, 2, temp, 0, 2);
						BlockNumber bn = new BlockNumber(temp);
						System.arraycopy(bn.getNext(), 0, block, 2, 2);
						break;
						
					case 4:// data after block number
						temp = new byte[7];
						temp[4] = 1;
						temp[5] = 1;
						temp[6] = 1;
						block = temp;
						break;
						
					default:
						System.out.println("Error: invalid error details.");
						break;
				}
				
				return block;
			} else {
				System.out.println("Invalid packet type chosen.");
				System.exit(1);
			}
			return packet.getData();
		} else if (this.errorType == TIP) {//TIP Error generator
			DatagramPacket temp = new DatagramPacket(packet.getData(),packet.getLength(),packet.getAddress(),packet.getPort());
			try {
				//Sends data on a fake port
				DatagramSocket fakePort = new DatagramSocket();
				fakePort.send(temp);
				//recieves error and closes port
				fakePort.receive(temp = new DatagramPacket(new byte[BUFFER_SIZE],BUFFER_SIZE));
				fakePort.close();
				// makes sure TID error is formatted properly
				if(temp.getData()[0]==0 && temp.getData()[1]==5 && temp.getData()[2]==0 && temp.getData()[3]==5) {
					int i;
					for(i = 4; i < BUFFER_SIZE; i++) {
						if(temp.getData()[i]==0) break;
					}
					if (i+1 >= BUFFER_SIZE - 4) {
						System.out.println("Error: TID Error Message has no closing zero");
						return packet.getData();
					}
					for(int j = i; j < BUFFER_SIZE; j++) {
						if (temp.getData()[j]!=0) {
							System.out.println("Error: TID Error Message has data after closing zero");
							return packet.getData();
						}
					}
					System.out.println("TID Error recieved");
					return packet.getData();
				} else {
					//responds that TID error is encoded properly and returns data to be sent from real port
					System.out.println("Error: TID Error encoded improperly");
					return packet.getData();
				}
			} catch (SocketException e) {
				e.printStackTrace();
				System.out.println("Socket Exception Error");
				System.exit(1);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Socket Exception Error");
				System.exit(1);
			}
		} else if (this.errorType == NETWORK) {
			if (this.errorDetail == DELAY) {
				//Delay the packet
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(1);
				}
				this.comparitorA = new byte[4];//resets the comparitors so error wont happen again on a resent packet
				return packet.getData();
			} else if (this.errorDetail == DELETE) {
				//deletes packet
				this.comparitorA = new byte[4];//resets the comparitors so error wont happen again on a resent packet
				return null;
			} else if (this.errorDetail == DUPLICATE) {
				//sends a duplicate packet
				try {
					this.socket.send(packet);
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(1);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				this.comparitorA = new byte[4];//resets the comparitors so error wont happen again on a resent packet
				return packet.getData();
			}
		} else {
			System.out.println("Incorrect error type.  Shutting down.");
			System.exit(1);
		}
		return packet.getData();
	}
}
