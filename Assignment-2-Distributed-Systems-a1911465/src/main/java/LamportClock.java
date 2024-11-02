/**
 * The LamportClock class implements a logical clock based on Lamport timestamps.
 * It provides methods to increment the clock and update its value based on received timestamps.
 */
public class LamportClock {
    private int value;

    /**
     * Constructs a LamportClock instance, initializing its value to 0.
     */
    public LamportClock() {
        this.value = 0;
    }

    /**
     * Gets the current value of the Lamport clock.
     *
     * @return The current value of the Lamport clock.
     */
    public int getValue() {
        return value;
    }

    /**
     * Increments the value of the Lamport clock by 1.
     */
    public void increment() {
        value++;
    }

    /**
     * Updates the Lamport clock value based on a new timestamp.
     * The new value is set to the maximum of the current clock value and the provided new value, plus one.
     *
     * @param newValue The new timestamp value to update the clock with.
     */
    public void update(int newValue) {
        // Set the value to the maximum of the current and the new value
        value = Math.max(value, newValue) + 1;
    }
}
