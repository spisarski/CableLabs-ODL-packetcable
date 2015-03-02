package org.umu.cops.prpdp;

import org.umu.cops.COPSReqStateMan;
import org.umu.cops.stack.COPSError;

/**
 * Shared functionality for all COPS Data processors.
 */
public interface COPSDataProcess {

    /**
     * Notifies a keep-alive timeout
     * @param man   The associated request state manager
     */
    public void notifyNoKAliveReceived(COPSReqStateMan man);

    /**
     * Notifies that the connection has been closed
     * @param man  The associated request state manager
     * @param error Reason
     */
    public void notifyClosedConnection(COPSReqStateMan man, COPSError error);

    /**
     * Notifies that a request state has been closed
     * @param man   The associated request state manager
     */
    public void closeRequestState(COPSReqStateMan man);

}
