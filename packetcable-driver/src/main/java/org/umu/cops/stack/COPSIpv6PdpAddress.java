/*
 * Copyright (c) 2003 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.stack;

import java.io.IOException;
import java.net.Socket;

/**
 * COPS IPv6 PDP Address
 *
 * @version COPSIpv6PdpAddress.java, v 1.00 2003
 *
 */
abstract public class COPSIpv6PdpAddress extends COPSPdpAddress {

    protected final COPSObjHeader _objHdr;
    protected final COPSIpv6Address _addr;
    private final short _reserved;
    protected final short _tcpPort;

    protected COPSIpv6PdpAddress() {
        _addr = new COPSIpv6Address();
        _objHdr = new COPSObjHeader();
        _objHdr.setCType((byte) 2);
        // _objHdr.setDataLength((short) _addr.getDataLength() + sizeof(u_int32_t));
        _objHdr.setDataLength((short) (_addr.getDataLength() + 4));
        _reserved = (short)0;
        _tcpPort = (short)0;
    }

    protected COPSIpv6PdpAddress(final byte[] dataPtr) {
        _addr = new COPSIpv6Address();
        _objHdr = new COPSObjHeader();
        _objHdr.parse(dataPtr);
        // _objHdr.checkDataLength();

        final byte[] buf = new byte[16];
        System.arraycopy(dataPtr,2,buf,0,16);
        _addr.parse(buf);

        short tmpReserved = (short)0;
        tmpReserved |= ((short) dataPtr[20]) << 8;
        tmpReserved |= ((short) dataPtr[21]) & 0xFF;
        _reserved = tmpReserved;

        short tmpTcpPort = (short)0;
        tmpTcpPort |= ((short) dataPtr[22]) << 8;
        tmpTcpPort |= ((short) dataPtr[23]) & 0xFF;
        _tcpPort = tmpTcpPort;

        // _objHdr.setDataLength(_addr.getDataLength() + sizeof(u_int32_t));
        _objHdr.setDataLength((short) (_addr.getDataLength() + 4));
    }

    /**
     * Returns size in number of octects, including header
     * @return   a short
     */
    public short getDataLength() {
        //Add the size of the header also
        return (_objHdr.getDataLength());
    }

    /**
     * Method isIpv6PdpAddress
     * @return   a boolean
     */
    public boolean isIpv6PdpAddress() {
        return true;
    }

    /**
     * Write data on a given network _socket
     * @param    id                  a  Socket
     * @throws   IOException
     */
    public void writeData(final Socket id) throws IOException {
        //
        _objHdr.writeData(id);
        _addr.writeData(id);

        final byte[] buf = new byte[4];
        buf[0] = (byte) (_reserved >> 8);
        buf[1] = (byte) _reserved;
        buf[2] = (byte) (_tcpPort >> 8);
        buf[3] = (byte) _tcpPort ;

        COPSUtil.writeData(id, buf, 4);
    }
}


