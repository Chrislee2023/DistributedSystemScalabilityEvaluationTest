package com.weather.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AggregationServer {

    // Atomic integer to manage the Lamport clock
    private final AtomicInteger lamportClock = new AtomicInteger(0);

    // ConcurrentHashMap to store weather data from multiple stations
    private final ConcurrentHashMap<String, WeatherStationData> weatherDataMap = new ConcurrentHashMap<>();

    // Server port number
    private final int port;

    // JSON file to persist data
    private static final String DATA_FILE = "weatherData.json";

    // Gson instance for JSON operations
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Constructor to initialize the server with a port number
    public AggregationServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        int portNumber = 4567; // Default port
        if (args.length > 0) {
            try {
                portNumber = Integer.parseInt(args[0]); // Parse port number from arguments
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number provided. Using default port 4567.");
            }
        }
        AggregationServer server = new AggregationServer(portNumber);
        server.startServer(); // Start the server
    }

    // Method to start the server and listen for client connections
    public void startServer() {
        // Load existing data from JSON file
        loadDataFromFile();

        // Start background task for data expiration
        startDataExpirationTask();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Aggregation Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept(); // Accept client connections
                new ServerHandler(socket, this).start(); // Handle each connection in a new thread
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
        }
    }

    // Update Lamport clock based on received clock from client
    public synchronized int updateLamportClock(int receivedClock) {
        int currentClock = lamportClock.get();
        lamportClock.set(Math.max(currentClock, receivedClock) + 1); // Update clock
        return lamportClock.get();
    }

    // Increment the Lamport clock and return the new value
    public synchronized int incrementLamportClock() {
        lamportClock.incrementAndGet();
        return lamportClock.get();
    }

    // Getter for the current Lamport clock value
    public int getLamportClock() {
        return lamportClock.get();
    }

    // Getter for the weather data map
    public ConcurrentHashMap<String, WeatherStationData> getWeatherDataMap() {
        return weatherDataMap;
    }

    // Save weather data to a JSON file
    synchronized void saveDataToFile() {
        try (FileWriter writer = new FileWriter(DATA_FILE)) {
            gson.toJson(weatherDataMap, writer); // Convert the map to JSON and write to file
            System.out.println("Data saved to " + DATA_FILE); // Log success
        } catch (IOException e) {
            System.err.println("Error saving data to JSON file: " + e.getMessage()); // Log error
        }
    }

    // Load weather data from a JSON file
    void loadDataFromFile() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                // Deserialize the JSON file back into the map
                ConcurrentHashMap<String, WeatherStationData> data = gson.fromJson(reader, new com.google.gson.reflect.TypeToken<ConcurrentHashMap<String, WeatherStationData>>() {}.getType());
                if (data != null) {
                    weatherDataMap.putAll(data);
                    System.out.println("Data loaded from " + DATA_FILE);
                }
            } catch (IOException e) {
                System.err.println("Error loading data from JSON file: " + e.getMessage());
            }
        } else {
            System.out.println("No existing data file found.");
        }
    }

    // Start a scheduled task to expire old weather data
    private void startDataExpirationTask() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            weatherDataMap.values().removeIf(data -> currentTime - data.getLastUpdateTime() > 30000);
        }, 30, 30, TimeUnit.SECONDS);
    }

    // Thread to handle each client request
    static class ServerHandler extends Thread {
        private final Socket socket; // Client socket
        private final AggregationServer server; // Reference to the server

        // Constructor to initialize the handler with the client socket and server reference
        public ServerHandler(Socket socket, AggregationServer server) {
            this.socket = socket;
            this.server = server;
        }

        // Main run method for handling client communication
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 OutputStream os = socket.getOutputStream()) {

                PrintWriter out = new PrintWriter(os, true);

                // Read the request line from the client
                String requestLine = in.readLine();
                if (requestLine == null) return; // Exit if no request line

                // Read headers from the client
                Map<String, String> headers = readHeaders(in);

                // Retrieve Lamport clock from headers
                int clientLamportClock = Integer.parseInt(headers.getOrDefault("Lamport-Clock", "0"));
                server.updateLamportClock(clientLamportClock); // Update the server clock

                // Process the request based on the method
                if (requestLine.startsWith("PUT")) {
                    handlePutRequest(in, headers, out, os); // Handle PUT request
                } else if (requestLine.startsWith("GET")) {
                    handleGetRequest(headers, out, os); // Handle GET request
                } else {
                    sendResponse(out, "HTTP/1.1 400 Bad Request", "Invalid request method.", "text/plain");
                }
            } catch (IOException e) {
                System.out.println("Exception in handler: " + e.getMessage());
            }
        }

        // Method to read headers from the client request
        private Map<String, String> readHeaders(BufferedReader in) throws IOException {
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) { // Read until an empty line is found
                String[] headerParts = headerLine.split(": ", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]); // Store header key-value pairs
                }
            }
            return headers;
        }

        // Handle PUT requests to update weather data
        private void handlePutRequest(BufferedReader in, Map<String, String> headers,
                                      PrintWriter out, OutputStream os) throws IOException {
            int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));

            // Handle empty content
            if (contentLength == 0) {
                sendResponse(out, "HTTP/1.1 204 No Content", "No content provided.", "text/plain");
                return;
            }

            // Read the JSON data from the request body
            char[] bodyChars = new char[contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int read = in.read(bodyChars, totalRead, contentLength - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }
            String body = new String(bodyChars);

            Map<String, String> weatherData;
            try {
                // Parse the JSON data into a map
                weatherData = parseJson(body);
            } catch (Exception e) {
                // Invalid JSON
                sendResponse(out, "HTTP/1.1 500 Internal Server Error", "Invalid JSON data.", "text/plain");
                return;
            }

            // Update the weather data map based on station ID
            String stationId = weatherData.get("id");
            if (stationId != null) {
                boolean isNewStation = !server.getWeatherDataMap().containsKey(stationId);
                // Get or create a new WeatherStationData object
                WeatherStationData stationData = server.getWeatherDataMap()
                        .computeIfAbsent(stationId, k -> new WeatherStationData());

                synchronized (stationData) {
                    // Update station data
                    stationData.updateData(weatherData);
                    stationData.setLamportClock(server.getLamportClock());
                    stationData.setLastUpdateTime(System.currentTimeMillis());
                }

                // Save data to file
                server.saveDataToFile();

                String statusLine = isNewStation ? "HTTP/1.1 201 Created" : "HTTP/1.1 200 OK";
                sendResponse(out, statusLine, "Data updated successfully.", "text/plain");
            } else {
                sendResponse(out, "HTTP/1.1 400 Bad Request", "Station ID is missing.", "text/plain");
            }
        }

        public void handleGetRequest(Map<String, String> headers, PrintWriter out, OutputStream os) {
            // Build JSON response with aggregated weather data
            StringBuilder responseBody = new StringBuilder();
            responseBody.append("[");

            Iterator<WeatherStationData> iterator = server.getWeatherDataMap().values().iterator();
            boolean isFirst = true;
            while (iterator.hasNext()) {
                WeatherStationData data = iterator.next();
                synchronized (data) {
                    if (!isFirst) {
                        responseBody.append(","); // Append comma before subsequent entries
                    }
                    responseBody.append(data.toJson());
                    isFirst = false;
                }
            }
            responseBody.append("]");

            // Log the response for debugging
            System.out.println("Generated JSON Response: " + responseBody);

            // Send response with aggregated data
            sendResponse(out, "HTTP/1.1 200 OK", responseBody.toString(), "application/json");
        }

        // Parse a JSON string into a Map
        private Map<String, String> parseJson(String json) throws Exception {
            Map<String, String> dataMap = new HashMap<>();
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1); // Remove curly braces
                String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Split by commas not within quotes
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replaceAll("^\"|\"$", "");
                        String value = kv[1].trim().replaceAll("^\"|\"$", "");
                        dataMap.put(key, value); // Add key-value pairs to the map
                    } else {
                        throw new Exception("Invalid JSON pair: " + pair);
                    }
                }
            } else {
                throw new Exception("Invalid JSON format");
            }
            return dataMap;
        }

        // Send HTTP response to the client
        private void sendResponse(PrintWriter out, String statusLine, String body, String contentType) {
            int lamportClock = server.incrementLamportClock(); // Increment the clock before sending
            out.println(statusLine);
            out.println("Content-Type: " + contentType);
            out.println("Lamport-Clock: " + lamportClock);
            out.println("Content-Length: " + body.getBytes().length);
            out.println(); // Empty line to indicate end of headers
            out.println(body); // Send the response body
            out.flush();
        }
    }
}

// Class to hold weather data for a specific station
class WeatherStationData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, String> data = new HashMap<>(); // Weather data key-value pairs
    private int lamportClock; // Lamport clock for data updates
    private long lastUpdateTime; // Timestamp of the last update

    // Update weather data with new values
    public void updateData(Map<String, String> newData) {
        data.putAll(newData); // Add all new data
    }

    // Set the Lamport clock value
    public void setLamportClock(int clock) {
        this.lamportClock = clock;
    }

    // Get the last update timestamp
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    // Set the last update timestamp
    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    // Convert weather station data to JSON format
    public String toJson() {
        return new Gson().toJson(data);
    }
}