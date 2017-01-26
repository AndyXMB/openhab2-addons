/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.openhab.binding.tivo.internal.service;

import java.util.Date;

/**
 * {@link TivoStatusData} class stores the data from the last status query from the TiVo and any other errors / status
 * codes.
 *
 * @param cmdOk boolean true = last command executed correctly, false = last command failed with error message
 * @param channelNum int = channel number, -1 indicates no channel received. Valid channel range 1-9999.
 * @param msg string status message from the TiVo socket
 * @param pubToUI boolean true = this status needs to be published to the UI / Thing, false = do not publish (or it
 *            already has been)
 * @param chScan boolean true = channel scan is in progress, suspend polling actions
 * @param connOK int 0 = offline, 1 = last connection test failed, 2 = connection test passed
 */

public class TivoStatusData {
    private boolean cmdOk = false;
    private Date time = new Date();
    private int channelNum = -1;
    private String msg = "NO STATUS QUERIED YET";
    private boolean pubToUI = true;
    private boolean chScan = false;
    private int connOK = 0;

    public TivoStatusData(boolean cmdOk, int channelNum, String msg, boolean pubToUI, boolean chScan, int connOK) {
        this.cmdOk = cmdOk;
        this.time = new Date();
        this.channelNum = channelNum;
        this.msg = msg;
        this.pubToUI = pubToUI;
        this.chScan = chScan;
        this.connOK = connOK;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "TivoStatusData [cmdOk=" + cmdOk + ", time=" + time + ", channelNum=" + channelNum + ", msg=" + msg
                + ", pubToUI=" + pubToUI + ", chScan=" + chScan + "]";
    }

    /**
     * Get {@link isCmdOK} indicates if the last command executed correctly.
     *
     * @return cmdOk boolean true = executed correctly, false = last command failed with error message
     */
    public boolean isCmdOk() {
        return cmdOk;
    }

    /**
     * {@link} sets the value indicating if the last command executed correctly.
     *
     * @param cmdOk boolean true = executed correctly, false = last command failed with error message
     */
    public void setCmdOk(boolean cmdOk) {
        this.cmdOk = cmdOk;
    }

    /**
     * {@link getTime} returns the date / time of the last status message update
     *
     * @return Date
     * @see Date
     */
    public Date getTime() {
        return time;
    }

    /**
     * {@link getChannelNum} gets the channel number, -1 indicates no channel received. Valid channel range 1-9999.
     *
     * @return the channel number
     */
    public int getChannelNum() {
        return channelNum;
    }

    /**
     * {@link setChannelNum} sets the channel number, -1 indicates no channel received. Valid channel range 1-9999.
     *
     * @param channelNum the new channel number
     */
    public void setChannelNum(int channelNum) {
        this.channelNum = channelNum;
    }

    /**
     * {@link getMsg} gets msg string status message from the TiVo socket
     *
     * @return the msg
     */
    public String getMsg() {
        return msg;
    }

    /**
     * {@link setMsg} sets msg string status message from the TiVo socket
     *
     * @param msg the new msg
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * {@link setPubToUI} set to true if this status needs to be published to the channel / UI / Thing, false = do not
     * publish (or it
     * already has been).
     *
     * @param pubToUI true = publish status to the channel objects
     */
    public void setPubToUI(boolean pubToUI) {
        this.pubToUI = pubToUI;
    }

    /**
     * {@link getPubToUI} get status indicating that the event needs to be published to the channel / UI / Thing, false
     * = do not publish (or it
     * already has been).
     *
     * @param pubToUI true = publish status to the channel objects
     */
    public boolean getPubToUI() {
        return pubToUI;
    }

    /**
     * {@link setchScan} set to true if a Channel Scan is in progress. Used to prevent any user inputs breaking this
     * process.
     * publish (or it
     * already has been).
     *
     * @param chScan true = channel scanning is in progress
     */
    public void setChScan(boolean chScan) {
        this.chScan = chScan;
    }

    /**
     * {@link getchScan} get status indicating that a Channel Scan is in progress. Used to prevent any user inputs
     * breaking this process.
     * = do not publish (or it
     * already has been).
     *
     * @param chScan true = true = channel scanning is in progress, false = normal operation
     */
    public boolean getChScan() {
        return chScan;
    }

    /**
     * {@link setConnOK} indicates the state of the connection / connection tests. Drives online/offline state of the
     * Thing and connection process.
     *
     * @param 0 = offline, 1 = last connection test failed, 2 = connection test passed
     */
    public void setConnOK(int connOK) {
        if (connOK != -1) {
            this.connOK = connOK;
        }
    }

    /**
     * {@link getConnOK} returns the state of the connection / connection tests. Drives online/offline state of the
     * Thing and connection process.
     *
     * @param 0 = offline, 1 = last connection test failed, 2 = connection test passed
     */
    public int getConnOK() {
        return connOK;
    }

}
