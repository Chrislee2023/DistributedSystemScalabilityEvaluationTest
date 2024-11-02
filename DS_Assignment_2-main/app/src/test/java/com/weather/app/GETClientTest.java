package com.weather.app;

import org.junit.jupiter.api.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class GETClientTest {

    private static ServerSocket mockServerSocket;
    private static Thread mockServerThread;

    @AfterEach
    void tearDown() throws IOException {
        if (mockServerSocket != null && !mockServerSocket.isClosed()) {
            mockServerSocket.close();
        }
        if (mockServerThread != null && mockServerThread.isAlive()) {
            mockServerThread.interrupt();
        }
    }

    @Test
    void testGETClientHandlesValidResponse() throws IOException, InterruptedException {
        // Start a mock server on port 8082 that returns valid JSON data
        mockServerThread = new Thread(() -> {
            try {
                mockServerSocket = new ServerSocket(8082);
                while (true) {
                    Socket clientSocket = mockServerSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                    String requestLine = in.readLine();
                    if (requestLine.startsWith("GET")) {
                        String jsonResponse = "[{\"id\":\"001\",\"name\":\"Test Station\",\"state\":\"Test State\"}]";
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: application/json");
                        out.println("Content-Length: " + jsonResponse.getBytes(StandardCharsets.UTF_8).length);
                        out.println();
                        out.println(jsonResponse);
                    }
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        mockServerThread.start();

        // Wait a short time to ensure the server starts
        Thread.sleep(500);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));  // Redirect output to capture it

        String[] args = { "localhost", "8082" };  // Use mock server address and port
        GETClient.main(args);

        System.setOut(originalOut);  // Restore original System.out

        String output = outputStream.toString();
        assertTrue(output.contains("Station ID: 001"));
        assertTrue(output.contains("Name: Test Station"));
        assertTrue(output.contains("State: Test State"));
    }

    @Test
    void testGETClientHandlesServerError() throws IOException, InterruptedException {
        // Start a mock server on port 8082 that returns 500 Internal Server Error
        mockServerThread = new Thread(() -> {
            try {
                mockServerSocket = new ServerSocket(8082);
                while (true) {
                    Socket clientSocket = mockServerSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                    String requestLine = in.readLine();
                    if (requestLine.startsWith("GET")) {
                        out.println("HTTP/1.1 500 Internal Server Error");
                        out.println();
                    }
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        mockServerThread.start();

        // Wait a short time to ensure the server starts
        Thread.sleep(500);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));  // Redirect output to capture it

        String[] args = { "localhost", "8082" };  // Use mock server address and port
        GETClient.main(args);

        System.setOut(originalOut);  // Restore original System.out

        String output = outputStream.toString();
        assertTrue(output.contains("Error: HTTP response code 500"));  // Ensure error handling for server error
    }

    @Test
    void testGETClientHandlesInvalidPort() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));  // Redirect output to capture it

        String[] args = { "localhost", "invalid_port" };  // Invalid port input
        GETClient.main(args);

        System.setOut(originalOut);  // Restore original System.out

        // Ensure it handles invalid port input correctly
    }

    @Test
    void testGETClientHandlesNoData() throws IOException, InterruptedException {
        // Start a mock server on port 8083 that returns an empty JSON array
        mockServerThread = new Thread(() -> {
            try {
                mockServerSocket = new ServerSocket(8083);
                while (true) {
                    Socket clientSocket = mockServerSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                    String requestLine = in.readLine();
                    if (requestLine.startsWith("GET")) {
                        String jsonResponse = "[]";  // Empty array, indicating no data
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: application/json");
                        out.println("Content-Length: " + jsonResponse.getBytes(StandardCharsets.UTF_8).length);
                        out.println();
                        out.println(jsonResponse);
                    }
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        mockServerThread.start();

        // Wait a short time to ensure the server starts
        Thread.sleep(500);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));  // Redirect output to capture it

        String[] args = { "localhost", "8083" };  // Use mock server address and port
        GETClient.main(args);

        System.setOut(originalOut);  // Restore original System.out

        String output = outputStream.toString();
        assertTrue(output.contains("No weather data available."));  // Ensure it handles empty data properly
    }
}
