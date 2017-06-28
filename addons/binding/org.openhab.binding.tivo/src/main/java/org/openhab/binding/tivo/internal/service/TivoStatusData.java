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
 * @param connectionStatus enum UNKNOWN= test not run/default, OFFLINE = offline, TESTFAIL = both connection tests
 *            failed, STANDBY = TiVo is in standby (write test only passed), ONLINE = Online, both connection tests
 *            passed
 */

public class TivoStatusData {
    private boolean cmdOk = false;
    private Date time = new Date();
    private int channelNum = -1;
    private String msg = "NO STATUS QUERIED YET";
    private boolean pubToUI = true;
    private boolean chScan = false;
    private ConnectionStatus connectionStatus = ConnectionStatus.UNKNOWN;

    public enum ConnectionStatus {
        UNKNOWN,
        OFFLINE,
        TESTFAIL,
        STANDBY,
        ONLINE;
    }

    public TivoStatusData(boolean cmdOk, int channelNum, String msg, boolean pubToUI,
            ConnectionStatus connectionStatus) {
        this.cmdOk = cmdOk;
        this.time = new Date();
        this.channelNum = channelNum;
        this.msg = msg;
        this.pubToUI = pubToUI;
        this.connectionStatus = connectionStatus;
    }

    public TivoStatusData(boolean cmdOk, int channelNum, String msg, boolean pubToUI) {
        this.cmdOk = cmdOk;
        this.time = new Date();
        this.channelNum = channelNum;
        this.msg = msg;
        this.pubToUI = pubToUI;
    }

    /**
     * {@link TivoStatusData} class stores the data from the last status query from the TiVo and any other errors /
     * status
     * codes.
     *
     * @param cmdOk boolean true = last command executed correctly, false = last command failed with error message
     * @param channelNum int = channel number, -1 indicates no channel received. Valid channel range 1-9999.
     * @param msg string status message from the TiVo socket
     * @param pubToUI boolean true = this status needs to be published to the UI / Thing, false = do not publish (or it
     *            already has been)
     * @param chScan boolean true = channel scan is in progress, suspend polling actions
     * @param connectionStatus enum UNKNOWN= test not run/default, OFFLINE = offline, TESTFAIL = both connection tests
     *            failed, STANDBY = TiVo is in standby (write test only passed), ONLINE = Online, both connection tests
     *            passed
     */
    @Override
    public String toString() {
        return "TivoStatusData [cmdOk=" + cmdOk + ", time=" + time + ", channelNum=" + channelNum + ", msg=" + msg
                + ", pubToUI=" + pubToUI + ", chScan=" + chScan + ", connectionStatus=" + connectionStatus + "]";
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
     * @return true = channel scanning is in progress
     */
    public void setChScan(boolean chScan) {
        this.chScan = chScan;
    }

    /**
     * {@link getChScan} get status indicating that a Channel Scan is in progress. Used to prevent any user inputs
     * breaking this process.
     * = do not publish (or it
     * already has been).
     *
     * @return true = true = channel scanning is in progress, false = normal operation
     */
    public boolean getChScan() {
        return chScan;
    }

    /**
     * {@link setConnOK} indicates the state of the connection / connection tests. Drives online/offline state of the
     * Thing and connection process.
     *
     * @param connectionStatus enum UNKNOWN= test not run/default, OFFLINE = offline, TESTFAIL = both connection tests
     *            failed, STANDBY = TiVo is in standby (write test only passed), ONLINE = Online, both connection tests
     *            passed
     */
    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    /**
     * {@link getConnOK} returns the state of the connection / connection tests. Drives online/offline state of the
     * Thing and connection process.
     *
     * @return ConnectionStatus enum UNKNOWN= test not run/default, OFFLINE = offline, TESTFAIL = both connection tests
     *         failed, STANDBY = TiVo is in standby (write test only passed), ONLINE = Online, both connection tests
     *         passed
     */
    public ConnectionStatus getConnOK() {
        return connectionStatus;
    }

}
