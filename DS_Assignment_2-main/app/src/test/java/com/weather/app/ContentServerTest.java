package com.weather.app;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class ContentServerTest {

    private static Thread serverThread;

    @BeforeAll
    static void setUpAggregationServer() {
        serverThread = new Thread(() -> {
            try {
                AggregationServer.main(new String[] { "4570" });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        try {
            Thread.sleep(2000);  // Wait for server to start
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void shouldSendWeatherDataSuccessfully() throws IOException {
        // Test case to send weather data from ContentServer
        String[] args = { "localhost", "4570", "testWeatherData.txt" };
        ContentServer.main(args);

        // Verify if data is received by AggregationServer
        try (Socket socket = new Socket("localhost", 4570);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET /weather.json HTTP/1.1");
            out.println("Host: localhost");
            out.println("Lamport-Clock: 5");
            out.println();
            String response = in.readLine();
            assertTrue(response.contains("200"));
        }
    }

    @Test
    void shouldHandleInvalidWeatherFile() throws IOException {
        // Prepare an invalid weather file
        try (PrintWriter writer = new PrintWriter(new FileWriter("invalidData.txt"))) {
            writer.println("Invalid data format");
        }

        String[] args = { "localhost", "4570", "invalidData.txt" };
        ContentServer.main(args);

        // Ensure invalid data wasn't sent
        try (Socket socket = new Socket("localhost", 4570);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET /weather.json HTTP/1.1");
            out.println("Host: localhost");
            out.println();
            String response = in.readLine();
            assertFalse(response.contains("Invalid"));
        }
    }

    @Test
    void shouldConvertFileDataToJsonCorrectly() throws IOException {
        // Test file-to-JSON conversion
        String testData = "id:001\nname:TestCity";
        File tempFile = File.createTempFile("weatherTest", ".txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(testData);
        }

        String jsonData = ContentServer.readWeatherDataFromFile(tempFile.getAbsolutePath());
        assertNotNull(jsonData);
        assertTrue(jsonData.contains("\"id\": \"001\""));
    }
}
