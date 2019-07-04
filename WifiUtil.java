package com.znykt.facewifitool.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import com.znykt.facewifitool.bean.IListener;
import com.znykt.facewifitool.main.AppInstance;

import java.util.List;

/**
 * Wifi 工具类
 * Created by EthanCo on 2016/4/23.
 */
public class WifiUtil {

    public static WifiManager getWifiManager(Context context) {
        return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public static WifiInfo getWifiInfo(Context context) {
        WifiInfo info = getWifiManager(context).getConnectionInfo();
        return info;
    }

    // 打开WIFI
    public static boolean openWifi(Context context) {
        if (!MyWifiApUtil.getInstance().isWifiApClosed(context)) {
            MyWifiApUtil.getInstance().closeWifiAp(context);
        }

        WifiManager wifiManager = getWifiManager(context);
        if (!wifiManager.isWifiEnabled()) {
            return wifiManager.setWifiEnabled(true);
        } else {
            return true;
        }
    }

    // 关闭WIFI
    public static boolean closeWifi(Context context) {
        WifiManager wifiManager = getWifiManager(context);
        if (wifiManager.isWifiEnabled()) {
            return wifiManager.setWifiEnabled(false);
        } else {
            return true;
        }
    }

    // 判断当前wifi是否打开，如果没打开，则打开，如果打开了，则判断是否打开当前的ssid,如果没有，则打开当前指定的ssid即可；
    public static void openWifiConnect(Context context, final String ssid, final String password, boolean reconnect, final IListener<Boolean> listener) {
        if (!MyWifiApUtil.getInstance().isWifiApClosed(context)) {
            MyWifiApUtil.getInstance().closeWifiAp(context);
        }

        final WifiManager wifiManager = getWifiManager(context);

        if (wifiManager.isWifiEnabled()) {
            WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (!reconnect && connectionInfo.getSSID().equals("\"" + ssid + "\"")) {
                listener.onResult(true);
            } else {
                boolean result = reConnectWifiInfo(wifiManager, ssid, password, TextUtils.isEmpty(password) ? WifiCipherType.WIFICIPHER_NOPASS : WifiCipherType.WIFICIPHER_WPA); //密码为type_3
                listener.onResult(result);
            }
        } else {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    wifiManager.setWifiEnabled(true);
                    boolean result = reConnectWifiInfo(wifiManager, ssid, password, TextUtils.isEmpty(password) ? WifiCipherType.WIFICIPHER_NOPASS : WifiCipherType.WIFICIPHER_WPA); //密码为type_3
                    listener.onResult(result);
                }
            }.start();
        }
    }

    public static boolean isConnectBySSID(Context context, String ssid) {
        WifiManager wifiManager = getWifiManager(context);
        if (wifiManager.isWifiEnabled()) {
            // 是否是 wifi 的连接
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo != null) {
                WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                return connectionInfo.getSSID().equals("\"" + ssid + "\"");
            }
        }
        return false;
    }

    public static void startScan(Context context) {
        WifiManager wifiManager = getWifiManager(context);
        wifiManager.startScan();
    }

    // 得到网络列表
    public static List<ScanResult> getWifiList(Context context) {
        WifiManager wifiManager = getWifiManager(context);
        WifiUtil.startScan(AppInstance.getApp());
        List<ScanResult> mWifiList = wifiManager.getScanResults();
        return mWifiList;
    }


    // 得到接入点的BSSID
    public static String getBSSID(Context context) {
        WifiInfo info = getWifiInfo(context);
        return (info == null) ? "NULL" : info.getBSSID();
    }

    // 得到IP地址
    public static int getIPAddress(Context context) {
        WifiInfo info = getWifiInfo(context);
        return (info == null) ? 0 : info.getIpAddress();
    }

    // 得到WifiInfo的所有信息包
    public static String getWifiInfoData(Context context) {
        WifiInfo info = getWifiInfo(context);
        return (info == null) ? "NULL" : info.toString();
    }

    // 断开指定ID的网络
    public void disconnectWifi(Context context, int netId) {
        WifiManager wifiManager = getWifiManager(context);
        wifiManager.disableNetwork(netId);
        wifiManager.disconnect();
    }

    //定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
    public static enum WifiCipherType {
        WIFICIPHER_WEP, //WEP
        WIFICIPHER_WPA, //WPA
        WIFICIPHER_NOPASS, //没有密码
        WIFICIPHER_INVALID //无效的
    }

    /**
     * 创建 一个 WifiConfiguration，之后调用wifiManager.addNetwork保存wifi密码
     *
     * @param SSID
     * @param password
     * @param type
     * @return
     */
    private static boolean reConnectWifiInfo(WifiManager wifiManager, String SSID, String password, WifiCipherType type) {
        romoveExistConfig(wifiManager, SSID);

        WifiConfiguration config = buildConfiguration(SSID, password, type);
        int networkId = wifiManager.addNetwork(config);
        return wifiManager.enableNetwork(networkId, true);
    }

    private static WifiConfiguration buildConfiguration(String SSID, String password, WifiCipherType type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        switch (type) {
            case WIFICIPHER_NOPASS:
                config.wepKeys[0] = "\"" + "\"";
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
                break;
            case WIFICIPHER_WEP:
                config.hiddenSSID = true;
                config.wepKeys[0] = "\"" + password + "\"";
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
                break;
            case WIFICIPHER_WPA:
                config.preSharedKey = "\"" + password + "\"";
                config.hiddenSSID = true;
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.status = WifiConfiguration.Status.ENABLED;
                break;
        }
        return config;
    }

    public static WifiConfiguration isExist(WifiManager wifiManager, String SSID) { // 查看以前是否已经配置过该SSID
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        if (existingConfigs == null || existingConfigs.size() == 0) {
            return null;
        }
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    private static void romoveExistConfig(WifiManager wifiManager, String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        if (existingConfigs == null || existingConfigs.size() == 0) {
            return;
        }
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                removeNetId(wifiManager, existingConfig.networkId);
            }
        }
    }

    private static boolean removeNetId(WifiManager wifiManager, int id) {
        boolean result = wifiManager.removeNetwork(id);
        wifiManager.saveConfiguration();
        return result;
    }

    public static boolean isNoPsk(ScanResult sr) {
        String capabilities = sr.capabilities.trim();
        if (capabilities != null) {
            capabilities = capabilities.replaceAll("\\[WPS\\]", "");
            capabilities = capabilities.replaceAll("\\[ESS\\]", "");
            if (TextUtils.isEmpty(capabilities)) {
                return true;
            }
        }
        return false;
    }

    //wifiInfo.getIpAddress
    public static String intToIp(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }
}
