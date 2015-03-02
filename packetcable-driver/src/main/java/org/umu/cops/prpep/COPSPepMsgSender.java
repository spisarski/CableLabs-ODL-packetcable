/*
 * Copyright (c) 2004 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.prpep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

/**
 * COPSPepMsgSender sends COPS messages to PDP.
 *
 * @version COPSPepMsgSender.java, v 2.00 2004
 *
 */
public class COPSPepMsgSender extends COPSMsgSender {

    private final static Logger logger = LoggerFactory.getLogger(COPSPepConnection.class);

    /**
     * Create a COPSPepMsgSender
     *
     * @param clientType        client-type
     * @param clientHandle      client handle
     * @param sock              _socket of PDP connection
     */
    public COPSPepMsgSender (final short clientType, final COPSHandle clientHandle, final Socket sock) {
        super(clientType, clientHandle, sock);
    }

    /**
     * Send Request to PDP.
     *   The PEP establishes a request state client handle for which the
     *   remote PDP may maintain state.
     *
     * @param    clientSIs              a  Hashtable
     * @throws   COPSPepException
     */
    public void sendRequest(final Map<String, String> clientSIs) throws COPSPepException {
        logger.info("Sending request");
        // Create COPS Message
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_REQ, _clientType);
        final COPSContext cntxt = new COPSContext(COPSContext.CONFIG , (short) 0);

        final COPSHandle handle = _handle;

        // Add the clientSIs
        final COPSReqMsg msg = new COPSReqMsg();
        try {
            msg.add(hdr);
            msg.add(handle);
            msg.add(cntxt);

            for (Map.Entry<String, String> entry : clientSIs.entrySet()) {
                String strprid = entry.getKey();
                String strepd = entry.getValue();

                //  (PRID)
                final COPSClientSI cSi = new COPSClientSI(COPSClientSI.CSI_NAMED);
                final COPSPrID prid = new COPSPrID();
                prid.setData(new COPSData(strprid));
                cSi.setData(new COPSData(prid.getDataRep(), 0, prid.getDataLength()));

                //  (EPD)
                final COPSClientSI cSi2 = new COPSClientSI(COPSClientSI.CSI_NAMED);
                final COPSPrEPD epd = new COPSPrEPD();
                epd.setData(new COPSData(strepd));
                cSi2.setData(new COPSData(epd.getDataRep(), 0, epd.getDataLength()));

                msg.add(cSi);
                msg.add(cSi2);
            }
        } catch (final COPSException e) {
            throw new COPSPepException("Error making Request Msg, reason: " + e.getMessage());
        }

        // Send message
        try {
            msg.writeData(_sock);
        } catch (final IOException e) {
            throw new COPSPepException("Failed to send the request, reason: " + e.getMessage());
        }
    }

    /**
     * Send Fail Report to PDP.
     *    The RPT message is used by the PEP to communicate to the PDP its
     *    success or failure in carrying out the PDP's decision, or to report
     *    an accounting related change in state.
     *
     * @throws   COPSPepException
     */
    public void sendFailReport(final Map<String, String> clientSIs) throws COPSPepException {
        logger.info("Sending fail report");
        sendReport(clientSIs, COPSReportType.FAILURE);
    }

    /**
     * Send Succes Report to PDP.
     *    The RPT message is used by the PEP to communicate to the PDP its
     *    success or failure in carrying out the PDP's decision, or to report
     *    an accounting related change in state.
     *
     * @throws   COPSPepException
     */
    public void sendSuccessReport(final Map<String, String> clientSIs) throws COPSPepException {
        logger.info("Sending success report");
        sendReport(clientSIs, COPSReportType.SUCCESS);
    }

    public void sendAcctReport(final Map<String, String> clientSIs) throws COPSPepException {
        logger.info("Sending accounting report");
        sendReport(clientSIs, COPSReportType.ACCT);
    }

    private void sendReport(final Map<String, String> clientSIs, final short type)
            throws COPSPepException {
        final COPSReportMsg msg = new COPSReportMsg();
        // Report SUCESS
        try {
            final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_RPT, _clientType);
            final COPSHandle hnd = _handle;

            final COPSReportType report = new COPSReportType(type);

            msg.add(hdr);
            msg.add(hnd);
            msg.add(report);

            for (final Map.Entry<String, String> entry : clientSIs.entrySet()) {
                final String strprid = entry.getKey();
                final String strepd = entry.getValue();

                //  (PRID)
                final COPSClientSI cSi = new COPSClientSI(COPSClientSI.CSI_NAMED);
                final COPSPrID prid = new COPSPrID();
                prid.setData(new COPSData(strprid));
                cSi.setData(new COPSData(prid.getDataRep(), 0, prid.getDataLength()));

                //  (EPD)
                final COPSClientSI cSi2 = new COPSClientSI(COPSClientSI.CSI_NAMED);
                final COPSPrEPD epd = new COPSPrEPD();
                epd.setData(new COPSData(strepd));
                cSi2.setData(new COPSData(epd.getDataRep(), 0, epd.getDataLength()));

                msg.add(cSi);
                msg.add(cSi2);
            }

        } catch (final COPSException ex) {
            throw new COPSPepException("Error making Msg");
        }

        try {
            msg.writeData(_sock);
        } catch (final IOException e) {
            throw new COPSPepException("Failed to send the report, reason: " + e.getMessage());
        }
    }

    /**
     * Send Sync State Complete to PDP.
     *   The Synchronize State Complete is sent by the PEP to the PDP after
     *   the PDP sends a synchronize state request to the PEP and the PEP has
     *   finished synchronization.
     *
     * @throws   COPSPepException
     */
    public void sendSyncComplete() throws COPSPepException {
        logger.info("Sending sync complete");
        // Common Header with the same ClientType as the request
        final COPSHeader hdr = new COPSHeader (COPSHeader.COPS_OP_SSC, _clientType);

        // Client Handle with the same clientHandle as the request
        final COPSHandle clienthandle = _handle;

        final COPSSyncStateMsg msg = new COPSSyncStateMsg();
        try {
            msg.add(hdr);
            msg.add(clienthandle);
        } catch (Exception e) {
            throw new COPSPepException("Error making Msg");
        }

        try {
            msg.writeData(_sock);
        } catch (final IOException e) {
            throw new COPSPepException("Failed to send the sync state request, reason: " + e.getMessage());
        }
    }

    /**
     * Send Delete Request to PDP.
     * When sent from the PEP this message indicates to the remote PDP that
     * the state identified by the client handle is no longer
     * available/relevant.
     *
     * @throws   COPSPepException
     */
    public void sendDeleteRequest() throws COPSPepException {
        logger.info("Sending delete request");
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_DRQ, _clientType);
        final COPSHandle handle = _handle;

        // *** TODO: send a real reason
        final COPSReason reason = new COPSReason((short) 234, (short) 345);

        final COPSDeleteMsg msg = new COPSDeleteMsg();
        try {
            msg.add(hdr);
            msg.add(handle);
            msg.add(reason);
        } catch (COPSException ex) {
            throw new COPSPepException("Error making Msg");
        }
        try {
            msg.writeData(_sock);
        } catch (final IOException e) {
            throw new COPSPepException("Failed to send the delete request, reason: " + e.getMessage());
        }
    }
}




