import java.io.*;
import java.net.*;


public class ServerThread implements Runnable{
	public static enum Request {ERROR, READ, WRITE};
	public static final String FILE_DIR = "ServerFiles/";
	public static final int MESSAGE_SIZE = 512;
	public static final int BUFFER_SIZE = MESSAGE_SIZE+4;
	public static final byte MAX_BLOCK_NUM = 127;
	public static final byte DATA = 3;
	public static final byte ACK = 4;
	private String[] PACKETTYPES = {"RRQ", "WRQ", "DATA", "ACK", "ERROR"}; // used for nice error string printing
	private DatagramPacket request;
	private DatagramSocket socket;
	private InetAddress ip;
	private int port;
	private String file;
	private String mode;
	private Request requestType;
	private int ackCount;
	
	/**
	 * Constructor for ServerThread
	 * @param request - The initial DatagramPacket request sent from the client
	 */
	public ServerThread(DatagramPacket request) {
		this.request = request;
		this.ackCount = 0;
		try {
			this.ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Method parses request to determine request type and handles request accordingly
	 */
	public void processRequest() {
		System.out.println("New client request:");
		
		parseRequest();
		
		System.out.println("With file: "+file);
		System.out.println("Encoded in: "+mode);
		System.out.print("Type: ");
		
	
		
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
		


		if (requestType==Request.READ) {
			System.out.println("read");
			//handle read request
			handleRead();
		} else if (requestType==Request.WRITE) {
			System.out.println("write");
			//submit write request
			handleWrite();
		} else {
			//submit invalid request*********************************************
					DatagramPacket err = FormError.illegalTFTP("INVALID OPCODE. expected READ or WRITE request got " + request.getData()[1]);
                    err.setAddress(request.getAddress());
                    err.setPort(request.getPort());
				try {
						socket.send(err);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			
			

          //   socket.send(err);
             System.out.println(request.getData()[1]);
             System.out.println("INVALID OPCODE");
   			 socket.close();
                                return;

		}
		
		//submit invalid request*********************************************
		
		
	}
	
	/**
	 * Parses request DatagramPacket and populates instance variables with data
	 */
	private void parseRequest() {
		int length  = this.request.getLength(); //temporarily stores length of request data
		byte data[] = this.request.getData(); //copies data from request
		this.ip = this.request.getAddress(); //stores ip address in instance variable
		this.port = this.request.getPort(); //stores port number in instance variable
		File here;
				try {
					socket=new DatagramSocket();
				} catch (SocketException se) {   // Can't create the socket.
					se.printStackTrace();
					System.exit(1);
				}


		
		if (data[0]!=0) {requestType = Request.ERROR;
								 //**************************

		


		
								DatagramPacket err = FormError.illegalTFTP("INVALID OPCODE. expected READ or WRITE request got " + request.getData()[0]);
                                err.setAddress(request.getAddress());
                                err.setPort(request.getPort());
                                		try {
   										socket.send(err);
										} catch (IOException e) {
										e.printStackTrace();
											System.exit(1);
											}
                             
                                System.out.println(request.getData()[0]);
                                System.out.println("INVALID OPCODE");
                                        
                                socket.close();
                                return;

		
		
		 //**************************
		
		}
														//Makes sure that request data starts with a 0
		else if (data[1]==1) requestType = Request.READ;//Checks if request is a read request
		else if (data[1]==2) requestType = Request.WRITE;//Checks if request is a write request
		
		
		
		
	else{ requestType = Request.ERROR;//If not a read or write, sets request type to invalid
								 //**************************
		
		
								DatagramPacket err = FormError.illegalTFTP("INVALID OPCODE. expected READ or WRITE request got " + request.getData()[1]);
                                err.setAddress(request.getAddress());
                                err.setPort(request.getPort());
                                
                               		try {
   										socket.send(err);
										} catch (IOException e) {
										e.printStackTrace();
											System.exit(1);
											}
                                System.out.println(request.getData()[1]);
                                System.out.println("INVALID OPCODE");
                                        
                                socket.close();
                                return;

		
		
		 //**************************




	}
	
	
	
	
	
		if(data[2]<=0) 					  //************************** Missing filename
										 //**************************
		{
			
								DatagramPacket err = FormError.illegalTFTP("Missing File name." + request.getData()[2]);
                                err.setAddress(request.getAddress());
                                err.setPort(request.getPort());
                                                               		try {
   										socket.send(err);
										} catch (IOException e) {
										e.printStackTrace();
											System.exit(1);
											}
                                System.out.println(request.getData()[2]);
                                System.out.println("Missing File name.");
                                        
                                socket.close();
                                return;

		}
		
		 //**************************
		
		if (requestType!=Request.ERROR) {
			//find filename
			int fileCount;//keeps track of position in data array while getting file name
			//finds length of file name (number of bytes between request type and next 0 or end of array)
			for(fileCount = 2; fileCount < length; fileCount++) {
				if (data[fileCount] == 0) break;
			}
			if (fileCount==length) {requestType=Request.ERROR;
								DatagramPacket err = FormError.illegalTFTP("No zero after the file name." + request.getData()[length]);
                                err.setAddress(request.getAddress());
                                err.setPort(request.getPort());
                                                                		try {
   										socket.send(err);
										} catch (IOException e) {
										e.printStackTrace();
											System.exit(1);
											}
                                System.out.println(request.getData()[length]);
                                System.out.println("No zero after the file name.");
                                        
                                socket.close();
                                return;				//**************************
			
			}//if there is no zero before the end of the array request is set to Invalid
			else {
				here = new File(FILE_DIR + new String(data,2,fileCount-2));//Otherwise, filename is converted into a string and stored in instance variable
				file = FILE_DIR + new String(data,2,fileCount-2);
				System.out.println("File is : file");
			}
			
			//find mode
			int modeCount;//keeps track of position in data array while getting encoding mode
			//finds length of encoding mode (number of bytes between request type and next 0 or end of array)
			for(modeCount = fileCount+1; modeCount < length; modeCount++) {
				if (data[modeCount] == 0) break;
			}
			

			mode = new String(data,fileCount+1,modeCount-fileCount-1);//Otherwise, filename is converted into a string and stored in instance variable
			
			if(!(mode.equalsIgnoreCase("octet")||mode.equalsIgnoreCase("netascii"))){
				 				System.out.println("INVALID MODE");

                                DatagramPacket err = FormError.illegalTFTP("INVALID MODE");
                                err.setAddress(request.getAddress());
                                err.setPort(request.getPort());
                                                                		try {
   										socket.send(err);
										} catch (IOException e) {
										e.printStackTrace();
											System.exit(1);
											}
                                socket.close();
                                return;
	
			}
			if(modeCount!=length-1) {requestType=Request.ERROR;
								DatagramPacket err = FormError.illegalTFTP("there is  data after final zero." + request.getData()[length]);
                                err.setAddress(request.getAddress());
                                err.setPort(request.getPort());
                                                                		try {
   										socket.send(err);
										} catch (IOException e) {
										e.printStackTrace();
											System.exit(1);
											}
                                System.out.println(request.getData()[length]);
                                System.out.println("there is  data after final zero");
                                        
                                socket.close();
                                return;				//**************************				//**************************
			
			}//Checks that there is no data after final zero
		}
	}
		
/********************************************************************************************************/

        
	/**
	 * handles a read request.  Continually loops, reading in data from selected file,
	 * packing this data into a TFTP Packet,
	 * sending the TFTP Packet to the client,
	 * waiting for a corresponding acknowledgement from client,
	 * and repeating until the entire file is sent
	 */
	private void handleRead() {
		try {
			//Opens an input stream
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			
			BlockNumber bn = new BlockNumber();
			bn.increment();
			
			byte[] msg;//buffer used to send data to client
			byte[] data = new byte[MESSAGE_SIZE];//buffer used to hold data read from file
			int n;
			
			//Reads data from file and makes sure data is still read
			do {
				n = in.read(data);
				msg = new byte[BUFFER_SIZE];//new empty buffer created
				//first four bits are set to TFTP Requirements
				msg[0] = 0;
				msg[1] = DATA;
				System.arraycopy(bn.getCurrent(),0,msg,2,2);
				//Data read from file
				System.arraycopy(data,0,msg,4,n);
				DatagramPacket send = new DatagramPacket(msg,msg.length,ip,port);
				try {
					System.out.println("Sending to ip: " + ip);
					System.out.println("Sending to port: " + port);
					//sends packet via default port
					socket.send(send);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				System.out.println("Sent chunk");
				
				
				boolean correctBlock;
				for(;;) {
					correctBlock = true;
					byte ack[] = new byte[BUFFER_SIZE];//Ack data buffer
					DatagramPacket temp = new DatagramPacket (ack, ack.length);//makes new packet to receive ack from client
					
					
					try {
						//****************************************************************************
						socket.receive(temp);//Receives ack from client on designated socket
						if (temp.getLength()!=4) {correctBlock = false;
					//handleError("acks");
						} //Checks for proper Ack size				//**************************

						
						else if(temp.getPort() != request.getPort()){
                                                        DatagramPacket err = FormError.unknownTransferID("Unkown client.");
                                                        err.setPort(temp.getPort());
                                                        err.setAddress(temp.getAddress());
                                                        socket.send(err);
                                                        System.out.println("Received packet from unkown client at address: " +temp.getAddress() +":"+temp.getPort());
                                                        
                                                        continue;
                                                }
                                                if(!checkForErrors(temp, 4, socket)){
                                                        System.out.println("");
                                                        return;
                                                }

						//****************************************************************************	
						byte block[] = new byte[2];
						System.arraycopy(temp.getData(), 2, block, 0, 2);
						
						if(temp.getData()[0]!=0 || temp.getData()[1]!=ACK || !bn.compare(block)) correctBlock = false;
						
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					if (correctBlock==true) break;//if ack has been accepted then loop is exited
				}
				
				bn.increment();
			} while (n >= MESSAGE_SIZE);
			
			//closes input stream
			in.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			handleError();
			return;
		} catch (IOException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			handleError();
			return;
		}
	}
	
	/**
	 * sends an ack to the client, confirming having received the latest block
	 * @param blockNumber - current block number
	 */
	private void sendAck(byte blockNumber[]) {
		byte msg[] = {0,ACK,0,0};
		System.arraycopy(blockNumber,0,msg,2,2);
		DatagramPacket temp = new DatagramPacket (msg, msg.length,ip,port);
		try {
			System.out.println("Sending ack to port "+port);
			socket.send(temp);
			System.out.println("Ack "+this.ackCount+" sent");
		} catch (IOException e) {
			System.out.println("Send Packet Error");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * waits for TFTP Packet from client until appropriate data block is recieved
	 * @param blockNumber - expected block number
	 * @return returns byte array of data to be written in write request
	 */
	private byte[] getBlock(BlockNumber blockNumber, BufferedOutputStream out) {
		byte incomingMsg[];// = new byte[BUFFER_SIZE];
		byte data[] = new byte[BUFFER_SIZE];
		for(;;) {
			incomingMsg = new byte[BUFFER_SIZE];
			DatagramPacket temp = new DatagramPacket (incomingMsg, incomingMsg.length);
			
			try {
				System.out.println("Waiting for data");
				socket.receive(temp);
			
				
				/*********************************************************/
			 if(port!=temp.getPort()){
			DatagramPacket err = FormError.unknownTransferID("Unkown client.");
             err.setPort(temp.getPort());
             err.setAddress(temp.getAddress());
              System.out.println("Received packet from unkown client at address: " +temp.getAddress() +":"+temp.getPort());
              socket.send(err);
              continue;}
				 	
				 if(!checkForErrors(temp, 3, socket)){out.close(); break; 	};
                                                	
                                                	
           	//****************************************************************************                                     	
				 	
			    System.out.println("Data received");
				byte bn[] = new byte[2];
				System.arraycopy(temp.getData(), 2, bn, 0, 2);
				if (temp.getData()[0] == 0 && temp.getData()[1] == DATA && blockNumber.compare(bn)) {
					System.out.println("Data good");
					System.arraycopy(temp.getData(), 4,data, 0, temp.getLength()-4);
					return data;
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	return data;	
	}
	
	/**
	 * Uses getBlock() and sendAck() methods to get data and send the appropriate ack
	 * Writes data blocks to designated file
	 */
	private void handleWrite() {
		BlockNumber bn = new BlockNumber();
		
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			for (;;) {
				sendAck(bn.getCurrent());
				bn.increment();
				ackCount++;
				byte[] temp = getBlock(bn,out);
				
				int length;
				
				for(length = 4; length < temp.length; length++) {
					if (temp[length] == 0) break;
				}
				out.write(temp, 0, length);
				System.out.println("Length is "+length);
				System.out.println("buffer is "+temp.length);
				if(length+1<MESSAGE_SIZE) {
					System.out.println("Closing file");
					out.close();
					sendAck(bn.getCurrent());
					break;
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			handleError();
			return;
		} catch (IOException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			handleError();
			return;
		}
	}
        /**
         * Extract the error message
         * @param packet
         * @return
         */
        private String ExtractErrorMsg(DatagramPacket packet){
                byte[] msg = packet.getData();
                byte[] data = new byte[packet.getLength() - 5];
                for(int i = 0; i < packet.getLength() - 5  ; i++){
                        data[i] = msg[i+4];
                }
                return new String(data);
        }	
	/**
	 * Will handle any errors that occur during a client request
	 * Not implemented properly for this increment
	 */
	private void handleError() {
		//TODO: Implement real error method
		/*NOTE: This is a test method filler simply
		 * replying with the appropriate request
		 * as per SYSC 3303 assignment 1
		 */
		 

		
		
		
	}

    private boolean checkForErrors(DatagramPacket packet, int expectedtype, DatagramSocket socket){
                DatagramPacket err = null;
                boolean goodPacket = true;
                        
                if(packet.getData()[1] == 5){
                        System.out.println(ExtractErrorMsg(packet));
                        return false;
                }
                if(packet.getData()[0] != 0){
                        err = FormError.illegalTFTP("First Opcode digit must be 0");
                        System.out.println("INVALID OP CODE FROM CLIENT");
                        goodPacket= false;
                }
                else if(expectedtype != packet.getData()[1] ){             
                        FormError.illegalTFTP("Wrong opcode got " + PACKETTYPES[(packet.getData()[1]) -1] + " expected " + PACKETTYPES[expectedtype -1]);
                        System.out.println("EXPECTED " + PACKETTYPES[expectedtype -1] + " GOT " +PACKETTYPES[(packet.getData()[1]) -1]);
                        goodPacket= false;
                }
                else if((packet.getData()[1]) < 1 || (packet.getData()[1])> 5){
                        FormError.illegalTFTP((packet.getData()[1]) + " is an invalid Opcode");
                        System.out.println("INVALID OP CODE FROM CLIENT");
                        goodPacket= false;
                }
                if((packet.getData()[1]) == 5){
                        goodPacket= false;
                        return goodPacket; //dont repond to error packets
                }
                if(err!= null){
                        err.setAddress(packet.getAddress());
                        err.setPort(packet.getPort());
                        try{
                                socket.send(err);
                        } catch(java.net.SocketException se) {
                                se.printStackTrace();
                                System.exit(1);
                        }catch(java.io.IOException io) {
                                io.printStackTrace();
                                System.exit(1);
                        }
                }
                return goodPacket;
                        
        }
            
      

	@Override
	public void run() {
		processRequest();
	}
	

}

class FormError {

   
        
        /**
         * Set the packets headers. Contains the generic code used by the other methods
         * @param data
         * @param msg
         * @param errorCode
         * @return
         */
        private static byte[] FormStart(byte[] data, byte[] msg, byte errorCode){
                
//              opcode
                data[0] = 0;
                data[1] = 5;
                
//              error code 
                data[2] = 0;
                data[3] = errorCode;            
                
                for(int i = 0; i < msg.length; i++)
                        data[i+4] = msg[i];
                data[data.length-1] = 0;
                return data;
        }
 
        /**
         * Generate an Unknown Transfer ID packet
         * @param errorMsg
         * @return
         */
        public static DatagramPacket unknownTransferID(String errorMsg){
                byte[] msg = errorMsg.getBytes();
                byte[] data = new byte[msg.length + 5];
                
                byte five =5;
                data = FormStart(data, msg, five);

                DatagramPacket packet = new DatagramPacket(data, data.length);
                
                return packet;
        }
        /**
         * Generate an Illegal TFTP operation packet
         * @param errorMsg
         * @return
         */
        public static DatagramPacket illegalTFTP(String errorMsg){
                byte[] msg = errorMsg.getBytes();
                byte[] data = new byte[msg.length + 5];
                
                byte four =4;
                data = FormStart(data, msg, four);

                DatagramPacket packet = new DatagramPacket(data, data.length);
                
                return packet;
        }
        /**
         * Extract the error code from a packet
         * @param packet
         * @return
         */
        public static byte getError(DatagramPacket packet){
                return packet.getData()[3];
        }
}
