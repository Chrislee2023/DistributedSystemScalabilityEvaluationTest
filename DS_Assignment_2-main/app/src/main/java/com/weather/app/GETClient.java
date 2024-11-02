package com.weather.app;

import com.google.gson.*;
import java.io.*;
import java.net.*;

public class GETClient {

    public static void main(String[] args) {
        if (args.length < 2) {  // Ensure correct number of arguments are provided
            System.out.println("Usage: GETClient <server-address> <server-port> [station-id]");
            return;
        }

        // Parse the server address and validate the port number
        String serverAddress = args[0];
        int serverPort;
        try {
            serverPort = Integer.parseInt(args[1]);  // Parse the port number
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid port number. Please provide a valid integer for the port.");
            return;  // Exit if the port number is invalid
        }

        String stationId = (args.length == 3) ? args[2] : null;  // Optional station ID

        try {
            // Build the URL for the GET request
            StringBuilder urlBuilder = new StringBuilder("http://").append(serverAddress).append(":").append(serverPort).append("/weather");
            if (stationId != null) {
                urlBuilder.append("/").append(stationId);  // Append station ID to the URL if provided
            }

            // Convert the string URL to a URL object
            URL url = new URL(urlBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");  // Set the request method to GET

            // Send the request and get the response code
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response from the server
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);  // Append each line of the response
                }
                in.close();

                // Process the JSON response from the server
                processJsonResponse(response.toString());
            } else {
                System.out.println("Error: HTTP response code " + responseCode);
            }

        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }

    // Method to process and display the JSON response from the server
    private static void processJsonResponse(String jsonResponse) {
        Gson gson = new Gson();
        JsonArray weatherArray = gson.fromJson(jsonResponse, JsonArray.class);  // Parse the JSON array from the response

        if (weatherArray == null || weatherArray.size() == 0) {
            System.out.println("No weather data available.");
            return;
        }

        // Loop through each weather entry in the JSON array and display the data
        for (JsonElement element : weatherArray) {
            JsonObject weatherObject = element.getAsJsonObject();
            
            // Handle potential missing fields with null checks
            System.out.println("Station ID: " + (weatherObject.has("id") ? weatherObject.get("id").getAsString() : "N/A"));
            System.out.println("Name: " + (weatherObject.has("name") ? weatherObject.get("name").getAsString() : "N/A"));
            System.out.println("State: " + (weatherObject.has("state") ? weatherObject.get("state").getAsString() : "N/A"));
            System.out.println("Time Zone: " + (weatherObject.has("time_zone") ? weatherObject.get("time_zone").getAsString() : "N/A"));
            System.out.println("Latitude: " + (weatherObject.has("lat") ? weatherObject.get("lat").getAsDouble() : "N/A"));
            System.out.println("Longitude: " + (weatherObject.has("lon") ? weatherObject.get("lon").getAsDouble() : "N/A"));
            System.out.println("Local DateTime: " + (weatherObject.has("local_date_time") ? weatherObject.get("local_date_time").getAsString() : "N/A"));
            System.out.println("Full DateTime: " + (weatherObject.has("local_date_time_full") ? weatherObject.get("local_date_time_full").getAsString() : "N/A"));
            System.out.println("Air Temperature: " + (weatherObject.has("air_temp") ? weatherObject.get("air_temp").getAsDouble() : "N/A"));
            System.out.println("Apparent Temperature: " + (weatherObject.has("apparent_t") ? weatherObject.get("apparent_t").getAsDouble() : "N/A"));
            System.out.println("Cloud: " + (weatherObject.has("cloud") ? weatherObject.get("cloud").getAsString() : "N/A"));
            System.out.println("Dew Point: " + (weatherObject.has("dewpt") ? weatherObject.get("dewpt").getAsDouble() : "N/A"));
            System.out.println("Pressure: " + (weatherObject.has("press") ? weatherObject.get("press").getAsDouble() : "N/A"));
            System.out.println("Relative Humidity: " + (weatherObject.has("rel_hum") ? weatherObject.get("rel_hum").getAsInt() : "N/A"));
            System.out.println("Wind Direction: " + (weatherObject.has("wind_dir") ? weatherObject.get("wind_dir").getAsString() : "N/A"));
            System.out.println("Wind Speed (km/h): " + (weatherObject.has("wind_spd_kmh") ? weatherObject.get("wind_spd_kmh").getAsInt() : "N/A"));
            System.out.println("Wind Speed (kt): " + (weatherObject.has("wind_spd_kt") ? weatherObject.get("wind_spd_kt").getAsInt() : "N/A"));
            System.out.println("--------------------------------------------------");
        }
    }
}
