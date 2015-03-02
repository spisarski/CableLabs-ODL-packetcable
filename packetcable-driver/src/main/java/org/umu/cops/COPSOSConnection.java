package org.umu.cops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.common.COPSDebug;
import org.umu.cops.stack.*;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

/**
 * Abstract class for all COPSOSConnections.
 */
public abstract class COPSOSConnection extends COPSConnection {

    private static final Logger logger = LoggerFactory.getLogger(COPSOSConnection.class);

    /**
     Accounting timer value (secs)
     */
    protected final short _acctTimer;

    /**
     Opcode of the latest message sent
     */
    protected transient byte _lastmessage;

    /**
     *  Time of the latest keep-alive received
     */
    protected transient Date _lastRecKa;

    public COPSOSConnection(final COPSPepId pepId, final Socket sock, final short kaTimer, final short acctTimer) {
        super(pepId, sock, kaTimer);
        this._acctTimer = acctTimer;
        _lastmessage = COPSHeader.COPS_OP_CAT;
    }

    /**
     * Message-processing loop
     */
    public void run () {
        Date _lastSendKa = new Date();
        _lastRecKa = new Date();

        try {
            while (!_sock.isClosed()) {
                if (_sock.getInputStream().available() != 0) {
                    _lastmessage = processMessage(_sock);
                    _lastRecKa = new Date();
                }

                // Keep Alive
                if (_kaTimer > 0) {
                    // Timeout del PDP
                    int _startTime = (int) (_lastRecKa.getTime());
                    int cTime = (int) (new Date().getTime());

                    if ((cTime - _startTime) > _kaTimer * 1000) {
                        _sock.close();
                        // Notify all Request State Managers
                        notifyNoKAAllReqStateMan();
                    }

                    // Send to PEP
                    _startTime = (int) (_lastSendKa.getTime());
                    cTime = (int) (new Date().getTime());

                    if ((cTime - _startTime) > ((_kaTimer*3/4) * 1000)) {
                        COPSHeader hdr = new COPSHeader(COPSHeader.COPS_OP_KA);
                        COPSKAMsg msg = new COPSKAMsg();

                        msg.add(hdr);

                        COPSTransceiver.sendMsg(msg, _sock);
                        _lastSendKa = new Date();
                    }
                }

                accounting();

                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    logger.error("Unexpected error while thread was sleeping", e);
                }
            }
        } catch (Exception e) {
            logger.error(COPSDebug.ERROR_SOCKET, e);
        }

        // connection closed by server
        // COPSDebug.out(getClass().getName(),"Connection closed by server");
        try {
            _sock.close();
        } catch (IOException e) {
            logger.error("Unexpected error closing _socket", e);
        }

        // Notify all Request State Managers
        try {
            notifyCloseAllReqStateMan();
        } catch (COPSException e) {
            logger.error("Unexpected error closing state managers");
        }
    }

    protected abstract void accounting() throws COPSException;

    protected abstract byte processMessage(Socket conn) throws COPSException, IOException;

}
