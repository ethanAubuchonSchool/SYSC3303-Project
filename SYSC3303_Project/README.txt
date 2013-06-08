SYSC 3303 Project Iteration 4
Group 4

This was written using eclipse

NOTE: Due to upload errors, no test files could be included in the project.
	The default directory for the server is ServerFiles
	The default directory for the client is the project folder

1) Start the Server.java first
2) Start the TFTPSim.java
3) If you wish to test the server by activating the simulator type 'y'.  Otherwise (for normal operation) press 'n'.
4) Start the Client.java
NOTE: Steps 5 and 6 are only to be followed if the simulator is turned on.
5) Navigate to the simulator view
6) Navigate through the menus to set up your error by typing the corresponding number.

7) The client will prompt you to choose either a read or a write.
	press 1 and enter to perform a read operation
	press 2 and enter to perform a write operation
8) Client will prompt you to see if you wish to change the directory (y is yes, n is no)
NOTE: Step 9 is only to be followed to change the directory
9) Enter a directory name without the closing slash (ie if you wish to read C:/Users/user/Documents/read.txt enter C:/Users/user/Documents as the directory)
NOTE: typing "d" as your directory will reset the client directory to the project directory
10) type in the name of the file you wish to use
11) type in the mode of encoding you wish to use (netascii or octet).


IO Error Simulation:
	To simulate a file not found error:
		Either read a file that doesn't exist on the server
		Or write a file that doesn't exist on the client
		
	To simulate an access violation:
		Either read a file that is protected on the server
		Write a file that is protected on the server
		Read a file that is protected on the client
		Write a file that is protected on the client
		
	To simulate a disk full violation:
		Either change the server directory to a full directory (no extra memory space) and perform a read
		Or change the client directory to a full directory (no extra memory space) and perform a read
		
	To simulate a file already exists error:
		Try to write to a file that already exists in the server

Start new simulation:
	To Start another transaction simply launch another Client.java and complete steps 5-9 again.

Close Server:
	To close the server, type exit into the server terminal window at any time.

Change directory:
	To change the server directory at any time, type cd into the server terminal
	You will then be prompted to enter a new directory

NOTE:
	Due to the nature of Eclipse, to run this project in another environment, the following constanst must be changed and the project must be rebuilt:
		In the Client.java, the constant FILE_DIR needs to be changed from "ClientFiles/" to "./ClientFiles"
		In the ServerThread.java the constant FILE_DIR needs to be changed from "ServerFiles/" to "./ServerFiles"
