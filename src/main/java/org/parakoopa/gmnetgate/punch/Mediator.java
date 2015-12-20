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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This class starts the UDP and TCP listening sockets.
 *
 * @author Parakoopa
 */
public class Mediator {

    /**
     * * (DEFAULT) SETTINGS **
     */
    /**
     * --port The port to listen on. Use the command line argument --port to
     * change it.
     */
    private static int port = 6510;

    /**
     * --version (read only) Version of this master server
     */
    private static final String version = "1.2.5";

    /**
     * Minimum required GMnet PUNCH version.
     */
    private static final String udphpMin = "1.2.0";

    /**
     * --quiet Whether or not the console output should be hidden.
     */
    private static boolean quiet = false;

    /**
     * --verbose Log additional info?
     */
    private static boolean verbose = false;

    /**
     * --log Logfile (or null)
     */
    private static String log = null;

    /**
     * --disable-lobby Is the lobby enabled?
     */
    private static boolean lobby = true;

    /**
     * --testing Enable or disable debugging with HTMT.
     */
    private static boolean testing = false;

    /**
     * --name Optional name of this master server.
     */
    private static String name = "";
    
    /**
     * DEBUGGING: Start server with a bunch of test servers in the list
     */
    private static boolean dbg_servers = false;

    /**
     * Object that represents a server. Contains TCP socket, port, and the 5
     * data-strings
     */
    private HashMap<String, Server> serverMap;
    /**
     * Object that represents a client Contains only port right now.
     */
    private HashMap<String, Client> clientMap;
    /**
     * TCP Server.
     */
    private ServerSocket server;
    /**
     * UDP Server.
     */
    private DatagramSocket server_udp;
    /**
     * Recieving buffer for UDP.
     */
    byte[] receiveData = new byte[1024];

    /**
     * Command-line main-method. Currently no command line paramters are
     * supported.
     *
     * @param args Not used.
     */
    public static void main(String[] args) {

        /* Setup command line args */
        String header = "Start GMnet GATE.PUNCH. "
                + "The master sercer for GMnet PUNCH / GMnet ENGINE. "
                + "More information: http://gmnet.parakoopa.de"
                + "Use GMnet GATE.TESTER to debug your master server (http://gmnet.parakoopa.de/tester).";

        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("port")
                .withDescription("Specify the TCP and UDP port this server will listen on. Default: " + Mediator.port)
                .hasArg()
                .withArgName("PORT")
                .create("p"));
        options.addOption("h", "help", false, "Print this help text.");
        options.addOption(OptionBuilder.withLongOpt("disable-lobby")
                .withDescription("Ignore all requests of listing the connected servers.")
                .create());
        options.addOption(OptionBuilder.withLongOpt("version")
                .withDescription("Print version information and exit.")
                .create());
        options.addOption(OptionBuilder.withLongOpt("name")
                .hasArg()
                .withDescription("Name this master server (can be used with HTMT)")
                .create());
        options.addOption(OptionBuilder.withLongOpt("testing")
                .withDescription("Enable testing mode to use debugging tools such as HTMT.")
                .create());
        options.addOption("q", "quiet", false, "Don't output anything to the console.");
        options.addOption("v", "verbose", false, "Print and/or log all information.");
        options.addOption(OptionBuilder.withLongOpt("log")
                .withDescription("Log output to this file. Will still log"
                        + " even if 'quiet' is set.")
                .hasArg()
                .withArgName("FILE")
                .create("l"));

        try {
            CommandLineParser parser = new BasicParser();
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar gmnet_gatepunch.jar", header, options, "", true);
                System.exit(0);
            }
            if (line.hasOption("quiet")) {
                Mediator.quiet = true;
            }
            if (line.hasOption("verbose")) {
                Mediator.verbose = true;
            }
            if (line.hasOption("testing")) {
                Mediator.testing = true;
            }
            if (line.hasOption("version")) {
                System.out.println("GMnet GATE.PUNCH");
                System.out.println("Version: " + Mediator.version);
                System.exit(0);
            }
            if (line.hasOption("name")) {
                Mediator.name = line.getOptionValue("name");
            }
            if (line.hasOption("log")) {
                Mediator.log = line.getOptionValue("log");
            }
            if (line.hasOption("port")) {
                Mediator.port = Integer.valueOf(line.getOptionValue("port"));
            }
            if (line.hasOption("disable-lobby")) {
                Mediator.lobby = false;
            }
        } catch (ParseException ex) {
            Logger.getLogger(Mediator.class.getName()).log(Level.SEVERE, null, ex);
        }
        /* END Setup command line args */

        new Mediator();
    }

    public Mediator() {
        try {
            //Set up some local variables
            server = new ServerSocket(port);
            server_udp = new DatagramSocket(port);
            serverMap = new HashMap();
            clientMap = new HashMap();
            final Mediator me = this;

            Mediator.log("GMnet GATE.PUNCH STARTED", false);
            Mediator.log("Starting UDP and TCP servers on port " + port, false);

            //Start two new threads for the servers, just to be on the safe side.
            new Thread() {
                @Override
                //START UDP SERVER
                public void run() {
                    Mediator.log("Loaded UDP Listener", true);
                    //When packet is processed: Continue with next packet
                    while (true) {
                        try {
                            //Create incoming packet and wait for it.
                            DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                            server_udp.receive(packet);
                            //When a packet arrivied: Deal with it. 
                            UDPPacket packetHandler = new UDPPacket(me, packet, server_udp);
                            //This was once also a seperate thread, that's why the method is called run.
                            //annother thread is not needed though.
                            //it is propably even more efficient to just swap the packet out instead of
                            //creating a new class above. Do what you want :)
                            packetHandler.run();
                        } catch (IOException ex) {
                            //Print all exceptions.
                            ex.printStackTrace();
                        }
                    }
                }
            }.start();
            new Thread() {
                @Override
                //START TCP SERVER
                public void run() {
                    Mediator.log("Loaded TCP Listener", true);
                    //When connection thread is created: Wait for next connection
                    while (true) {
                        try {
                            //Wait for connection
                            Socket client = server.accept();
                            //When connection is opened: Start thread that handles it.
                            TCPConnection connectionHandler = new TCPConnection(me, client, server);
                            new Thread(connectionHandler).start();
                        } catch (IOException ex) {
                            //Print all exceptions.
                            ex.printStackTrace();
                        }
                    }
                }
            }.start();
            if (Mediator.dbg_servers) {
                for (int i = 0; i<50; i++) {
                    Server serverObj = this.getServer(UUID.randomUUID().toString());
                    serverObj.setData1(UUID.randomUUID().toString());
                    serverObj.setData2(UUID.randomUUID().toString());
                    serverObj.setData3(UUID.randomUUID().toString());
                    serverObj.setData4(UUID.randomUUID().toString());
                    serverObj.setData5(UUID.randomUUID().toString());
                    serverObj.setData6(UUID.randomUUID().toString());
                    serverObj.setData7(UUID.randomUUID().toString());
                    serverObj.setData8(UUID.randomUUID().toString());
                }
            }
        } catch (IOException ex) {
            //Print all exceptions.
            ex.printStackTrace();
        }
    }

    /**
     * Returns server HashMap. Each server object contains TCP socket, port, and
     * the 5 data-strings
     *
     * @return HashMap containing the server objects
     */
    public HashMap<String, Server> getServerMap() {
        return serverMap;
    }

    /**
     * Returns client HashMap. Each client object contains port
     *
     * @return HashMap containing the client objects
     */
    public HashMap<String, Client> getClientMap() {
        return clientMap;
    }

    /**
     * Get the server object that represents the ip (and create it if it doesn't
     * exist).
     *
     * @param ip IP of the server
     */
    
    public Server getServer(String ip) {
        Server serverObj;
        if (serverMap.containsKey(ip)) {
            serverObj = serverMap.get(ip);
        } else {
            serverObj = new Server(ip);
            serverMap.put(ip, serverObj);
        }
        return serverObj;
    }

    /**
     * Get the client object that represents the ip (and create it if it doesn't
     * exist)
     *
     * @param ip IP of the client
     * @return client object with that ip
     */
    public Client getClient(String ip) {
        Client clientObj;
        if (clientMap.containsKey(ip)) {
            clientObj = clientMap.get(ip);
        } else {
            clientObj = new Client();
            clientMap.put(ip, clientObj);
        }
        return clientObj;
    }

    /**
     * Remove this IP from the server list.
     *
     * @param hostAddress
     */
    void destroyServer(String ip) {
        serverMap.remove(ip);
    }

    /**
     * Remove this IP from the client list.
     *
     * @param hostAddress
     */
    void destroyClient(String ip) {
        clientMap.remove(ip);
    }

    /**
     * Recieving buffer for UDP.
     *
     * @return A buffer.
     */
    public byte[] getReceiveData() {
        return receiveData;
    }

    public static boolean isLobby() {
        return lobby;
    }

    public static boolean isTesting() {
        return testing;
    }

    public static String getName() {
        return name;
    }

    public static String getVersion() {
        return version;
    }

    public static String getUdphpMin() {
        return udphpMin;
    }

    /**
     * Logs to the console (if quiet was not set) and to the logfile if
     * specified The logfile will also get timestamps for each event.
     *
     * @param str String to log
     * @param verbose boolean Should this be logged only with --verbose?
     */
    public static void log(String str, boolean verbose) {
        //Don't print verbose lines if not requested
        if (verbose && !Mediator.verbose) {
            return;
        }
        DateFormat date = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT,
                Locale.getDefault());
        /* CONSOLE OUTPUT */
        if (!Mediator.quiet) {
            System.out.println(date.format(new Date()) + " : " + str);
        }
        /* FILE LOG */
        if (Mediator.log != null) {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(Mediator.log, true)))) {
                out.println(date.format(new Date()) + " : " + str);
            } catch (IOException ex) {
                Logger.getLogger(Mediator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Compares two version strings.
     *
     * Use this instead of String.compareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     * By http://stackoverflow.com/questions/6701948/efficient-way-to-compare-version-strings-in-java
     *
     * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return The result is a negative integer if str1 is _numerically_ less
     * than str2. The result is a positive integer if str1 is _numerically_
     * greater than str2. The result is zero if the strings are _numerically_
     * equal.
     */
    public static Integer versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        } // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        else {
            return Integer.signum(vals1.length - vals2.length);
        }
    }

}
