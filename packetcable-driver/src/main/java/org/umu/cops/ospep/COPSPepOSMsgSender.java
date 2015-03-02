package org.umu.cops.ospep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.prpep.COPSMsgSender;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * COPS message transceiver class for outsourcing connections at the PEP side.
 */
public class COPSPepOSMsgSender extends COPSMsgSender {

    private static final Logger logger = LoggerFactory.getLogger(COPSPepOSMsgSender.class);

    /**
     * Creates a COPSPepMsgSender
     *
     * @param clientType        Client-type
     * @param clientHandle      Client handle
     * @param sock              Socket connected to the PDP
     */
    public COPSPepOSMsgSender(final short clientType, final COPSHandle clientHandle, final Socket sock) {
        super(clientType, clientHandle, sock);
        logger.info("Creating new message sender");
    }

    /**
     * Sends a request to the PDP.
     * The PEP establishes a request state client handle for which the
     * remote PDP may maintain state.
     * @param    clientSIs              Client data
     * @throws   COPSPepException
     */
    public void sendRequest(final List<COPSClientSI> clientSIs) throws COPSPepException {
        logger.info("-Sending request");
        // Create COPS Message
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_REQ, _clientType);
        final COPSContext cntxt = new COPSContext(COPSContext.CONFIG , (short) 0);
        final COPSReqMsg msg = new COPSReqMsg();
        try {
            msg.add(hdr);
            msg.add(_handle);
            msg.add(cntxt);

            for (final COPSClientSI clientSI : clientSIs) {
                msg.add(clientSI);
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
     * Sends a failure report to the PDP. This report message notifies the PDP
     * of failure when carrying out the PDP's decision, or when reporting
     *  an accounting related state change.
     * @param clientSIs Report data
     * @throws   COPSPepException
     */
    public void sendFailReport(final List<COPSClientSI> clientSIs) throws COPSException {
        logger.info("Sending fail report");
        sendReport(COPSReportType.FAILURE, clientSIs);
    }

    /**
     * Sends a success report to the PDP. This report message notifies the PDP
     * of success when carrying out the PDP's decision, or when reporting
     *  an accounting related state change.
     * @param   clientSIs   Report data
     * @throws  COPSPepException
     */
    public void sendSuccessReport(final List<COPSClientSI> clientSIs) throws COPSException {
        logger.info("Sending success report");
        sendReport(COPSReportType.SUCCESS, clientSIs);
    }

    /**
     * Sends an accounting report to the PDP
     * @param clientSIs Report data
     * @throws COPSPepException
     */
    public void sendAcctReport(final List<COPSClientSI> clientSIs) throws COPSException {
        logger.info("Sending accounting report");
        sendReport(COPSReportType.ACCT, clientSIs);
    }

    private void sendReport(final short type, final List<COPSClientSI> clientSIs) throws COPSException {
        final COPSReportMsg msg = new COPSReportMsg();
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_RPT, _clientType);
        final COPSHandle hnd = _handle;

        final COPSReportType report = new COPSReportType(type);

        msg.add(hdr);
        msg.add(hnd);
        msg.add(report);

        for (final COPSClientSI clientSI : clientSIs) {
            msg.add(clientSI);
        }

        try {
            msg.writeData(_sock);
        } catch (IOException e) {
            throw new COPSPepException("Failed to send the report, reason: " + e.getMessage());
        }
    }

    /**
     * Sends a sync-complete message to the PDP. This indicates the
     * end of a synchronization requested by the PDP.
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
        } catch (IOException e) {
            throw new COPSPepException("Failed to send the sync state request, reason: " + e.getMessage());
        }
    }

    /**
     * Sends a delete request to the PDP.
     * When sent from the PEP this message indicates to the remote PDP that
     * the state identified by the client handle is no longer
     * available/relevant.
     * @throws   COPSPepException
     */
    public void sendDeleteRequest() throws COPSPepException {
        logger.info("Sending delete request");
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_DRQ, _clientType);
        final COPSHandle handle = _handle;

        // *** TODO: use real reason codes
        final COPSReason reason = new COPSReason((short) 234, (short) 345);

        final COPSDeleteMsg msg = new COPSDeleteMsg();
        try {
            msg.add(hdr);
            msg.add(handle);
            msg.add(reason);
            msg.writeData(_sock);
        } catch (COPSException ex) {
            throw new COPSPepException("Error making Msg");
        } catch (IOException e) {
            throw new COPSPepException("Failed to send the delete request, reason: " + e.getMessage());
        }
    }

}
