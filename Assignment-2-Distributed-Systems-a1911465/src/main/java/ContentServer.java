import java.io.*;
import java.net.Socket;

/**
 * The ContentServer is responsible for reading weather data from a specified file,
 * converting it into JSON format, and sending it to the Aggregation Server.
 * It also utilizes a Lamport clock for synchronization in distributed systems.
 */
public class ContentServer {
    private static final LamportClock lamportClock = new LamportClock();

    /**
     * Main entry point for the Content Server.
     *
     * @param args Command line arguments containing server URL and file path.
     */
    public static void main(String[] args) {
        // Check if the correct number of arguments is provided
        if (args.length != 2) {
            System.out.println("Usage: java ContentServer <server-url:port> <file-path>");
            return;
        }

        String serverUrl = args[0]; // Server URL and port
        String feedFilePath = args[1]; // Path to the weather data file

        // Attempt to send weather data to the server
        if (!sendWeatherData(serverUrl, feedFilePath)) {
            System.out.println("Failed to send data after retries.");
        }
    }

    /**
     * Sends the weather data to the Aggregation Server.
     *
     * @param serverUrl    The URL of the Aggregation Server.
     * @param feedFilePath The path to the weather data file.
     * @return true if the data was successfully sent, false otherwise.
     */
    public static boolean sendWeatherData(String serverUrl, String feedFilePath) {
        int maxRetries = 3; // Maximum number of retries for sending data

        // Retry sending data if it fails
        while (maxRetries > 0) {
            try (Socket socket = createSocket(serverUrl)) {
                int currentClockValue = lamportClock.getValue(); // Get current Lamport clock value
                boolean success = sendWeatherToServer(socket, feedFilePath, currentClockValue); // Send data

                if (success) {
                    return true; // Data sent successfully
                }

                System.out.println("Failed to send data. Retrying... " + (--maxRetries) + " retries left.");
                Thread.sleep(5000); // Wait before retrying

            } catch (IOException | InterruptedException e) {
                e.printStackTrace(); // Log any exceptions encountered
            }
        }
        return false; // Data sending failed after retries
    }

    /**
     * Sends weather data to the Aggregation Server via a socket.
     *
     * @param socket         The socket connection to the server.
     * @param feedFilePath   The path to the weather data file.
     * @param lamportClockValue The current Lamport clock value.
     * @return true if the data was sent successfully, false otherwise.
     * @throws IOException If an I/O error occurs during communication.
     */
    public static boolean sendWeatherToServer(Socket socket, String feedFilePath, int lamportClockValue) throws IOException {
        DataOutputStream outputData = new DataOutputStream(socket.getOutputStream());
        BufferedReader inputData = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Use BufferedReader for line reading

        String jsonData = convertToJson(feedFilePath); // Convert the data to JSON
        if (jsonData == null) {
            System.out.println("Failed to convert feed to JSON.");
            return false; // JSON conversion failed
        }

        // Log the JSON data being sent
        System.out.println("Sending JSON Data: " + jsonData);

        // Prepare the HTTP PUT request
        String requestData = "PUT /weather.json HTTP/1.1\r\n" +
                "User-Agent: ATOMClient/1/0\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + jsonData.length() + "\r\n\r\n" + // Content length header
                jsonData;

        outputData.writeUTF(requestData); // Send the HTTP request
        outputData.flush(); // Flush the output stream to ensure data is sent

        lamportClock.increment(); // Increment the Lamport clock

        // Read the server response
        StringBuilder serverResponse = new StringBuilder();
        String line;
        while ((line = inputData.readLine()) != null) {
            serverResponse.append(line).append("\n");
            if (line.isEmpty()) {
                break; // Stop reading when we reach the end of the headers
            }
        }

        System.out.println("Server Response: " + serverResponse.toString().trim());

        // Check the HTTP response status
        return serverResponse.toString().contains("HTTP/1.1 200") || serverResponse.toString().contains("HTTP/1.1 201");
    }

    /**
     * Creates a socket connection to the specified server URL.
     *
     * @param serverUrl The server URL in the format "host:port".
     * @return The socket connected to the server.
     * @throws IOException If an error occurs while creating the socket.
     */
    public static Socket createSocket(String serverUrl) throws IOException {
        try {
            // Remove "http://" from the URL if present
            if (serverUrl.startsWith("http://")) {
                serverUrl = serverUrl.substring(7);  // Remove "http://"
            }

            // Split the remaining part of the URL to get the host and port
            String[] urlParts = serverUrl.split(":");
            String host = urlParts[0];  // Host part
            int port = 80; // Default to port 80 if port is not specified

            // Check if port is provided in the URL
            if (urlParts.length > 1) {
                port = Integer.parseInt(urlParts[1]);  // Port part
            }

            // Create and return the socket connection
            return new Socket(host, port);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new IOException("Invalid server URL format. Expected format: <host>:<port>", e);
        }
    }

    /**
     * Converts the weather data from the specified text file into a JSON string.
     *
     * @param feedFilePath The path to the data file.
     * @return A JSON string representation of the weather data, or null if an error occurs.
     */
    public static String convertToJson(String feedFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(feedFilePath))) {
            StringBuilder jsonDataBuilder = new StringBuilder();
            String line;
            boolean firstEntry = true;
            boolean validID = false;

            jsonDataBuilder.append("{"); // Start JSON object

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(": ", 2); // Split into 2 parts
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    if (!firstEntry) {
                        jsonDataBuilder.append(","); // Add comma for subsequent entries
                    }

                    // Handle the key-value pairs
                    if (key.equals("lat") || key.equals("lon") ||
                            key.equals("air_temp") || key.equals("apparent_t") ||
                            key.equals("dewpt") || key.equals("press") ||
                            key.equals("rel_hum") || key.equals("wind_spd_kmh") ||
                            key.equals("wind_spd_kt")) {
                        // Treat as number
                        jsonDataBuilder.append("\"").append(key).append("\":").append(value);
                    } else {
                        // Treat as string
                        jsonDataBuilder.append("\"").append(key).append("\":\"").append(value).append("\"");
                    }

                    // Check for valid ID
                    if (key.equals("id")) {
                        validID = true;
                    }
                    firstEntry = false; // After first entry, subsequent entries will not be first
                }
            }

            jsonDataBuilder.append("}"); // End JSON object

            if (!validID) {
                System.err.println("Error: No 'id' found in the input file.");
                return null;
            }

            return jsonDataBuilder.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public void start() {
        // Simulate server startup
        System.out.println("Content server started");
    }

    public void stop() {
        // Simulate server shutdown
        System.out.println("Content server stopped");
    }

	public boolean sendPutRequest(String serverUrl, int basePort, String sampleData) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean loadDataFromFile(String string) {
		// TODO Auto-generated method stub
		return false;
	}
}
