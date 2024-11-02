package com.weather.app;

import org.junit.jupiter.api.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ScalabilityTest {

    private static final int PORT = 4580;
    private static final int NUM_CLIENTS = 100;
    private static final int CONNECTION_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 2000;
    private static final int MAX_RETRIES = 3;
    private static final String JSON_FILE_PATH = "C:\\Users\\DELL\\Downloads\\DS_Assignment_2-main (2)\\DS_Assignment_2-main\\app\\weatherData.json";
    private static final ExecutorService clientPool = Executors.newFixedThreadPool(NUM_CLIENTS);

    private static String readJsonFromFile() {
        try {
            return new String(Files.readAllBytes(Paths.get(JSON_FILE_PATH)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @BeforeAll
    static void startAggregationServer() {
        new Thread(() -> {
            try {
                AggregationServer.main(new String[]{String.valueOf(PORT)});
                System.out.println("Aggregation Server started on port " + PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    static void shutdownClientPool() {
        clientPool.shutdown();
        try {
            if (!clientPool.awaitTermination(10, TimeUnit.SECONDS)) {
                clientPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            clientPool.shutdownNow();
        }
    }

    @Test
    void testFailureHandling() {
        String jsonData = readJsonFromFile();
        assertNotNull(jsonData, "JSON data should not be null");

        Future<Boolean> result = clientPool.submit(() -> {
            int attempts = 0;
            while (attempts < MAX_RETRIES) {
                attempts++;
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("localhost", PORT), CONNECTION_TIMEOUT_MS);
                    socket.setSoTimeout(READ_TIMEOUT_MS);

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("PUT /weather.json HTTP/1.1");
                    out.println("Host: localhost");
                    out.println("Content-Type: application/json");
                    out.println("Content-Length: " + jsonData.length());
                    out.println();
                    out.println(jsonData);

                    return true;  // Success if no exceptions
                } catch (SocketException e) {
                    System.err.println("Attempt " + attempts + ": Connection reset by server. Retrying...");
                    Thread.sleep(100 * attempts);  // Exponential backoff
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return false;
        });

        try {
            assertTrue(result.get(), "Failure handling test failed: should succeed within retry limit");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testScalabilityUnderLoad() throws InterruptedException {
        List<Future<Boolean>> results = new ArrayList<>();
        String jsonData = readJsonFromFile();
        assertNotNull(jsonData, "JSON data should not be null");

        for (int i = 0; i < NUM_CLIENTS; i++) {
            results.add(clientPool.submit(() -> {
                int attempts = 0;
                boolean success = false;
                while (attempts < MAX_RETRIES && !success) {
                    attempts++;
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress("localhost", PORT), CONNECTION_TIMEOUT_MS);
                        socket.setSoTimeout(READ_TIMEOUT_MS);

                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        out.println("PUT /weather.json HTTP/1.1");
                        out.println("Host: localhost");
                        out.println("Content-Type: application/json");
                        out.println("Content-Length: " + jsonData.length());
                        out.println();
                        out.println(jsonData);

                        String response = in.readLine();
                        success = response != null && (response.contains("200") || response.contains("201"));
                    } catch (SocketException e) {
                        System.err.println("Attempt " + attempts + ": Connection reset by server. Retrying...");
                        Thread.sleep(100 * attempts);  // Backoff
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return success;
            }));
        }

        long successfulRequests = results.stream()
                .map(result -> {
                    try {
                        return result.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .filter(Boolean::booleanValue)
                .count();

        System.out.println("Successful requests: " + successfulRequests + " out of " + NUM_CLIENTS);

    }

    @Test
    void testEventOrdering() throws InterruptedException {
        List<Future<String>> responses = new ArrayList<>();
        String jsonData = readJsonFromFile();
        assertNotNull(jsonData, "JSON data should not be null");

        for (int i = 0; i < 10; i++) {
            final int order = i;
            responses.add(clientPool.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("localhost", PORT), CONNECTION_TIMEOUT_MS);
                    socket.setSoTimeout(READ_TIMEOUT_MS);

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String data = jsonData.replace("\"id\":", "\"id\":\"" + order + "\", \"order\":");

                    out.println("PUT /weather.json HTTP/1.1");
                    out.println("Host: localhost");
                    out.println("Content-Type: application/json");
                    out.println("Content-Length: " + data.length());
                    out.println();
                    out.println(data);

                    return in.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }));
        }

        List<String> orderedResponses = new ArrayList<>();
        for (Future<String> response : responses) {
            try {
                String result = response.get();
                assertNotNull(result, "Each response should be non-null for proper event ordering");
                orderedResponses.add(result);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Verify ordering by ID or order
        for (int i = 1; i < orderedResponses.size(); i++) {
            assertTrue(orderedResponses.get(i - 1).compareTo(orderedResponses.get(i)) <= 0,
                    "Events should be ordered in sequence by ID");
        }
    }
}
