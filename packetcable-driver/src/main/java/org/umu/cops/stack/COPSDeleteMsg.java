/*
 * Copyright (c) 2003 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.stack;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * COPS Delete Message (RFC 2748 pag. 24)
 *
 *    When sent from the PEP this message indicates to the remote PDP that
 *    the state identified by the client handle is no longer
 *    available/relevant. This information will then be used by the remote
 *    PDP to initiate the appropriate housekeeping actions. The reason code
 *    object is interpreted with respect to the client-type and signifies
 *    the reason for the removal.
 *
 *    The format of the Delete Request State message is as follows:
 *
 *               <Delete Request>  ::= <Common Header>
 *                                     <Client Handle>
 *                                     <Reason>
 *                                     [<Integrity>]
 *
 *
 * @version COPSDeleteMsg.java, v 1.00 2003
 *
 */
public class COPSDeleteMsg extends COPSMsg {
    /* COPSHeader coming from base class */
    private COPSHandle  _clientHandle;
    private COPSReason _reason;
    private COPSIntegrity _integrity;

    /**
     * Default Constructor
     */
    public COPSDeleteMsg() {
        _clientHandle = null;
        _reason = null;
        _integrity = null;
    }

    /**
     * Constructor with data
     * @param data - the data to parse
     * @throws COPSException
     */
    protected COPSDeleteMsg(final byte[] data) throws COPSException {
        this();
        parse(data);
    }

    @Override
    public void checkSanity() throws COPSException {
        if ((_hdr == null) || (_clientHandle == null) || (_reason == null))
            throw new COPSException("Bad message format");
    }

    /**
     * Add message header
     * @param    hdr                 a  COPSHeader
     * @throws   COPSException
     */
    public void add(final COPSHeader hdr) throws COPSException {
        if (hdr == null)
            throw new COPSException ("Null Header");
        if (hdr.getOpCode() != COPSHeader.COPS_OP_DRQ)
            throw new COPSException ("Error Header (no COPS_OP_DRQ)");
        _hdr = hdr;
        setMsgLength();
    }

    /**
     * Add Reason object to the message
     * @param    reason              a  COPSReason
     * @throws   COPSException
     */
    public void add(final COPSReason reason) throws COPSException {
        if (_reason != null)
            throw new COPSException ("No null Reason");

        //Message integrity object should be the very last one
        //If it is already added
        if (_integrity != null)
            throw new COPSException ("No null Integrity");
        _reason = reason;
        setMsgLength();
    }

    /**
     * Add Handle object
     * @param    handle              a  COPSHandle
     * @throws   COPSException
     */
    public void add(final COPSHandle handle) throws COPSException {
        if (handle == null)
            throw new COPSException ("Null Handle");
        _clientHandle = handle;
        setMsgLength();
    }

    /**
     * Add Integrity object
     * @param    integrity           a  COPSIntegrity
     * @throws   COPSException
     */
    public void add (final COPSIntegrity integrity) throws COPSException {
        if (integrity == null)
            throw new COPSException ("Null Integrity");
        if (!integrity.isMessageIntegrity())
            throw new COPSException ("Error Integrity");
        _integrity = integrity;
        setMsgLength();
    }

    /**
     * Get Client Handle
     * @return   a COPSHandle
     */
    public COPSHandle getClientHandle() {
        return _clientHandle;
    };

    /**
     * Get Reason
     * @return   a COPSReason
     */
    public COPSReason getReason() {
        return _reason;
    };

    /**
     * Get Integrity. Should check hasIntegrity() before calling
     * @return   a COPSIntegrity
     */
    public COPSIntegrity getIntegrity() {
        return (_integrity);
    }

    @Override
    public void writeData(final Socket id) throws IOException {
        if (_hdr != null) _hdr.writeData(id);
        if (_clientHandle != null) _clientHandle.writeData(id);
        if (_reason != null) _reason.writeData(id);
        if (_integrity != null) _integrity.writeData(id);
    }

    @Override
    protected void parse(final byte[] data) throws COPSException {
        super.parseHeader(data);

        while (_dataStart < _dataLength) {
            byte[] buf = new byte[data.length - _dataStart];
            System.arraycopy(data,_dataStart,buf,0,data.length - _dataStart);

            final COPSObjHeader objHdr = new COPSObjHeader (buf);
            switch (objHdr.getCNum()) {
                case COPSObjHeader.COPS_HANDLE: {
                    _clientHandle = new COPSHandle(buf);
                    _dataStart += _clientHandle.getDataLength();
                }
                break;
                case COPSObjHeader.COPS_REASON_CODE: {
                    _reason = new COPSReason(buf);
                    _dataStart += _reason.getDataLength();
                }
                break;
                case COPSObjHeader.COPS_MSG_INTEGRITY: {
                    _integrity = new COPSIntegrity(buf);
                    _dataStart += _integrity.getDataLength();
                }
                break;
                default: {
                    throw new COPSException("Bad Message format, unknown object type");
                }
            }
        }
        checkSanity();
    }

    @Override
    protected void parse(final COPSHeader hdr, final byte[] data) throws COPSException {
        if (hdr.getOpCode() != COPSHeader.COPS_OP_DRQ)
            throw new COPSException("Error Header");
        _hdr = hdr;
        parse(data);
        setMsgLength();
    }

    /**
     * Set the message length, base on the set of objects it contains
     * @throws   COPSException
     */
    protected void setMsgLength()  throws COPSException  {
        short len = 0;
        if (_clientHandle != null) len += _clientHandle.getDataLength();
        if (_reason != null) len += _reason.getDataLength();
        if (_integrity != null) len += _integrity.getDataLength();
        _hdr.setMsgLength(len);
    }

    @Override
    public void dump(final OutputStream os) throws IOException {
        _hdr.dump(os);

        if (_clientHandle != null)
            _clientHandle.dump(os);

        if (_reason != null)
            _reason.dump(os);

        if (_integrity != null) {
            _integrity.dump(os);
        }
    }

}




