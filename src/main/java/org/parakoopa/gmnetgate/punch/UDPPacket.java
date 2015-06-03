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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

/**
 * Handles incoming UDP packets.
 * UDP is connectionless. The TCPConnection class handles all packets of one connection, this here handles
 * ALL packets of ALL "connections".
 * Stores server and client ports that are sent in the TCPConnection class when a client requests connection.
 * @author Parakoopa
 */
public class UDPPacket {

    private final DatagramSocket server;
    private final Mediator main;
    private DatagramPacket packet;
    /** Game Maker Studio seperates strings in buffers with this char (buffer_string). */
    private char gm_string_seperator = 0;

    /**
     * Set's up a new connection listener that handles all packets of one connection.
     * @param main Mediator class instance that this server was created with.
     * @param packet Packet that we should maintain.
     * @param server Our UDP server socket the client is sending to. (not actually used)
     */
    public UDPPacket(Mediator main, DatagramPacket packet, DatagramSocket server) {
        this.server = server;
        this.main = main;
        this.packet = packet;
    }

    /** Helper method. 
     *  This will remove zeros at the end of our packet.
     *  @args bytes Byte-array to trim
     *  @return Trimmed byte-array
     */
    static byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }

    /**
     * Starts the processing of a packet.
     * We can't respond because we assume the client/server is behind a very
     * unfriendly nat. We use the TCP connection for answering.
     */
    public void run() {
        //Prepare command
        byte[] inputData = trim(packet.getData());
        //Strip of GM:Studio Header
        // - For tcp connections we use network_send_raw in GM:Studio. This means GM will not send the
        //   protocol header
        // - For udp connections, there is no command like this yet. We do it manually.
        //   The protocol header is exactly 12 bytes long so we cut the first 12 bytes of.
        inputData = Arrays.copyOfRange(inputData, 12, inputData.length);
        //Split the command into multiple commands, like this we can send multiple commands or commands+arguments
        //in one packet. Not used for this server, since the udp part only needs one command for client and servers.
        String[] inputLine = new String(inputData).split("\\r?\\n");
        //
        String debug_string = packet.getAddress().getHostAddress()+":"+packet.getPort()+" - UDP - ";
        Mediator.log(debug_string+" Recieved data!",true);
        //PROCESS COMMAND
        try {
            switch (inputLine[0]) {
                case "reg":
                    //A server wants to register. Save the IP of the server to the map.
                    Mediator.log(debug_string+" Server registered!",false);
                    Server serverObj = this.main.getServer(packet.getAddress().getHostAddress());
                    serverObj.setPort(packet.getPort());
                    //Make server invalid for now (see Lobby for more details (in TCPConnection))
                    serverObj.setData1("<INV>");
                    
                break;
                case "connect":
                    //A client wants to register. Save the IP of the server to the map.
                    Mediator.log(debug_string + " Client registered!",false);
                    Client clientObj = this.main.getClient(packet.getAddress().getHostAddress());
                    clientObj.setPort(packet.getPort());
                    break;
                default:
                    Mediator.log(debug_string+" Unknown command "+inputLine[0],true);
            }
            //We are done here. The rest gets done in the TCPConnection class.
            //There the socket of the server gets saved to respond to it, and the connect
            //command of the client is used there to actually send the information to the clients
        } catch (Exception ex) {
            Mediator.log(debug_string+" Could not read packet - Error: "+ex.getClass().getName(),true);
        }
    }
}
