package org.umu.cops;

import org.umu.cops.prpdp.COPSDataProcess;
import org.umu.cops.prpdp.COPSPdpException;
import org.umu.cops.stack.COPSData;
import org.umu.cops.stack.COPSError;
import org.umu.cops.stack.COPSException;
import org.umu.cops.stack.COPSHandle;

import java.net.Socket;

/**
 * Abstract state manager class for provisioning requests, at the PEP/PDP side.
 */
public abstract class COPSReqStateMan {

    /**
     * Request State created
     */
    public final static short ST_CREATE = 1;
    /**
     * Request received
     */
    public final static short ST_INIT = 2;
    /**
     * Decisions sent
     */
    public final static short ST_DECS = 3;
    /**
     * Report received
     */
    public final static short ST_REPORT = 4;
    /**
     * Request State finalized
     */
    public final static short ST_FINAL = 5;
    /**
     * New Request State solicited
     */
    public final static short ST_NEW = 6;
    /**
     * Delete Request State solicited
     */
    public final static short ST_DEL = 7;
    /**
     * SYNC request sent
     */
    public final static short ST_SYNC = 8;
    /**
     * SYNC completed
     */
    public final static short ST_SYNCALL = 9;
    /**
     * Close connection received
     */
    public final static short ST_CCONN = 10;
    /**
     * Keep-alive timeout
     */
    public final static short ST_NOKA = 11;
    /**
     * Accounting timeout
     */
    public final static short ST_ACCT = 12;

    /**
     * COPS client-type that identifies the policy client
     */
    protected final short _clientType;

    /**
     * COPS client handle used to uniquely identify a particular
     * PEP's request for a client-type
     */
    protected final COPSHandle _handle;

    /**
     *  State Request State
     */
    protected transient short _status;

    protected final COPSDataProcess _process;

    // TODO - may want to send in the COPSHandle object instead of making one each instantiation
    public COPSReqStateMan(final short clientType, final String handle, final COPSDataProcess process) {
        this._clientType = clientType;
        _process = process;
        _handle = new COPSHandle(new COPSData(handle));
        _status = ST_CREATE;
    }

    /**
     * Initializes a new request state over a _socket
     *
     * @param sock Socket to the PEP
     * @throws org.umu.cops.prpdp.COPSPdpException
     */
    public abstract void initRequestState(Socket sock) throws COPSException;

    /**
     * Called when connection is closed
     *
     * @param error Reason
     * @throws org.umu.cops.prpdp.COPSPdpException
     */
    public void processClosedConnection(final COPSError error) throws COPSException {
        if (_process != null) {
            _process.notifyClosedConnection(this, error);
        }

        _status = ST_CCONN;
    }

    /**
     * Called when no keep-alive is received
     *
     * @throws COPSPdpException
     */
    protected void processNoKAConnection() throws COPSException {
        if (_process != null) {
            _process.notifyNoKAliveReceived(this);
        }
        _status = ST_NOKA;
    }

}
