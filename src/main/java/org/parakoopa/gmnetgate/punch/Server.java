/*
 * Copyright (c) 2015 Marco Köpcke <parakoopa at live.de>.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.parakoopa.gmnetgate.punch;

import com.google.gson.annotations.Expose;
import java.net.Socket;

/**
 *
 * @author Marco
 */
public class Server {
     /**
     * Contains the IP of this server
     */
    @Expose private String ip = "";
     /**
     * Contains the Ports of the Server.
     * A port is assigned to an ip, so only one ip per server is possible.
     */
    private Integer port = 0;
    
    /**
     * Contains the TCP Sockets of the Server.
     * For sending the connection requests to the servers.
     */
    private Socket tcp_socket = null;
    /**
     * The 8 data strings.
     */
    @Expose private String data1 = "";
    @Expose private String data2 = "";
    @Expose private String data3 = "";
    @Expose private String data4 = "";
    @Expose private String data5 = "";
    @Expose private String data6 = "";
    @Expose private String data7 = "";
    @Expose private String data8 = "";
    
    /**
     * Time the server was created
     */
    @Expose private long createdTime;

    public Server(String ip) {
        this.createdTime = System.currentTimeMillis() / 1000L;
        this.ip = ip;
    }
    
     /**
     * Contains the Ports of the Server.
     * A port is assigned to an ip, so only one ip per server is possible.
     */
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Contains the TCP Sockets of the Server.
     * For sending the connection requests to the servers.
     */
    public Socket getTCPsocket() {
        return tcp_socket;
    }

    public void setTCPsocket(Socket tcp_socket) {
        this.tcp_socket = tcp_socket;
    }

    public String getData1() {
        return data1;
    }

    public void setData1(String data1) {
        this.data1 = data1;
    }

    public String getData2() {
        return data2;
    }

    public void setData2(String data2) {
        this.data2 = data2;
    }

    public String getData3() {
        return data3;
    }

    public void setData3(String data3) {
        this.data3 = data3;
    }

    public String getData4() {
        return data4;
    }

    public void setData4(String data4) {
        this.data4 = data4;
    }

    public String getData5() {
        return data5;
    }

    public void setData5(String data5) {
        this.data5 = data5;
    }

    public String getData6() {
        return data6;
    }

    public void setData6(String data6) {
        this.data6 = data6;
    }

    public String getData7() {
        return data7;
    }

    public void setData7(String data7) {
        this.data7 = data7;
    }

    public String getData8() {
        return data8;
    }

    public void setData8(String data8) {
        this.data8 = data8;
    }

    public long getCreatedTime() {
        return createdTime;
    }
    
}
