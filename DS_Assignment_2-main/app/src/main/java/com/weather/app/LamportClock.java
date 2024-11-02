package com.weather.app;

// A class that simulates a Lamport clock for event ordering in distributed systems
public class LamportClock {
    private int clockValue;  // The current clock value

    // Constructor that initializes the Lamport clock to 0
    public LamportClock() {
        this.clockValue = 0;
    }

    // Method to increment the clock value when a local event occurs
    public synchronized void increment() {
        clockValue += 1;  // Increase the clock value by 1
    }

    // Method to update the clock when a message is received from another process
    // It takes the maximum of the local clock and the received clock, then increments by 1
    public synchronized void update(int receivedClock) {
        clockValue = Math.max(clockValue, receivedClock) + 1;
    }

    // Method to return the current clock value
    public synchronized int getClock() {
        return clockValue;
    }
}
