package me.czvn.blelibrary.utils;


import me.czvn.blelibrary.interfaces.IReceiver;

/**
 * Created by andy on 2016/1/13.
 *
 */
public final class MsgReceiver {
    private static final String TAG = MsgReceiver.class.getSimpleName();

    private boolean sending;
    private int length;
    private int nowLength;
    private byte[] totalBytes;
    private String result;
    private IReceiver receiver;


    public MsgReceiver(IReceiver receiver) {
        init();
        this.receiver = receiver;
    }

    private void init() {
        sending = false;
        length = 0;
        nowLength = 0;
        totalBytes = new byte[0];
        result = null;
    }

    public void outputData(byte[] bytes) {
        if (!sending) {
            length = MsgCommonUtil.goInt(bytes);
            sending = true;
        } else {
            nowLength += bytes.length;
            totalBytes = MsgCommonUtil.merge(totalBytes, bytes);
            if (nowLength >= length) {
                result = new String(totalBytes);
                receiver.receiveMessage(result);
                init();
            }
        }

    }

}
