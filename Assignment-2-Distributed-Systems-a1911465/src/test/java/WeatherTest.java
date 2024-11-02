import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class WeatherTest {
    private Weather weather;

    @Before
    public void setUp() {
        // Initialize with all parameters based on your Weather class
        weather = new Weather(
                "IDS60901",
                "Adelaide",
                "SA",
                "CST",
                -34.9,
                138.6,
                "15/04:00pm",
                "20230715160000",
                13.3,
                9.5,
                "Partly cloudy",
                5.7,
                1023.9,
                60.0,
                "S",
                15.0,
                8.0
        );
    }

    @Test
    public void testGettersAndSetters() {
        assertEquals("IDS60901", weather.getId());
        assertEquals("Adelaide", weather.getName());
        assertEquals("SA", weather.getState());
        assertEquals("CST", weather.getTime_zone());
        assertEquals(-34.9, weather.getLat(), 0.0);
        assertEquals(138.6, weather.getLon(), 0.0);
        assertEquals("15/04:00pm", weather.getLocal_date_time());
        assertEquals("20230715160000", weather.getLocal_date_time_full());
        assertEquals(13.3, weather.getAir_temp(), 0.0);
        assertEquals(9.5, weather.getApparent_t(), 0.0);
        assertEquals("Partly cloudy", weather.getCloud());
        assertEquals(5.7, weather.getDewpt(), 0.0);
        assertEquals(1023.9, weather.getPress(), 0.0);
        assertEquals(60.0, weather.getRel_hum(), 0.0);
        assertEquals("S", weather.getWind_dir());
        assertEquals(15.0, weather.getWind_spd_kmh(), 0.0);
        assertEquals(8.0, weather.getWind_spd_kt(), 0.0);

        // Update values
        weather.setId("IDS60902");
        weather.setName("Sydney");
        weather.setState("NSW");
        weather.setTime_zone("AEDT");
        weather.setLat(-33.9);
        weather.setLon(151.2);
        weather.setLocal_date_time("15/05:00pm");
        weather.setLocal_date_time_full("20230715170000");
        weather.setAir_temp(25.0);
        weather.setApparent_t(20.0);
        weather.setCloud("Clear");
        weather.setDewpt(10.0);
        weather.setPress(1010.0);
        weather.setRel_hum(70.0);
        weather.setWind_dir("E");
        weather.setWind_spd_kmh(20.0);
        weather.setWind_spd_kt(10.0);

        // Assert updated values
        assertEquals("IDS60902", weather.getId());
        assertEquals("Sydney", weather.getName());
        assertEquals("NSW", weather.getState());
        assertEquals("AEDT", weather.getTime_zone());
        assertEquals(-33.9, weather.getLat(), 0.0);
        assertEquals(151.2, weather.getLon(), 0.0);
        assertEquals("15/05:00pm", weather.getLocal_date_time());
        assertEquals("20230715170000", weather.getLocal_date_time_full());
        assertEquals(25.0, weather.getAir_temp(), 0.0);
        assertEquals(20.0, weather.getApparent_t(), 0.0);
        assertEquals("Clear", weather.getCloud());
        assertEquals(10.0, weather.getDewpt(), 0.0);
        assertEquals(1010.0, weather.getPress(), 0.0);
        assertEquals(70.0, weather.getRel_hum(), 0.0);
        assertEquals("E", weather.getWind_dir());
        assertEquals(20.0, weather.getWind_spd_kmh(), 0.0);
        assertEquals(10.0, weather.getWind_spd_kt(), 0.0);
    }

    @Test
    public void testToString() {
        String expectedString = "Weather{" +
                "id:'" + "IDS60901" + '\'' +
                ", name:'" + "Adelaide" + '\'' +
                ", state:'" + "SA" + '\'' +
                ", time_zone:'" + "CST" + '\'' +
                ", lat:" + -34.9 +
                ", lon:" + 138.6 +
                ", local_date_time:'" + "15/04:00pm" + '\'' +
                ", local_date_time_full:'" + "20230715160000" + '\'' +
                ", air_temp:" + 13.3 +
                ", apparent_t:" + 9.5 +
                ", cloud:'" + "Partly cloudy" + '\'' +
                ", dewpt:" + 5.7 +
                ", press:" + 1023.9 +
                ", rel_hum:" + 60.0 +
                ", wind_dir:'" + "S" + '\'' +
                ", wind_spd_kmh:" + 15.0 +
                ", wind_spd_kt:" + 8.0 +
                '}';
        assertEquals(expectedString, weather.toString());
    }
}
