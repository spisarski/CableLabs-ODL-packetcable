package org.umu.cops.prpep;

import org.umu.cops.stack.COPSHandle;

import java.net.Socket;

/**
 * Abstract COPS message transceiver class for provisioning connections at the PEP/PDP side.
 */
public abstract class COPSMsgSender {

    /**
     * Socket connection to PDP
     */
    protected final Socket _sock;

    /**
     * The client-type identifies the policy client
     */
    protected final short _clientType;

    /**
     * The client handle is used to uniquely identify a particular
     * PEP's request for a client-type
     */
    protected final COPSHandle _handle;

    protected COPSMsgSender(final short clientType, final COPSHandle handle, final Socket sock) {
        this._clientType = clientType;
        this._handle = handle;
        this._sock = sock;
    }

    /**
     * Gets the client handle
     * @return   Client's <tt>COPSHandle</tt>
     */
    public COPSHandle getClientHandle() {
        return _handle;
    }

    /**
     * Gets the client-type
     * @return   Client-type value
     */
    public short getClientType() {
        return _clientType;
    }

}
