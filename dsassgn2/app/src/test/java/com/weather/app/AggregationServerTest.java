package com.weather.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationServerTest {

    private AggregationServer server;
    private static final String TEST_DATA_FILE = "weatherData.json";

    @BeforeEach
    public void setUp() {
        server = new AggregationServer(4567);
        File testDataFile = new File(TEST_DATA_FILE);
        if (testDataFile.exists()) {
            testDataFile.delete();
        }
    }

    @AfterEach
    public void tearDown() {
        File testDataFile = new File(TEST_DATA_FILE);
        if (testDataFile.exists()) {
            testDataFile.delete();
        }
    }

    // Test saving and loading weather data
    @Test
    public void testSaveAndLoadDataToJson() {
        WeatherStationData stationData = new WeatherStationData();
        stationData.updateData(Map.of("id", "1", "temperature", "25"));
        server.getWeatherDataMap().put("1", stationData);

        server.saveDataToFile();
        AggregationServer newServer = new AggregationServer(4567);
        newServer.loadDataFromFile();

        ConcurrentHashMap<String, WeatherStationData> loadedData = newServer.getWeatherDataMap();
        assertNotNull(loadedData.get("1"), "Weather data for station '1' should be loaded.");
        assertTrue(loadedData.get("1").toJson().contains("\"temperature\":\"25\""), "Temperature for station '1' should be '25'.");
    }

    // Test handling valid PUT requests
    @Test
    public void testHandleValidPutRequest() throws IOException {
        String putRequest = "PUT / HTTP/1.1\r\n"
                + "Content-Length: 49\r\n"
                + "Lamport-Clock: 1\r\n"
                + "\r\n"
                + "{\"id\":\"1\",\"temperature\":\"25\"}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(putRequest.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Socket mockSocket = createMockSocket(inputStream, outputStream);
        AggregationServer.ServerHandler handler = new AggregationServer.ServerHandler(mockSocket, server);
        handler.run();

        assertNotNull(server.getWeatherDataMap().get("1"), "Weather data for station '1' should be present.");
        assertTrue(server.getWeatherDataMap().get("1").toJson().contains("\"temperature\":\"25\""), "Temperature for station '1' should be '25'.");

        String response = outputStream.toString();
        assertTrue(response.contains("HTTP/1.1 201 Created"), "Response should contain '201 Created'.");
    }

    // Test handling PUT requests with missing ID
    @Test
    public void testHandlePutRequestMissingId() throws IOException {
        String putRequest = "PUT / HTTP/1.1\r\n"
                + "Content-Length: 49\r\n"
                + "Lamport-Clock: 1\r\n"
                + "\r\n"
                + "{\"temperature\":\"25\"}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(putRequest.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Socket mockSocket = createMockSocket(inputStream, outputStream);
        AggregationServer.ServerHandler handler = new AggregationServer.ServerHandler(mockSocket, server);
        handler.run();

        String response = outputStream.toString();
        assertTrue(response.contains("HTTP/1.1 400 Bad Request"), "Response should contain '400 Bad Request'.");
    }

    // Test handling PUT requests with invalid JSON
    @Test
    public void testHandlePutRequestInvalidJson() throws IOException {
        String putRequest = "PUT / HTTP/1.1\r\n"
                + "Content-Length: 37\r\n"
                + "Lamport-Clock: 1\r\n"
                + "\r\n"
                + "{invalid json}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(putRequest.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Socket mockSocket = createMockSocket(inputStream, outputStream);
        AggregationServer.ServerHandler handler = new AggregationServer.ServerHandler(mockSocket, server);
        handler.run();

        String response = outputStream.toString();
        assertTrue(response.contains("HTTP/1.1 500 Internal Server Error"), "Response should contain '500 Internal Server Error'.");
    }

    // Test handling valid GET requests
    @Test
    public void testHandleValidGetRequest() throws IOException {
        WeatherStationData stationData = new WeatherStationData();
        stationData.updateData(Map.of("id", "1", "temperature", "25"));
        server.getWeatherDataMap().put("1", stationData);

        String getRequest = "GET / HTTP/1.1\r\n"
                + "Lamport-Clock: 1\r\n"
                + "\r\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(getRequest.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Socket mockSocket = createMockSocket(inputStream, outputStream);
        AggregationServer.ServerHandler handler = new AggregationServer.ServerHandler(mockSocket, server);
        handler.run();

        String response = outputStream.toString();
        assertTrue(response.contains("\"id\":\"1\""), "Response should contain station '1' ID.");
        assertTrue(response.contains("\"temperature\":\"25\""), "Response should contain the temperature '25'.");
    }

  

    // Test that the Lamport clock is updated correctly
    @Test
    public void testLamportClockUpdate() {
        assertEquals(1, server.incrementLamportClock(), "Lamport clock should be incremented to 1.");
        assertEquals(2, server.incrementLamportClock(), "Lamport clock should be incremented to 2.");
        
        int newClock = server.updateLamportClock(3);
        assertEquals(4, newClock, "Lamport clock should be updated to 4.");
    }

    // Utility method to create a mock socket for testing purposes
    private Socket createMockSocket(InputStream inputStream, OutputStream outputStream) {
        return new Socket() {
            @Override
            public InputStream getInputStream() {
                return inputStream;
            }

            @Override
            public OutputStream getOutputStream() {
                return outputStream;
            }
        };
    }
}
