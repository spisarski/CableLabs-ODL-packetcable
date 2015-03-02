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
 *     COPS Client Accept Message
 *
 * @version COPSClientAcceptMsg.java, v 1.00 2003
 *
 */
public class COPSClientAcceptMsg extends COPSMsg {

    private final static Logger logger = LoggerFactory.getLogger(COPSClientAcceptMsg.class);

    /* COPSHeader coming from base class */
    private transient COPSKATimer _kaTimer;
    private transient COPSAcctTimer _acctTimer;
    private transient COPSIntegrity _integrity;

    /**
     * Default constructor
     */
    public COPSClientAcceptMsg() {
        _kaTimer = null;
        _acctTimer = null;
        _integrity = null;
        logger.info("New COPS client accept message");
    }

    /**
     * Constructor with some data
     * @param data - the data to parse
     * @throws COPSException
     */
    protected COPSClientAcceptMsg(final byte[] data) throws COPSException {
        this();
        parse(data);
    }

    @Override
    public void checkSanity() throws COPSException {
        logger.info("Checking sanity");
        if ((_hdr == null) || (_kaTimer == null))
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
        if (hdr.getOpCode() != COPSHeader.COPS_OP_CAT)
            throw new COPSException ("Error Header (no COPS_OP_CAT)");
        _hdr = hdr;
        setMsgLength();
    }

    /**
     * Add Timer object to the message
     * @param    timer               a  COPSTimer
     * @throws   COPSException
     */
    public void add(final COPSTimer timer) throws COPSException {
        logger.info("Adding COPSTimer");
        if (timer.isKATimer()) {
            _kaTimer = (COPSKATimer) timer;
        } else {
            _acctTimer = (COPSAcctTimer) timer;
        }
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
     * Method getKATimer
     * @return   a COPSKATimer
     */
    public COPSKATimer getKATimer() {
        return _kaTimer;
    }

    /**
     * Should check hasAcctTimer() before calling
     * @return   a COPSAcctTimer
     */
    public COPSAcctTimer getAcctTimer() {
        return (_acctTimer);
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
        if (_kaTimer != null) _kaTimer.writeData(id);
        if (_acctTimer != null) _acctTimer.writeData(id);
        if (_integrity != null) _integrity.writeData(id);
    }

    @Override
    protected void parse(final byte[] data) throws COPSException {
        parseHeader(data);

        while (_dataStart < _dataLength) {
            byte[] buf = new byte[data.length - _dataStart];
            System.arraycopy(data,_dataStart,buf,0,data.length - _dataStart);

            COPSObjHeader objHdr = new COPSObjHeader (buf);
            switch (objHdr.getCNum()) {
            case COPSObjHeader.COPS_KA: {
                _kaTimer = new COPSKATimer(buf);
                _dataStart += _kaTimer.getDataLength();
            }
            break;
            case COPSObjHeader.COPS_ACCT_TIMER: {
                _acctTimer = new COPSAcctTimer(buf);
                _dataStart += _acctTimer.getDataLength();
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
        if (hdr.getOpCode() != COPSHeader.COPS_OP_CAT)
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
        short len = 0;
        if (_kaTimer != null) len += _kaTimer.getDataLength();
        if (_acctTimer != null) len += _acctTimer.getDataLength();
        if (_integrity != null) len += _integrity.getDataLength();
        _hdr.setMsgLength(len);
    }

    @Override
    public void dump(final OutputStream os) throws IOException {
        logger.info("Dump");
        _hdr.dump(os);

        if (_kaTimer != null)
            _kaTimer.dump(os);

        if (_acctTimer != null)
            _acctTimer.dump(os);

        if (_integrity != null) {
            _integrity.dump(os);
        }
    }
}

