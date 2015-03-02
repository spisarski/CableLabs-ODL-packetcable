/*
 * Copyright (c) 2003 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

/**
 * COPS Transceiver
 *
 * @version COPSTransceiver.java, v 1.00 2003
 *
 */
public class COPSTransceiver {

    private final static Logger logger = LoggerFactory.getLogger(COPSTransceiver.class);

    /**
     * Method sendMsg
     * @param    msg                 a  COPSMsg
     * @param    fd                  a  Socket
     * @throws   IOException, COPSException
     */
    static public void sendMsg(final COPSMsg msg, final Socket fd) throws IOException, COPSException {
        logger.info("sendMsg ******************************** START" );
        msg.checkSanity();
        msg.writeData(fd);
        logger.info("sendMsg ******************************** END" );
    }

    /**
     * Method receiveMsg
     * @param    fd                  a  Socket
     * @return   a COPSMsg
     * @throws   IOException
     * @throws   COPSException
     */
    static public COPSMsg receiveMsg(final Socket fd) throws IOException, COPSException {
        final byte[] hBuf = new byte[8];
        logger.info("receiveMsg ******************************** START");

        int nread = COPSUtil.readData(fd, hBuf, 8);

        if (nread == 0) {
            throw new COPSException("Error reading connection");
        }

        if (nread != 8) {
            throw new COPSException("Bad COPS message");
        }

        final COPSHeader hdr = new COPSHeader(hBuf);
        final int dataLen = hdr.getMsgLength() - hdr.getHdrLength();
        logger.info("COPS Msg length :[" + dataLen + "]");
        final byte[] buf = new byte[dataLen + 1];
        nread = COPSUtil.readData(fd, buf, dataLen);
        buf[dataLen] = (byte) '\0';
        logger.info("Data read length:[" + nread + "]");

        if (nread != dataLen) {
            throw new COPSException("Bad COPS message");
        }

        final COPSMsgParser prser = new COPSMsgParser();
        return prser.parse(hdr, buf);
    }
}

