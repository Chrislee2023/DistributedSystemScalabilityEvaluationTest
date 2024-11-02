# Distributed Systems Assignment 2
**Student:** Chrislyne Pathrose (a1938588)

## Content Server
The Content Server is responsible for transmitting weather data to another server through a socket connection. It reads weather data from a specified feed file, converts it to JSON format, and sends it to the target server using HTTP PUT requests. The class includes retry logic for connection failures and tracks the last active time for each socket.

### Key Features
-Converts weather data from a feed file into JSON format. 
-Retries sending data in case of transmission failures. 
-Responds with appropriate HTTP status codes based on the success or failure of the data transfer.

## Aggregation Server
The Aggregation Server serves as a receiver for data from multiple content servers, managing client connections and processing requests. It is designed to aggregate and manage the weather data provided by these content servers.

### Key Features 
-Centralizes the collection of weather data from various sources. -Concurrently manages incoming client connections to ensure data integrity. -Organizes and stores weather data in the data/ directory. Automatically purges outdated data (data from old sockets or data not received in the last 20 messages) to maintain accuracy.
-Processes GET and PUT requests to facilitate data retrieval and submission. -Provides detailed error messages and status codes.

## GET Client
The GETClient is an application that retrieves weather data from the aggregation server. It sends HTTP GET requests and processes the server's responses. The GET Client will return only the weather data received in the last 30 seconds.

### Key Features 
-Sends HTTP GET requests to a content server for retrieving weather data. -Implements a heartbeat mechanism to maintain an active connection with the server. 
-Uses a Lamport clock to timestamp requests, ensuring correct ordering. -Processes and formats server responses for improved readability.



## Running in Eclipse To run the project in Eclipse, follow these steps:

Prerequisites
- Ensure you have the Java Development Kit (JDK) installed. 
- Place the JSON library in a lib folder within your project directory.
- Importing the Project Open Eclipse and select File > Import. 
- Choose Existing Projects into Workspace and click Next. 
- Browse to the location of your project and select it, then click Finish.
- Compile and Run the Programs

## Aggregation Server

- Open the AggregationServer.java file. 
- Right-click on the file in the Package Explorer and select Run As > Java Application. 
- Provide the port number as an argument in the run configuration: Click on Run > Run Configurations...
-  Select Java Application for your AggregationServer. 
- In the Arguments tab, add: http://localhost:8080 
- Click Apply and then Run.

## Content Server

- Open the ContentServer.java file.
-  Right-click on the file in the Package Explorer and select Run As > Java Application. 
- Provide the necessary arguments in the run configuration: Click on Run > Run Configurations... 
- Select Java Application on the left, then select your ContentServer.
- In the Arguments tab, add: http://localhost:8080 
- Click Apply and then Run.

## GET Client 

- Open the GETClient.java file. 
- Right-click on the file in the Package Explorer and select Run As > Java Application. 
- In the run configuration, provide the server URL as an argument: Click on Run > Run Configurations... 
- Select Java Application for your GETClient. 
- In the Arguments tab, add: http://localhost:8080 .
- Click Apply and then Run.
