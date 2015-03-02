/**
 @header@
 */
package org.pcmm.objects;

/**
 * this class holds and maps flow ID to PCMM gate ID and Transaction ID
 *
 */
public class PCMMIDHolder extends PCMMResource {

    /**
     * flow id.
     */
    private final int flowID;
    /**
     * gate id.
     */
    private transient int gateID;
    /**
     * transaction id.
     */
    private final short transactionID;

    public PCMMIDHolder(final int flowID, final int gateID, final short transactionID) {
        this.flowID = flowID;
        this.gateID = gateID;
        this.transactionID = transactionID;

    }

    public int getFlowID() {
        return flowID;
    }

    public void setGateID(final int gateID) {
        this.gateID = gateID;
    }

    public int getGateID() {
        return gateID;
    }

    public short getTransactionID() {
        return transactionID;
    }

}
