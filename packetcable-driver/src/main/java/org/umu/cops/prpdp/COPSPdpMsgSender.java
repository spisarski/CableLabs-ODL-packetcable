/*
 * Copyright (c) 2004 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.prpdp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.prpep.COPSMsgSender;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

/**
 * COPS message transceiver class for provisioning connections at the PDP side.
 */
public class COPSPdpMsgSender extends COPSMsgSender {

    private final static Logger logger = LoggerFactory.getLogger(COPSPdpMsgSender.class);

    /**
     * Creates a COPSPepMsgSender
     *
     * @param clientType        COPS client-type
     * @param clientHandle      Client handle
     * @param sock              Socket to the PEP
     */
    public COPSPdpMsgSender(final short clientType, final COPSHandle clientHandle, final Socket sock) {
        super(clientType, clientHandle, sock);
        logger.info("Created new COPS PDP message sender");
    }

    /**
     * Sends a decision message
     * @param removeDecs    Decisions to be removed
     * @param installDecs   Decisions to be installed
     * @throws COPSPdpException
     */
    public void sendDecision(final Map<String, String> removeDecs, final Map<String, String> installDecs)
            throws COPSPdpException {
        logger.info("Send decision");
        /* <Decision Message> ::= <Common Header: Flag SOLICITED>
         *                          <Client Handle>
         *                          *(<Decision>) | <Error>
         *                          [<Integrity>]
         * <Decision> ::= <Context>
         *                  <Decision: Flags>
         *                  [<Named Decision Data: Provisioning>]
         * <Decision: Flags> ::= <Command-Code> NULLFlag
         * <Command-Code> ::= NULLDecision | Install | Remove
         * <Named Decision Data> ::= <<Install Decision> | <Remove Decision>>
         * <Install Decision> ::= *(<PRID> <EPD>)
         * <Remove Decision> ::= *(<PRID> | <PPRID>)
         *
         * Very important, this is actually being treated like this:
         * <Install Decision> ::= <PRID> | <EPD>
         * <Remove Decision> ::= <PRID> | <PPRID>
         *
        */

        // Common Header with the same ClientType as the request
        final COPSHeader hdr = new COPSHeader (COPSHeader.COPS_OP_DEC, getClientType());
        hdr.setFlag(COPSHeader.COPS_FLAG_SOLICITED);

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

                for (final Map.Entry<String, String> entry : removeDecs.entrySet()) {
                    //  <Named Decision Data: Provisioning> (PRID)
                    final COPSDecision dec2 = new COPSDecision(COPSDecision.DEC_NAMED);
                    final COPSPrID prid = new COPSPrID();
                    prid.setData(new COPSData(entry.getKey()));
                    dec2.setData(new COPSData(prid.getDataRep(), 0, prid.getDataLength()));
                    //  <Named Decision Data: Provisioning> (EPD)
                    final COPSDecision dec3 = new COPSDecision(COPSDecision.DEC_NAMED);
                    final COPSPrEPD epd = new COPSPrEPD();
                    epd.setData(new COPSData(entry.getValue()));
                    dec3.setData(new COPSData(epd.getDataRep(), 0, epd.getDataLength()));

                    decisionMsg.addDecision(dec2, cntxt);
                    decisionMsg.addDecision(dec3, cntxt);
                }
            }

            // Install Decisions
            //  <Decision: Flags>
            if (installDecs.size() > 0) {
                final COPSDecision idec1 = new COPSDecision();
                idec1.setCmdCode(COPSDecision.DEC_INSTALL);

                decisionMsg.addDecision(idec1, cntxt);

                for (final Map.Entry<String, String> entry : installDecs.entrySet()) {
                    //  <Named Decision Data: Provisioning> (PRID)
                    final COPSDecision dec2 = new COPSDecision(COPSDecision.DEC_NAMED);
                    final COPSPrID prid = new COPSPrID();
                    prid.setData(new COPSData(entry.getKey()));
                    dec2.setData(new COPSData(prid.getDataRep(), 0, prid.getDataLength()));
                    //  <Named Decision Data: Provisioning> (EPD)
                    final COPSDecision dec3 = new COPSDecision(COPSDecision.DEC_NAMED);
                    final COPSPrEPD epd = new COPSPrEPD();
                    epd.setData(new COPSData(entry.getValue()));
                    dec3.setData(new COPSData(epd.getDataRep(), 0, epd.getDataLength()));

                    decisionMsg.addDecision(dec2, cntxt);
                    decisionMsg.addDecision(dec3, cntxt);
                }

                /**
                COPSIntegrity intr = new COPSIntegrity();
                intr.setKeyId(19);
                intr.setSeqNum(9);
                intr.setKeyDigest(new COPSData("KEY DIGEST"));
                decisionMsg.add(intr);
                /**/
            }
        } catch (final COPSException e) {
            throw new COPSPdpException("Error making Msg");
        }

        //** Send the decision
        //**
        try {
            decisionMsg.writeData(_sock);
        } catch (IOException e) {
            throw new COPSPdpException("Failed to send the decision, reason: " + e.getMessage());
        }
    }

    /**
     * Sends a decision message which was not requested by the PEP
     * @param removeDecs    Decisions to be removed
     * @param installDecs   Decisions to be installed
     * @throws COPSPdpException
     */
    public void sendUnsolicitedDecision(final Map<String, String> removeDecs, final Map<String, String> installDecs)
            throws COPSPdpException {
        logger.info("Sending unsolicited decision");
        //** Example of an UNSOLICITED decision
        //**

        /* <Decision Message> ::= <Common Header: Flag UNSOLICITED>
         *                          <Client Handle>
         *                          *(<Decision>) | <Error>
         *                          [<Integrity>]
         * <Decision> ::= <Context>
         *                  <Decision: Flags>
         *                  [<Named Decision Data: Provisioning>]
         * <Decision: Flags> ::= <Command-Code> NULLFlag
         * <Command-Code> ::= NULLDecision | Install | Remove
         * <Named Decision Data> ::= <<Install Decision> | <Remove Decision>>
         * <Install Decision> ::= *(<PRID> <EPD>)
         * <Remove Decision> ::= *(<PRID> | <PPRID>)
         *
         * Very important, this is actually being treated like this:
         * <Install Decision> ::= <PRID> | <EPD>
         * <Remove Decision> ::= <PRID> | <PPRID>
         *
        */

        // Common Header with the same ClientType as the request
        final COPSHeader hdr = new COPSHeader (COPSHeader.COPS_OP_DEC, getClientType());

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
            final COPSDecision rdec1 = new COPSDecision();
            rdec1.setCmdCode(COPSDecision.DEC_REMOVE);

            decisionMsg.addDecision(rdec1, cntxt);

            for (final Map.Entry<String, String> entry : removeDecs.entrySet()) {
                //  <Named Decision Data: Provisioning> (PRID)
                final COPSDecision dec2 = new COPSDecision(COPSDecision.DEC_NAMED);
                final COPSPrID prid = new COPSPrID();
                prid.setData(new COPSData(entry.getKey()));
                dec2.setData(new COPSData(prid.getDataRep(), 0, prid.getDataLength()));
                //  <Named Decision Data: Provisioning> (EPD)
                final COPSDecision dec3 = new COPSDecision(COPSDecision.DEC_NAMED);
                final COPSPrEPD epd = new COPSPrEPD();
                epd.setData(new COPSData(entry.getValue()));
                dec3.setData(new COPSData(epd.getDataRep(), 0, epd.getDataLength()));

                decisionMsg.addDecision(dec2, cntxt);
                decisionMsg.addDecision(dec3, cntxt);
            }

            // Install Decisions
            //  <Decision: Flags>
            final COPSDecision idec1 = new COPSDecision();
            idec1.setCmdCode(COPSDecision.DEC_INSTALL);

            decisionMsg.addDecision(idec1, cntxt);

            for (final Map.Entry<String, String> entry : installDecs.entrySet()) {
                //  <Named Decision Data: Provisioning> (PRID)
                final COPSDecision dec2 = new COPSDecision(COPSDecision.DEC_NAMED);
                final COPSPrID prid = new COPSPrID();
                prid.setData(new COPSData(entry.getKey()));
                dec2.setData(new COPSData(prid.getDataRep(), 0, prid.getDataLength()));
                //  <Named Decision Data: Provisioning> (EPD)
                final COPSDecision dec3 = new COPSDecision(COPSDecision.DEC_NAMED);
                final COPSPrEPD epd = new COPSPrEPD();
                epd.setData(new COPSData(entry.getValue()));
                dec3.setData(new COPSData(epd.getDataRep(), 0, epd.getDataLength()));

                decisionMsg.addDecision(dec2, cntxt);
                decisionMsg.addDecision(dec3, cntxt);
            }

            /**
            COPSIntegrity intr = new COPSIntegrity();
            intr.setKeyId(19);
            intr.setSeqNum(9);
            intr.setKeyDigest(new COPSData("KEY DIGEST"));
            decisionMsg.add(intr);
            /**/
        } catch (COPSException e) {
            throw new COPSPdpException("Error making Msg");
        }

        //** Send the decision
        try {
            decisionMsg.writeData(_sock);
        } catch (IOException e) {
            throw new COPSPdpException("Failed to send the decision, reason: " + e.getMessage());
        }
    }

    /**
     * Sends a message asking that the request state be deleted
     * @throws   COPSPdpException
     */
    public void sendDeleteRequestState() throws COPSPdpException {
        logger.info("Sending delete request state");
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
        final COPSHeader hdr = new COPSHeader (COPSHeader.COPS_OP_DEC, getClientType());

        // Client Handle with the same clientHandle as the request
        final COPSHandle clienthandle = new COPSHandle(_handle.getId());
        // Decisions
        //  <Context>
        final COPSContext cntxt = new COPSContext(COPSContext.CONFIG, (short) 0);
        //  <Decision: Flags>
        final COPSDecision dec = new COPSDecision();
        dec.setCmdCode(COPSDecision.DEC_REMOVE);
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
     * Sends a request asking that a new request state be created
     * @throws   COPSPdpException
     */
    public void sendOpenNewRequestState() throws COPSPdpException {
        logger.info("Sending open new request state");
        /* <Decision Message> ::= <Common Header: Flag UNSOLICITED>
         *                          <Client Handle>
         *                          *(<Decision>)
         *                          [<Integrity>]
         * <Decision> ::= <Context>
         *                  <Decision: Flags>
         * <Decision: Flags> ::= Install Request-State
         *
        */

        // Common Header with the same ClientType as the request (default UNSOLICITED)
        final COPSHeader hdr = new COPSHeader (COPSHeader.COPS_OP_DEC, getClientType());

        // Client Handle with the same clientHandle as the request
        final COPSHandle clienthandle = new COPSHandle(_handle.getId());

        // Decisions
        //  <Context>
        final COPSContext cntxt = new COPSContext(COPSContext.CONFIG, (short) 0);
        //  <Decision: Flags>
        final COPSDecision dec = new COPSDecision();
        dec.setCmdCode(COPSDecision.DEC_INSTALL);
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
        final COPSHeader hdr = new COPSHeader (COPSHeader.COPS_OP_SSQ, getClientType());

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
