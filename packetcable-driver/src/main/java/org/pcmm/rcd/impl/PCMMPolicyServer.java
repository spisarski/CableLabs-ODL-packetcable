/**
 * @header@
 */
package org.pcmm.rcd.impl;

import org.pcmm.PCMMConstants;
import org.pcmm.PCMMGlobalConfig;
import org.pcmm.PCMMProperties;
import org.pcmm.gates.*;
import org.pcmm.gates.IGateSpec.DSCPTOS;
import org.pcmm.gates.IGateSpec.Direction;
import org.pcmm.gates.impl.*;
import org.pcmm.messages.IMessage.MessageProperties;
import org.pcmm.messages.impl.MessageFactory;
import org.pcmm.objects.MMVersionInfo;
import org.pcmm.rcd.IPCMMPolicyServer;
import org.pcmm.utils.PCMMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.prpdp.COPSPdpConnection;
import org.umu.cops.prpdp.COPSPdpDataProcess;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * 
 * PCMM policy server
 * 
 */
public class PCMMPolicyServer extends AbstractPCMMServer implements IPCMMPolicyServer {

    private final static Logger logger = LoggerFactory.getLogger(PCMMPolicyServer.class);

	/**
	 * since PCMMPolicyServer can connect to multiple CMTS (PEP) we need to
	 * manage each connection in a separate thread.
	 */

	public PCMMPolicyServer() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.pcmm.rcd.IPCMMPolicyServer#requestCMTSConnection(java.lang.String)
	 */
	public IPSCMTSClient requestCMTSConnection(final String host) {
		try {
            final InetAddress address = InetAddress.getByName(host);
			return requestCMTSConnection(address);
		} catch (UnknownHostException e) {
			logger.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.pcmm.rcd.IPCMMPolicyServer#requestCMTSConnection(java.net.InetAddress
	 * )
	 */
	public IPSCMTSClient requestCMTSConnection(final InetAddress host) {
        logger.info("Requesting CMTS Connection");
        final PSCMTSClient client = new PSCMTSClient();
		try {
			if (client.tryConnect(host, PCMMProperties.get(PCMMConstants.PCMM_PORT, Integer.class))) {
				boolean endNegotiation = false;
				while (!endNegotiation) {
					logger.debug("waiting for OPN message from CMTS");
                    final COPSMsg opnMessage = client.readMessage();
					// Client-Close
					if (opnMessage.getHeader().isAClientClose()) {
                        if (opnMessage instanceof COPSClientCloseMsg) {
                            final COPSError error = ((COPSClientCloseMsg) opnMessage).getError();
                            logger.debug("CMTS requetsed Client-Close");
                            throw new PCMMException(new PCMMError(error.getErrCode(), error.getErrSubCode()));
                        } else {
                            logger.error("Message is not an instance of COPSClientCloseMsg");
                        }
					} else if (opnMessage.getHeader().isAClientOpen()) { // Client-Open
						logger.debug("OPN message received from CMTS");
                        if (opnMessage instanceof COPSClientOpenMsg) {
                            final COPSClientOpenMsg opn = (COPSClientOpenMsg) opnMessage;
                            if (opn.getClientSI() == null)
                                throw new COPSException("CMTS shoud have sent MM version info in Client-Open message");
                            else {
                                // set the version info
                                final MMVersionInfo vInfo = new MMVersionInfo(opn.getClientSI().getData().getData());
                                client.setVersionInfo(vInfo);
                                logger.debug("CMTS sent MMVersion info : major:" + vInfo.getMajorVersionNB() + "  minor:" + vInfo.getMinorVersionNB()); //
                                if (client.getVersionInfo().getMajorVersionNB() == client.getVersionInfo().getMinorVersionNB()) {
                                    // send a CC since CMTS has exhausted all
                                    // protocol selection attempts
                                    throw new COPSException("CMTS exhausted all protocol selection attempts");
                                }
                            }
                            // send CAT response
                            final Properties prop = new Properties();
                            logger.debug("send CAT to the CMTS ");
                            final COPSMsg catMsg = MessageFactory.getInstance().create(COPSHeader.COPS_OP_CAT, prop);
                            client.sendRequest(catMsg);
                            // wait for REQ msg
                            final COPSMsg reqMsg = client.readMessage();
                            // Client-Close
                            if (reqMsg.getHeader().isAClientClose()) {
                                // This will never occur please see if instanceof above
                                if (reqMsg instanceof COPSClientCloseMsg) {
                                    final COPSError error = ((COPSClientCloseMsg) reqMsg).getError();
                                    logger.debug("CMTS requetsed Client-Close");
                                    throw new PCMMException(new PCMMError(error.getErrCode(), error.getErrSubCode()));
                                } else {
                                    logger.error("Message is not of type COPSClientCloseMsg");
                                    throw new RuntimeException("Unable process Client close");
                                }
                            } else // Request
                                if (reqMsg.getHeader().isARequest()) {
                                    logger.debug("Received REQ message form CMTS");
                                    // end connection attempts
                                    if (reqMsg instanceof COPSReqMsg) {
                                        final COPSReqMsg req = (COPSReqMsg) reqMsg;
                                        // set the client handle to be used later by the
                                        // gate-set
                                        client.setClientHandle(req.getClientHandle().getId().str());

                                        if (catMsg instanceof COPSClientAcceptMsg) {
                                            final COPSPdpDataProcess processor = null;
                                            final COPSPdpConnection copsPdpConnection =
                                                    new COPSPdpConnection(opn.getPepId(), client.getSocket(), processor,
                                                            ((COPSClientAcceptMsg) catMsg).getKATimer().getTimerVal());
                                            pool.schedule(pool.adapt(copsPdpConnection));
                                        } else {
                                            logger.error("Message is not of instance COPSClientAcceptMsg");
                                        }
                                    } else {
                                        logger.error("Message not of type COPSReqMsg");
                                    }
                                    endNegotiation = true;
                                } else
                                    throw new COPSException("Can't understand request");
                        } else {
                            logger.error("Message is not an instance of COPSClientOpenMsg");
                        }
					} else {
						throw new COPSException("Can't understand request");
					}
				}
			}
			// else raise exception.
		} catch (final Exception e) {
			logger.error(e.getMessage());
			// no need to keep connection.
			client.disconnect();
			return null;
		}
		return client;
	}

	@Override
	protected IPCMMClientHandler getPCMMClientHandler(final Socket socket) {
        logger.info("Requesting the PCMM client handler");
		// TODO - Implement me
		return null;
	}

	/**
	 * 
	 * @see {@link IPSCMTSClient}
	 */
	static class PSCMTSClient extends AbstractPCMMClient implements IPSCMTSClient {
		/**
		 * Transaction id is
		 */
		private transient short transactionID;
		private final short classifierID;
		private transient int gateID;

		public PSCMTSClient() {
			super();
            // TODO - determine how this value should be set
            classifierID = 0;
			logger.info("Client " + getClass() + hashCode() + " created and started");
		}

		public PSCMTSClient(final Socket socket) {
            this();
			setSocket(socket);
		}

        @Override
		public boolean gateSet() {
			logger.debug("Sending Gate-Set message");
			if (!isConnected())
				throw new IllegalArgumentException("Not connected");
			// XXX check if other values should be provided
			//
            final ITrafficProfile trafficProfile = buildTrafficProfile();
			// PCMMGlobalConfig.DefaultBestEffortTrafficRate);
            final ITransactionID trID = new TransactionID();
			// set transaction ID to gate set
			trID.setGateCommandType(ITransactionID.GateSet);
			transactionID = (transactionID == 0 ? (short) (Math.random() * hashCode()) : transactionID);
			trID.setTransactionIdentifier(transactionID);
			// AMID
            final IAMID amid = getAMID();
			// GATE SPEC
            final IGateSpec gateSpec = getGateSpec();
            final ISubscriberID subscriberID = new SubscriberID();
			// Classifier if MM version <4, Extended Classifier else
            final IClassifier eclassifier = getClassifier(subscriberID);

            final IPCMMGate gate = new PCMMGateReq();
			gate.setTransactionID(trID);
			gate.setAMID(amid);
			gate.setSubscriberID(subscriberID);
			gate.setGateSpec(gateSpec);
			gate.setTrafficProfile(trafficProfile);
			gate.setClassifier(eclassifier);
			byte[] data = gate.getData();

			// configure message properties
            final Properties prop = new Properties();
			prop.put(MessageProperties.CLIENT_HANDLE, getClientHandle());
			prop.put(MessageProperties.DECISION_CMD_CODE, COPSDecision.DEC_INSTALL);
			prop.put(MessageProperties.DECISION_FLAG, (short) COPSDecision.DEC_NULL);
			prop.put(MessageProperties.GATE_CONTROL, new COPSData(data, 0, data.length));
            final COPSMsg decisionMsg = MessageFactory.getInstance().create(COPSHeader.COPS_OP_DEC, prop);
			// ** Send the GateSet Decision
			// **
			sendRequest(decisionMsg);
			// TODO check on this ?
			// waits for the gate-set-ack or error
            final COPSMsg responseMsg = readMessage();
			if (responseMsg.getHeader().isAReport()) {
				logger.info("processing received report from CMTS");
                final COPSReportMsg reportMsg = (COPSReportMsg) responseMsg;
				if (reportMsg.getClientSI().size() == 0) {
					logger.debug("CMTS responded with an empty SI");
					return false;
				}
                final COPSClientSI clientSI = reportMsg.getClientSI().get(0);
                final IPCMMGate responseGate = new PCMMGateReq(clientSI.getData().getData());
                final IPCMMError error = responseGate.getError();
				if (error != null) {
					logger.error(error.toString());
					return false;
				}
				logger.info("the CMTS has sent TransactionID :"+responseGate.getTransactionID());
				if (responseGate.getTransactionID() != null && responseGate.getTransactionID().getGateCommandType() == ITransactionID.GateSetAck) {
					logger.info("the CMTS has sent a Gate-Set-Ack response");
					// here CMTS responded that he acknowledged the Gate-Set
					// TODO do further check of Gate-Set-Ack GateID etc...
					gateID = responseGate.getGateID().getGateID();
					return true;
				} else {
					return false;
				}
			}
			return false;
		}

		@Override
		public boolean gateDelete() {
            logger.info("Deleting gate");
			if (!isConnected()) {
				logger.error("Not connected");
				return false;
			}
            final ITransactionID trID = new TransactionID();
			// set transaction ID to gate set
			trID.setGateCommandType(ITransactionID.GateDelete);
			trID.setTransactionIdentifier(transactionID);
			// AMID
            final IAMID amid = getAMID();
			// GATE SPEC
            final ISubscriberID subscriberID = new SubscriberID();
			try {
				subscriberID.setSourceIPAddress(InetAddress.getLocalHost());
			} catch (UnknownHostException e1) {
				logger.error(e1.getMessage());
			}

            final IGateID gateIdObj = new GateID();
			gateIdObj.setGateID(gateID);

            final IPCMMGate gate = new PCMMGateReq();
			gate.setTransactionID(trID);
			gate.setAMID(amid);
			gate.setSubscriberID(subscriberID);
			gate.setGateID(gateIdObj);

			// configure message properties
            final Properties prop = new Properties();
			prop.put(MessageProperties.CLIENT_HANDLE, getClientHandle());
			prop.put(MessageProperties.DECISION_CMD_CODE, COPSDecision.DEC_INSTALL);
			prop.put(MessageProperties.DECISION_FLAG, (short) COPSDecision.DEC_NULL);
			byte[] data = gate.getData();
			prop.put(MessageProperties.GATE_CONTROL, new COPSData(data, 0, data.length));
			COPSMsg decisionMsg = MessageFactory.getInstance().create(COPSHeader.COPS_OP_DEC, prop);
			// ** Send the GateSet Decision
			// **
			try {
				decisionMsg.writeData(getSocket());
			} catch (IOException e) {
				logger.error("Failed to send the decision, reason: " + e.getMessage());
				return false;
			}
			// waits for the gate-delete-ack or error
            final COPSMsg responseMsg = readMessage();
			if (responseMsg.getHeader().isAReport()) {
				logger.info("processing received report from CMTS");
                final COPSReportMsg reportMsg = (COPSReportMsg) responseMsg;
				if (reportMsg.getClientSI().size() == 0) {
					return false;
				}
                final COPSClientSI clientSI = reportMsg.getClientSI().get(0);
                final IPCMMGate responseGate = new PCMMGateReq(clientSI.getData().getData());
                final IPCMMError error = responseGate.getError();
				if (error != null) {
					logger.error(error.toString());
					return false;
				}
				// here CMTS responded that he acknowledged the Gate-delete
				// message
                final ITransactionID responseTransactionID = responseGate.getTransactionID();
				if (responseTransactionID != null && responseTransactionID.getGateCommandType() == ITransactionID.GateDeleteAck) {
					// TODO check : Is this test needed ??
					if (responseGate.getGateID().getGateID() == gateID && responseTransactionID.getTransactionIdentifier() == transactionID) {
						logger.info("the CMTS has sent a Gate-Delete-Ack response");
						return true;
					}
				}

			}
			return false;
		}

		@Override
		public boolean gateInfo() {
            logger.info("Gate info");
			if (!isConnected()) {
				logger.error("Not connected");
				return false;
			}
            final ITransactionID trID = new TransactionID();
			// set transaction ID to gate set
			trID.setGateCommandType(ITransactionID.GateInfo);
			trID.setTransactionIdentifier(transactionID);
			// AMID
            final IAMID amid = getAMID();
			// GATE SPEC
            final ISubscriberID subscriberID = new SubscriberID();
			try {
				subscriberID.setSourceIPAddress(InetAddress.getLocalHost());
			} catch (UnknownHostException e1) {
				logger.error(e1.getMessage());
			}
            final IGateID gateIdObj = new GateID();
			gateIdObj.setGateID(gateID);

            final IPCMMGate gate = new PCMMGateReq();
			gate.setTransactionID(trID);
			gate.setAMID(amid);
			gate.setSubscriberID(subscriberID);
			gate.setGateID(gateIdObj);

			// configure message properties
            final Properties prop = new Properties();
			prop.put(MessageProperties.CLIENT_HANDLE, getClientHandle());
			prop.put(MessageProperties.DECISION_CMD_CODE, COPSDecision.DEC_INSTALL);
			prop.put(MessageProperties.DECISION_FLAG, (short) COPSDecision.DEC_NULL);
            final byte[] data = gate.getData();
			prop.put(MessageProperties.GATE_CONTROL, new COPSData(data, 0, data.length));
            final COPSMsg decisionMsg = MessageFactory.getInstance().create(COPSHeader.COPS_OP_DEC, prop);
			// ** Send the GateSet Decision
			// **
			try {
				decisionMsg.writeData(getSocket());
			} catch (IOException e) {
				logger.error("Failed to send the decision, reason: " + e.getMessage());
				return false;
			}
			// waits for the gate-Info-ack or error
            final COPSMsg responseMsg = readMessage();
			if (responseMsg.getHeader().isAReport()) {
				logger.info("processing received report from CMTS");
                final COPSReportMsg reportMsg = (COPSReportMsg) responseMsg;
				if (reportMsg.getClientSI().size() == 0) {
					return false;
				}
                final COPSClientSI clientSI = reportMsg.getClientSI().get(0);
                final IPCMMGate responseGate = new PCMMGateReq(clientSI.getData().getData());
                final IPCMMError error = responseGate.getError();
                final ITransactionID responseTransactionID = responseGate.getTransactionID();
				if (error != null) {
					logger.debug(responseTransactionID != null ? responseTransactionID.toString() : "returned Transaction ID is null");
					logger.error(error.toString());
					return false;
				}
				// here CMTS responded that he acknowledged the Gate-Info
				// message
				/*
				 * <Gate-Info-Ack> = <ClientSI Header> <TransactionID> <AMID>
				 * <SubscriberID> <GateID> [<Event Generation Info>] <Gate-Spec>
				 * <classifier> <classifier...>] <Traffic Profile> <Gate Time
				 * Info> <Gate Usage Info> [<Volume-Based Usage Limit>] [<PSID>]
				 * [<Msg-Receipt-Key>] [<UserID>] [<Time-Based Usage Limit>]
				 * [<Opaque Data>] <GateState> [<SharedResourceID>]
				 */
				if (responseTransactionID != null && responseTransactionID.getGateCommandType() == ITransactionID.GateInfoAck) {
					// TODO need to implement missing data wrapper
					logger.info("TransactionID : " + responseTransactionID.toString());
					logger.info("AMID :" + String.valueOf(responseGate.getAMID()));
					logger.info("SubscriberID :" + String.valueOf(responseGate.getSubscriberID()));
					logger.info("Traffic Profile :" + String.valueOf(responseGate.getTrafficProfile()));
					logger.info("Gate Time Info :");
					logger.info("Gate Usage Info :");
					logger.info("GateState :");
					return true;
				}

			}
			return false;
		}

		@Override
		public boolean gateSynchronize() {
            logger.info("Gate synchronize");
			if (!isConnected()) {
				logger.error("Not connected");
				return false;
			}
            final ITransactionID trID = new TransactionID();
			// set transaction ID to gate set
			trID.setGateCommandType(ITransactionID.SynchRequest);
			trID.setTransactionIdentifier(transactionID);
			// AMID
            final IAMID amid = getAMID();
			// GATE SPEC
            final ISubscriberID subscriberID = new SubscriberID();
			try {
				subscriberID.setSourceIPAddress(InetAddress.getLocalHost());
			} catch (UnknownHostException e1) {
				logger.error(e1.getMessage());
			}
            final IGateID gateIdObj = new GateID();
			gateIdObj.setGateID(gateID);

            final IPCMMGate gate = new PCMMGateReq();
			gate.setTransactionID(trID);
			gate.setAMID(amid);
			gate.setSubscriberID(subscriberID);
			gate.setGateID(gateIdObj);

			// configure message properties
            final Properties prop = new Properties();
			prop.put(MessageProperties.CLIENT_HANDLE, getClientHandle());
			prop.put(MessageProperties.DECISION_CMD_CODE, COPSDecision.DEC_INSTALL);
			prop.put(MessageProperties.DECISION_FLAG, (short) COPSDecision.DEC_NULL);
            final byte[] data = gate.getData();
			prop.put(MessageProperties.GATE_CONTROL, new COPSData(data, 0, data.length));
            final COPSMsg decisionMsg = MessageFactory.getInstance().create(COPSHeader.COPS_OP_DEC, prop);
			// ** Send the GateSet Decision
			// **
			try {
				decisionMsg.writeData(getSocket());
			} catch (IOException e) {
				logger.error("Failed to send the decision, reason: " + e.getMessage());
				return false;
			}
			// waits for the gate-Info-ack or error
            final COPSMsg responseMsg = readMessage();
			if (responseMsg.getHeader().isAReport()) {
				logger.info("processing received report from CMTS");
                final COPSReportMsg reportMsg = (COPSReportMsg) responseMsg;
				if (reportMsg.getClientSI().size() == 0) {
					return false;
				}
                final COPSClientSI clientSI = reportMsg.getClientSI().get(0);
                final IPCMMGate responseGate = new PCMMGateReq(clientSI.getData().getData());
                final IPCMMError error = responseGate.getError();
                final ITransactionID responseTransactionID = responseGate.getTransactionID();
				if (error != null) {
					logger.debug(responseTransactionID != null ? responseTransactionID.toString() : "returned Transaction ID is null");
					logger.error(error.toString());
					return false;
				}
				// here CMTS responded that he acknowledged the Gate-Info
				// message
				/*
				 * <Gate-Info-Ack> = <ClientSI Header> <TransactionID> <AMID>
				 * <SubscriberID> <GateID> [<Event Generation Info>] <Gate-Spec>
				 * <classifier> <classifier...>] <Traffic Profile> <Gate Time
				 * Info> <Gate Usage Info> [<Volume-Based Usage Limit>] [<PSID>]
				 * [<Msg-Receipt-Key>] [<UserID>] [<Time-Based Usage Limit>]
				 * [<Opaque Data>] <GateState> [<SharedResourceID>]
				 */
				if (responseTransactionID != null && responseTransactionID.getGateCommandType() == ITransactionID.SynchReport) {
					// TODO need to implement missing data wrapper
					logger.info("TransactionID : " + responseTransactionID.toString());
					logger.info("AMID :" + String.valueOf(responseGate.getAMID()));
					logger.info("SubscriberID :" + String.valueOf(responseGate.getSubscriberID()));
					logger.info("Traffic Profile :" + String.valueOf(responseGate.getTrafficProfile()));
					logger.info("Gate Time Info :");
					logger.info("Gate Usage Info :");
					logger.info("GateState :");
					return true;
				}

			}
			return false;
		}

		private IAMID getAMID() {
            final IAMID amid = new AMID();
			amid.setApplicationType((short) 1);
			amid.setApplicationMgrTag((short) 1);
			return amid;
		}

		private IClassifier getClassifier(final ISubscriberID subscriberID) {
			// if the version major is less than 4 we need to use Classifier
			if (getVersionInfo().getMajorVersionNB() >= 4) {
				final ExtendedClassifier classifier = new ExtendedClassifier();
				// eclassifier.setProtocol(IClassifier.Protocol.NONE);
				classifier.setProtocol(IClassifier.Protocol.TCP);
				try {
					InetAddress subIP = InetAddress.getByName(PCMMGlobalConfig.SubscriberID);
					InetAddress srcIP = InetAddress.getByName(PCMMGlobalConfig.srcIP);
					InetAddress dstIP = InetAddress.getByName(PCMMGlobalConfig.dstIP);
					InetAddress mask = InetAddress.getByName(PCMMProperties.get(PCMMConstants.DEFAULT_MASK, String.class));
					subscriberID.setSourceIPAddress(subIP);
					classifier.setSourceIPAddress(srcIP);
					classifier.setDestinationIPAddress(dstIP);
					classifier.setIPDestinationMask(mask);
					classifier.setIPSourceMask(mask);
				} catch (final UnknownHostException unae) {
					logger.error("Error getByName", unae);
				}
				classifier.setSourcePortStart(PCMMGlobalConfig.srcPort);
				classifier.setSourcePortEnd(PCMMGlobalConfig.srcPort);
				classifier.setDestinationPortStart(PCMMGlobalConfig.dstPort);
				classifier.setDestinationPortEnd(PCMMGlobalConfig.dstPort);
				classifier.setActivationState((byte) 0x01);
				/*
				 * check if we have a stored value of classifierID else we just
				 * create one eclassifier.setClassifierID((short) 0x01);
				 */
				classifier.setClassifierID((short) (classifierID == 0 ? Math.random() * hashCode() : classifierID));
				// XXX - testie
				// eclassifier.setClassifierID((short) 1);
				classifier.setAction((byte) 0x00);
				// XXX - temp default until Gate Modify is hacked in
				// eclassifier.setPriority(PCMMGlobalConfig.EClassifierPriority);
				classifier.setPriority((byte) 65);
                return classifier;
			} else {
                final Classifier classifier = new Classifier();
				classifier.setProtocol(IClassifier.Protocol.TCP);
				try {
					InetAddress subIP = InetAddress.getByName(PCMMGlobalConfig.SubscriberID);
					InetAddress srcIP = InetAddress.getByName(PCMMGlobalConfig.srcIP);
					InetAddress dstIP = InetAddress.getByName(PCMMGlobalConfig.dstIP);
					subscriberID.setSourceIPAddress(subIP);
					classifier.setSourceIPAddress(srcIP);
					classifier.setDestinationIPAddress(dstIP);
				} catch (final UnknownHostException unae) {
                    logger.error("Error getByName", unae);
				}
				classifier.setSourcePort(PCMMGlobalConfig.srcPort);
				classifier.setDestinationPort(PCMMGlobalConfig.dstPort);
                return classifier;
			}
		}

		/**
		 * 
		 * @return GateSpec object
		 */
		private IGateSpec getGateSpec() {
			final IGateSpec gateSpec = new GateSpec();
			gateSpec.setDirection(Direction.UPSTREAM);
			gateSpec.setDSCP_TOSOverwrite(DSCPTOS.OVERRIDE);
			gateSpec.setTimerT1(PCMMGlobalConfig.GateT1);
			gateSpec.setTimerT2(PCMMGlobalConfig.GateT2);
			gateSpec.setTimerT3(PCMMGlobalConfig.GateT3);
			gateSpec.setTimerT4(PCMMGlobalConfig.GateT4);
			return gateSpec;
		}

		/**
		 * creates a traffic profile with 3 envelops (Authorized, Reserved and
		 * Committed).
		 * 
		 * @return Traffic profile
		 */
		private ITrafficProfile buildTrafficProfile() {
            final BestEffortService trafficProfile = new BestEffortService(BestEffortService.DEFAULT_ENVELOP);
			trafficProfile.getAuthorizedEnvelop().setTrafficPriority(BestEffortService.DEFAULT_TRAFFIC_PRIORITY);
			trafficProfile.getAuthorizedEnvelop().setMaximumTrafficBurst(BestEffortService.DEFAULT_MAX_TRAFFIC_BURST);
			trafficProfile.getAuthorizedEnvelop().setRequestTransmissionPolicy(PCMMGlobalConfig.BETransmissionPolicy);
			trafficProfile.getAuthorizedEnvelop().setMaximumSustainedTrafficRate(PCMMGlobalConfig.DefaultLowBestEffortTrafficRate);
			// PCMMGlobalConfig.DefaultBestEffortTrafficRate);

			trafficProfile.getReservedEnvelop().setTrafficPriority(BestEffortService.DEFAULT_TRAFFIC_PRIORITY);
			trafficProfile.getReservedEnvelop().setMaximumTrafficBurst(BestEffortService.DEFAULT_MAX_TRAFFIC_BURST);
			trafficProfile.getReservedEnvelop().setRequestTransmissionPolicy(PCMMGlobalConfig.BETransmissionPolicy);
			trafficProfile.getReservedEnvelop().setMaximumSustainedTrafficRate(PCMMGlobalConfig.DefaultLowBestEffortTrafficRate);
			// PCMMGlobalConfig.DefaultBestEffortTrafficRate);

			trafficProfile.getCommittedEnvelop().setTrafficPriority(BestEffortService.DEFAULT_TRAFFIC_PRIORITY);
			trafficProfile.getCommittedEnvelop().setMaximumTrafficBurst(BestEffortService.DEFAULT_MAX_TRAFFIC_BURST);
			trafficProfile.getCommittedEnvelop().setRequestTransmissionPolicy(PCMMGlobalConfig.BETransmissionPolicy);
			trafficProfile.getCommittedEnvelop().setMaximumSustainedTrafficRate(PCMMGlobalConfig.DefaultLowBestEffortTrafficRate);
			return trafficProfile;
		}

		@Override
		public short getClassifierId() {
			return classifierID;
		}

		@Override
		public short getTransactionId() {
			return transactionID;
		}

		@Override
		public int getGateId() {
			return gateID;
		}
	}

}
