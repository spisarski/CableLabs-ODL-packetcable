package org.umu.cops.ospdp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.prpep.COPSMsgSender;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * COPS message transceiver class for outsourcing connections at the PDP side.
 */
public class COPSPdpOSMsgSender extends COPSMsgSender {

    private static final Logger logger = LoggerFactory.getLogger(COPSPdpOSMsgSender.class);

    /**
     * Creates a COPSPepMsgSender
     *
     * @param clientType   COPS client-type
     * @param clientHandle Client handle
     * @param sock         Socket to the PEP
     */
    public COPSPdpOSMsgSender(final short clientType, final COPSHandle clientHandle, final Socket sock) {
        super(clientType, clientHandle, sock);
    }

    /**
     * Sends a decision message which was requested by the PEP
     *
     * @param removeDecs  Decisions to be removed
     * @param installDecs Decisions to be installed
     * @throws COPSPdpException
     */
    public void sendSolicitedDecision(final List<COPSDecision> removeDecs, final List<COPSDecision> installDecs)
            throws COPSPdpException {
        sendDecision(removeDecs, installDecs, true);
    }

    /**
     * Sends a decision message which was not requested by the PEP
     *
     * @param removeDecs  Decisions to be removed
     * @param installDecs Decisions to be installed
     * @throws COPSPdpException
     */
    public void sendUnsolicitedDecision(final List<COPSDecision> removeDecs, final List<COPSDecision> installDecs)
            throws COPSPdpException {
        logger.info("Sending unsolicited decision");
        sendDecision(removeDecs, installDecs, false);
    }

    /**
     * Sends a decision message to the PEP
     *
     * @param removeDecs  Decisions to be removed
     * @param installDecs Decisions to be installed
     * @param solicited   <tt>true</tt> if the PEP requested this decision, <tt>false</tt> otherwise
     * @throws COPSPdpException
     */
    public void sendDecision(final List<COPSDecision> removeDecs, final List<COPSDecision> installDecs,
                             boolean solicited) throws COPSPdpException {
        logger.info("Sending decision");
        // Common Header holding the same ClientType as the request
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_DEC, getClientType());

        if (solicited) {
            hdr.setFlag(COPSHeader.COPS_FLAG_SOLICITED);
        }

        // Client Handle with the same clientHandle as the request
        final COPSHandle handle = new COPSHandle(getClientHandle().getId());
        final COPSDecisionMsg decisionMsg = new COPSDecisionMsg();
        try {
            decisionMsg.add(hdr);
            decisionMsg.add(handle);

            // Decisions (no flags supplied)
            //  <Context>
            final COPSContext cntxt = new COPSContext(COPSContext.CONFIG, (short) 0);

            // Remove Decisions
            //  <Decision: Flags>
            if (removeDecs.size() > 0) {
                final COPSDecision rdec1 = new COPSDecision();
                rdec1.setCmdCode(COPSDecision.DEC_REMOVE);
                decisionMsg.addDecision(rdec1, cntxt);
                for (final COPSDecision decision : removeDecs) {
                    decisionMsg.addDecision(decision, cntxt);
                }
            }

            // Install Decisions
            //  <Decision: Flags>
            if (installDecs.size() > 0) {
                final COPSDecision idec1 = new COPSDecision();
                idec1.setCmdCode(COPSDecision.DEC_INSTALL);
                decisionMsg.addDecision(idec1, cntxt);
                for (final COPSDecision decision : installDecs) {
                    decisionMsg.addDecision(decision, cntxt);
                }
            }
        } catch (final COPSException e) {
            e.printStackTrace();
            throw new COPSPdpException("Error making Msg");
        }

        //** Send decision
        //**
        try {
            decisionMsg.writeData(_sock);
        } catch (final IOException e) {
            throw new COPSPdpException("Failed to send the decision, reason: " + e.getMessage());
        }
    }

    /**
     * FIXME: unused?
     * Sends a message asking that the request state be deleted
     *
     * @throws COPSPdpException
     */
    public void sendDeleteRequestState() throws COPSPdpException {
        logger.info("Sending delete request state");
        sendRequestState(COPSDecision.DEC_REMOVE);
    }

    /**
     * Method sendOpenNewRequestState
     *
     * @throws COPSPdpException
     */
    //FIXME: Unused?
    public void sendOpenNewRequestState() throws COPSPdpException {
        logger.info("Sending open new request state");
        sendRequestState(COPSDecision.DEC_INSTALL);
    }

    /**
     * Sends the COPS request state
     * @param decision - the decision code
     * @throws COPSPdpException
     */
    private void sendRequestState(final byte decision) throws COPSPdpException {
        /* <Decision Message> ::= <Common Header: Flag UNSOLICITED>
         *                          <Client Handle>
         *                          *(<Decision>)
         *                          [<Integrity>]
         * <Decision> ::= <Context>
         *                  <Decision: Flags>
         * <Decision: Flags> ::= Remove Request-State
         *
        */

        // Common Header with the same ClientType as the request (default UNSOLICITED)
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_DEC, getClientType());

        // Client Handle with the same clientHandle as the request
        final COPSHandle clienthandle = new COPSHandle(_handle.getId());
        // Decisions
        //  <Context>
        final COPSContext cntxt = new COPSContext(COPSContext.CONFIG, (short) 0);
        //  <Decision: Flags>
        final COPSDecision dec = new COPSDecision();
        dec.setCmdCode(decision);
        dec.setFlags(COPSDecision.F_REQSTATE);

        final COPSDecisionMsg decisionMsg = new COPSDecisionMsg();
        try {
            decisionMsg.add(hdr);
            decisionMsg.add(clienthandle);
            decisionMsg.addDecision(dec, cntxt);
        } catch (COPSException e) {
            throw new COPSPdpException("Error making Msg");
        }

        try {
            decisionMsg.writeData(_sock);
        } catch (IOException e) {
            throw new COPSPdpException("Failed to send the open new request state, reason: " + e.getMessage());
        }
    }

    /**
     * Sends a message asking for a COPS sync operation
     * @throws COPSPdpException
     */
    public void sendSyncRequestState() throws COPSPdpException {
        logger.info("Sending sync request state");
        /* <Synchronize State Request>  ::= <Common Header>
         *                                  [<Client Handle>]
         *                                  [<Integrity>]
         */

        // Common Header with the same ClientType as the request
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_SSQ, getClientType());

        // Client Handle with the same clientHandle as the request
        final COPSHandle clienthandle = new COPSHandle(_handle.getId());
        final COPSSyncStateMsg msg = new COPSSyncStateMsg();
        try {
            msg.add(hdr);
            msg.add(clienthandle);
        } catch (Exception e) {
            throw new COPSPdpException("Error making Msg");
        }

        try {
            msg.writeData(_sock);
        } catch (IOException e) {
            throw new COPSPdpException("Failed to send the sync state request, reason: " + e.getMessage());
        }
    }

}
