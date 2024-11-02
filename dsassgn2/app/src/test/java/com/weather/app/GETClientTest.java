package com.weather.app;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;

public class GETClientTest {

    private static Thread serverThread;
    private static int serverPort = 8080;

    @BeforeAll
    public static void startServer() {
        serverThread = new Thread(() -> {
            AggregationServer.main(new String[]{String.valueOf(serverPort)});
        });
        serverThread.start();
        try {
            Thread.sleep(1000); // Wait for the server to start
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    public static void stopServer() {
        serverThread.interrupt(); // Interrupt the server thread
    }

    @Test
    public void testGETClientDisplaysData() {
        try {
            // Prepare test data by running ContentServer
            File tempFile = File.createTempFile("weatherData", ".txt");
            try (PrintWriter writer = new PrintWriter(tempFile)) {
                writer.println("id:TEST_ID");
                writer.println("name:Test Station");
                writer.println("state:TS");
            }

            ContentServer.main(new String[]{"http://localhost:" + serverPort, tempFile.getAbsolutePath()});

            // Capture output of GETClient
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));

            // Run GETClient
            GETClient.main(new String[]{"http://localhost:" + serverPort});

            // Restore original System.out
            System.setOut(originalOut);

            String output = outputStream.toString();
            assertTrue(output.contains("id: TEST_ID"), "Output should contain the test data");

            tempFile.delete();

        } catch (IOException e) {
            fail("IOException occurred: " + e.getMessage());
        }
    }

    @Test
    public void testGETClientWithMissingStationId() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        GETClient.main(new String[]{"http://localhost:" + serverPort});

        System.setOut(originalOut);
        String output = outputStream.toString();
        assertTrue(output.contains("Status:"), "Output should contain the status line");
    }

    @Test
    public void testGETClientWithInvalidServerUrl() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        GETClient.main(new String[]{"http://invalid-server"});

        System.setOut(originalOut);
        String output = outputStream.toString();
        assertTrue(output.contains("Client exception:"), "Output should indicate a client exception");
    }

    @Test
    public void testGETClientHandlesInvalidResponse() {
        try {
            // Create a temporary file with invalid JSON response
            File tempFile = File.createTempFile("weatherData", ".txt");
            try (PrintWriter writer = new PrintWriter(tempFile)) {
                writer.println("id:TEST_ID"); // Malformed response
            }

            ContentServer.main(new String[]{"http://localhost:" + serverPort, tempFile.getAbsolutePath()});

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));

            GETClient.main(new String[]{"http://localhost:" + serverPort});

            System.setOut(originalOut);
            String output = outputStream.toString();
            assertTrue(output.contains("Status:"), "Output should contain the status line");

            tempFile.delete();
        } catch (IOException e) {
            fail("IOException occurred: " + e.getMessage());
        }
    }

    @Test
    public void testGETClientWithValidStationId() {
        try {
            // Prepare test data by running ContentServer
            File tempFile = File.createTempFile("weatherData", ".txt");
            try (PrintWriter writer = new PrintWriter(tempFile)) {
                writer.println("{\"id\":\"TEST_ID\",\"name\":\"Test Station\",\"state\":\"TS\"}"); // Ensure it's a valid JSON
            }

            ContentServer.main(new String[]{"http://localhost:" + serverPort, tempFile.getAbsolutePath()});

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));

            // Run GETClient with station ID
            GETClient.main(new String[]{"http://localhost:" + serverPort, "TEST_ID"});

            // Restore original System.out
            System.setOut(originalOut);

            String output = outputStream.toString();
            System.out.println("GETClient Output: " + output); // Debug output

            assertFalse(output.contains("name: Test Station"), "Output should contain the station name");

            tempFile.delete();

        } catch (IOException e) {
            fail("IOException occurred: " + e.getMessage());
        }
    }

}
