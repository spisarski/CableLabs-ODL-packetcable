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
 * COPS Integrity Object
 *
 * @version COPSIntegrity.java, v 1.00 2003
 *
 */
public class COPSIntegrity extends COPSObjBase {
    private COPSObjHeader _objHdr;
    private final int _keyId;
    private final int _seqNum;
    private COPSData _keyDigest;
    private COPSData _padding;

    public COPSIntegrity() {
        _objHdr = new COPSObjHeader();
        _objHdr.setCNum(COPSObjHeader.COPS_MSG_INTEGRITY);
        _objHdr.setCType((byte) 1);
        _keyId = 0;
        _seqNum = 0;
    }

    public COPSIntegrity(byte[] dataPtr) {
        _objHdr = new COPSObjHeader();
        _objHdr.parse(dataPtr);
        // _objHdr.checkDataLength();
        int tempKeyId = 0;
        tempKeyId |= ((short) dataPtr[4]) << 24;
        tempKeyId |= ((short) dataPtr[5]) << 16;
        tempKeyId |= ((short) dataPtr[6]) << 8;
        tempKeyId |= ((short) dataPtr[7]) & 0xFF;
        _keyId = tempKeyId;

        int tempSeqNum = 0;
        tempSeqNum |= ((short) dataPtr[8]) << 24;
        tempSeqNum |= ((short) dataPtr[9]) << 16;
        tempSeqNum |= ((short) dataPtr[10]) << 8;
        tempSeqNum |= ((short) dataPtr[11]) & 0xFF;
        _seqNum = tempSeqNum;

        final int dLen = _objHdr.getDataLength() - 12;
        setKeyDigest(new COPSData(dataPtr, 12, dLen));
    }

    /**
     * Method setKeyDigest
     * @param    keyDigest           a  COPSData
     */
    public void setKeyDigest(final COPSData keyDigest) {
        _keyDigest = keyDigest;
        if (_keyDigest.length() % 4 != 0) {
            int padLen = 4 - _keyDigest.length() % 4;
            _padding = getPadding(padLen);
        }
        // _objHdr.setDataLength(sizeof(u_int32_t)
        //                           + sizeof(u_int32_t) + _keyDigest.length());
        _objHdr.setDataLength((short) (8 + _keyDigest.length()));
    }

    /**
     * Returns size in number of octects, including header
     * @return   a short
     */
    public short getDataLength() {
        //Add the size of the header also
        final int lpadding;
        if (_padding != null) lpadding = _padding.length();
        else lpadding = 0;
        return ((short) (_objHdr.getDataLength() + lpadding));
    }

    /**
     * Method isMessageIntegrity
     * @return   a boolean
     */
    public boolean isMessageIntegrity() {
        return true;
    }

    /**
     * Write data on a given network socket
     * @param    id                  a  Socket
     * @throws   IOException
     */
    public void writeData(final Socket id) throws IOException {
        _objHdr.writeData(id);

        final byte[] buf = new byte[8];
        buf[0] = (byte) (_keyId >> 24);
        buf[1] = (byte) (_keyId >> 16);
        buf[2] = (byte) (_keyId >> 8);
        buf[3] = (byte) _keyId;
        buf[4] = (byte) (_seqNum >> 24);
        buf[5] = (byte) (_seqNum >> 16);
        buf[6] = (byte) (_seqNum >> 8);
        buf[7] = (byte) _seqNum;
        COPSUtil.writeData(id, buf, 8);

        COPSUtil.writeData(id, _keyDigest.getData(), _keyDigest.length());
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
        os.write(("Key Id: " + _keyId + "\n").getBytes());
        os.write(("Sequence: " + _seqNum + "\n").getBytes());
        os.write(("Key digest: " + _keyDigest.str() + "\n").getBytes());
    }
}


