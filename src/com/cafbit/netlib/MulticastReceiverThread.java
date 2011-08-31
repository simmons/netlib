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
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Set;

import com.cafbit.netlib.ipc.DatagramCommand;
import com.cafbit.netlib.ipc.ErrorCommand;
import com.cafbit.netlib.ipc.CommandHandler;

import android.util.Log;

public class MulticastReceiverThread extends Thread implements ReceiverThread {

    protected static final String TAG = NetUtil.TAG;
    private static final int BUFFER_SIZE = 4096;

    private NetworkManagerThread networkManagerThread;
    private CommandHandler managerHandler;
    private NetworkInterface networkInterface;
    private InetAddress groupAddress;
    private int port;
    
    private MulticastSocket socket;
    private Set<InetAddress> localAddresses;
    private boolean quitFlag = false;
    
    public MulticastReceiverThread(
            NetworkManagerThread networkManagerThread,
            byte[] groupAddress,
            int port
    ) throws IOException {
        super("multicast-receiver");
        this.networkManagerThread = networkManagerThread;
        this.managerHandler = networkManagerThread.getHandler();
        this.networkInterface = networkManagerThread.getNetworkInterface();
        this.groupAddress = InetAddress.getByAddress(groupAddress);
        this.port = port;
        openSocket();
        localAddresses = NetUtil.getLocalAddresses();
    }

    private void openSocket() throws IOException {
        socket = new MulticastSocket(port);
        socket.setTimeToLive(2); // TODO: should this be 1?
        socket.setReuseAddress(true);
        socket.setLoopbackMode(true); // "true" means don't loopback.  strange, but true!
        socket.setNetworkInterface(networkInterface);
        socket.joinGroup(groupAddress);
    }

    /**
     * The main network loop.  Multicast datagrams are received and
     * forwarded to the listener.  This thread may be terminated by
     * closing the multicast socket via quit().
     */
    @Override
    public void run() {
        
        // loop!
        while (true) {
            // set up the buffer for incoming packets.
            // it would be nice to have reusable buffers, but we are passing
            // these buffers to other threads.  we could set up a re-use
            // scheme, but it would be overkill.  this isn't designed to be
            // high-speed networking code.
            byte[] responseBuffer = new byte[BUFFER_SIZE];
            DatagramPacket response = new DatagramPacket(responseBuffer, BUFFER_SIZE);

            // receive a packet
            try {
                socket.receive(response);
            } catch (IOException e) {
                if (! quitFlag) {
                    Log.v(TAG, "quiting multicast thread due to exception.",e);
                }
                break;
            }
            
            // ignore our own packet transmissions.
            if (localAddresses.contains(response.getAddress())) {
                continue;
            }
            
            // pass the packet to the listener
            try {
                handlePacket(response);
            } catch (Exception e) {
                managerHandler.sendCommand(new ErrorCommand(e));
            }
        }

    }
    
    protected void handlePacket(DatagramPacket datagramPacket) {
        DatagramCommand datagramCommand = new DatagramCommand(this, socket, datagramPacket);
        managerHandler.sendCommand(datagramCommand);
    }
    
    /**
     * Use this thread's socket to send a packet.
     * @param packet
     * @throws IOException
     */
    public void send(DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

    /**
     * Ask the multicast receiver thread to quit, by closing the
     * socket.  This will interrupt the receive() and conclude the
     * thread. 
     */
    public void quit() {
        quitFlag = true;
        socket.close();
    }
    
    protected MulticastSocket getSocket() {
        return socket;
    }
    
    protected NetworkManagerThread getNetworkManagerThread() {
        return networkManagerThread;
    }
    
}
