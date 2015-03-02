package org.umu.cops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.prpdp.COPSPdpConnection;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a connection with a COPS PDP Agent such as a Cable Modem Termination System (CMTS).
 *
 * All inbound communications are received by this class and
 */
public abstract class AbstractCOPSPdpAgent extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCOPSPdpAgent.class);

    /**
     * PEP host name
     */
    protected final String _psHost;

    /**
     * PEP port
     */
    protected final int _psPort;

    /**
     * The client type
     */
    protected final short _clientType;

    /**
     * Keep-alive timer (secs)
     */
    protected final short _kaTimer;

    /**
     * Keep-alive timer (secs)
     */
    protected final short _acctTimer;

    /**
     * Maps a PEP-ID to a connection
     */
    protected final Map<String, COPSConnection> _connectionMap;

    /**
     * Map of all threads so that they can be gracefully stopped upon request
     */
    protected final Map<COPSConnection, Thread> _threadMap;

    /**
     * This socket is shared with the COPSConnections this class is responsible for creating therefore it should be
     * managed here only.
     */
    protected transient Socket _socket;

    /**
     * Constructor
     * @param psHost - the host
     * @param psPort - the port
     * @param clientType - the type of client
     * @param kaTimer - keep alive timer
     * @param acctTimer - accounting timer
     */
    public AbstractCOPSPdpAgent(final String psHost, final int psPort, final short clientType, final short kaTimer,
                                final short acctTimer) {
        _psHost = psHost;
        _psPort = psPort;
        _clientType = clientType;
        _kaTimer = kaTimer;
        _acctTimer = acctTimer;
        _connectionMap = new ConcurrentHashMap<>();
        _threadMap = new ConcurrentHashMap<>();
    }

    /**
     * Should be called during shutdown
     */
    public void stopAgent() {
        logger.info("Begin stopping all threads");
        for (final Thread thread : _threadMap.values()) {
            thread.interrupt();
        }
        logger.info("Completed stopping all threads");
    }

    /**
     * Runs the PDP process
     */
    @Override
    public void run() {
        try {
            // Create Socket and send OPN
            _socket = openSocket();

            // Loop through for Incoming messages until an open is received then hand off the socket to
            // a COPSPdpConnection to handle the rest of the communications
            while (_socket.isConnected()) {
                try {
                    processMessage(COPSTransceiver.receiveMsg(_socket));
                } catch (IOException e) {
                    logger.error("IOException - exiting thread");
                    break;
                } catch (COPSException e) {
                    logger.warn("Error parsing message");
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected exception. Exiting thread");
        }
    }

    private void processMessage(final COPSMsg msg) throws IOException, COPSException {
        logger.info("Processing message from - " + _socket.getInetAddress());
        if (msg.getHeader().isAClientOpen()) {
            logger.info("Received Client Open message");
            if (msg instanceof COPSClientOpenMsg) {
                handleClientOpenMsg(_socket, (COPSClientOpenMsg)msg);
            } else {
                logger.error("Message is not of the expected type COPSClientOpenMsg");
            }
        } else if (msg.getHeader().isAClientClose()) {
            logger.info("Received Client Close message");
            if (msg instanceof COPSClientCloseMsg) {
                handleClientCloseMsg(_socket, (COPSClientCloseMsg) msg);
            } else {
                logger.error("Message is not of the expected type COPSClientCloseMsg");
            }
        } else if (msg.getHeader().isAClientAccept()) {
            logger.info("Received Client Accept message");
        } else if (msg.getHeader().isADecision()) {
            logger.info("Received Client Decision message");
        } else if (msg.getHeader().isADeleteReq()) {
            logger.info("Received Client Delete Request message");
        } else if (msg.getHeader().isAKeepAlive()) {
            logger.info("Received Client Keep Alive message");
        } else if (msg.getHeader().isAReport()) {
            logger.info("Received Report message");
        } else if (msg.getHeader().isARequest()) {
            logger.info("Received Request message");
        } else if (msg.getHeader().isASyncComplete()) {
            logger.info("Received Sync Complete message");
        } else if (msg.getHeader().isASyncStateReq()) {
            logger.info("Received Sync State Request message");
        }
    }

    /**
     * Opens a socket connection that should stay alive
     * @return - the socket
     * @throws IOException
     */
    private Socket openSocket() throws IOException {
        logger.info("Connecting to host - " + _psHost + ':' + _psPort);
        final InetAddress addr = InetAddress.getByName(_psHost);
        final Socket socket = new Socket(addr, _psPort);
        socket.setKeepAlive(true);
        logger.info("PDP Socket Opened with keep alive true");
        return socket;
    }

    /**
     * Adds connections to manage by PEP ID
     * @param pepId - the PEP ID
     * @param conn - the COPS Connection
     */
    protected void addPepConnection(final String pepId, final COPSConnection conn) {
        _connectionMap.put(pepId, conn);
    }

    /**
     * Disconnects a PEP
     *
     * @param pepID PEP-ID of the PEP to be disconnected
     * @param error COPS Error to be reported as a reason
     * @throws COPSException
     * @throws IOException
     */
    private void disconnect(final String pepID, final COPSError error) throws COPSException, IOException {
        final COPSPdpConnection pdpConn = (COPSPdpConnection)_connectionMap.get(pepID);
        final COPSHeader cHdr = new COPSHeader(COPSHeader.COPS_OP_CC, _clientType);
        final COPSClientCloseMsg closeMsg = new COPSClientCloseMsg();
        closeMsg.add(cHdr);
        if (error != null) {
            closeMsg.add(error);
        }
        closeMsg.writeData(pdpConn.getSocket());
    }

    /**
     * Removes a PEP from the connection map
     * @param pepID PEP-ID of the PEP to be removed
     */
    public void delete(final String pepID) {
        _connectionMap.remove(pepID);
    }

    protected void handleClientCloseMsg(final Socket socket, final COPSClientCloseMsg closeMsg)
            throws COPSException, IOException {
        logger.info("Handling client close message");
        // TODO - Obtain PEP ID somehow and
        final String pepID = "";
        if(_connectionMap.remove(pepID) != null) {
            final COPSError err = new COPSError(COPSError.COPS_ERR_SHUTTING_DOWN, (short) 0);
            disconnect(pepID, err);
        }
    }

    protected abstract void handleClientOpenMsg(final Socket conn, final COPSClientOpenMsg cMsg) throws COPSException, IOException;

}
