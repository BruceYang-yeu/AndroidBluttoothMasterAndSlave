package me.czvn.blelibrary.utils;

/**
 * Created by andy on 2016/1/19.
 * 链表实现的队列
 */
public class MsgQueue {
    private Node first;
    private Node last;
    private int length;

    public boolean isEmpty() {
        return length == 0;
    }

    public int size() {
        return length;
    }

    public void enQueue(byte[] item) {
        Node oldlast = last;
        last = new Node();
        last.item = item;
        last.next = null;
        if (isEmpty()) {
            first = last;
        } else {
            oldlast.next = last;
        }
        length++;

    }

    public byte[] deQueue() {
        if (isEmpty()) {
            last = null;
            return null;
        }
        byte[] values = first.item;
        first = first.next;
        length--;
        return values;
    }

    private class Node {
        byte[] item;
        Node next;
    }
}
