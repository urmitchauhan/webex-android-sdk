package com.cisco.spark.android.util;

import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetworkUtils {

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address
                            && !intf.getDisplayName().startsWith("usbnet")) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("getLocalIpAddress()", ex.toString());
        }
        return null;
    }

    public static String resolveHostName(String hostName) {
        InetAddress serverAddr;
        try {
            serverAddr = InetAddress.getByName(hostName);
        } catch (java.net.UnknownHostException exception) {
            Log.e("resolveHostName()", exception.toString());
            return "";
        }

        return serverAddr.getHostAddress();
    }

}
