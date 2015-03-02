package org.umu.cops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.common.COPSDebug;
import org.umu.cops.stack.*;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract class for all COPS connections
 */
public abstract class COPSConnection {

    private final static Logger logger = LoggerFactory.getLogger(COPSConnection.class);

    /**
     PEP identifier
     * TODO - not being used now but other clients are injecting this in and it seems like it should be used at some point
     */
    protected final COPSPepId _pepId;

    /** Socket connected to PDP */
    protected final Socket _sock;

    /**
     * Keep-alive timer value (secs)
     */
    protected final short _kaTimer;

    /**
     * Maps a Client Handle to a Handler
     */
    protected final Map<COPSHandle, COPSReqStateMan> _managerMap;

    /**
     COPS error returned by PEP
     */
    protected COPSError _error;

    public COPSConnection(final COPSPepId pepId, final Socket sock, final short kaTimer) {
        this._pepId = pepId;
        this._sock = sock;
        this._kaTimer = kaTimer;
        _managerMap = new ConcurrentHashMap<>();
    }

    /**
     * Gets all request state managers
     * @return  A <tt>Hashatable</tt> holding all request state managers
     */
    public Map<COPSHandle, COPSReqStateMan> getReqStateMans() {
        // Defensive copy
        return new HashMap<>(_managerMap);
    }

    protected void notifyNoKAAllReqStateMan() throws COPSException {
        for (final COPSReqStateMan man : _managerMap.values()) {
            man.processNoKAConnection();
        }
    }

    /**
     * Checks whether the _socket to the PDP is closed or not
     * @return  <tt>true</tt> if the _socket is closed, <tt>false</tt> otherwise
     */
    public final boolean isClosed() {
        return _sock.isClosed();
    }

    /**
     * Returns the COPS Connection socket
     * @return - the socket connection
     */
    public final Socket getSocket() { return _sock;}

    /**
     * Handle Client Close Message, close the passed connection
     * @param    conn                a  Socket
     * @param    cMsg                 a  COPSMsg
     * <Client-Close> ::= <Common Header>
     *                      <Error>
     *                      [<Integrity>]
     * Not support [<Integrity>]
     */
    protected void handleClientCloseMsg(Socket conn, final COPSClientCloseMsg cMsg) {
        _error = cMsg.getError();

        logger.info("Got close request, closing connection " +
                conn.getInetAddress() + ":" + conn.getPort() + ":[Error " + _error.getDescription() + "]");
        try {
            // Support
            if (cMsg.getIntegrity() != null) {
                logger.error(COPSDebug.ERROR_NOSUPPORTED
                        + " - Unsupported objects (Integrity) to connection " + conn.getInetAddress());
            }
            conn.close();
        } catch (Exception unae) {
            logger.error("Unexpected error clossing the connection", unae);
        }
    }

    /**
     * Closes all state managers
     * @throws COPSException
     */
    protected void notifyCloseAllReqStateMan() throws COPSException {
        for (final COPSReqStateMan man : _managerMap.values()) {
            man.processClosedConnection(_error);
        }
    }

}
