package org.pcmm.concurrent.impl;

import org.pcmm.concurrent.IWorker;
import org.pcmm.concurrent.IWorkerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pool to manage PCMM workers
 */
public class WorkerPool implements IWorkerPool {

    private static final Logger logger = LoggerFactory.getLogger(IWorkerPool.class);

	private final Map<Integer, WeakReference<IWorker>> workersMap;

	private final ExecutorService executor;

	public WorkerPool() {
		this(DEFAULT_MAX_WORKERS);
	}

	public WorkerPool(final int size) {
		logger.info("Pool size :" + size);
		workersMap = new ConcurrentHashMap<>();
		executor = Executors.newFixedThreadPool(size);
	}

	@Override
	public int schedule(final IWorker worker, final int t) {
		if (worker == null)
			return -1;
		logger.debug("woker[" + worker + "] added, starts in " + t + " ms");
        final WeakReference<IWorker> workerRef = new WeakReference<>(worker);
        final int ref = workerRef.hashCode();
		workersMap.put(ref, new WeakReference<>(worker));
		worker.shouldWait(t);
		executor.execute(worker);
		return ref;
	}

	@Override
	public int schedule(IWorker worker) {
		return schedule(worker, 0);
	}

	@Override
	public void sendKillSignal(int pid) {
        logger.info("Sending kill signal to pid - " + pid);
		if (workersMap.size() > 0) {
			WeakReference<IWorker> weakRef = workersMap.get(pid);
			if (weakRef != null) {
				IWorker ref = weakRef.get();
				if (ref != null)
					ref.done();
				if (!weakRef.isEnqueued()) {
					weakRef.clear();
					weakRef.enqueue();
				}
			}
		}

	}

	@Override
	public void killAll() {
        logger.info("Killing all workers");
		for (WeakReference<IWorker> weakRef : workersMap.values()) {
			IWorker ref = weakRef.get();
			if (ref != null)
				ref.done();
			if (!weakRef.isEnqueued()) {
				weakRef.clear();
				weakRef.enqueue();
			}
		}
		recycle();
	}

	@Override
	public void recycle() {
        logger.info("Recycling all workers");
        for (final Map.Entry<Integer, WeakReference<IWorker>> entry : workersMap.entrySet()) {
			WeakReference<IWorker> weakRef = entry.getValue();
			IWorker ref = weakRef.get();
			if (ref == null) {
				if (!weakRef.isEnqueued()) {
					weakRef.clear();
					weakRef.enqueue();
				}
				workersMap.remove(entry.getKey());
			}
		}

	}

	@Override
	public Object adapt(Object object, Class<?> clazz) {
        logger.info("Adapt with object and class");
		if (clazz.isAssignableFrom(object.getClass()))
			return object;
		return null;
	}

	@Override
	public IWorker adapt(Object object) {
        logger.info("Adapt with object");
		IWorker worker = (IWorker) adapt(object, IWorker.class);
		if (worker == null) {
			if (object instanceof Callable)
				worker = new Worker((Callable<?>) object);
			else if (object instanceof Runnable) {
				final Runnable runner = (Runnable) object;
				worker = new Worker(new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						runner.run();
						return null;
					}
				});
			}
		}
		return worker;
	}

}
