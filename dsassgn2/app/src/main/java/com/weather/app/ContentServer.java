package com.weather.app;

import java.io.*;
import java.net.*;
import java.util.*;

public class ContentServer {

    private static int lamportClock = 0; // Lamport clock for synchronization

    public static void main(String[] args) {
        // Check if the correct number of command-line arguments is provided
        if (args.length < 2) {
            System.out.println("Usage: java ContentServer <server-url> <file-path>");
            return; // Exit if not enough arguments
        }

        String serverUrl = args[0]; // The server URL to send data to
        String filePath = args[1]; // The file path containing weather data

        try {
            // Read data from the specified file
            Map<String, String> weatherData = readDataFromFile(filePath);
            // Check if data was read successfully
            if (weatherData.isEmpty()) {
                System.out.println("No data read from file.");
                return; // Exit if no data is available
            }

            // Convert the weather data map to a JSON string
            String jsonData = convertToJson(weatherData);
            // Send the JSON data to the server using a PUT request
            sendPutRequest(serverUrl, jsonData);
            // Confirm that the data is stored on the server as expected
            confirmDataOnServer(serverUrl, weatherData.get("id"));

        } catch (IOException e) {
            // Handle any IO exceptions that may occur
            System.err.println("Error: " + e.getMessage());
        }
    }

    // Method to read key-value data from the specified file
    private static Map<String, String> readDataFromFile(String filePath) throws IOException {
        Map<String, String> dataMap = new HashMap<>(); // Map to store the key-value pairs
        // Use try-with-resources to ensure the BufferedReader is closed automatically
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Read each line from the file
            while ((line = br.readLine()) != null) {
                // Split the line into key and value based on the first ':' character
                String[] kv = line.split(":", 2);
                // Only add valid key-value pairs to the map
                if (kv.length == 2) {
                    dataMap.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        return dataMap; // Return the populated map
    }

    // Method to convert the data map to a JSON string
    private static String convertToJson(Map<String, String> dataMap) {
        StringBuilder json = new StringBuilder("{"); // Start JSON object
        Iterator<Map.Entry<String, String>> iterator = dataMap.entrySet().iterator();

        // Iterate over the map entries to construct the JSON string
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            // Append key-value pairs in JSON format
            json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            // Add a comma if there are more entries
            if (iterator.hasNext()) {
                json.append(",");
            }
        }
        json.append("}"); // Close JSON object
        return json.toString(); // Return the constructed JSON string
    }

    // Method to send a PUT request to the specified server URL with JSON data
    private static void sendPutRequest(String serverUrl, String jsonData) {
        try {
            URL url = new URL(serverUrl); // Create a URL object from the server URL
            // Open a connection to the server
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT"); // Set the request method to PUT
            // Set the content type to JSON
            connection.setRequestProperty("Content-Type", "application/json");
            // Set the content length of the request
            connection.setRequestProperty("Content-Length", String.valueOf(jsonData.length()));
            // Set User-Agent header
            connection.setRequestProperty("User-Agent", "ATOMClient/1/0");
            // Set Lamport-Clock header
            connection.setRequestProperty("Lamport-Clock", String.valueOf(lamportClock));
            connection.setDoOutput(true); // Enable output for the connection

            // Write JSON data to the request body
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonData.getBytes()); // Convert JSON string to bytes and send
                os.flush(); // Ensure all data is sent
            }

            // Handle the server response
            handleResponse(connection);

        } catch (IOException e) {
            // Handle any IO exceptions that may occur during the request
            System.err.println("Error in sendPutRequest: " + e.getMessage());
        }
    }

    // Method to handle and read the server's response to the PUT request
    private static void handleResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode(); // Get the response code
        String responseMessage = connection.getResponseMessage();
        System.out.println("Response Code: " + responseCode + " " + responseMessage); // Print the response code

        // Update Lamport clock
        String serverLamportClockStr = connection.getHeaderField("Lamport-Clock");
        if (serverLamportClockStr != null) {
            int serverLamportClock = Integer.parseInt(serverLamportClockStr);
            lamportClock = Math.max(lamportClock, serverLamportClock) + 1;
        } else {
            lamportClock++;
        }

        // Read the server's response from the input stream
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                connection.getInputStream()))) {
            String line;
            StringBuilder response = new StringBuilder(); // StringBuilder to collect response lines
            // Read each line from the response
            while ((line = in.readLine()) != null) {
                response.append(line); // Append each line to the response
            }
            // Print the complete server response
            System.out.println("Server response: " + response.toString());
        } catch (IOException e) {
            // Handle any IO exceptions that may occur while reading the response
            System.err.println("Error reading response: " + e.getMessage());
        }

        // Handle different response codes
        if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
            // Success
        } else if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
            System.out.println("No content provided to the server.");
        } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
            System.out.println("Bad request sent to the server.");
        } else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            System.out.println("Server encountered an internal error.");
        } else {
            System.out.println("Unexpected response from the server.");
        }
    }

    // Method to confirm that data is stored on the server as expected
    private static void confirmDataOnServer(String serverUrl, String stationId) throws IOException {
        URL url = new URL(serverUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        // Set Lamport-Clock header
        connection.setRequestProperty("Lamport-Clock", String.valueOf(lamportClock));
        // Set User-Agent header
        connection.setRequestProperty("User-Agent", "ATOMClient/1/0");

        // Handle the server response
        int responseCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage();
        System.out.println("Confirm GET Response Code: " + responseCode + " " + responseMessage);

        // Update Lamport clock
        String serverLamportClockStr = connection.getHeaderField("Lamport-Clock");
        if (serverLamportClockStr != null) {
            int serverLamportClock = Integer.parseInt(serverLamportClockStr);
            lamportClock = Math.max(lamportClock, serverLamportClock) + 1;
        } else {
            lamportClock++;
        }

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                StringBuilder responseBody = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    responseBody.append(line);
                }

                // Check if the data is present
                if (responseBody.toString().contains("\"id\":\"" + stationId + "\"")) {
                    System.out.println("Data confirmed on server.");
                } else {
                    System.out.println("Data not found on server.");
                }
            }
        } else {
            System.out.println("Failed to confirm data on server.");
        }
    }
}