/*
 * Copyright (c) 2004 University of Murcia.  All rights reserved.
 * --------------------------------------------------------------
 * For more information, please see <http://www.umu.euro6ix.org/>.
 */

package org.umu.cops.prpep;

import org.umu.cops.prpdp.COPSDataProcess;

import java.util.Map;

/**
 * COPSPepDataProcess process policy data and events.
 *
 * @version COPSPepDataProcess.java, v 2.00 2004
 *
 */
public interface COPSPepDataProcess extends COPSDataProcess {

    /**
     * Establish PDP decisions
     *
     * @param removeDecs - the remove decisions
     * @param installDecs - the install decisions
     * @param errorDecs - the error decisions
     */
	public void setDecisions(COPSPepReqStateMan man, Map<String, String> removeDecs, Map<String, String> installDecs,
                             Map<String, String> errorDecs);

    /**
     *  If the report is fail, return true
     * @return - true if there is a failure report
     */
    public boolean isFailReport(COPSPepReqStateMan man);

    /**
     * Return Report Data
     * @return - the report data
     */
    public Map<String, String> getReportData(COPSPepReqStateMan man);

    /**
     * Return Client Data
     * @return - the client data
     */
    public Map<String, String> getClientData(COPSPepReqStateMan man);

    /**
     * Return Accounting Data
     * @return - the accounting data
     */
    public Map<String, String> getAcctData(COPSPepReqStateMan man);

    /**
     * Process a PDP request to open a new Request State
     *
     * @param man - the PEP request state manager
     */
    public void newRequestState(COPSPepReqStateMan man);
}

