
import java.io.*;
import java.net.*;
import java.util.*;

//define a connection-manager class;
public class TFTPSimManager  implements Runnable
{
	
	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;
	private int length;

	

	  byte[] data, sending;
	   
	   int clientPort, j=0;
	   InetAddress clientaddress;

  public TFTPSimManager( DatagramPacket DP ) throws UnknownHostException {
  	// Get a reference to the data inside the received datagram.
  	data=DP.getData();
  	length=DP.getLength();
  	clientaddress = DP.getAddress(); ////////////////////***************************************/////////////////////////////////
    clientPort = DP.getPort();

	 //  Construct  sendPacket to be sent to the server (to port 69)
  	sendPacket = new DatagramPacket(data, length, InetAddress.getLocalHost(), 69);
  }

  public void run() {
  	
	  try {
      // Construct a datagram socket and bind it to any available port on the local host machine. 
      // This socket will be used to send and receive UDP Datagram packets to/from the server.
      sendReceiveSocket = new DatagramSocket();
	   } catch (SocketException se) {
	      se.printStackTrace();
	      System.exit(1);
	   }
  
             System.out.println("TFTPSim*: sending packet.");
             System.out.println("To host: " + sendPacket.getAddress());
             System.out.println("Destination host port: " + sendPacket.getPort());
             System.out.println("Length: " + sendPacket.getLength());
           
// Send the datagram packet to the server via the send/receive socket.
         try {
             sendReceiveSocket.send(sendPacket);
             } catch (IOException e) {
                   e.printStackTrace();
                   System.exit(1);
               }

// Construct a DatagramPacket for receiving packets up to 100 bytes long (the length of the byte array).

           data = new byte[100];
           receivePacket = new DatagramPacket(data, data.length);

           System.out.println("TFTPSim: Waiting for packet.");
         try {
            // Block until a datagram is received via sendReceiveSocket.
            sendReceiveSocket.receive(receivePacket);
            } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
             }

// Process the received datagram.
             System.out.println("TFTPSim: Packet received:");
             System.out.println("From host: " + receivePacket.getAddress());
             System.out.println("Host port: " + receivePacket.getPort());
             System.out.println("Length: " + receivePacket.getLength());
       

         sendPacket = new DatagramPacket(data, receivePacket.getLength(), clientaddress, clientPort);

         System.out.println( "TFTPSim: Sending packet:");
         System.out.println("To host: " + sendPacket.getAddress());
         System.out.println("Destination host port: " + sendPacket.getPort());
         System.out.println("Length: " + sendPacket.getLength());
    
// Send the datagram packet to the client via a new socket.

        try {
// Construct a new datagram socket and bind it to the same port that the cli used at the beginning 
// 
// send UDP Datagram packets.

            sendSocket = new DatagramSocket();

            } catch (SocketException se) {
                se.printStackTrace();
                System.exit(1);
                }

        try {
            sendSocket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
                }

        System.out.println("TFTPSim: packet sent");
        System.out.println("***************************FIN*******************************");

// We're finished with this socket, so close it.
        sendSocket.close();
  }
}