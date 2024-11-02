package com.weather.app;

import java.io.*;
import java.net.*;
import java.util.*;

public class ContentServer {
    private static String serverAddress;
    private static int serverPort;
    private static String weatherDataFilePath;
    public static final LamportClock lamportClockInstance = new LamportClock();

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java ContentServer <server-address> <server-port> <file-path>");
            return;
        }

        serverAddress = args[0];
        try {
            serverPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number provided.");
            return;
        }

        weatherDataFilePath = args[2];

        try {
            String jsonWeatherData = readWeatherDataFromFile(weatherDataFilePath);
            if (jsonWeatherData != null) {
                try (Socket socket = new Socket(serverAddress, serverPort);
                     PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    sendWeatherDataToServer(jsonWeatherData, socket, writer, reader);

                } catch (IOException e) {
                    System.err.println("Failed to send weather data: " + e.getMessage());
                }
            } else {
                System.out.println("Error: Weather data is invalid or missing 'id'.");
            }
        } catch (IOException e) {
            System.err.println("Error reading weather data file: " + e.getMessage());
        }
    }

    public static String readWeatherDataFromFile(String filePath) throws IOException {
        Map<String, String> weatherDataMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] keyValue = line.split(":", 2);
                if (keyValue.length == 2) {
                    weatherDataMap.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }

        if (!weatherDataMap.containsKey("id")) {
            return null;
        }

        return convertMapToJson(weatherDataMap);
    }

    private static String convertMapToJson(Map<String, String> dataMap) {
        StringBuilder jsonBuilder = new StringBuilder("{\n");
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            jsonBuilder.append("  \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\",\n");
        }
        jsonBuilder.setLength(jsonBuilder.length() - 2);
        jsonBuilder.append("\n}");
        return jsonBuilder.toString();
    }

    // Updated sendWeatherDataToServer method signature
    public static void sendWeatherDataToServer(String jsonData, Socket socket, PrintWriter writer, BufferedReader reader) {
        lamportClockInstance.increment();

        String httpRequest = buildHttpPutRequest(jsonData);
        writer.print(httpRequest);
        writer.flush();

        try {
            processServerResponse(reader);
        } catch (IOException e) {
            System.err.println("Error processing server response: " + e.getMessage());
        }
    }

    public static String buildHttpPutRequest(String jsonData) {
        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append("PUT /weather.json HTTP/1.1\r\n");
        requestBuilder.append("Host: ").append(serverAddress).append("\r\n");
        requestBuilder.append("User-Agent: ContentServer/1.0\r\n");
        requestBuilder.append("Content-Type: application/json\r\n");
        requestBuilder.append("Content-Length: ").append(jsonData.length()).append("\r\n");
        requestBuilder.append("Lamport-Clock: ").append(lamportClockInstance.getClock()).append("\r\n");
        requestBuilder.append("\r\n");
        requestBuilder.append(jsonData);
        return requestBuilder.toString();
    }

    public static void processServerResponse(BufferedReader responseReader) throws IOException {
        String responseLine;
        int receivedLamportClock = lamportClockInstance.getClock();
        boolean lamportClockReceived = false;

        while ((responseLine = responseReader.readLine()) != null && !responseLine.isEmpty()) {
            System.out.println("Server Response: " + responseLine);

            if (responseLine.startsWith("Lamport-Clock:")) {
                receivedLamportClock = Integer.parseInt(responseLine.substring("Lamport-Clock:".length()).trim());
                lamportClockReceived = true;
            }
        }

        if (lamportClockReceived) {
            lamportClockInstance.update(receivedLamportClock);
            System.out.println("Lamport clock updated to: " + lamportClockInstance.getClock());
        }
    }
}
