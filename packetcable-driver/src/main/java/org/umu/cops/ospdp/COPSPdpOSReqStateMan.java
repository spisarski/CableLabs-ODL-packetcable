package org.umu.cops.ospdp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.COPSReqStateMan;
import org.umu.cops.stack.*;

import java.net.Socket;
import java.util.List;

/**
 * State manager class for outsourcing requests, at the PDP side.
 */
public class COPSPdpOSReqStateMan extends COPSReqStateMan {

    private static final Logger logger = LoggerFactory.getLogger(COPSPdpOSReqStateMan.class);

    /** COPS message transceiver used to send COPS messages */
    protected transient COPSPdpOSMsgSender _sender;

    private final COPSPdpOSDataProcess _thisProcess;

    /**
     * Creates a request state manager
     * @param clientType    Client-type
     * @param clientHandle  Client handle
     */
    public COPSPdpOSReqStateMan(final short clientType, final String clientHandle, final COPSPdpOSDataProcess process) {
        super(clientType, clientHandle, process);
        _thisProcess = process;
        _status = ST_CREATE;
    }

    /**
     * Called when COPS sync is completed
     * @param    repMsg              COPS sync message
     * @throws   COPSPdpException
     */
    protected void processSyncComplete(final COPSSyncStateMsg repMsg) throws COPSPdpException {

        _status = ST_SYNCALL;

        // TODO - notifySyncComplete ...
    }

    /**
     * Initializes a new request state over a _socket
     * @param sock  Socket to the PEP
     * @throws COPSException
     */
    public void initRequestState(final Socket sock) throws COPSException {
        logger.info("Initializing request state");
        // Inits an object for sending COPS messages to the PDP
        _sender = new COPSPdpOSMsgSender(_clientType, _handle, sock);

        // Initial state
        _status = ST_INIT;
    }

    /**
     * Processes a COPS request
     * @param msg   COPS request received from the PEP
     * @throws COPSPdpException
     */
    protected void processRequest(final COPSReqMsg msg) throws COPSPdpException {
        //** Here we must retrieve a decision depending on the
        //** supplied ClientSIs
        /*Vector removeDecs = new Vector();
        Vector installDecs = new Vector();*/
        _thisProcess.setClientData(this, msg.getClientSI());

        final List<COPSDecision> removeDecs = _thisProcess.getRemovePolicy(this);
        final List<COPSDecision> installDecs = _thisProcess.getInstallPolicy(this);

        //** We create a SOLICITED decision
        //**
        _sender.sendSolicitedDecision(removeDecs, installDecs);
        _status = ST_DECS;
    }

    /**
     * Processes a report
     * @param msg   Report message from the PEP
     * @throws COPSPdpException
     */
    protected void processReport(final COPSReportMsg msg) throws COPSPdpException {
        //** Analyze the report
        //**

        /*
         * <Report State> ::= <Common Header>
         *                      <Client Handle>
         *                      <Report Type>
         *                      *(<Named ClientSI>)
         *                      [<Integrity>]
         * <Named ClientSI: Report> ::= <[<GPERR>] *(<report>)>
         * <report> ::= <ErrorPRID> <CPERR> *(<PRID><EPD>)
         *
         * Important, <Named ClientSI> is not parsed
        */

        // COPSHeader hdrmsg = msg.getHeader();
        // COPSHandle handlemsg = msg.getClientHandle();

        // Report Type
        final COPSReportType rtypemsg = msg.getReport();

        // Named ClientSI
        final List<COPSClientSI> clientSIs = msg.getClientSI();

        //** We should act here in accordance with
        //** the received report
        if (rtypemsg.isSuccess()) {
            _status = ST_REPORT;
            _thisProcess.successReport(this, clientSIs);
        } else if (rtypemsg.isFailure()) {
            _status = ST_REPORT;
            _thisProcess.failReport(this, clientSIs);
        } else if (rtypemsg.isAccounting()) {
            _status = ST_ACCT;
            _thisProcess.acctReport(this, clientSIs);
        }
    }

    /**
     * Called when connection is closed
     * @param error Reason
     * @throws COPSException
     */
    public void processClosedConnection(final COPSError error) throws COPSException {
        logger.info("Processing closed connection");
        if (_process != null)
            _process.notifyClosedConnection(this, error);

        _status = ST_CCONN;
    }

    /**
     * Called when no keep-alive is received
     * @throws COPSException
     */
    protected void processNoKAConnection() throws COPSException {
        if (_process != null)
            _process.notifyNoKAliveReceived(this);

        _status = ST_NOKA;
    }

    /**
     * Deletes the request state
     * @throws COPSPdpException
     */
    protected void finalizeRequestState() throws COPSPdpException {
        _sender.sendDeleteRequestState();
        _status = ST_FINAL;
    }

    /**
     * Asks for a COPS sync
     * @throws COPSPdpException
     */
    protected void syncRequestState() throws COPSPdpException {
        _sender.sendSyncRequestState();
        _status = ST_SYNC;
    }

    /**
     * Opens a new request state
     * @throws COPSPdpException
     */
    protected void openNewRequestState() throws COPSPdpException {
        _sender.sendOpenNewRequestState();
        _status = ST_NEW;
    }

    /**
     * Processes a COPS delete message
     * @param dMsg  <tt>COPSDeleteMsg</tt> received from the PEP
     * @throws COPSException
     */
    protected void processDeleteRequestState(final COPSDeleteMsg dMsg) throws COPSException {
        if (_process != null)
            _process.closeRequestState(this);

        _status = ST_DEL;
    }

}
