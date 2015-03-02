/**
 @header@
 */

package org.pcmm;

import org.pcmm.gates.ITransactionID;
import org.pcmm.gates.impl.PCMMGateReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.prpdp.COPSPdpException;
import org.umu.cops.prpdp.COPSPdpReqStateMan;
import org.umu.cops.stack.*;

import java.net.Socket;
import java.util.Arrays;
import java.util.List;

/**
 * State manager class for provisioning requests, at the PDP side.
 */
public class PCMMPdpReqStateMan extends COPSPdpReqStateMan {

    private final static Logger logger = LoggerFactory.getLogger(PCMMPdpReqStateMan.class);

    private final PCMMPdpDataProcess _thisProcess;

    /**
     * Creates a request state manager
     *
     * @param clientType   Client-type
     * @param clientHandle Client handle
     */
    public PCMMPdpReqStateMan(final short clientType, final String clientHandle, final PCMMPdpDataProcess process) {
        super(clientType, clientHandle, process);
        _thisProcess = process;
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
        _sender = new PCMMPdpMsgSender(_clientType, _handle, sock);
        init();
    }

    /**
     * Processes a report
     *
     * @param msg Report message from the PEP
     * @throws COPSPdpException
     */
    @Override
    protected void processReport(final COPSReportMsg msg) throws COPSPdpException {
        logger.info("Processing report");
        //** Analyze the report
        //**

        /*
           * <Report State> ::= <Common Header>
           *                        <Client Handle>
          *                     <Report Type>
         *                      *(<Named ClientSI>)
          *                     [<Integrity>]
         * <Named ClientSI: Report> ::= <[<GPERR>] *(<report>)>
         * <report> ::= <ErrorPRID> <CPERR> *(<PRID><EPD>)
         *
         * Important, <Named ClientSI> is not parsed
        */

        // COPSHeader hdrmsg = msg.getHeader();
        // COPSHandle handlemsg = msg.getClientHandle();

        // WriteBinaryDump("COPSReportMessage", msg.getData().getData());
        // Report Type
        final COPSReportType rtypemsg = msg.getReport();

        // Named ClientSI
        final List<COPSClientSI> clientSIs = msg.getClientSI();
        final COPSClientSI myclientSI = msg.getClientSI().get(0);
        byte[] data = Arrays.copyOfRange(myclientSI.getData().getData(), 0, myclientSI.getData().getData().length);

        // PCMMUtils.WriteBinaryDump("COPSReportClientSI", data);
        logger.info("PCMMGateReq Parse Gate Message");
        final PCMMGateReq gateMsg = new PCMMGateReq(data);

//        final Map<String, String> repSIs = new HashMap<>();
        String strobjprid = "";
        for (final COPSClientSI clientSI : clientSIs) {
            final COPSPrObjBase obj = new COPSPrObjBase(clientSI.getData().getData());
            switch (obj.getSNum()) {
                case COPSPrObjBase.PR_PRID:
                    logger.info("COPSPrObjBase.PR_PRID");
                    strobjprid = obj.getData().str();
                    break;
                case COPSPrObjBase.PR_EPD:
                    logger.info("COPSPrObjBase.PR_EPD");
//                    repSIs.put(strobjprid, obj.getData().str());
                    break;
                default:
                    logger.info("Object s-num: " + obj.getSNum() + "stype " + obj.getSType());
                    logger.info("PRID: " + strobjprid);
                    logger.info("EPD: " + obj.getData().str());
                    break;
            }
        }

        logger.info("rtypemsg process");
        //** Here we must act in accordance with
        //** the report received
        if (rtypemsg.isSuccess()) {
            logger.info("rtypemsg success");
            _status = ST_REPORT;
            if (_thisProcess != null) {
                _thisProcess.successReport(this, gateMsg);
            } else {
                if (gateMsg.getTransactionID().getGateCommandType() == ITransactionID.GateDeleteAck) {
                    logger.info("GateDeleteAck: GateID = " + gateMsg.getGateID().getGateID());
                    if (gateMsg.getGateID().getGateID() == PCMMGlobalConfig.getGateID1()) {
                        PCMMGlobalConfig.setGateID1(0);
                    }
                    if (gateMsg.getGateID().getGateID() == PCMMGlobalConfig.getGateID2()) {
                        PCMMGlobalConfig.setGateID2(0);
                    }

                }
                if (gateMsg.getTransactionID().getGateCommandType() == ITransactionID.GateSetAck) {
                    logger.info("GateSetAck: GateID = " + gateMsg.getGateID().getGateID());
                    if (0 == PCMMGlobalConfig.getGateID1()) {
                        PCMMGlobalConfig.setGateID1(gateMsg.getGateID().getGateID());
                    }
                    if (0 == PCMMGlobalConfig.getGateID2()) {
                        PCMMGlobalConfig.setGateID2(gateMsg.getGateID().getGateID());
                    }
                }

            }
        } else {
            if (rtypemsg.isFailure()) {
                logger.info("rtypemsg failure");
                _status = ST_REPORT;
                if (_thisProcess != null) {
                    _thisProcess.failReport(this, gateMsg);
                } else {
                    logger.info("Gate message error - " + gateMsg.getError().toString());
                }

            } else {
                if (rtypemsg.isAccounting()) {
                    logger.info("rtypemsg account");
                    _status = ST_ACCT;
                    if (_thisProcess != null) {
                        _thisProcess.acctReport(this, gateMsg);
                    }
                }
            }
        }
        logger.info("Out processReport");
    }

}
