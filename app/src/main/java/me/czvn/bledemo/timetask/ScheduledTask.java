package me.czvn.bledemo.timetask;

import java.util.TimerTask;

import me.czvn.blelibrary.bluetooth.BLEAdvertiser;
import me.czvn.blelibrary.bluetooth.BLEClient;
import me.czvn.blelibrary.bluetooth.BLEScanner;
import me.czvn.blelibrary.bluetooth.BLEServer;

/**
 * Created by Administrator on 16.4.13.
 */
public class ScheduledTask extends TimerTask {

    private BLEScanner bleScanner;
    private BLEClient bleClient;
    private BLEAdvertiser bleAdvertiser;
    private BLEServer bleServer;

    public ScheduledTask(BLEAdvertiser mBLEAdvertiser, BLEClient mBLEClient, BLEServer mBLEServer, BLEScanner mBLEScanner)
    {
        bleScanner = mBLEScanner;
        bleClient = mBLEClient;
        bleAdvertiser = mBLEAdvertiser;
        bleServer = mBLEServer;
    }
    // Add your task here
    public void run() {

    }
}
