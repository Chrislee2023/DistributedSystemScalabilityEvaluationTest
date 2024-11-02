package com.weather.app;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.sun.net.httpserver.*;
import com.google.gson.*;

public class ContentServerTest {

    private static HttpServer testServer;
    private static int serverPort = 8000;
    private static String receivedData;

    @BeforeAll
    public static void startServer() throws IOException {
        testServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
        testServer.createContext("/", new TestHandler());
        testServer.setExecutor(null); // creates a default executor
        testServer.start();
        System.out.println("Test server started on port " + serverPort);
    }

    @AfterAll
    public static void stopServer() {
        testServer.stop(0);
        System.out.println("Test server stopped.");
    }

    @BeforeEach
    public void resetReceivedData() {
        receivedData = null; // Reset the received data before each test
    }

    @Test
    public void testContentServerSendsValidData() {
        try {
            // Create a temporary file with valid test data
            File tempFile = createTempFileWithData("id:TEST_ID\nname:Test Station\nstate:TS");

            // Run ContentServer
            ContentServer.main(new String[]{"http://localhost:" + serverPort, tempFile.getAbsolutePath()});

            // Wait for the server to process the request
            Thread.sleep(500);

            // Verify that the data received matches expected data
            String expectedData = "{\"id\":\"TEST_ID\",\"name\":\"Test Station\",\"state\":\"TS\"}";
            JsonObject expectedJson = JsonParser.parseString(expectedData).getAsJsonObject();
            JsonObject receivedJson = JsonParser.parseString(receivedData).getAsJsonObject();

            assertEquals(expectedJson, receivedJson, "Server should receive the expected JSON data");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void testContentServerHandlesInvalidData() {
        try {
            // Create a temporary file with invalid data
            File tempFile = createTempFileWithData("invalid data line");

            // Run ContentServer
            ContentServer.main(new String[]{"http://localhost:" + serverPort, tempFile.getAbsolutePath()});

            // Wait for the server to process the request
            Thread.sleep(500);

            // Check if receivedData is still null or empty
            assertNull(receivedData, "Server should not receive any valid data from invalid input.");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void testContentServerHandlesEmptyFile() {
        try {
            // Create a temporary empty file
            File tempFile = File.createTempFile("emptyFile", ".txt");
            tempFile.deleteOnExit(); // Ensure the file is deleted on exit

            // Run ContentServer
            ContentServer.main(new String[]{"http://localhost:" + serverPort, tempFile.getAbsolutePath()});

            // Wait for the server to process the request
            Thread.sleep(500);

            // Check if receivedData is still null or empty
            assertNull(receivedData, "Server should not receive any data from an empty file.");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void testContentServerHandlesFileNotFound() {
        try {
            // Use a non-existing file path
            ContentServer.main(new String[]{"http://localhost:" + serverPort, "non_existing_file.txt"});

            // We expect the method to not throw an uncaught exception; we cannot assert output here
            // Just log that it handled the error gracefully
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void testContentServerHandlesFileNotFound1() {
        try {
            // Attempt to run ContentServer with a non-existing file path
            ContentServer.main(new String[]{"http://localhost:" + serverPort, "non_existing_file.txt"});
            
            // Optionally, check logs or internal state if necessary (not shown here)
        } catch (Exception e) {
            // Log the error or check if the right exception was thrown
            assertTrue(e.getMessage().contains("The system cannot find the file specified"), "Expected file not found exception.");
        }
    }


    // Utility method to create a temporary file with specified data
    private File createTempFileWithData(String data) throws IOException {
        File tempFile = File.createTempFile("weatherData", ".txt");
        tempFile.deleteOnExit(); // Ensure the file is deleted on exit
        try (PrintWriter writer = new PrintWriter(tempFile)) {
            writer.println(data);
        }
        return tempFile;
    }

    // Handler to process incoming requests
    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Read the request body
                InputStream is = exchange.getRequestBody();
                receivedData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                is.close();

                // Send response
                String response = "Data received";
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            } else {
                // Handle other methods
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            }
        }
    }
}
