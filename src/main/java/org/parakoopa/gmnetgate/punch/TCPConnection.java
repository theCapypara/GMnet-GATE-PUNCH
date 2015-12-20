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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Handles incoming TCP connections.
 * Stores server sockets and on client request send server ports to client and
 * client ports to server via the stored socket.
 * @author Parakoopa
 */
public class TCPConnection implements Runnable {

    private Mediator main;
    private Socket client;
    private ServerSocket server;
    /** Game Maker Studio seperates strings in buffers with this char (buffer_string). */
    private char gm_string_seperator = 0;
    /** True, if the "reg" command was used on this connection **/
    private boolean isServer = false;

    /**
     * Set's up a new connection listener that handles all packets of one connection.
     * @param main Mediator class instance that this server was created with.
     * @param client Socket that the client is connected to.
     * @param server Our TCP server socket the client is connected to. (not actually used)
     */
    public TCPConnection(Mediator main, Socket client, ServerSocket server) {
        this.server = server;
        this.client = client;
        this.main = main;
    }

    /**
     * Starts listening for incoming packets and responds to it.
     */
    @Override
    public void run() {
        String debug_string = this.client.getInetAddress().getHostAddress()+":"+this.client.getPort()+" | TCP | ";
        Mediator.log(debug_string+" Connected!",true);
        try {
            //TcpNoDelay configures the socket to transfer messages immediately, otherwise GM:S won't pick them up
            this.client.setTcpNoDelay(true);
            //Input and Output streams. We write bytes out and take Strings in.
            OutputStream out = client.getOutputStream();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));

            String inputLine;
            Server serverObj;
            //Process all packets. This while loop will stop when the peer disconnected.
            while ((inputLine = in.readLine()) != null) {
                //This will kill Threads (or commands) if the client don't send
                //all of the data expected. Needed otherwise this could lead
                //to many hanging threads.
                client.setSoTimeout(1000);
                //Cleans string, it might contain some garbage characters.
                inputLine = inputLine.replaceAll("\\p{C}", "");
                switch (inputLine) {
                    case "reg2":
                        Mediator.log(debug_string+" Server wants to register!",true);
                        //A server wants to register/reregister. We put the socket in the socket map so we can use it later.
                        //Check version compatibility
                        String version = in.readLine().replaceAll("\\p{C}", "");
                        Mediator.log(debug_string+" Version: "+version,true);
                        if (!(Mediator.versionCompare(version,Mediator.getUdphpMin()) >= 0)) {
                            //For now just silently end the connection.
                            //Proper error messages will follow in the next release
                            Mediator.log(debug_string+" Server not accepted. Version too old.",true);
                            client.close();
                            return;
                        }
                        Mediator.log(debug_string+" Server registered!",false);
                        
                        serverObj = this.main.getServer(this.client.getInetAddress().getHostAddress());
                        this.isServer = true;
                        serverObj.setTCPsocket(this.client);
                        //Write the 8 data strings
                        serverObj.setData1(in.readLine().replaceAll("\\p{C}", ""));
                        Mediator.log(debug_string+" Data 1: "+serverObj.getData1(),true);
                        serverObj.setData2(in.readLine().replaceAll("\\p{C}", ""));
                        Mediator.log(debug_string+" Data 2: "+serverObj.getData2(),true);
                        serverObj.setData3(in.readLine().replaceAll("\\p{C}", ""));
                        Mediator.log(debug_string+" Data 3: "+serverObj.getData3(),true);
                        serverObj.setData4(in.readLine().replaceAll("\\p{C}", ""));
                        Mediator.log(debug_string+" Data 4: "+serverObj.getData4(),true);
                        serverObj.setData5(in.readLine().replaceAll("\\p{C}", ""));
                        Mediator.log(debug_string+" Data 5: "+serverObj.getData5(),true);
                        serverObj.setData6(in.readLine().replaceAll("\\p{C}", ""));
                        Mediator.log(debug_string+" Data 6: "+serverObj.getData6(),true);
                        serverObj.setData7(in.readLine().replaceAll("\\p{C}", ""));
                        Mediator.log(debug_string+" Data 7: "+serverObj.getData7(),true);
                        serverObj.setData8(in.readLine().replaceAll("\\p{C}", ""));
                        Mediator.log(debug_string+" Data 8: "+serverObj.getData8(),true);
                    break;
                    case "connect":
                        //A client wants to connect. Now the interesting part begins
                        //Wait for next line that contains the requested IP adress.
                        String requested_server = in.readLine().replaceAll("\\p{C}", "");
                        String debug_string2 = debug_string + " Client <-> "+requested_server+" ->";
                        Mediator.log(debug_string2+" Connecting...",false);
                        if (this.main.getServerMap().containsKey(requested_server)) {
                            //SERVER FOUND
                            serverObj = this.main.getServer(requested_server);
                            //get server connection socket from the map (stored above)
                            Socket gameserver = serverObj.getTCPsocket();
                            if (!gameserver.isClosed()) {
                                String connect_to_server = requested_server;
                                //Get server port
                                int connect_to_port = serverObj.getPort();
                                //Send server port to client
                                Mediator.log(debug_string2+" Found server",true);
                                Mediator.log(debug_string2+" Send server port "+connect_to_port+" to client",true);
                                ByteArrayOutputStream bb = new ByteArrayOutputStream();
                                bb.write((byte) 255);
                                bb.write((connect_to_server+this.gm_string_seperator).getBytes());
                                bb.write((String.valueOf(connect_to_port)+this.gm_string_seperator).getBytes());
                                out.write(bb.toByteArray());
                                //Send buffer to client
                                out.flush();
                                //Get client port
                                Client clientObj = this.main.getClient(this.client.getInetAddress().getHostAddress());
                                int connect_to_port_server = clientObj.getPort();
                                Mediator.log(debug_string2+" Send client port "+connect_to_port_server+" to server",true);
                                //Get an output stream for the server socket. We will contact the server with this.
                                OutputStream out_server = gameserver.getOutputStream();
                                ByteArrayOutputStream bb_server = new ByteArrayOutputStream();
                                bb_server.write((byte) 255);
                                bb_server.write((this.client.getInetAddress().getHostAddress()+this.gm_string_seperator).getBytes());
                                bb_server.write(String.valueOf(connect_to_port_server+this.gm_string_seperator).getBytes());
                                out_server.write(bb_server.toByteArray());
                                //Send buffer to server
                                out_server.flush();
                                //We are done! Client and Server now connect to each other and the hole is punched!
                                Mediator.log(debug_string2+" CONNECTED!",false);
                            } else {
                                //SERVER FOUND BUT SOCKET IS DEAD
                                Mediator.log(debug_string+" CONNECTION FAILED - Server not reachable",false);
                                out.write((byte) 254);
                                out.flush();
                            }   
                        } else {
                            //SERVER NOT FOUND
                            Mediator.log(debug_string+" CONECTION FAILED - Server not found",false);
                            out.write((byte) 254);
                            out.flush();
                        }
                        this.main.destroyClient(this.client.getInetAddress().getHostAddress());
                    break;
                    case "lobby2":
                        if (Mediator.isLobby() || Mediator.isTesting()) {
                            Mediator.log(debug_string+" Sending lobby based on requested filters",true);
                            
                            HashMap<String, Server> servers =  new HashMap<String, Server>(main.getServerMap());

                            String filter_data1 = in.readLine().replaceAll("\\p{C}", "");
                            String filter_data2 = in.readLine().replaceAll("\\p{C}", "");
                            String filter_data3 = in.readLine().replaceAll("\\p{C}", "");
                            String filter_data4 = in.readLine().replaceAll("\\p{C}", "");
                            String filter_data5 = in.readLine().replaceAll("\\p{C}", "");
                            String filter_data6 = in.readLine().replaceAll("\\p{C}", "");
                            String filter_data7 = in.readLine().replaceAll("\\p{C}", "");
                            String filter_data8 = in.readLine().replaceAll("\\p{C}", "");
                            final String filter_sortby = in.readLine().replaceAll("\\p{C}", "");
                            final String filter_sortby_dir = in.readLine().replaceAll("\\p{C}", "");
                            String filter_limit = in.readLine().replaceAll("\\p{C}", "");
                            
                            //Skip servers with <INV> gamename (this might happen if a server was created using UDP connection but never initialized via TCP)
                            //TODO: Remove these invalid servers after some time.
                            Iterator<Map.Entry<String, Server>> iterInv = servers.entrySet().iterator();
                            while (iterInv.hasNext()) {
                                Map.Entry<String, Server> entry = iterInv.next();
                                if (entry.getValue().getData1().equals("<INV>")) {
                                    iterInv.remove();
                                }
                            }
                            
                            if (!"".equals(filter_data1)) {
                                Iterator<Map.Entry<String, Server>> iter = servers.entrySet().iterator();
                                while (iter.hasNext()) {
                                    Map.Entry<String, Server> entry = iter.next();
                                    if (!entry.getValue().getData1().equals(filter_data1)) {
                                        iter.remove();
                                    }
                                }
                            }
                            
                            if (!"".equals(filter_data2)) {
                                Iterator<Map.Entry<String, Server>> iter = servers.entrySet().iterator();
                                while (iter.hasNext()) {
                                    Map.Entry<String, Server> entry = iter.next();
                                    if (!entry.getValue().getData2().equals(filter_data2)) {
                                        iter.remove();
                                    }
                                }
                            }
                            
                            if (!"".equals(filter_data3)) {
                                Iterator<Map.Entry<String, Server>> iter = servers.entrySet().iterator();
                                while (iter.hasNext()) {
                                    Map.Entry<String, Server> entry = iter.next();
                                    if (!entry.getValue().getData3().equals(filter_data3)) {
                                        servers.remove(entry.getKey());
                                    }
                                }
                            }
                            
                            if (!"".equals(filter_data4)) {
                                Iterator<Map.Entry<String, Server>> iter = servers.entrySet().iterator();
                                while (iter.hasNext()) {
                                    Map.Entry<String, Server> entry = iter.next();
                                    if (!entry.getValue().getData4().equals(filter_data4)) {
                                        iter.remove();
                                    }
                                }
                            }
                            
                            if (!"".equals(filter_data5)) {
                                Iterator<Map.Entry<String, Server>> iter = servers.entrySet().iterator();
                                while (iter.hasNext()) {
                                    Map.Entry<String, Server> entry = iter.next();
                                    if (!entry.getValue().getData5().equals(filter_data5)) {
                                        iter.remove();
                                    }
                                }
                            }
                            
                            if (!"".equals(filter_data6)) {
                                Iterator<Map.Entry<String, Server>> iter = servers.entrySet().iterator();
                                while (iter.hasNext()) {
                                    Map.Entry<String, Server> entry = iter.next();
                                    if (!entry.getValue().getData6().equals(filter_data6)) {
                                        iter.remove();
                                    }
                                }
                            }
                            
                            if (!"".equals(filter_data7)) {
                                Iterator<Map.Entry<String, Server>> iter = servers.entrySet().iterator();
                                while (iter.hasNext()) {
                                    Map.Entry<String, Server> entry = iter.next();
                                    if (!entry.getValue().getData7().equals(filter_data7)) {
                                        iter.remove();
                                    }
                                }
                            }
                            
                            if (!"".equals(filter_data8)) {
                                Iterator<Map.Entry<String, Server>> iter = servers.entrySet().iterator();
                                while (iter.hasNext()) {
                                    Map.Entry<String, Server> entry = iter.next();
                                    if (!entry.getValue().getData8().equals(filter_data8)) {
                                        iter.remove();
                                    }
                                }
                            }
                            
                            Server[] arr = servers.values().toArray(new Server[servers.values().size()]);
                            Arrays.sort(arr, new Comparator<Server>() {
                                @Override
                                public int compare(Server o1, Server o2) {
                                    int mp = 1;
                                    int rt = 0;
                                    if ("ASC".equals(filter_sortby_dir)) {
                                        mp = -1;
                                    }
                                    switch (filter_sortby) {
                                        default:
                                        case "date":
                                            rt = new Long(o1.getCreatedTime()).compareTo(o2.getCreatedTime()) * mp;
                                        break;
                                        case "data1":
                                            rt = o1.getData1().compareTo(o2.getData1()) * mp;
                                        break;
                                        case "data2":
                                            rt = o1.getData2().compareTo(o2.getData2()) * mp;
                                        break;
                                        case "data3":
                                            rt = o1.getData3().compareTo(o2.getData3()) * mp;
                                        break;
                                        case "data4":
                                            rt = o1.getData4().compareTo(o2.getData4()) * mp;
                                        break;
                                        case "data5":
                                            rt = o1.getData5().compareTo(o2.getData5()) * mp;
                                        break;
                                        case "data6":
                                            rt = o1.getData6().compareTo(o2.getData6()) * mp;
                                        break;
                                        case "data7":
                                            rt = o1.getData7().compareTo(o2.getData7()) * mp;
                                        break;
                                        case "data8":
                                            rt = o1.getData8().compareTo(o2.getData8()) * mp;
                                        break;
                                    }
                                    return rt;
                                }
                            });
                            
                            if (!"".equals(filter_limit) && Integer.valueOf(filter_limit) <= arr.length) {
                                arr = Arrays.copyOfRange(arr, 0, Integer.valueOf(filter_limit));
                            }
                            
                            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
                            String json = gson.toJson(arr);
                            ByteArrayOutputStream bb = new ByteArrayOutputStream();
                            bb.write((byte) 249);
                            bb.write(json.getBytes());
                            bb.write(10);
                            //Send buffer to server
                            out.write(bb.toByteArray());
                            out.flush();
                        }
                    break;
                    case "istesting":
                        out.write((byte) 248);
                        if (Mediator.isTesting()) {
                            Mediator.log(debug_string+" Sending if testing is enabled",true);
                            out.write((byte) 1);
                        } else {
                            out.write((byte) 0);
                        }
                        //Send buffer
                        out.flush();
                    break;
                    case "testinginfos":
                        out.write((byte) 247);
                        if (Mediator.isTesting()) {
                            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
                            ByteArrayOutputStream bb = new ByteArrayOutputStream();
                            bb.write(Mediator.getName().getBytes());
                            bb.write(10);
                            bb.write(Mediator.getVersion().getBytes());
                            bb.write(10);
                            bb.write(Mediator.getUdphpMin().getBytes());
                            bb.write(10);
                            out.write(bb.toByteArray());
                            Mediator.log(debug_string+" Sending testing information",true);
                        } else {
                            out.write((byte) 0);
                        }
                        //Send buffer
                        out.flush();
                    break;
                    case "version":
                        ByteArrayOutputStream bb = new ByteArrayOutputStream();
                        bb.write((byte) 246);
                        bb.write(Mediator.getVersion().getBytes());
                        bb.write(10);
                        out.write(bb.toByteArray());
                        Mediator.log(debug_string+" Sending version information",true);
                        //Send buffer
                        out.flush();
                    break;
                    default:
                        //Ignore unknown commands (client disconnection will cause an unknown command)
                    break;
                }
                //Disable timout again and wait for next command
                client.setSoTimeout(0);
            }
            client.close();
            Mediator.log(debug_string+" Disconnected!",true);
            //Cleanup, when they loose TCP connection, this data can't be used anymore, so it's safe to remove
            if (this.isServer) {
                this.main.destroyServer(this.client.getInetAddress().getHostAddress());
                Mediator.log(debug_string+" Server deleted!",false);
            }
        } catch (Exception ex) {
            Mediator.log(debug_string+" Disconnected (e: "+ex.getClass().getName()+")",true);
            //Cleanup, when they loose TCP connection, this data can't be used anymore, so it's safe to remove
            if (this.isServer) {
                this.main.destroyServer(this.client.getInetAddress().getHostAddress());
                Mediator.log(debug_string+" Server deleted!",false);
            }
        }
    }
}
