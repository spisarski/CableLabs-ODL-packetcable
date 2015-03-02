/*
 * Copyright (c) 2003 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.stack;

import java.io.IOException;
import java.net.Socket;

/**
 * COPS IPv6 Input Interface
 *
 * @version COPSIpv6InInterface.java, v 1.00 2003
 *
 */
public class COPSIpv6InInterface extends COPSIpv6Interface {

    public COPSIpv6InInterface() {
        this((COPSIpv6Address)null);
    }
    public COPSIpv6InInterface(final COPSIpv6Address addr) {
        super(addr);
        _objHdr.setCNum(COPSObjHeader.COPS_ININTF);
    }

    public COPSIpv6InInterface(final byte[] dataPtr) {
        super(null, dataPtr);
    }

    public COPSIpv6InInterface(final COPSIpv6Address addr, final byte[] dataPtr) {
        super(addr, dataPtr);
    }

    /**
     * Method isInInterface
     * @return   a boolean
     */
    public boolean isInInterface() {
        return true;
    }

    /**
     * Writes data to given socket
     *
     * @param    id                  a  Socket
     * @throws   IOException
     */
    public void writeData(final Socket id) throws IOException {
    }
}


