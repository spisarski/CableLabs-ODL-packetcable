package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.packetcable.packetcable.provider.impl.rev140131;

import org.opendaylight.controller.packetcable.provider.OpendaylightPacketcableProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketcableProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.packetcable.packetcable.provider.impl.rev140131.AbstractPacketcableProviderModule {
    private static final Logger logger = LoggerFactory.getLogger(PacketcableProviderModule.class);
    public PacketcableProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
        logger.info("Constructing new PacketcableProviderModule without old");
    }

    public PacketcableProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.packetcable.packetcable.provider.impl.rev140131.PacketcableProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
        logger.info("Constructing new PacketcableProviderModule with old");
    }

    @Override
    public void customValidation() {
        logger.info("Custom validation");
        // add custom validation form module attributes here.
    }

    /**
     * Called by ODL OSGI framework to start the process
     * @return - the OpendaylightPacketcableProvider object
     */
    @Override
    public java.lang.AutoCloseable createInstance() {
        logger.info("Creating instance");
        final OpendaylightPacketcableProvider provider = new OpendaylightPacketcableProvider();
        this.getBrokerDependency().registerProvider(provider);
        logger.info("PacketCableProvider Registered with Broker");
        return provider;
    }

}
