package org.umu.cops.ospep;

import org.umu.cops.prpdp.COPSDataProcess;
import org.umu.cops.stack.COPSClientSI;
import org.umu.cops.stack.COPSDecisionMsg;
import org.umu.cops.stack.COPSError;

import java.util.List;

/**
 * Abstract class for implementing policy data processing classes for outsourcing PEPs.
 */
public interface COPSPepOSDataProcess extends COPSDataProcess {
    /**
     * Applies the decisions from the PDP
     * @param man   The request state manager
     * @param dMsg  The decisions message
     * @return <tt>true</tt> if failed (reports indicate failure), <tt>false</tt> otherwise
     */
    public boolean setDecisions(COPSPepOSReqStateMan man, COPSDecisionMsg dMsg);

    /**
     * Gets the report data
     * @param man   The request state manager
     * @return A <tt>Vector</tt> holding the report data
     */
    public List<COPSClientSI> getReportData(COPSPepOSReqStateMan man);

    /**
     * Gets the supplied client data
     * @param man   The request state manager
     * @return A <tt>Vector</tt> holding the client data
     */
    public List<COPSClientSI> getClientData(COPSPepOSReqStateMan man);

    /**
     * Gets the account data
     * @param man   The request state manager
     * @return A <tt>Vector</tt> holding the account data
     */
    public List<COPSClientSI> getAcctData(COPSPepOSReqStateMan man);

    /**
     * Called when the connection is closed
     * @param man   The request state manager
     * @param error Reason
     */
    public void notifyClosedConnection (COPSPepOSReqStateMan man, COPSError error);

    /**
     * Called when the keep-alive message is not received
     * @param man   The request state manager
     */
    public void notifyNoKAliveReceived (COPSPepOSReqStateMan man);

    /**
     * Process a PDP request to close a Request State
     * @param man   The request state manager
     */
    public void closeRequestState(COPSPepOSReqStateMan man);
}
