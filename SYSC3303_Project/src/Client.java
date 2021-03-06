
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client  {	
	public static enum Request {ERROR, READ, WRITE};
	public static final int MESSAGE_SIZE = 512;
	public static final int BUFFER_SIZE = MESSAGE_SIZE+4;
	public static final byte MAX_BLOCK_NUM = 127;
	public static final byte DATA = 3;
	public static final byte ACK = 4;
	public static final byte ERROR = 5;
	public static final String MODE = "octet";
	public static final int SIM_PORT = 68;
	public static final int SERVER_PORT = 69;
	
	private String file;
	private String dir;
	private DatagramPacket sendPacket; //
	private DatagramSocket sendReceiveSocket;
	private BlockNumber bnum;
	private int sendPort;
	private int wellKnownPort;
	private byte msg[];
	private int counter;
	private DatagramPacket block;
	private DatagramPacket readpack;
	private boolean simulator;
	private InetAddress serverIP;
	
	private byte ack[];

	private int count=0;
	private int TIMEOUT = 1000;	
	int MAX_TIMEOUTS = 3;														//Maximum timeouts before abort
	private boolean received;   
    //private boolean ex; 	//to terminate
	public Client(){
		sendPort = 0;
		try {
			// Construct a datagram socket and bind it to any available
			// port on the local host machine. This socket will be used to
			// send and receive UDP Datagram packets.
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {   // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void main(String []args){
		Client c = new Client();
		c.run();
	}

	public void run(){
		Scanner scanner = new Scanner (System.in);
		for(;;){
			System.out.println("Do you wish to use the error simulator? (y/n)");
			String temp = scanner.next();
			if(temp.equals("y") || temp.equals("Y")) {
				this.wellKnownPort = SIM_PORT;
				break;
			} else if (temp.equals("n") || temp.equals("N")) {
				this.wellKnownPort = SERVER_PORT;
				break;
			}
			System.out.println("Invalid option. Try again.");
		}
		for(;;){
			System.out.println("Please enter the server's IP address:");
			String ip = scanner.next();
			try {
				serverIP = InetAddress.getByName(ip);
				break;
			} catch (UnknownHostException e1) {
				System.out.println("Invalid host.");
				System.out.println("Try again.");
				System.out.println();
				System.out.println();
			}
		}
		for(;;) {
			counter = 0;
			msg = new byte[BUFFER_SIZE];
			System.out.println("1) Read");
			System.out.println("2) Write");
			System.out.println("3) To terminate");
			System.out.println("Select a mode:");
			int request = scanner.nextInt();
			
			//check if user wants to exit
			if(request == 3){
				System.exit(0);
			}
			System.out.println("Change directory? (y/n): ");
			String temp = scanner.next();
			if(temp.equals("y") || temp.equals("Y")) {
				System.out.println("Enter directory (d for default): ");
				temp = scanner.next();
				if(temp.equals("d") || temp.equals("D")) {
					this.dir = new String();
				} else {
					this.dir = temp;
				}
			} else {
				this.dir = new String();
			}
			
			
			System.out.println("Type File name: ");
			file = scanner.next();
			dir+=file;
			
			//if writing the file to the server, check if the file actually exists
			//if not, return to the main menu with error message
			if(request == 2 && new File(file).exists() == false){
				System.out.println(file + " cannot be found.");
				continue;
			}
			
			//if writing the file to the server, check if the file is allowed to be read
			//if not, return to the menu with error message
			if(request == 2){
				try{
					BufferedInputStream in = new BufferedInputStream(new FileInputStream(dir));
					in.close();
				} catch (Exception e){
					System.out.println("You do not have the proper read permission for " + file + " to write to server.");
					continue;
				}
			}
			
			msg[0] = 0;
			
			
			
			this.sendPort = 0;
			int iterator = 2;
			if(request==3){
				scanner.close();
				terminate();
			}
			else if (request == 2 ){
				msg[1] = 2;
				System.arraycopy(file.getBytes(),0,msg,iterator,file.getBytes().length);
				iterator+=file.getBytes().length;
				msg[iterator] = 0;
				iterator ++;
				System.arraycopy(MODE.getBytes(),0,msg,iterator,MODE.getBytes().length);
				iterator+=MODE.getBytes().length;
				msg[iterator] = 0;	
				sendData(iterator+1,2);
				clientWrite();
				
			}else if(request == 1){
				msg[1] = 1;
				System.arraycopy(file.getBytes(),0,msg,iterator,file.getBytes().length);
				iterator+=file.getBytes().length;
				msg[iterator] = 0;
				iterator ++;
				System.arraycopy(MODE.getBytes(),0,msg,iterator,MODE.getBytes().length);
				iterator+=MODE.getBytes().length;
				msg[iterator] = 0;	
				sendData(iterator+1,1);
				clientRead();		
			}else{
				System.out.println("Request error");
			}
		}
	}
	
	public void sendData(int size, int reqt){  //add new para to method reqt(r or w=1 or 2)
		if(reqt==1){
			received = false;
			count=0;
			System.out.println("Sending RT: Read to port: "+this.wellKnownPort);
			for (int i = 0; i < size; i++) {
				System.out.print(msg[i]);
			}
			System.out.println();
			sendPacket = new DatagramPacket(this.msg, size,this.serverIP, this.wellKnownPort);
			try{
				sendReceiveSocket.send(sendPacket);
			}catch(IOException e){
				System.out.println(e);
				System.exit(1);
			}
		}else if(reqt==2){       
			received = false;
			count=0;
        	System.out.println("Sending RT= Write to port: "+this.wellKnownPort);
        	for (int i = 0; i < size; i++) {
				 System.out.print(msg[i]);
		 	}
			System.out.println();
		    sendPacket = new DatagramPacket(this.msg, size,this.serverIP, this.wellKnownPort);	
			try{  
				sendReceiveSocket.send(sendPacket);
			}catch(IOException e){ 
				System.out.println(e); 
				System.exit(1); 
			}	
		}
	    System.out.println("Client: Packet sent.");
	}
	
	public void clientWrite(){
		try {
			//Opens an input stream
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(dir));
			
			bnum = new BlockNumber();
			
			byte[] pack = null;//buffer used to send data to client
			byte[] data;//buffer used to hold data read from file
			int n = -1;
			
			//Reads data from file and makes sure data is still read
			for(;;) {
				data = new byte[MESSAGE_SIZE];
				byte ack[] = new byte[BUFFER_SIZE];//Ack data buffer
				DatagramPacket temp = new DatagramPacket (ack, ack.length);//makes new packet to receive ack from client
				try {
					System.out.println("Waiting for Ack " + counter);
					int count = 0;
					for(;;){
						sendReceiveSocket.setSoTimeout(TIMEOUT);
						try {
							sendReceiveSocket.receive(temp);//Receives ack from client on designated socket
							break;
						} catch (SocketTimeoutException e) {
							count ++;
							System.out.println("Timeout #"+count);
							if (count >= MAX_TIMEOUTS) {
								System.out.println("Too many timeouts. Connection lost.  Server may no longer be working.");
								return;
							}
							if(counter == 0){
								sendReceiveSocket.send(new DatagramPacket(msg,msg.length,this.serverIP,this.wellKnownPort));
							} else {
								sendReceiveSocket.send(new DatagramPacket(pack,pack.length,this.serverIP, this.sendPort));
							}
						}
					}
					counter++;
					if (this.sendPort == 0) {
						this.sendPort = temp.getPort();
					}
					
					if (sendPort != temp.getPort() || !serverIP.equals(temp.getAddress())){  // checking for error 5
						byte[] error5 = new byte[BUFFER_SIZE];
						error5[0] = 0;
						error5[1] = ERROR;
						error5[2] = 0;
						error5[3] = 5;
						String errorMessage;
						
						if(sendPort != temp.getPort()){
							System.out.println("Error 5: unknown port");
							errorMessage = "Unknow Port";
						}else{
							System.out.println("Error 5: unknown IP Address");
							errorMessage = "Unknow IP Address";
						}
						
						System.arraycopy(errorMessage.getBytes(),0,error5,4,errorMessage.getBytes().length);
						error5[4 + errorMessage.getBytes().length] = 0;
						DatagramPacket errorPacket = new DatagramPacket(error5, error5.length,temp.getAddress(), temp.getPort());
						sendReceiveSocket.send(errorPacket);
					}
					
					System.out.println("Recieved Ack ");
					byte bn[] = new byte[2];
					System.arraycopy(temp.getData(), 2, bn, 0, 2);
					
					if (temp.getData()[1] == ERROR){
						if(temp.getData()[3] == 4){
							System.out.println("Error 4: packet has formatting errors.");
						} else if(temp.getData()[3] == 1){
							System.out.println("Error 1: " + file + " cannot be found on the Server.");
						} else if(temp.getData()[3] == 2){
							System.out.println("Error 2: Server does not have permission to read " + file);
						} else if(temp.getData()[3] == 3){
							System.out.println("Error 3: Server has ran out of space. Transfer failed.");
						} else if(temp.getData()[3] == 6){
							System.out.println("Error 6: Cannot write " + file + " to Server. Server already has file with same name. Transfer failed.");
						} else if(temp.getData()[3] == 5){
							System.out.println("Error 5: TIP error Message Recieved");
						} else {
							System.out.println("Unknown error received.");
						}
						in.close();
						return;
					}
					
					
						if(temp.getData()[0] == 0 && temp.getData()[1] == ACK && bnum.lessThanOrEqualTo(bn)) {
							System.out.println("Ack good");
						}else{
							byte[] error4 = new byte[BUFFER_SIZE];
							error4[0] = 0;
							error4[1] = ERROR;
							error4[2] = 0;
							error4[3] = 4;
							String errorMessage;
							
							if(temp.getData()[0] != 0){
								errorMessage = "first byte of ACK is not 0";
								System.out.println(errorMessage);
							}else if(temp.getData()[1] != ACK){
								errorMessage = "second byte of ACK is not ACK code '4'";
								System.out.println(errorMessage);
							}else{
								errorMessage = "Block number is not matched";
								System.out.println(errorMessage);
							}
							
							System.arraycopy(errorMessage.getBytes(),0,error4,4,errorMessage.getBytes().length);
							error4[4 + errorMessage.getBytes().length] = 0;
							DatagramPacket errorPacket = new DatagramPacket(error4, error4.length,temp.getAddress(), temp.getPort());
							System.out.println("the error 4 has been sent");								
							sendReceiveSocket.send(errorPacket);
							return;
						}
					
					
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Ack Reception Error");
					System.exit(1);
				}								
				
				if (n < MESSAGE_SIZE && n >= 0) {
					System.out.println("Transfer Complete");
					break;
				}
				
				bnum.increment();
				
				n = in.read(data);
				//System.out.println("n = "+n);
				if(n < 0) {
					pack = new byte[5];
					pack[0] = 0;
					pack[1] = DATA;
					System.arraycopy(bnum.getCurrent(), 0, pack, 2, 2);
					pack[4] = 0;
				} else {
					pack = new byte[BUFFER_SIZE];//new empty buffer created
					//first four bits are set to TFTP Requirements
					pack[0] = 0;
					pack[1] = DATA;
					System.arraycopy(bnum.getCurrent(), 0, pack, 2, 2);
					//Data read from file
					System.arraycopy(data,0,pack,4,n);
				}
				
				System.out.println("Sending data to port: " + this.sendPort);
			    block = new DatagramPacket(pack,pack.length,this.serverIP, this.sendPort);
				sendReceiveSocket.send(block);
				System.out.println("Sent data block");

			}
			
			//closes input stream
			in.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			//handleError();
			return;
		} catch (IOException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			//handleError();
			return;
		}
	}
	
	public void clientRead(){
		
		this.bnum = new BlockNumber();
		this.bnum.increment();	
		byte[] block = bnum.getCurrent();
		this.counter = 1;
		
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dir));
			for(;;){
				int length;
				byte[] packet = getBlock(bnum.getCurrent());
				
				if(packet == null) {
					out.close();
					return;
				}
				byte[] temp = new byte[packet.length-4];
				
				block[0] = packet[2];
				block[1] = packet[3];
				System.arraycopy(packet, 4,temp, 0, packet.length-4);
				for(length = 4; length < temp.length; length++) {
					//System.out.print(temp[length]+","); // Used to check incoming byte array for debugging
					if (temp[length] == 0) break;
				}
				
				//check the free space on current partition is less than the length of the received data
				//if true, then there are not enough available space, therefore should throw an error and exit
				long partition = (new File(dir).getFreeSpace());
				if(partition < (long)length){
					System.out.println("Need " + length + " bytes of free space, but current partition only has " + partition + " bytes.");
					System.out.println("Client will exit.");
					
					//send ERROR 3
					//TO BE IMPLEMENTED
					byte [] error3 = {(byte)0, (byte)5, (byte)0, (byte)3};
					
					DatagramPacket errorPacket = new DatagramPacket(error3, error3.length,this.serverIP, sendPort);
					sendReceiveSocket.send(errorPacket);
					
					System.out.println("Transfer complete");
					//exit
					out.close();
					return;
				}
				
				try {
					out.write(temp,0,length);
				}catch(IOException e) {
					String message = "File too large";
					System.out.println(message);
					byte data[] = new byte[4+message.getBytes().length+1];
					data[0] = 0;
					data[1] = 5;
					data[2] = 0;
					data[3] = 1;
					System.arraycopy(message.getBytes(), 0, data, 4, message.getBytes().length);
					data[data.length-1] = 0;
					this.sendReceiveSocket.send(new DatagramPacket(data,data.length,this.serverIP,this.sendPort));
					out.close();
					return;
				}
				System.out.println("Sending ack");
				sendAck(block);					
				
				System.out.println("length is: "+length);
				
				
				if(length<MESSAGE_SIZE) {
					out.close();
					break;
				}
				
				this.bnum.increment();				
			}
		}  catch (IOException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			 System.exit(1);
			return;
		}
	}
	
	
	private void sendAck(byte[] blockNumber) {
		byte temp[] = new byte[4];
		temp[0] = 0;
		temp[1] = 4;
		System.arraycopy(blockNumber, 0, temp, 2, 2);
		
		try {
			readpack = new DatagramPacket (temp, temp.length, this.serverIP, this.sendPort);
			sendReceiveSocket.send(readpack);
		} catch (IOException e) {
			System.out.println("Send Packet Error");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public byte[] getBlock(byte[] blockNumber) {
		byte msg[] = new byte[516];
		System.out.println("getting data block");
		for(;;) {
			
			DatagramPacket temp = new DatagramPacket (msg, msg.length);
			

			try {
				System.out.println("Waiting for packet on port: "+sendReceiveSocket.getLocalPort());
				
				int count = 0;
				for(;;) {
					sendReceiveSocket.setSoTimeout(TIMEOUT);
					try {
						System.out.println("Waiting on port "+this.sendReceiveSocket.getLocalPort());
						sendReceiveSocket.receive(temp);
						System.out.println("Recieved block #"+counter);
						break;
					} catch (SocketTimeoutException e) {
						count ++;
						System.out.println("Timeout #"+count);
						if (count >= MAX_TIMEOUTS) {
							System.out.println("Connection lost.  Closing transfer.  Server may no longer be working.");
							return null;
						}
						if(counter == 1){
							sendReceiveSocket.send(new DatagramPacket(msg,msg.length,this.serverIP,this.wellKnownPort));
						}
					}
					
				}
				counter++;
				System.out.println("Block recieved");
				if(this.sendPort==0) 
					sendPort = temp.getPort();
				
				if (sendPort != temp.getPort() || !serverIP.equals(temp.getAddress())){  // checking for error 5
					byte[] error5 = new byte[BUFFER_SIZE];
					error5[0] = 0;
					error5[1] = ERROR;
					error5[2] = 0;
					error5[3] = 5;
					String errorMessage;
					
					if(sendPort != temp.getPort()){
						System.out.println("Error 5: unknown port");
						errorMessage = "Unknow Port";
					}else{
						System.out.println("Error 5: unknown IP Address");
						errorMessage = "Unknow IP Address";
					}
					
					System.arraycopy(errorMessage.getBytes(),0,error5,4,errorMessage.getBytes().length);
					error5[4 + errorMessage.getBytes().length] = 0;
					DatagramPacket errorPacket = new DatagramPacket(error5, error5.length,temp.getAddress(), temp.getPort());
					sendReceiveSocket.send(errorPacket);
				}
				
				
				//checking error packets
				if (temp.getData()[1] == ERROR){
					if(temp.getData()[3] == 4){
						System.out.println("Error 4: packet has formatting errors.");
					} else if(temp.getData()[3] == 1){
						System.out.println("Error 1: " + file + " cannot be found on the Server.");
					} else if(temp.getData()[3] == 2){
						System.out.println("Error 2: Server does not have permission to read " + file);
					} else if(temp.getData()[3] == 3){
						System.out.println("Error 3: Server has ran out of space. Transfer failed.");
					} else if(temp.getData()[3] == 6){
						System.out.println("Error 6: Cannot write " + file + " to Server. Server already has file with same name. Transfer failed.");
					} else if(temp.getData()[3] == 5){
						System.out.println("Error 5: TIP error Message Recieved");
					} else {
						System.out.println("Unknown error received.");
					}
					
					return null;
				}
				
				
				
				byte blockNumCheck[] = new byte[2];
				System.arraycopy(temp.getData(), 2, blockNumCheck, 0, 2);
				if (temp.getData()[0] == 0 && temp.getData()[1] == DATA && bnum.lessThanOrEqualTo(blockNumCheck)) {
					System.out.println("Data is good");
					return temp.getData();
				}
				else{
					byte[] error4 = new byte[BUFFER_SIZE];
					error4[0] = 0;
					error4[1] = ERROR;
					error4[2] = 0;
					error4[3] = 4;
					String errorMessage;
					
					if(temp.getData()[0] != 0){
						errorMessage = "first byte of DATA is not 0";
						System.out.println(errorMessage);
					}else if(temp.getData()[1] != DATA){
						errorMessage = "second byte of DATA is not ACK code '3'";
						System.out.println(errorMessage);
					}else{
						errorMessage = "Block number is not matched";
						System.out.println(errorMessage);
					}
					
					System.arraycopy(errorMessage.getBytes(),0,error4,4,errorMessage.getBytes().length);
					error4[4 + errorMessage.getBytes().length] = 0;
					DatagramPacket errorPacket = new DatagramPacket(error4, error4.length,temp.getAddress(), temp.getPort());
					sendReceiveSocket.send(errorPacket);
				
					System.out.println("the error 4 has been sent");			
					return null;
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Error in getBlock");
				System.exit(1);
			}
		}
	}
	
	    /**
     * This method terminates the socket, disregards all errors and terminates.
     * @param socket 
     */
    public void terminate() {
        try {
                sendReceiveSocket.close();
        }catch(Exception e) {} 														//Lets ignore any error in socket closure.
        System.out.println("System terminated succesfully");
        System.exit(0);
    }
	
	
	
}