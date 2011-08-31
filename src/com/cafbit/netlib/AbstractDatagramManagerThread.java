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
import java.net.DatagramSocket;
import java.net.NetworkInterface;
import java.util.LinkedList;
import java.util.List;

import com.cafbit.netlib.MulticastReceiverThread;
import com.cafbit.netlib.NetUtil;
import com.cafbit.netlib.ipc.Command;
import com.cafbit.netlib.ipc.CommandListener;
import com.cafbit.netlib.ipc.DatagramCommand;
import com.cafbit.netlib.ipc.ErrorCommand;
import com.cafbit.netlib.ipc.CommandHandler;
import com.cafbit.netlib.ipc.QuitCommand;

import android.content.Context;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Looper;
import android.util.Log;

/**
 * This thread runs in the background while the user has our
 * program in the foreground, and handles sending mDNS queries
 * and processing incoming mDNS packets.
 * @author simmons
 */
public abstract class AbstractDatagramManagerThread extends Thread implements CommandListener,NetworkManagerThread {

    public static final String TAG = NetUtil.TAG;
    
    private static final String MULTICAST_LOCK_NAME = "cafbit";
    
    protected CommandHandler handler;
    private NetUtil netUtil;
    private NetworkInterface networkInterface;
    protected CommandHandler upstreamHandler;
    
    private List<ReceiverThread> receiverThreads =
        new LinkedList<ReceiverThread>();
    
    /**
     * Construct the network thread.
     * @param activity
     */
    public AbstractDatagramManagerThread(String threadName, Context context, CommandHandler upstreamHandler) {
        super(threadName);
        this.upstreamHandler = upstreamHandler;
        netUtil = new NetUtil(context);
    }
    
    /**
     * The main network loop.  Multicast DNS packets are received,
     * processed, and sent to the UI.
     * 
     * This loop may be interrupted by closing the multicastSocket,
     * at which time any commands in the commandQueue will be
     * processed.
     */
    @Override
    public final void run() {
        boolean useMulticast = false;
        MulticastLock multicastLock = null;
        
        Log.v(TAG, "starting network thread");

        // initialize the network
        try {
            networkInterface = netUtil.getFirstWifiOrEthernetInterface();
            System.out.println("MY NETWORK INTERFACE: "+networkInterface);
            if (networkInterface == null) {
                throw new IOException("Your WiFi is not enabled.");
            }
        } catch (IOException e1) {
            upstreamHandler.error("cannot initialize network", e1);
            return;
        }
        
        // Allow the subclass to perform initialization.
        // The subclass is expected to create receiver threads in its
        // init() method.
        try {
            init();
        } catch (IOException e1) {
            upstreamHandler.error(e1);
            return;
        }
        
        // set up the IPC
        Looper.prepare();
        this.handler = new CommandHandler(this);
        
        // do any receiver threads use multicast?
        for (ReceiverThread thread : receiverThreads) {
            if (thread instanceof MulticastReceiverThread) {
                useMulticast = true;
                break;
            }
        }
        
        // initialize multicast, if necessary
        if (useMulticast) {
            multicastLock = netUtil.getWifiManager().createMulticastLock(MULTICAST_LOCK_NAME);
            multicastLock.acquire();
        }
        
        // start child threads
        for (ReceiverThread thread : receiverThreads) {
            thread.start();
        }
        
        // allow handlers to perform any last-minute initialization
        beforeLoop();
        
        // loop!
        Looper.loop();
        System.out.println("QUITING MANAGER THREAD");

        // allow handlers to perform any cleanup
        afterLoop();

        // stop child threads
        for (ReceiverThread thread : receiverThreads) {
            thread.quit();
        }
        
        // finalize multicast, if necessary
        if (useMulticast) {
            // release the multicast lock
            multicastLock.release();
            multicastLock = null;
        }

        Log.v(TAG, "stopping network thread");
    }

    protected void beforeLoop() {}
    protected void afterLoop() {}
    
    protected abstract void init() throws IOException;
    protected abstract void handleIncoming(DatagramSocket socket, DatagramPacket response);
    
    //
    
    protected void addReceiverThread(ReceiverThread receiverThread) {
        receiverThreads.add(receiverThread);
    }
    
    protected List<ReceiverThread> getReceiverThreads() {
        return receiverThreads;
    }
    
    // implements NetworkManager

    public CommandHandler getHandler() {
        return handler;
    }
    public CommandListener getCommandListener() {
        return this;
    }
    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }
    
    ////////////////////////////////////////////////////////////
    // inter-process communication
    ////////////////////////////////////////////////////////////
    
    public void onCommand(Command command) {
        if (command instanceof QuitCommand) {
            Looper.myLooper().quit();
        } else if (command instanceof DatagramCommand) {
            DatagramCommand datagramCommand = (DatagramCommand)command;
            handleIncoming(datagramCommand.getSocket(), datagramCommand.getDatagramPacket());
        } else if (command instanceof ErrorCommand) {
            ErrorCommand errorCommand = (ErrorCommand)command;
            if (errorCommand.getMessage() == null) {
                upstreamHandler.error(errorCommand.getThrowable());
            } else {
                upstreamHandler.error(errorCommand.getMessage(), errorCommand.getThrowable());
            }
        }
    }

}
