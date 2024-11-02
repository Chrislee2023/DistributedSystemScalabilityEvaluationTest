package com.weather.app;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class AggregationServerTest {

    @BeforeAll
    static void startAggregationServer() {
        new Thread(() -> {
            try {
                AggregationServer.main(new String[] { "4571" });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        try {
            Thread.sleep(2000);  // Wait for server startup
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void shouldProcessPutRequestSuccessfully() throws IOException {
        String jsonData = "{ \"id\": \"001\", \"name\": \"TestCity\", \"state\": \"TestState\" }";

        Socket socket = new Socket("localhost", 4571);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("PUT /weather.json HTTP/1.1");
        out.println("Host: localhost");
        out.println("Content-Type: application/json");
        out.println("Content-Length: " + jsonData.length());
        out.println();
        out.println(jsonData);

        String response = in.readLine();
        assertTrue(response.contains("201") || response.contains("200"));  // Created or OK status

        socket.close();
    }

    @Test
    void shouldHandleInvalidJsonData() throws IOException {
        String invalidJsonData = "{ \"id\": }";  // Invalid JSON

        Socket socket = new Socket("localhost", 4571);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("PUT /weather.json HTTP/1.1");
        out.println("Host: localhost");
        out.println("Content-Type: application/json");
        out.println("Content-Length: " + invalidJsonData.length());
        out.println();
        out.println(invalidJsonData);

        String response = in.readLine();
        assertTrue(response.contains("500"));  // Internal Server Error

        socket.close();
    }
    
    @Test
    void shouldReturnWeatherDataOnGetRequest() throws IOException {
        // Sending a GET request to AggregationServer
        Socket socket = new Socket("localhost", 4571);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("GET /weather.json HTTP/1.1");
        out.println("Host: localhost");
        out.println();
        String response = in.readLine();
        assertTrue(response.contains("200"));  // OK status

        socket.close();
    }
}
