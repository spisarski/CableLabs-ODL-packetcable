/*
 * Copyright (c) 2003 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.stack;

import java.io.IOException;
import java.net.Socket;

/**
 * COPS IPv4 Output Interface
 *
 * @version COPSIpv4OutInterface.java, v 1.00 2003
 *
 */
public class COPSIpv4OutInterface extends COPSIpv4Interface {
    public COPSIpv4OutInterface() {
        _objHdr = new COPSObjHeader();
        _objHdr.setCNum(COPSObjHeader.COPS_ININTF);
    }

    public COPSIpv4OutInterface(final byte[] dataPtr) {
        super(dataPtr);
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
     * @param    id                  a  Socket
     * @throws   IOException
     */
    public void writeData(final Socket id) throws IOException {
    }

}


