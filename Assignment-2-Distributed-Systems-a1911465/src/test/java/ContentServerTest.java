import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ContentServerTest is a test class for testing the functionality of the ContentServer.
 * It includes tests for converting feed data to JSON format and sending weather data to the server.
 */
public class ContentServerTest {

    private static final String WEATHER_DATA_FILE_PATH = "/Users/jyothisjoy/Documents/Distributed Systems/Assignment 2/Assignment-2-Distributed-Systems-a1911465/weather_data.txt";

    /**
     * Sets up the test environment by creating a temporary feed file with sample data.
     *
     * @throws IOException if an I/O error occurs while creating the file.
     */
    @Before
    public void setUp() throws IOException {
        // Ensure the weather data file exists or create it
        File tempFile = new File(WEATHER_DATA_FILE_PATH);
        if (!tempFile.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write("id: IDS60901\n");
                writer.write("name: Adelaide (West Terrace / ngayirdapira)\n");
                writer.write("state: SA\n");
                writer.write("time_zone: CST\n");
                writer.write("lat: -34.9\n");
                writer.write("lon: 138.6\n");
                writer.write("local_date_time: 15/04:00pm\n");
                writer.write("local_date_time_full: 20230715160000\n");
                writer.write("air_temp: 13.3\n");
                writer.write("apparent_t: 9.5\n");
                writer.write("cloud: Partly cloudy\n");
                writer.write("dewpt: 5.7\n");
                writer.write("press: 1023.9\n");
                writer.write("rel_hum: 60\n");
                writer.write("wind_dir: S\n");
                writer.write("wind_spd_kmh: 15\n");
                writer.write("wind_spd_kt: 8\n");
            }
        }
    }

    /**
     * Tests the convertToJson method of the ContentServer class.
     * It verifies that the JSON output contains the expected keys and values.
     */
    @Test
    public void testConvertToJson() {
        String jsonData = ContentServer.convertToJson(WEATHER_DATA_FILE_PATH);
        assertNotNull("JSON data should not be null", jsonData);

        // Check if all expected keys are present in the JSON data
        assertTrue("JSON should contain id", jsonData.contains("\"id\":\"IDS60901\""));
        assertTrue("JSON should contain name", jsonData.contains("\"name\":\"Adelaide (West Terrace / ngayirdapira)\""));
        assertTrue("JSON should contain state", jsonData.contains("\"state\":\"SA\""));
        assertTrue("JSON should contain time_zone", jsonData.contains("\"time_zone\":\"CST\""));
        assertTrue("JSON should contain lat", jsonData.contains("\"lat\":-34.9"));
        assertTrue("JSON should contain lon", jsonData.contains("\"lon\":138.6"));
        assertTrue("JSON should contain local_date_time", jsonData.contains("\"local_date_time\":\"15/04:00pm\""));
        assertTrue("JSON should contain local_date_time_full", jsonData.contains("\"local_date_time_full\":\"20230715160000\""));
        assertTrue("JSON should contain air_temp", jsonData.contains("\"air_temp\":13.3"));
        assertTrue("JSON should contain apparent_t", jsonData.contains("\"apparent_t\":9.5"));
        assertTrue("JSON should contain cloud", jsonData.contains("\"cloud\":\"Partly cloudy\""));
        assertTrue("JSON should contain dewpt", jsonData.contains("\"dewpt\":5.7"));
        assertTrue("JSON should contain press", jsonData.contains("\"press\":1023.9"));
        assertTrue("JSON should contain rel_hum", jsonData.contains("\"rel_hum\":60"));
        assertTrue("JSON should contain wind_dir", jsonData.contains("\"wind_dir\":\"S\""));
        assertTrue("JSON should contain wind_spd_kmh", jsonData.contains("\"wind_spd_kmh\":15"));
        assertTrue("JSON should contain wind_spd_kt", jsonData.contains("\"wind_spd_kt\":8"));
    }

    /**
     * Tests the sendWeatherToServer method of the ContentServer class.
     * It mocks a socket connection and simulates sending weather data to the server,
     * then asserts that the data was sent successfully.
     *
     * @throws IOException if an I/O error occurs while sending data.
     */
    @Test
    public void testSendWeatherDataSuccess() throws IOException {
        // Mock socket and simulate server response
        Socket mockSocket = Mockito.mock(Socket.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayInputStream inputStream = new ByteArrayInputStream("HTTP/1.1 200 OK\r\n\r\n".getBytes());

        Mockito.when(mockSocket.getOutputStream()).thenReturn(outputStream);
        Mockito.when(mockSocket.getInputStream()).thenReturn(inputStream);

        boolean result = ContentServer.sendWeatherToServer(mockSocket, WEATHER_DATA_FILE_PATH, 1);
        assertTrue("Should successfully send weather data", result);
    }

    /**
     * Tests the sendWeatherToServer method when the server responds with a 500 status code.
     * It verifies that the method returns false for an unsuccessful response.
     *
     * @throws IOException if an I/O error occurs while sending data.
     */
    @Test
    public void testSendWeatherDataServerError() throws IOException {
        // Mock socket and simulate server response
        Socket mockSocket = Mockito.mock(Socket.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayInputStream inputStream = new ByteArrayInputStream("HTTP/1.1 500 Internal Server Error\r\n\r\n".getBytes());

        Mockito.when(mockSocket.getOutputStream()).thenReturn(outputStream);
        Mockito.when(mockSocket.getInputStream()).thenReturn(inputStream);

        boolean result = ContentServer.sendWeatherToServer(mockSocket, WEATHER_DATA_FILE_PATH, 1);
        assertTrue("Should not send weather data successfully", !result);
    }

    /**
     * Tests the sendWeatherToServer method when an invalid file path is provided.
     * It verifies that the method returns false and handles the error gracefully.
     *
     * @throws IOException if an I/O error occurs while sending data.
     */
    @Test
    public void testSendWeatherDataInvalidPath() throws IOException {
        // Mock socket and simulate server response
        Socket mockSocket = Mockito.mock(Socket.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayInputStream inputStream = new ByteArrayInputStream("HTTP/1.1 200 OK\r\n\r\n".getBytes());

        Mockito.when(mockSocket.getOutputStream()).thenReturn(outputStream);
        Mockito.when(mockSocket.getInputStream()).thenReturn(inputStream);

        boolean result = ContentServer.sendWeatherToServer(mockSocket, "invalid/path.txt", 1);
        assertTrue("Should not send weather data successfully due to invalid path", !result);
    }
}
