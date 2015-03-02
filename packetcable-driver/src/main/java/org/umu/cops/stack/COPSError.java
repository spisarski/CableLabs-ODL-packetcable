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
 * COPS Error
 *
 * @version COPSError.java, v 1.00 2003
 *
 */
public class COPSError extends COPSObjBase {

    public final static byte COPS_ERR_BAD_HANDLE = 1;
    public final static byte COPS_ERR_BAD_HANDLE_REF = 2;
    public final static byte COPS_ERR_BAD_MSG_FORMAT = 3;
    public final static byte COPS_ERR_FAIL_PROCESS = 4;
    public final static byte COPS_ERR_MISSING_INFO = 5;
    public final static byte COPS_ERR_UNSUPPORTED_CLIENT_TYPE = 6;
    public final static byte COPS_ERR_MANDATORY_OBJECT_MISSING = 7;
    public final static byte COPS_ERR_CLIENT_FAILURE = 8;
    public final static byte COPS_ERR_COMM_FAILURE = 9;
    public final static byte COPS_ERR_UNKNOWN = 10;
    public final static byte COPS_ERR_SHUTTING_DOWN = 11;
    public final static byte COPS_ERR_PDP_REDIRECT = 12;
    public final static byte COPS_ERR_UNKNOWN_OBJECT = 13;
    public final static byte COPS_ERR_AUTH_FAILURE = 14;
    public final static byte COPS_ERR_AUTH_REQUIRED = 15;
    public final static byte COPS_ERR_MA = 16;

    private final static String G_errmsgArray[] = {
        "Unknown.",
        "Bad handle.",
        "Invalid handle reference.",
        "Bad message format (Malformed message).",
        "Unable to process.",
        "Mandatory client-specific info missing.",
        "Unsupported client-type",
        "Mandatory COPS object missing.",
        "Client failure.",
        "Communication failure.",
        "Unknown.",
        "Shutting down.",
        "Redirect to preferred server.",
        "Unknown COPS object",
        "Authentication failure.",
        "Authentication required.",
    };

    private final COPSObjHeader _objHdr;
    private transient short _errCode;
    private transient short _errSubCode;

    public COPSError(final short errCode, final short subCode) {
        _objHdr = new COPSObjHeader();
        _errCode = errCode;
        _errSubCode = subCode;
        _objHdr.setCNum(COPSObjHeader.COPS_ERROR);
        _objHdr.setCType((byte) 1);
        _objHdr.setDataLength((short) 4);
    }

    protected COPSError(final byte[] dataPtr) {
        _objHdr = new COPSObjHeader();
        _objHdr.parse(dataPtr);
        // _objHdr.checkDataLength();

        _errCode |= ((short) dataPtr[4]) << 8;
        _errCode |= ((short) dataPtr[5]) & 0xFF;
        _errSubCode |= ((short) dataPtr[6]) << 8;
        _errSubCode |= ((short) dataPtr[7]) & 0xFF;

        // _objHdr.setDataLength(sizeof(u_int32_t));
        _objHdr.setDataLength((short) 4);
    }

    public short getErrCode() {
		return _errCode;
	}
    
    public short getErrSubCode() {
		return _errSubCode;
	}

    @Override
    public short getDataLength() {
        return (_objHdr.getDataLength());
    }

    /**
     * Method getDescription
     * @return   a String
     */
    public String getDescription() {
        //TODO - define error sub-codes
        return (G_errmsgArray[_errCode] + ':');
    }

    @Override
    public boolean isError() {
        return true;
    }

    @Override
    public void writeData(final Socket id) throws IOException {
        _objHdr.writeData(id);

        final byte[] buf = new byte[4];

        buf[0] = (byte) (_errCode >> 8);
        buf[1] = (byte) _errCode;
        buf[2] = (byte) (_errSubCode >> 8);
        buf[3] = (byte) _errSubCode;

        COPSUtil.writeData(id, buf, 4);
    }

    /**
     * Write an object textual description in the output stream
     * @param    os                  an OutputStream
     * @throws   IOException
     */
    public void dump(final OutputStream os) throws IOException {
        _objHdr.dump(os);
        os.write(("Error Code: " + _errCode + "\n").getBytes());
        os.write(("Error Sub Code: " + _errSubCode + "\n").getBytes());
    }
}

