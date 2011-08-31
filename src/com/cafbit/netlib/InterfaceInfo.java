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

/**
 * 
 */
package com.cafbit.netlib;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class InterfaceInfo {
    private NetworkInterface networkInterface;
    private SortedSet<Address> addresses;
    private int flags = 0;
    public static final int NET_LOCALHOST = 1<<0;
    public static final int NET_WIFI      = 1<<1;
    public static final int NET_WIRED     = 1<<2;
    
    public InterfaceInfo(NetworkInterface networkInterface, List<Address> addresses, int flags) {
        this.networkInterface = networkInterface;
        this.addresses = new TreeSet<Address>(addresses);
        this.flags = flags;
        
        for (Address a : addresses) {
            a.setInterfaceInfo(this);
        }
    }
    
    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }
    public SortedSet<Address> getAddresses() {
        return addresses;
    }
    public int getFlags() {
        return flags;
    }
    public boolean isLocalhost() {
        return ((flags & NET_LOCALHOST) != 0);
    }
    public boolean isWifi() {
        return ((flags & NET_WIFI) != 0);
    }
    public boolean isWired() {
        return ((flags & NET_WIRED) != 0);
    }
    
    public static boolean isAddressLinkLocal(InetAddress address) {
        // check for link-local
        byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address) {
            // ipv6 - fe80::/10
            if ((bytes[0] == 0xFE) && ((bytes[1] & 0xC0) == 0x80)) {
                return true;
            }
        } else if (address instanceof Inet4Address) {
            // ipv4 - 169.254.1.0 - 169.254.254.255
            if ((bytes[0] == 169) && (bytes[1] == 254) && (bytes[2] > 0) && (bytes[2] < 255)) {
                return true;
            }
        }
        return false;
    }
    
    public String getFlagStrings() {
        StringBuilder sb = new StringBuilder();
        if ((flags & NET_LOCALHOST)!=0) { sb.append("localhost "); }
        if ((flags & NET_WIFI)!=0) { sb.append("wifi "); }
        if ((flags & NET_WIRED)!=0) { sb.append("wired "); }
        if (sb.length()>0) {
            sb.setLength(sb.length()-1);
        }
        return sb.toString();
    }
    
    public static String getAddressFlagStrings(InetAddress address) {
        StringBuilder sb = new StringBuilder();
        if (address instanceof Inet4Address) {
            sb.append("IPv4 ");
        } else if (address instanceof Inet6Address) {
            sb.append("IPv6 ");
        } else {
            sb.append("unknown-family ");
        }
        if (isAddressLinkLocal(address)) {
            sb.append("link-local ");
        }
        if (sb.length()>0) {
            sb.setLength(sb.length()-1);
        }
        return sb.toString();           
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("interface "+networkInterface+" : ");
        sb.append(getFlagStrings());
        sb.append("\n");
        for (Address address : addresses) {
            sb.append("  "+address+"\n");
        }
        return sb.toString();
    }
    
}