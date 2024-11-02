import java.io.Serializable;

/**
 * The Weather class represents weather data, including various meteorological parameters.
 * It implements the Serializable interface to allow for object serialization.
 */
public class Weather implements Serializable {

    private String id;
    private String name;
    private String state;
    private String time_zone;
    private double lat;
    private double lon;
    private String local_date_time;
    private String local_date_time_full;
    private double air_temp;
    private double apparent_t;
    private String cloud;
    private double dewpt;
    private double press;
    private double rel_hum;
    private String wind_dir;
    private double wind_spd_kmh;
    private double wind_spd_kt;

    /**
     * Constructor to initialize the weather data fields.
     *
     * @param id                   The unique identifier for the weather data.
     * @param name                 The name of the location for the weather data.
     * @param state                The state in which the location is situated.
     * @param time_zone            The time zone of the location.
     * @param lat                  The latitude of the location.
     * @param lon                  The longitude of the location.
     * @param local_date_time      The local date and time of the weather data.
     * @param local_date_time_full The full local date and time (e.g., YYYYMMDDHHMMSS).
     * @param air_temp             The air temperature at the location.
     * @param apparent_t           The apparent temperature at the location.
     * @param cloud                The cloud conditions (e.g., "Partly cloudy").
     * @param dewpt                The dew point temperature.
     * @param press                The atmospheric pressure.
     * @param rel_hum              The relative humidity.
     * @param wind_dir             The wind direction.
     * @param wind_spd_kmh        The wind speed in kilometers per hour.
     * @param wind_spd_kt          The wind speed in knots.
     */
    public Weather(String id, String name, String state, String time_zone, double lat, double lon,
                   String local_date_time, String local_date_time_full, double air_temp, double apparent_t,
                   String cloud, double dewpt, double press, double rel_hum, String wind_dir,
                   double wind_spd_kmh, double wind_spd_kt) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.time_zone = time_zone;
        this.lat = lat;
        this.lon = lon;
        this.local_date_time = local_date_time;
        this.local_date_time_full = local_date_time_full;
        this.air_temp = air_temp;
        this.apparent_t = apparent_t;
        this.cloud = cloud;
        this.dewpt = dewpt;
        this.press = press;
        this.rel_hum = rel_hum;
        this.wind_dir = wind_dir;
        this.wind_spd_kmh = wind_spd_kmh;
        this.wind_spd_kt = wind_spd_kt;
    }

    // Getter and setter methods for all fields

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getTime_zone() { return time_zone; }
    public void setTime_zone(String time_zone) { this.time_zone = time_zone; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }

    public String getLocal_date_time() { return local_date_time; }
    public void setLocal_date_time(String local_date_time) { this.local_date_time = local_date_time; }

    public String getLocal_date_time_full() { return local_date_time_full; }
    public void setLocal_date_time_full(String local_date_time_full) { this.local_date_time_full = local_date_time_full; }

    public double getAir_temp() { return air_temp; }
    public void setAir_temp(double air_temp) { this.air_temp = air_temp; }

    public double getApparent_t() { return apparent_t; }
    public void setApparent_t(double apparent_t) { this.apparent_t = apparent_t; }

    public String getCloud() { return cloud; }
    public void setCloud(String cloud) { this.cloud = cloud; }

    public double getDewpt() { return dewpt; }
    public void setDewpt(double dewpt) { this.dewpt = dewpt; }

    public double getPress() { return press; }
    public void setPress(double press) { this.press = press; }

    public double getRel_hum() { return rel_hum; }
    public void setRel_hum(double rel_hum) { this.rel_hum = rel_hum; }

    public String getWind_dir() { return wind_dir; }
    public void setWind_dir(String wind_dir) { this.wind_dir = wind_dir; }

    public double getWind_spd_kmh() { return wind_spd_kmh; }
    public void setWind_spd_kmh(double wind_spd_kmh) { this.wind_spd_kmh = wind_spd_kmh; }

    public double getWind_spd_kt() { return wind_spd_kt; }
    public void setWind_spd_kt(double wind_spd_kt) { this.wind_spd_kt = wind_spd_kt; }

    /**
     * Returns a string representation of the Weather object.
     *
     * @return A string displaying the weather data in a readable format.
     */
    @Override
    public String toString() {
        return "Weather{" +
                "id:'" + id + '\'' +
                ", name:'" + name + '\'' +
                ", state:'" + state + '\'' +
                ", time_zone:'" + time_zone + '\'' +
                ", lat:" + lat +
                ", lon:" + lon +
                ", local_date_time:'" + local_date_time + '\'' +
                ", local_date_time_full:'" + local_date_time_full + '\'' +
                ", air_temp:" + air_temp +
                ", apparent_t:" + apparent_t +
                ", cloud:'" + cloud + '\'' +
                ", dewpt:" + dewpt +
                ", press:" + press +
                ", rel_hum:" + rel_hum +
                ", wind_dir:'" + wind_dir + '\'' +
                ", wind_spd_kmh:" + wind_spd_kmh +
                ", wind_spd_kt:" + wind_spd_kt +
                '}';
    }
}
