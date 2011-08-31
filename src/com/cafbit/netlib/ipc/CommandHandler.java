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

import android.os.Handler;
import android.os.Message;

public class CommandHandler extends Handler {
    
    public static final int IPCHANDLER_COMMAND = 1;
    
    protected CommandListener commandListener;
    
    public CommandHandler(CommandListener commandListener) {
        this.commandListener = commandListener;
    }
    
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (msg.what == IPCHANDLER_COMMAND) {
            Command command = (Command)msg.obj;
            commandListener.onCommand(command);
        }
    }
    
    public void sendCommand(Command command) {
        sendMessage(Message.obtain(this, IPCHANDLER_COMMAND, command));
    }
    
    // helper methods

    public void error(Throwable throwable) {
        sendMessage(Message.obtain(this, IPCHANDLER_COMMAND, new ErrorCommand(throwable)));
    }
    public void error(String message, Throwable throwable) {
        sendMessage(Message.obtain(this, IPCHANDLER_COMMAND, new ErrorCommand(message, throwable)));
    }

}
