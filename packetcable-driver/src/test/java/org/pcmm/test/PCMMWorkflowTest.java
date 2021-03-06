package org.pcmm.test;

import org.junit.After;
import org.junit.Before;
import org.pcmm.rcd.ICMTS;
import org.pcmm.rcd.IPCMMPolicyServer;
import org.pcmm.rcd.IPCMMPolicyServer.IPSCMTSClient;
import org.pcmm.rcd.impl.CMTS;
import org.pcmm.rcd.impl.PCMMPolicyServer;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

public class PCMMWorkflowTest {

	/**
	 * CMTS emulator, when testing with a real CMTS this should be set to null
	 * and shoudln't be started
	 */
	private ICMTS cmts;
	/**
	 * CMTS host address, when testing with a real CMTS this should be CMTS
	 * address
	 */
	private InetAddress host;

	private IPCMMPolicyServer server;
	private IPSCMTSClient client;
	private boolean real_cmts = false;

	@Before
	public void setUpBeforeClass() throws Exception {
		if (real_cmts == true) {
		     cmts = null;
		} else {
		     cmts = new CMTS();
	             cmts.startServer();
		}

		server = new PCMMPolicyServer();
		try {
			if (real_cmts == true) {
			     // this should be set to the cmts host ex :
			     // host = InetAddress.getByName(PCMMGlobalConfig.DefaultCMTS);
			     host = InetAddress.getByName("10.200.90.3");
			     host = InetAddress.getByName("127.0.0.1");
			     // InetAddress.getByName("my-cmts-host-name");
			} else {
			     host = InetAddress.getLocalHost();
			}
			assertNotNull(host);

		} catch (UnknownHostException uhe) {
			fail("could not get host address ");
		}
		setupConnection();
	}

	@After
	public void tearDownAfterClass() throws Exception {
		tearDown();
		if (cmts != null)
			cmts.stopServer();
	}

	public void setupConnection() {
		client = server.requestCMTSConnection(host);
		assertNotNull(client);
	}

	public void tearDown() throws Exception {
		assertNotNull(client);
		assertTrue("Client disconnection failed", client.disconnect());
	}

	
	//@Test
	public void testGateSet() {
		assertNotNull(client);
		assertTrue("Gate-Set failed", client.gateSet());
	}

	//@Test
	public void testGateDelete() {
		assertNotNull(client);
		assertTrue("Gate-Delete failed", client.gateDelete());

	}

	//@Test
	public void testGateInfo() {
		assertNotNull(client);
		assertTrue("Gate-Info failed", client.gateInfo());
	}

	//@Test
	public void testGateSynchronize() {
		assertNotNull(client);
		assertTrue("Gate-Synchronize failed", client.gateSynchronize());
	}

}
