/*
 * Copyright (c) 2004 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.prpep;

import org.umu.cops.stack.COPSException;

/**
 * COPS PEP Exception
 *
 * @version COPSPepException.java, v 2.00 2004
 *
 */
public class COPSPepException extends COPSException {

    public COPSPepException(final String s) {
        super(s, 0);
    }

    public COPSPepException(final String msg, final int retCode) {
        super(msg, retCode);
    }

}
