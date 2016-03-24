package me.czvn.blelibrary.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.lang.ref.WeakReference;

import me.czvn.blelibrary.interfaces.IAdvertiseResultListener;

/**
 * Created by andy on 2016/1/13.
 * 对bluetoothLeAdvertiser进行了封装
 */
public final class BLEAdvertiser {
    private static final String TAG = BLEAdvertiser.class.getSimpleName();

    private static BLEAdvertiser instance;


    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;
    private AdvertiseSettings advertiseSettings;
    private AdvertiseData advertiseData;


    private WeakReference<Context> contextWeakReference;//使用弱引用防止内存泄漏
    private WeakReference<IAdvertiseResultListener> listenerWeakReference;

    private boolean prepared;//是否准备好广播

    /**
     * 单例模式
     *
     * @param context  context引用
     * @param listener 广播结果的监听
     * @return BLEAdvertiser的实例
     */
    public static BLEAdvertiser getInstance(Context context, IAdvertiseResultListener listener) {
        if (instance == null) {
            instance = new BLEAdvertiser(context, listener);
        } else {
            instance.contextWeakReference = new WeakReference<>(context);
            instance.listenerWeakReference = new WeakReference<>(listener);
        }
        return instance;
    }

    /**
     * 开始广播
     */
    public void startAdvertise() {
        if (!prepared) {
            initAdvertiseData();
        }
        if (advertiser == null) {
            return;
        }
        advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
    }

    /**
     * 停止广播
     */
    public void stopAdvertise() {
        if (prepared) {
            advertiser.stopAdvertising(advertiseCallback);
        }
    }


    private BLEAdvertiser(Context context, IAdvertiseResultListener listener) {
        prepared = false;
        contextWeakReference = new WeakReference<>(context);
        listenerWeakReference = new WeakReference<>(listener);
    }

    private void initAdvertiseData() {
        //初始化Advertise的设定
        Context mContext = contextWeakReference.get();
        if (mContext == null) {
            prepared = false;
            return;
        }
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.i(TAG, "Advertise success");
                IAdvertiseResultListener advertiseResultListener = listenerWeakReference.get();
                if (advertiseResultListener != null) {
                    advertiseResultListener.onAdvertiseSuccess();
                }
                if (settingsInEffect != null) {
                    Log.d(TAG, "onStartSuccess TxPowerLv=" + settingsInEffect.getTxPowerLevel() + " mode=" + settingsInEffect.getMode()
                            + " timeout=" + settingsInEffect.getTimeout());
                } else {
                    Log.e(TAG, "onStartSuccess, settingInEffect is null");
                }
                Log.i(TAG, "onStartSuccess settingsInEffect" + settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e(TAG, "Advertise failed.Error code: " + errorCode);
                IAdvertiseResultListener advertiseResultListener = listenerWeakReference.get();
                if (advertiseResultListener != null) {
                    advertiseResultListener.onAdvertiseFailed(errorCode);
                }
            }
        };
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setConnectable(true);
        settingsBuilder.setTimeout(0);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
        advertiseSettings = settingsBuilder.build();
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(true);
        dataBuilder.addServiceUuid(ParcelUuid.fromString(BLEProfile.UUID_SERVICE));
        advertiseData = dataBuilder.build();
        prepared = true;
    }
}
