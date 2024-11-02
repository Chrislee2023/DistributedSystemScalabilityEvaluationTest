package com.weather.app;

import java.io.*;
import java.net.*;
import java.util.*;

public class GETClient {

    private static int lamportClock = 0; // Clock to manage synchronization

    public static void main(String[] args) {
        // Check for command-line arguments
        if (args.length < 1) {
            System.out.println("Usage: java GETClient <server-url> [station-id]");
            return;
        }

        String serverUrl = args[0]; // Server URL from arguments
        String stationId = args.length > 1 ? args[1] : null; // Optional station ID

        try {
            // Initialize socket connection to the server
            URL url = new URL(serverUrl);
            Socket socket = new Socket(url.getHost(), url.getPort() == -1 ? 80 : url.getPort());

            // Prepare input and output streams
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Create and send the GET request
            String response = sendGetRequest(out, url, stationId);

            // Read and process the server response
            processServerResponse(in, response);

            socket.close(); // Close the socket connection

        } catch (Exception e) {
            System.out.println("Client exception: " + e.getMessage());
        }
    }

    // Method to send a GET request and return the response
    private static String sendGetRequest(PrintWriter out, URL url, String stationId) {
        // Construct the request path
        String path = url.getPath();
        if (stationId != null) {
            path += "?id=" + stationId; // Append station ID if provided
        }

        lamportClock++; // Increment Lamport clock
        out.println("GET " + path + " HTTP/1.1"); // Send GET request line
        out.println("Host: " + url.getHost()); // Specify the host
        out.println("Lamport-Clock: " + lamportClock); // Include Lamport clock
        out.println("User-Agent: GETClient/1/0");
        out.println("Connection: close"); // Request to close connection
        out.println(); // End of headers

        return path; // Return the request path for later use
    }

    // Method to process the server's response
    private static void processServerResponse(BufferedReader in, String requestPath) throws IOException {
        // Read the status line
        String statusLine = in.readLine();
        if (statusLine == null) {
            System.out.println("No response from server.");
            return;
        }
        System.out.println("Status: " + statusLine);

        // Read headers and update Lamport clock
        Map<String, String> headers = readHeaders(in);
        updateLamportClock(headers);

        // Get content length
        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        String contentType = headers.getOrDefault("Content-Type", "text/plain");

        // Read the response body
        char[] bodyChars = new char[contentLength];
        int totalRead = 0;
        while (totalRead < contentLength) {
            int read = in.read(bodyChars, totalRead, contentLength - totalRead);
            if (read == -1) {
                break;
            }
            totalRead += read;
        }
        String responseBody = new String(bodyChars);

        // Parse and display the response data
        parseAndDisplayJson(responseBody);
    }

    // Method to read headers from the response
    private static Map<String, String> readHeaders(BufferedReader in) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String headerLine;

        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            String[] headerParts = headerLine.split(": ", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]); // Store headers in a map
            }
        }
        return headers; // Return the map of headers
    }

    // Method to update the Lamport clock based on server response
    private static void updateLamportClock(Map<String, String> headers) {
        int serverLamportClock = Integer.parseInt(headers.getOrDefault("Lamport-Clock", "0"));
        lamportClock = Math.max(lamportClock, serverLamportClock) + 1; // Update Lamport clock
    }

    // Method to parse and display the JSON response
    private static void parseAndDisplayJson(String json) {
        // Assuming the response is a JSON array
        json = json.trim();
        if (json.startsWith("[")) {
            json = json.substring(1, json.length() - 1); // Remove surrounding brackets
        }

        // Split the JSON objects
        List<String> jsonObjects = splitJsonObjects(json);

        for (String obj : jsonObjects) {
            obj = obj.trim();
            if (!obj.startsWith("{")) {
                obj = "{" + obj;
            }
            if (!obj.endsWith("}")) {
                obj = obj + "}";
            }
            Map<String, String> dataMap = parseJsonObject(obj);
            displayDataMap(dataMap);
            System.out.println("-------------------------");
        }
    }

    // Method to split JSON objects in an array
    private static List<String> splitJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        StringBuilder currentObject = new StringBuilder();
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            currentObject.append(c);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    objects.add(currentObject.toString());
                    currentObject.setLength(0);
                }
            }
        }
        return objects;
    }

    // Method to parse a JSON object string into a Map
    private static Map<String, String> parseJsonObject(String json) {
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
                }
            }
        }
        return dataMap;
    }

    // Method to display data map
    private static void displayDataMap(Map<String, String> dataMap) {
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}