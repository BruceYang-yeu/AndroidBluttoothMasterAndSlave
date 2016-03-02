package me.czvn.blelibrary.utils;

import me.czvn.blelibrary.interfaces.ISender;

/**
 * Created by andy on 2016/1/13.
 *
 */
public final class MsgSender {
    private static final int DEFAULT_SIZE = 20;
    private ISender sender;


    public MsgSender(ISender sender) {
        this.sender = sender;
    }


    public void sendMessage(String msg) {
        sendMessage(msg, DEFAULT_SIZE);
    }


    public void sendMessage(String msg, int size) {
        byte[] bytes = msg.getBytes();
        int length = bytes.length;
        int counter = length / size;
        int rest = length % size;
        byte[] buffer = new byte[size];
        byte[] rests = new byte[rest];

        sender.inputData(MsgCommonUtil.goBytes(length));
        for (int i = 0; i < counter; i++) {
            for (int j = 0; j < buffer.length; j++) {
                buffer[j] = bytes[i * size + j];
            }
            sender.inputData(buffer);
        }
        for (int i = 0; i < rests.length; i++) {
            rests[i] = bytes[i + counter * size];
        }
        sender.inputData(rests);
    }
}
