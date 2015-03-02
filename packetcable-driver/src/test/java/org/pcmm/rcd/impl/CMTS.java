/**
 @header@
 */
package org.pcmm.rcd.impl;

import org.pcmm.gates.IPCMMGate;
import org.pcmm.gates.ITransactionID;
import org.pcmm.gates.impl.PCMMGateReq;
import org.pcmm.messages.impl.MessageFactory;
import org.pcmm.rcd.ICMTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.COPSReqStateMan;
import org.umu.cops.prpep.COPSPepConnection;
import org.umu.cops.prpep.COPSPepDataProcess;
import org.umu.cops.prpep.COPSPepException;
import org.umu.cops.prpep.COPSPepReqStateMan;
import org.umu.cops.stack.*;

import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * This class is meant to act as a mock CMTS emulator
 */
public class CMTS extends AbstractPCMMServer implements ICMTS {

    private final static Logger logger = LoggerFactory.getLogger(CMTS.class);

	@Override
	protected IPCMMClientHandler getPCMMClientHandler(final Socket socket) {

		return new AbstractPCMMClientHandler(socket) {

//			private String handle;

			public void run() {
				try {
					// send OPN message
					// set the major version info and minor version info to
					// default (5,0)
					logger.info("Send OPN message to the PS");

                    // TODO - Why are the properties empty? What is supposed to be populated here?
                    final Properties props = new Properties();
//                    props.put("foo", "bar");
					sendRequest(MessageFactory.getInstance().create(COPSHeader.COPS_OP_OPN, props));
					// wait for CAT
					final COPSMsg recvMsg = readMessage();

					if (recvMsg.getHeader().isAClientClose()) {
                        final COPSClientCloseMsg cMsg = (COPSClientCloseMsg) recvMsg;
						logger.info("PS requested Client-Close" + cMsg.getError().getDescription());
						// send a CC message and close the _socket
						disconnect();
						return;
					}
					if (recvMsg.getHeader().isAClientAccept()) {
						logger.info("received Client-Accept from PS");
                        final COPSClientAcceptMsg cMsg = (COPSClientAcceptMsg) recvMsg;
						// Support
						if (cMsg.getIntegrity() != null) {
							throw new COPSPepException("Unsupported object (Integrity)");
						}

						// Mandatory KATimer
                        final COPSKATimer kt = cMsg.getKATimer();
						if (kt == null)
							throw new COPSPepException("Mandatory COPS object missing (KA Timer)");
						final short kaTimeVal = kt.getTimerVal();

						// ACTimer
                        final COPSAcctTimer at = cMsg.getAcctTimer();
						short acctTimer = 0;
						if (at != null)
							acctTimer = at.getTimerVal();

						logger.info("Send a REQ message to the PS");
                        final Properties prop = new Properties();
                        final COPSMsg reqMsg = MessageFactory.getInstance().create(COPSHeader.COPS_OP_REQ, prop);
                        final String handle = ((COPSReqMsg) reqMsg).getClientHandle().getId().str();
                        sendRequest(reqMsg);
						// Create the connection manager

                        // TODO - Determine if this is correct as a default PEP ID does not seem correct
                        final COPSPepId pepId = new COPSPepId(new COPSData(handle));

						final COPSPepConnection conn =
                                new COPSPepConnection(pepId, CLIENT_TYPE, socket, kaTimeVal, acctTimer);
						// pcmm specific handler
						// conn.addReqStateMgr(handle, new
						// PCMMPSReqStateMan(CLIENT_TYPE, handle));

                        final COPSHandle copsHandle = new COPSHandle(new COPSData(handle));
						conn.addRequestState(copsHandle, new CmtsDataProcessor());
						logger.info(getClass().getName() + " Thread(conn).start");

                        // TODO - manage threads
						new Thread(conn).start();
					} else {
						// messages of other types are not expected
						throw new COPSPepException("Message not expected. Closing connection for " + socket.toString());
					}
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}

			@Override
			public void task(Callable<?> c) {
				// TODO Auto-generated method stub

			}

			@Override
			public void shouldWait(int t) {
				// TODO Auto-generated method stub

			}

			@Override
			public void done() {
				// TODO Auto-generated method stub

			}

		};
	}

	@SuppressWarnings("rawtypes")
	class PCMMPSReqStateMan extends COPSPepReqStateMan {

		public PCMMPSReqStateMan(final short clientType, final String clientHandle) {
			super(clientType, clientHandle, new CmtsDataProcessor());

		}

		@Override
		protected void processDecision(final COPSDecisionMsg dMsg) throws COPSPepException {

			// COPSHandle handle = dMsg.getClientHandle();
            final Map<COPSContext, List<COPSDecision>> decisions = dMsg.getDecisions();

            final Map<String, String> removeDecs = new HashMap<>();
            final Map<String, String> installDecs = new HashMap<>();
            final Map<String, String> errorDecs = new HashMap<>();

			for (final Map.Entry<COPSContext, List<COPSDecision>> entry : decisions.entrySet()) {

				final COPSDecision cmddecision = entry.getValue().get(0);

				// cmddecision --> we must check whether it is an error!

                // TODO - refactor as both blocks are basically the same
				if (cmddecision.isInstallDecision()) {
					String prid = "";
					for (int i = 1; i < entry.getValue().size(); i++) {
						final COPSDecision decision = entry.getValue().get(i);
						final COPSPrObjBase obj = new COPSPrObjBase(decision.getData().getData());
						switch (obj.getSNum()) {
                            // TODO when there is install request only the PR_PRID
                            // is git but the ClientSI object containing the PR_EPD
                            // is null??? this is why the tests fail and so I set
                            // the assertion to NOT true....
                            case COPSPrObjBase.PR_PRID:
                                prid = obj.getData().str();
                            break;
                            case COPSPrObjBase.PR_EPD:
                                installDecs.put(prid, obj.getData().str());
                            break;
                            default:
                            break;
						}
					}
				}
				if (cmddecision.isRemoveDecision()) {
					String prid = "";
                    for (int i = 1; i < entry.getValue().size(); i++) {
                        final COPSDecision decision = entry.getValue().get(i);
						final COPSPrObjBase obj = new COPSPrObjBase(decision.getData().getData());
						switch (obj.getSNum()) {
						case COPSPrObjBase.PR_PRID:
							prid = obj.getData().str();
						break;
						case COPSPrObjBase.PR_EPD:
							removeDecs.put(prid, obj.getData().str());
						break;
						default:
						break;
						}
					}
				}
			}
			if (_process != null) {
				// ** Apply decisions to the configuration
				_process.setDecisions(this, removeDecs, installDecs, errorDecs);
				_status = ST_DECS;
				if (_process.isFailReport(this)) {
					// COPSDebug.out(getClass().getName(),"Sending FAIL Report\n");
					_sender.sendFailReport(_process.getReportData(this));
				} else {
					// COPSDebug.out(getClass().getName(),"Sending SUCCESS Report\n");
					_sender.sendSuccessReport(_process.getReportData(this));
				}
				_status = ST_REPORT;
			}
		}
	}

    /**
     * Responsible for processing PEP Data
     */
	class CmtsDataProcessor implements COPSPepDataProcess {

		private final Map<String, String> removeDecs;
		private final Map<String, String> installDecs;
		private final Map<String, String> errorDecs;
		private transient COPSPepReqStateMan stateManager;

        /**
         * Constructor
         */
		public CmtsDataProcessor() {
            removeDecs = new HashMap<>();
            installDecs = new HashMap<>();
            errorDecs = new HashMap<>();
		}

		@Override
		public void setDecisions(final COPSPepReqStateMan man, final Map<String, String> removeDecs,
                                 final Map<String, String> installDecs, final Map<String, String> errorDecs) {
            this.removeDecs.putAll(removeDecs);
            this.installDecs.putAll(installDecs);
            this.errorDecs.putAll(errorDecs);
            this.stateManager = man;
		}

		@Override
		public boolean isFailReport(final COPSPepReqStateMan man) {
			return (errorDecs.size() > 0);
		}

		@Override
		public Map<String, String> getReportData(final COPSPepReqStateMan man) {
			if (isFailReport(man)) {
				return errorDecs;
			} else {
                // TODO - this is a bug, where is the key supposed to come from??? It will always be null
				final Map<String, String> siDataHashTable = new HashMap<>();
				if (installDecs.size() > 0) {
					final String data = installDecs.keySet().iterator().next();
					final ITransactionID transactionID = new PCMMGateReq(new COPSData(data).getData()).getTransactionID();
                    final IPCMMGate responseGate = new PCMMGateReq();
					responseGate.setTransactionID(transactionID);

                    // TODO - determine the correct key here.
                    // There was a bug here where the key to the Map was always null so I'm just using the data value for now
					siDataHashTable.put(data, new String(responseGate.getData()));
				}
				return siDataHashTable;
			}
		}

		@Override
		public Map<String, String> getClientData(final COPSPepReqStateMan man) {
			// TODO Implement me
			return new HashMap<>();
		}

		@Override
		public Map<String, String> getAcctData(final COPSPepReqStateMan man) {
            // TODO Implement me
			return new HashMap<>();
		}

		@Override
		public void notifyClosedConnection(final COPSReqStateMan man, final COPSError error) {
            // TODO Implement me
		}

		@Override
		public void notifyNoKAliveReceived(final COPSReqStateMan man) {
            // TODO Implement me
		}

		@Override
		public void closeRequestState(final COPSReqStateMan man) {
            // TODO Implement me
		}

		@Override
		public void newRequestState(final COPSPepReqStateMan man) {
            // TODO Implement me
		}

	}
}
