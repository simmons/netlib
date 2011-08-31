/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cafbit.netlib;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Various Android network utility methods
 * @author simmons
 */
public class NetUtil {
    
    public static final String TAG = "NetLib";
    private WifiManager wifiManager;
    
    public static class NetInfoException extends Exception {
        private static final long serialVersionUID = 5543786811674326615L;
        public NetInfoException() {}
        public NetInfoException(String message) {
            super(message);
        }
        public NetInfoException(Throwable e) {
            super(e);
        }
        public NetInfoException(String message, Throwable e) {
            super(message, e);
        }
    }
    
    public NetUtil(Context context) {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }
    
    public WifiManager getWifiManager() {
        return wifiManager;
    }   

    public static Set<InetAddress> getLocalAddresses() {
        Set<InetAddress> addresses = new HashSet<InetAddress>();
        
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            Log.v(TAG, "getNetworkInterfaces(): "+e.getMessage(), e);
            return null;
        }
        
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> addressEnum = networkInterface.getInetAddresses();
            while (addressEnum.hasMoreElements()) {
                addresses.add(addressEnum.nextElement());
            }
        }

        return addresses;
    }
    
    public List<InterfaceInfo> getNetworkInformation() throws NetInfoException {
        List<InterfaceInfo> interfaceList = new ArrayList<InterfaceInfo>();
        
        InetAddress wifiAddress = null;
        InetAddress reversedWifiAddress = null;
        if (wifiManager.isWifiEnabled()) {
            // get the ip address of the wifi interface
            int rawAddress = wifiManager.getConnectionInfo().getIpAddress();
            try {
                wifiAddress = InetAddress.getByAddress(new byte[] {
                    (byte) ((rawAddress >> 0) & 0xFF),
                    (byte) ((rawAddress >> 8) & 0xFF),
                    (byte) ((rawAddress >> 16) & 0xFF),
                    (byte) ((rawAddress >> 24) & 0xFF),
                });
                // It's unclear how to interpret the byte order
                // of the WifiInfo.getIpAddress() int value, so
                // we also compare with the reverse order.  The
                // result is probably consistent with ByteOrder.nativeOrder(),
                // but we don't know for certain since there's no documentation.
                reversedWifiAddress = InetAddress.getByAddress(new byte[] {
                    (byte) ((rawAddress >> 24) & 0xFF),
                    (byte) ((rawAddress >> 16) & 0xFF),
                    (byte) ((rawAddress >> 8) & 0xFF),
                    (byte) ((rawAddress >> 0) & 0xFF),
                });
            } catch (UnknownHostException e) {
                throw new NetInfoException("problem retreiving wifi ip address", e);
            }
        }
        
        InetAddress localhost;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (Exception e) {
            throw new NetInfoException("cannot determine the localhost address", e);
        }

        // get a list of all network interfaces
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new NetInfoException("problem getting net interfaces", e);
        }

        // find the wifi network interface based on the ip address
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            int flags = 0;
            Enumeration<InetAddress> addressEnum = networkInterface.getInetAddresses();
            List<Address> addresses = new ArrayList<Address>();
            while (addressEnum.hasMoreElements()) {
                InetAddress address = addressEnum.nextElement();

                // check for localhost
                if (address.equals(localhost)) {
                    flags |= InterfaceInfo.NET_LOCALHOST;
                }
                
                // check for wifi
                if ( (wifiAddress != null) &&
                     (reversedWifiAddress != null) &&
                     (address.equals(wifiAddress) || address.equals(reversedWifiAddress))
                ) {
                    flags |= InterfaceInfo.NET_WIFI;
                }
                
                addresses.add(new Address(address));
            }
            
            // assume an eth* interface that isn't wifi is wired ethernet.
            if (((flags & InterfaceInfo.NET_WIFI)==0) && networkInterface.getName().startsWith("eth")) {
                flags |= InterfaceInfo.NET_WIRED;
            }

            interfaceList.add(new InterfaceInfo(networkInterface, addresses, flags));
        }
        return interfaceList;
    }
    
    public NetworkInterface getFirstWifiInterface() {
        try {
            for (InterfaceInfo ii : getNetworkInformation()) {
                if (ii.isWifi()) {
                    return ii.getNetworkInterface();
                }
            }
        } catch (NetInfoException e) {
            Log.w(TAG, "cannot find a wifi interface");
        }
        return null;
    }

    public NetworkInterface getFirstWifiOrEthernetInterface() {
        try {
            for (InterfaceInfo ii : getNetworkInformation()) {
                if (ii.isWifi() || ii.isWired()) {
                    return ii.getNetworkInterface();
                }
            }
        } catch (NetInfoException e) {
            Log.w(TAG, "cannot find a wifi/ethernet interface");
        }
        return null;
    }

    public String getNetworkInformationString() {
        NetworkInterface inUseInterface = getFirstWifiOrEthernetInterface();
        List<InterfaceInfo> lii;
        try {
            lii = getNetworkInformation();
        } catch (NetInfoException e) {
            return "Error fetching network information:\n" + e.getMessage();
        }
        
        // reorder interface list with the in-use interface shown first
        List<InterfaceInfo> lii2 = new ArrayList<InterfaceInfo>(lii.size());
        if (inUseInterface != null) {
            for (InterfaceInfo ii : lii) {
                if (ii.getNetworkInterface().equals(inUseInterface)) {
                    lii2.add(ii);
                    break;
                }
            }
        }
        for (InterfaceInfo ii : lii) {
            if ((inUseInterface == null) || (! ii.getNetworkInterface().equals(inUseInterface))) {
                lii2.add(ii);
            }
        }
        lii = lii2;
        
        StringBuilder sb = new StringBuilder();
        for (InterfaceInfo ii : lii) {
            NetworkInterface ni = ii.getNetworkInterface();
            sb.append("interface "+ni.getName()+":");
            if ((inUseInterface != null) && (inUseInterface.equals(ni))) {
                sb.append(" *");
            }
            if (ii.isLocalhost()) {
                sb.append(" localhost");
            }
            if (ii.isWifi()) {
                sb.append(" wifi");
            }
            if (ii.isWired()) {
                sb.append(" wired");
            }
            sb.append('\n');
            for (Address a : ii.getAddresses()) {
                sb.append("    "+a.toString()+"\n");
            }
        }
        
        return sb.toString();
    }

    public static boolean testIPv6Support() {
        try {
            // attempt to connect to ipv6-localhost
            InetAddress ipv6Localhost =
                InetAddress.getByAddress(new byte[] {0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,1});
            Socket socket = new Socket(ipv6Localhost, 4242);
            socket.close();
        } catch (ConnectException e) {
            // "connection refused", so the stack is okay with IPv6.
            return true;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
