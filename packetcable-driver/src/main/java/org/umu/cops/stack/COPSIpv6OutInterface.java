/*
 * Copyright (c) 2003 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.stack;

import java.io.IOException;
import java.net.Socket;

/**
 * COPS IPv6 Output Interface
 *
 * @version COPSIpv6OutInterface.java, v 1.00 2003
 *
 */
public class COPSIpv6OutInterface extends COPSIpv6Interface {
    public COPSIpv6OutInterface(final COPSIpv6Address addr) {
        super(addr);
        _objHdr.setCNum(COPSObjHeader.COPS_OUTINTF);
    }

    public COPSIpv6OutInterface(final byte[] dataPtr) {
        super(null, dataPtr);
    }

    public COPSIpv6OutInterface(final COPSIpv6Address addr, final byte[] dataPtr) {
        super(addr, dataPtr);
    }

    /**
     * Method isInInterface
     * @return   a boolean
     */
    public boolean isInInterface() {
        return false;
    }

    /**
     * Writes data to given _socket
     * @param    id                  a  Socket
     * @throws   IOException
     */
    public void writeData(final Socket id) throws IOException {
        // TODO - implement me
    }

}
