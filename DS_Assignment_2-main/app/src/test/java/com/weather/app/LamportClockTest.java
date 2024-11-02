package com.weather.app;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LamportClockTest {

    @Test
    void testIncrementClock() {
        LamportClock clock = new LamportClock();
        assertEquals(0, clock.getClock(), "Initial clock value should be 0");

        clock.increment();
        assertEquals(1, clock.getClock(), "Clock value should increment to 1");
    }

    @Test
    void testUpdateClock() {
        LamportClock clock = new LamportClock();
        clock.update(5);  // Simulate receiving a clock value of 5
        assertEquals(6, clock.getClock(), "Clock value should be updated to 6");
    }

    @Test
    void testUpdateWithLowerClock() {
        LamportClock clock = new LamportClock();
        clock.increment();
        clock.update(0);  // Update with a lower clock value
        assertEquals(2, clock.getClock(), "Clock value should still increment after receiving a lower clock value");
    }
}
