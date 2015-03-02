/*
 * Copyright (c) 2004 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.prpep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.COPSReqStateMan;
import org.umu.cops.stack.*;

import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * COPSPepReqStateMan manages Request State using Client Handle (RFC 2748 pag. 21)
 * in PEP.
 *
 *   The client handle is used to identify a unique request state for a
 *   single PEP per client-type. Client handles are chosen by the PEP and
 *   are opaque to the PDP. The PDP simply uses the request handle to
 *   uniquely identify the request state for a particular Client-Type over
 *   a particular TCP connection and generically tie its decisions to a
 *   corresponding request. Client handles are initiated in request
 *   messages and are then used by subsequent request, decision, and
 *   report messages to reference the same request state. When the PEP is
 *   ready to remove a local request state, it will issue a delete message
 *   to the PDP for the corresponding client handle. A handle MUST be
 *   explicitly deleted by the PEP before it can be used by the PEP to
 *   identify a new request state. Handles referring to different request
 *   states MUST be unique within the context of a particular TCP
 *   connection and client-type.
 *
 * @version COPSPepReqStateMan.java, v 2.00 2004
 *
 */
public class COPSPepReqStateMan extends COPSReqStateMan {

    private final static Logger logger = LoggerFactory.getLogger(COPSPepReqStateMan.class);

    /**
        The PolicyDataProcess is used to process policy data in the PEP
     */
    protected final COPSPepDataProcess _process;

    /**
     *  State Request State
     */
    protected short _status;

    /**
        The Msg Sender is used to send COPS messages
     */
    protected COPSPepMsgSender _sender;

    /**
     * Sync State
     */
    protected boolean _syncState;

    /**
     * Create a State Request Manager
     *
     * @param    clientHandle                a Client Handle
     *
     */
    public COPSPepReqStateMan(final short clientType, final String clientHandle, final COPSPepDataProcess process) {
        super(clientType, clientHandle, process);
        _process = process;
        _syncState = true;
        logger.info("Created new COPS PEP request state manager");
    }

    /**
     * Init Request State
     *
     * @throws   COPSPepException
     *
     */
    @Override
    public void initRequestState(final Socket sock) throws COPSException {
        logger.info("Initializing request state");
        // Inits an object for sending COPS messages to the PDP
        _sender = new COPSPepMsgSender(_clientType, _handle, sock);

        // If an object for retrieving PEP features exists,
        // use it for retrieving them
        final Map<String, String> clientSIs;
        if (_process != null)
            clientSIs = _process.getClientData(this);
        else
            clientSIs = null;

        // Send the request
        _sender.sendRequest(clientSIs);

        // Initial state
        _status = ST_INIT;
    }

    /**
     * Finalize Request State
     *
     * @throws   COPSPepException
     *
     */
    protected void finalizeRequestState() throws COPSPepException {
        _sender.sendDeleteRequest();
        _status = ST_FINAL;
    }

    /**
     * Process the message Decision
     *
     * @param    dMsg                a  COPSDecisionMsg
     *
     * @throws   COPSPepException
     *
     */
    protected void processDecision(final COPSDecisionMsg dMsg) throws COPSPepException {
        logger.info("ClientId:" + _handle.getId().str());

        // COPSHandle handle = dMsg.getClientHandle();
        final Map<COPSContext, List<COPSDecision>> decisions = dMsg.getDecisions();

        final Map<String, String> removeDecs = new HashMap<>();
        final Map<String, String>  installDecs = new HashMap<>();
        final Map<String, String>  errorDecs = new HashMap<>();
        for (final List<COPSDecision> copsDecisions : decisions.values()) {
            final COPSDecision cmddecision = copsDecisions.get(0);

            // cmddecision --> we must check whether it is an error!
            // TODO - see if both blocks can be refactored as both are the same
            if (cmddecision.isInstallDecision()) {
                String prid = "";
                for (int i = 1; i < copsDecisions.size(); i++) {
                    final COPSDecision decision = copsDecisions.get(i);
                    final COPSPrObjBase obj = new COPSPrObjBase(decision.getData().getData());
                    switch (obj.getSNum()) {
                    case COPSPrObjBase.PR_PRID:
                        prid = obj.getData().str();
                        break;
                    case COPSPrObjBase.PR_EPD:
                        installDecs.put(prid, obj.getData().str());
                        break;
                    default:
                        break;
                    }
                }
            }
            if (cmddecision.isRemoveDecision()) {
                String prid = "";
                for (int i = 1; i < copsDecisions.size(); i++) {
                    final COPSDecision decision = copsDecisions.get(i);
                    final COPSPrObjBase obj = new COPSPrObjBase(decision.getData().getData());
                    switch (obj.getSNum()) {
                    case COPSPrObjBase.PR_PRID:
                        prid = obj.getData().str();
                        break;
                    case COPSPrObjBase.PR_EPD:
                        removeDecs.put(prid, obj.getData().str());
                        break;
                    default:
                        break;
                    }
                }
            }
        }

        //** Apply decisions to the configuration
        _process.setDecisions(this, removeDecs, installDecs, errorDecs);
        _status = ST_DECS;


        if (_process.isFailReport(this)) {
            logger.info("Sending FAIL Report");
            _sender.sendFailReport(_process.getReportData(this));
        } else {
            logger.info("Sending SUCCESS Report");
            _sender.sendSuccessReport(_process.getReportData(this));
        }
        _status = ST_REPORT;

        if (!_syncState) {
            _sender.sendSyncComplete();
            _syncState = true;
            _status = ST_SYNCALL;
        }
    }

    /**
     * Process the message NewRequestState
     *
     * @throws   COPSPepException
     *
     */
    protected void processOpenNewRequestState() throws COPSPepException {
        if (_process != null)
            _process.newRequestState(this);
        _status = ST_NEW;
    }

    /**
     * Process the message DeleteRequestState
     *
     * @param    dMsg                a  COPSDecisionMsg
     *
     * @throws   COPSPepException
     * TODO - determine what to do with the COPSDecisionMsg parameter
     */
    protected void processDeleteRequestState(final COPSDecisionMsg dMsg) throws COPSPepException {
        if (_process != null)
            _process.closeRequestState(this);
        _status = ST_DEL;
    }

    /**
     * Process the message SycnStateRequest.
     * The message SycnStateRequest indicates that the remote PDP
     * wishes the client (which appears in the common header)
     * to re-send its state.
     *
     * @param    ssMsg               a  COPSSyncStateMsg
     *
     * @throws   COPSPepException
     * TODO - determine what to do with the COPSSyncStateMsg
     */
    protected void processSyncStateRequest(final COPSSyncStateMsg ssMsg) throws COPSPepException {
        _syncState = false;
        // If an object for retrieving PEP features exists,
        // use it for retrieving them
        final Map<String, String> clientSIs;
        if (_process != null)
            clientSIs = _process.getClientData(this);
        else
            clientSIs = null;

        // Send request
        _sender.sendRequest(clientSIs);
        _status = ST_SYNC;
    }

    protected void processAcctReport() throws COPSPepException {
        final Map<String, String> report;
        if (_process != null)
            report = _process.getAcctData(this);
        else
            report = new HashMap<>();

        _sender.sendAcctReport(report);
        _status = ST_ACCT;
    }

}
