SYSC 3303 Project Iteration 1
Group 4

This was written using eclipse


1) Start the Server.java first
2) Start the TFTPSim.java
3) If you wish to test the server by activating the simulator type 'y'.  Otherwise (for normal operation) press 'n'.
4) Start the Client.java
5) The client will prompt you to choose either a read or a write.
	press 1 and enter to perform a read operation
	press 2 and enter to perform a write operation
6) type in the name of the file you wish to use
7) type in the mode of encoding you wish to use (netascii or octet).
NOTE: Steps 8 and 9 are only to be followed if the simulator is turned on.
8) Navigate to the simulator view
9) Navigate through the menus to set up your error by typing the corresponding number.

To Start another transaction simply launch another Client.java and complete steps 5-9 again.

To close the server, type exit into the server terminal window at any time.

NOTE: The system can only read to or write from a file if a file of that name exists in both the ServerFiles and the ClientFiles directory.
Due to the nature of Eclipse, to run this project in another environment, the following constanst must be changed and the project must be rebuilt:

In the Client.java, the constant FILE_DIR needs to be changed from "ClientFiles/" to "./ClientFiles"
In the ServerThread.java the constant FILE_DIR needs to be changed from "ServerFiles/" to "./ServerFiles"
