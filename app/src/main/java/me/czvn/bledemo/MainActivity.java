package me.czvn.bledemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.czvn.bledemo.adapter.ChatListAdapter;
import me.czvn.bledemo.adapter.ScanListAdapter;
import me.czvn.bledemo.datas.MsgData;
import me.czvn.bledemo.datas.ScanData;
import me.czvn.blelibrary.bluetooth.BLEAdvertiser;
import me.czvn.blelibrary.bluetooth.BLEClient;
import me.czvn.blelibrary.bluetooth.BLEScanner;
import me.czvn.blelibrary.bluetooth.BLEServer;
import me.czvn.blelibrary.interfaces.IAdvertiseResultListener;
import me.czvn.blelibrary.interfaces.IBLECallback;
import me.czvn.blelibrary.interfaces.IScanResultListener;

/**
 * Created by andy on 2016/2/26.
 * 简单的Demo，使用BLE实现聊天，没有对界面进行美化
 */

public final class MainActivity extends AppCompatActivity implements IBLECallback {
    public static final String TAG = "BBK_" + MainActivity.class.getSimpleName();
    public static final boolean LOG_DEBUG = BuildConfig.DEBUG;
    public static final int REQUEST_ENABLE_BLUETOOTH = 15;//请求打开蓝牙
    public static final int SCAN_DURATION = 2000;//扫描时长

    private Button btnSendMsg;
    private Button btnCheckAdvertise;
    private EditText etMsg;
    private ListView listScanResult;
    private ListView listChat;

    private BLEScanner bleScanner;
    private BLEClient bleClient;
    private BLEAdvertiser bleAdvertiser;
    private BLEServer bleServer;

    private List<MsgData> msgList;
    private List<ScanData> scanList;

    private BaseAdapter chatListAdapter;
    private BaseAdapter scanListAdapter;

    private ConnectType connectType;
    private boolean connected;

    private MyHandler mHandler;
    public static  boolean ConnedStatus;
    private String mPreviousDevicesAddr = null;
    public int mPreviousRssi = -127;
    private Thread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initVariables();
        initViews();
        initData();
        Log.d(TAG, "["+
                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
    }

    private void initVariables() {
        btnSendMsg = getView(R.id.btn_send);
        btnCheckAdvertise = getView(R.id.btn_checkAdvertise);
        etMsg = getView(R.id.et_msg);

        listScanResult = getView(R.id.list_scan_result);
        listChat = getView(R.id.list_chat);
        msgList = new ArrayList<>();
        scanList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(msgList, this);
        scanListAdapter = new ScanListAdapter(scanList, this);
        connected = false;
        mHandler = new MyHandler();
    }

    private void initViews() {
        listChat.setAdapter(chatListAdapter);
        listScanResult.setAdapter(scanListAdapter);

        btnCheckAdvertise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter().isMultipleAdvertisementSupported() ?
                        getString(R.string.advertise_support) : getString(R.string.advertise_not_support);
                makeToast(msg);
            }
        });

        btnSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!connected) {
                    makeToast(MainActivity.this.getString(R.string.not_connected));
                    return;
                }
                String msg = etMsg.getText().toString();
//                if (connectType == ConnectType.CENTRAL) {
//                    bleClient.sendMsg(msg);
//
//                }
//                if (connectType == ConnectType.PERIPHERAL) {
//                    bleServer.sendMsg(msg);
//                }
                bleClient.sendMsg(msg);
                bleServer.sendMsg(msg);
                msgList.add(new MsgData(msg));
                chatListAdapter.notifyDataSetChanged();
                etMsg.setText("");
            }
        });
        listScanResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bleClient.startConnect(scanList.get(position).getAddress());
            }
        });

    }

    public void StartPeripherals()
    {
        Log.d(TAG, "["+
                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
        bleClient.stopConnect();
        bleScanner.stopScan();
        bleServer.startGattServer();
        bleAdvertiser.startAdvertise();
        connectType = ConnectType.PERIPHERAL;
        Log.d(TAG, "["+
                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
    }


    public void StartCentral()
    {
        Log.d(TAG, "["+
                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
        bleAdvertiser.stopAdvertise();
        bleServer.stopGattServer();
        bleScanner.startScan();
        connectType = ConnectType.CENTRAL;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "["+
                        Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                        Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                        Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
                bleScanner.stopScan();
            }
        }, SCAN_DURATION);
        Log.d(TAG, "["+
                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
    }

    private void initData() {
        bleClient = new BLEClient(this, this);
        bleServer = BLEServer.getInstance(this, this);
        bleAdvertiser = BLEAdvertiser.getInstance(this, new IAdvertiseResultListener() {
            @Override
            public void onAdvertiseSuccess() {
                if (LOG_DEBUG) {
                    Log.d(TAG, "["+
                            Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                            Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                            Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
                    Log.i(TAG, "advertise success");
                }
            }

            @Override
            public void onAdvertiseFailed(int errorCode) {
                if (LOG_DEBUG) {
                    Log.d(TAG, "["+
                            Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                            Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                            Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
                    Log.e(TAG, "advertise failed");
                }
            }
        });
        bleScanner = BLEScanner.getInstance(this, new IScanResultListener() {
            @Override
            public void onResultReceived(String deviceName, String deviceAddress, int mDeviceRssi) {
                scanList.add(new ScanData(deviceName, deviceAddress));
                mHandler.sendEmptyMessage(MyHandler.REFRESH_SCAN_LIST);
                Log.d(TAG, "["+
                        Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                        Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                        Thread.currentThread().getStackTrace()[2].getMethodName() + "]" +
                        " deviceAddress: "+ deviceAddress + "  mDeviceRssi: " + mDeviceRssi +
                        " mPreviousDevicesAddr: " + mPreviousDevicesAddr+" mPreviousRssi: " + mPreviousRssi);
                //TODO Add ywm
                //bleClient.startConnect(deviceAddress);
                  if (mDeviceRssi > mPreviousRssi){
                      mPreviousRssi = mDeviceRssi;
                      mPreviousDevicesAddr = deviceAddress;
                  }

            }

            @Override
            public void onScanFailed(int errorCode) {
                if (LOG_DEBUG) {
                    Log.e(TAG, "scan failed" + errorCode);
                }
            }
        });

//        mThread = new Thread(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.d(TAG, "[" +
//                                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
//                                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
//                                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
//                        if(bleServer.startGattServer()) {
//                            bleAdvertiser.startAdvertise();
//                            connectType = ConnectType.PERIPHERAL;
//                            Log.d(TAG, "[" +
//                                    Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
//                                    Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
//                                    Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
//                        }
//
//                        bleScanner.startScan();
//                        connectType = ConnectType.CENTRAL;
//
//                        mHandler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                Log.d(TAG, "["+
//                                        Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
//                                        Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
//                                        Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
//                                bleScanner.stopScan();
//                                if(mPreviousDevicesAddr != null)
//                                    bleClient.startConnect(mPreviousDevicesAddr);
//                            }
//                        }, SCAN_DURATION);
//                    }
//                }
//        );
//        mThread.start();
        if(bleServer.startGattServer()) {
            bleAdvertiser.startAdvertise();
            connectType = ConnectType.PERIPHERAL;
            Log.d(TAG, "[" +
                    Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                    Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                    Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
        }

        bleScanner.startScan();
        connectType = ConnectType.CENTRAL;

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "["+
                        Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                        Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                        Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
                bleScanner.stopScan();
                if(mPreviousDevicesAddr != null)
                    bleClient.startConnect(mPreviousDevicesAddr);
            }
        }, SCAN_DURATION);

    }


    @Override
    protected void onStart() {
        super.onStart();
        checkBluetoothOpened();
        Log.d(TAG, "["+
                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
    }


    @Override
    protected void onResume() {
        super.onResume();
        mHandler.attach(chatListAdapter, scanListAdapter);
        Log.d(TAG, "["+
                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.d(TAG, "["+
                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
    }

    @Override
    protected void onStop(){
        super.onStop();
        Log.d(TAG, "["+
                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "["+
                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
        mHandler.detach();
        System.exit(0);
    }

    @Override
    public void onConnected()
    {
        connected = true;
        Log.d(TAG, "["+
                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
        makeToast(getString(R.string.connected));
    }

    @Override
    public void onDisconnected() {
        connected = false;
        Log.d(TAG, "["+
                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
        makeToast(getString(R.string.disconnected));
    }

    @Override
    public void onMessageReceived(String msg) {
        msgList.add(new MsgData(msg));
        mHandler.sendEmptyMessage(MyHandler.REFRESH_CHAT_LIST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH && resultCode == RESULT_CANCELED) {
            finish();
        }
    }

    private void checkBluetoothOpened() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        }

    }

    //Handler来刷新UI
    private  class MyHandler extends Handler {
        //可以使用runOnUiThread或者持有View使用View.post来简化代码
        public static final int REFRESH_SCAN_LIST = 250;
        public static final int REFRESH_CHAT_LIST = 38;

        public static final int START_PERIPHERAL = 1;
        public static final int START_CENTRAL = 0;

        private BaseAdapter chatAdapter;
        private BaseAdapter scanAdapter;

        public void attach(BaseAdapter chatAdapter, BaseAdapter scanAdapter) {
            this.chatAdapter = chatAdapter;
            this.scanAdapter = scanAdapter;
        }

        public void detach() {
            chatAdapter = null;
            scanAdapter = null;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case REFRESH_CHAT_LIST:
                    {
                        if (chatAdapter != null) {
                            chatAdapter.notifyDataSetChanged();
                        }
                    }
                    break;
                case REFRESH_SCAN_LIST:
                    {
                        if (scanAdapter != null) {
                            scanAdapter.notifyDataSetChanged();
                        }
                    }
                    break;
                case START_CENTRAL:
                    {
                            Log.d(TAG, "["+
                                Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                                Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                                Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
                            StartCentral();
                    }
                    break;
                case START_PERIPHERAL:
                    {

                            Log.d(TAG, "[" +
                                    Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                                    Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                                    Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
                            StartPeripherals();
                    }
                    break;
            }

           /* if (msg.what == REFRESH_CHAT_LIST) {
                if (chatAdapter != null) {
                    chatAdapter.notifyDataSetChanged();
                }
            }
            if (msg.what == REFRESH_SCAN_LIST) {
                if (scanAdapter != null) {
                    scanAdapter.notifyDataSetChanged();
                }
            }

            if (msg.what == START_PERIPHERAL) {
                Log.d(TAG, "["+
                        Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                        Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                        Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
                StartCentral();
            }

            if (msg.what == START_CENTRAL) {
                Log.d(TAG, "["+
                        Thread.currentThread().getStackTrace()[2].getFileName() + "_" +
                        Thread.currentThread().getStackTrace()[2].getLineNumber() + "_" +
                        Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
                StartPeripherals();
            }*/
        }
    }

    //显示通知
    private void makeToast(final String toast) {
        btnSendMsg.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //可以减少许多次强制类型转换
    @SuppressWarnings("unchecked")
    private <T extends View> T getView(int resID) {
        return (T) findViewById(resID);
    }
}
