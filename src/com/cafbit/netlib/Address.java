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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Address implements Comparable<Address> {
    
    private InetAddress inetAddress;
    private InterfaceInfo interfaceInfo;
    private int version;
    private long prefix; // consider this to be 32-bit unsigned.
    private boolean isPrivate = false;
    private boolean isMulticast = false;
    private boolean isLoopback = false;
    private boolean isLinkLocal = false;
    private boolean isAny = false;
    private boolean isEthernet = false;
    
    public Address(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
        this.interfaceInfo = null;
        study();
    }
    
    public Address(InetAddress inetAddress, InterfaceInfo interfaceInfo) {
        this.inetAddress = inetAddress;
        this.interfaceInfo = interfaceInfo;
        study();
    }
    
    public void setInterfaceInfo(InterfaceInfo interfaceInfo) {
        this.interfaceInfo = interfaceInfo;
    }
    
    private boolean testPrefix(long network, int netmaskBits) {
        long mask = ((~(0xFFFFFFFFL >> netmaskBits)) & 0xFFFFFFFFL);
        return ((prefix & mask) == (network & mask));
    }
    
    private void study() {
        // family
        if (inetAddress instanceof Inet4Address) {
            version = 4;
        } else if (inetAddress instanceof Inet6Address) {
            version = 6;
        } else {
            version = 0;
        }
        
        // prefix (for simple numeric comparison purposes) --
        // use the most significant 32 bits, regardless
        // of the address family.
        byte[] bytes = inetAddress.getAddress();
        if (bytes.length >= 4) {
            prefix =
                (((int)(bytes[0] & 0xFF)) << 24) |
                (((int)(bytes[1] & 0xFF)) << 16) |
                (((int)(bytes[2] & 0xFF)) << 8) |
                ((int)(bytes[3] & 0xFF));
        }
        
        if (version == 4) {
            
            // private
            if (testPrefix(0x0A000000, 8) ||
                testPrefix(0xAC100000, 12) ||
                testPrefix(0xC0A80000, 16)
            ) {
                isPrivate = true;
            }
            
            // multicast
            if (testPrefix(0xE0000000, 4)) {
                isMulticast = true;
            }
            
            // loopback
            if (testPrefix(0x7F000000, 8)) {
                isLoopback = true;
            }
            
            // link-local
            if (testPrefix(0xA9FE0000, 16)) {
                // the first and last network within this
                // prefix are not considered link-local.
                int octet = (int)((prefix>>8) & 0xFF);
                if (octet > 0 && octet < 255) {
                    isLinkLocal = true;
                }
            }
            
        } else if (version == 6) {
            
            // private
            if (testPrefix(0xFC000000, 7)) {
                isPrivate = true;
            }
            
            // multicast
            if (testPrefix(0xFF000000, 8)) {
                isMulticast = true;
            }
            
            // loopback
            isLoopback = true;
            for (int i=0; i<bytes.length; i++) {
                if (i != 15) {
                    if (bytes[i] != 0) {
                        isLoopback = false;
                        break;
                    }
                } else {
                    if (bytes[i] != 1) {
                        isLoopback = false;
                        break;
                    }                   
                }
            }
            
            // link-local
            if (testPrefix(0xFE800000, 10)) {
                isLinkLocal = true;
            }
        }
        
        // any
        isAny = true;
        for (int i=0; i<bytes.length; i++) {
            if (bytes[i] != 0) {
                isAny = false;
                break;
            }
        }
        
        // ethernet
        if (interfaceInfo != null) {
            if (interfaceInfo.isWifi() || interfaceInfo.isWired()) {
                isEthernet = true;
            }
        }
    }
    
    // family methods

    public boolean isIPv4() {
        return (inetAddress instanceof Inet4Address);
    }

    public boolean isIPv6() {
        return (inetAddress instanceof Inet6Address);
    }
    
    public int getIPVersion() {
        if (isIPv4()) {
            return 4;
        } else if (isIPv6()) {
            return 6;
        } else {
            return 0;
        }
    }
    
    public String getFamily() {
        switch(getIPVersion()) {
        case 4:
            return "IPv4";
        case 6:
            return "IPv6";
        default:
            return "unknown-family";
        }
    }
    
    // other getters
    
    public InetAddress getInetAddress() {
        return inetAddress;
    }
    
    public String getIPAddress() {
        return inetAddress.getHostAddress();
    }
    
    public boolean isPrivate() {
        return isPrivate;
    }
    
    public boolean isMulticast() {
        return isMulticast;
    }
    
    public boolean isLoopback() {
        return isLoopback;
    }
    
    public boolean isLinkLocal() {
        return isLinkLocal;
    }
    
    public boolean isAny() {
        return isAny;
    }
    
    public boolean isEthernet() {
        return isEthernet;
    }
    
    // string representation
    
    public String toString() {
        List<String> flags = new ArrayList<String>();
        if (isAny) {
            flags.add("any");
        }
        if (isLinkLocal) {
            flags.add("link-local");
        }
        if (isLoopback) {
            flags.add("loopback");
        }
        if (isMulticast) {
            flags.add("multicast");
        }
        if (isPrivate) {
            flags.add("private");
        }
        String flaglist = join(flags, " ");
        
        return String.format(
            "%s %s %s",
            inetAddress.getHostAddress(),
            getFamily(),
            flaglist
        );
    }
    
    private String join(Collection<String> strings, String delimiter) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : strings) {
            if (! first) {
                sb.append(delimiter);
            }
            sb.append(s);
            first = false;
        }
        return sb.toString();
    }

    // natural comparison -- 
    // this orders address according to decreasing "goodness":
    // 1. prefer unicast over multicast
    // 2. prefer wired/wireless ethernet addresses over non-ethernet
    // 3. prefer IPv4 over IPv6. (for now!)
    // 4. prefer non-link-local
    // 5. prefer non-private
    
    @Override
    public int compareTo(Address other) {
        
        // 1. multicast
        if (this.isMulticast && (!other.isMulticast)) {
            return +1;
        } else if ((!this.isMulticast) && other.isMulticast) {
            return -1;
        }
        
        // 2. wired/wireless ethernet vs. other (i.e. 3G/4G/etc.)
        if (this.isEthernet && (!other.isEthernet)) {
            return -1;
        } else if ((!this.isEthernet) && other.isEthernet) {
            return +1;
        }

        // 3. IPv4 vs. IPv6
        if (this.version < other.version) {
            return -1;
        } else if (this.version > other.version) {
            return +1;
        }
        
        // 4. prefer non-link-local
        if (this.isLinkLocal && (!other.isLinkLocal)) {
            return +1;
        } else if ((!this.isLinkLocal) && other.isLinkLocal) {
            return -1;
        }

        // 5. prefer non-private
        if (this.isPrivate && (!other.isPrivate)) {
            return +1;
        } else if ((!this.isPrivate) && other.isPrivate) {
            return -1;
        }

        // numerical comparison
        byte[] b1 = this.inetAddress.getAddress();
        byte[] b2 = other.inetAddress.getAddress();
        if (b1.length < b2.length) {
            return -1;
        } else if (b1.length > b2.length) {
            return +1;
        } else {
            for (int i=0; i<b1.length; i++) {
                int i1 = (int)(b1[i] & 0xFF);
                int i2 = (int)(b2[i] & 0xFF);
                if (i1 < i2) {
                    return -1;
                } else if (i1 > i2) {
                    return +1;
                }
            }
        }
        return 0; // equality
    }

    // equality
    
    @Override
    public int hashCode() {
        return inetAddress.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (! (other instanceof Address)) {
            return false;
        }
        return this.inetAddress.equals(((Address)other).inetAddress);
    }
    
}
