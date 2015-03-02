/**
 @header@
 */
package org.pcmm.rcd.impl;

import org.pcmm.PCMMConstants;
import org.pcmm.PCMMProperties;
import org.pcmm.concurrent.IWorkerPool;
import org.pcmm.concurrent.impl.WorkerPool;
import org.pcmm.messages.impl.MessageFactory;
import org.pcmm.rcd.IPCMMServer;
import org.pcmm.state.IState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umu.cops.stack.COPSHeader;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

/**
  * Base PCMM server.
  *
  * TODO - Determine why this is in the PCMM ODL codebase as the PCMM plugin really should not ever be starting a
  * server to accept TCP connections
  */
public abstract class AbstractPCMMServer implements IPCMMServer {

    private final static Logger logger = LoggerFactory.getLogger(AbstractPCMMServer.class);

	/*
	 * A ServerSocket to accept messages ( OPN requests)
	 */
	private transient ServerSocket serverSocket;

    /**
     * The socket connection used to stop this server
     */
	private transient Socket stopSocket;

    /**
     * Used to determine whether the server will continue to accept messages
     */
	private volatile boolean keepAlive;

	/**
     * The port on which to start the server
     */
	private final int port;

    /**
     * Thread pool
     */
	protected final IWorkerPool pool;

    /**
     * Default constructor who uses the default port number
     */
	protected AbstractPCMMServer() {
        this(PCMMProperties.get(PCMMConstants.PCMM_PORT, Integer.class));
	}

    /**
     * Main constructor
     * @param port - the port on which to listen
     */
	protected AbstractPCMMServer(final int port) {
		// XXX - Assert.assertTrue(port >= 0 && port <= 65535);
		this.port = port;
		keepAlive = true;
		int poolSize = PCMMProperties.get(PCMMConstants.PS_POOL_SIZE, Integer.class);
		pool = new WorkerPool(poolSize);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pcmm.rcd.IPCMMServer#startServer()
	 */
    @Override
	public void startServer() {
        logger.info("Attempting to start PCMM server");
		if (serverSocket != null)
			return;
		try {
			serverSocket = new ServerSocket(port);
			logger.info("Server started and listening on port :" + port);
		} catch (IOException e) {
			logger.error("Unexpected error obtaining server socket to port - " + port, e);
            throw new RuntimeException("Unexpected error obtaining server socket to port - " + port, e);
		}
		// execute this in a single thread executor
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
            while (keepAlive) {
                try {
                    final Socket socket = serverSocket.accept();
                    logger.info("Accepted a new connection from :"
                            + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                    if (keepAlive) {
                        pool.schedule(getPCMMClientHandler(socket));
                        logger.info("Handler attached to : "
                                + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                    } else {
                        logger.info("connection to be closed : "
                                + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                        socket.close();
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
            try {
                if (stopSocket != null && stopSocket.isConnected()) {
                    logger.info("Cleaning up");
                    stopSocket.close();
                }
                if (serverSocket != null && serverSocket.isBound()) {
                    logger.info("Server about to stop");
                    serverSocket.close();
                    logger.info("Server stopped");
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
			}
		});
	}

	/**
	 * This client is used to handle requests from within the Application
	 * Manager
	 * 
	 * @param socket - the connector
	 * @return client handler
	 */
	protected abstract IPCMMClientHandler getPCMMClientHandler(Socket socket);

    @Override
	public void stopServer() {
        logger.info("Attempting to stop PCMM server");
		// set to stop
		keepAlive = false;
		try {
			if (serverSocket != null) {
				stopSocket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
				logger.info("STOP _socket created and attached");
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

    @Override
	public void recordState() {
        logger.info("Recording state");
        // TODO - Implement me
	}

    @Override
	public IState getRecoredState() {
        logger.info("Retrieving the recorded state");
        // TODO - Implement me
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pcmm.rcd.IPCMMServer.IPCMMClientHandler
	 */
	public abstract class AbstractPCMMClientHandler extends AbstractPCMMClient implements IPCMMClientHandler {

		protected boolean sendCCMessage = false;

		public AbstractPCMMClientHandler(final Socket socket) {
			super();
            logger.info("Instantiating new AbstractPCMMClientHandler");
			setSocket(socket);
		}

		@Override
		public boolean disconnect() {
            logger.info("AbstractPCMMClientHandler disconnecting");
			// XXX send CC message
			sendCCMessage = true;
			/*
			 * is this really needed ?
			 */
			// if (getSocket() != null)
			// handlersPool.remove(getSocket());
			sendRequest(MessageFactory.getInstance().create(COPSHeader.COPS_OP_CC));
			return super.disconnect();
		}

	}

}
