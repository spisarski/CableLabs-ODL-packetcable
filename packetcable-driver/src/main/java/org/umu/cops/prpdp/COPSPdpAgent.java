/*
 * Copyright (c) 2004 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.prpdp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.AbstractCOPSPdpAgent;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.Socket;

/**
 * Core PDP agent for provisioning
 */
public abstract class COPSPdpAgent extends AbstractCOPSPdpAgent {

    private final static Logger logger = LoggerFactory.getLogger(COPSPdpAgent.class);

    /**
     * The data processor
     */
    private final COPSPdpDataProcess _process;

    /**
     * Creates a PDP Agent
     *
     * @param port       Port to listen to
     * @param clientType COPS Client-type
     * @param process    Object to perform policy data processing
     */
    public COPSPdpAgent(final String host, final int port, final short clientType, final short kaTimer,
                        final short acctTimer, final COPSPdpDataProcess process) {
        super(host, port, clientType, kaTimer, acctTimer);
        this._process = process;
        logger.info("Create new COPS PDP Agent");
    }

    /**
     * Requests a COPS sync for a PEP
     *
     * @param pepID PEP-ID of the PEP to be synced
     * @throws COPSException
     * @throws COPSPdpException
     */
    public void sync(final String pepID) throws COPSException {
        logger.info("Synching with pepID - " + pepID);
        final COPSPdpConnection pdpConn = (COPSPdpConnection)_connectionMap.get(pepID);
        pdpConn.syncAllRequestState();
    }

    /**
     * Handles a COPS client-open message
     *
     * @param conn Socket to the PEP
     * @param cMsg  <tt>COPSMsg</tt> holding the client-open message
     * @throws COPSException
     * @throws IOException
     */
    @Override
    protected void handleClientOpenMsg(final Socket conn, final COPSClientOpenMsg cMsg)
            throws COPSException, IOException {
        logger.info("Handling client open message");
        final COPSPepId pepId = cMsg.getPepId();

        // Validate Client Type
        if (cMsg.getHeader().getClientType() != _clientType || pepId == null || cMsg.getClientSI() == null) {
            final COPSHeader cHdr = new COPSHeader(COPSHeader.COPS_OP_CC, cMsg.getHeader().getClientType());
            final COPSClientCloseMsg closeMsg = new COPSClientCloseMsg();

            final COPSError err;
            if (cMsg.getHeader().getClientType() != _clientType) {
                // Unsupported client type
                err = new COPSError(COPSError.COPS_ERR_UNSUPPORTED_CLIENT_TYPE, (short) 0);
            } else if (pepId == null) {
                // PEPId is mandatory
                err = new COPSError(COPSError.COPS_ERR_MANDATORY_OBJECT_MISSING, (short) 0);
            } else {
                err = new COPSError(COPSError.COPS_ERR_UNKNOWN_OBJECT, (short) 0);
            }
            closeMsg.add(cHdr);
            closeMsg.add(err);
            try {
                closeMsg.writeData(conn);
            } catch (IOException unae) {
                logger.error("Unexpected error writing COPS message", unae);
            }

            // TODO - determine if a checked/thrown exception is a good pattern here
            if (cMsg.getHeader().getClientType() != _clientType) {
                throw new COPSException("Unsupported client type");
            } else if (pepId == null) {
                throw new COPSException("Mandatory COPS object missing (PEPId)");
            } else {
                throw new COPSException("Unsupported objects (PdpAddress, Integrity)");
            }
        }

        // Connection accepted
        final COPSHeader ahdr = new COPSHeader(COPSHeader.COPS_OP_CAT, cMsg
                .getHeader().getClientType());
        final COPSKATimer katimer = new COPSKATimer(_kaTimer);
        final COPSAcctTimer acctTimer = new COPSAcctTimer();
        final COPSClientAcceptMsg acceptMsg = new COPSClientAcceptMsg();
        acceptMsg.add(ahdr);
        acceptMsg.add(katimer);

        // TODO - many need to make this value a readonly member value
        if (_acctTimer != 0) {
            acceptMsg.add(acctTimer);
        }
        acceptMsg.writeData(conn);

        final COPSPdpConnection pdpConn = createPdpConnection(pepId, conn);
        logger.info("Starting PDP Connection thread");
        final Thread thread = new Thread(pdpConn, "COPS Client - " + pepId.getData().str());
        thread.start();
        _threadMap.put(pdpConn, thread);
        addPepConnection(pepId.getData().str(), pdpConn);
    }

    protected void validate(final COPSPepId pepId, final COPSMsg msg, final Socket conn) throws COPSException {
        // Nothing to do right now. May want to move logic from PCMMPdpAgent here
    }

    protected COPSPdpConnection createPdpConnection(final COPSPepId pepId, final Socket conn) {
        return new COPSPdpConnection(pepId, conn, _process, _kaTimer);
    }

}



