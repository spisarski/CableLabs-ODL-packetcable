/**
 @header@
 */

package org.pcmm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.common.COPSDebug;
import org.umu.cops.prpep.COPSPepAgent;
import org.umu.cops.prpep.COPSPepConnection;
import org.umu.cops.prpep.COPSPepException;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * This is a provisioning COPS PEP. Responsible for making connection to the PDP
 * and maintaining it
 */
public class PCMMPepAgent extends COPSPepAgent implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(PCMMPepAgent.class);

    /**
     * PDP host IP
     */
    private transient ServerSocket serverSocket;

    /**
     * PDP host port
     */
    private final int serverPort;

/*
    */
/**
     * COPS error returned by PDP
     *//*

    private transient COPSError error;
*/

    /**
     * Creates a PEP agent
     *
     * @param pepID
     *            PEP-ID
     * @param clientType
     *            Client-type
     */
    public PCMMPepAgent(String pepID, short clientType, final String host, final int port) {
        super(pepID, clientType, host, port);
        serverPort = port;
    }

    /**
     * Runs the PEP process XXX - not sure of the exception throwing
     */
    public void run() {
        logger.info("Running in thread");
        try {

            logger.info("Create Server Socket on Port " + serverPort);

            serverSocket = new ServerSocket(serverPort);
            // Loop through for Incoming messages

            // server infinite loop
            // TODO - determine if any state should be added to control this loop
            while (true) {

                // Wait for an incoming connection from a PEP
                Socket socket = serverSocket.accept();

                logger.info("New connection accepted " + socket.getInetAddress() + ":" + socket.getPort());

                processConnection(socket);
                /**
                 * XXX - processConnection handles the open request from PEP And
                 * a thread is created for conn = new
                 * COPSPepConnection(_clientType, _socket); the main processing
                 * loop for PEP
                 */

            }
        } catch (Exception e) {
            logger.error(COPSDebug.ERROR_SOCKET, e);
        }
    }

    /**
     * Establish connection to PDP's IP address
     *
     * <Client-Open> ::= <Common Header> <PEPID> [<ClientSI>] [<LastPDPAddr>]
     * [<Integrity>]
     *
     * Not support [<ClientSI>], [<LastPDPAddr>], [<Integrity>]
     *
     * <Client-Accept> ::= <Common Header> <KA Timer> [<ACCT Timer>]
     * [<Integrity>]
     *
     * Not send [<Integrity>]
     *
     * <Client-Close> ::= <Common Header> <Error> [<PDPRedirAddr>] [<Integrity>]
     *
     * Not send [<PDPRedirAddr>], [<Integrity>]
     *
     * @throws UnknownHostException
     * @throws IOException
     * @throws COPSException
     * @throws COPSPepException
     *
     */
    private COPSPepConnection processConnection(final Socket socket) throws IOException, COPSException {
        // Build OPN
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_OPN, _clientType);

        final COPSPepId pepId = new COPSPepId();
        final COPSData d = new COPSData(_pepID);
        pepId.setData(d);

        final COPSClientOpenMsg msg = new COPSClientOpenMsg();
        msg.add(hdr);
        msg.add(pepId);

        logger.info("Send COPSClientOpenMsg to PDP");
        msg.writeData(socket);

        // Receive the response
        logger.info("Receive the resposne from PDP");
        final COPSMsg recvmsg = COPSTransceiver.receiveMsg(socket);

        if (recvmsg.getHeader().isAClientAccept()) {
            logger.info("isAClientAccept from PDP");
            final COPSClientAcceptMsg cMsg = (COPSClientAcceptMsg) recvmsg;

            // Support
            if (cMsg.getIntegrity() != null) {
                throw new COPSPepException("Unsupported object (Integrity)");
            }

            // Mandatory KATimer
            final COPSKATimer kt = cMsg.getKATimer();
            if (kt == null)
                throw new COPSPepException(
                    "Mandatory COPS object missing (KA Timer)");
            short _kaTimeVal = kt.getTimerVal();

            // ACTimer
            final COPSAcctTimer at = cMsg.getAcctTimer();
            short _acctTimer = 0;
            if (at != null)
                _acctTimer = at.getTimerVal();

            // Create the connection manager
            final COPSPepConnection conn = new COPSPepConnection(pepId, _clientType, socket, _kaTimeVal, _acctTimer);
            logger.info("Thread(conn).start");
            new Thread(conn).start();

            return conn;
        } else if (recvmsg.getHeader().isAClientClose()) {
            logger.info("isAClientClose from PDP");
            final COPSClientCloseMsg cMsg = (COPSClientCloseMsg) recvmsg;
            _error = cMsg.getError();
            socket.close();
            return null;
        } else { // messages of other types are not expected
            throw new COPSPepException("Message not expected. Closing connection for "
                + socket.toString());
        }
    }

}
