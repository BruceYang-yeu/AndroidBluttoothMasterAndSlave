package me.czvn.bledemo.datas;

/**
 * Created by andy on 2016/2/26.
 *
 */
public final class ScanData {
    private String deviceName;
    private String address;

    public ScanData(String deviceName, String address) {
        this.deviceName = deviceName;
        this.address = address;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getAddress() {
        return address;
    }
}
