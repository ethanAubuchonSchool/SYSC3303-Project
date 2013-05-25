

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
	public static final String FILE_DIR = "ClientFiles/";

	private String file;
	private String mode;
	private DatagramPacket sendPacket; //
	private DatagramSocket sendReceiveSocket;
	private BlockNumber bnum;
	private int sendPort;
	private int wellKnownPort;
	private byte msg[];
	private int counter;
	private DatagramPacket block;
	private DatagramPacket readpack;   
	   
	public Client(){
		counter = 0;
		
		wellKnownPort = 68;
		
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
		msg = new byte[BUFFER_SIZE];
		System.out.println("1) Read");
		System.out.println("2) Write");
		System.out.println("Select a mode:");
		Scanner scanner = new Scanner (System.in);
		int request = scanner.nextInt();
		System.out.println("Type File name: ");
		file = scanner.next();
		System.out.println("Type mode: ");
		mode = scanner.next();
		msg[0] = 0;
		scanner.close();
		
		
			this.sendPort = 0;
			int iterator = 2;
			if (request == 2){
				msg[1] = 2;
				System.arraycopy(file.getBytes(),0,msg,iterator,file.getBytes().length);
				iterator+=file.getBytes().length;
				msg[iterator] = 0;
				iterator ++;
				System.arraycopy(mode.getBytes(),0,msg,iterator,mode.getBytes().length);
				iterator+=mode.getBytes().length;
				msg[iterator] = 0;	
				sendData(iterator+1);
				clientWrite();
				
			}else if(request == 1){
				msg[1] = 1;
				System.arraycopy(file.getBytes(),0,msg,iterator,file.getBytes().length);
				iterator+=file.getBytes().length;
				msg[iterator] = 0;
				iterator ++;
				System.arraycopy(mode.getBytes(),0,msg,iterator,mode.getBytes().length);
				iterator+=mode.getBytes().length;
				msg[iterator] = 0;	
				sendData(iterator+1);
				clientRead();		
			}else{
				System.out.println("Request error");
			}
	}
	
	public void sendData(int size){
		 try {
			 System.out.println("Sending packet to port: "+this.wellKnownPort);
			 for (int i = 0; i < size; i++) {
				 System.out.print(msg[i]);
			 }
			 System.out.println();
			 sendPacket = new DatagramPacket(this.msg, size,InetAddress.getLocalHost(), this.wellKnownPort);
			 sendReceiveSocket.send(sendPacket);
	        } catch (IOException e) {
	           e.printStackTrace();
	           System.exit(1);
	        }

	        System.out.println("Client: Packet sent.");
	}
	
	public void clientWrite(){
		try {
			//Opens an input stream
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(FILE_DIR+file));
			
			bnum = new BlockNumber();
			
			byte[] pack;//buffer used to send data to client
			byte[] data;//buffer used to hold data read from file
			int n;
			
			//Reads data from file and makes sure data is still read
			do {
				data = new byte[MESSAGE_SIZE];
				for(;;) {
					byte ack[] = new byte[BUFFER_SIZE];//Ack data buffer
					DatagramPacket temp = new DatagramPacket (ack, ack.length);//makes new packet to receive ack from client
					try {
						System.out.println("Waiting for Ack " + counter);
						counter++;
						sendReceiveSocket.receive(temp);//Receives ack from client on designated socket
						
						if (this.sendPort == 0) 
							this.sendPort = temp.getPort();
						
						
						if (sendPort != temp.getPort()){  // checking for error 5
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
							//waiting to receive the correct acknowledgment and repeats
							//the loop if it is again from unknown port or IP.
							sendReceiveSocket.receive(temp);
						}
						
						System.out.println("Recieved Ack ");
						byte bn[] = new byte[2];
						System.arraycopy(temp.getData(), 2, bn, 0, 2);
						
							while(temp.getData()[1] == ERROR){
								if(temp.getData()[3] == 4){
									System.exit(1);
								}else{
									sendReceiveSocket.send(block);
									System.out.println("sending the corrupted  block");
								}
							}
						
						
							if(temp.getData()[0] == 0 && temp.getData()[1] == ACK && bnum.compare(bn)) {
								System.out.println("Ack good");
								break;
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
								sendReceiveSocket.send(errorPacket);
								System.out.println("the error 4 has been sent and request is starting again");
								System.exit(1);
							}
						
						
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("Ack Reception Error");
						System.exit(1);
					}					
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
			    block = new DatagramPacket(pack,pack.length,InetAddress.getLocalHost(), this.sendPort);
				sendReceiveSocket.send(block);
				System.out.println("Sent data block");
				
			} while (n >= MESSAGE_SIZE);
			
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
		
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(FILE_DIR+file));
			for(;;){
				int length;
				byte[] temp = getBlock(bnum.getCurrent());
				for(length = 4; length < temp.length; length++) {
					//System.out.print(temp[length]+","); // Used to check incoming byte array for debugging
					if (temp[length] == 0) break;
				}
				out.write(temp,0,length);
				System.out.println("Sending ack");
				sendAck(bnum.getCurrent());					
				
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
			readpack = new DatagramPacket (temp, temp.length, InetAddress.getLocalHost(), this.sendPort);
			sendReceiveSocket.send(readpack);
		} catch (IOException e) {
			System.out.println("Send Packet Error");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public byte[] getBlock(byte[] blockNumber) {
		byte msg[] = new byte[516];
		byte data[] = new byte[512];
		System.out.println("getting data block");
		for(;;) {
			
			DatagramPacket temp = new DatagramPacket (msg, msg.length);

			try {
				System.out.println("Waiting for packet on port: "+sendReceiveSocket.getLocalPort());
				sendReceiveSocket.receive(temp);
				System.out.println("Block recieved");
				if(this.sendPort==0) 
					sendPort = temp.getPort();
				
				if (sendPort != temp.getPort()){  // checking for error 5
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
					//waiting to receive the correct acknowledgment and repeats
					//the loop if it is again from unknown port or IP.
					sendReceiveSocket.receive(temp);
				}
				
				while(temp.getData()[1] == ERROR){
					if(temp.getData()[3] == 4){
						System.exit(1);
					}else{
						sendReceiveSocket.send(readpack);
						System.out.println("sending the corrupted  block");
					}
				}
				byte blockNumCheck[] = new byte[2];
				System.arraycopy(temp.getData(), 2, blockNumCheck, 0, 2);
				if (temp.getData()[0] == 0 && temp.getData()[1] == DATA && bnum.compare(blockNumCheck)) {
					System.out.println("Data is good");
					System.arraycopy(temp.getData(), 4,data, 0, temp.getLength()-4);
					return data;
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
					System.out.println("the error 4 has been sent and request is starting again");
					System.exit(1);
				}
				/**for(int i = 0; i < 4; i ++) {
					System.out.println("Byte "+i+": "+temp.getData()[i]);
				}
				System.out.println("Expecting: 0"+DATA+bnum.getCurrent()[0]+bnum.getCurrent()[1]);
				System.out.println((temp.getData()[0] == 0)+" "+(temp.getData()[1] == DATA)+" "+(bnum.compare(blockNumCheck)));
		        */
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Error in getBlock");
				System.exit(1);
			}
		}
	}
	
	
	
}