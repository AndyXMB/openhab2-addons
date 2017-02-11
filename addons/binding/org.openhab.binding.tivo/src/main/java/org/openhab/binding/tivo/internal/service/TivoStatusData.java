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
import java.util.Observable;

/**
 * {@link TivoStatusData} class stores the data from the last status query from the TiVo and any other errors / status
 * codes.
 *
 * @param cmdOk boolean true = last command executed correctly, false = last command failed with error message
 * @param channelNum int = channel number, -1 indicates no channel received. Valid channel range 1-9999.
 * @param msg string status message from the TiVo socket
 * @param pubToUI boolean true = this status needs to be published to the UI / Thing, false = do not publish (or it
 *            already has been)
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
 * @param chScan boolean true = channel scan is in progress
 * @param connectionStatus ConnectionStatus enum UNKNOWN= test not run/default, OFFLINE = offline, STANDBY = TiVo is in
 *            standby,
 *            ONLINE = Online
 *
 * @author Jayson Kubilis (DigitalBytes) - Initial contribution
 * @author Andrew Black (AndyXMB) - minor updates, removal of unused functions.
=======
 * @param chScan boolean true = channel scan is in progress, suspend polling actions
 * @param connOK int -1= test not run/default, 0 = offline, 1 = both connection tests failed, 2 = TiVo is in standby
 *            (write test only passed), 3 = Online, both connection tests passed
 *
>>>>>>> 73f9dc6 Initial Commit
 */

public class TivoStatusData extends Observable {
    private boolean cmdOk = false;
    private Date time = new Date();
    private int channelNum = -1;
    private String msg = "NO STATUS QUERIED YET";
    private boolean pubToUI = true;
    private boolean chScan = false;
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
    private ConnectionStatus connectionStatus = ConnectionStatus.UNKNOWN;
=======
    private int connOK = -1;
>>>>>>> 73f9dc6 Initial Commit

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
    public TivoStatusData(boolean cmdOk, int channelNum, String msg, boolean pubToUI,
            ConnectionStatus connectionStatus) {
=======
    public TivoStatusData(boolean cmdOk, int channelNum, String msg, boolean pubToUI, int connOK) {
>>>>>>> 73f9dc6 Initial Commit
        this.cmdOk = cmdOk;
        this.time = new Date();
        this.channelNum = channelNum;
        this.msg = msg;
        this.pubToUI = pubToUI;
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        this.connectionStatus = connectionStatus;

=======
        this.connOK = connOK;
>>>>>>> 73f9dc6 Initial Commit
    }

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
    public enum ConnectionStatus {
        UNKNOWN,
        OFFLINE,
        STANDBY,
        ONLINE;
    }

    /**
     * {@link TivoStatusData} class stores the data from the last status query from the TiVo and any other errors /
     * status codes.
=======
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
>>>>>>> 73f9dc6 Initial Commit
     *
     * @param cmdOk boolean true = last command executed correctly, false = last command failed with error message
     * @param channelNum int = channel number, -1 indicates no channel received. Valid channel range 1-9999.
     * @param msg string status message from the TiVo socket
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
     * @param pubToUI boolean true = this status needs to be published to the UI, false = do not publish (or it
     *            already has been)
     * @param chScan boolean true = channel scan is in progress
     * @param connectionStatus enum UNKNOWN= test not run/default, OFFLINE = offline, STANDBY = TiVo is in standby
     *            , ONLINE = Online
=======
     * @param pubToUI boolean true = this status needs to be published to the UI / Thing, false = do not publish (or it
     *            already has been)
     * @param chScan boolean true = channel scan is in progress, suspend polling actions
     * @param connOK int -1= test not run/default, 0 = offline, 1 = both connection tests failed, 2 = TiVo is in standby
     *            (write test only passed), 3 = Online, both connection tests passed
>>>>>>> 73f9dc6 Initial Commit
     */
    @Override
    public String toString() {
        return "TivoStatusData [cmdOk=" + cmdOk + ", time=" + time + ", channelNum=" + channelNum + ", msg=" + msg
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
                + ", pubToUI=" + pubToUI + ", chScan=" + chScan + ", connectionStatus=" + connectionStatus + "]";
=======
                + ", pubToUI=" + pubToUI + ", chScan=" + chScan + ", connOK=" + connOK + "]";
>>>>>>> 73f9dc6 Initial Commit
    }

    /**
     * {@link isCmdOK} indicates if the last command executed correctly.
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
     * {@link getMsg} gets status message string
     *
     * @return msg string
     */
    public String getMsg() {
        return msg;
    }

    /**
     * {@link setPubToUI} set to true if this status needs to be published to the channel / UI / Thing, false = do not
     * publish (or it already has been).
     *
     * @param pubToUI true = publish status to the channel objects
     */
    public void setPubToUI(boolean pubToUI) {
        this.pubToUI = pubToUI;
    }

    /**
     * {@link getPubToUI} get status indicating that the event needs to be published to the channel / UI / Thing, false
     * = do not publish (or it already has been).
     *
     * @return pubToUI true = publish status to the channel objects
     */
    public boolean getPubToUI() {
        return pubToUI;
    }

    /**
     * {@link setChScan} set to true if a Channel Scan is in progress. Used to prevent any user inputs breaking this
     * process.
     *
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
     * @param chScan boolean true = channel scanning is in progress, false = normal operation
=======
     * @return true = channel scanning is in progress
>>>>>>> 73f9dc6 Initial Commit
     */
    public void setChScan(boolean chScan) {
        this.chScan = chScan;
    }

    /**
     * {@link isChannelScanInProgress} get status indicating that a Channel Scan is in progress. Used to prevent any
     * user inputs breaking this process.
     *
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
     * @return chScan boolean true = channel scanning is in progress, false = normal operation
=======
     * @return true = true = channel scanning is in progress, false = normal operation
>>>>>>> 73f9dc6 Initial Commit
     */
    public boolean isChannelScanInProgress() {
        return chScan;
    }

    /**
     * {@link setConnectionStatus} indicates the state of the connection / connection tests. Drives online/offline state
     * of the
     * Thing and connection process.
     *
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
     * @param connectionStatus enum UNKNOWN= test not run/default, OFFLINE = offline, STANDBY = TiVo is in standby,
     *            ONLINE = Online
=======
     * @param connOK int -1= defaut/not set, 0 = offline, 1 = both connection tests failed, 2 = TiVo is in standby
     *            (write test only passed), 3 = Online, both connection tests passed
>>>>>>> 73f9dc6 Initial Commit
     */
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
=======
    public void setConnOK(int connOK) {
        this.connOK = connOK;
>>>>>>> 73f9dc6 Initial Commit
    }

    /**
     * {@link getConnectionStatus} returns the state of the connection / connection tests. Drives online/offline state
     * of the
     * Thing and connection process.
     *
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
     * @return ConnectionStatus enum UNKNOWN= test not run/default, OFFLINE = offline, STANDBY = TiVo is in standby,
     *         ONLINE = Online
=======
     * @return int -1= defaut/not set, 0 = offline, 1 = both connection tests failed, 2 = TiVo is in standby
     *         (write test only passed), 3 = Online, both connection tests passed
>>>>>>> 73f9dc6 Initial Commit
     */
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

}
