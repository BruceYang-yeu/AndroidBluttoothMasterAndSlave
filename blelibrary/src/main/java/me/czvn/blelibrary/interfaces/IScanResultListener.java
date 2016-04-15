package me.czvn.blelibrary.interfaces;


/**
 * Created by andy on 2016/1/14.
 *
 */
public interface IScanResultListener {
    /**
     * 这个方法会在成功接收到扫描结果时调用
     * @param deviceName 设备名称
     * @param deviceAddress 设备地址
     * @param mDeviceRssi   设备信号强度
     */
    void onResultReceived(String deviceName, String deviceAddress, int mDeviceRssi);

    /**
     * 这个方法会在扫描失败时调用，
     * @param errorCode 请查阅ScanCallback类的API
     */
    void onScanFailed(int errorCode);
}
