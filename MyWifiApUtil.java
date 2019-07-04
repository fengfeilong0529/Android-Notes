package com.znykt.facewifitool.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.hwangjr.rxbus.RxBus;
import com.znykt.facewifitool.bean.RxApInfo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MyWifiApUtil {
    private static final String TAG = "MyWifiApUtil";
    private WifiManager.LocalOnlyHotspotReservation mReservantion;

    private MyWifiApUtil() {
    }

    private static MyWifiApUtil mInstance;

    public static MyWifiApUtil getInstance() {
        if (mInstance == null) {
            synchronized (MyWifiApUtil.class) {
                if (mInstance == null) {
                    mInstance = new MyWifiApUtil();
                }
            }
        }
        return mInstance;
    }

    /**
     * 创建Wifi热点
     */
    public boolean createWifiAp(Context context, String ssid, String psk) {
        WifiManager wifiManager = WifiUtil.getWifiManager(context);
        //如果wifi处于打开状态，则关闭wifi,
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
        if (isWifiApEnable(context)) {
            closeWifiAp(context);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mReservantion != null) {
                mReservantion.close();
                mReservantion = null;
            }
        }

        WifiConfiguration config = new WifiConfiguration();
        if (TextUtils.isEmpty(psk)) {
            config.SSID = ssid;
            config.hiddenSSID = false;
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else {
            config.SSID = ssid;
            config.hiddenSSID = false;
            config.preSharedKey = psk;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);//开放系统认证
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        try {
            //Android 8.0以上开启热点
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                openApAfterAndroidO(context);
                return true;
            } else {
                //通过反射调用设置热点
                Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                boolean result = (Boolean) method.invoke(wifiManager, config, true);
                return result;
            }
        } catch (Exception e) {
            CatchUtil.handle(e);
            return false;
        }
    }

    /**
     * android 8.0前 创建Wifi热点
     */
    public boolean createWifiApBeforeAndroidO(Context context, String ssid, String psk) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        //如果wifi处于打开状态，则关闭wifi,
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
        if (isWifiApEnable(context)) {
            closeWifiAp(context);
        }

        WifiConfiguration config = new WifiConfiguration();
        if (TextUtils.isEmpty(psk)) {
            config.SSID = ssid;
            config.hiddenSSID = false;
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else {
            config.SSID = ssid;
            config.hiddenSSID = false;
            config.preSharedKey = psk;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);//开放系统认证
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        try {
            //通过反射调用设置热点
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            boolean result = (Boolean) method.invoke(wifiManager, config, true);
            return result;
        } catch (Exception e) {
            CatchUtil.handle(e);
            return false;
        }
    }

    /**
     * Android 8.0以上开启热点
     *
     * @param context
     */
    public void openApAfterAndroidO(Context context) {
        try {
            WifiUtil.getWifiManager(context).startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    mReservantion = reservation;
                    String ssid = reservation.getWifiConfiguration().SSID;
                    String psk = reservation.getWifiConfiguration().preSharedKey;
                    Log.d(TAG, "onStarted: " + ssid + "   psk:" + psk);
                    RxBus.get().post(new RxApInfo(ssid, psk));
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    Log.d(TAG, "onStopped: ");
                }

                @Override
                public void onFailed(int reason) {
                    super.onFailed(reason);
                    Log.d(TAG, "onFailed: " + reason);
                }
            }, new Handler());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeWifiAp2(Context context) {
        try {
            WifiManager wifiManager = WifiUtil.getWifiManager(context);
            //如果wifi处于打开状态，则关闭wifi,
            if (wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (mReservantion != null) {
                    mReservantion.close();
                    mReservantion = null;
                }
            }
            Method method = wifiManager.getClass().getMethod("cancelLocalOnlyHotspotRequest");
            method.setAccessible(true);
            method.invoke(wifiManager);
            System.out.println("已关闭热点");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * 适配于Android_O上关闭热点的方法
     */
    public void closeWifiApAfterAndroidO(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Field iConnMgrField;
        try {
            iConnMgrField = connManager.getClass().getDeclaredField("mService");
            iConnMgrField.setAccessible(true);
            Object iConnMgr = iConnMgrField.get(connManager);
            Class<?> iConnMgrClass = Class.forName(iConnMgr.getClass().getName());
            Method stopTethering = iConnMgrClass.getMethod("stopTethering", int.class);
            stopTethering.invoke(iConnMgr, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 关闭WiFi热点
     */
    public boolean closeWifiAp(Context context) {
        try {
            WifiManager wifiManager = WifiUtil.getWifiManager(context);
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            method.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManager);
            Method method2 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            boolean result = (Boolean) method2.invoke(wifiManager, config, false);
            return result;
        } catch (Exception e) {
            CatchUtil.handle(e);
            return false;
        }
    }


    //是否启用中
    public boolean isWifiApEnable(Context context) {
        try {
            WifiManager wifiManager = WifiUtil.getWifiManager(context);
            Method method = wifiManager.getClass().getMethod("getWifiApState");
            Integer result = (Integer) method.invoke(wifiManager);
//            return result ==WifiManager.WIFI_AP_STATE_ENABLED;
            return result == 13;
        } catch (Exception e) {
            CatchUtil.handle(e);
            return false;
        }
    }

    //是否关闭中  与启用中 不是完全相反  还有 enabling  和 closing 状态
    public boolean isWifiApClosed(Context context) {
        try {
            WifiManager wifiManager = WifiUtil.getWifiManager(context);
            Method method = wifiManager.getClass().getMethod("getWifiApState");
            Integer result = (Integer) method.invoke(wifiManager);
            return result == 11;
        } catch (Exception e) {
            CatchUtil.handle(e);
            return false;
        }
    }


    /**
     * 获取热点数据的历史配置
     *
     * @param context
     * @return 热点配置
     */
    public WifiConfiguration getApConfiguration(Context context) {
        try {
            WifiManager wifiManager = WifiUtil.getWifiManager(context);
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            method.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManager);
            return config;
        } catch (Exception e) {
            CatchUtil.handle(e);
            return null;
        }
    }

    public String getApIp() throws SocketException {
        String ipAddress = "";
        String maskAddress = "";
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();

        while (en.hasMoreElements()) {
            NetworkInterface intf = en.nextElement();
            if (intf.getName().contains("wlan") || intf.getName().contains("ap")) {
                Enumeration<InetAddress> addresses = intf.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        Log.e("WifiAp", "网卡接口名称：" + intf.getName());
                        Log.e("WifiAp", "网卡接口地址：" + addr.getHostAddress());
                        return addr.getHostAddress();
                    }
                }
            }
        }
        return null;
    }

    private String calcMaskByPrefixLength(int length) {
        int mask = -1 << (32 - length);
        int partsNum = 4;
        int bitsOfPart = 8;
        int maskParts[] = new int[partsNum];
        int selector = 0x000000ff;

        for (int i = 0; i < maskParts.length; i++) {
            int pos = maskParts.length - 1 - i;
            maskParts[pos] = (mask >> (i * bitsOfPart)) & selector;
        }

        String result = "";
        result = result + maskParts[0];
        for (int i = 1; i < maskParts.length; i++) {
            result = result + "." + maskParts[i];
        }
        return result;
    }
}
