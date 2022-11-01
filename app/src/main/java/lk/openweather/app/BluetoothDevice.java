package lk.openweather.app;

public class BluetoothDevice {
    private String macAddress;
    private String name;

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BluetoothDevice(String macAddress, String name) {
        this.macAddress = macAddress;
        this.name = name;
    }
}
