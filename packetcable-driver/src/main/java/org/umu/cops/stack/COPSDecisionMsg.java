/*
 * Copyright (c) 2003 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.stack;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * COPS Decision Message
 *
 * @version COPSDecisionMsg.java, v 1.00 2003
 *
 */
public class COPSDecisionMsg extends COPSMsg {

    /* COPSHeader coming from base class */
    private final Map<COPSContext, List<COPSDecision>> _decisions;
    private transient COPSHandle _clientHandle;
    private transient COPSError _error;
    private transient COPSIntegrity _integrity;
    private transient COPSContext _decContext;
    private transient COPSClientSI _decSI;

    /**
     * Default constructor
     */
    public COPSDecisionMsg() {
        _decisions = new ConcurrentHashMap<>();
    }

    /**
     * Constructor with data
     * @param data - the data to parse
     * @throws COPSException
     */
    protected COPSDecisionMsg(final byte[] data) throws COPSException  {
        this();
        parse(data);
    }

    @Override
    public void checkSanity() throws COPSException {
        if ((_hdr == null) || (_clientHandle == null) || ( (_error == null) && (_decisions.size() == 0))) {
            throw new COPSException("Bad message format");
        }
    }

    @Override
    protected void parse(final byte[] data) throws COPSException {
        super.parseHeader(data);

        while (_dataStart < _dataLength) {
            final byte[] buf = new byte[data.length - _dataStart];
            System.arraycopy(data,_dataStart,buf,0,data.length - _dataStart);

            final COPSObjHeader objHdr = new COPSObjHeader (buf);
            switch (objHdr.getCNum()) {
                case COPSObjHeader.COPS_HANDLE: {
                    _clientHandle = new COPSHandle(buf);
                    _dataStart += _clientHandle.getDataLength();
                }
                break;
                case COPSObjHeader.COPS_CONTEXT: {
                    //dec context
                    _decContext = new COPSContext(buf);
                    _dataStart += _decContext.getDataLength();
                }
                break;
                case COPSObjHeader.COPS_ERROR: {
                    _error = new COPSError(buf);
                    _dataStart += _error.getDataLength();
                }
                break;
                case COPSObjHeader.COPS_DEC: {
                    COPSDecision decs = new COPSDecision(buf);
                    _dataStart += decs.getDataLength();
                    addDecision(decs, _decContext);
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
        _hdr = hdr;
        parse(data);
        setMsgLength();
    }

    /**
     * Add message header
     * @param    hdr                 a  COPSHeader
     * @throws   COPSException
     */
    public void add(final COPSHeader hdr) throws COPSException {
        if (hdr == null)
            throw new COPSException ("Null Header");
        if (hdr.getOpCode() != COPSHeader.COPS_OP_DEC)
            throw new COPSException ("Error Header (no COPS_OP_DEC)");
        _hdr = hdr;
        setMsgLength();
    }

    /**
     * Add client handle to the message
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
     * Add an Error object
     * @param    error               a  COPSError
     * @throws   COPSException
     */
    public void add(final COPSError error) throws COPSException {
        if (_decisions.size() != 0)
            throw new COPSException ("No null decisions");
        if (_error != null)
            throw new COPSException ("No null error");
        //Message integrity object should be the very last one
        //If it is already added
        if (_integrity != null)
            throw new COPSException ("No null integrity");
        _error = error;
        setMsgLength();
    }

    /**
     * Add one or more local decision object for a given decision context
     * the context is optional, if null all decision object are tided to
     * message context
     * @param    decision            a  COPSDecision
     * @param    context             a  COPSContext
     * @throws   COPSException
     */
    public void addDecision(final COPSDecision decision, final COPSContext context)  throws COPSException {
        //Either error or decision can be added
        //If error is aleady there assert
        if (_error != null)
            throw new COPSException ("No null error");

        if (decision.isLocalDecision())
            throw new COPSException ("Is local decision");

        final List<COPSDecision> decisions;
        if (_decisions.get(context) != null) {
            decisions = _decisions.get(context);
        } else {
            decisions = new ArrayList<>();
        }

        // TODO - determine what needs to be done here
        if (decision.isFlagSet()) {//Commented out as advised by Felix
            //if (v.size() != 0)
            //{
            //Only one set of decision flags is allowed
            //for each context
            //     throw new COPSException ("Bad Message format, only one set of decision flags is allowed.");
            //}
        } else {
            if (decisions == null || decisions.size() == 0) {
                //The flags decision must precede any other
                //decision message, since the decision is not
                //flags throw exception
                throw new COPSException ("Bad Message format, flags decision must precede any other decision object.");
            }
        }
        decisions.add(decision);
        _decisions.put(context, decisions);

        setMsgLength();
    }

    /**
     * Add integrity object
     * @param    integrity           a  COPSIntegrity
     * @throws   COPSException
     */
    public void add(final COPSIntegrity integrity)  throws COPSException {
        if (integrity == null)
            throw new COPSException ("Null Integrity");
        if (!integrity.isMessageIntegrity())
            throw new COPSException ("Error Integrity");
        _integrity = integrity;
        setMsgLength();
    }
    /**
     * Add clientSI object
     * @param    clientSI           a  COPSIntegrity
     * @throws   COPSException
     */
    public void add(final COPSClientSI clientSI)  throws COPSException {
        if (clientSI == null)
            throw new COPSException ("Null clientSI");
        /*
                  if (!integrity.isMessageIntegrity())
                       throw new COPSException ("Error Integrity");
        */
        _decSI = clientSI;
        setMsgLength();
    }

    @Override
    public void writeData(final Socket id) throws IOException {
        // checkSanity();
        if (_hdr != null) _hdr.writeData(id);
        if (_clientHandle != null) _clientHandle.writeData(id);
        if (_error != null) _error.writeData(id);

        //Display decisions
        //Display any local decisions
        for (final Map.Entry<COPSContext, List<COPSDecision>> entry : _decisions.entrySet()) {
            entry.getKey().writeData(id);
            for (final COPSDecision decision : entry.getValue()) {
                decision.writeData(id);
            }
        }

        if (_decSI != null) _decSI.writeData(id);
        if (_integrity != null) _integrity.writeData(id);
    }

    @Override
    public COPSHeader getHeader() {
        return _hdr;
    }

    /**
     * Method getClientHandle
     * @return   a COPSHandle
     */
    public COPSHandle getClientHandle() {
        return _clientHandle;
    }

    /**
     * Should check hasError() before calling
     * @return   a COPSError
     */
    public COPSError getError() {
        return _error;
    }

    /**
     * Returns a map of decision for which is an arry of context and vector
     * of associated decision object.
     * @return   a Map
     */
    public Map<COPSContext, List<COPSDecision>> getDecisions() {
        // Defensive copy
        return new HashMap<>(_decisions);
    }

    /**
     * Method setMsgLength
     * @throws   COPSException
     */
    protected void setMsgLength()  throws COPSException {
        short len = 0;
        if (_clientHandle != null)
            len += _clientHandle.getDataLength();
        if (_error != null)
            len += _error.getDataLength();

        //Display any local decisions
        for (final Map.Entry<COPSContext, List<COPSDecision>> entry : _decisions.entrySet()) {
            len += entry.getKey().getDataLength();
            for (final COPSDecision decision : entry.getValue()) {
                len += decision.getDataLength();
            }
        }
        if (_decSI != null) {
            len += _decSI.getDataLength();
        }
        if (_integrity != null) {
            len += _integrity.getDataLength();
        }
        _hdr.setMsgLength((int) len);
    }

    @Override
    public void dump(final OutputStream os) throws IOException {
        _hdr.dump(os);
        if (_clientHandle != null)
            _clientHandle.dump(os);
        if (_error != null)
            _error.dump(os);

        //Display any local decisions
        for (final Map.Entry<COPSContext, List<COPSDecision>> entry : _decisions.entrySet()) {
            entry.getKey().dump(os);
            for (final COPSDecision decision : entry.getValue()) {
                decision.dump(os);
            }
        }
        if (_decSI != null) {
            _decSI.dump(os);
        }
        if (_integrity != null) {
            _integrity.dump(os);
        }
    }
}



