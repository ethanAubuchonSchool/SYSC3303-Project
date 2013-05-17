// TFTPSim.java
// This class is the beginnings of an error simulator for a simple TFTP server 
// based on UDP/IP. The simulator receives a read or write packet from a client and
// passes it on to the server.  Upon receiving a response, it passes it on to the 
// client.
// One socket (68) is used to receive from the client, and another to send/receive
// from the server.  A new socket is used for each communication back to the client.   

import java.io.*;
import java.net.*;
import java.util.*;

public class TFTPSim {

// UDP datagram packets and sockets used to send / receive
private DatagramPacket sendPacket, receivePacket;
private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;

public TFTPSim()
{
   try {
      // Construct a datagram socket and bind it to port 68 on the local host machine.
      // This socket will be used to receive UDP Datagram packets from clients.
      receiveSocket = new DatagramSocket(68);

   } catch (SocketException se) {
      se.printStackTrace();
      System.exit(1);
   }
}

public DatagramPacket FormaPacket() throws UnknownHostException
{
   byte[] data;
   
   int clientPort, j=0;
   InetAddress clientaddress;

  
      // Construct a DatagramPacket for receiving packets up
      // to 100 bytes long (the length of the byte array).
      data = new byte[100];
      receivePacket = new DatagramPacket(data, data.length);

      System.out.println("TFTPSim: Waiting for packet");
      // Block until a datagram packet is received from receiveSocket.
      try {
         receiveSocket.receive(receivePacket);
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }

      // Process the received datagram.
      System.out.println("TFTPSim: Packet received:");
      System.out.println("From host: " + receivePacket.getAddress());
      clientaddress = receivePacket.getAddress(); ////////////////////***************************************/////////////////////////////////
      clientPort = receivePacket.getPort();
      System.out.println("Host port: " + clientPort);
      System.out.println("Length: " + receivePacket.getLength());

      
     // data = receivePacket.getData();
      
      // Now pass it on to the server (to port 69)
      // Construct a datagram packet that is to be sent to a specified port on a specified host.
      // The arguments are:
      //  msg - the message contained in the packet (the byte array)
      //  the length we care about - k+1
      //  InetAddress.getLocalHost() - the Internet address of the destination host.
     //  69 - the destination port number on the destination host.
     // int length = receivePacket.getLength();
       
     // end of loop
    return receivePacket;

}

public static void main( String args[] ) throws UnknownHostException
{
	for(;;){
	
   TFTPSim s = new TFTPSim();
    Thread connect = new Thread ( new TFTPSimManager(s.FormaPacket()));
        connect.start();
	}
}
}




