package lk.openweather.app;

public class Datalog {
    private String temperature;
    private String humidity;
    private String pressure;
    private long timestamp;

    public Datalog() {
    }

    public Datalog(String temperature, String humidity, String pressure, long timestamp) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
        this.timestamp = timestamp;
    }
}
