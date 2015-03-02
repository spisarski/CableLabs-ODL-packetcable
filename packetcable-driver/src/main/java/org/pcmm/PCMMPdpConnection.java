/*
 @header@
 */

package org.pcmm;

import org.umu.cops.prpdp.COPSPdpConnection;
import org.umu.cops.stack.COPSPepId;

import java.net.Socket;

/**
 * Class for managing an provisioning connection at the PDP side.
 * // TODO - Determine if this class should go away completely as it is now identical to the super
 */
public class PCMMPdpConnection extends COPSPdpConnection implements Runnable {

    /**
     * Creates a new PDP connection
     *
     * @param sock    Socket connected to PEP
     * @param process Object for processing policy data
     */
    public PCMMPdpConnection(final COPSPepId pepId, final Socket sock, final PCMMPdpDataProcess process,
                             final short kaTimer) {
        super(pepId, sock, process, kaTimer);
    }

}
