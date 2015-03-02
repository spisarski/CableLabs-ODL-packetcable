package org.umu.cops.ospep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.prpep.COPSPepAgent;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

/**
 * This is a outsourcing COPS PEP. Responsible for making
 * connection to the PDP and maintaining it
 */
public class COPSPepOSAgent extends COPSPepAgent {

    private final static Logger logger = LoggerFactory.getLogger(COPSPepOSAgent.class);

    /**
     * Policy data processor class
     */
    private final COPSPepOSDataProcess _process;

    /**
     * The PEP OS Connection
     */
    private transient COPSPepOSConnection _conn;

    /**
     * Creates a PEP agent
     * @param    pepID              PEP-ID
     * @param    clientType         Client-type
     */
    public COPSPepOSAgent(final String pepID, final short clientType, final String host, final int port,
                          final COPSPepOSDataProcess process) {
        super(pepID, clientType, host, port);
        this._process = process;
    }

    @Override
    public boolean connect() throws IOException, COPSException {
        logger.info("Connect to host:port - " + _psHost + ':' + _psPort);
        // COPSDebug.out(getClass().getName(), "Thread ( " + _pepID + ") - Connecting to PDP");
        // Check whether it already exists
        if (_conn == null)
            _conn = processOSConnection(_psHost, _psPort);
        else {
            // Check if it's closed
            if (_conn.isClosed()) {
                _conn = processOSConnection(_psHost, _psPort);
            } else {
                disconnect(null);
                _conn = processOSConnection(_psHost, _psPort);
            }
        }

        return (_conn != null);
    }

    /**
     * Creates a new request state when the outsourcing event is detected.
     * @param handle The COPS handle for this request
     * @param clientSIs The client specific data for this request
     */
    public void dispatchEvent(final COPSHandle handle, final List<COPSClientSI> clientSIs) {
        logger.info("Dispatching event");
        try {
            addRequestState(handle, clientSIs);
        } catch (Exception e) {
            logger.error("Unexpected error adding the request state", e);
        }
    }
    /**
     * Adds a request state to the connection manager.
     * @param clientSIs The client data from the outsourcing event
     * @return  The newly created connection manager
     * @throws COPSPepException
     * @throws COPSException
     */
    public COPSPepOSReqStateMan addRequestState(final COPSHandle handle, final List<COPSClientSI> clientSIs)
            throws COPSException {
        logger.info("Adding request state");
        if (_conn != null)
            return _conn.addRequestState(handle, _process, clientSIs);

        return null;
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
     * @throws   IOException
     * @throws   COPSException
     * @throws   COPSPepException
     *
     */
    protected COPSPepOSConnection processOSConnection(final String psHost, final int psPort)
            throws IOException, COPSException {
        // Build OPN
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_OPN, _clientType);

        final COPSPepId pepId = new COPSPepId();
        final COPSData d = new COPSData(_pepID);
        pepId.setData(d);

        final COPSClientOpenMsg msg = new COPSClientOpenMsg();
        msg.add(hdr);
        msg.add(pepId);

        // Create _socket and send OPN
        final InetAddress addr = InetAddress.getByName(psHost);
        final Socket socket = new Socket(addr,psPort);
        msg.writeData(socket);

        // Get response
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

            // Create connection manager
            final COPSPepOSConnection conn = new COPSPepOSConnection(pepId, _clientType, socket, _kaTimeVal, _acctTimer);
            new Thread(conn).start();

            return conn;
        } else if (recvmsg.getHeader().isAClientClose()) {
            COPSClientCloseMsg cMsg = (COPSClientCloseMsg) recvmsg;
            _error = cMsg.getError();
            socket.close();
            return null;
        } else { // other message types are unexpected
            throw new COPSPepException("Message not expected. Closing connection for " + socket.toString());
        }
    }

}
