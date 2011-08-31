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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;

import com.cafbit.netlib.dns.DNSMessage;

public class MDNSReceiverThread extends MulticastReceiverThread {

    // the standard mDNS multicast address and port number
    private static final byte[] MDNS_ADDR =
        new byte[] {(byte) 224,(byte) 0,(byte) 0,(byte) 251};
    private static final int MDNS_PORT = 5353;
    
    private Random random = new Random(System.currentTimeMillis());

    public MDNSReceiverThread(NetworkManagerThread networkManager) throws IOException {
        super(networkManager, MDNS_ADDR, MDNS_PORT);
    }

    @Override
    protected void handlePacket(DatagramPacket datagramPacket) {
        /*
        Log.v(TAG, String.format("received: offset=0x%04X (%d) length=0x%04X (%d)", datagramPacket.getOffset(), datagramPacket.getOffset(), datagramPacket.getLength(), datagramPacket.getLength()));
        Log.v(TAG, Util.hexDump(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength()));
        */
        
        // parse the DNS packet
        DNSMessage message = new DNSMessage(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength());

        // summarize the packet contents
        PacketEntry packetEntry = new MDNSPacketEntry(datagramPacket, getSocket(), message);
        
        // send the packet entry to the network manager
        System.out.println("MDNSReceiverThread: SENDING PACKETENTRY");
        getNetworkManagerThread().getHandler().sendCommand(packetEntry);
    }

    public void sendQuery(String name) throws IOException {
        byte[] requestData = (new DNSMessage(name)).serialize();
        DatagramPacket request =
            new DatagramPacket(requestData, requestData.length, InetAddress.getByAddress(MDNS_ADDR), MDNS_PORT);
        send(request);
    }
}
