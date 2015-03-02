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
import org.umu.cops.common.COPSDebug;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.List;

/**
 * COPSPepConnection represents a PEP-PDP Connection Manager.
 * Responsible for processing messages received from PDP.
 */
public class COPSPepConnection extends COPSConnection implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(COPSPepConnection.class);

    /** Time to wait responses (milliseconds), default is 10 seconds */
//    protected final int _responseTime;

    /** COPS Client-type */
    protected final short _clientType;

    /**
        Accounting timer value (secs)
     */
    protected final short _acctTimer;

    /**
     *  Time of the latest keep-alive received
     */
    protected transient Date _lastRecKa;

    /**
        Opcode of the latest message sent
    */
    protected transient byte _lastmessage;

    /**
     * Creates a new PEP connection
     * @param clientType    PEP's client-type
     * @param sock          Socket connected to PDP
     */
    public COPSPepConnection(final COPSPepId pepId, final short clientType, final Socket sock, final short kaTimer,
                             final short acctTimer) {
        super(pepId, sock, kaTimer);
        _clientType = clientType;

        // Timers
        _acctTimer = acctTimer;
        _lastmessage = COPSHeader.COPS_OP_CAT;
        logger.info("New COPS PEP connection");
    }

    /**
     * Message-processing loop
     */
    public void run () {
        logger.info("Running in thread");
        Date _lastSendKa = new Date();
        Date _lastSendAcc = new Date();
        _lastRecKa = new Date();
        try {
            while (!_sock.isClosed()) {
                if (_sock.getInputStream().available() != 0) {
                    _lastmessage = processMessage(_sock);
                    _lastRecKa = new Date();
                }

                // Keep Alive
                if (_kaTimer > 0) {
                    // Timeout at PDP
                    int _startTime = (int) (_lastRecKa.getTime());
                    int cTime = (int) (new Date().getTime());

                    if ((cTime - _startTime) > _kaTimer * 1000) {
                        _sock.close();
                        // Notify all Request State Managers
                        notifyNoKAAllReqStateMan();
                    }

                    // Send to PEP
                    _startTime = (int) (_lastSendKa.getTime());
                    cTime = (int) (new Date().getTime());

                    if ((cTime - _startTime) > ((_kaTimer * 3/4) * 1000)) {
                        COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_KA);
                        COPSKAMsg msg = new COPSKAMsg();

                        msg.add(hdr);

                        COPSTransceiver.sendMsg(msg, _sock);
                        _lastSendKa = new Date();
                    }
                }

                // Accounting
                if (_acctTimer > 0) {
                    int _startTime = (int) (_lastSendAcc.getTime());
                    int cTime = (int) (new Date().getTime());

                    if ((cTime - _startTime) > ((_acctTimer* 3/4) * 1000)) {
                        // Notify all Request State Managers
                        notifyAcctAllReqStateMan();
                        _lastSendAcc = new Date();
                    }
                }

                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    logger.error("Unexpected error while thread was sleeping", e);
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error in thread", e);
        }

        // connection closed by server
        // COPSDebug.out(getClass().getName(),"Connection closed by server");
        try {
            _sock.close();
        } catch (IOException e) {
            logger.error("Unexpected error closing _socket", e);
        }

        // Notify all Request State Managers
        try {
            notifyCloseAllReqStateMan();
        } catch (COPSException e) {
            logger.error("Unexpected error closing all state managers");
        }
    }

    /**
     * Gets a COPS message from the _socket and processes it
     * @param conn  Socket connected to the PDP
     * @return COPS message type
     * @throws COPSException
     * @throws IOException
     */
    protected byte processMessage(final Socket conn) throws COPSException, IOException {
        final COPSMsg msg = COPSTransceiver.receiveMsg(conn);

        if (msg.getHeader().isAClientClose()) {
            if (msg instanceof COPSClientCloseMsg) {
                handleClientCloseMsg(conn, (COPSClientCloseMsg)msg);
            } else {
                logger.error("Message was not of the expected type of COPSClientCloseMsg");
            }
            return COPSHeader.COPS_OP_CC;
        } else if (msg.getHeader().isADecision()) {
            if (msg instanceof COPSDecisionMsg) {
                handleDecisionMsg(conn, (COPSDecisionMsg)msg);
            } else {
                logger.error("Message was not of the expected type of COPSDecisionMsg");
            }
            return COPSHeader.COPS_OP_DEC;
        } else if (msg.getHeader().isASyncStateReq()) {
            if (msg instanceof COPSSyncStateMsg) {
                handleSyncStateReqMsg(conn, (COPSSyncStateMsg)msg);
            } else {
                logger.error("Message was not of the expected type of COPSSyncStateMsg");
            }
            return COPSHeader.COPS_OP_SSQ;
        } else if (msg.getHeader().isAKeepAlive()) {
            if (msg instanceof COPSKAMsg) {
                handleKeepAliveMsg(conn, (COPSKAMsg)msg);
            } else {
                logger.error("Message was not of the expected type of COPSKAMsg");
            }
            return COPSHeader.COPS_OP_KA;
        } else {
            throw new COPSPepException("Message not expected (" + msg.getHeader().getOpCode() + ").");
        }
    }

    /**
     * Handle Keep Alive Message
     *
     * <Keep-Alive> ::= <Common Header>
     *                  [<Integrity>]
     *
     * Not support [<Integrity>]
     *
     * @param    conn                a  Socket
     * @param    cMsg                 a  COPSMsg
     *
     */
    private void handleKeepAliveMsg(final Socket conn, final COPSKAMsg cMsg) {
        logger.info("Get KAlive Msg");

        // Support
        if (cMsg.getIntegrity() != null) {
            logger.error(COPSDebug.ERROR_NOSUPPORTED
                          + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());
        }

        // should we do anything else?? ....
    }

    /**
     * Method handleDecisionMsg
     *
     * <Decision Message> ::= <Common Header: Flag SOLICITED>
     *                          <Client Handle>
     *                          *(<Decision>) | <Error>
     *                          [<Integrity>]
     * <Decision> ::= <Context>
     *                  <Decision: Flags>
     *                  [<Named Decision Data: Provisioning>]
     * <Decision: Flags> ::= <Command-Code> NULLFlag
     * <Command-Code> ::= NULLDecision | Install | Remove
     * <Named Decision Data> ::= <<Install Decision> | <Remove Decision>>
     * <Install Decision> ::= *(<PRID> <EPD>)
     * <Remove Decision> ::= *(<PRID> | <PPRID>)
     *
     * Very important, this is actually being treated like this:
     * <Install Decision> ::= <PRID> | <EPD>
     * <Remove Decision> ::= <PRID> | <PPRID>
     *
     * @param    conn                a  Socket
     * @param    dMsg                 a  COPSMsg
     */
    private void handleDecisionMsg(final Socket conn, final COPSDecisionMsg dMsg) throws COPSPepException {
        final COPSHandle handle = dMsg.getClientHandle();
        for (final List<COPSDecision> decisions : dMsg.getDecisions().values()) {
            for (final COPSDecision decision : decisions) {
                // Get the associated manager
                final COPSPepReqStateMan manager = (COPSPepReqStateMan) _managerMap.get(handle);
                if (manager == null)
                    logger.error("Null manager");
                else {
                    // Check message type
                    if (decision.getFlags() == COPSDecision.F_REQSTATE) {
                        if (decision.isRemoveDecision())
                            // Delete Request State
                            manager.processDeleteRequestState(dMsg);
                        else
                            // Open new Request State
                            handleOpenNewRequestStateMsg(conn, handle);
                    } else
                        // Decision
                        manager.processDecision(dMsg);
                }
            }
        }
    }


    /**
     * Method handleOpenNewRequestStateMsg
     *
     * @param    conn                a  Socket
     * @param    handle              a  COPSHandle
     * TODO - determin if the Socket conn object is really necessary here
     */
    private void handleOpenNewRequestStateMsg(final Socket conn, final COPSHandle handle) throws COPSPepException {
        final COPSPepReqStateMan manager = (COPSPepReqStateMan) _managerMap.get(handle);
        if (manager == null)
            logger.error(COPSDebug.ERROR_NOEXPECTEDMSG + " - Cannot open new state as manager is null");
        else
            manager.processOpenNewRequestState();
    }

    /**
     * Method handleSyncStateReqMsg
     *
     *              <Synchronize State> ::= <Common Header>
     *                                      [<Client Handle>]
     *                                      [<Integrity>]
     *
     * @param    conn                a  Socket
     * @param    cMsg                 a  COPSMsg
     */
    private void handleSyncStateReqMsg(final Socket conn, final COPSSyncStateMsg cMsg) throws COPSPepException {
        // Support
        if (cMsg.getIntegrity() != null) {
            logger.error(COPSDebug.ERROR_NOSUPPORTED
                          + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());
        }

        final COPSPepReqStateMan manager = (COPSPepReqStateMan) _managerMap.get(cMsg.getClientHandle());
        if (manager == null) {
            logger.error(COPSDebug.ERROR_NOEXPECTEDMSG + " - Cannot open new state as manager is null");
        } else {
            manager.processSyncStateRequest(cMsg);
        }
    }

    /**
     * Method createRequestState
     *
     * @param    clientHandle             a  String
     * @param    process                  a  COPSPepDataProcess
     *
     * @return   a COPSPepmanager
     *
     * @throws   COPSException
     * @throws   COPSPepException
     */
    public COPSPepReqStateMan addRequestState(final COPSHandle clientHandle, final COPSPepDataProcess process)
            throws COPSException {
        final COPSPepReqStateMan manager = new COPSPepReqStateMan(_clientType, clientHandle.getId().str(), process);
        if (_managerMap.get(clientHandle) != null)
            throw new COPSPepException("Duplicate Handle, rejecting " + clientHandle);

        _managerMap.put(clientHandle,manager);
        manager.initRequestState(_sock);
        return manager;
    }

    /**
     * Method deleteRequestState
     *
     * @param    manager             a  COPSPepReqStateMan
     *
     * @throws   COPSException
     * @throws   COPSPepException
     */
    public void deleteRequestState(final COPSPepReqStateMan manager) throws COPSException {
        manager.finalizeRequestState();
    }

    private void notifyAcctAllReqStateMan() throws COPSPepException {
        for (final COPSReqStateMan man : _managerMap.values()) {
            ((COPSPepReqStateMan)man).processAcctReport();
        }
    }

}

