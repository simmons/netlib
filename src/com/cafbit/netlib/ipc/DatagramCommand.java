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

package com.cafbit.netlib.ipc;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class DatagramCommand implements Command {
    
    private Thread thread;
    private DatagramSocket socket;
    private DatagramPacket datagramPacket;

    public DatagramCommand(Thread thread, DatagramSocket socket, DatagramPacket datagramPacket) {
        this.thread = thread;
        this.socket = socket;
        this.datagramPacket = datagramPacket;
    }
    
    public Thread getThread() {
        return thread;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public DatagramPacket getDatagramPacket() {
        return datagramPacket;
    }

}
