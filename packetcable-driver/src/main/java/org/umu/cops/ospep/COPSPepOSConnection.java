package org.umu.cops.ospep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.COPSOSConnection;
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
public class COPSPepOSConnection extends COPSOSConnection implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(COPSPepOSConnection.class);

    /** COPS Client-type */
    private final short _clientType;

    private transient Date _lastSendAcc = new Date();

    /** Time to wait responses (milliseconds), default is 10 seconds */
//    private final int _responseTime;

    /**
     * Creates a new PEP connection
     * @param clientType    PEP's client-type
     * @param sock          Socket connected to PDP
     */
    public COPSPepOSConnection(final COPSPepId pepId, final short clientType, final Socket sock, final short kaTimer,
                               final short acctTimer) {
        super(pepId, sock, kaTimer, acctTimer);
        _clientType = clientType;
    }

    @Override
    protected void accounting() throws COPSException {
        logger.info("Accounting");
        // Accounting
        if (_acctTimer > 0) {
            int _startTime = (int) (_lastSendAcc.getTime());
            int cTime = (int) (new Date().getTime());

            if ((cTime - _startTime) > ((_acctTimer*3/4)*1000)) {
                // Notify all Request State Managers
                notifyAcctAllReqStateMan();
                _lastSendAcc = new Date();
            }
        }
    }

    /**
     * Gets a COPS message from the _socket and processes it
     * @param conn  Socket connected to the PDP
     * @return COPS message type
     * @throws COPSPepException
     * @throws COPSException
     * @throws IOException
     */
    protected byte processMessage(final Socket conn) throws COPSException, IOException {
        final COPSMsg msg = COPSTransceiver.receiveMsg(conn);

        if (msg.getHeader().isAClientClose()) {
            if (msg instanceof COPSClientCloseMsg) {
                handleClientCloseMsg(conn, (COPSClientCloseMsg)msg);
            } else {
                logger.error("Message was not of expected type COPSClientCloseMsg");
            }
            return COPSHeader.COPS_OP_CC;
        } else if (msg.getHeader().isADecision()) {
            if (msg instanceof COPSDecisionMsg) {
                handleDecisionMsg((COPSDecisionMsg) msg);
            } else {
                logger.error("Message was not of expected type COPSDecisionMsg");
            }
            return COPSHeader.COPS_OP_DEC;
        } else if (msg.getHeader().isASyncStateReq()) {
            if (msg instanceof COPSSyncStateMsg) {
                handleSyncStateReqMsg(conn, (COPSSyncStateMsg) msg);
            } else {
                logger.error("Message was not of expected type COPSSyncStateMsg");
            }
            return COPSHeader.COPS_OP_SSQ;
        } else if (msg.getHeader().isAKeepAlive()) {
            if (msg instanceof COPSKAMsg) {
                handleKeepAliveMsg(conn, (COPSKAMsg) msg);
            } else {
                logger.error("Message was not of expected type COPSKAMsg");
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
        if (cMsg.getIntegrity() != null)
            logger.error(COPSDebug.ERROR_NOSUPPORTED
                    + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());

        // TODO - Do something else here???
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
     *                  [<ClientSI Decision Data: Outsourcing>]
     * <Decision: Flags> ::= <Command-Code> NULLFlag
     * <Command-Code> ::= NULLDecision | Install | Remove
     * <ClientSI Decision Data> ::= <<Install Decision> | <Remove Decision>>
     * <Install Decision> ::= *(<PRID> <EPD>)
     * <Remove Decision> ::= *(<PRID> | <PPRID>)
     *
     * @param    dMsg                 a  COPSMsg
     *
     */
    private void handleDecisionMsg(final COPSDecisionMsg dMsg) throws COPSException {
        final COPSHandle handle = dMsg.getClientHandle();
        final COPSPepOSReqStateMan manager = (COPSPepOSReqStateMan) _managerMap.get(handle);
        manager.processDecision(dMsg);
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
     *
     */
    private void handleSyncStateReqMsg(final Socket conn, final COPSSyncStateMsg cMsg) throws COPSPepException {
        // Support
        if (cMsg.getIntegrity() != null)
            logger.error(COPSDebug.ERROR_NOSUPPORTED
                          + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());

        final COPSPepOSReqStateMan manager = (COPSPepOSReqStateMan) _managerMap.get(cMsg.getClientHandle());

        if (manager == null)
            logger.error(COPSDebug.ERROR_NOEXPECTEDMSG);
        else
            manager.processSyncStateRequest(cMsg);
    }

    /**
     * Adds a new request state
     * @param clientHandle  Client's handle
     * @param process       Policy data processing object
     * @param clientSIs     Client data from the outsourcing event
     * @return              The newly created request state manager
     * @throws COPSException
     * @throws COPSPepException
     */
    protected COPSPepOSReqStateMan addRequestState(final COPSHandle clientHandle, final COPSPepOSDataProcess process,
                                                   List<COPSClientSI> clientSIs) throws COPSException {
        final COPSPepOSReqStateMan manager = new COPSPepOSReqStateMan(_clientType, clientHandle.getId().str(), process,
                clientSIs);
        if (_managerMap.get(clientHandle) != null)
            throw new COPSPepException("Duplicate Handle, rejecting " + clientHandle);
        _managerMap.put(clientHandle, manager);
        manager.initRequestState(_sock);
        return manager;
    }

    /**
     * Deletes a request state
     * @param manager   Request state manager
     * @throws COPSException
     * @throws COPSPepException
     */
    protected void deleteRequestState(final COPSPepOSReqStateMan manager) throws COPSException {
        manager.finalizeRequestState();
    }

    private void notifyAcctAllReqStateMan() throws COPSException {
        for (final COPSReqStateMan man : _managerMap.values()) {
            ((COPSPepOSReqStateMan)man).processAcctReport();
        }
    }

}
