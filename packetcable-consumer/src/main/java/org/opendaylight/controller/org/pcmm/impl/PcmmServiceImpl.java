package org.opendaylight.controller.org.pcmm.impl;

import com.google.common.collect.Maps;
import org.opendaylight.controller.org.pcmm.api.PcmmService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.node.cmts.broker.rev140909.CmtsAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.node.cmts.broker.rev140909.CmtsRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.node.cmts.broker.rev140909.CmtsUpdated;
import org.pcmm.rcd.IPCMMPolicyServer;
import org.pcmm.rcd.IPCMMPolicyServer.IPSCMTSClient;
import org.pcmm.rcd.impl.PCMMPolicyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PcmmServiceImpl implements PcmmService {

	private static final Logger logger = LoggerFactory.getLogger(PcmmServiceImpl.class);

	private final Map<IpAddress, IPSCMTSClient> cmtsClients;
	private final IPCMMPolicyServer policyServer;

	public PcmmServiceImpl() {
		policyServer = new PCMMPolicyServer();
		cmtsClients = Maps.newConcurrentMap();
	}

	@Override
	public void onCmtsAdded(final CmtsAdded notification) {
        logger.info("CMTS Added");
		String ipv4 = notification.getAddress().getIpv4Address().getValue();
		IPSCMTSClient client = policyServer.requestCMTSConnection(ipv4);
		if (client.isConnected()) {
			cmtsClients.put(notification.getAddress(), client);
		}
	}

	@Override
	public void onCmtsRemoved(final CmtsRemoved notification) {
        logger.info("CMTS Removed");
		if (cmtsClients.containsKey(notification.getAddress())) {
			cmtsClients.remove(notification.getAddress()).disconnect();
		}
	}

	@Override
	public void onCmtsUpdated(final CmtsUpdated notification) {
        logger.info("CMTS Updated");
		// TODO
	}

	@Override
	public Boolean sendGateDelete() {
        logger.info("Sending Gate Delete");
		// TODO change me
		boolean ret = true;
        for (final IPSCMTSClient client : cmtsClients.values()) {
            ret &= client.gateDelete();
        }
		return ret;
	}

	@Override
	public Boolean sendGateSynchronize() {
        logger.info("Sending Gate Synchronize");
		boolean ret = true;
        for (final IPSCMTSClient client : cmtsClients.values()) {
            ret &= client.gateSynchronize();
        }
		return ret;
	}

	@Override
	public Boolean sendGateInfo() {
        logger.info("Sending Gate Info");
		boolean ret = true;
        for (final IPSCMTSClient client : cmtsClients.values()) {
            ret &= client.gateInfo();
        }
		return ret;
	}

	@Override
	public Boolean sendGateSet() {
        logger.info("Sending Gate Set");
		boolean ret = true;
        for (final IPSCMTSClient client : cmtsClients.values()) {
            ret &= client.gateSet();
        }
		return ret;
	}
}
