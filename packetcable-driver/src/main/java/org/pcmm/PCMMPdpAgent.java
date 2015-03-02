/**
 @header@
 */

package org.pcmm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.prpdp.COPSPdpAgent;
import org.umu.cops.prpdp.COPSPdpConnection;
import org.umu.cops.prpdp.COPSPdpException;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.Socket;

/**
 * Core PDP agent for provisioning
 */
public class PCMMPdpAgent extends COPSPdpAgent {

    private final static Logger logger = LoggerFactory.getLogger(PCMMPdpAgent.class);

    /**
     * Policy data processing object
     */
    private final PCMMPdpDataProcess _process;

    /**
     * Creates a PDP Agent
     *
     * @param psHost     - Host to connect to
     * @param psPort     - Port to connect to
     * @param clientType - COPS Client-type
     * @param process    - Object to perform policy data processing
     */
    public PCMMPdpAgent(final String psHost, final int psPort, final short clientType, final PCMMPdpDataProcess process,
                        final short kaTimer, final short acctTimer) {
        super(psHost, psPort, clientType, kaTimer, acctTimer, process);
        this._process = process;
        logger.info("Created new PCMMPdpAgent");
    }

    @Override
    protected void validate(final COPSPepId pepId, final COPSMsg msg, final Socket conn) throws COPSException {
        COPSHandle handle = null;
        try {
            logger.info("PDP COPSTransceiver.receiveMsg");
            final COPSMsg rmsg = COPSTransceiver.receiveMsg(_socket);
            // Client-Close
            if (rmsg.getHeader().isAClientClose()) {
                // TODO - figure out a better means to determine the message type
                if (rmsg instanceof COPSClientCloseMsg) {
                    logger.info("Client close message - " + ((COPSClientCloseMsg) rmsg).getError().getDescription());
                    // close the _socket
                    final COPSHeader cHdr = new COPSHeader(COPSHeader.COPS_OP_CC, msg.getHeader().getClientType());
                    final COPSError err = new COPSError(COPSError.COPS_ERR_UNKNOWN_OBJECT, (short) 0);
                    final COPSClientCloseMsg closeMsg = new COPSClientCloseMsg();
                    closeMsg.add(cHdr);
                    closeMsg.add(err);
                    try {
                        closeMsg.writeData(conn);
                    } catch (IOException unae) {
                        logger.error("Unexpected error closing client close message", unae);
                    }
                    throw new COPSException("CMTS requetsed Client-Close");
                } else {
                    logger.error("Message is not an instance of COPSClientCloseMsg");
                }
            } else {
                // Request
                if (rmsg.getHeader().isARequest()) {
                    if (rmsg instanceof COPSReqMsg) {
                        handle = ((COPSReqMsg) rmsg).getClientHandle();
                    } else {
                        logger.error("Message is not an instance of COPSReqMsg");
                        throw new COPSException("Can't understand request");
                    }
                } else {
                    throw new COPSException("Can't understand request");
                }
            }
        } catch (Exception e) { // COPSException, IOException
            throw new COPSException("Error COPSTransceiver.receiveMsg");
        }

        logger.info("Obtaining PDPCOPSConnection for pepId - " + pepId + " to - " + conn.getInetAddress());
        final PCMMPdpConnection pdpConn = (PCMMPdpConnection) createPdpConnection(pepId, conn);

        if (handle != null) {
            final PCMMPdpReqStateMan man = new PCMMPdpReqStateMan(_clientType, handle.getId().str(), _process);
            pdpConn.addStateManager(handle, man);
            try {
                man.initRequestState(conn);
            } catch (COPSPdpException unae) {
                logger.error("Unexpected error initializing request state", unae);
            }
        } else {
            logger.error("Null handle, cannot request state manager");
        }
    }

    @Override
    protected COPSPdpConnection createPdpConnection(final COPSPepId pepId, final Socket conn) {
        return new PCMMPdpConnection(pepId, conn, _process, _kaTimer);
    }
}
