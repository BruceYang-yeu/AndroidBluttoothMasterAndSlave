package me.czvn.blelibrary.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import me.czvn.blelibrary.interfaces.IBLECallback;
import me.czvn.blelibrary.interfaces.IReceiver;
import me.czvn.blelibrary.interfaces.ISender;
import me.czvn.blelibrary.utils.MsgQueue;
import me.czvn.blelibrary.utils.MsgReceiver;
import me.czvn.blelibrary.utils.MsgSender;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by andy on 2016/1/13.
 * 对bluetoothGatt进行了封装
 */
public final class BLEClient {
    private static final String TAG = BLEClient.class.getSimpleName();

    private WeakReference<Context> contextWeakReference;

    private BluetoothGattCallback gattCallback;
    private BluetoothGatt bluetoothGatt;

    private MsgReceiver msgReceiver;
    private MsgSender msgSender;

    private IBLECallback ibleCallback;


    private BluetoothGattCharacteristic writeChannel;

    private boolean connected;

    private MsgQueue<byte[]> msgQueue;//使用消息队列达到异步处理数据发送的问题

    private boolean onWrite;//是否正在发送数据


    public BLEClient(Context mContext, IBLECallback IBLECallback) {
        contextWeakReference=new WeakReference<>(mContext);
        this.ibleCallback = IBLECallback;
        connected = false;
        msgSender = new MsgSender(new ISender() {
            @Override
            public void inputData(byte[] bytes) {
                msgQueue.enQueue(Arrays.copyOf(bytes,bytes.length));
                startWrite();
            }
        });
        msgReceiver = new MsgReceiver(new IReceiver() {
            @Override
            public void receiveMessage(String msg) {
                ibleCallback.onMessageReceived(msg);
            }
        });
        msgQueue = new MsgQueue<>();
        initGattCallback();

    }

    /**
     * 开始使用Gatt连接
     *
     * @param address 要连接的蓝牙设备的地址
     * @return 是否连接成功
     */
    public boolean startConnect(String address) {
        Context mContext=contextWeakReference.get();
        if(mContext==null){
            return false;
        }
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothGatt = bluetoothAdapter.getRemoteDevice(address).connectGatt(mContext, false, gattCallback);
        if (bluetoothGatt.connect()) {
            connected = true;
            return true;
        }
        return false;
    }


    /**
     * 断开连接
     */
    public void stopConnect() {
        if (connected) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
        connected = false;
    }


    /**
     * 发送消息
     *
     * @param msg 要发送的消息
     */
    public void sendMsg(String msg) {
        if (connected) {
            msgSender.sendMessage(msg);
        }
    }


    private void initGattCallback() {
        gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                Log.i(TAG, "onConnectionStateChange");
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Client success & start discover services");
                    gatt.discoverServices();
                    ibleCallback.onConnected();

                }
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e(TAG, "connect closed");
                    ibleCallback.onDisconnected();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.i(TAG, "onServicesDiscovered");

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "Discover Services failed");
                    return;
                }
                BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(BLEProfile.UUID_SERVICE));
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BLEProfile.UUID_CHARACTERISTIC_NOTIFY));
                    if (characteristic != null) {
                        //订阅通知，这段代码对iOS的peripheral也能订阅
                        Log.i(TAG, "SetNotification");
                        bluetoothGatt.setCharacteristicNotification(characteristic, true);
                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }

                    } else {
                        Log.e(TAG, "The notify characteristic is null");
                    }
                    writeChannel = service.getCharacteristic(UUID.fromString(BLEProfile.UUID_CHARACTERISTIC_WRITE));
                    if (characteristic == null) {
                        Log.e(TAG, "The write characteristic is null");
                    }
                } else {
                    Log.e(TAG, "The special service is null");
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                Log.i(TAG, "onCharacteristicRead");
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "Read Characteristic failed");
                }

            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                nextWrite();
                Log.i(TAG, "onCharacteristicWrite");
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                //处理notify
                super.onCharacteristicChanged(gatt, characteristic);
                Log.i(TAG, "Notify: onCharacteristicChange");
                byte[] value = characteristic.getValue();
                msgReceiver.outputData(value);//将数据整合成String
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
                Log.i(TAG, "onDescriptorRead");
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                Log.i(TAG, "onDescriptorWrite");
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
                Log.i(TAG, "onReliableWriteCompleted");
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);
                Log.i(TAG, "onReadRemoteRssi");
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
                Log.i(TAG, "onMtuChanged");
            }
        };
    }





    /*
     * 控制稳定write区域
     */
    private void startWrite() {
        if (onWrite) {
            return;
        }
        nextWrite();
    }

    /*
     * 控制稳定write区域
     */
    private void nextWrite() {
        if (msgQueue.isEmpty()) {
            onWrite = false;
            return;
        }
        write();
    }

    /*
     * 控制稳定write区域
     */
    private void write() {
        onWrite = true;
        byte[] bytes = msgQueue.deQueue();
        try {
            writeChannel.setValue(bytes);
            bluetoothGatt.writeCharacteristic(writeChannel);
        } catch (NullPointerException e) {
            Log.e(TAG, "null pointer on characteristic");
        }
    }

}
