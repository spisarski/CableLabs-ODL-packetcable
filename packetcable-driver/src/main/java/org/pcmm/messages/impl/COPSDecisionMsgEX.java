/**
 @header@
 */
package org.pcmm.messages.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * COPS Decision Message
 *
 *
 */

public class COPSDecisionMsgEX extends COPSMsg {

    private final static Logger logger = LoggerFactory.getLogger(COPSDecisionMsgEX.class);

    /* COPSHeader coming from base class */
    private transient COPSHandle _clientHandle;
    private transient COPSError _error;
    private transient COPSIntegrity _integrity;
    private transient COPSContext _decContext;
    private transient COPSClientSI clientSI;

    private final Map<COPSContext, List<COPSDecision>> _decisions;

    /**
     * Default Constructor
     */
    public COPSDecisionMsgEX() {
        _clientHandle = null;
        _error = null;
        _integrity = null;
        _decContext = null;
        clientSI = null;
        _decisions = new ConcurrentHashMap<>();
    }

    /**
     * Constructor with data
     * @param data - the data to parse
     * @throws COPSException
     */
    protected COPSDecisionMsgEX(final byte[] data) throws COPSException {
        this();
        parse(data);
    }

    @Override
    public void checkSanity() throws COPSException {
        if ((_hdr == null) || (_clientHandle == null)
                || ((_error == null) && (_decisions.size() == 0))) {
            throw new COPSException("Bad message format");
        }
    }

    @Override
    protected void parse(final byte[] data) throws COPSException {
        super.parseHeader(data);

        while (_dataStart < _dataLength) {
            byte[] buf = new byte[data.length - _dataStart];
            System.arraycopy(data, _dataStart, buf, 0, data.length - _dataStart);

            final COPSObjHeader objHdr = new COPSObjHeader(buf) {};
            switch (objHdr.getCNum()) {
                case COPSObjHeader.COPS_HANDLE: {
                    _clientHandle = new COPSHandle(buf) {
                    };
                    _dataStart += _clientHandle.getDataLength();
                }
                break;
                case COPSObjHeader.COPS_CONTEXT: {
                    // dec context
                    _decContext = new COPSContext(buf) {
                    };
                    _dataStart += _decContext.getDataLength();
                }
                break;
                case COPSObjHeader.COPS_ERROR: {
                    _error = new COPSError(buf) {
                    };
                    _dataStart += _error.getDataLength();
                }
                break;
                case COPSObjHeader.COPS_DEC: {
                    COPSDecision decs = new COPSDecision(buf) {
                    };
                    _dataStart += decs.getDataLength();
                    addDecision(decs, _decContext);
                }
                break;
                case COPSObjHeader.COPS_MSG_INTEGRITY: {
                    _integrity = new COPSIntegrity(buf);
                    _dataStart += _integrity.getDataLength();
                }
                break;
                case COPSObjHeader.COPS_CSI: {
                    clientSI = new COPSClientSI(buf) {
                    };
                    _dataStart += clientSI.getDataLength();
                }
                break;
                default: {
                    throw new COPSException(
                        "Bad Message format, unknown object type");
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
     * @param hdr a COPSHeader
     * @throws COPSException
     */
    public void add(final COPSHeader hdr) throws COPSException {
        logger.info("Adding COPSHeader");
        if (hdr == null)
            throw new COPSException("Null Header");
        if (hdr.getOpCode() != COPSHeader.COPS_OP_DEC)
            throw new COPSException("Error Header (no COPS_OP_DEC)");
        _hdr = hdr;
        setMsgLength();
    }

    /**
     * Add client handle to the message
     * @param handle a COPSHandle
     * @throws COPSException
     */
    public void add(final COPSHandle handle) throws COPSException {
        logger.info("Adding COPSHandle");
        if (handle == null)
            throw new COPSException("Null Handle");
        _clientHandle = handle;
        setMsgLength();
    }

    /**
     * Add an Error object
     * @param error a COPSError
     * @throws COPSException
     */
    public void add(final COPSError error) throws COPSException {
        logger.info("Adding COPSError");
        if (_decisions.size() != 0)
            throw new COPSException("No null decisions");
        if (_error != null)
            throw new COPSException("No null error");
        // Message integrity object should be the very last one
        // If it is already added
        if (_integrity != null)
            throw new COPSException("No null integrity");
        _error = error;
        setMsgLength();
    }

    /**
     * Add one or more local decision object for a given decision context the
     * context is optional, if null all decision object are tided to message
     * context
     * @param decision a COPSDecision
     * @param context a COPSContext
     * @throws COPSException
     */
    public void addDecision(final COPSDecision decision, final COPSContext context) throws COPSException {
        logger.info("Adding COPSDecision");
        // Either error or decision can be added
        // If error is aleady there assert
        if (_error != null)
            throw new COPSException("No null error");

        if (decision.isLocalDecision())
            throw new COPSException("Is local decision");

        List<COPSDecision> decisionList = _decisions.get(context);
        if (decisionList == null)
            decisionList = new ArrayList<>();

        if (decision.isFlagSet()) {// Commented out as advised by Felix
            // if (v.size() != 0)
            // {
            // Only one set of decision flags is allowed
            // for each context
            // throw new COPSException
            // ("Bad Message format, only one set of decision flags is allowed.");
            // }
        } else {
            if (decisionList.size() == 0) {
                // The flags decision must precede any other
                // decision message, since the decision is not
                // flags throw exception
                throw new COPSException(
                    "Bad Message format, flags decision must precede any other decision object.");
            }
        }
        decisionList.add(decision);
        _decisions.put(context, decisionList);

        setMsgLength();
    }

    /**
     * Add integrity object
     * @param integrity a COPSIntegrity
     * @throws COPSException
     */
    public void add(final COPSIntegrity integrity) throws COPSException {
        logger.info("Adding COPSIntegrity");
        if (integrity == null)
            throw new COPSException("Null Integrity");
        if (!integrity.isMessageIntegrity())
            throw new COPSException("Error Integrity");
        _integrity = integrity;
        setMsgLength();
    }

    /**
     * Add a client specific informations
     * @param clientSI a COPSClientSI
     * @throws COPSException
     */
    public void add(final COPSClientSI clientSI) throws COPSException {
        logger.info("Adding COPSClientSI");
        if (clientSI == null)
            throw new COPSException("Null ClientSI");
        this.clientSI = clientSI;
        setMsgLength();
    }

    @Override
    public void writeData(final Socket id) throws IOException {
        logger.info("Writing data");
        // checkSanity();
        if (_hdr != null)
            _hdr.writeData(id);
        if (_clientHandle != null)
            _clientHandle.writeData(id);
        if (_error != null)
            _error.writeData(id);

        // Display decisions
        // Display any local decisions
        for (final Map.Entry<COPSContext, List<COPSDecision>> entry : _decisions.entrySet()) {
            // TODO - this looks dangerous and could possibly throw an NPE
            entry.getKey().writeData(id);
            for (final COPSDecision decision : entry.getValue()) {
                decision.writeData(id);
            }
        }
        if (clientSI != null)
            clientSI.writeData(id);
        if (_integrity != null)
            _integrity.writeData(id);
    }

    @Override
    public COPSHeader getHeader() {
        return _hdr;
    }

    /**
     * Method getClientHandle
     * @return a COPSHandle
     */
    public COPSHandle getClientHandle() {
        return _clientHandle;
    }

    public COPSClientSI getClientSI() {
        return clientSI;
    }

    /**
     * Method setMsgLength
     * @throws COPSException
     */
    protected void setMsgLength() throws COPSException {
        short len = 0;
        if (_clientHandle != null)
            len += _clientHandle.getDataLength();
        if (_error != null)
            len += _error.getDataLength();

        // Display any local decisions
        for (final Map.Entry<COPSContext, List<COPSDecision>> entry : _decisions.entrySet()) {
            final COPSContext context = entry.getKey();
            len += context.getDataLength();
            for (final COPSDecision decision : entry.getValue()) {
                len += decision.getDataLength();
            }
        }
        if (clientSI != null)
            len += clientSI.getDataLength();
        if (_integrity != null) {
            len += _integrity.getDataLength();
        }

        _hdr.setMsgLength((int) len);
    }

    @Override
    public void dump(final OutputStream os) throws IOException {
        logger.info("Dumping to OutputStream");
        _hdr.dump(os);

        if (_clientHandle != null)
            _clientHandle.dump(os);
        if (_error != null)
            _error.dump(os);

        // Display any local decisions
        for (final Map.Entry<COPSContext, List<COPSDecision>> entry : _decisions.entrySet()) {
            final COPSContext context = entry.getKey();
            context.dump(os);
            for (final COPSDecision decision : entry.getValue()) {
                decision.dump(os);
            }
        }
        if (clientSI != null)
            clientSI.dump(os);
        if (_integrity != null) {
            _integrity.dump(os);
        }
    }
}
