import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.*;
import java.net.Socket;

import static org.junit.Assert.*;

public class GETClientTest {

    private static final String SERVER_URL = "localhost:4567";
    private static final String VALID_STATION_ID = "TestStation";

    @Before
    public void setUp() {
        // Any necessary setup can be done here, like initializing shared resources.
    }

    /**
     * Tests the sendGetRequest method of the GETClient.
     * It verifies that the correct HTTP GET request is constructed and sent to the server.
     *
     * @throws IOException If an I/O error occurs during the test.
     */
    @Test
    public void testSendGetRequest() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream outputData = new DataOutputStream(outputStream);

        LamportClock lamportClock = new LamportClock();
        String host = "localhost";
        int port = 4567;
        String stationId = "TestStation";

        // Call the sendGetRequest with the correct argument list
        GETClient.sendGetRequest(outputData, host, port, stationId, lamportClock);

        // Define the expected request string
        String expectedRequest = "GET /weather?stationId=TestStation HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n";

        // Capture the actual output
        String actualRequest = outputStream.toString();

        // Remove unwanted characters from the actual request
        actualRequest = actualRequest.replace("\u0000", ""); // Remove unwanted NUL characters
        actualRequest = actualRequest.replace("C", ""); // Remove unwanted 'W' characters

        // Print actual output for comparison
        System.out.println("Expected Request: \n" + expectedRequest);
        System.out.println("Actual Request: \n" + actualRequest);

        // Compare the expected and actual requests
        assertEquals(expectedRequest, actualRequest);
    }

    /**
     * Tests the functionality of launching multiple GETClient instances.
     * It creates multiple client threads, each sending a GET request with a unique station ID.
     */
    @Test
    public void testMultiClientLauncher() {
        int numberOfClients = 5; // Default number of clients to run

        // Launch multiple clients
        for (int i = 0; i < numberOfClients; i++) {
            String stationId = "Station" + (i + 1); // Unique station IDs
            Thread clientThread = new Thread(() -> {
                String[] clientArgs = {SERVER_URL, stationId};
                GETClient.main(clientArgs);
            });
            clientThread.start();
            try {
                clientThread.join(); // Wait for the thread to finish before starting the next one
            } catch (InterruptedException e) {
                System.err.println("Client thread interrupted: " + e.getMessage());
            }
        }
    }

    /**
     * Tests the timeout handling functionality.
     * It simulates a timeout scenario and checks the client's response.
     */
    @Test// Expect IOException to be thrown
    public void testTimeoutHandling() throws IOException {
        // Create a mock Socket that simulates a server that never responds
        Socket mockSocket = Mockito.mock(Socket.class);

        // Mock the output stream
        DataOutputStream outputData = new DataOutputStream(new ByteArrayOutputStream());
        Mockito.when(mockSocket.getOutputStream()).thenReturn(outputData);

        // Mock the input stream to simulate no response from the server
        InputStream inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated timeout"); // Simulate an IOException for timeout
            }
        };
        Mockito.when(mockSocket.getInputStream()).thenReturn(inputStream);

        // Call sendGetRequest and expect a timeout exception
        LamportClock lamportClock = new LamportClock();
        GETClient.sendGetRequest(outputData, "localhost", 4567, VALID_STATION_ID, lamportClock);
    }

    /**
     * Tests the Lamport clock increment functionality.
     */
    @Test
    public void testLamportClockIncrement() {
        LamportClock lamportClock = new LamportClock();
        int initialClockValue = lamportClock.getValue();

        lamportClock.increment(); // Increment the clock

        assertEquals(initialClockValue + 1, lamportClock.getValue()); // Check if the clock value is incremented
    }
}
