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

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.cafbit.netlib.dns.DNSMessage;
import com.cafbit.netlib.ipc.Command;

/**
 * Encapsulate packet details that we are interested in.
 * @author simmons
 */
public class MDNSPacketEntry extends PacketEntry implements Command {
    public DNSMessage message;
    public MDNSPacketEntry(DatagramPacket dp, DatagramSocket socket, DNSMessage message) {
        super(dp,socket);
        this.message = message;
    }
    public String toString() {
        return message.toString().trim();
    }
}
