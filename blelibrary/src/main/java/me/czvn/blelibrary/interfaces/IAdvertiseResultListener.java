package me.czvn.blelibrary.interfaces;

/**
 * Created by andy on 2016/2/17.
 *
 */
public interface IAdvertiseResultListener {
    /**
     * 这个方法会在广播成功时调用
     */
    void onAdvertiseSuccess();

    /**
     * 这个方法会在广播失败时调用
     * @param errorCode 请查阅AdvertiseCallback的API
     */
    void onAdvertiseFailed(int errorCode);
}
