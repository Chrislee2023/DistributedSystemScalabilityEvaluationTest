import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * AggregationServer is a server that handles incoming connections from content servers,
 * processes weather data, and maintains a persistent storage of weather information.
 */
public class AggregationServer {
    static final Map<Socket, String> serverIds = new ConcurrentHashMap<>(); // Map to store server IDs
    static final Map<Socket, Long> serverLastActiveTime = new ConcurrentHashMap<>(); // Map to track last active time of servers
    private static final List<Weather> recentWeatherData = new ArrayList<>(); // List to hold recent weather data
    private static final long CONNECTION_TIMEOUT = 30000; // 30-second timeout for inactive connections
    private static final String DATA_FILE = "data/weatherData.json"; // File to store weather data
    private static ObjectOutput outputData;

    /**
     * Main method to start the Aggregation Server.
     *
     * @param args Command line arguments for port configuration
     */
    public static void main(String[] args) {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 4567; // Default port is 4567
        loadWeatherData(); // Load existing weather data at startup
        startDataCleanupThread(); // Start a thread for cleaning up stale data
        startConnectionManagerThread(); // Start a thread for managing connections
        createAndHandleClientConnections(port); // Start accepting client connections
    }

    /**
     * Manages client connections by checking for idle connections
     * and cleaning up any that exceed the defined timeout.
     */
    public static void manageConnections() {
        long currentTime = System.currentTimeMillis(); // Get the current time
        List<Socket> socketsToClose = new ArrayList<>(); // List to keep track of sockets to be closed

        // Iterate over all connected client sockets
        for (Socket clientSocket : serverLastActiveTime.keySet()) {
            long lastActiveTime = serverLastActiveTime.get(clientSocket); // Get the last active time for the socket

            // Check if the connection has been idle longer than the timeout
            if (currentTime - lastActiveTime > CONNECTION_TIMEOUT) {
                socketsToClose.add(clientSocket); // Mark this socket for closure
            }
        }

        // Close the marked sockets and perform cleanup
        for (Socket socketToClose : socketsToClose) {
            try {
                System.out.println("Closing idle connection with client: " + socketToClose.getRemoteSocketAddress()); // Log the closure
                socketToClose.close(); // Close the idle socket
                serverIds.remove(socketToClose); // Remove from server ID map
                serverLastActiveTime.remove(socketToClose); // Remove from last active time map
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage()); // Log any errors during closure
            }
        }
    }


    /**
     * Loads existing weather data from the DATA_FILE into memory.
     */
    private static void loadWeatherData() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder jsonData = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonData.append(line); // Read the entire file content
                }
                parseAndStoreWeatherData(jsonData.toString()); // Parse JSON data into Weather objects
            } catch (IOException e) {
                System.err.println("Error loading weather data: " + e.getMessage());
            }
        }
    }

    /**
     * Parses JSON data and stores it into the recentWeatherData list.
     *
     * @param jsonData The JSON data string to parse
     */
    private static void parseAndStoreWeatherData(String jsonData) {
        try {
            JSONArray jsonArray = new JSONArray(jsonData); // Parse the JSON array
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i); // Get each JSON object
                Weather weatherData = new Weather(
                        jsonObject.getString("id"),
                        jsonObject.getString("name"),
                        jsonObject.getString("state"),
                        jsonObject.getString("time_zone"),
                        jsonObject.getDouble("lat"),
                        jsonObject.getDouble("lon"),
                        jsonObject.getString("local_date_time"),
                        jsonObject.getString("local_date_time_full"),
                        jsonObject.getDouble("air_temp"),
                        jsonObject.getDouble("apparent_t"),
                        jsonObject.getString("cloud"),
                        jsonObject.getDouble("dewpt"),
                        jsonObject.getDouble("press"),
                        jsonObject.getDouble("rel_hum"),
                        jsonObject.getString("wind_dir"),
                        jsonObject.getDouble("wind_spd_kmh"),
                        jsonObject.getDouble("wind_spd_kt")
                );
                recentWeatherData.add(weatherData); // Add the Weather object to the list
            }
        } catch (JSONException e) {
            System.err.println("Error parsing weather data: " + e.getMessage());
        }
    }

    /**
     * Starts a thread for cleaning up stale data.
     */
    private static void startDataCleanupThread() {
        Thread dataCleanupThread = new Thread(() -> {
            while (true) {
                cleanupStaleData(); // Cleanup stale data periodically
                try {
                    Thread.sleep(1000); // Sleep for 1 second between cleanup cycles
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        dataCleanupThread.start();
    }

    /**
     * Starts a thread for managing client connections.
     */
    private static void startConnectionManagerThread() {
        Thread connectionManagerThread = new Thread(() -> {
            while (true) {
                manageConnections(); // Manage connections periodically
                try {
                    Thread.sleep(1000); // Sleep for 1 second between connection checks
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        connectionManagerThread.start();
    }

    /**
     * Accepts client connections and handles them.
     *
     * @param port The port number to listen for incoming connections
     */
    public static void createAndHandleClientConnections(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Aggregation Server started on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming client connections
                System.out.println("Received connection from client: " + clientSocket.getRemoteSocketAddress());
                String serverId = generateUniqueId(); // Generate a unique ID for the client
                serverIds.put(clientSocket, serverId); // Store the client socket and ID

                // Handle client connection in a separate thread
                Thread clientHandlerThread = new Thread(() -> handleClient(clientSocket, serverId));
                clientHandlerThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles incoming client connections, processes their requests,
     * and sends appropriate HTTP responses based on the request type.
     *
     * @param clientSocket The socket representing the client connection.
     * @param serverId     The unique ID of the server handling the client.
     */
    public static void handleClient(Socket clientSocket, String serverId) {
        try (
                // Input and output streams for communication with the client
                DataInputStream inputData = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream outputData = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            // Read the message sent by the client
            String message = inputData.readUTF();
            clientSocket.setSoTimeout(15000); // Set a timeout for reading data

            // Check if the message is not null and process it
            if (message != null) {
                if (message.startsWith("GET")) {
                    // Handle GET request by sending weather data
                    sendWeatherData(outputData);
                } else if (message.startsWith("PUT")) {
                    StringBuilder jsonDataBuilder = new StringBuilder();
                    // Process incoming weather data
                    if (processWeatherData(message, serverId, jsonDataBuilder)) {
                        boolean isDataInitialized = false;
                        if (!isDataInitialized) {
                            outputData.writeUTF("HTTP/1.1 201 Created\r\n\r\nData created."); // Respond with status 201
                            isDataInitialized = true; // Mark data as initialized
                        } else {
                            outputData.writeUTF("HTTP/1.1 200 OK\r\n\r\nData updated."); // Respond with status 200
                        }
                        saveWeatherData(); // Save the updated weather data to file
                    } else {
                        // Invalid data provided
                        outputData.writeUTF("HTTP/1.1 400 Bad Request\r\n\r\nInvalid data."); // Respond with status 400
                    }
                    outputData.flush(); // Ensure the response is sent
                } else {
                    // Handle unsupported request methods
                    outputData.writeUTF("HTTP/1.1 400 Bad Request\r\n\r\nInvalid request."); // Respond with status 400
                    outputData.flush(); // Ensure the response is sent
                }
            }
        } catch (IOException e) {
            // Log connection error messages to standard error output
            System.err.println("Connection error: " + e.getMessage());
        } catch (Exception e) {
            // Handle unexpected exceptions
            try {
                // Respond with HTTP 500 Internal Server Error
                outputData.writeUTF("HTTP/1.1 500 Internal Server Error\r\n\r\n" + e.getMessage()); // Include the error message
                outputData.flush(); // Ensure the response is sent
            } catch (IOException ioException) {
                // Log any failure to send the error response
                System.err.println("Failed to send error response: " + ioException.getMessage());
            }
        } finally {
            // Close the client socket to free up resources
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Failed to close client socket: " + e.getMessage());
            }
        }
    }


    /**
     * Sends the most recent weather data to the client in response to a GET request.
     *
     * @param outputData The output stream to send the response
     * @throws IOException If an I/O error occurs while sending the response
     */
    private static void sendWeatherData(DataOutputStream outputData) throws IOException {
        if (!recentWeatherData.isEmpty()) {
            Weather latestWeather = recentWeatherData.get(recentWeatherData.size() - 1); // Get the latest weather data
            String responseData = latestWeather.toString(); // Prepare response data

            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("HTTP/1.1 200 OK\r\n");
            responseBuilder.append("Content-Type: application/json\r\n");
            responseBuilder.append("Content-Length: ").append(responseData.length()).append("\r\n\r\n");
            responseBuilder.append(responseData);

            outputData.writeUTF(responseBuilder.toString()); // Send the response to the client
            outputData.flush();
        } else {
            outputData.writeUTF("HTTP/1.1 204 No Content\r\n\r\n"); // Respond with status 204 if no content is available
            outputData.flush();
        }
    }

    /**
     * Processes weather data received in a PUT request and stores it.
     *
     * @param message         The PUT request message containing weather data
     * @param serverId        The ID of the server sending the data
     * @param jsonDataBuilder The StringBuilder to append JSON data
     * @return True if the data was successfully processed; false otherwise
     */
    public static boolean processWeatherData(String message, String serverId, StringBuilder jsonDataBuilder) {
        try {
            int jsonStartIndex = message.indexOf("{");
            if (jsonStartIndex == -1) {
                return false; // Invalid JSON format
            }

            String jsonData = message.substring(jsonStartIndex); // Extract JSON data from the message
            jsonDataBuilder.append(jsonData);

            JSONObject jsonObject = new JSONObject(jsonData); // Parse the JSON data

            // Creating the Weather object from JSON fields
            Weather weatherData = new Weather(
                    jsonObject.getString("id"),
                    jsonObject.getString("name"),
                    jsonObject.getString("state"),
                    jsonObject.getString("time_zone"),
                    jsonObject.getDouble("lat"),
                    jsonObject.getDouble("lon"),
                    jsonObject.getString("local_date_time"),
                    jsonObject.getString("local_date_time_full"),
                    jsonObject.getDouble("air_temp"),
                    jsonObject.getDouble("apparent_t"),
                    jsonObject.getString("cloud"),
                    jsonObject.getDouble("dewpt"),
                    jsonObject.getDouble("press"),
                    jsonObject.getDouble("rel_hum"),
                    jsonObject.getString("wind_dir"),
                    jsonObject.getDouble("wind_spd_kmh"),
                    jsonObject.getDouble("wind_spd_kt")
            );

            recentWeatherData.add(weatherData); // Add the new weather data to the list
            if (recentWeatherData.size() > 10) {
                recentWeatherData.remove(0); // Keep only the latest 10 entries
            }

            return true; // Data processed successfully
        } catch (JSONException e) {
            System.err.println("JSON parsing error: " + e.getMessage()); // Log parsing errors
            return false; // Data processing failed
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage()); // Log unexpected errors
            return false; // Data processing failed
        }
    }

    /**
     * Cleans up stale data by removing old connections and related weather data.
     */
    public static void cleanupStaleData() {
        try {
            long currentTime = System.currentTimeMillis();
            List<Socket> socketsToClose = new ArrayList<>();

            for (Socket clientSocket : serverLastActiveTime.keySet()) {
                long lastActiveTime = serverLastActiveTime.get(clientSocket);
                if (currentTime - lastActiveTime > CONNECTION_TIMEOUT) {
                    socketsToClose.add(clientSocket); // Mark idle connections for closure
                }
            }

            for (Socket socketToClose : socketsToClose) {
                try {
                    System.out.println("Closing idle connection with client: " + socketToClose.getRemoteSocketAddress());
                    socketToClose.close(); // Close the idle socket
                    serverIds.remove(socketToClose);
                    serverLastActiveTime.remove(socketToClose);
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage()); // Log any exceptions
                }
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up stale data: " + e.getMessage()); // Log any errors that occur
        }
    }

    /**
     * Generates a unique ID using Universally Unique Identifier (UUID).
     *
     * @return A unique ID represented as a string
     */
    public static String generateUniqueId() {
        return UUID.randomUUID().toString(); // Generate and return a unique ID
    }

    /**
     * Saves the current weather data to the main data file.
     */
    public static void saveWeatherData() {
        try (FileWriter fileWriter = new FileWriter(DATA_FILE)) {
            JSONArray jsonArray = new JSONArray(); // Create a new JSON array
            for (Weather weather : recentWeatherData) {
                JSONObject jsonObject = new JSONObject(); // Create a new JSON object for each Weather entry
                jsonObject.put("id", weather.getId());
                jsonObject.put("name", weather.getName());
                jsonObject.put("state", weather.getState());
                jsonObject.put("time_zone", weather.getTime_zone());
                jsonObject.put("lat", weather.getLat());
                jsonObject.put("lon", weather.getLon());
                jsonObject.put("local_date_time", weather.getLocal_date_time());
                jsonObject.put("local_date_time_full", weather.getLocal_date_time_full());
                jsonObject.put("air_temp", weather.getAir_temp());
                jsonObject.put("apparent_t", weather.getApparent_t());
                jsonObject.put("cloud", weather.getCloud());
                jsonObject.put("dewpt", weather.getDewpt());
                jsonObject.put("press", weather.getPress());
                jsonObject.put("rel_hum", weather.getRel_hum());
                jsonObject.put("wind_dir", weather.getWind_dir());
                jsonObject.put("wind_spd_kmh", weather.getWind_spd_kmh());
                jsonObject.put("wind_spd_kt", weather.getWind_spd_kt());
                jsonArray.put(jsonObject); // Add each JSON object to the array
            }
            fileWriter.write(jsonArray.toString()); // Write the JSON array to the data file
        } catch (IOException e) {
            System.err.println("Error saving weather data: " + e.getMessage()); // Log any errors
        }
    }

    public static void storeData(String data, String serverId) {
        String dataDirectory = "data/";
        File directory = new File(dataDirectory);
        if (!directory.exists() && !directory.mkdirs()) {
            System.err.println("Failed to create data directory: " + dataDirectory);
            return;
        }

        // Store the data in a temporary file first
        try {
            String tempFileName = dataDirectory + serverId + "_temp.json";
            try (FileWriter tempFileWriter = new FileWriter(tempFileName)) {
                tempFileWriter.write(data);
            }

            // Rename the temporary file to the actual file name
            String finalFileName = dataDirectory + serverId + ".json";
            File tempFile = new File(tempFileName);
            File finalFile = new File(finalFileName);
            if (!tempFile.renameTo(finalFile)) {
                System.err.println("Failed to rename temp file to final file.");
            }
        } catch (IOException e) {
            System.err.println("Error storing data: " + e.getMessage());
        }
    }

    public static void loadData(String serverId) {
        String finalFileName = "data/" + serverId + ".json";
        File dataFile = new File(finalFileName);

        if (dataFile.exists()) {
            // Logic to read and parse the JSON data
            try {
                String jsonData = new String(Files.readAllBytes(dataFile.toPath()));
                // Process the jsonData as needed
            } catch (IOException e) {
                System.err.println("Failed to load data: " + e.getMessage());
            }
        } else {
            System.out.println("No previous data found for server ID: " + serverId);
        }
    }


    public static boolean processRequest(String message) {

        return false;
    }

    private static boolean running = false;
    private static List<String> staleData = new ArrayList<>();


        // Start server logic
    // Set server to running state


    public static void stopServer() {
        running = false; // Set server to stopped state
    }

    public static boolean isRunning() {
        return running; // Return the current state of the server
    }

    public static void addStaleData(String data) {
        staleData.add(data); // Add data to the stale data list
    }

    public static boolean hasData(String data) {
        return staleData.contains(data); // Check if the data exists in the stale data list
    }

}
