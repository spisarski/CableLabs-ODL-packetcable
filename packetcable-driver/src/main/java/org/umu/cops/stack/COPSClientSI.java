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
 * COPS Client Specific Information Object
 *
 * @version COPSClientSI.java, v 1.00 2003
 *
 */
public class COPSClientSI extends COPSObjBase {
    public final static byte CSI_SIGNALED = 1;
    public final static byte CSI_NAMED = 2;

    private final COPSObjHeader _objHdr;
    private transient COPSData _data;
    private transient COPSData _padding;

    /**
     * Constructor #1
     * @param type - used to set the CType on the COPSObjHeader
     */
    public COPSClientSI(final byte type) {
        this(COPSObjHeader.COPS_CSI, type);
    }

    /**
     * Constructor #2
     * @param ctype - used to set the CType value on the COPSObjHeader
     * @param cnum - used to set the CNum value on the COPSObjHeader
     */
    public COPSClientSI(final byte cnum, final byte ctype) {
        _objHdr = new COPSObjHeader();
        _objHdr.setCNum(cnum);
        _objHdr.setCType(ctype);
    }

    /**
     Parse the data and create a ClientSI object
     */
    protected COPSClientSI(final byte[] dataPtr) {
        _objHdr = new COPSObjHeader();
        _objHdr.parse(dataPtr);
        // _objHdr.checkDataLength();

        //Get the length of data following the obj header
        final short dLen = (short) (_objHdr.getDataLength() - 4);
        setData(new COPSData(dataPtr, 4, dLen));
    }

    /**
     * Method setData
     * @param    data                a  COPSData
     */
    public void setData(final COPSData data) {
        _data = data;
        if (_data.length() % 4 != 0) {
            _padding = getPadding(_data.length() % 4);
        }
        _objHdr.setDataLength((short) _data.length());
    }

    @Override
    public short getDataLength() {
        //Add the size of the header also
        final int lpadding;
        if (_padding != null) lpadding = _padding.length();
        else lpadding = 0;
        return (short) (_objHdr.getDataLength() + lpadding);
    }

    /**
     * Method getData
     * @return   a COPSData
     */
    public COPSData getData() {
        return _data;
    }

    @Override
    public boolean isClientSI() {
        return true;
    }

    @Override
    public void writeData(final Socket id) throws IOException {
        _objHdr.writeData(id);
        COPSUtil.writeData(id, _data.getData(), _data.length());
        if (_padding != null) {
            COPSUtil.writeData(id, _padding.getData(), _padding.length());
        }
    }

    /**
     * Write an object textual description in the output stream
     * @param    os                  an OutputStream
     * @throws   IOException
     */
    public void dump(final OutputStream os) throws IOException {
        _objHdr.dump(os);
        os.write(("client-SI: " + _data.str() + "\n").getBytes());
    }
}

