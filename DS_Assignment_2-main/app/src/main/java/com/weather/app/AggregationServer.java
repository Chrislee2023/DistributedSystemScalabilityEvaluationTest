package com.weather.app;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class AggregationServer {
    private static final int DEFAULT_PORT = 4567;  // Default port number
    private static final String DATA_FILE = "weatherData.json";  // File to store weather data
    private static final String TEMP_FILE = "weatherData.tmp";  // Temporary file for data persistence
    private static final long EXPIRATION_TIME_MILLIS = 30_000;  // Expiration time for data (30 seconds)
    public static final LamportClock lamportClockInstance = new LamportClock();  // Instance of Lamport Clock

    // Data structures to store weather data and timestamps of content servers
    public static final Map<String, JsonObject> weatherDataStore = new ConcurrentHashMap<>();
    public static final Map<String, Long> serverTimestamps = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;

        // Check if a port number is passed as a command-line argument
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port: " + DEFAULT_PORT);
            }
        }

        // Schedule periodic cleanup of expired entries
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(AggregationServer::removeExpiredData, 10, 10, TimeUnit.SECONDS);

        // Start server and listen for client connections
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Aggregation Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClientRequest(clientSocket)).start();  // Handle each client in a new thread
            }
        }
    }

    // Handle client requests (GET or PUT)
    public static void handleClientRequest(Socket clientSocket) {
        try (Socket socket = clientSocket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String requestLine = in.readLine();
            System.out.println("Received request: " + requestLine);

            String[] requestParts = requestLine.split(" ", 2);
            String method = requestParts.length >= 1 ? requestParts[0] : "";  // Get request method (e.g., GET or PUT)
            String path = requestParts.length >= 2 ? requestParts[1] : "";  // Get request path

            // Read headers and extract Lamport-Clock
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            int clientClockValue = 0;

            // Read headers and check for Lamport-Clock
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                int colonIndex = headerLine.indexOf(":");
                if (colonIndex != -1) {
                    String headerName = headerLine.substring(0, colonIndex).trim();
                    String headerValue = headerLine.substring(colonIndex + 1).trim();
                    headers.put(headerName, headerValue);

                    // Capture the Lamport-Clock value from the header
                    if (headerName.equalsIgnoreCase("Lamport-Clock")) {
                        clientClockValue = Integer.parseInt(headerValue);
                    }
                }
            }

            // Update the Lamport clock
            lamportClockInstance.update(clientClockValue);

            // Handle the request based on method (PUT or GET)
            if ("PUT".equalsIgnoreCase(method)) {
                processPutRequest(in, out, clientSocket.getInetAddress().toString(), headers);
            } else if ("GET".equalsIgnoreCase(method)) {
                processGetRequest(out, headers);
            } else {
                out.println("HTTP/1.1 400 Bad Request");
                out.println("Lamport-Clock: " + lamportClockInstance.getClock());
                out.println();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Process PUT requests
    public static void processPutRequest(BufferedReader in, PrintWriter out, String contentServer, Map<String, String> headers) throws IOException {
        lamportClockInstance.increment();

        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));

        // No content provided
        if (contentLength == 0) {
            out.println("HTTP/1.1 204 No Content");
            out.println("Lamport-Clock: " + lamportClockInstance.getClock());
            out.println();
            return;
        }

        // Read the request body (JSON data)
        char[] requestBody = new char[contentLength];
        in.read(requestBody, 0, contentLength);
        String jsonData = new String(requestBody);

        // Validate the received JSON data
        if (!isJsonValid(jsonData)) {
            System.out.println("Invalid JSON received: " + jsonData);
            out.println("HTTP/1.1 500 Internal Server Error");
            out.println("Lamport-Clock: " + lamportClockInstance.getClock());
            out.println();
            return;
        }

        // Add metadata (timestamp and content server origin)
        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
        jsonObject.addProperty("origin", contentServer);
        jsonObject.addProperty("timestamp", Instant.now().toEpochMilli());

        // Store the weather data
        String entryId = jsonObject.get("id").getAsString();
        weatherDataStore.put(entryId, jsonObject);
        serverTimestamps.put(contentServer, Instant.now().toEpochMilli());

        // Persist the data to a file
        boolean isNewFile = !new File(DATA_FILE).exists();
        try {
            writeToTempFile(weatherDataStore);  // Write to temp file first
            if (commitTempFile()) {  // Commit temp file to final storage
                if (isNewFile) {
                    out.println("HTTP/1.1 201 Created");
                } else {
                    out.println("HTTP/1.1 200 OK");
                }
                out.println("Lamport-Clock: " + lamportClockInstance.getClock());
                out.println();
            } else {
                out.println("HTTP/1.1 500 Internal Server Error");
                out.println("Lamport-Clock: " + lamportClockInstance.getClock());
                out.println();
            }
        } catch (IOException e) {
            System.out.println("File write error: " + e.getMessage());
            out.println("HTTP/1.1 500 Internal Server Error");
            out.println("Lamport-Clock: " + lamportClockInstance.getClock());
            out.println();
        }
    }

    // Validate if the string is a valid JSON object
    public static boolean isJsonValid(String jsonData) {
        try {
            JsonElement jsonElement = JsonParser.parseString(jsonData);
            return jsonElement.isJsonObject();
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    // Process GET requests
    public static void processGetRequest(PrintWriter out, Map<String, String> headers) throws IOException {
        lamportClockInstance.increment();

        // If no weather data, return an empty JSON array
        String jsonResponse = weatherDataStore.isEmpty() ? "[]" : convertWeatherDataToJson(weatherDataStore);

        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: application/json");
        out.println("Content-Length: " + jsonResponse.length());
        out.println("Lamport-Clock: " + lamportClockInstance.getClock());
        out.println();
        out.print(jsonResponse);  // Send the weather data as JSON response
        out.flush();
    }

    // Write data to a temporary file before committing
    public static void writeToTempFile(Map<String, JsonObject> data) throws IOException {
        try (FileWriter fileWriter = new FileWriter(TEMP_FILE)) {
            fileWriter.write(new Gson().toJson(data.values()));
        }
    }

    // Commit the temporary file to final storage (move from temp file to data file)
    private static boolean commitTempFile() throws IOException {
        File tempFile = new File(TEMP_FILE);
        File finalFile = new File(DATA_FILE);

        try (FileReader fileReader = new FileReader(tempFile);
             FileWriter fileWriter = new FileWriter(finalFile)) {

            char[] buffer = new char[1024];
            int read;
            while ((read = fileReader.read(buffer)) != -1) {
                fileWriter.write(buffer, 0, read);
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error while committing file: " + e.getMessage());
            return false;
        } finally {
            tempFile.delete();  // Delete the temporary file after committing
        }
    }

    // Convert weather data map to JSON string
    public static String convertWeatherDataToJson(Map<String, JsonObject> weatherDataMap) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Collection<JsonObject> dataCollection = weatherDataMap.values();
        return gson.toJson(dataCollection);
    }

    // Remove expired data from the data store
    public static void removeExpiredData() {
        long currentTime = Instant.now().toEpochMilli();
        Iterator<Map.Entry<String, JsonObject>> iterator = weatherDataStore.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonObject> entry = iterator.next();
            JsonObject jsonObject = entry.getValue();
            long timestamp = jsonObject.get("timestamp").getAsLong();
            String origin = jsonObject.get("origin").getAsString();

            // Remove expired entries (older than expiration time)
            if (currentTime - timestamp > EXPIRATION_TIME_MILLIS) {
                System.out.println("Removing expired entry from " + origin);
                iterator.remove();
                serverTimestamps.remove(origin);
            }
        }
    }
}