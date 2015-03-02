/*
 * Copyright (c) 2003 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.stack;

/**
 * COPS IPv6 Interface
 *
 * @version COPSIpv6Interface.java, v 1.00 2003
 *
 */
public abstract class COPSIpv6Interface extends COPSInterface {

    protected final COPSObjHeader _objHdr;
    private final COPSIpv6Address _addr;
    private final int _ifindex;

    protected COPSIpv6Interface(final COPSIpv6Address addr) {
        this._addr = addr;
        _objHdr = new COPSObjHeader();
        _objHdr.setCType((byte) 2);
        _objHdr.setDataLength((short) (_addr.getDataLength() + 4));
        this._ifindex = 0;
    }

    protected COPSIpv6Interface(final COPSIpv6Address addr, byte[] dataPtr) {
        this._addr = addr;
        _objHdr = new COPSObjHeader();
        _objHdr.parse(dataPtr);
        // _objHdr.checkDataLength();

        byte[] buf = new byte[4];
        System.arraycopy(dataPtr,4,buf,0,16);

        _addr.parse(buf);

        int tmpIfIndex = 0;
        tmpIfIndex |= ((int) dataPtr[20]) << 24;
        tmpIfIndex |= ((int) dataPtr[21]) << 16;
        tmpIfIndex |= ((int) dataPtr[22]) << 8;
        tmpIfIndex |= ((int) dataPtr[23]) & 0xFF;
        _ifindex = tmpIfIndex;

        _objHdr.setDataLength((short) (_addr.getDataLength() + 4));
    }

    @Override
    public boolean isIpv6Address() {
        return true;
    }

    /**
     * Returns size in number of octects, including header
     * @return   a short
     */
    public short getDataLength() {
        //Add the size of the header also
        return (_objHdr.getDataLength());
    }

}




