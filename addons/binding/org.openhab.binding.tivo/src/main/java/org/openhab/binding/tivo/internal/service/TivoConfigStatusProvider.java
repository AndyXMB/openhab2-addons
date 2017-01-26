/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.openhab.binding.tivo.internal.service;

import static org.openhab.binding.tivo.TiVoBindingConstants.CONFIG_SOCKET_TIMEOUT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.tivo.handler.TiVoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * Class variables to hold our connection out to our Tivo managed by this instance of a Thing.
 *
 * @author Jayson Kubilis - Initial contribution
 * @author Andrew Black - Updates / compilation corrections
 */

public class TivoConfigStatusProvider {
    private Socket tivoSocket = null;
    private PrintStream tivoIOSendCommand = null;
    private BufferedReader tivoIOReadStatus = null;
    private TivoStatusData tivoStatusData = null;
    private TivoConfigData tivoConfigData = null;
    private TiVoHandler tivoHandler = null;
    private Logger logger = LoggerFactory.getLogger(TivoConfigStatusProvider.class);
    private Boolean pausePolling = false;

    /**
     * Instantiates a new tivo config status provider.
     *
     * @param tivoConfigData the tivo config data
     * @param tivoStatusData the tivo status data
     * @param tivoHandler the tivo handler
     */
    public TivoConfigStatusProvider(TivoConfigData tivoConfigData, TivoStatusData tivoStatusData,
            TiVoHandler tivoHandler, Boolean pausePolling) {
        this.tivoStatusData = tivoStatusData;
        this.tivoConfigData = tivoConfigData;
        this.tivoHandler = tivoHandler;
        this.pausePolling = pausePolling;
    }

    /**
     * Do refresh device status.
     *
     * @return the tivo status data
     */
    public TivoStatusData doRefreshDeviceStatus() {

        /*
         * Checks the current status data in the tivoStatusData, attempts connection to the Tivo
         * to retrieve the current status. Updates the status with valid (useful) updated values or retains the
         * existing
         * state data
         *
         * TiVo only shows the result of a command for 2-3 seconds after connection or a command is issued, else it
         * displays COMMAND_TIMEOUT (in reality waiting for next command) or NULL when this value has been read etc.
         */

        if (!pausePolling) {

            if (tivoStatusData != null) {
                logger.debug(" While refreshing thing '{}' - existing status data FOUND in tivoStatusData class - '{}'",
                        tivoConfigData.getCfgIdentifier(), tivoStatusData.toString());
                if (tivoStatusData.isCmdOk()) {

                    // We need to do something here to update valid channel states after the initial connection
                    // these do not currently get populated and because the channel numbers do not get initialised
                    // all subsequent channel changes do not work correctly

                    if (tivoStatusData.getPubToUI()) {
                        tivoHandler.updateTivoStatus(tivoStatusData);
                    }

                }

            } else {
                logger.debug(" While refreshing thing '{}' - existing status data NOT FOUND in tivoStatusData class",
                        tivoConfigData.getCfgIdentifier());
            }

            TivoStatusData myTivo = getTivoStatus();

            if (myTivo != null) {
                logger.debug(" While refreshing thing '{}' - status data FOUND in CURRENT CHECK - '{}'",
                        tivoConfigData.getCfgIdentifier(), myTivo.toString());

            } else {
                logger.debug(" While refreshing thing '{}' - status data NOT FOUND in CURRENT CHECK!",
                        tivoConfigData.getCfgIdentifier());
            }

            if (myTivo != null && myTivo.isCmdOk()) {
                // We found valid TiVo status values, updating
                tivoHandler.setTivoOnline();
                tivoStatusData = myTivo;

                tivoHandler.updateTivoStatus(tivoStatusData);

            } else {
                logger.debug(
                        " While refreshing thing '{}' - TiVo returned no new status data, the existing stored status will be retained",
                        tivoConfigData.getCfgIdentifier());
            }
            return tivoStatusData;
        }
        logger.debug(
                " While refreshing thing '{}' - status polling actions are PAUSED/SKIPPED, while we execute a command",
                tivoConfigData.getCfgIdentifier());
        return tivoStatusData;
    }

    /**
     * Sends a command to the Tivo.
     *
     * @param pCmd the cmd
     * @return the tivo status data
     */
    public TivoStatusData setTivoCommand(String pCmd) {

        boolean oC = false; // Tracks if this step caused a connection to be opened
        TivoStatusData tmp = null;
        pausePolling = true;

        // Open a connection if one does not already exist
        if (!getTivoConnected()) {
            setTivoConnectRetry(true);
            oC = true;
        }

        tmp = setTivoCommandRetry(pCmd, tivoConfigData.getCfgNumConnRetry());

        // Will close the connection after operation if we are not keeping one open.
        if (!tivoConfigData.isCfgKeepConnOpen() && getTivoConnected() && oC) {
            setTivoConnectRetry(false);
        }
        pausePolling = false;
        return tmp;

    }

    /**
     * Sets the tivo status.
     *
     * @param pCmd the cmd
     * @param retryCount the retry count
     * @return the tivo status data
     */
    private TivoStatusData setTivoCommandRetry(String pCmd, int retryCount) {

        logger.info(" TiVo '{}' - sending command: '{}' retry @ '{}'", tivoConfigData.getCfgIdentifier(), pCmd,
                retryCount);

        if (!getTivoConnected()) {
            logger.error(" TiVo '{}' - called setTivoStatus but not connected!", tivoConfigData.getCfgIdentifier());
            return new TivoStatusData(false, -1, "NOT_CONNECTED", false, false, 0);
        }

        try {
            tivoIOSendCommand.println(pCmd.toString() + "\r");
            // flushes the command buffer AND returns true if there is a problem
            if (tivoIOSendCommand.checkError()) {
                logger.error(" TiVo '{}' - called setTivoStatus and encountered an IO error",
                        tivoConfigData.getCfgIdentifier(), tivoSocket.isConnected(), tivoSocket.isClosed());

                setTivoConnectRetry(false); // close connection
                setTivoConnectRetry(true); // open a new connection

                if (retryCount <= 0) {
                    // We have failed at life... Give up.
                    return new TivoStatusData(false, -1, "CONNECTION_RETRIES_EXHAUSTED", false, false, 0);

                } else {
                    doNappTime();
                    return setTivoCommandRetry(pCmd, retryCount - 1);
                }

            }

        } catch (Exception e) {
            logger.error(" TiVo '{}' - message send '{}' failed with error '{}'", tivoConfigData.getCfgIdentifier(),
                    pCmd.toString(), e.getMessage());

        }
        // Now read the response
        return getTivoStatus();

    }

    /**
     * {@link getTivoStatus} checks if the current connection is "Keep Open" and connected. Connect, query and close as
     * required.
     *
     * @return the tivo status
     */

    private TivoStatusData getTivoStatus() {

        //

        boolean oC = false;
        TivoStatusData rtnVal = null;

        // Will open a connection prior to operation if we are not keeping one open.
        if (!getTivoConnected() | !tivoConfigData.isCfgKeepConnOpen()) {
            setTivoConnectRetry(true);
            oC = true;
        }

        // rtnVal = getTivoStatusRetry(tivoConfigData.getCfgNumConnRetry());
        rtnVal = getTivoStatusRetry(tivoConfigData.getCfgNumConnRetry());

        // Will close the connection after operation if we are not keeping one open.
        if (!tivoConfigData.isCfgKeepConnOpen() && getTivoConnected() && oC) {
            setTivoConnectRetry(false);
        }

        // Do the needful.
        return rtnVal;
    }

    /**
     * {@link getTivoStatus} queries the TiVo device for the current status and parses the resultant data.
     * <p>
     *
     * When a <b>new connection</b> is made,Tivo displays the current channel in the format 'CH_STATUS channel reason'
     * or 'CH_STATUS channel sub-channel reason' e.g CH_STATUS 0110 LOCAL.
     * <p>
     *
     * When a command is executed, the system either returns a new CH_STATUS message or any error. Most commands do not
     * return successful status codes (with the exception of a channel change).
     * <p>
     *
     * Message strings do not have valid newline (/n) terminators.
     *
     * @param retryCount integer, number of times a message retrieval will be attempted if a communication error occurs
     * @return TivoStatusData object containing the result of the connection to the DVR.
     */
    private TivoStatusData getTivoStatusRetry(int retryCount) {

        // Queries the TiVo device for the current status. Status of previous command is displayed for 3-4 seconds.
        // COMMAND_TIMEOUT is then displayed (equivalent to 'waiting for next command').

        logger.debug(" TiVo '{}' - reading from TiVo (retry @ '{}')", tivoConfigData.getCfgIdentifier(), retryCount);

        String line = null;
        String tivoStatus = null;

        // If were not connected we can't provide a status
        if (!getTivoConnected()) {
            logger.error(" TiVo '{}' - called getTivoStatus but not connected!", tivoConfigData.getCfgIdentifier());
            return new TivoStatusData(false, -1, "NOT_CONNECTED", false, false, 0);
        }

        // tivoIOReadStatus.ready() is not reliable indicator that the data stream is ready to be read
        try {
            while ((line = tivoIOReadStatus.readLine()) != null) {
                logger.debug(" TiVo handler read: '{}'", line);
                if (!line.isEmpty() && !line.trim().equalsIgnoreCase("COMMAND_TIMEOUT")) {
                    // use this line
                    tivoStatus = line;
                } else {
                    logger.debug(" TiVo handler ignored line: '{}'", line);
                }

            }
        } catch (IOException e) {
            logger.debug(" TiVo '{}' - I/O exception: '{}'", tivoConfigData.getCfgIdentifier(), e);
            if (e.getMessage().equalsIgnoreCase("READ TIMED OUT")) {
                logger.debug(
                        " TiVo '{}' - I/O exception: 'Read timed out' is expected/normal when there is nothing else to read from the stream",
                        tivoConfigData.getCfgIdentifier());

            } else if (e.getMessage().equalsIgnoreCase("Connection reset")) {
                logger.error(" TiVo '{}' - CONNECTION_RESET! '{}'", tivoConfigData.getCfgIdentifier(), e.getMessage());

                setTivoConnectRetry(false); // close connection
                setTivoConnectRetry(true); // open a new connection

                if (retryCount <= 0) {
                    // We have failed at life... Give up.
                    tivoStatus = "";
                    return new TivoStatusData(false, -1, "CONNECTION_RETRIES_EXHAUSTED", false, false, 0);
                } else {
                    doNappTime();
                    return getTivoStatusRetry(retryCount - 1);
                }
            } else {
                logger.error(" TiVo '{}' - couldn't read from the tivo - '{}'", tivoConfigData.getCfgIdentifier(),
                        e.getMessage());
            }
        }

        return parseStatus(tivoStatus);
    }

    /**
     * {@link getParsedStatus} processes the status message returned from the TiVo. Calls {@link getParsedChannel} and
     * returns the channel number (if a match is found in a valid formatted msg).
     *
     * @param strTivoStatus string representing the message text
     * @return TivoStatusData with updated status message
     */

    private TivoStatusData parseStatus(String strTivoStatus) {

        logger.debug(" TiVo '{}' - getParsedStatus running on string '{}'", tivoConfigData.getCfgIdentifier(),
                strTivoStatus);

        if (strTivoStatus == null) {
            return new TivoStatusData(false, -1, "NO_STATUS_DATA_RETURNED", false, false, 2);
        } else {
            switch (strTivoStatus) {
                // COMMAND_TIMEOUT (more accurately waiting for next command) status is dropped earlier in the process
                // and returns ""
                case "":
                    return new TivoStatusData(false, -1, "NO_STATUS_DATA_RETURNED", false, false,
                            tivoStatusData.getConnOK());
                case "CH_FAILED NO_LIVE":
                    return new TivoStatusData(false, -1, "CH_FAILED NO_LIVE", true, false, 2);
                case "CH_FAILED RECORDING":
                    return new TivoStatusData(false, -1, "CH_FAILED INVALID_CHANNEL", true, false, 2);
                case "CH_FAILED MISSING_CHANNEL":
                    return new TivoStatusData(false, -1, "CH_FAILED MISSING_CHANNEL", true, false, 2);
                case "CH_FAILED MALFORMED_CHANNEL":
                    return new TivoStatusData(false, -1, "CH_FAILED NO_LIVE", true, false, 2);
                case "CH_FAILED INVALID_CHANNEL":
                    return new TivoStatusData(false, -1, "CH_FAILED INVALID_CHANNEL", true, false, 2);
                case "LIVETV_READY":
                    return new TivoStatusData(true, -1, "LIVETV_READY", true, false, 2);
                case "INVALID_COMMAND":
                    return new TivoStatusData(false, -1, "INVALID_COMMAND", false, false, 2);
                case "CONNECTION_RETRIES_EXHAUSTED":
                    return new TivoStatusData(false, -1, "CONNECTION_RETRIES_EXHAUSTED", false, false, 2);

            }
        }

        // Only other documented status is in the form 'CH_STATUS channel reason' or 'CH_STATUS channel sub-channel
        // reason'

        // Pattern tivoStatusPattern = Pattern.compile("[0]+(\\d+\\s*\\d*)");
        Pattern tivoStatusPattern = Pattern.compile("[0]+(\\d+)\\s+");
        Matcher matcher = tivoStatusPattern.matcher(strTivoStatus);
        Integer chNum = -1; // -1 used globally to indicate channel number error

        if (matcher.find()) {
            logger.debug(" TiVo '{}' - getParsedChannel groups '{}' with group count of '{}'",
                    tivoConfigData.getCfgIdentifier(), matcher.group(), matcher.groupCount());
            // The first set of numbers is always the channel number i.e. CH_STATUS channel sub-channel reason
            if (matcher.groupCount() == 1 | matcher.groupCount() == 2) {
                chNum = new Integer(Integer.parseInt(matcher.group(1).trim()));
            }
            logger.debug(" TiVo '{}' - getParsedStatus parsed channel '{}'", tivoConfigData.getCfgIdentifier(), chNum);
            return new TivoStatusData(true, chNum, strTivoStatus, true, false, 2);
        }

        logger.warn(" TiVo '{}' - Unhandled / unexpected status message recieved: '{}'",
                tivoConfigData.getCfgIdentifier(), strTivoStatus);
        return new TivoStatusData(false, -1, strTivoStatus, false, false, tivoStatusData.getConnOK());
    }

    /**
     * {@link getParsedChannel} parses the status message returned from the TiVo and returns the channel number found.
     * Expects message in the format 'CH_STATUS channel reason' or 'CH_STATUS channel sub-channel reason' e.g. CH_STATUS
     * 0101 LOCAL
     *
     * @param strTivoStatus the str tivo status
     * @return Integer representing the channel number between 1 and 9999 or null (no match)
     */

    /**
     * {@link getTivoConnected} returns the connection state of the Socket connection object to the TiVo.
     *
     * @return true = connection exists, false = connection does not exist
     *
     */

    public boolean getTivoConnected() {
        if (tivoSocket == null) {
            logger.debug("'{}' - getTivoConnected: tivoSocket=null", tivoConfigData.getCfgIdentifier());
            return false;
        } else if (tivoSocket != null && !tivoSocket.isConnected()) {
            logger.debug("'{}' - getTivoConnected: tivoSocket.isConnected=false", tivoConfigData.getCfgIdentifier());
            return false;
        } else if (tivoSocket != null && tivoSocket.isClosed()) {
            logger.debug("'{}' - getTivoConnected: tivoSocket.isClosed=true", tivoConfigData.getCfgIdentifier());
            return false;
        } else if (tivoIOSendCommand == null) {
            logger.debug("'{}' - getTivoConnected: tivoIOSendCommand=null", tivoConfigData.getCfgIdentifier());
            return false;
        } else if (tivoIOReadStatus == null) {
            logger.debug("'{}' - getTivoConnected: tivoIOReadStatus=null", tivoConfigData.getCfgIdentifier());
            return false;
        }

        // As best we can tell here, the system is connected
        return true;
    }

    /**
     * {@link setTivoConnectRetry} opens and closes the Socket connection to the DVR. Creates a BufferedReader (Input)
     * {@code tivoIOReadStatus} to read the responses from the TiVo and a PrintStream (Output) {@code tivoIOSendCommand}
     * to send commands to the device.
     *
     * @param pConnect true = make a new connection , false = close existing connection
     * @return true = connected, false = not connected
     */

    private boolean setTivoConnectRetry(boolean pConnect) {

        if (pConnect == false) {
            // Disconnect, no retry required
            setTivoConnect(pConnect);

        } else {
            for (int iL = 1; iL < tivoConfigData.getCfgNumConnRetry(); iL++) {
                logger.debug(" Connecting '{}' - starting connection process '{}' of '{}'.",
                        tivoConfigData.getCfgIdentifier(), iL, tivoConfigData.getCfgNumConnRetry());
                if (setTivoConnect(pConnect)) {

                    logger.debug(" Connecting '{}' - Socket created / connection made.",
                            tivoConfigData.getCfgIdentifier());
                    if (testConnection()) {
                        logger.debug(" Connecting '{}' - while connecting, test command suceeded, we are ONLINE.",
                                tivoConfigData.getCfgIdentifier());
                        return true;
                    } else {
                        logger.debug(" Connecting '{}' - while connecting, test command failed, we are OFFLINE.",
                                tivoConfigData.getCfgIdentifier());
                    }

                } else {
                    logger.debug(" Connecting '{}' - Socket dissconnected. ", tivoConfigData.getCfgIdentifier());
                }
                doNappTime();
            }
        }
        return false;
    }

    public boolean setTivoConnect(boolean pConnect) {

        // DISCONNECT
        if (!pConnect && getTivoConnected()) {
            logger.debug(" Connecting '{}' - requested to disconnect", tivoConfigData.getCfgIdentifier());
            try {
                tivoIOReadStatus.close();
                tivoIOSendCommand.close();
                tivoSocket.close();
                tivoSocket = null;

            } catch (IOException e) {
                logger.error(" Connecting '{}' - I/O exception while disconnecting: '{}'.  Connection closed.",
                        tivoConfigData.getCfgIdentifier(), e.getMessage());
            }
            return false;

        } else if (!pConnect) {
            logger.debug(" Connecting '{}' -  requested to disconnect, but already not connected.  Ignoring.",
                    tivoConfigData.getCfgIdentifier());
            return false;

        } else {
            // CONNECT
            if (getTivoConnected()) {
                logger.debug(" Connecting '{}' - requested to connect while aready connected. Ignoring.",
                        tivoConfigData.getCfgIdentifier());
                return true;
            }

            logger.debug(" Connecting '{}' - setTivoConnectRetry attempting connection to host '{}', port '{}'",
                    tivoConfigData.getCfgIdentifier(), tivoConfigData.getCfgHost(), tivoConfigData.getCfgTcpPort());

            try {
                tivoSocket = new Socket(tivoConfigData.getCfgHost(), tivoConfigData.getCfgTcpPort());
                tivoSocket.setKeepAlive(true);
                tivoSocket.setSoTimeout(CONFIG_SOCKET_TIMEOUT);
                tivoSocket.setReuseAddress(true);

                if (tivoSocket.isConnected() && !tivoSocket.isClosed()) {
                    tivoIOReadStatus = new BufferedReader(new InputStreamReader(tivoSocket.getInputStream()));
                    tivoIOSendCommand = new PrintStream(tivoSocket.getOutputStream(), false);
                }
                return true;

            } catch (UnknownHostException e) {
                logger.error(" Connecting '{}' - while connecting, unexpected host error: '{}'",
                        tivoConfigData.getCfgIdentifier(), e.getMessage());
            } catch (IOException e) {
                logger.error(" Connecting '{}' - I/O exception while connecting: '{}'",
                        tivoConfigData.getCfgIdentifier(), e.getMessage());
            }
            return false;
        }
    }

    /**
     *
     * @param srchStatus
     * @return
     */

    private boolean testConnection() {

        boolean getOk = false;
        boolean sendOk = false;

        // When we connect we should get the current channel status CH_S
        logger.debug(" Connecting '{}' - testing initial Channel status.", tivoConfigData.getCfgIdentifier());
        getOk = readStatusMatch(readStatus(), "CH_STATUS", true);
        if (!getOk) {
            logger.debug(" Connecting '{}' - testing read of initial channel state FAILED",
                    tivoConfigData.getCfgIdentifier());
        }

        logger.debug(" Connecting '{}' - testing dummy write command 'DUMMY_CMD'.", tivoConfigData.getCfgIdentifier());

        // Send DUMMY command to see if the connection is really working (only reliable way)
        tivoIOSendCommand.print("DUMMY_CMD" + "\r");
        tivoIOSendCommand.flush();
        // flushes the command to the Tivo and checks for any error returned
        if (tivoIOSendCommand.checkError()) {
            logger.debug(" Connecting '{}' - testing dummy command failed.  Unable to send.",
                    tivoConfigData.getCfgIdentifier());
            sendOk = false;
        }

        // now see what we got back
        doNappTime();
        sendOk = readStatusMatch(readStatus(), "INVALID_COMMAND", false);
        if (!sendOk) {
            logger.debug(" Connecting '{}' - testing of dummy write command FAILED", tivoConfigData.getCfgIdentifier());
        }

        if (getOk & sendOk) {
            tivoStatusData.setConnOK(2);
            tivoHandler.setTivoOnline();
            return true;
        } else {
            tivoStatusData.setConnOK(1);
            return false;
        }
    }

    private ArrayList<TivoStatusData> readStatus() {

        ArrayList<String> msgArr = new ArrayList<String>();
        ArrayList<TivoStatusData> statusArr = new ArrayList<TivoStatusData>();
        String line = null;
        int iL = 0;

        try {
            while ((line = tivoIOReadStatus.readLine()) != null) {
                msgArr.add(line);
                logger.debug(" Connecting '{}' - while connecting read [{}]: '{}'", tivoConfigData.getCfgIdentifier(),
                        iL, msgArr.get(iL));
                iL++;
            }
        } catch (IOException e) {
            logger.debug(" Connecting '{}' - I/O exception while connecting: '{}'", tivoConfigData.getCfgIdentifier(),
                    e.getMessage());

        }

        if (msgArr.size() > 0)

        {
            for (iL = 0; iL < msgArr.size(); iL++) {
                statusArr.add(parseStatus(msgArr.get(iL)));
                logger.debug(" Connecting '{}' - testing command returned status: {}.",
                        tivoConfigData.getCfgIdentifier(), statusArr.get(iL));
            }
        }
        return statusArr;

    }

    private boolean readStatusMatch(ArrayList<TivoStatusData> statusArr, String strMatch, boolean updateStatus) {

        for (int i = 0; i < statusArr.size(); i++) {
            if (statusArr.get(i).getMsg().contains(strMatch)) {
                if (updateStatus) {
                    tivoStatusData = statusArr.get(i);
                }
                return true;
            }
        }
        return false;
    }

    private void doNappTime() {
        try {
            logger.debug("   TiVo '{}' - I feel like napping for '{}' milliseconds", tivoConfigData.getCfgIdentifier(),
                    tivoConfigData.getCfgCmdWait());
            TimeUnit.MILLISECONDS.sleep(tivoConfigData.getCfgCmdWait());
        } catch (Exception e) {
        }
    }

}
