/**
 * 
 */
package org.pcmm.nio;

import org.pcmm.PCMMProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.stack.COPSException;
import org.umu.cops.stack.COPSHeader;
import org.umu.cops.stack.COPSMsg;
import org.umu.cops.stack.COPSMsgParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Date;

/**
 * this class provides a set of utilities to efficiently read/write data from a
 * stream, it could parameterized with a reading timeout or -1 for blocking
 * until get a message
 * 
 */
public class PCMMChannel {

	private static final Logger logger = LoggerFactory.getLogger(PCMMChannel.class);

    // TODO - What is the ByteBuffer used for. It isn't doing anything here
//	private final ByteBuffer dataBuffer;
	private final Socket socket;
	private final int timeout;
	public static final int DEFAULT_BYTE_BUFFER_SIZE = 2048;
	public static final int DEFAULT_READ_TIMEOUT = -1;

	public PCMMChannel(final Socket socket) {
		this(socket, PCMMProperties.get(PCMMProperties.DEFAULT_TIEMOUT, Integer.class, DEFAULT_READ_TIMEOUT));
	}

	public PCMMChannel(final Socket socket, final int timeout) {
		this.socket = socket;
//        dataBuffer = ByteBuffer.allocateDirect(DEFAULT_BYTE_BUFFER_SIZE);
		logger.info("Allocated byte buffer with size = "
				+ DEFAULT_BYTE_BUFFER_SIZE);
		this.timeout = timeout;
		logger.info("Set read/write timeout to : " + timeout);

	}

	public int readData(final byte[] dataRead, final int nchar) throws IOException {
        final InputStream input = getSocket().getInputStream();
        int nread = 0;
        int startTime = (int) (new Date().getTime());
		do {
			if (timeout == -1 || input.available() != 0) {
				nread += input.read(dataRead, nread, nchar - nread);
				startTime = (int) (new Date().getTime());
			} else {
				int nowTime = (int) (new Date().getTime());
				if ((nowTime - startTime) > timeout)
					break;
			}
		} while (nread != nchar);
		return nread;
	}

	/**
	 * Method sendMsg
	 * 
	 * @param msg
	 *            a COPSMsg
	 * @throws IOException
	 * @throws COPSException
	 */
	public void sendMsg(final COPSMsg msg) throws IOException, COPSException {
		logger.debug("sendMsg({})==>{}", getSocket(), msg);
		msg.checkSanity();
		msg.writeData(getSocket());
	}

	/**
	 * Method receiveMessage
	 * @return a COPSMsg
	 * @throws IOException
	 * @throws COPSException
	 */
	public COPSMsg receiveMessage() throws IOException, COPSException {
		byte[] hBuf = new byte[8];

		logger.debug("receiveMessage({})", getSocket());

		int nread = readData(hBuf, 8);

		if (nread == 0) {
			throw new COPSException("Error reading connection");
		}

		if (nread != 8) {
			throw new COPSException("Bad COPS message");
		}

        final COPSHeader hdr = new COPSHeader(hBuf);
        final int dataLen = hdr.getMsgLength() - hdr.getHdrLength();
		logger.debug("COPS Msg length :[" + dataLen + "]\n");
        final byte[] buf = new byte[dataLen + 1];

		nread = readData(buf, dataLen);
		buf[dataLen] = (byte) '\0';
		logger.debug("Data read length:[" + nread + "]\n");

		if (nread != dataLen) {
			throw new COPSException("Bad COPS message");
		}
		final COPSMsgParser prser = new COPSMsgParser();
		return prser.parse(hdr, buf);
	}

	/**
	 * @return the _socket
	 */
	public Socket getSocket() {
		return socket;
	}

}
