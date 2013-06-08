
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
	private String dir;
	private String[] PACKETTYPES = {"RRQ", "WRQ", "DATA", "ACK", "ERROR"}; // used for nice error string printing
	private DatagramPacket request;
	private DatagramSocket socket;
	private InetAddress ip;
	private int port;
	private String file;
	private String mode;
	private Request requestType;
	private int ackCount;
	private int TIMEOUT = 500;	
	int MAX_TIMEOUTS = 3;

	/**
	 * Constructor for ServerThread
	 * @param request - The initial DatagramPacket request sent from the client
	 */
	public ServerThread(DatagramPacket request, String directory) {
		dir = new String();
		if(directory.equals("")) {
			this.dir = FILE_DIR;
		} else {
			this.dir = directory + "/";
		}
		System.out.println("Directory: "+dir);
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

	        System.out.println(request.getData()[1]);
	        System.out.println("INVALID OPCODE expected READ or WRITE request got");
	        socket.close();
	        return;
		}
	}

	/**
	 * Parses request DatagramPacket and populates instance variables with data
	 */
	private void parseRequest() {
		int length  = this.request.getLength(); //temporarily stores length of request data
		byte data[] = this.request.getData(); //copies data from request
		this.ip = this.request.getAddress(); //stores ip address in instance variable
		this.port = this.request.getPort(); //stores port number in instance variable
		//File here;
		try {
			socket=new DatagramSocket();
		} catch (SocketException se) {   // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}

		//Makes sure that request data starts with a 0
		if (data[0]!=0) {
			requestType = Request.ERROR;
			DatagramPacket err = FormError.illegalTFTP("No starting Zero Data[0]= " + request.getData()[0]);
			err.setAddress(request.getAddress());
			err.setPort(request.getPort());
			try {
				socket.send(err);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
	
			System.out.println(request.getData()[0]);
			System.out.println("No starting Zero Data[0]");
			socket.close();
			return;
		} else if (data[1]==1) {
			requestType = Request.READ;//Checks if request is a read request
		} else if (data[1]==2) {
			requestType = Request.WRITE;//Checks if request is a write request
		} else if(!(data[1]==1 || data[2]==2)) {
			requestType = Request.ERROR;//If not a read or write, sets request type to invalid
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
	        System.out.println("INVALID OPCODE expected READ or WRITE request got " + request.getData()[1]);
	        socket.close();
	        return;
		} else if(data[2]<=0) {
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

		//find filename
		int fileCount;//keeps track of position in data array while getting file name
		//finds length of file name (number of bytes between request type and next 0 or end of array)
		for(fileCount = 2; fileCount < length; fileCount++) {
			if (data[fileCount] == 0) break;
		}
		//if there is no zero before the end of the array request is set to Invalid
		if (fileCount==length) {
			requestType=Request.ERROR;
			DatagramPacket err = FormError.illegalTFTP("No zero after the file name." + request.getData());
	        err.setAddress(request.getAddress());
	        err.setPort(request.getPort());
	        try {
	        	socket.send(err);
	        } catch (IOException e) {
	        	e.printStackTrace();
	        	System.exit(1);
	        }
	        System.out.println(request.getData());
	        System.out.println("No zero after the file name.");

		    socket.close();
		    return;
		} else {
			//here = new File(this.dir + new String(data,2,fileCount-2));//Otherwise, filename is converted into a string and stored in instance variable
			file = this.dir + new String(data,2,fileCount-2);
			System.out.println("File is : " + file);
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
		//Checks that there is no data after final zero
		if(!(modeCount==length || modeCount==length-1 || modeCount==length+1) && !(request.getData()[length-1]==0) ) {requestType=Request.ERROR;
			DatagramPacket err = FormError.illegalTFTP("there is  data after final zero." + request.getData());
	        err.setAddress(request.getAddress());
	        err.setPort(request.getPort());
	    	try {
	    		socket.send(err);
			} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
			}
	        System.out.println(request.getData());
	        System.out.println("there is  data after final zero"+fileCount+" "+modeCount);

	        socket.close();
	        return;
		}
	}


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
			DatagramPacket sendPacket;

			//Reads data from file and makes sure data is still read
			do {
				n = in.read(data);
				msg = new byte[BUFFER_SIZE];//new empty buffer created
				//first four bits are set to TFTP Requirements
				msg[0] = 0;
				msg[1] = DATA;
				System.arraycopy(bn.getCurrent(),0,msg,2,2);
				if(n>=0) System.arraycopy(data,0,msg,4,n);

				//Data read from file
				byte ack[] = new byte[BUFFER_SIZE];//Ack data buffer
				DatagramPacket temp = new DatagramPacket (ack, ack.length);//makes new packet to receive ack from client
				boolean received = false;
				System.out.println("Sending to ip: " + ip);
				System.out.println("Sending to port: " + port);
				int count = 0;
				while(!received) {
					try{
						socket.setSoTimeout(TIMEOUT);
						sendPacket = new DatagramPacket(msg,msg.length,ip,port);	
						socket.send(sendPacket);
						socket.receive(temp);//Receives ack from client on designated socket

						//Checks for proper Ack size
						received = true;
					}catch(SocketTimeoutException ste) {
						count++;
	                    System.out.println("'SERVERTHREAD.JAVA' : Sending again to ip(TIMEOUT): " + ip);
						System.out.println("'SERVERTHREAD.JAVA' : Sending again to port(TIMEOUT): " + port);
						if(count >=MAX_TIMEOUTS) {
								//We don't throw errors here
							System.out.println("Server Timed out time #"+ MAX_TIMEOUTS);
							in.close();
							return;
						}				
					}catch (IOException e){
						System.exit(1);
					}
				}

				try {
					if(temp.getPort() != request.getPort()){
						DatagramPacket err = FormError.unknownTransferID("Unkown client.");
						err.setPort(temp.getPort());
						err.setAddress(temp.getAddress());
						socket.send(err);
						System.out.println("Received packet from unkown client at address: " +temp.getAddress() +":"+temp.getPort());
						continue;
					}
					if(!checkForErrors(temp, 4, socket)){
						System.out.println("");
						in.close();
						return;
					}
					byte block[] = new byte[2];
					System.arraycopy(temp.getData(), 2, block, 0, 2);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				bn.increment();
			} while (n >= MESSAGE_SIZE);

			//closes input stream
			in.close();
		} catch (FileNotFoundException e) {			
			byte errornum = 1;
			DatagramPacket err = FormError.IOerror("FILE NOT FOUND",errornum);
			err.setAddress(request.getAddress());
			err.setPort(port);			   
			try {			   
				socket.send(err);			   
			} catch (IOException ex) {			   
				ex.printStackTrace();			   
				System.exit(1);			   
			}			   
			socket.close();			   
			//System.out.println("File not found to read");			   
			return;
		} catch(SecurityException e){			
			byte errornum = 2;
			DatagramPacket err = FormError.IOerror("NO ACCESS TO READ",errornum);
			err.setAddress(request.getAddress());
			err.setPort(port);
			try {			   
				socket.send(err);			   
			} catch (IOException ex) {			   
				ex.printStackTrace();			   
				System.exit(1);			   
			}		   
			socket.close();			   
			System.out.println("File has no access to read");			   
			return;
		} catch (IOException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
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
			boolean received = false;
			System.out.println("Waiting for data");
			int count = 0;
			while(!received) {
				try{
					socket.setSoTimeout(TIMEOUT);
					socket.receive(temp);					
					received = true;
				}catch(SocketTimeoutException e){
					count++;
					System.out.println("'SERVERTHREAD.JAVA':Waiting for data again(TIMEOUT)");
                    if(count >=MAX_TIMEOUTS) {
                    	//We don't throw errors here
                        System.out.println("Server Timed out time #"+ MAX_TIMEOUTS);
                        return null;
                    }
				}catch(IOException e){
					System.exit(1);
				}
			}
			
			try {

				if(port!=temp.getPort()){
					DatagramPacket err = FormError.unknownTransferID("Unkown client.");
					err.setPort(temp.getPort());
					err.setAddress(temp.getAddress());
					System.out.println("Received packet from unkown client at address: " +temp.getAddress() +":"+temp.getPort());
					socket.send(err);
					continue;
				} else if(!checkForErrors(temp, 3, socket)) {
					out.close();
					break;
				}
                                                	
				System.out.println("Data received");
				byte bn[] = new byte[2];
				System.arraycopy(temp.getData(), 2, bn, 0, 2);
				if (temp.getData()[0] == 0 && temp.getData()[1] == DATA && blockNumber.lessThanOrEqualTo(bn)) {
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
			
			if((new File(file)).exists()){
				byte errornum = 6;
				DatagramPacket err = FormError.IOerror("FILE ALREADY EXIST",errornum);
				err.setAddress(request.getAddress());
				err.setPort(port);				   
				try {				   
					socket.send(err);				   
				} catch (IOException ex) {				   
					ex.printStackTrace();				   
					System.exit(1);				   
				}				   
				System.out.println("can't overwrite the file. file already exist");				   
				socket.close();		
				return;
			}
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			
			for (;;) {
				sendAck(bn.getCurrent());
				bn.increment();
				ackCount++;
				byte[] temp = getBlock(bn,out);
				if(temp == null) return;

				if((int)(new File(file).getFreeSpace()) < temp.length){
					byte errornum = 3;
					DatagramPacket err = FormError.IOerror("No SPACE on the disk",errornum);
					err.setAddress(request.getAddress());
					err.setPort(port);					   
					try {					   
						socket.send(err);					   
					} catch (IOException ex) {					   
						ex.printStackTrace();					   
						System.exit(1);					   
					}					   
					System.out.println("disk is full");					   
					socket.close();					   
					return;
				}
				
				int length;

				for(length = 4; length < temp.length; length++) {
					if (temp[length] == 0) break;
				}
				try {
					out.write(temp, 0, length);
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
					this.socket.send(new DatagramPacket(data,data.length,InetAddress.getLocalHost(),this.port));
					out.close();
					return;
				}
		
				if(length+1<MESSAGE_SIZE) {
					System.out.println("Closing file");
					out.close();
					sendAck(bn.getCurrent());
					break;
				}
			}
		} catch (FileNotFoundException e) {
			byte errornum = 1;
			DatagramPacket err = FormError.IOerror("FILE NOT FOUND",errornum);
			err.setAddress(request.getAddress());
			err.setPort(port);			   
			try {			   
				socket.send(err);			   
			} catch (IOException ex) {			   
				ex.printStackTrace();			   
				System.exit(1);			   
			}			   
			socket.close();			   
			System.out.println("File not found to read");			   
			return;
			
		} catch(SecurityException e){			
			byte errornum = 2;
			DatagramPacket err = FormError.IOerror("NO ACCESS TO READ",errornum);
			err.setAddress(request.getAddress());
			err.setPort(port);
			try {			   
				socket.send(err);			   
			} catch (IOException ex) {			   
				ex.printStackTrace();			   
				System.exit(1);			   
			}		
			socket.close();			   
			System.out.println("File has no access to read");			   
			return;
			
		} catch (IOException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();

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
    	} else if(expectedtype != packet.getData()[1] ) {             
    		FormError.illegalTFTP("Wrong opcode got " + PACKETTYPES[(packet.getData()[1]) -1] + " expected " + PACKETTYPES[expectedtype -1]);
    		System.out.println("EXPECTED " + PACKETTYPES[expectedtype -1] + " GOT " +PACKETTYPES[(packet.getData()[1]) -1]);
    		goodPacket= false;
    	} else if((packet.getData()[1]) < 1 || (packet.getData()[1])> 5) {
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
		// opcode
		data[0] = 0;
		data[1] = 5;
		// error code 
		data[2] = 0;
		data[3] = errorCode;            
		for(int i = 0; i < msg.length; i++) {
			data[i+4] = msg[i];
		}
		
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
		data = FormStart(data, msg, (byte)5);
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
		
		data = FormStart(data, msg, (byte)4);
		
		DatagramPacket packet = new DatagramPacket(data, data.length);
		String m=new String(packet.getData());
		System.out.println(m);
		return packet;
	}
	
	
	/**Generate an IOerror packets
	 * 
	 * @param errorMsg
	 * @param num
	 * @return
	 */
	public static DatagramPacket IOerror(String errorMsg,byte num){
		byte[] msg = errorMsg.getBytes();
		byte[] data = new byte[msg.length + 5];            
		data = FormStart(data, msg, num);
		DatagramPacket packet = new DatagramPacket(data, data.length);
		String m=new String(packet.getData());
		System.out.println(m);
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