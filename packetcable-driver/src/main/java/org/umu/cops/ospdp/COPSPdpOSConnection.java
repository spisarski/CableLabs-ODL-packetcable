package org.umu.cops.ospdp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.COPSOSConnection;
import org.umu.cops.COPSReqStateMan;
import org.umu.cops.common.COPSDebug;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.Socket;

/**
 * Class for managing an outsourcing connection at the PDP side.
 */
public class COPSPdpOSConnection extends COPSOSConnection implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(COPSPdpOSConnection.class);

    /**
     *  PDP policy data processor class
     */
    protected final COPSPdpOSDataProcess _process;

    /**
     * Creates a new PDP connection
     *
     * @param pepId PEP-ID of the connected PEP
     * @param sock  Socket connected to PEP
     * @param process   Object for processing policy data
     */
    public COPSPdpOSConnection(final COPSPepId pepId, final Socket sock, final COPSPdpOSDataProcess process,
                               final short acctTimer, final short kaTimer) {
        super(pepId, sock, kaTimer, acctTimer);
        _lastmessage = COPSHeader.COPS_OP_OPN;
        _process = process;
        logger.info("Created COPS PDP OS Connection");
    }

    @Override
    protected void accounting() {
        logger.info("Accounting");
        // Nothing to do here
    }

    /**
     * Gets a COPS message from the _socket and processes it
     * @param    conn Socket connected to the PEP
     * @return Type of COPS message
     */
    @Override
    protected byte processMessage(final Socket conn) throws COPSException, IOException {
        logger.info("Processing message");

        final COPSMsg msg = COPSTransceiver.receiveMsg(conn);

        if (msg.getHeader().isAClientClose()) {
            if (msg instanceof COPSClientCloseMsg) {
                handleClientCloseMsg(conn, (COPSClientCloseMsg)msg);
            } else {
                logger.error("Message is not of expected type COPSClientCloseMsg");
            }
            return COPSHeader.COPS_OP_CC;
        } else if (msg.getHeader().isAKeepAlive()) {
            if (msg instanceof COPSKAMsg) {
                handleKeepAliveMsg(conn, (COPSKAMsg)msg);
            } else {
                logger.error("Message is not of expected type COPSKAMsg");
            }
            return COPSHeader.COPS_OP_KA;
        } else if (msg.getHeader().isARequest()) {
            if (msg instanceof COPSReqMsg) {
                handleRequestMsg(conn, (COPSReqMsg)msg);
            } else {
                logger.error("Message is not of expected type COPSReqMsg");
            }
            return COPSHeader.COPS_OP_REQ;
        } else if (msg.getHeader().isAReport()) {
            if (msg instanceof COPSReportMsg) {
                handleReportMsg(conn, (COPSReportMsg)msg);
            } else {
                logger.error("Message is not of expected type COPSReportMsg");
            }
            return COPSHeader.COPS_OP_RPT;
        } else if (msg.getHeader().isADeleteReq()) {
            if (msg instanceof COPSDeleteMsg) {
                handleDeleteRequestMsg(conn, (COPSDeleteMsg)msg);
            } else {
                logger.error("Message is not of expected type COPSDeleteMsg");
            }
            return COPSHeader.COPS_OP_DRQ;
        } else if (msg.getHeader().isASyncComplete()) {
            if (msg instanceof COPSSyncStateMsg) {
                handleSyncComplete(conn, (COPSSyncStateMsg)msg);
            } else {
                logger.error("Message is not of expected type COPSSyncStateMsg");
            }
            return COPSHeader.COPS_OP_SSC;
        } else {
            throw new COPSPdpException("Message not expected (" + msg.getHeader().getOpCode() + ").");
        }
    }

    /**
     * Handle Keep Alive Message
     *
     * <Keep-Alive> ::= <Common Header>
     *                  [<Integrity>]
     *
     * Not support [<Integrity>]
     * @param    conn                a  Socket
     * @param    cMsg                 a  COPSMsg
     */
    private void handleKeepAliveMsg(final Socket conn, final COPSKAMsg cMsg) {
        try {
            // Support
            if (cMsg.getIntegrity() != null) {
                logger.error(COPSDebug.ERROR_NOSUPPORTED
                              + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());
            }
            cMsg.writeData(conn);
        } catch (final Exception unae) {
            logger.error("Unexpected error writing data", unae);
        }
    }

    /**
     * Handle Delete Request Message
     *
     * <Delete Request> ::= <Common Header>
     *                      <Client Handle>
     *                      <Reason>
     *                      [<Integrity>]
     *
     * Not support [<Integrity>]
     * @param    conn                a  Socket
     * @param    cMsg                 a  COPSMsg
     */
    private void handleDeleteRequestMsg(final Socket conn, final COPSDeleteMsg cMsg) throws COPSException {
        logger.info("Removing ClientHandle for "
                + conn.getInetAddress() + ":" + conn.getPort() + ":[Reason " + cMsg.getReason().getDescription() + "]");

        // Support
        if (cMsg.getIntegrity() != null) {
            logger.error(COPSDebug.ERROR_NOSUPPORTED
                          + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());
        }

        // Delete clientHandler
        if (_managerMap.remove(cMsg.getClientHandle()) == null) {
            logger.error("Missing for ClientHandle " + cMsg.getClientHandle());
        }

        final COPSPdpOSReqStateMan man = (COPSPdpOSReqStateMan) _managerMap.get(cMsg.getClientHandle());
        if (man == null) {
            logger.error(COPSDebug.ERROR_NOEXPECTEDMSG);
        } else {
            man.processDeleteRequestState(cMsg);
        }

    }

    /**
     * Handle Request Message
     *
     * <Request> ::= <Common Header>
     *                  <Client Handle>
     *                  <Context>
     *                  *(<Named ClientSI>)
     *                  [<Integrity>]
     * <Named ClientSI> ::= <*(<PRID> <EPD>)>
     *
     * Not support [<Integrity>]
     * @param    conn                a  Socket
     * @param    reqMsg                 a  COPSMsg
     */
    private void handleRequestMsg(final Socket conn, final COPSReqMsg reqMsg) throws COPSException {
        final COPSHeader header = reqMsg.getHeader();
        short cType   = header.getClientType();

        // Support
        if (reqMsg.getIntegrity() != null) {
            logger.error(COPSDebug.ERROR_NOSUPPORTED + " - Unsupported objects (Integrity) to connection " +
                    conn.getInetAddress());
        }

        COPSPdpOSReqStateMan man = (COPSPdpOSReqStateMan) _managerMap.get(reqMsg.getClientHandle());
        if (man == null) {
            man = new COPSPdpOSReqStateMan(cType, reqMsg.getClientHandle().getId().str(), _process);
            _managerMap.put(reqMsg.getClientHandle(), man);
            man.initRequestState(_sock);
        }

        man.processRequest(reqMsg);
    }

    /**
     * Handle Report Message
     *
     * <Report State> ::= <Common Header>
     *                      <Client Handle>
     *                      <Report Type>
     *                      *(<Named ClientSI>)
     *                      [<Integrity>]
     *
     * Not support [<Integrity>]
     * @param    conn                a  Socket
     * @param    repMsg                 a  COPSMsg
     */
    private void handleReportMsg(final Socket conn, final COPSReportMsg repMsg) throws COPSPdpException {
        // Support
        if (repMsg.getIntegrity() != null) {
            logger.error(COPSDebug.ERROR_NOSUPPORTED
                          + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());
        }

        final COPSPdpOSReqStateMan man = (COPSPdpOSReqStateMan) _managerMap.get(repMsg.getClientHandle());
        if (man == null) {
            logger.error(COPSDebug.ERROR_NOEXPECTEDMSG);
        } else {
            man.processReport(repMsg);
        }
    }

    /**
     * Method handleSyncComplete
     * @param    conn                a  Socket
     * @param    cMsg                 a  COPSMsg
     */
    private void handleSyncComplete(final Socket conn, final COPSSyncStateMsg cMsg) throws COPSPdpException {
        // Support
        if (cMsg.getIntegrity() != null) {
            logger.error(COPSDebug.ERROR_NOSUPPORTED
                          + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());
        }

        final COPSPdpOSReqStateMan man = (COPSPdpOSReqStateMan) _managerMap.get(cMsg.getClientHandle());
        if (man == null) {
            logger.error(COPSDebug.ERROR_NOEXPECTEDMSG);
        } else {
            man.processSyncComplete(cMsg);
        }
    }

    /**
     * Requests a COPS sync from the PEP
     * @throws COPSException
     * @throws COPSPdpException
     */
    protected void syncAllRequestState() throws COPSException {
        for (final COPSReqStateMan man : _managerMap.values()) {
            ((COPSPdpOSReqStateMan)man).syncRequestState();
        }
    }

}
