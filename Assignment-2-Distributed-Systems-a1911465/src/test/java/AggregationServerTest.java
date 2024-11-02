import org.json.JSONException;
import org.json.JSONObject;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

/**
 * The AggregationServerTest class contains unit tests for the AggregationServer functionality.
 * It tests the processing of weather data and the storage of that data in files.
 */
public class AggregationServerTest {

    private static final String TEST_SERVER_ID = "TestServer";
    private static final String TEST_DATA_DIRECTORY = "data";

    /**
     * Sets up the test environment before each test is run.
     * This method creates a data directory for storing test data files.
     */
    @Before
    public void setUp() {
        File dataDirectory = new File(TEST_DATA_DIRECTORY);
        if (!dataDirectory.exists()) {
            dataDirectory.mkdir();
        }
    }

    /**
     * Tests the processWeatherData method of the AggregationServer.
     * It simulates a PUT request with valid weather data and verifies
     * that the data is processed correctly.
     */
    @Test
    public void testProcessWeatherDataValid() {
        String message = "PUT /weather.json HTTP/1.1\r\n" +
                "Content-Type: application/json\r\n" +
                "Lamport-Clock: 1\r\n\r\n" +
                "{\"id\":\"IDS60901\",\"name\":\"Adelaide (West Terrace / ngayirdapira)\",\"state\":\"SA\",\"time_zone\":\"CST\",\"lat\":-34.9,\"lon\":138.6,\"local_date_time\":\"15/04:00pm\",\"local_date_time_full\":\"20230715160000\",\"air_temp\":13.3,\"apparent_t\":9.5,\"cloud\":\"Partly cloudy\",\"dewpt\":5.7,\"press\":1023.9,\"rel_hum\":60,\"wind_dir\":\"S\",\"wind_spd_kmh\":15,\"wind_spd_kt\":8}";

        StringBuilder jsonDataBuilder = new StringBuilder();
        boolean result = AggregationServer.processWeatherData(message, TEST_SERVER_ID, jsonDataBuilder);

        assertTrue(result);
        try {
            JSONAssert.assertEquals("{\"id\":\"IDS60901\",\"name\":\"Adelaide (West Terrace / ngayirdapira)\",\"state\":\"SA\",\"air_temp\":13.3}", jsonDataBuilder.toString(), false);
        } catch (JSONException e) {
            fail("JSON comparison failed: " + e.getMessage());
        }
    }

    /**
     * Tests the processWeatherData method with invalid JSON data.
     * It ensures that invalid input does not get processed.
     */
    @Test
    public void testProcessWeatherDataInvalidJson() {
        String message = "PUT /weather.json HTTP/1.1\r\n" +
                "Content-Type: application/json\r\n" +
                "Lamport-Clock: 1\r\n\r\n" +
                "{\"invalid_json";

        StringBuilder jsonDataBuilder = new StringBuilder();
        boolean result = AggregationServer.processWeatherData(message, TEST_SERVER_ID, jsonDataBuilder);

        assertFalse(result); // Should return false for invalid JSON
    }

    /**
     * Tests the storeData method of the AggregationServer.
     * It verifies that the weather data can be successfully stored in a file.
     *
     * @throws IOException If an I/O error occurs during file operations.
     */
    @Test
    public void testStoreData() throws IOException {
        String jsonData = "{\"id\":\"123\",\"name\":\"TestCity\"}";

        AggregationServer.storeData(jsonData, TEST_SERVER_ID);

        File[] files = new File(TEST_DATA_DIRECTORY).listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0);

        // Clean up: delete the test file after checking
        for (File file : files) {
            if (file.getName().equals(TEST_SERVER_ID + ".json")) {
                file.delete();
            }
        }
    }



    /**
     * Tests that the loadData method correctly loads existing weather data.
     *
     * @throws IOException If an I/O error occurs during file operations.
     */
    @Test
    public void testLoadData() throws IOException {
        String jsonData = "{\"id\":\"456\",\"name\":\"TestCity2\"}";

        AggregationServer.storeData(jsonData, TEST_SERVER_ID);
        AggregationServer.loadData(TEST_SERVER_ID); // Should load the data

        // Add assertions or checks based on the expected behavior after loading
        // (e.g., checking if the data is correctly loaded into memory)
    }

    /**
     * Tests cleanup of stale data from the AggregationServer.
     */
    @Test
    public void testServerStateManagement() {
        try {
            // Simulating a connection
            Socket mockSocket = new Socket("localhost", 4567);
            AggregationServer.serverIds.put(mockSocket, TEST_SERVER_ID); // Manually add the socket to the server state

            assertTrue(AggregationServer.serverIds.containsKey(mockSocket)); // Verify that the server state tracks the socket correctly

            // Cleanup
            mockSocket.close();
            AggregationServer.serverIds.remove(mockSocket); // Remove mock socket after test
        } catch (IOException e) {
            fail("Exception during socket management: " + e.getMessage());
        }
    }

    /**
     * Tests cleanup of stale data by checking that old data is removed after a certain period.
     */
    @Test
    public void testCleanupStaleData() {
        try {
            // Simulating a stale connection by adding it to the server
            Socket mockSocket = new Socket("localhost", 4567);
            AggregationServer.serverLastActiveTime.put(mockSocket, System.currentTimeMillis() - 31000); // Simulate timeout (30 seconds + 1 ms)

            // Run cleanup logic
            AggregationServer.cleanupStaleData(); // Invoke the cleanup method

            // Verify that the stale connection is removed
            assertFalse(AggregationServer.serverIds.containsKey(mockSocket)); // The socket should no longer be tracked
            mockSocket.close(); // Cleanup
        } catch (IOException e) {
            fail("Exception during stale data cleanup: " + e.getMessage());
        }
    }

    /**
     * Tests the proper handling of a timeout scenario.
     */
    @Test
    public void testTimeoutHandling() {
        try {
            // Simulate connection with a mock socket
            Socket mockSocket = new Socket("localhost", 4567);
            AggregationServer.serverLastActiveTime.put(mockSocket, System.currentTimeMillis() - 31000); // Simulate timeout

            // Simulate a client connection being open and check the timeout handling
            AggregationServer.manageConnections(); // Call the connection manager

            // Verify that the mock socket was closed due to timeout
            assertFalse(AggregationServer.serverIds.containsKey(mockSocket)); // The socket should have been removed from the server's state
        } catch (IOException e) {
            fail("Exception during timeout handling: " + e.getMessage());
        }
    }



    /**
     * Tests the server's response code handling for invalid requests.
     *
     * @throws IOException If an I/O error occurs during file operations.
     */
    @Test
    public void testInvalidRequestResponseHandling() throws IOException {
        String message = "INVALID /weather HTTP/1.1\r\n" +
                "Host: localhost\r\n\r\n"; // Invalid request

        // Simulate the server processing this request
        assertFalse(AggregationServer.processRequest(message)); // Replace with actual method to test
    }
}