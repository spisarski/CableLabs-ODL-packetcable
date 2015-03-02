package org.opendaylight.controller.packetcable.provider;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.packetcable.provider.processors.PCMMDataProcessor;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.traffic.profile.rev140908.TrafficProfileBestEffortAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.traffic.profile.rev140908.TrafficProfileDocsisServiceClassNameAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.traffic.profile.rev140908.TrafficProfileFlowspecAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.traffic.profile.rev140908.add.flow.input.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.BestEffortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.traffic.profile.rev140908.add.flow.input.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.DocsisServiceClassNameCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.traffic.profile.rev140908.add.flow.input.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.FlowspecCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContextRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.node.cmts.broker.rev140909.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.node.cmts.rev140909.CmtsCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.node.cmts.rev140909.nodes.node.CmtsNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packetcable.match.types.rev140909.UdpMatchRangesRpcRemoveFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packetcable.match.types.rev140909.UdpMatchRangesRpcUpdateFlowOriginal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packetcable.match.types.rev140909.UdpMatchRangesRpcUpdateFlowUpdated;
import org.opendaylight.yangtools.concepts.CompositeObjectRegistration;
import org.opendaylight.yangtools.concepts.CompositeObjectRegistration.CompositeObjectRegistrationBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.pcmm.PCMMDef;
import org.pcmm.PCMMPdpAgent;
import org.pcmm.PCMMPdpDataProcess;
import org.pcmm.gates.IClassifier;
import org.pcmm.gates.ITrafficProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@SuppressWarnings("unused")
public class OpendaylightPacketcableProvider implements DataChangeListener, SalFlowService,
        OpenDaylightPacketCableProviderService, BindingAwareProvider, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(OpendaylightPacketcableProvider.class);

    // TODO - Why isn't this object being constructed instead of being set which can cause NPEs
	private NotificationProviderService notificationProvider;

    /**
     * Optional
     */
    // TODO - Why isn't this object being constructed instead of being set which can cause NPEs
	private transient DataBroker dataProvider;

    /**
     * Thread pool
     * TODO - this still has yet to have any Runnables added to it
     * TODO - may need to provide back pressure for requests going into thread pool as it could cause OutOfMemoryError
     */
//	private final ExecutorService executor;

	// The following holds the Future for the current make toast task.
	// This is used to cancel the current toast.
    // TODO - Why is this reference never being used???
//	private final AtomicReference<Future<?>> currentConnectionsTasks;
	private transient ProviderContext providerContext;

    // TODO - this isn't being used really anywhere, why???
	private transient NotificationProviderService notificationService;

	private DataBroker dataBroker;

    // TODO - this isn't being used really anywhere, why???
	private transient ListenerRegistration<DataChangeListener> listenerRegistration;

    /**
     * The managed CMTSs
     */
	private final List<InstanceIdentifier<?>> cmtsInstances;

    /**
     * Processess PCMM data
     */
	private final PCMMDataProcessor pcmmDataProcessor;

    private final Map<String, PCMMPdpAgent> pdpAgents;

    /**
     * Constructor
     */
	public OpendaylightPacketcableProvider() {
        logger.info("Constructing OpendaylightPacketcableProvider");
//		executor = Executors.newCachedThreadPool();
		cmtsInstances = Lists.newArrayList();
		pcmmDataProcessor = new PCMMDataProcessor();
//        currentConnectionsTasks = new AtomicReference<>();
        pdpAgents = new HashMap<>();
    }

    /**
     * Called indirectly via PacketcableProviderModule#createInstance() line where
     * BindingAwareBroker#registerProvider(this)
     * @param session - the provider context session
     */
    @Override
    public void onSessionInitiated(final ProviderContext session) {
        logger.info("Initiating session");
        providerContext = session;
        notificationService = session.getSALService(NotificationProviderService.class);
        dataBroker = session.getSALService(DataBroker.class);
        final InstanceIdentifier<CmtsNode> listenTo = InstanceIdentifier.create(Nodes.class).child(Node.class)
                .augmentation(CmtsCapableNode.class).child(CmtsNode.class);
        listenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, listenTo, this,
                DataChangeScope.BASE);

        final PCMMPdpAgent agent = new PCMMPdpAgent("localhost", 3918, PCMMDef.C_PCMM, new PCMMPdpDataProcess(),
                (short)0, (short)0);
/*
        try {
            agent.connect();
            logger.info("Connected to PCMMPdpAgent");
        } catch (Exception e) {
            logger.error("Error connecting to agent", e);
            throw new RuntimeException("Error connecting to agent", e);
        }
*/
        logger.info("Starting PCMMPdpAgent thread");
        agent.start();
        logger.info("PCMMPdpAgent thread started");
        pdpAgents.put("localhost:3918", agent);
    }

    public void setNotificationProvider(final NotificationProviderService salService) {
        logger.info("Setting notification provider");
		this.notificationProvider = salService;
	}

	public void setDataProvider(final DataBroker salDataProvider) {
        logger.info("Setting data provider");
		this.dataProvider = salDataProvider;
	}

	/**
	 * Implemented from the AutoCloseable interface.
	 */
	@Override
	public void close() throws ExecutionException, InterruptedException {
        logger.info("Closing provider");
//		executor.shutdown();
        listenerRegistration.close();
		if (dataProvider != null) {
			for (final InstanceIdentifier<?> instance : cmtsInstances) {
				WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
				tx.delete(LogicalDatastoreType.OPERATIONAL, instance);
				Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
					@Override
					public void onSuccess(final Void result) {
						logger.debug("Delete commit result: " + result);
					}

					@Override
					public void onFailure(final Throwable t) {
						logger.error("Delete operation failed", t);
					}
				});
			}
		}

        for (final PCMMPdpAgent agent : pdpAgents.values()) {
            agent.stopAgent();
            agent.interrupt();
        }
	}

	/**
	 * Implemented from the DataChangeListener interface.
	 */
	@Override
	public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        logger.info("Data changed");
        final DataObject dataObject = change.getUpdatedSubtree();
		logger.debug("OpendaylightPacketcableProvider.onDataChanged() :" + dataObject);
	}

	public void notifyConsumerOnCmtsAdd(final CmtsNode input, final TransactionId transactionId) {
        logger.info("CMTS Add");
        final CmtsAdded cmtsRemoved = new CmtsAddedBuilder().setAddress(input.getAddress()).setPort(input.getPort())
                .setTransactionId(transactionId).build();
		notificationProvider.publish(cmtsRemoved);
	}

	public void notifyConsumerOnCmtsRemove(final CmtsNode input, final TransactionId transactionId) {
        logger.info("CMTS remove");
        final CmtsRemoved cmtsRemoved = new CmtsRemovedBuilder().setAddress(input.getAddress()).setPort(input.getPort())
                .setTransactionId(transactionId).build();
		notificationProvider.publish(cmtsRemoved);
	}

	public void notifyConsumerOnCmtsUpdate(final CmtsNode input, final TransactionId transactionId) {
        logger.info("CMTS Update");
        final CmtsUpdated cmtsRemoved = new CmtsUpdatedBuilder().setAddress(input.getAddress()).setPort(input.getPort())
                .setTransactionId(transactionId).build();
		notificationProvider.publish(cmtsRemoved);
	}

	@Override
	public Future<RpcResult<AddFlowOutput>> addFlow(final AddFlowInput input) {
        logger.info("Adding flow");
        final Match match = input.getMatch();
        final CmtsNode cmts = getCmtsNode(input);
		if (cmts != null)
			cmtsInstances.add(input.getNode().getValue());
        final IClassifier classifier = buildClassifier(match);
//        final ITrafficProfile trafficProfie = null;
		for (Instruction i : input.getInstructions().getInstruction()) {
			if (i.getInstruction() instanceof ApplyActionsCase) {
                final ApplyActionsCase aac = (ApplyActionsCase) i.getInstruction();
				for (Action a : aac.getApplyActions().getAction()) {
					if (a.getAction() instanceof FlowspecCase) {
						// TODO - Implement me
						// trafficProfie = buildTrafficProfile(((FlowspecCase)
						// a.getAction()).getFlowspec());
					} else if (a.getAction() instanceof BestEffortCase) {
                        // TODO - Why is this object not being used downstream
//						trafficProfie = buildTrafficProfile(((BestEffortCase) a.getAction()).getBestEffort());
						buildTrafficProfile(((BestEffortCase) a.getAction()).getBestEffort());
						break;
					} else if (a.getAction() instanceof DocsisServiceClassNameCase) {
                        // TODO - Why is this object not being used downstream
//						trafficProfie = buildTrafficProfile(((DocsisServiceClassNameCase) a.getAction()).getDocsisServiceClassName());
						buildTrafficProfile(((DocsisServiceClassNameCase) a.getAction()).getDocsisServiceClassName());
						break;
					}
				}
			}
		}

        // TODO - Why is this not being set? NPE waiting to happen
		final TransactionId transactionId = null;
		notifyConsumerOnCmtsAdd(cmts, transactionId);
		return Futures.immediateFuture(RpcResultBuilder.success(new AddFlowOutputBuilder().setTransactionId(
                transactionId).build()).build());
	}

	@Override
	public ITrafficProfile buildTrafficProfile(final TrafficProfileDocsisServiceClassNameAttributes docsis) {
        logger.info("Building traffic profile with service class name attributes");
		return pcmmDataProcessor.process(docsis);
	}

	@Override
	public ITrafficProfile buildTrafficProfile(final TrafficProfileBestEffortAttributes bestEffort) {
        logger.info("Building traffic profile with profile best effort");
		return pcmmDataProcessor.process(bestEffort);
	}

	@Override
	public ITrafficProfile buildTrafficProfile(final TrafficProfileFlowspecAttributes flowSpec) {
        logger.info("Building traffic profile with profile spec attributes");
		return pcmmDataProcessor.process(flowSpec);
	}

	@Override
	public IClassifier buildClassifier(final Match match) {
		return pcmmDataProcessor.process(match);
	}

	@Override
	public Future<RpcResult<RemoveFlowOutput>> removeFlow(final RemoveFlowInput input) {
        logger.info("Removing flow");
        final UdpMatchRangesRpcRemoveFlow updRange = input.getMatch().getAugmentation(
                UdpMatchRangesRpcRemoveFlow.class);
		notifyConsumerOnCmtsRemove(getCmtsNode(input), null);
		return null;
	}

	@Override
	public Future<RpcResult<UpdateFlowOutput>> updateFlow(final UpdateFlowInput input) {
        logger.info("Updating flow");
        final OriginalFlow foo = input.getOriginalFlow();
        final UdpMatchRangesRpcUpdateFlowOriginal bar = foo.getMatch().getAugmentation(
                UdpMatchRangesRpcUpdateFlowOriginal.class);
        final UpdatedFlow updated = input.getUpdatedFlow();
        final UdpMatchRangesRpcUpdateFlowUpdated updatedRange = updated.getMatch().getAugmentation(
                UdpMatchRangesRpcUpdateFlowUpdated.class);
		notifyConsumerOnCmtsUpdate(getCmtsNode(input), null);
		return null;
	}

	@SuppressWarnings("unchecked")
	protected CmtsNode getCmtsNode(final NodeContextRef input) {
        final NodeRef nodeRef = input.getNode();
        final InstanceIdentifier<Node> instanceIdentifier = (InstanceIdentifier<Node>) nodeRef.getValue();
        final ReadOnlyTransaction rtransaction = dataBroker.newReadOnlyTransaction();
        final CheckedFuture<Optional<Node>, ReadFailedException> value = rtransaction.read(
                LogicalDatastoreType.CONFIGURATION, instanceIdentifier);
		rtransaction.close();
        final Optional<Node> opt;
		try {
			opt = value.get();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
        final Node node = opt.get();
        final CmtsCapableNode cmts = node.getAugmentation(CmtsCapableNode.class);
        return cmts.getCmtsNode();
	}

	public void onSessionAdded(/* Whatever you need per CmtsConnection */) {
        logger.info("Adding session");
        // TODO - This looks like it needs to be further implemented
        final CompositeObjectRegistrationBuilder<OpendaylightPacketcableProvider> builder =
                CompositeObjectRegistration.builderFor(this);
		/*
		 * You will need a routedRpc registration per Cmts... I'm not doing the
		 * accounting of storing them here, but you will need to so you can
		 * close them when your provider is closed
		 */
        final RoutedRpcRegistration<SalFlowService> registration = providerContext.addRoutedRpcImplementation(SalFlowService.class, this);
		/*
		 * You will need to get your identifier somewhere... this is your
		 * nodeId. I would recommend adoption a convention like
		 * "cmts:<ipaddress>" for CmtsCapableNodes
		 * registration.registerPath(NodeContext.class, getIdentifier());
		 */
	}

}
