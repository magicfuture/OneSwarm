package edu.washington.cs.oneswarm.f2f.datagram;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.crypto.NoSuchPaddingException;

import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.impl.PRUDPPacketHandlerImpl;

import edu.uw.cse.netlab.utils.CoreWaiter;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;

public class DatagramConnectionManager extends CoreWaiter {
    private static DatagramConnectionManager instance;
    public final static Logger logger = Logger.getLogger(DatagramConnectionManager.class.getName());

    public synchronized static DatagramConnectionManager get() {
        if (instance == null)
            instance = new DatagramConnectionManager();

        return instance;
    }

    private final HashMap<String, DatagramConnection> connections = new HashMap<String, DatagramConnection>();

    private DatagramSocket socket;

    private DatagramConnectionManager() {
        super();
    }

    public DatagramConnection createConnection(FriendConnection connection)
            throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidAlgorithmParameterException {
        return new DatagramConnection(this, connection);
    }

    @Override
    protected void init() {
        // Get the socket for the udp.listen.port
        PRUDPPacketHandlerImpl handler = (PRUDPPacketHandlerImpl) (PRUDPPacketHandlerFactory
                .getHandler(COConfigurationManager.getIntParameter("UDP.Listen.Port")));
        socket = handler.getSocket();
    }

    public boolean packetRecieved(DatagramPacket packet) {
        String key = getKey(packet.getAddress(), packet.getPort());
        logger.finest("checking udp packet match: " + key);
        DatagramConnection conn = connections.get(key);
        if (conn == null) {
            logger.finest("no match found");
            return false;
        }
        if (!conn.messageReceived(packet)) {
            logger.finest("matched packet failed decryption");
            return false;
        }
        return true;
    }

    static String getKey(InetAddress address, int port) {
        return address.getHostAddress() + ":" + port;
    }

    void register(DatagramConnection connection) {
        String key = connection.getKey();
        logger.fine("registering connection to key: " + key);
        DatagramConnection existing = connections.get(key);
        if (existing != null) {
            logger.warning("Registered udp connection but one is already there!");
            existing.close();
        }
        connections.put(key, connection);
    }

    void send(DatagramPacket packet) {
        try {
            // logger.finest("Sending packet: " + packet);
            socket.send(packet);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void deregister(DatagramConnection conn) {
        String key = conn.getKey();
        DatagramConnection registered = connections.get(key);
        if (registered.equals(conn)) {
            connections.remove(key);
        }
    }

    int getPort() {
        return socket.getLocalPort();
    }
}