/*
 * Copyright (c) 2003 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * COPS Client Close Message
 * @version COPSClientCloseMsg.java, v 1.00 2003
 */
public class COPSClientCloseMsg extends COPSMsg {

    private final static Logger logger = LoggerFactory.getLogger(COPSClientCloseMsg.class);

    /* COPSHeader coming from base class */
    private transient COPSError _error;
    private transient COPSIntegrity _integrity;

    /**
     * Default constructor
     */
    public COPSClientCloseMsg() {
        _error = null;
        _integrity = null;
    }

    /**
     * Constructor with data
     * @param data - the data to parse
     * @throws COPSException
     */
    protected COPSClientCloseMsg(final byte[] data) throws COPSException {
        this();
        parse(data);
    }

    public void checkSanity() throws COPSException {
        logger.info("Check sanity");
        if ((_hdr == null) || (_error == null))
            throw new COPSException("Bad message format");
    }

    /**
     * Add message header
     * @param    hdr                 a  COPSHeader
     * @throws   COPSException
     */
    public void add(final COPSHeader hdr) throws COPSException {
        logger.info("Adding COPSHeader");
        if (hdr == null)
            throw new COPSException ("Null Header");
        if (hdr.getOpCode() != COPSHeader.COPS_OP_CC)
            throw new COPSException ("Error Header (no COPS_OP_CC)");
        _hdr = hdr;
        setMsgLength();
    }

    /**
     * Add Error object
     * @param    error               a  COPSError
     * @throws   COPSException
     */
    public void add(final COPSError error) throws COPSException {
        logger.info("Adding COPSError");
        //Message integrity object should be the very last one
        //If it is already added
        if (_error != null)
            throw new COPSException ("No null Error");
        _error = error;
        setMsgLength();
    }

    /**
     * Add Integrity objects
     * @param    integrity           a  COPSIntegrity
     * @throws   COPSException
     */
    public void add(final COPSIntegrity integrity) throws COPSException {
        logger.info("Adding COPSIntegrity");
        if (integrity == null)
            throw new COPSException ("Null Integrity");
        if (!integrity.isMessageIntegrity())
            throw new COPSException ("Error Integrity");
        _integrity = integrity;
        setMsgLength();
    }

    /**
     * Method getError
     * @return   a COPSError
     */
    public COPSError getError() {
        return _error;
    }

    /**
     * Should check hasIntegrity() before calling
     * @return   a COPSIntegrity
     */
    public COPSIntegrity getIntegrity() {
        return (_integrity);
    }

    @Override
    public void writeData(final Socket id) throws IOException {
        logger.info("Writing data");
        // checkSanity();
        if (_hdr != null) _hdr.writeData(id);
        if (_error != null) _error.writeData(id);
        if (_integrity != null) _integrity.writeData(id);
    }

    @Override
    protected void parse(final byte[] data) throws COPSException {
        parseHeader(data);

        while (_dataStart < _dataLength) {
            byte[] buf = new byte[data.length - _dataStart];
            System.arraycopy(data,_dataStart,buf,0,data.length - _dataStart);

            final COPSObjHeader objHdr = new COPSObjHeader (buf);
            switch (objHdr.getCNum()) {
            case COPSObjHeader.COPS_ERROR: {
                _error = new COPSError(buf);
                _dataStart += _error.getDataLength();
            }
            break;
            case COPSObjHeader.COPS_MSG_INTEGRITY: {
                _integrity = new COPSIntegrity(buf);
                _dataStart += _integrity.getDataLength();
            }
            break;
            default: {
                throw new COPSException("Bad Message format");
            }
            }
        }
        checkSanity();
    }

    @Override
    protected void parse(final COPSHeader hdr, final byte[] data) throws COPSException {
        if (hdr.getOpCode() != COPSHeader.COPS_OP_CC)
            throw new COPSException("Error Header");
        _hdr = hdr;
        parse(data);
        setMsgLength();
    }

    /**
     * Set the message length, base on the set of objects it contains
     * @throws   COPSException
     */
    protected void setMsgLength() throws COPSException {
        int len = 0;
        if (_error != null) len += _error.getDataLength();
        if (_integrity != null) len += _integrity.getDataLength();
        _hdr.setMsgLength(len);
    }

    @Override
    public void dump(final OutputStream os) throws IOException {
        logger.info("Dump");
        _hdr.dump(os);

        if (_error != null)
            _error.dump(os);

        if (_integrity != null) {
            _integrity.dump(os);
        }
    }

}

