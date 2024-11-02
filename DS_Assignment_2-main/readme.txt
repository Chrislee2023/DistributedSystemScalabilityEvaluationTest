This project implements a distributed weather data system consisting of three components: the Content Server, Aggregation Server, and GET Client. The system uses HTTP-based communication, and a Lamport Clock is used for event synchronization in a distributed environment.

#################
Aggregation Server
#################

The Aggregation Server listens for HTTP requests from Content Servers and GET Clients. It processes weather data sent via PUT requests and stores it in a persistent JSON file. The server can also respond to GET requests, returning the latest weather data in JSON format.

Aggregates weather data from multiple Content Servers.
Supports both PUT and GET requests.
Automatically removes outdated weather data (30 seconds expiration).
Utilizes a Lamport Clock for consistent event ordering across distributed systems.

Example to run:
**************
java AggregationServer 4567

The server listens on a specified port for incoming connections.
Data sent via PUT requests is stored and managed in a persistent file.
The server periodically cleans up expired data to ensure up-to-date results.





##############
Content Server
##############

The Content Server reads weather data from a file, converts it to JSON format, and sends it to the Aggregation Server using HTTP PUT requests. The server also manages a Lamport Clock to ensure correct event ordering between distributed processes.


Reads weather data from a file and converts it into JSON.
Sends HTTP PUT requests to the Aggregation Server.
Uses Lamport Clock for synchronizing event timestamps.

Example to run:
**************
java ContentServer localhost 4567 weatherData.txt


Provide the server address, port, and file path as arguments.
The server sends the weather data in JSON format to the Aggregation Server.
Handles errors such as missing "id" in the weather data or invalid port numbers.




##########
GET Client
##########

The GET Client retrieves the weather data from the Aggregation Server by sending HTTP GET requests. It processes the server's JSON response and displays the data in a user-friendly format.

Sends HTTP GET requests to the Aggregation Server.
Processes and displays weather data received from the server.
Uses Lamport Clock for synchronizing requests and responses.

Example to run:
**************
java GETClient localhost 4567

Provide the server address and port (optional station ID) as arguments.
The client sends an HTTP GET request to fetch weather data.
Displays weather data in a readable format, handling missing fields gracefully.

path to java files: app\src\main\java\com\weather\app
path to test files: app\src\test\java\com\weather\app

References
**********

Refered chatgpt for solving some errors


