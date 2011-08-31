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

package com.cafbit.netlib.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.cafbit.netlib.Util;

/**
 * This class represents a DNS "answer" component.
 * @author simmons
 */
public class DNSAnswer extends DNSComponent {
    
    public String name;
    public Type type;
    public int ttl;
    public byte[] rdata;
    public Data data;
    
    public abstract class Data {};
    public class A extends Data {
        public InetAddress address;
        public A(InetAddress address) {
            this.address = address;
        }
        public String toString() {
            return address.toString();
        }
    }
    public class TXT extends Data {
        public List<String> lines;
        public TXT(List<String> lines) {
            this.lines = lines;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<lines.size(); i++) {
                sb.append(lines.get(i));
                if (i < (lines.size()-1)) {
                    sb.append(" // ");
                }
            }
            return sb.toString();
        }
    }
    public class PTR extends Data {
        public String name;
        public PTR(String name) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
    }
    public class SRV extends Data {
        public short priority;
        public short weight;
        public short port;
        public String name;
        public SRV(short priority, short weight, short port, String name) {
            this.priority = priority;
            this.weight = weight;
            this.port = port;
            this.name = name;
        }
        public String toString() {
            return ""+priority+"/"+weight+"/"+port+" "+name;
        }
    };
    
    public DNSAnswer(DNSBuffer buffer) {
        parse(buffer);
    }

    @Override
    public int length() {
        // TODO: implement
        return 0;
    }

    @Override
    public void serialize(DNSBuffer buffer) {
        // TODO: implement
    }

    private void parse(DNSBuffer buffer) {
        name = buffer.readName();
        short typeNumber = buffer.readShort();
        type = Type.getType(typeNumber);
        
        // the most significant bit of the rrclass is special
        // in Multicast DNS -- it is used as a "cache flush" bit,
        // and only the least significant 15 bits should be used
        // as the class.
        // see:
        //   http://tools.ietf.org/html/draft-cheshire-dnsext-multicastdns-05
        //   section 11.3
        int aclass = buffer.readShortAsInt();
        //boolean cacheFlush = ((aclass & 0x8000) != 0);
        aclass = aclass & 0x7FFF;
        if (aclass != 1) {
            throw new DNSException("only class IN supported.  (got "+aclass+")");
        }
        
        ttl = buffer.readInteger();
        rdata = buffer.readRdata();
        
        if (type.equals(Type.A) || type.equals(Type.AAAA)) {
            try {
                data = new A(InetAddress.getByAddress(rdata));
            } catch (UnknownHostException e) {
                throw new DNSException("problem parsing rdata");
            }
        } else if (type.equals(Type.TXT)) {
            List<String> lines = new ArrayList<String>();
            for (int i=0; i<rdata.length; ) {
                int length = rdata[i++];
                String line = DNSBuffer.bytesToString(rdata, i, length);
                lines.add(line);
                i += length;
            }
            data = new TXT(lines);
        } else if (type.equals(Type.PTR)) {
            // rewind the buffer to the beginning of the
            // name (just after the 16-bit name-length field)
            // and reparse the name to allow for compression
            // offsets.
            int oldoffset = buffer.offset;
            buffer.offset -= rdata.length;
            data = new PTR(buffer.readName());
            if (oldoffset != buffer.offset) {
                throw new DNSException("bad PTR rdata");
            }
        } else if (type.equals(Type.SRV)) {
            short priority = (short)((rdata[0]&0xFF)<<8 | (rdata[1]&0xFF));
            short weight = (short)((rdata[2]&0xFF)<<8 | (rdata[3]&0xFF));
            short port = (short)((rdata[4]&0xFF)<<8 | (rdata[5]&0xFF));
            // rewind the buffer to the beginning of the
            // name (just after the 16-bit name-length field)
            // and reparse the name to allow for compression
            // offsets.
            int oldoffset = buffer.offset;
            buffer.offset -= rdata.length-6;
            String name = buffer.readName();
            if (oldoffset != buffer.offset) {
                throw new DNSException("bad PTR rdata");
            }
            data = new SRV(priority, weight, port, name);
        }

    }
    
    public String toString() {
        return name+" "+type.toString()+" "+getRdataString();
    }

    public String getRdataString() {
        if (data != null) {
            return data.toString();
        } else {
            return "data["+rdata.length+"]";
        }
    }
}