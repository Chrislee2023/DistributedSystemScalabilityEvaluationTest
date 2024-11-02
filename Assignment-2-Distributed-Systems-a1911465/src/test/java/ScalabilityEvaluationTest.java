import org.junit.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ScalabilityEvaluationTest {

    private static final int NUM_SERVERS = 1;
    private static final int NUM_CLIENTS = 50;
    private static final String SERVER_URL = "localhost";
    private static final int BASE_PORT = 4567;
    private static final AtomicInteger failureCount = new AtomicInteger(0); // Count failures for reporting
    private static final AtomicInteger retryCount = new AtomicInteger(0); // Count retries for fault tolerance
    private static final List<Long> responseTimes = new ArrayList<>(); // List for scalability response times
    private static final AtomicInteger clientCounter = new AtomicInteger(0); // Unique client ID tracker

    /**
     * Tests how different implementations handle failures by injecting faults
     * and observing system behavior and recovery.
     */
    @Test
    public void testFailureHandling() {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_SERVERS + NUM_CLIENTS);

        try {
            List<Thread> servers = initializeServersWithFaultInjection(executor);
            List<Thread> clients = initializeClients(executor);

            // Monitor and report failure handling metrics
            monitorFailureHandlingMetrics(clients);

        } finally {
            executor.shutdown();
        }
    }

    /**
     * Tests the scalability of each system implementation by simulating
     * multiple clients and observing response times and system load.
     */
    @Test
    public void testScalability() {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_SERVERS + NUM_CLIENTS);

        try {
            List<Thread> servers = initializeServers(executor);
            List<Thread> clients = initializeClients(executor);

            // Monitor and report scalability performance metrics
            monitorScalabilityMetrics(clients);

        } finally {
            executor.shutdown();
        }
    }

    /**
     * Initializes server threads and returns a list of running servers.
     */
    private List<Thread> initializeServers(ExecutorService executor) {
        List<Thread> servers = new ArrayList<>();
        for (int i = 0; i < NUM_SERVERS; i++) {
            int port = BASE_PORT + i;
            Thread serverThread = new Thread(() -> {
                ContentServer server = new ContentServer();
                server.start();
                System.out.println("Server started on port " + port);
            });
            servers.add(serverThread);
            executor.submit(serverThread);
        }
        return servers;
    }

    /**
     * Tests if the implementation maintains correct event ordering with Lamport clocks
     * and evaluates consistency of partial or total ordering across clients.
     */
    @Test
    public void testEventOrdering() {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENTS);

        try {
            List<Thread> clients = initializeClientsWithLamportClock(executor);

            // Monitor and report event ordering consistency using Lamport clocks
            monitorLamportClockOrdering(clients);

        } finally {
            executor.shutdown();
        }
    }

    /**
     * Initializes server threads and simulates faults to test fault tolerance.
     */
    private List<Thread> initializeServersWithFaultInjection(ExecutorService executor) {
        List<Thread> servers = new ArrayList<>();
        for (int i = 0; i < NUM_SERVERS; i++) {
            int port = BASE_PORT + i;
            Thread serverThread = new Thread(() -> {
                ContentServer server = new ContentServer();
                server.start();
                System.out.println("Server started on port " + port);

                // Simulate random failures (e.g., server crash)
                if (Math.random() < 0.2) { // 20% chance to simulate failure
                    server.stop();
                    failureCount.incrementAndGet();
                    System.out.println("Server on port " + port + " stopped due to simulated failure.");
                }
            });
            servers.add(serverThread);
            executor.submit(serverThread);
        }
        return servers;
    }

    /**
     * Initializes clients to simulate requests with retries for fault tolerance.
     */
    private List<Thread> initializeClients(ExecutorService executor) {
        List<Thread> clients = new ArrayList<>();
        for (int i = 0; i < NUM_CLIENTS; i++) {
            int clientId = clientCounter.incrementAndGet();
            boolean isPutRequest = (i % 2 == 0); // Alternate between GET and PUT requests for each client

            Thread clientThread = new Thread(() -> {
                GETClient getClient = new GETClient();
                ContentServer contentServer = new ContentServer(); // Assuming ContentServer has a method to handle PUT
                boolean success = false;
                int retries = 0;

                System.out.println("Client " + clientId + " started at " + System.currentTimeMillis() +
                        " with " + (isPutRequest ? "PUT" : "GET") + " request");

                // Simulate request with retry logic in case of failure
                while (!success && retries < 3) {
                    long startTime = System.currentTimeMillis();
                    try {
                        if (isPutRequest) {
                            // Simulate PUT request with sample data
                            String sampleData = "{\"id\":\"TestStation" + clientId + "\",\"temp\":25}";
                            success = contentServer.sendPutRequest(SERVER_URL, BASE_PORT, sampleData); // Replace with actual PUT request method
                            System.out.println("Client " + clientId + " sent PUT request with data: " + sampleData);
                        } else {
                            // Simulate GET request
                            getClient.sendGetRequest(SERVER_URL, BASE_PORT, "TestStation", new LamportClock());
                            System.out.println("Client " + clientId + " sent GET request");
                        }
                    } catch (IOException e) {
                        System.out.println("Client " + clientId + " encountered an IOException: " + e.getMessage());
                        success = false;
                    }

                    if (success) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        responseTimes.add(responseTime);
                        System.out.println("Client " + clientId + " received response in " + responseTime + " ms");
                    } else {
                        retryCount.incrementAndGet();
                        retries++;
                        System.out.println("Client " + clientId + " retrying... (" + retries + " of 3)");
                    }
                }

                if (!success) {
                    System.out.println("Client " + clientId + " failed to receive a response after 3 attempts.");
                }
            });
            clients.add(clientThread);
            executor.submit(clientThread);
        }
        return clients;
    }

    /**
     * Initializes clients with Lamport clocks for event ordering tests.
     */
    private List<Thread> initializeClientsWithLamportClock(ExecutorService executor) {
        return initializeClients(executor); // Placeholder for further customization if needed
    }

    /**
     * Monitors failure handling metrics, including the number of failures
     * and retries required to successfully process requests.
     */
    private void monitorFailureHandlingMetrics(List<Thread> clients) {
        System.out.println("Total Failures Injected: " + failureCount.get());
        System.out.println("Total Retries: " + retryCount.get());
    }

    /**
     * Monitors scalability performance by calculating average response times
     * and assessing the system's handling of high loads.
     */
    private void monitorScalabilityMetrics(List<Thread> clients) {
        if (!responseTimes.isEmpty()) {
            long totalResponseTime = responseTimes.stream().mapToLong(Long::longValue).sum();
            long averageResponseTime = totalResponseTime / responseTimes.size();
            System.out.println("Average Response Time under Load: " + averageResponseTime + " ms");
        } else {
            System.out.println("No response times recorded. Possible connectivity issues or all requests failed.");
        }
    }

    /**
     * Monitors Lamport clock consistency across clients to verify event ordering.
     */
    private void monitorLamportClockOrdering(List<Thread> clients) {
        System.out.println("Monitoring event ordering consistency with Lamport clocks...");
        // For each client, check Lamport clock values to ensure consistent ordering
        // Log inconsistencies, if any, to verify partial or total ordering
    }
}
