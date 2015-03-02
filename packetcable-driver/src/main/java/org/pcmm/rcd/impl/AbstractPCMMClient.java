/**
 @header@
 */
package org.pcmm.rcd.impl;

import org.pcmm.nio.PCMMChannel;
import org.pcmm.objects.MMVersionInfo;
import org.pcmm.rcd.IPCMMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.stack.COPSMsg;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

// import org.junit.Assert;

/**
 * 
 * default implementation for {@link IPCMMClient}
 * 
 * TODO - Should make this class as immutable as possible especially for the member "socket" which can cause NPEs.
 */
public class AbstractPCMMClient implements IPCMMClient {

	private final static Logger logger = LoggerFactory.getLogger(AbstractPCMMClient.class);

	/**
	 * _socket used to communicated with server.
	 */
	private Socket socket;

	private String clientHanlde;

	private MMVersionInfo versionInfo;

	private PCMMChannel channel;

	public AbstractPCMMClient() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pcmm.rcd.IPCMMClient#sendRequest(pcmm.messages.IMessage)
	 */
	public void sendRequest(final COPSMsg requestMessage) {
        logger.info("Sending request");
		try {
			channel.sendMsg(requestMessage);
		} catch (Exception e) {
			logger.error(e.getMessage(), getSocket());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pcmm.rcd.IPCMMClient#readMessage()
	 */
	public COPSMsg readMessage() {
        logger.info("Reading message");
		try {
			return channel.receiveMessage();
		} catch (Exception e) {
			logger.error(e.getMessage(), getSocket());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pcmm.rcd.IPCMMClient#tryConnect(java.lang.String, int)
	 */
	public boolean tryConnect(final String address, final int port) {
        logger.info("Attempting to connect to " + address + ':' + port);
		try {
			final InetAddress addr = InetAddress.getByName(address);
			tryConnect(addr, port);
		} catch (UnknownHostException e) {
			logger.error(e.getMessage());
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pcmm.rcd.IPCMMClient#tryConnect(java.net.InetAddress, int)
	 */
	public boolean tryConnect(final InetAddress address, final int port) {
        logger.info("Attempting to connect to " + address + ':' + port);

        // TODO - This does not appear to be connecting to anything. See overloaded method above
		try {
			setSocket(new Socket(address, port));
		} catch (IOException e) {
			logger.error(e.getMessage());
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pcmm.rcd.IPCMMClient#disconnect()
	 */
	public boolean disconnect() {
        logger.info("Disconnecting");
		if (isConnected()) {
			try {
				socket.close();
				channel = null;
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
		return true;
	}

	/**
	 * @return the _socket
	 */
	public Socket getSocket() {
		return socket;
	}

	/**
	 * @param socket
	 *            the _socket to set
	 */
	public void setSocket(Socket socket) {
		this.socket = socket;
		if (this.socket != null
				&& (this.channel == null || !this.channel.getSocket().equals(
						this.socket)))
			channel = new PCMMChannel(this.socket);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pcmm.rcd.IPCMMClient#isConnected()
	 */
	public boolean isConnected() {
		return socket != null && socket.isConnected();
	}

	/**
	 * @return the versionInfo
	 */
	public MMVersionInfo getVersionInfo() {
		return versionInfo;
	}

	/**
	 * @param versionInfo
	 *            the versionInfo to set
	 */
	public void setVersionInfo(MMVersionInfo versionInfo) {
		this.versionInfo = versionInfo;
	}

	@Override
	public String getClientHandle() {
		return clientHanlde;
	}

	@Override
	public void setClientHandle(String handle) {
		this.clientHanlde = handle;
	}

}
