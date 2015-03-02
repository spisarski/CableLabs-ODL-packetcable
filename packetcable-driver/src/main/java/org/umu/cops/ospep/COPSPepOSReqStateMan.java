package org.umu.cops.ospep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.COPSReqStateMan;
import org.umu.cops.stack.COPSClientSI;
import org.umu.cops.stack.COPSDecisionMsg;
import org.umu.cops.stack.COPSException;
import org.umu.cops.stack.COPSSyncStateMsg;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * State manager class for outsourcing requests, at the PEP side.
 */
public class COPSPepOSReqStateMan extends COPSReqStateMan {

    private static final Logger logger = LoggerFactory.getLogger(COPSPepOSReqStateMan.class);

    /**
     * ClientSI data from signaling.
     */
    protected final List<COPSClientSI> _clientSIs;

    /**
        COPS message transceiver used to send COPS messages
     */
    protected transient COPSPepOSMsgSender _sender;

    /**
     * Sync state
     */
    protected transient boolean _syncState;

    /**
     * Same object as the super._process member but this one is properly cast.
     */
    private final COPSPepOSDataProcess _thisProcess;

    /**
     * Creates a state request manager
     * @param    clientType Client-type
     * @param   handle    Client's <tt>COPSHandle</tt>
     */
    public COPSPepOSReqStateMan(final short clientType, final String handle, final COPSPepOSDataProcess process,
                                final List<COPSClientSI> clientSIs) {
        super(clientType, handle, process);
        _syncState = true;
        _clientSIs = new ArrayList<>(clientSIs);
        _thisProcess = process;
        logger.info("Created new PepOS request state manager");
    }

    /**
     * Initializes a new request state over a _socket
     * @param sock  Socket to the PDP
     * @throws COPSPepException
     */
    public void initRequestState(Socket sock) throws COPSPepException {
        logger.info("Initializing request state");
        // Inits an object for sending COPS messages to the PDP
        _sender = new COPSPepOSMsgSender(_clientType, _handle, sock);

        // If an object exists for retrieving the PEP features,
        // use it for retrieving them.
        /*      Hashtable clientSIs;
                if (_process != null)
                    clientSIs = _process.getClientData(this);
                else
                    clientSIs = null;*/

        // Semd the request
        _sender.sendRequest(_clientSIs);

        // Initial state
        _status = ST_INIT;
    }

    /**
     * Deletes the request state
     * @throws COPSPepException
     */
    protected void finalizeRequestState() throws COPSPepException {
        _sender.sendDeleteRequest();
        _status = ST_FINAL;
    }

    /**
     * Processes the decision message
     * @param    dMsg Decision message from the PDP
     * @throws   COPSPepException
     */
    protected void processDecision(final COPSDecisionMsg dMsg) throws COPSException {
        // COPSDebug.out(getClass().getName(), "ClientId:" + getClientHandle().getId().str());

        //Hashtable decisionsPerContext = dMsg.getDecisions();

        //** Applies decisions to the configuration
        //_process.setDecisions(this, removeDecs, installDecs, errorDecs);
        // second param changed to dMsg so that the data processor
        // can check the 'solicited' flag
        final boolean isFailReport = _thisProcess.setDecisions(this, dMsg /*decisionsPerContext*/);
        _status = ST_DECS;

        if (isFailReport) {
            // COPSDebug.out(getClass().getName(),"Sending FAIL Report\n");
            _sender.sendFailReport(_thisProcess.getReportData(this));
        } else {
            // COPSDebug.out(getClass().getName(),"Sending SUCCESS Report\n");
            _sender.sendSuccessReport(_thisProcess.getReportData(this));
        }
        _status = ST_REPORT;

        if (!_syncState) {
            _sender.sendSyncComplete();
            _syncState = true;
            _status = ST_SYNCALL;
        }
    }


    /**
     * Processes a COPS delete message
     * @param dMsg  <tt>COPSDeleteMsg</tt> received from the PDP
     * @throws COPSPepException
     */
    protected void processDeleteRequestState(final COPSDecisionMsg dMsg) throws COPSPepException {
        if (_process != null)
            _process.closeRequestState(this);

        _status = ST_DEL;
    }

    /**
     * Processes the message SycnStateRequest.
     * The message SycnStateRequest indicates that the remote PDP
     * wishes the client (which appears in the common header)
     * to re-send its state.
     *
     * @param    ssMsg               The sync request from the PDP
     *
     * @throws   COPSPepException
     */
    protected void processSyncStateRequest(final COPSSyncStateMsg ssMsg) throws COPSPepException {
        _syncState = false;
        // If an object exists for retrieving the PEP features,
        // use it for retrieving them.

        // Send the request
        _sender.sendRequest(_clientSIs);

        _status = ST_SYNC;
    }

    /**
     * Processes the accounting report
     * @throws COPSPepException
     */
    protected void processAcctReport() throws COPSException {
        final List<COPSClientSI> clientSIs;

        if (_process != null)
            clientSIs = _thisProcess.getAcctData(this);
        else
            throw new COPSPepException("No clientSIs found");

        _sender.sendAcctReport(clientSIs);

        _status = ST_ACCT;
    }
}
