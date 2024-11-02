import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class LamportClockTest {
    private LamportClock clock;

    @Before
    public void setUp() {
        clock = new LamportClock();
    }

    @Test
    public void testIncrement() {
        assertEquals(0, clock.getValue());

        clock.increment();
        assertEquals(1, clock.getValue());

        clock.increment();
        assertEquals(2, clock.getValue());
    }

    @Test
    public void testUpdate() {
        clock.increment();
        assertEquals(1, clock.getValue());

        clock.update(2);
        assertEquals(3, clock.getValue());

        clock.update(5);
        assertEquals(6, clock.getValue());

        clock.update(4);
        assertEquals(7, clock.getValue());
    }
}
