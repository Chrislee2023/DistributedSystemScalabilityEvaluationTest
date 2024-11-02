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

public class ScalabilityEvaluationTest{

    private static final int PORT = 4580;
    private static final int NUM_CLIENTS = 50;
    private static final int CONNECTION_TIMEOUT_MS = 5000;  // Increased timeout
    private static final int READ_TIMEOUT_MS = 5000;  // Increased timeout
    private static final int MAX_RETRIES = 3;
    private static final String JSON_FILE_PATH = "D:\\COLLEGE\\Univeristy of Adelaide\\SEMESTER 1\\Distributed System\\dsassgn2\\app\\data.txt";
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
            AggregationServer.main(new String[]{String.valueOf(PORT)});
            System.out.println("Aggregation Server started on port " + PORT);
        }).start();
        try {
            Thread.sleep(5000);  // Wait for the server to start
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
    public void testFailureHandling() {
        String jsonData = readJsonFromFile();
        assertNotNull(jsonData, "JSON data should not be null");

        Future<Boolean> result = clientPool.submit(() -> {
            int attempts = 0;
            while (attempts < MAX_RETRIES) {
                attempts++;
                try (Socket socket = new Socket()) {
                    System.out.println("Attempting connection: attempt #" + attempts);
                    socket.connect(new InetSocketAddress("localhost", PORT), CONNECTION_TIMEOUT_MS);
                    socket.setSoTimeout(READ_TIMEOUT_MS);

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    System.out.println("Client attempt " + attempts + " sending data to server.");
                    out.println("PUT /weather.json HTTP/1.1");
                    out.println("Host: localhost");
                    out.println("Content-Type: application/json");
                    out.println("Content-Length: " + jsonData.length());
                    out.println();
                    out.println(jsonData);

                    System.out.println("Client attempt " + attempts + " completed successfully.");
                    return true;  // Success if no exceptions
                } catch (SocketException e) {
                    System.err.println("Attempt " + attempts + ": Connection reset by server. Retrying...");
                    Thread.sleep((long) (100 * attempts * Math.random()));  // Exponential backoff with jitter
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
    public void testScalabilityUnderLoad() throws InterruptedException {
        List<Future<Boolean>> results = new ArrayList<>();
        String jsonData = readJsonFromFile();
        assertNotNull(jsonData, "JSON data should not be null");

        for (int i = 0; i < NUM_CLIENTS; i++) {
            final int clientId = i;
            results.add(clientPool.submit(() -> {
                int attempts = 0;
                boolean success = false;
                while (attempts < MAX_RETRIES && !success) {
                    attempts++;
                    try (Socket socket = new Socket()) {
                        System.out.println("Client " + clientId + " attempt " + attempts + " sending data.");
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
                        System.out.println("Client " + clientId + " received response: " + response);
                    } catch (SocketException e) {
                        System.err.println("Client " + clientId + " attempt " + attempts + ": Connection reset by server. Retrying...");
                        Thread.sleep((long) (100 * attempts * Math.random()));  // Exponential backoff with jitter
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
    public void testEventOrdering() throws InterruptedException {
        List<Future<Integer>> responses = new ArrayList<>();
        String jsonData = readJsonFromFile();
        assertNotNull(jsonData, "JSON data should not be null");

        for (int i = 0; i < 10; i++) {
            final int order = i;
            Future<Integer> result = clientPool.submit(() -> {
                try (Socket socket = new Socket()) {
                    Thread.sleep(order * 100); // Introduce delay based on order

                    socket.connect(new InetSocketAddress("localhost", PORT), CONNECTION_TIMEOUT_MS);
                    socket.setSoTimeout(READ_TIMEOUT_MS);

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String data = jsonData.replace("\"id\":", "\"id\":\"" + order + "\", \"order\":\"" + order + "\",");
                    out.println("PUT /weather.json HTTP/1.1");
                    out.println("Host: localhost");
                    out.println("Content-Type: application/json");
                    out.println("Content-Length: " + data.length());
                    out.println();
                    out.println(data);

                    String serverResponse = in.readLine(); // Renamed to serverResponse
                    System.out.println("Client received response for event with ID " + order + ": " + serverResponse);

                    if (serverResponse != null && serverResponse.contains("\"order\":\"" + order + "\"")) {
                        return order;
                    } else {
                        return -1;
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    return -1;
                }
            });
            responses.add(result);
        }

        // Collect and check the ordered responses
        for (int i = 0; i < responses.size(); i++) {
            try {
                Integer result = responses.get(i).get();
                System.out.println("Expected order: " + i + ", Received order: " + result);
               
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    
}
