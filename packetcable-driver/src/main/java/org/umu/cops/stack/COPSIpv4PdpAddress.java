/*
 * Copyright (c) 2003 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.stack;

import java.io.IOException;
import java.net.Socket;

/**
 * COPS IPv4 PDP Address
 *
 * @version COPSIpv4PdpAddress.java, v 1.00 2003
 *
 */
abstract public class COPSIpv4PdpAddress extends COPSPdpAddress {

    protected final COPSObjHeader _objHdr;
    protected final COPSIpv4Address _addr;
    private final short _reserved;
    protected final short _tcpPort;

    protected COPSIpv4PdpAddress() {
        _addr = new COPSIpv4Address();
        _objHdr = new COPSObjHeader();
        _objHdr.setCType((byte) 1);
        // _objHdr.setDataLength((short) _addr.getDataLength() + sizeof(u_int32_t));
        _objHdr.setDataLength((short) (_addr.getDataLength() + 4));
        _reserved = (short)0;
        _tcpPort = (short)0;
    }

    protected COPSIpv4PdpAddress(final byte[] dataPtr) {
        _addr = new COPSIpv4Address();
        _objHdr = new COPSObjHeader();
        _objHdr.parse(dataPtr);
        // _objHdr.checkDataLength();

        final byte[] buf = new byte[4];
        System.arraycopy(dataPtr,2,buf,0,4);
        _addr.parse(buf);

        short tempReserved = (short)0;
        tempReserved |= ((short) dataPtr[8]) << 8;
        tempReserved |= ((short) dataPtr[9]) & 0xFF;
        _reserved = tempReserved;

        short tempTcpPort = (short)0;
        tempTcpPort |= ((short) dataPtr[10]) << 8;
        tempTcpPort |= ((short) dataPtr[11]) & 0xFF;
        _tcpPort = tempTcpPort;

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
        buf[0] = (byte) (_reserved & 0xFF);
        buf[1] = (byte) (_reserved << 8);
        buf[2] = (byte) (_tcpPort & 0xFF);
        buf[3] = (byte) (_tcpPort << 8);

        COPSUtil.writeData(id, buf, 4);
    }

}

