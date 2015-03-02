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

/**
 * COPS Report Message (RFC 2748 pag. 25)
 *
 *    The RPT message is used by the PEP to communicate to the PDP its
 *   success or failure in carrying out the PDP's decision, or to report
 *   an accounting related change in state. The Report-Type specifies the
 *   kind of report and the optional ClientSI can carry additional
 *   information per Client-Type.
 *
 *   For every DEC message containing a configuration context that is
 *   received by a PEP, the PEP MUST generate a corresponding Report State
 *   message with the Solicited Message flag set describing its success or
 *   failure in applying the configuration decision. In addition,
 *   outsourcing decisions from the PDP MAY result in a corresponding
 *   solicited Report State from the PEP depending on the context and the
 *   type of client. RPT messages solicited by decisions for a given
 *   Client Handle MUST set the Solicited Message flag and MUST be sent in
 *   the same order as their corresponding Decision messages were
 *   received. There MUST never be more than one Report State message
 *   generated with the Solicited Message flag set per Decision.
 *
 *   The Report State may also be used to provide periodic updates of
 *   client specific information for accounting and state monitoring
 *   purposes depending on the type of the client. In such cases the
 *   accounting report type should be specified utilizing the appropriate
 *   client specific information object.
 *
 *              <Report State> ::== <Common Header>
 *                                  <Client Handle>
 *                                  <Report-Type>
 *                                  [<ClientSI>]
 *                                  [<Integrity>]
 *
 * @version COPSReportMsg.java, v 1.00 2003
 *
 */
public class COPSReportMsg extends COPSMsg {
    /* COPSHeader coming from base class */
    private final List<COPSClientSI> _clientSI;
    private transient COPSHandle _clientHandle;
    private transient COPSReportType _report;
    private transient COPSIntegrity _integrity;

    /**
     * Default Constructor
     */
    public COPSReportMsg() {
        _clientHandle = null;
        _report = null;
        _integrity = null;
        _clientSI = new ArrayList<>();
    }

    /**
     * Constructor with data
     * @param data - the data to parse
     * @throws COPSException
     */
    protected COPSReportMsg (final byte[] data) throws COPSException {
        this();
        if (data != null) parse(data);
    }

    @Override
    public void checkSanity() throws COPSException {
        if ((_hdr == null) || (_clientHandle == null) || (_report == null))
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
        if (hdr.getOpCode() != COPSHeader.COPS_OP_RPT)
            throw new COPSException ("Error Header (no COPS_OP_REQ)");
        _hdr = hdr;
        setMsgLength();
    }

    /**
     * Add Report object to the message
     * @param    report              a  COPSReportType
     * @throws   COPSException
     */
    public void add(final COPSReportType report) throws COPSException {
        if (report == null)
            throw new COPSException ("Null Handle");

        //Message integrity object should be the very last one
        //If it is already added
        if (_integrity != null)
            throw new COPSException ("No null Handle");

        _report = report;
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

        //Message integrity object should be the very last one
        //If it is already added
        if (_integrity != null)
            throw new COPSException ("No null Handle");

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
        _clientSI.add(clientSI);
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

    /**
     * Get client Handle
     * @return   a COPSHandle
     */
    public COPSHandle getClientHandle() {
        return _clientHandle;
    }

    /**
     * Get report type
     * @return   a COPSReportType
     */
    public COPSReportType getReport() {
        return _report;
    }

    /**
     * Get clientSI
     * @return   a Vector
     */
    public List<COPSClientSI> getClientSI() {
        // Defensive Copy
        return new ArrayList<>(_clientSI);
    }

    /**
     * Get Integrity. Should check hasIntegrity() before calling
     * @return   a COPSIntegrity
     */
    public COPSIntegrity getIntegrity() {
        return (_integrity);
    }

    @Override
    public void writeData(final Socket id) throws IOException {
        //checkSanity();
        if (_hdr != null) _hdr.writeData(id);
        if (_clientHandle != null) _clientHandle.writeData(id);
        if (_report != null) _report.writeData(id);

        for (final COPSClientSI clientSI : _clientSI) {
            clientSI.writeData(id);
        }

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
                case COPSObjHeader.COPS_RPT: {
                    _report = new COPSReportType(buf);
                    _dataStart += _report.getDataLength();
                }
                break;
                case COPSObjHeader.COPS_CSI: {
                    COPSClientSI csi = new COPSClientSI(buf);
                    _dataStart += csi.getDataLength();
                    _clientSI.add(csi);
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
        if (hdr.getOpCode() != COPSHeader.COPS_OP_RPT)
            throw new COPSException ("Null Header");
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
        if (_clientHandle != null) len += _clientHandle.getDataLength();
        if (_report != null) len += _report.getDataLength();

        for (final COPSClientSI clientSI : _clientSI) {
            len += clientSI.getDataLength();
        }

        if (_integrity != null) len += _integrity.getDataLength();
        _hdr.setMsgLength(len);
    }

    @Override
    public void dump(final OutputStream os) throws IOException {
        _hdr.dump(os);

        if (_clientHandle != null)
            _clientHandle.dump(os);

        if (_report != null)
            _report.dump(os);

        for (final COPSClientSI clientSI : _clientSI) {
            clientSI.dump(os);
        }

        if (_integrity != null) {
            _integrity.dump(os);
        }
    }
}



