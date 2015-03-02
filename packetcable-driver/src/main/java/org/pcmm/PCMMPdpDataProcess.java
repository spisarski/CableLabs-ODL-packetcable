/**
 @header@
 */

package org.pcmm;

import org.pcmm.gates.ITransactionID;
import org.pcmm.gates.impl.PCMMGateReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.COPSReqStateMan;
import org.umu.cops.prpdp.COPSPdpDataProcess;
import org.umu.cops.prpdp.COPSPdpReqStateMan;
import org.umu.cops.stack.COPSError;

import java.util.HashMap;
import java.util.Map;

public class PCMMPdpDataProcess implements COPSPdpDataProcess {

    private final static Logger logger = LoggerFactory.getLogger(PCMMPdpDataProcess.class);

    /**
     * PEP configuration items for sending inside the request
     *
     * @param man - the state manager
     * @param reqSIs - TODO Genericize
     */
    public void setClientData(final PCMMPdpReqStateMan man, final Map reqSIs) {
        logger.info("Request Info");
        /*
                for (Enumeration e = reqSIs.keys() ; e.hasMoreElements() ;) {
                    String strprid = (String) e.nextElement();
                    String strepd = (String) reqSIs.get(strprid);

                    // Check PRID-EPD
                    // ....
                    System.out.println(getClass().getName() + ": " + "PRID: " + strprid);
                    System.out.println(getClass().getName() + ": " + "EPD: " + strepd);
                }

                // Create policies to be deleted
                // ....

                // Create policies to be installed
                String prid = new String("<XPath>");
                String epd = new String("<?xml this is an XML policy>");
                installPolicy.put(prid, epd);
        */
    }

    @Override
    public Map getRemovePolicy(final COPSPdpReqStateMan man) {
        logger.info("Retrieving the remove policy");
        // TODO - Implement me
        return new HashMap();
    }

    @Override
    public Map getInstallPolicy(final COPSPdpReqStateMan man) {
        logger.info("Retrieving the install policy");
        // TODO - Implement me
        return new HashMap();
    }

    @Override
    public void setClientData(final COPSPdpReqStateMan man, final Map reqSIs) {
        logger.info("Setting the client data");
        // TODO - Implement me
    }

    @Override
    public void failReport (final COPSPdpReqStateMan man, final Map reportSIs) {
        logger.info("Fail report notified");
        // TODO - Implement me
    }

    /**
     * Fail report received
     * @param man - the state manager
     * @param gateMsg - the gate message
     */
    public void failReport(final PCMMPdpReqStateMan man, final PCMMGateReq gateMsg) {
        logger.info("Fail Report notified. - " + gateMsg.getError().toString());
        // TODO - Implement me
    }

    @Override
    public void successReport (final COPSPdpReqStateMan man, final Map reportSIs) {
        logger.info("Success report notified");
        // TODO - Implement me
    }

    /**
     * Positive report received
     * @param man - the state manager
     * @param gateMsg - the gate message
     */
    public void successReport(final PCMMPdpReqStateMan man, final PCMMGateReq gateMsg) {
        logger.info("Success Report notified.");

        if ( gateMsg.getTransactionID().getGateCommandType() == ITransactionID.GateDeleteAck ) {
            logger.info("GateDeleteAck: GateID = " + gateMsg.getGateID().getGateID());
            if (gateMsg.getGateID().getGateID() == PCMMGlobalConfig.getGateID1())
                PCMMGlobalConfig.setGateID1(0);
            if (gateMsg.getGateID().getGateID() == PCMMGlobalConfig.getGateID2())
                PCMMGlobalConfig.setGateID2(0);

        }
        if ( gateMsg.getTransactionID().getGateCommandType() == ITransactionID.GateSetAck ) {
            logger.info("GateSetAck: GateID = " + gateMsg.getGateID().getGateID());
            if (0 == PCMMGlobalConfig.getGateID1())
                PCMMGlobalConfig.setGateID1(gateMsg.getGateID().getGateID());
            if (0 == PCMMGlobalConfig.getGateID2())
                PCMMGlobalConfig.setGateID2(gateMsg.getGateID().getGateID());
        }
    }

    @Override
    public void acctReport (final COPSPdpReqStateMan man, final Map reportSIs) {
        logger.info("Acct Report notified.");
        // TODO - had to implement but do not know what to do here
    }
    /**
     * Accounting report received
     * @param man - the state manager (should be instance of PCMMPdpReqStateMan)
     * @param gateMsg - the gate message
     */
    public void acctReport(final PCMMPdpReqStateMan man, final PCMMGateReq gateMsg) {
        logger.info("Acct Report notified.");
        // TODO - Implement me
    }

    /**
     * Notifies that an Accounting report is missing
     * @param man - the state manager (should be instance of PCMMPdpReqStateMan)
     */
    @Override
    public void notifyNoAcctReport(final COPSPdpReqStateMan man) {
        logger.info("No Acct Report notified.");
        // TODO - Impelement me
    }

    /**
     * Notifies that a KeepAlive message is missing
     * @param man - the state manager (should be instance of PCMMPdpReqStateMan)
     */
    @Override
    public void notifyNoKAliveReceived(final COPSReqStateMan man) {
        logger.info("Notify No K alive received.");
        // TODO - Impelement me
    }

    /**
     * PEP closed the connection
     * @param man - the state manager (should be instance of PCMMPdpReqStateMan)
     * @param error - the COPS error
     */
    @Override
    public void notifyClosedConnection(final COPSReqStateMan man, final COPSError error) {
        logger.info("Connection was closed by PEP");
        // TODO - Implement me
    }

    /**
     * Delete request state received
     * @param man - the state manager (should be instance of PCMMPdpReqStateMan)
     */
    @Override
    public void notifyDeleteRequestState(final COPSPdpReqStateMan man) {
        logger.info("Delete request state notified");
        // TODO - Impelement me
    }

    /**
     * Closes request state
     * @param man - the state manager (should be instance of PCMMPdpReqStateMan)
     */
    @Override
    public void closeRequestState(final COPSReqStateMan man) {
        logger.info("Close request state notified");
        // TODO - Impelement me
    }
}
