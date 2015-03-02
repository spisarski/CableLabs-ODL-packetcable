/**
 * 
 */
package org.pcmm.concurrent.impl;

import org.pcmm.concurrent.IWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * 
 */
public class Worker implements IWorker {

    private int waitTimer;

	private Callable<?> task;
	private static final Logger logger = LoggerFactory.getLogger(Worker.class);

	public Worker() {

	}

	public Worker(Callable<?> task) {
		this.task = task;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
        logger.info("Running");
		try {
			if (waitTimer > 0)
				Thread.sleep(waitTimer);
			task.call();
		} catch (Throwable e) {
			logger.error(e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pcmm.threading.IWorker#task(java.util.concurrent.Callable)
	 */
	@Override
	public void task(Callable<?> c) {
		logger.debug("Task added " + c);
		this.task = c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pcmm.threading.IWorker#shouldWait(int)
	 */
	@Override
	public void shouldWait(int t) {
		logger.debug("Worker will start after :" + t + " ms");
		waitTimer = t;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pcmm.threading.IWorker#done()
	 */
	@Override
	public void done() {
		logger.debug("worker finished tasks");

	}

}
