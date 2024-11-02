import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

/**
 * This class is responsible for sending the GET request to the server,
 * retrieving weather data for a specified station, and maintaining
 * synchronization using a Lamport clock.
 */
public class GETClient {
    private static final LamportClock lamportClock = new LamportClock();

    /**
     * Initiates the GETClient.
     * Requires the server address (host:port) and an optional station ID.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: java GETClient <server-url:port> [stationId]");
            System.exit(1);
        }

        String serverUrl = args[0];
        String stationId = (args.length == 2) ? args[1] : null;

        try {
            URL url = new URL(serverUrl.startsWith("http://") ? serverUrl : "http://" + serverUrl);
            String host = url.getHost();
            int port = (url.getPort() != -1) ? url.getPort() : 80;

            try (Socket socket = new Socket(host, port)) {
                socket.setSoTimeout(5000); // Set a 5-second timeout for the socket

                try (DataOutputStream outputData = new DataOutputStream(socket.getOutputStream());
                     BufferedReader inputReader = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()))) {

                    sendGetRequest(outputData, host, port, stationId, lamportClock);
                    processServerResponse(inputReader);
                }
            }

        } catch (MalformedURLException e) {
            System.out.println("Invalid server URL format: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error connecting to the server: " + e.getMessage());
        }
    }

    /**
     * Constructs and sends the GET request to the server.
     *
     * @param host        The server host
     * @param port        The server port
     * @param stationId   The optional station ID
     * @param lamportClock The Lamport clock for synchronization
     * @return 
     * @throws IOException If an I/O error occurs while sending the request
     */
    public static boolean sendGetRequest(String host, int port, String stationId, LamportClock lamportClock) throws IOException {
        GETClient.lamportClock.increment();


        StringBuilder requestBuilder = new StringBuilder("GET /weather");
        if (stationId != null) {
            requestBuilder.append("?stationId=").append(stationId);
        }
        requestBuilder.append(" HTTP/1.1\r\n")
                .append("Host: ").append(host).append(":").append(port).append("\r\n");

        outputData.writeUTF(requestBuilder.toString());
        outputData.flush();

        System.out.println("Request sent:\n" + requestBuilder.toString());
    }

    /**
     * Processes the server's response to the GET request.
     *
     * @param inputReader The input reader to read the server's response.
     * @throws IOException If an I/O error occurs while reading the response.
     */
    public static void processServerResponse(BufferedReader inputReader) throws IOException {
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        boolean isBody = false;
        String statusLine = null;

        while ((line = inputReader.readLine()) != null) {
            if (line.startsWith("HTTP/")) {
                statusLine = line; // Capture the status line
            }
            if (line.isEmpty()) {
                isBody = true; // The body of the response starts after an empty line
                continue;
            }
            if (isBody) {
                responseBuilder.append(line);
            }
        }

        // Check for a 200 OK response
        if (statusLine != null && !statusLine.contains("200 OK")) {
            System.err.println("Error: Server responded with status - " + statusLine);
            return; // Exit if the response is not 200 OK
        }

        // Parse the full response
        String jsonResponse = responseBuilder.toString();

        // Print the full response for debugging
        System.out.println("Full Server Response: " + jsonResponse);

        // Check for multiple JSON objects
        int startIndex = jsonResponse.indexOf("{");
        int endIndex = jsonResponse.lastIndexOf("}");

        // Extract the JSON object if found
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            String validJson = jsonResponse.substring(startIndex, endIndex + 1);
            System.out.println("Valid JSON Response: " + validJson); // Print the valid JSON for debugging

            try {
                JSONObject jsonObject = new JSONObject(validJson);

                // Constructing the output
                System.out.println("Weather Data:");
                System.out.println("id: '" + jsonObject.getString("id") + "'");
                System.out.println("name: '" + jsonObject.getString("name") + "'");
                System.out.println("state: '" + jsonObject.getString("state") + "'");
                System.out.println("time_zone: '" + jsonObject.getString("time_zone") + "'");
                System.out.println("lat: " + jsonObject.getDouble("lat"));
                System.out.println("lon: " + jsonObject.getDouble("lon"));
                System.out.println("local_date_time: '" + jsonObject.getString("local_date_time") + "'");
                System.out.println("local_date_time_full: '" + jsonObject.getString("local_date_time_full") + "'");
                System.out.println("air_temp: " + jsonObject.getDouble("air_temp"));
                System.out.println("apparent_t: " + jsonObject.getDouble("apparent_t"));
                System.out.println("cloud: '" + jsonObject.getString("cloud") + "'");
                System.out.println("dewpt: " + jsonObject.getDouble("dewpt"));
                System.out.println("press: " + jsonObject.getDouble("press"));
                System.out.println("rel_hum: " + jsonObject.getDouble("rel_hum"));
                System.out.println("wind_dir: '" + jsonObject.getString("wind_dir") + "'");
                System.out.println("wind_spd_kmh: " + jsonObject.getDouble("wind_spd_kmh"));
                System.out.println("wind_spd_kt: " + jsonObject.getDouble("wind_spd_kt"));
            } catch (Exception e) {
                System.err.println("Error parsing JSON: " + e.getMessage());
            }
        } else {
            System.err.println("Invalid JSON response received.");
        }
    }
   
      

}
