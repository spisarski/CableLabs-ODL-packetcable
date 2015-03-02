/*
 * Copyright (c) 2004 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.prpdp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.COPSReqStateMan;
import org.umu.cops.stack.*;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * State manager class for provisioning requests, at the PDP side.
 */
public class COPSPdpReqStateMan extends COPSReqStateMan {

    private final static Logger logger = LoggerFactory.getLogger(COPSPdpReqStateMan.class);

    /**
     * Object for performing policy data processing
     * This value can be null
     */
    private final COPSPdpDataProcess _pdpProcess;

    /**
     * Current state of the request being managed
     */
    protected short _status;

    /**
     * COPS message transceiver used to send COPS messages
     */
    protected transient COPSPdpMsgSender _sender;

    /**
     * Creates a request state manager
     *
     * @param clientType   Client-type
     * @param clientHandle Client handle
     */
    public COPSPdpReqStateMan(final short clientType, final String clientHandle) {
        this(clientType, clientHandle, null);
    }

    /**
     * Creates a request state manager
     *
     * @param clientType   Client-type
     * @param clientHandle Client handle
     * @param process      the data processor which can be null
     */
    public COPSPdpReqStateMan(final short clientType, final String clientHandle, final COPSPdpDataProcess process) {
        super(clientType, clientHandle, process);
        _pdpProcess = process;
        logger.info("New COPS PDP request state manager");
    }

    /**
     * Called when COPS sync is completed
     *
     * @param repMsg COPS sync message
     * @throws COPSPdpException
     */
    public void processSyncComplete(final COPSSyncStateMsg repMsg) throws COPSPdpException {
        logger.info("Process sync complete");
        _status = ST_SYNCALL;
        // TODO - notifySyncComplete ...
    }

    /**
     * Initializes a new request state over a _socket
     *
     * @param sock Socket to the PEP
     * @throws COPSPdpException
     */
    @Override
    public void initRequestState(final Socket sock) throws COPSException {
        logger.info("Initializing request state");
        // Inits an object for sending COPS messages to the PEP
        _sender = new COPSPdpMsgSender(_clientType, _handle, sock);
        init();
    }

    /**
     * Sets the initial status
     */
    protected void init() {
        // Initial state
        _status = ST_INIT;
    }

    /**
     * Processes a COPS request
     *
     * @param msg COPS request received from the PEP
     * @throws COPSPdpException
     */
    public void processRequest(final COPSReqMsg msg) throws COPSPdpException {
        logger.info("Processing request");
        // TODO - Implement me
/*
        final COPSHeader hdrmsg = msg.getHeader();
        final COPSHandle handlemsg = msg.getClientHandle();
        final COPSContext contextmsg = msg.getContext();
*/

        //** Analyze the request
        //**

        /* <Request> ::= <Common Header>
        *                   <Client Handle>
        *                   <Context>
        *                   *(<Named ClientSI>)
        *                   [<Integrity>]
        * <Named ClientSI> ::= <*(<PRID> <EPD>)>
        *
        * Very important, this is actually being treated like this:
        * <Named ClientSI> ::= <PRID> | <EPD>
        *

        // Named ClientSI
        Vector clientSIs = msg.getClientSI();
        Hashtable reqSIs = new Hashtable(40);
        String strobjprid = new String();
        for (Enumeration e = clientSIs.elements() ; e.hasMoreElements() ;) {
            COPSClientSI clientSI = (COPSClientSI) e.nextElement();

            COPSPrObjBase obj = new COPSPrObjBase(clientSI.getData().getData());
            switch (obj.getSNum())
            {
                case COPSPrObjBase.PR_PRID:
                    strobjprid = obj.getData().str();
                    break;
                case COPSPrObjBase.PR_EPD:
                    reqSIs.put(strobjprid, obj.getData().str());
                    // COPSDebug.out(getClass().getName(),"PRID: " + strobjprid);
                    // COPSDebug.out(getClass().getName(),"EPD: " + obj.getData().str());
                    break;
                default:
                    break;
            }
        }

        //** Here we must retrieve a decision depending on
        //** the supplied ClientSIs
        // reqSIs is a hashtable with the prid and epds

        // ................
        //
        Hashtable removeDecs = new Hashtable();
        Hashtable installDecs = new Hashtable();
        _pdpProcess.setClientData(this, reqSIs);

        removeDecs = _pdpProcess.getRemovePolicy(this);
        installDecs = _pdpProcess.getInstallPolicy(this);

        //** We create the SOLICITED decision
        //**
        _sender.sendDecision(removeDecs, installDecs);
        _status = ST_DECS;
        */
    }

    /**
     * Processes a report
     *
     * @param msg Report message from the PEP
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
        final Map<String, String> repSIs = new HashMap<>();
        String strobjprid = "";
        for (final COPSClientSI clientSI : msg.getClientSI()) {
            COPSPrObjBase obj = new COPSPrObjBase(clientSI.getData().getData());
            switch (obj.getSNum()) {
                case COPSPrObjBase.PR_PRID:
                    strobjprid = obj.getData().str();
                    break;
                case COPSPrObjBase.PR_EPD:
                    repSIs.put(strobjprid, obj.getData().str());
                    // COPSDebug.out(getClass().getName(),"PRID: " + strobjprid);
                    // COPSDebug.out(getClass().getName(),"EPD: " + obj.getData().str());
                    break;
                default:
                    break;
            }
        }

        //** Here we must act in accordance with
        //** the report received
        if (rtypemsg.isSuccess()) {
            _status = ST_REPORT;
            if (_pdpProcess != null) _pdpProcess.successReport(this, repSIs);
        } else if (rtypemsg.isFailure()) {
            _status = ST_REPORT;
            if (_pdpProcess != null) _pdpProcess.failReport(this, repSIs);
        } else  if (rtypemsg.isAccounting()) {
            _status = ST_ACCT;
            if (_pdpProcess != null) _pdpProcess.acctReport(this, repSIs);
        }
    }

    /**
     * Deletes the request state
     *
     * @throws COPSPdpException
     */
    protected void finalizeRequestState() throws COPSPdpException {
        if (_sender != null) {
            _sender.sendDeleteRequestState();
        }
        _status = ST_FINAL;
    }

    /**
     * Asks for a COPS sync
     *
     * @throws COPSPdpException
     */
    protected void syncRequestState() throws COPSPdpException {
        if (_sender != null) {
            _sender.sendSyncRequestState();
        }
        _status = ST_SYNC;
    }

    /**
     * Opens a new request state
     *
     * @throws COPSPdpException
     */
    protected void openNewRequestState() throws COPSPdpException {
        if (_sender != null) {
            _sender.sendOpenNewRequestState();
        }
        _status = ST_NEW;
    }

    /**
     * Processes a COPS delete message
     *
     * @param dMsg <tt>COPSDeleteMsg</tt> received from the PEP
     * @throws COPSPdpException
     */
    public void processDeleteRequestState(COPSDeleteMsg dMsg) throws COPSPdpException {
        logger.info("Process delete request state");
        if (_pdpProcess != null) {
            _pdpProcess.closeRequestState(this);
        }
        _status = ST_DEL;
    }

}
