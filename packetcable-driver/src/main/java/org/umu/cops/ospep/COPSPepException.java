/*
 * Copyright (c) 2004 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.ospep;

import org.umu.cops.stack.COPSException;

/**
 * COPS PEP Exception
 *
 * @version COPSPepException.java, v 2.00 2004
 *
 */
public class COPSPepException extends COPSException {

    /**
     * Creates a <tt>COPSPdpException</tt> with the given message.
     * @param msg    Exception message
     */

    public COPSPepException(final String msg) {
        super(msg, 0);
    }

    /**
      * Creates a <tt>COPSPdpException</tt> with the given message and return code.
      * @param msg      Exception message
      * @param retCode     Return code
      */
    public COPSPepException(final String msg, final int retCode) {
        super(msg, retCode);
    }

}
