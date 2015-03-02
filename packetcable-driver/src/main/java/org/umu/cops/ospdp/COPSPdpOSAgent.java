package org.umu.cops.ospdp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.AbstractCOPSPdpAgent;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.Socket;

/**
 * Core PDP agent for outsourcing.
 */
public abstract class COPSPdpOSAgent extends AbstractCOPSPdpAgent {

    private static final Logger logger = LoggerFactory.getLogger(COPSPdpOSAgent.class);

    private COPSPdpOSDataProcess _process;

    /**
     * Creates a PDP Agent
     *
     * @param port  Port to listen to
     * @param clientType    COPS Client-type
     * @param process   Object to perform policy data processing
     */
    public COPSPdpOSAgent(final String host, final int port, final short clientType, final COPSPdpOSDataProcess process,
                          final short acctTimer, final short kaTimer) {
        super(host, port, clientType, kaTimer, acctTimer);
        _process = process;
        logger.info("Created agent");
    }

    public void stopThreads() {
        logger.info("Stopping threads");
        for (final Thread thread : _threadMap.values()) {
            thread.interrupt();
        }
    }

    /**
     * Requests a COPS sync for a PEP
     * @param pepID PEP-ID of the PEP to be synced
     * @throws COPSException
     * @throws COPSPdpException
     */
    public void sync(final String pepID) throws COPSException {
        logger.info("Sync with pepID - " + pepID);
        ((COPSPdpOSConnection)_connectionMap.get(pepID)).syncAllRequestState();
    }

    /**
      * Handles a COPS client-open message
      * @param    conn Socket to the PEP
      * @param    cMsg <tt>COPSMsg</tt> holding the client-open message
      * @throws COPSException
      * @throws IOException
      */
    @Override
    protected void handleClientOpenMsg(final Socket conn, final COPSClientOpenMsg cMsg) throws COPSException, IOException {
        final COPSPepId pepId = cMsg.getPepId();

        // Validate Client Type
        if (cMsg.getHeader().getClientType() != _clientType) {
            // Unsupported client type
            writeCloseMsg(conn, cMsg, COPSError.COPS_ERR_UNSUPPORTED_CLIENT_TYPE);
            throw new COPSException("Unsupported client type");
        }

        // PEPId is mandatory
        if (pepId == null) {
            // Mandatory COPS object missing
            writeCloseMsg(conn, cMsg, COPSError.COPS_ERR_MANDATORY_OBJECT_MISSING);
            throw new COPSException("Mandatory COPS object missing (PEPId)");
        }

        // Support
        if ( (cMsg.getClientSI() != null) || (cMsg.getPdpAddress() != null) || (cMsg.getIntegrity() != null)) {
            writeCloseMsg(conn, cMsg, COPSError.COPS_ERR_UNKNOWN_OBJECT);
            throw new COPSException("Unsupported objects (ClientSI, PdpAddress, Integrity)");
        }

        // Connection accepted
        final COPSHeader ahdr = new COPSHeader(COPSHeader.COPS_OP_CAT, cMsg.getHeader().getClientType());
        final COPSKATimer katimer = new COPSKATimer(_kaTimer);
        final COPSAcctTimer acctTimer = new COPSAcctTimer(_acctTimer);
        final COPSClientAcceptMsg acceptMsg = new COPSClientAcceptMsg();
        acceptMsg.add(ahdr);
        acceptMsg.add(katimer) ;
        if (_acctTimer != 0) acceptMsg.add(acctTimer);
        acceptMsg.writeData(conn);

        final COPSPdpOSConnection pdpConn = new COPSPdpOSConnection(pepId, conn, _process, _acctTimer, _kaTimer);
        final Thread thread = new Thread(pdpConn);
        thread.start();
        _threadMap.put(pdpConn, thread);
        _connectionMap.put(pepId.getData().str(), pdpConn);
    }

    private void writeCloseMsg(final Socket conn, final COPSMsg msg, final byte errorCode) throws COPSException {
        final COPSHeader cHdr = new COPSHeader(COPSHeader.COPS_OP_CC, msg.getHeader().getClientType());
        final COPSError err = new COPSError(errorCode, (short) 0);
        final COPSClientCloseMsg closeMsg = new COPSClientCloseMsg();
        closeMsg.add(cHdr);
        closeMsg.add(err);
        try {
            closeMsg.writeData(conn);
        } catch (IOException unae) {
            logger.error("Unexpected error writing data close message data", unae);
        }
    }
}
