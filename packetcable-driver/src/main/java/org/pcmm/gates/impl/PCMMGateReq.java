/**
 @header@
 */
package org.pcmm.gates.impl;

import org.pcmm.base.IPCMMBaseObject;
import org.pcmm.gates.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * <p>
 * <Gate-set>=<Decision Header><TransactionID><AMID> <SubscriberID> [<GateI>]
 * <GateSpec> <Traffic Profile> <classifier>
 * </p>
 */
public class PCMMGateReq implements IPCMMGate {

    private final static Logger logger = LoggerFactory.getLogger(PCMMGateReq.class);

    private IGateID gateID;
    private IAMID iamid;
    private IPCMMError error;
    private ISubscriberID subscriberID;
    private ITransactionID transactionID;
    private IGateSpec gateSpec;
    private ITrafficProfile trafficProfile;
    private IClassifier classifier;

    public PCMMGateReq() {
        logger.info("New PCMMGateReq");
    }

    public PCMMGateReq(final byte[] data) {
        this();
        short offset = 0;
        while (offset + 5 < data.length) {
            short len = 0;
            len |= ((short) data[offset]) << 8;
            len |= ((short) data[offset + 1]) & 0xFF;
            final byte sNum = data[offset + 2];
            final byte sType = data[offset + 3];
            byte[] dataBuffer = Arrays.copyOfRange(data, offset, offset + len);
            switch (sNum) {
            case IGateID.SNUM:
                setGateID(new GateID(dataBuffer));
                break;
            case IAMID.SNUM:
                setAMID(new AMID(dataBuffer));
                break;
            case ISubscriberID.SNUM:
                setSubscriberID(new SubscriberID(dataBuffer));
                break;
            case ITransactionID.SNUM:
                setTransactionID(new TransactionID(dataBuffer));
                break;
            case IGateSpec.SNUM:
                setGateSpec(new GateSpec(dataBuffer));
                break;
            case ITrafficProfile.SNUM:
                setTrafficProfile(new BestEffortService(dataBuffer));
                break;
            case IClassifier.SNUM:
                setClassifier(new Classifier(dataBuffer));
                break;
            case IPCMMError.SNUM:
                error = new PCMMError(dataBuffer);
                break;
            default:
                logger.error("unhandled Object skept : S-NUM=" + sNum
                                   + "  S-TYPE=" + sType + "  LEN=" + len);
            }
            offset += len;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.pcmm.gates.IPCMMGate#isMulticast()
     */
    @Override
    public boolean isMulticast() {
        // TODO - determine how to deal with this
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.pcmm.gates.IPCMMGate#setGateID(short)
     */
    @Override
    public void setGateID(final IGateID gateid) {
        this.gateID = gateid;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.pcmm.gates.IPCMMGate#setAMID(org.pcmm.gates.IAMID)
     */
    @Override
    public void setAMID(final IAMID iamid) {
        this.iamid = iamid;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.pcmm.gates.IPCMMGate#getSubscriberID(org.pcmm.gates.ISubscriberID)
     */
    @Override
    public void setSubscriberID(final ISubscriberID subscriberID) {
        this.subscriberID = subscriberID;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.pcmm.gates.IPCMMGate#getGateSpec(org.pcmm.gates.IGateSpec)
     */
    @Override
    public void setGateSpec(final IGateSpec gateSpec) {
        this.gateSpec = gateSpec;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.pcmm.gates.IPCMMGate#getClassifier(org.pcmm.gates.IClassifier)
     */
    @Override
    public void setClassifier(final IClassifier classifier) {
        this.classifier = classifier;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.pcmm.gates.IPCMMGate#getTrafficProfile(org.pcmm.gates.ITrafficProfile
     * )
     */
    @Override
    public void setTrafficProfile(final ITrafficProfile profile) {
        this.trafficProfile = profile;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.pcmm.gates.IPCMMGate#getGateID()
     */
    @Override
    public IGateID getGateID() {
        return gateID;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.pcmm.gates.IPCMMGate#getAMID()
     */
    @Override
    public IAMID getAMID() {
        return iamid;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.pcmm.gates.IPCMMGate#getSubscriberID()
     */
    @Override
    public ISubscriberID getSubscriberID() {
        return subscriberID;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.pcmm.gates.IPCMMGate#getGateSpec()
     */
    @Override
    public IGateSpec getGateSpec() {
        return gateSpec;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.pcmm.gates.IPCMMGate#getClassifier()
     */
    @Override
    public IClassifier getClassifier() {
        return classifier;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.pcmm.gates.IPCMMGate#getTrafficProfile()
     */
    @Override
    public ITrafficProfile getTrafficProfile() {
        return trafficProfile;
    }

    @Override
    public void setTransactionID(final ITransactionID transactionID) {
        this.transactionID = transactionID;
    }

    @Override
    public ITransactionID getTransactionID() {
        return transactionID;
    }

    public IPCMMError getError() {
        return error;
    }

    public void setError(IPCMMError error) {
        this.error = error;
    }

    @Override
    public byte[] getData() {
        logger.info("Retrieving data");
        byte[] array = new byte[0];
        if (getTransactionID() != null) {
            array = fill(array, getTransactionID());
        }
        if (getGateID() != null) {
            array = fill(array, getGateID());
        }
        if (getAMID() != null) {
            array = fill(array, getAMID());

        }
        if (getSubscriberID() != null) {
            array = fill(array, getSubscriberID());
        }
        if (getGateSpec() != null) {
            array = fill(array, getGateSpec());
        }
        if (getTrafficProfile() != null) {
            array = fill(array, getTrafficProfile());
        }
        if (getClassifier() != null) {
            array = fill(array, getClassifier());
        }
        return array;
    }

    private byte[] fill(byte[] array, IPCMMBaseObject obj) {
        byte[] a = obj.getAsBinaryArray();
        int offset = array.length;
        array = Arrays.copyOf(array, offset + a.length);
        System.arraycopy(a, 0, array, offset, a.length);
        return array;
    }
}
