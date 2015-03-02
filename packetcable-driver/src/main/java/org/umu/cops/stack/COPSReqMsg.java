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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * COPS Request Message (RFC 2748 pag. 22)
 *
 *   The PEP establishes a request state client handle for which the
 *   remote PDP may maintain state. The remote PDP then uses this handle
 *   to refer to the exchanged information and decisions communicated over
 *   the TCP connection to a particular PEP for a given client-type.
 *
 *   Once a stateful handle is established for a new request, any
 *   subsequent modifications of the request can be made using the REQ
 *   message specifying the previously installed handle. The PEP is
 *   responsible for notifying the PDP whenever its local state changes so
 *   the PDP's state will be able to accurately mirror the PEP's state.
 *
 *   The format of the Request message is as follows:
 *
 *               <Request Message> ::=  <Common Header>
 *                                      <Client Handle>
 *                                      <Context>
 *                                      [<IN-Int>]
 *                                      [<OUT-Int>]
 *                                      [<ClientSI(s)>]
 *                                      [<LPDPDecision(s)>]
 *                                      [<Integrity>]
 *
 *               <ClientSI(s)> ::= <ClientSI> | <ClientSI(s)> <ClientSI>
 *
 *               <LPDPDecision(s)> ::= <LPDPDecision> |
 *                                     <LPDPDecision(s)> <LPDPDecision>
 *
 *               <LPDPDecision> ::= [<Context>]
 *                                  <LPDPDecision: Flags>
 *                                  [<LPDPDecision: Stateless Data>]
 *                                  [<LPDPDecision: Replacement Data>]
 *                                  [<LPDPDecision: ClientSI Data>]
 *                                  [<LPDPDecision: Named Data>]
 *
 *   The context object is used to determine the context within which all
 *   the other objects are to be interpreted. It also is used to determine
 *   the kind of decision to be returned from the policy server. This
 *   decision might be related to admission control, resource allocation,
 *   object forwarding and substitution, or configuration.
 *
 *   The interface objects are used to determine the corresponding
 *   interface on which a signaling protocol message was received or is
 *   about to be sent. They are typically used if the client is
 *   participating along the path of a signaling protocol or if the client
 *   is requesting configuration data for a particular interface.
 *
 *   ClientSI, the client specific information object, holds the client-
 *   type specific data for which a policy decision needs to be made. In
 *   the case of configuration, the Named ClientSI may include named
 *   information about the module, interface, or functionality to be
 *   configured. The ordering of multiple ClientSIs is not important.
 *
 *   Finally, LPDPDecision object holds information regarding the local
 *   decision made by the LPDP.
 *
 *   Malformed Request messages MUST result in the PDP specifying a
 *   Decision message with the appropriate error code.
 *
 * @version COPSReqMsg.java, v 1.00 2003
 *
 */
public class COPSReqMsg extends COPSMsg {

    /* COPSHeader coming from base class */
    private transient COPSHandle _clientHandle;
    private transient COPSContext _context;
    private transient COPSInterface _inInterface;
    private transient COPSInterface _outInterface;
    private transient List<COPSClientSI> _clientSIs;
    private final Map<COPSContext, List<COPSLPDPDecision>> _decisions;
    private transient COPSIntegrity _integrity;
    private transient COPSContext _lpdpContext;

    /**
     * Default Constructor
     */
    public COPSReqMsg() {
        _clientHandle = null;
        _context = null;
        _inInterface = null;
        _outInterface = null;
        _clientSIs = new ArrayList<>();
        _decisions = new ConcurrentHashMap<>();
        _integrity = null;
        _lpdpContext = null;
    }

    /**
     * Constructor with data
     * @param data - the data to parse
     * @throws COPSException
     */
    protected COPSReqMsg(final byte[] data) throws COPSException {
        this();
        parse(data);
    }

    @Override
    public void checkSanity() throws COPSException {
        if ((_hdr == null) || (_clientHandle == null) || (_context == null)) {
            throw new COPSException("Bad message format");
        }
    }

    /**
     * Add an IN or OUT interface object
     * @param    inter               a  COPSInterface
     * @throws   COPSException
     */
    public void add(final COPSInterface inter) throws COPSException {
        if (!(inter.isInInterface() || inter.isOutInterface()))
            throw new COPSException ("No Interface");

        //Message integrity object should be the very last one
        //If it is already added
        if (_integrity != null)
            throw new COPSException ("Integrity should be the last one");

        if (inter.isInInterface()) {
            if (_inInterface != null) {
                throw new COPSException ("Object inInterface exits");
            } else {
                _inInterface = inter;
            }
        } else {
            if (_outInterface != null) {
                throw new COPSException ("Object outInterface exits");
            } else {
                _outInterface = inter;
            }
        }
        setMsgLength();
    }

    /**
     * Add header to the message
     * @param    hdr                 a  COPSHeader
     * @throws   COPSException
     */
    public void add(final COPSHeader hdr) throws COPSException {
        if (hdr == null)
            throw new COPSException ("Null Header");
        if (hdr.getOpCode() != COPSHeader.COPS_OP_REQ)
            throw new COPSException ("Error Header (no COPS_OP_REQ)");
        _hdr = hdr;
        setMsgLength();
    }

    /**
     * Add Context object to the message
     * @param    context             a  COPSContext
     * @throws   COPSException
     */
    public void add(final COPSContext context) throws COPSException {
        if (context == null)
            throw new COPSException ("Null Context");
        _context = context;
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
     * Add one or more clientSI objects
     * @param    clientSI            a  COPSClientSI
     * @throws   COPSException
     */
    public void add(final COPSClientSI clientSI) throws COPSException {
        if (clientSI == null)
            throw new COPSException ("Null ClientSI");
        _clientSIs.add(clientSI);
        setMsgLength();
    }

    /**
     * Add one or more local decision object for a given decision context
     * the context is optional, if null all decision object are tided to
     * message context
     * @param    decision            a  COPSLPDPDecision
     * @param    context             a  COPSContext
     * @throws   COPSException
     */
    public void addLocalDecision(final COPSLPDPDecision decision, final COPSContext context) throws COPSException {
        if (!decision.isLocalDecision())
            throw new COPSException ("Local Decision");

        final List<COPSLPDPDecision> decisions = _decisions.get(context);
        if (decision.isFlagSet()) {
            if (decisions.size() != 0) {
                //Only one set of decision flags is allowed
                //for each context
                throw new COPSException ("Bad Message format, only one set of decision flags is allowed.");
            }
        } else {
            if (decisions.size() == 0) {
                //The flags decision must precede any other
                //decision message, since the decision is not
                //flags throw exception
                throw new COPSException ("Bad Message format, flags decision must precede any other decision object.");
            }
        }
        decisions.add(decision);
        _decisions.put(context,decisions);
        setMsgLength();
    }

    /**
     * Add integrity object
     * @param    integrity           a  COPSIntegrity
     * @throws   COPSException
     */
    public void add(final COPSIntegrity integrity) throws COPSException {
        if (integrity == null)
            throw new COPSException ("Null Integrity");
        if (!integrity.isMessageIntegrity())
            throw new COPSException ("Error Integrity");
        _integrity = integrity;
        setMsgLength();
    }

    @Override
    public void writeData(final Socket id) throws IOException {
        // checkSanity();
        if (_hdr != null) _hdr.writeData(id);
        if (_clientHandle != null) _clientHandle.writeData(id);
        if (_context != null) _context.writeData(id);

        for (final COPSClientSI clientSI : _clientSIs) {
            clientSI.writeData(id);
        }

        //Display any local decisions
        for (final Map.Entry<COPSContext, List<COPSLPDPDecision>> entry : _decisions.entrySet()) {
            entry.getKey().writeData(id);
            for (final COPSLPDPDecision decision : entry.getValue()) {
                decision.writeData(id);
            }
        }
        if (_integrity != null) _integrity.writeData(id);
    }

    @Override
    public COPSHeader getHeader() {
        return _hdr;
    }

    /**
     * Return client Handle
     * @return   a COPSHandle
     */
    public COPSHandle getClientHandle() {
        return _clientHandle;
    }

    /**
     * Return Context
     * @return   a COPSContext
     */
    public COPSContext getContext() {
        return _context;
    }

    /**
     * Returns a vector if ClientSI objects
     * @return   a Vector
     */
    public List<COPSClientSI> getClientSI() {
        // Defensive copy
        return new ArrayList<>(_clientSIs);
    }

    /**
     * Get Integrity. Should check hasIntegrity() becfore calling
     * @return   a COPSIntegrity
     */
    public COPSIntegrity getIntegrity() {
        return _integrity;
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
                    if (_context == null) {
                        //Message context
                        _context = new COPSContext(buf);
                        _dataStart += _context.getDataLength();
                    } else {
                        //lpdp context
                        _lpdpContext = new COPSContext(buf);
                        _dataStart += _lpdpContext.getDataLength();
                    }
                }
                break;
                case COPSObjHeader.COPS_ININTF: {
                    if (objHdr.getCType() == 1) {
                        _inInterface = new COPSIpv4InInterface(buf);
                    } else {
                        _inInterface = new COPSIpv6InInterface(buf);
                    }
                    _dataStart += _inInterface.getDataLength();
                }
                break;
                case COPSObjHeader.COPS_OUTINTF: {
                    if (objHdr.getCType() == 1) {
                        _outInterface = new COPSIpv4OutInterface(buf);
                    } else {
                        _outInterface = new COPSIpv6OutInterface(buf);
                    }
                    _dataStart += _outInterface.getDataLength();
                }
                break;
                case COPSObjHeader.COPS_LPDP_DEC: {
                    final COPSLPDPDecision lpdp = new COPSLPDPDecision(buf);
                    _dataStart += lpdp.getDataLength();
                    addLocalDecision(lpdp, _lpdpContext);
                }
                break;
                case COPSObjHeader.COPS_CSI: {
                    final COPSClientSI csi = new COPSClientSI(buf);
                    _dataStart += csi.getDataLength();
                    _clientSIs.add(csi);
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
     * Set the message length, base on the set of objects it contains
     * @throws   COPSException
     */
    protected void setMsgLength() throws COPSException {
        short len = 0;

        if (_clientHandle != null)
            len += _clientHandle.getDataLength();

        if (_context != null)
            len += _context.getDataLength();

        for (final COPSClientSI clientSI : _clientSIs) {
            len += clientSI.getDataLength();
        }

        //Display any local decisions
        for (final Map.Entry<COPSContext, List<COPSLPDPDecision>> entry : _decisions.entrySet()) {
            len += entry.getKey().getDataLength();
            for (final COPSLPDPDecision decision : entry.getValue()) {
                len += decision.getDataLength();
            }
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

        if (_context != null)
            _context.dump(os);

        for (final COPSClientSI clientSI : _clientSIs) {
            clientSI.dump(os);
        }

        //Display any local decisions
        for (final Map.Entry<COPSContext, List<COPSLPDPDecision>> entry : _decisions.entrySet()) {
            entry.getKey().dump(os);
            for (final COPSLPDPDecision decision : entry.getValue()) {
                decision.dump(os);
            }
        }

        if (_integrity != null) {
            _integrity.dump(os);
        }
    }
}

