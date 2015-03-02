/*
 * Copyright (c) 2004 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.prpep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.COPSConnection;
import org.umu.cops.COPSReqStateMan;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a provisioning COPS PEP. Responsible for making
 * connection to the PDP and maintaining it
 */
public class COPSPepAgent {

    private final static Logger logger = LoggerFactory.getLogger(COPSPepAgent.class);

    /**
     PEP's Identifier
     */
    protected final String _pepID;

    /**
     PEP's client-type
     */
    protected final short _clientType;

    /**
     PDP host name
     */
    protected final String _psHost;

    /**
     PDP port
     */
    protected final int _psPort;

    /**
     PEP-PDP connection manager
     */
    protected COPSPepConnection _conn;

    /**
     COPS error returned by PDP
     */
    protected COPSError _error;

    /**
     * Map of all threads so that they can be gracefully stopped upon request
     */
    protected final Map<COPSConnection, Thread> _threadMap;

    public COPSPepAgent(final String pepID, final short clientType, final String host, final int port) {
        _pepID = pepID;
        _clientType = clientType;
        _psHost = host;
        _psPort = port;
        _threadMap = new ConcurrentHashMap<>();
    }

    /**
     * Connects to a PDP
     * @return   <tt>true</tt> if PDP accepts the connection; <tt>false</tt> otherwise
     * @throws   java.net.UnknownHostException
     * @throws   java.io.IOException
     * @throws org.umu.cops.stack.COPSException
     * @throws org.umu.cops.stack.COPSException
     */
    public boolean connect() throws IOException, COPSException {
        logger.info("Connect to host:port - " + _psHost + ':' + _psPort);
        // COPSDebug.out(getClass().getName(), "Thread ( " + _pepID + ") - Connecting to PDP");
        // Check whether it already exists
        if (_conn == null)
            _conn = processConnection(_psHost, _psPort);
        else {
            // Check if it's closed
            if (_conn.isClosed()) {
                _conn = processConnection(_psHost, _psPort);
            } else {
                disconnect(null);
                _conn = processConnection(_psHost, _psPort);
            }
        }

        return (_conn != null);
    }

    /**
     * Establish connection to PDP's IP address
     *
     * <Client-Open> ::= <Common Header>
     *                  <PEPID>
     *                  [<ClientSI>]
     *                  [<LastPDPAddr>]
     *                  [<Integrity>]
     *
     * Not support [<ClientSI>], [<LastPDPAddr>], [<Integrity>]
     *
     * <Client-Accept> ::= <Common Header>
     *                      <KA Timer>
     *                      [<ACCT Timer>]
     *                      [<Integrity>]
     *
     * Not send [<Integrity>]
     *
     * <Client-Close> ::= <Common Header>
     *                      <Error>
     *                      [<PDPRedirAddr>]
     *                      [<Integrity>]
     *
     * Not send [<PDPRedirAddr>], [<Integrity>]
     *
     * @throws java.net.UnknownHostException
     * @throws   IOException
     * @throws   COPSException
     * @throws org.umu.cops.prpep.COPSPepException
     *
     */
    protected COPSPepConnection processConnection(final String psHost, final int psPort)
            throws IOException, COPSException {
        // Build OPN
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_OPN, _clientType);

        final COPSPepId pepId = new COPSPepId();
        final COPSData d = new COPSData(_pepID);
        pepId.setData(d);

        final COPSClientOpenMsg msg = new COPSClientOpenMsg();
        msg.add(hdr);
        msg.add(pepId);

        // Create Socket and send OPN
        final InetAddress addr = InetAddress.getByName(psHost);
        final Socket socket = new Socket(addr,psPort);
        msg.writeData(socket);

        // Receive the response
        final COPSMsg recvmsg = COPSTransceiver.receiveMsg(socket);

        if (recvmsg.getHeader().isAClientAccept()) {
            final COPSClientAcceptMsg cMsg = (COPSClientAcceptMsg) recvmsg;

            // Support
            if (cMsg.getIntegrity() != null) {
                throw new COPSPepException("Unsupported object (Integrity)");
            }

            // Mandatory KATimer
            final COPSKATimer kt = cMsg.getKATimer();
            if (kt == null)
                throw new COPSPepException ("Mandatory COPS object missing (KA Timer)");
            short _kaTimeVal = kt.getTimerVal();

            // ACTimer
            final COPSAcctTimer at = cMsg.getAcctTimer();
            short _acctTimer = 0;
            if (at != null)
                _acctTimer = at.getTimerVal();

            // Create the connection manager
            final COPSPepConnection conn = new COPSPepConnection(pepId, _clientType, socket, _kaTimeVal, _acctTimer);

            // TODO - manage threads
            new Thread(conn).start();

            return conn;
        } else if (recvmsg.getHeader().isAClientClose()) {
            final COPSClientCloseMsg cMsg = (COPSClientCloseMsg) recvmsg;
            _error = cMsg.getError();
            socket.close();
            return null;
        } else { // messages of other types are not expected
            throw new COPSPepException("Message not expected. Closing connection for " + socket.toString());
        }
    }

    /**
     * Disconnects from the PDP
     * @param error Reason
     * @throws COPSException
     * @throws IOException
     */
    public void disconnect(final COPSError error) throws COPSException, IOException {
        logger.info("Disconnect");
        final COPSHeader cHdr = new COPSHeader(COPSHeader.COPS_OP_CC, _clientType);
        final COPSClientCloseMsg closeMsg = new COPSClientCloseMsg();
        closeMsg.add(cHdr);
        if (error != null)
            closeMsg.add(error);

        closeMsg.writeData(_conn.getSocket());
    }

    /**
     * Adds a request state to the connection manager.
     * @return  The newly created connection manager
     * @throws COPSPepException
     * @throws COPSException
     */
    public COPSPepReqStateMan addRequestState (final COPSHandle handle, final COPSPepDataProcess process)
            throws COPSException {
        logger.info("Adding request state");
        if (_conn != null) {
            return _conn.addRequestState(handle, process);
        }
        return null;
    }


    /**
     * Queries the connection manager to delete a request state
     * @param man   Request state manager
     * @throws COPSPepException
     * @throws COPSException
     */
    public void deleteRequestState (final COPSPepReqStateMan man) throws COPSException {
        logger.info("Deleting request state");
        if (_conn != null)
            _conn.deleteRequestState(man);
    }

    /**
     * Gets all the request state managers
     * @return  A <tt>Hashtable</tt> holding all active request state managers
     */
    public Map<COPSHandle, COPSReqStateMan> getReqStateMans() {
        logger.info("Retrieving the request state managers");
        if (_conn != null)
            return _conn.getReqStateMans();
        return null;
    }

}



