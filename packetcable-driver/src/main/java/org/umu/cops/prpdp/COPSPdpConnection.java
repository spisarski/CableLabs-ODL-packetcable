/*
 * Copyright (c) 2004 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.prpdp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.COPSConnection;
import org.umu.cops.COPSReqStateMan;
import org.umu.cops.common.COPSDebug;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

/**
 * Class for managing an provisioning connection at the PDP side.
 */
public class COPSPdpConnection extends COPSConnection implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(COPSPdpConnection.class);

    /**
     * Time of the latest keep-alive received
     */
    protected Date _lastRecKa;

    /**
     * PDP policy data processor class
     */
    protected final COPSPdpDataProcess _process;

    /**
     * Creates a new PDP connection
     *
     * @param sock    Socket connected to PEP
     * @param process Object for processing policy data (this value can be null)
     * @param kaTimer timer value
     */
    public COPSPdpConnection(final COPSPepId pepId, final Socket sock, final COPSPdpDataProcess process,
                             final short kaTimer) {
        super(pepId, sock, kaTimer);
        _process = process;
        logger.info("Creating new COPS PDP Connection");
    }

    /**
     * Main loop
     */
    public void run() {
        Date _lastSendKa = new Date();
        _lastRecKa = new Date();
        try {
            while (_sock.isConnected()) {
                if (_sock.getInputStream().available() != 0) {
                    _lastRecKa = new Date();
                }

                // Keep Alive
                if (_kaTimer > 0) {
                    // Timeout at PDP
                    int _startTime = (int) (_lastRecKa.getTime());
                    int cTime = (int) (new Date().getTime());

                    if (cTime - _startTime > _kaTimer * 1000) {
                        _sock.close();
                        // Notify all Request State Managers
                        notifyNoKAAllReqStateMan();
                    }

                    // Send to PEP
                    _startTime = (int) (_lastSendKa.getTime());
                    cTime = (int) (new Date().getTime());

                    if ((cTime - _startTime) > ((_kaTimer * 3 / 4) * 1000)) {
                        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_KA);
                        final COPSKAMsg msg = new COPSKAMsg();

                        msg.add(hdr);

                        COPSTransceiver.sendMsg(msg, _sock);
                        _lastSendKa = new Date();
                    }
                }

                try {
                    // TODO - find a better means than sleep (i.e. make this into a Timer)
                    Thread.sleep(500);
                } catch (Exception e) {
                    logger.error("Unexpected error while thread was sleeping", e);
                }
            }
        } catch (final Exception e) {
            logger.error(COPSDebug.ERROR_SOCKET, e);
        }

        // connection closed by server
        // COPSDebug.out(getClass().getName(),"Connection closed by client");
        try {
            _sock.close();
        } catch (final IOException e) {
            logger.error("Unexpected error closing _socket");
        }

        // Notify all Request State Managers
        try {
            notifyCloseAllReqStateMan();
        } catch (final COPSException e) {
            logger.error("Unexpected error notifying all request state managers");
        }
    }

    /**
     * Gets a COPS message from the _socket and processes it
     *
     * @param conn Socket connected to the PEP
     * @return Type of COPS message
     */
    private byte processMessage(final Socket conn) throws COPSException, IOException {
        final COPSMsg msg = COPSTransceiver.receiveMsg(conn);

        // TODO - determine a better means to determing the message type
        if (msg.getHeader().isAClientClose()) {
            if (msg instanceof COPSClientCloseMsg) {
                handleClientCloseMsg(conn, (COPSClientCloseMsg)msg);
            } else {
                logger.error("Close message is not of type COPSClientCloseMsg");
            }
            return COPSHeader.COPS_OP_CC;
        } else if (msg.getHeader().isAKeepAlive()) {
            if (msg instanceof COPSKAMsg) {
                handleKeepAliveMsg(conn, (COPSKAMsg)msg);
            } else {
                logger.error("Close message is not of type COPSKAMsg");
            }
            return COPSHeader.COPS_OP_KA;
        } else if (msg.getHeader().isARequest()) {
            if (msg instanceof COPSReqMsg) {
                handleRequestMsg(conn, (COPSReqMsg)msg);
            } else {
                logger.error("Close message is not of type COPSReqMsg");
            }
            return COPSHeader.COPS_OP_REQ;
        } else if (msg.getHeader().isAReport()) {
            if (msg instanceof COPSReportMsg) {
                handleReportMsg(conn, (COPSReportMsg)msg);
            } else {
                logger.error("Close message is not of type COPSReportMsg");
            }
            return COPSHeader.COPS_OP_RPT;
        } else if (msg.getHeader().isADeleteReq()) {
            if (msg instanceof COPSDeleteMsg) {
                handleDeleteRequestMsg(conn, (COPSDeleteMsg)msg);
            } else {
                logger.error("Close message is not of type COPSDeleteMsg");
            }
            return COPSHeader.COPS_OP_DRQ;
        } else if (msg.getHeader().isASyncComplete()) {
            if (msg instanceof COPSSyncStateMsg) {
                handleSyncComplete(conn, (COPSSyncStateMsg)msg);
            } else {
                logger.error("Close message is not of type COPSSyncStateMsg");
            }
            return COPSHeader.COPS_OP_SSC;
        } else {
            throw new COPSPdpException(
                "Message not expected (" + msg.getHeader().getOpCode() + ").");
        }
    }

    /**
     * Handle Keep Alive Message
     * <p/>
     * <Keep-Alive> ::= <Common Header>
     * [<Integrity>]
     * <p/>
     * Not support [<Integrity>]
     *
     * @param conn a  Socket
     * @param cMsg  a  COPSMsg
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
            logger.error("Unexpeced error closing the connection", unae);
        }
    }

    /**
     * Handle Delete Request Message
     * <p/>
     * <Delete Request> ::= <Common Header>
     * <Client Handle>
     * <Reason>
     * [<Integrity>]
     * <p/>
     * Not support [<Integrity>]
     *
     * @param conn a  Socket
     * @param cMsg  a  COPSMsg
     */
    private void handleDeleteRequestMsg(final Socket conn, final COPSDeleteMsg cMsg) throws COPSPdpException {
        // Support
        if (cMsg.getIntegrity() != null) {
            logger.error(COPSDebug.ERROR_NOSUPPORTED
                    + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());
        }

        // Delete clientHandler
        _managerMap.remove(cMsg.getClientHandle());

        final COPSReqStateMan man = _managerMap.get(cMsg.getClientHandle());
        if (man == null) {
            logger.error(COPSDebug.ERROR_NOEXPECTEDMSG);
        } else {
            ((COPSPdpReqStateMan)man).processDeleteRequestState(cMsg);
        }
    }

    /**
     * Handle Request Message
     * <p/>
     * <Request> ::= <Common Header>
     * <Client Handle>
     * <Context>
     * *(<Named ClientSI>)
     * [<Integrity>]
     * <Named ClientSI> ::= <*(<PRID> <EPD>)>
     * <p/>
     * Not support [<Integrity>]
     *
     * @param conn a  Socket
     * @param reqMsg  a  COPSMsg
     */
    private void handleRequestMsg(final Socket conn, final COPSReqMsg reqMsg) throws COPSException {

        final COPSHeader header = reqMsg.getHeader();
        final short cType = header.getClientType();

        // Support
        if (reqMsg.getIntegrity() != null) {
            logger.error(COPSDebug.ERROR_NOSUPPORTED
                    + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());
        }
        final COPSPdpReqStateMan man;
        if (_managerMap.get(reqMsg.getClientHandle()) == null) {

            man = new COPSPdpReqStateMan(cType, reqMsg.getClientHandle().getId().str(), _process);
            _managerMap.put(reqMsg.getClientHandle(), man);
            man.initRequestState(_sock);
        } else {
            man = (COPSPdpReqStateMan)_managerMap.get(reqMsg.getClientHandle());
        }
        man.processRequest(reqMsg);
    }

    public void addStateManager(final COPSHandle handle, final COPSPdpReqStateMan man) {
        _managerMap.put(handle, man);
    }

    /**
     * Handle Report Message
     * <p/>
     * <Report State> ::= <Common Header>
     * <Client Handle>
     * <Report Type>
     * *(<Named ClientSI>)
     * [<Integrity>]
     * <p/>
     * Not support [<Integrity>]
     *
     * @param conn a  Socket
     * @param repMsg  a  COPSMsg
     */
    private void handleReportMsg(final Socket conn, final COPSReportMsg repMsg) throws COPSPdpException {
        // Support
        if (repMsg.getIntegrity() != null) {
            logger.error(COPSDebug.ERROR_NOSUPPORTED
                    + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());
        }
        final COPSPdpReqStateMan man = (COPSPdpReqStateMan)_managerMap.get(repMsg.getClientHandle());
        if (man == null) {
            logger.error(COPSDebug.ERROR_NOEXPECTEDMSG);
        } else {
            man.processReport(repMsg);
        }
    }

    /**
     * Method handleSyncComplete
     *
     * @param conn a  Socket
     * @param cMsg  a  COPSMsg
     */
    private void handleSyncComplete(final Socket conn, final COPSSyncStateMsg cMsg) throws COPSPdpException {

        // Support
        if (cMsg.getIntegrity() != null) {
            logger.error(COPSDebug.ERROR_NOSUPPORTED
                    + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());
        }
        final COPSPdpReqStateMan man = (COPSPdpReqStateMan)_managerMap.get(cMsg.getClientHandle());
        if (man == null) {
            logger.error(COPSDebug.ERROR_NOEXPECTEDMSG);
        } else {
            man.processSyncComplete(cMsg);
        }
    }

    /**
     * Requests a COPS sync from the PEP
     *
     * @throws COPSException
     */
    protected void syncAllRequestState() throws COPSException {
        for (final COPSReqStateMan man : _managerMap.values()) {
            ((COPSPdpReqStateMan)man).syncRequestState();
        }
    }

}

