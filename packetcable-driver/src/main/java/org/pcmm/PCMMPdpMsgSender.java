/**
 @header@
 */

package org.pcmm;

import org.pcmm.gates.*;
import org.pcmm.gates.IGateSpec.DSCPTOS;
import org.pcmm.gates.IGateSpec.Direction;
import org.pcmm.gates.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.prpdp.COPSPdpException;
import org.umu.cops.prpdp.COPSPdpMsgSender;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

//temp
//pcmm
/*
 * Example of an UNSOLICITED decision
 *
 * <Gate Control Command> = <COPS Common Header> <Client Handle> <Context> <Decision Flags> <ClientSI Data>
 *
 * <ClientSI Data> = <Gate-Set> | <Gate-Info> | <Gate-Delete> |
 *                   <PDP-Config> | <Synch-Request> | <Msg-Receipt>
 * <Gate-Set>      = <Decision Header> <TransactionID> <AMID> <SubscriberID> [<GateID>] <GateSpec>
 *                   <Traffic Profile> <classifier> [<classifier...>] [<Event Generation Info>]
 *                   [<Volume-Based Usage Limit>] [<Time-Based Usage Limit>][<Opaque Data>] [<UserID>]
 *
 * COPS message transceiver class for provisioning connections at the PDP side.
 *
 * TODO - Methods in this class could use some refactoring
 */
public class PCMMPdpMsgSender extends COPSPdpMsgSender {

    private final static Logger logger = LoggerFactory.getLogger(PCMMPdpMsgSender.class);

    protected transient short _transactionID;
    protected transient short _classifierID;
    // XXX - this does not need to be here
    protected int _gateID;

    /**
     * Creates a PCMMPdpMsgSender
     *
     * @param clientType   - COPS client-type
     * @param clientHandle - Client handle
     * @param sock         - Socket to the PEP
     */
    public PCMMPdpMsgSender(final short clientType, final COPSHandle clientHandle, final Socket sock) {
        this(clientType, (short) 0, clientHandle, sock);
    }

    public PCMMPdpMsgSender(final short clientType, final short tID, final COPSHandle clientHandle, final Socket sock) {
        super(clientType, clientHandle, sock);
        _transactionID = tID;
        _classifierID = 0;
    }

    /**
     * Sends a PCMM GateSet COPS Decision message
     *
     * @param gate - the gate
     * @throws COPSPdpException
     */
    public void sendGateSet(final IPCMMGate gate) throws COPSPdpException {
        logger.info("Sending gate set");
        // Common Header with the same ClientType as the request

        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_DEC, getClientType());

        // Client Handle with the same clientHandle as the request
        final COPSHandle handle = new COPSHandle(getClientHandle().getId());
        final COPSDecisionMsg decisionMsg = new COPSDecisionMsg();
        final ITransactionID trID = new TransactionID();

        // set transaction ID to gate set
        trID.setGateCommandType(ITransactionID.GateSet);
        _transactionID = (_transactionID == 0 ? (short) (Math.random() * hashCode()) : _transactionID);
        trID.setTransactionIdentifier(_transactionID);

        gate.setTransactionID(trID);


        // new pcmm specific clientsi
        final COPSClientSI clientSD = new COPSClientSI(COPSObjHeader.COPS_DEC, (byte) 4);
        byte[] data = gate.getData();
        clientSD.setData(new COPSData(data, 0, data.length));
        try {
            decisionMsg.add(hdr);
            decisionMsg.add(handle);
            // Decisions (no flags supplied)
            // <Context>
            final COPSContext cntxt = new COPSContext(COPSContext.CONFIG, (short) 0);
            final COPSDecision install = new COPSDecision();
            install.setCmdCode(COPSDecision.DEC_INSTALL);
            install.setFlags(COPSDecision.F_REQERROR);
            decisionMsg.addDecision(install, cntxt);
            decisionMsg.add(clientSD); // setting up the gate
            /* TODO - determine why this block has been commented out
                        try {
                            decisionMsg.dump(System.out);
                        } catch (IOException unae) {
                            System.out.println("Error dumping " + unae.getMessage());
                        }
            */

        } catch (COPSException e) {
            logger.error("Error making the decision message", e);
        }

        // ** Send the GateSet Decision
        // **
        try {
            decisionMsg.writeData(_sock);
        } catch (IOException e) {
            logger.error("Failed to send the decision.", e);
        }

    }

    /**
     * Sends a PCMM GateSet COPS Decision message
     *
     * @param num - the gate number
     * @throws COPSPdpException
     */
    public void sendGateSetDemo(final int num) throws COPSPdpException {
        logger.info("Sending gate set demo");
        // Common Header with the same ClientType as the request

        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_DEC, getClientType());

        // Client Handle with the same clientHandle as the request
        final COPSDecisionMsg decisionMsg = new COPSDecisionMsg();

        final IPCMMGate gate = new PCMMGateReq();
        final ITransactionID trID = new TransactionID();

        final IAMID amid = new AMID();
        final ISubscriberID subscriberID = new SubscriberID();
        final IGateSpec gateSpec = new GateSpec();
        final IClassifier classifier = new Classifier();
        final IExtendedClassifier eclassifier = new ExtendedClassifier();
        final int trafficRate;

        if (num == 1) {
            trafficRate = PCMMGlobalConfig.DefaultBestEffortTrafficRate;
        } else {
            trafficRate = PCMMGlobalConfig.DefaultLowBestEffortTrafficRate;
        }

        final BestEffortService trafficProfile = new BestEffortService((byte) 7); //BestEffortService.DEFAULT_ENVELOP);
        trafficProfile.getAuthorizedEnvelop()
                .setTrafficPriority(BestEffortService.DEFAULT_TRAFFIC_PRIORITY);
        trafficProfile.getAuthorizedEnvelop()
                .setMaximumTrafficBurst(BestEffortService.DEFAULT_MAX_TRAFFIC_BURST);
        trafficProfile.getAuthorizedEnvelop()
                .setRequestTransmissionPolicy(PCMMGlobalConfig.BETransmissionPolicy);
        trafficProfile.getAuthorizedEnvelop().setMaximumSustainedTrafficRate(trafficRate);
        //  PCMMGlobalConfig.DefaultLowBestEffortTrafficRate );
        //  PCMMGlobalConfig.DefaultBestEffortTrafficRate);

        trafficProfile.getReservedEnvelop()
                .setTrafficPriority(BestEffortService.DEFAULT_TRAFFIC_PRIORITY);
        trafficProfile.getReservedEnvelop()
                .setMaximumTrafficBurst(BestEffortService.DEFAULT_MAX_TRAFFIC_BURST);
        trafficProfile.getReservedEnvelop()
                .setRequestTransmissionPolicy(PCMMGlobalConfig.BETransmissionPolicy);
        trafficProfile.getReservedEnvelop().setMaximumSustainedTrafficRate(trafficRate);
        //  PCMMGlobalConfig.DefaultLowBestEffortTrafficRate );
        //  PCMMGlobalConfig.DefaultBestEffortTrafficRate);


        trafficProfile.getCommittedEnvelop()
                .setTrafficPriority(BestEffortService.DEFAULT_TRAFFIC_PRIORITY);
        trafficProfile.getCommittedEnvelop()
                .setMaximumTrafficBurst(BestEffortService.DEFAULT_MAX_TRAFFIC_BURST);
        trafficProfile.getCommittedEnvelop()
                .setRequestTransmissionPolicy(PCMMGlobalConfig.BETransmissionPolicy);
        trafficProfile.getCommittedEnvelop().setMaximumSustainedTrafficRate(trafficRate);
        //  PCMMGlobalConfig.DefaultLowBestEffortTrafficRate );
        //  PCMMGlobalConfig.DefaultBestEffortTrafficRate);


        // new pcmm specific clientsi
        final COPSClientSI clientSD = new COPSClientSI(COPSObjHeader.COPS_DEC, (byte) 4);

        final COPSHandle handle = new COPSHandle(getClientHandle().getId());

        // set transaction ID to gate set
        trID.setGateCommandType(ITransactionID.GateSet);
        _transactionID = (_transactionID == 0 ? (short) (Math.random() * hashCode()) : _transactionID);
        trID.setTransactionIdentifier(_transactionID);

        amid.setApplicationType((short) 1);
        amid.setApplicationMgrTag((short) 1);
        gateSpec.setDirection(Direction.UPSTREAM);
        gateSpec.setDSCP_TOSOverwrite(DSCPTOS.OVERRIDE);
        gateSpec.setTimerT1(PCMMGlobalConfig.GateT1);
        gateSpec.setTimerT2(PCMMGlobalConfig.GateT2);
        gateSpec.setTimerT3(PCMMGlobalConfig.GateT3);
        gateSpec.setTimerT4(PCMMGlobalConfig.GateT4);

        // XXX - if the version major is less than 4 we need to use Classifier
        // TODO - need to change to a real boolean condition
        if (true) {
            //eclassifier.setProtocol(IClassifier.Protocol.NONE);
            eclassifier.setProtocol(IClassifier.Protocol.TCP);
            try {
                InetAddress subIP = InetAddress.getByName(PCMMGlobalConfig.SubscriberID);
                InetAddress srcIP = InetAddress.getByName(PCMMGlobalConfig.srcIP);
                InetAddress dstIP = InetAddress.getByName(PCMMGlobalConfig.dstIP);
                InetAddress mask = InetAddress.getByName("0.0.0.0");
                subscriberID.setSourceIPAddress(subIP);
                eclassifier.setSourceIPAddress(srcIP);
                eclassifier.setDestinationIPAddress(dstIP);
                eclassifier.setIPDestinationMask(mask);
                eclassifier.setIPSourceMask(mask);
            } catch (UnknownHostException unae) {
                logger.error("Error getByName", unae);
            }
            eclassifier.setSourcePortStart(PCMMGlobalConfig.srcPort);
            eclassifier.setSourcePortEnd(PCMMGlobalConfig.srcPort);
            eclassifier.setDestinationPortStart(PCMMGlobalConfig.dstPort);
            eclassifier.setDestinationPortEnd(PCMMGlobalConfig.dstPort);
            eclassifier.setActivationState((byte) 0x01);
            // check if we have a stored value of classifierID else we just
            // create
            // one
            // eclassifier.setClassifierID((short) 0x01);
            eclassifier.setClassifierID((short) (_classifierID == 0 ? Math.random() * hashCode() : _classifierID));
            // XXX - testie
            // eclassifier.setClassifierID((short) 1);

            eclassifier.setAction((byte) 0x00);
            // XXX - temp default until Gate Modify is hacked in
            // eclassifier.setPriority(PCMMGlobalConfig.EClassifierPriority);
            eclassifier.setPriority((byte) 65);

        } else {
            classifier.setProtocol(IClassifier.Protocol.TCP);
            try {
                final InetAddress subIP = InetAddress.getByName(PCMMGlobalConfig.SubscriberID);
                final InetAddress srcIP = InetAddress.getByName(PCMMGlobalConfig.srcIP);
                final InetAddress dstIP = InetAddress.getByName(PCMMGlobalConfig.dstIP);
                subscriberID.setSourceIPAddress(subIP);
                classifier.setSourceIPAddress(srcIP);
                classifier.setDestinationIPAddress(dstIP);
            } catch (UnknownHostException unae) {
                logger.error("Error getByName", unae);
            }
            classifier.setSourcePort(PCMMGlobalConfig.srcPort);
            classifier.setDestinationPort(PCMMGlobalConfig.dstPort);
        }

        gate.setTransactionID(trID);
        gate.setAMID(amid);
        gate.setSubscriberID(subscriberID);
        gate.setGateSpec(gateSpec);
        gate.setTrafficProfile(trafficProfile);
        gate.setClassifier(eclassifier);

        final byte[] data = gate.getData();

        // new pcmm specific clientsi
        clientSD.setData(new COPSData(data, 0, data.length));
        try {
            decisionMsg.add(hdr);
            decisionMsg.add(handle);
            // Decisions (no flags supplied)
            // <Context>
            final COPSContext cntxt = new COPSContext(COPSContext.CONFIG, (short) 0);
            final COPSDecision install = new COPSDecision();
            install.setCmdCode(COPSDecision.DEC_INSTALL);
            install.setFlags(COPSDecision.F_REQERROR);
            decisionMsg.addDecision(install, cntxt);
            decisionMsg.add(clientSD); // setting up the gate
            /* TODO - determine why this block has been commented out
                        try {
                            decisionMsg.dump(System.out);
                        } catch (IOException unae) {
                            System.out.println("Error dumping " + unae.getMessage());
                        }
            */

        } catch (final COPSException e) {
            logger.error("Error making Msg", e);
        }

        // ** Send the GateSet Decision
        // **
        try {
            decisionMsg.writeData(_sock);
        } catch (final IOException e) {
            logger.error("Failed to send the decision.", e);
        }

    }

    /**
     * Sends a PCMM GateSet COPS Decision message
     * @throws COPSPdpException
     */
    public void sendGateSetBestEffortWithExtendedClassifier() throws COPSPdpException {
        // Common Header with the same ClientType as the request

        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_DEC, getClientType());

        // Client Handle with the same clientHandle as the request
        final COPSDecisionMsg decisionMsg = new COPSDecisionMsg();

        final IPCMMGate gate = new PCMMGateReq();
        final ITransactionID trID = new TransactionID();

        final IAMID amid = new AMID();
        final ISubscriberID subscriberID = new SubscriberID();
        final IGateSpec gateSpec = new GateSpec();
        final IClassifier classifier = new Classifier();
        final IExtendedClassifier eclassifier = new ExtendedClassifier();

        // XXX check if other values should be provided
        //
        final BestEffortService trafficProfile = new BestEffortService((byte) 7); //BestEffortService.DEFAULT_ENVELOP);
        trafficProfile.getAuthorizedEnvelop()
                .setTrafficPriority(BestEffortService.DEFAULT_TRAFFIC_PRIORITY);
        trafficProfile.getAuthorizedEnvelop()
                .setMaximumTrafficBurst(BestEffortService.DEFAULT_MAX_TRAFFIC_BURST);
        trafficProfile.getAuthorizedEnvelop()
                .setRequestTransmissionPolicy(PCMMGlobalConfig.BETransmissionPolicy);
        trafficProfile.getAuthorizedEnvelop()
                .setMaximumSustainedTrafficRate(PCMMGlobalConfig.DefaultLowBestEffortTrafficRate);
        //  PCMMGlobalConfig.DefaultBestEffortTrafficRate);

        trafficProfile.getReservedEnvelop()
                .setTrafficPriority(BestEffortService.DEFAULT_TRAFFIC_PRIORITY);
        trafficProfile.getReservedEnvelop()
                .setMaximumTrafficBurst(BestEffortService.DEFAULT_MAX_TRAFFIC_BURST);
        trafficProfile.getReservedEnvelop()
                .setRequestTransmissionPolicy(PCMMGlobalConfig.BETransmissionPolicy);
        trafficProfile.getReservedEnvelop()
                .setMaximumSustainedTrafficRate(PCMMGlobalConfig.DefaultLowBestEffortTrafficRate);
        //  PCMMGlobalConfig.DefaultBestEffortTrafficRate);


        trafficProfile.getCommittedEnvelop()
                .setTrafficPriority(BestEffortService.DEFAULT_TRAFFIC_PRIORITY);
        trafficProfile.getCommittedEnvelop()
                .setMaximumTrafficBurst(BestEffortService.DEFAULT_MAX_TRAFFIC_BURST);
        trafficProfile.getCommittedEnvelop()
                .setRequestTransmissionPolicy(PCMMGlobalConfig.BETransmissionPolicy);
        trafficProfile.getCommittedEnvelop()
                .setMaximumSustainedTrafficRate(PCMMGlobalConfig.DefaultLowBestEffortTrafficRate);
        //  PCMMGlobalConfig.DefaultBestEffortTrafficRate);


        // new pcmm specific clientsi
        final COPSClientSI clientSD = new COPSClientSI(COPSObjHeader.COPS_DEC, (byte) 4);

        final COPSHandle handle = new COPSHandle(getClientHandle().getId());

        // set transaction ID to gate set
        trID.setGateCommandType(ITransactionID.GateSet);
        _transactionID = (_transactionID == 0 ? (short) (Math.random() * hashCode()) : _transactionID);
        trID.setTransactionIdentifier(_transactionID);

        amid.setApplicationType((short) 1);
        amid.setApplicationMgrTag((short) 1);
        gateSpec.setDirection(Direction.UPSTREAM);
        gateSpec.setDSCP_TOSOverwrite(DSCPTOS.OVERRIDE);
        gateSpec.setTimerT1(PCMMGlobalConfig.GateT1);
        gateSpec.setTimerT2(PCMMGlobalConfig.GateT2);
        gateSpec.setTimerT3(PCMMGlobalConfig.GateT3);
        gateSpec.setTimerT4(PCMMGlobalConfig.GateT4);

        // XXX - if the version major is less than 4 we need to use Classifier
        // TODO - Need to use some variable here
        if (true) {
            //eclassifier.setProtocol(IClassifier.Protocol.NONE);
            eclassifier.setProtocol(IClassifier.Protocol.TCP);
            try {
                final InetAddress subIP = InetAddress.getByName(PCMMGlobalConfig.SubscriberID);
                final InetAddress srcIP = InetAddress.getByName(PCMMGlobalConfig.srcIP);
                final InetAddress dstIP = InetAddress.getByName(PCMMGlobalConfig.dstIP);
                final InetAddress mask = InetAddress.getByName("0.0.0.0");
                subscriberID.setSourceIPAddress(subIP);
                eclassifier.setSourceIPAddress(srcIP);
                eclassifier.setDestinationIPAddress(dstIP);
                eclassifier.setIPDestinationMask(mask);
                eclassifier.setIPSourceMask(mask);
            } catch (UnknownHostException unae) {
                logger.error("Error getByName", unae);
            }
            eclassifier.setSourcePortStart(PCMMGlobalConfig.srcPort);
            eclassifier.setSourcePortEnd(PCMMGlobalConfig.srcPort);
            eclassifier.setDestinationPortStart(PCMMGlobalConfig.dstPort);
            eclassifier.setDestinationPortEnd(PCMMGlobalConfig.dstPort);
            eclassifier.setActivationState((byte) 0x01);
            // check if we have a stored value of classifierID else we just
            // create
            // one
            // eclassifier.setClassifierID((short) 0x01);
            eclassifier.setClassifierID((short) (_classifierID == 0 ? Math.random() * hashCode() : _classifierID));
            // XXX - testie
            // eclassifier.setClassifierID((short) 1);

            eclassifier.setAction((byte) 0x00);
            // XXX - temp default until Gate Modify is hacked in
            // eclassifier.setPriority(PCMMGlobalConfig.EClassifierPriority);
            eclassifier.setPriority((byte) 65);

        } else {
            classifier.setProtocol(IClassifier.Protocol.TCP);
            try {
                final InetAddress subIP = InetAddress.getByName(PCMMGlobalConfig.SubscriberID);
                final InetAddress srcIP = InetAddress.getByName(PCMMGlobalConfig.srcIP);
                final InetAddress dstIP = InetAddress.getByName(PCMMGlobalConfig.dstIP);
                subscriberID.setSourceIPAddress(subIP);
                classifier.setSourceIPAddress(srcIP);
                classifier.setDestinationIPAddress(dstIP);
            } catch (UnknownHostException unae) {
                logger.error("Error getByName", unae);
            }
            classifier.setSourcePort(PCMMGlobalConfig.srcPort);
            classifier.setDestinationPort(PCMMGlobalConfig.dstPort);
        }

        gate.setTransactionID(trID);
        gate.setAMID(amid);
        gate.setSubscriberID(subscriberID);
        gate.setGateSpec(gateSpec);
        gate.setTrafficProfile(trafficProfile);
        gate.setClassifier(eclassifier);

        byte[] data = gate.getData();

        // new pcmm specific clientsi
        clientSD.setData(new COPSData(data, 0, data.length));
        try {
            decisionMsg.add(hdr);
            decisionMsg.add(handle);
            // Decisions (no flags supplied)
            // <Context>
            final COPSContext cntxt = new COPSContext(COPSContext.CONFIG, (short) 0);
            final COPSDecision install = new COPSDecision();
            install.setCmdCode(COPSDecision.DEC_INSTALL);
            install.setFlags(COPSDecision.F_REQERROR);
            decisionMsg.addDecision(install, cntxt);
            decisionMsg.add(clientSD); // setting up the gate
            /* TODO - determine why this block has been commented out
                        try {
                            decisionMsg.dump(System.out);
                        } catch (IOException unae) {
                            System.out.println("Error dumping " + unae.getMessage());
                        }
            */

        } catch (final COPSException e) {
            logger.error("Error making Msg", e);
        }

        // ** Send the GateSet Decision
        // **
        try {
            decisionMsg.writeData(_sock);
        } catch (final IOException e) {
            logger.error("Failed to send the decision.", e);
        }

    }


    public boolean handleGateReport(final Socket socket) throws COPSPdpException {
        try {
            // waits for the gate-set-ack or error
            final COPSMsg responseMsg = COPSTransceiver.receiveMsg(socket);
            if (responseMsg.getHeader().isAReport()) {
                logger.info("Processing received report from CMTS");
                final COPSReportMsg reportMsg = (COPSReportMsg) responseMsg;
                if (reportMsg.getClientSI().size() == 0) {
                    return false;
                }
                final COPSClientSI clientSI = reportMsg.getClientSI().get(0);
                final IPCMMGate responseGate = new PCMMGateReq(clientSI.getData().getData());
                if (responseGate.getTransactionID() != null &&
                        responseGate.getTransactionID().getGateCommandType() == ITransactionID.GateSetAck) {
                    logger.info("The CMTS has sent a Gate-Set-Ack response");
                    // here CMTS responded that he acknowledged the Gate-Set
                    // TODO do further check of Gate-Set-Ack GateID etc...
                    _gateID = responseGate.getGateID().getGateID();
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        } catch (Exception e) { // COPSException, IOException
            throw new COPSPdpException("Error COPSTransceiver.receiveMsg");
        }
    }


    /**
     * Sends a PCMM GateSet COPS Decision message
     * @throws COPSPdpException
     */
    public void sendGateSet() throws COPSPdpException {
        // Common Header with the same ClientType as the request

        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_DEC, getClientType());

        // Client Handle with the same clientHandle as the request
        final COPSDecisionMsg decisionMsg = new COPSDecisionMsg();

        final IPCMMGate gate = new PCMMGateReq();
        final ITransactionID trID = new TransactionID();

        final IAMID amid = new AMID();
        final ISubscriberID subscriberID = new SubscriberID();
        final IGateSpec gateSpec = new GateSpec();
        final IClassifier classifier = new Classifier();
        // XXX check if other values should be provided
        final BestEffortService trafficProfile = new BestEffortService(BestEffortService.DEFAULT_ENVELOP);
        trafficProfile.getAuthorizedEnvelop()
                .setTrafficPriority(BestEffortService.DEFAULT_TRAFFIC_PRIORITY);
        trafficProfile.getAuthorizedEnvelop()
                .setMaximumTrafficBurst(BestEffortService.DEFAULT_MAX_TRAFFIC_BURST);
        trafficProfile.getAuthorizedEnvelop()
                .setRequestTransmissionPolicy(PCMMGlobalConfig.BETransmissionPolicy);

        // new pcmm specific clientsi
        final COPSClientSI clientSD = new COPSClientSI(COPSObjHeader.COPS_DEC, (byte) 4);

        final COPSHandle handle = new COPSHandle(getClientHandle().getId());
        // byte[] content = "1234".getBytes();

        // handle.setId(new COPSData(content, 0, content.length));

        // set transaction ID to gate set
        trID.setGateCommandType(ITransactionID.GateSet);
        _transactionID = (_transactionID == 0 ? (short) (Math.random() * hashCode()) : _transactionID);
        trID.setTransactionIdentifier(_transactionID);

        amid.setApplicationType((short) 1);
        amid.setApplicationMgrTag((short) 1);
        gateSpec.setDirection(Direction.UPSTREAM);
        gateSpec.setDSCP_TOSOverwrite(DSCPTOS.OVERRIDE);
        gateSpec.setTimerT1(PCMMGlobalConfig.GateT1);
        gateSpec.setTimerT2(PCMMGlobalConfig.GateT2);
        gateSpec.setTimerT3(PCMMGlobalConfig.GateT3);
        gateSpec.setTimerT4(PCMMGlobalConfig.GateT4);

        /*
         * ((DOCSISServiceClassNameTrafficProfile) trafficProfile)
         * .setServiceClassName("S_up");
         */

        classifier.setProtocol(IClassifier.Protocol.TCP);
        try {
            final InetAddress subIP = InetAddress.getByName(PCMMGlobalConfig.SubscriberID);
            final InetAddress srcIP = InetAddress.getByName(PCMMGlobalConfig.srcIP);
            final InetAddress dstIP = InetAddress.getByName(PCMMGlobalConfig.dstIP);
            subscriberID.setSourceIPAddress(subIP);
            classifier.setSourceIPAddress(srcIP);
            classifier.setDestinationIPAddress(dstIP);
        } catch (UnknownHostException unae) {
            logger.error("Error getByName", unae);
        }
        classifier.setSourcePort(PCMMGlobalConfig.srcPort);
        classifier.setDestinationPort(PCMMGlobalConfig.dstPort);

        gate.setTransactionID(trID);
        gate.setAMID(amid);
        gate.setSubscriberID(subscriberID);
        gate.setGateSpec(gateSpec);
        gate.setTrafficProfile(trafficProfile);
        gate.setClassifier(classifier);

        final byte[] data = gate.getData();

        // new pcmm specific clientsi
        clientSD.setData(new COPSData(data, 0, data.length));

        try {
            decisionMsg.add(hdr);
            decisionMsg.add(handle);
            // Decisions (no flags supplied)
            // <Context>
            final COPSContext cntxt = new COPSContext(COPSContext.CONFIG, (short) 0);
            final COPSDecision install = new COPSDecision();
            install.setCmdCode(COPSDecision.DEC_INSTALL);
            install.setFlags(COPSDecision.F_REQERROR);
            decisionMsg.addDecision(install, cntxt);
            decisionMsg.add(clientSD); // setting up the gate
            /* TODO - determine why this block has been commented out
                        try {
                            decisionMsg.dump(System.out);
                        } catch (IOException unae) {
                            System.out.println("Error dumping " + unae.getMessage());
                        }
            */

        } catch (final COPSException e) {
            logger.error("Error making the decision message", e);
        }

        // ** Send the GateSet Decision
        // **
        try {
            decisionMsg.writeData(_sock);
        } catch (final IOException e) {
            logger.error("Failed to send the decision.", e);
        }

    }

    /**
     * Sends a message asking that the request state be deleted
     *
     * @throws COPSPdpException
     */
    public void sendGateDelete(int gID) throws COPSPdpException {
        /*
         * Example of an UNSOLICITED decision <Gate Control Command> = <COPS
         * Common Header> <Client Handle> <Context> <Decision Flags> <ClientSI
         * Data> <ClientSI Data> = <Gate-Set> | <Gate-Info> | <Gate-Delete> |
         * <PDP-Config> | <Synch-Request> | <Msg-Receipt> <Gate-Delete> =
         * <Decision Header> <TransactionID> <AMID> <SubscriberID> <GateID>
         */
        // Common Header with the same ClientType as the request
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_DEC, getClientType());

        // Client Handle with the same clientHandle as the request
        final COPSDecisionMsg decisionMsg = new COPSDecisionMsg();

        final IPCMMGate gate = new PCMMGateReq();
        final ITransactionID trID = new TransactionID();

        final IAMID amid = new AMID();
        final ISubscriberID subscriberID = new SubscriberID();
        final IGateID gateID = new GateID();

        // new pcmm specific clientsi
        final COPSClientSI clientSD = new COPSClientSI(COPSObjHeader.COPS_DEC, (byte) 4);
        final COPSHandle handle = new COPSHandle(getClientHandle().getId());

        // set transaction ID to gate set
        trID.setGateCommandType(ITransactionID.GateDelete);
        _transactionID = (_transactionID == 0 ? (short) (Math.random() * hashCode()) : _transactionID);
        trID.setTransactionIdentifier(_transactionID);

        amid.setApplicationType((short) 1);
        amid.setApplicationMgrTag((short) 1);
        gateID.setGateID(gID);

        try {
            subscriberID.setSourceIPAddress(InetAddress.getByName(PCMMGlobalConfig.SubscriberID));
        } catch (UnknownHostException unae) {
            logger.error("Unexpeced error retrieving the InetAddress", unae);
        }

        gate.setTransactionID(trID);
        gate.setAMID(amid);
        gate.setSubscriberID(subscriberID);
        gate.setGateID(gateID);

        // XXX - GateID
        byte[] data = gate.getData();

        // new pcmm specific clientsi
        clientSD.setData(new COPSData(data, 0, data.length));

        try {
            decisionMsg.add(hdr);
            decisionMsg.add(handle);
            // Decisions (no flags supplied)
            // <Context>
            final COPSContext cntxt = new COPSContext(COPSContext.CONFIG, (short) 0);
            final COPSDecision install = new COPSDecision();
            install.setCmdCode(COPSDecision.DEC_INSTALL);
            install.setFlags(COPSDecision.F_REQERROR);
            decisionMsg.addDecision(install, cntxt);
            decisionMsg.add(clientSD); // setting up the gate
            /* TODO - determine why this block has been commented out
                        try {
                            decisionMsg.dump(System.out);
                        } catch (IOException unae) {
                            System.out.println("Error dumping " + unae.getMessage());
                        }
            */

        } catch (final COPSException e) {
            logger.error("Error making the decision message", e);
        }

        // ** Send the GateDelete Decision
        // **
        try {
            decisionMsg.writeData(_sock);
            // decisionMsg.writeData(socket_id);
        } catch (final IOException e) {
            logger.error("Failed to send the decision.", e);
        }
    }

    /**
     * Sends a request asking that a new request state be created
     *
     * @throws COPSPdpException
     */
    public void sendOpenNewRequestState() throws COPSPdpException {
        /*
         * <Decision Message> ::= <Common Header: Flag UNSOLICITED> <Client
         * Handle> *(<Decision>) [<Integrity>] <Decision> ::= <Context>
         * <Decision: Flags> <Decision: Flags> ::= Install Request-State
         */

        // Common Header with the same ClientType as the request (default
        // UNSOLICITED)
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_DEC, getClientType());

        // Client Handle with the same clientHandle as the request
        final COPSHandle clienthandle = new COPSHandle(_handle.getId());

        // Decisions
        // <Context>
        final COPSContext cntxt = new COPSContext(COPSContext.CONFIG, (short) 0);
        // <Decision: Flags>
        final COPSDecision dec = new COPSDecision();
        dec.setCmdCode(COPSDecision.DEC_INSTALL);
        dec.setFlags(COPSDecision.F_REQSTATE);

        final COPSDecisionMsg decisionMsg = new COPSDecisionMsg();
        try {
            decisionMsg.add(hdr);
            decisionMsg.add(clienthandle);
            decisionMsg.addDecision(dec, cntxt);
        } catch (final COPSException e) {
            throw new COPSPdpException("Error making Msg");
        }

        try {
            decisionMsg.writeData(_sock);
        } catch (final IOException e) {
            throw new COPSPdpException("Failed to send the open new request state, reason: " + e.getMessage());
        }
    }

    /**
     * Sends a message asking for a COPS sync operation
     *
     * @throws COPSPdpException
     */
    public void sendGateInfo() throws COPSPdpException {
        /*
         * <Gate-Info> ::= <Common Header> [<Client Handle>] [<Integrity>]
         */

        // Common Header with the same ClientType as the request
        final COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_SSQ, getClientType());

        // Client Handle with the same clientHandle as the request
        final COPSHandle clienthandle = new COPSHandle(_handle.getId());

        final COPSSyncStateMsg msg = new COPSSyncStateMsg();
        try {
            msg.add(hdr);
            msg.add(clienthandle);
        } catch (final Exception e) {
            throw new COPSPdpException("Error making Msg");
        }

        try {
            msg.writeData(_sock);
        } catch (final IOException e) {
            throw new COPSPdpException("Failed to send the GateInfo request, reason: " + e.getMessage());
        }
    }

    /**
     * Sends a message asking for a COPS sync operation
     *
     * @throws COPSPdpException
     */
    public void sendSyncRequest() throws COPSPdpException {
        /*
         * <Synchronize State Request> ::= <Common Header> [<Client Handle>]
         * [<Integrity>]
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

    // XXX - Temp
    public void sendSyncRequestState() throws COPSPdpException {
    }

    // XXX - Temp
    public void sendDeleteRequestState() throws COPSPdpException {
    }
}
