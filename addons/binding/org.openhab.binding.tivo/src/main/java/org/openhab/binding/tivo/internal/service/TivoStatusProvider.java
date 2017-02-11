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
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.tivo.handler.TiVoHandler;
import org.openhab.binding.tivo.internal.service.TivoStatusData.ConnectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TivoStatusProvider class to maintain a connection out to the Tivo, monitor and process status messages returned..
 *
 * @author Jayson Kubilis - Initial contribution
 * @author Andrew Black - Updates / compilation corrections
 */

public class TivoStatusProvider {

    private Socket tivoSocket = null;
    private PrintStream streamWriter = null;
    public StreamReader streamReader = null;
    private TivoStatusData tivoStatusData = null;
    private TivoConfigData tivoConfigData = null;
    private TiVoHandler tivoHandler = null;
    private final Logger logger = LoggerFactory.getLogger(TivoStatusProvider.class);

    private static final Integer READ_TIMEOUT = 1000;

    /**
     * Instantiates a new TivoConfigStatusProvider.
     *
     * @param tivoConfigData {@link TivoConfigData} configuration data for the specific thing.
     * @param tivoStatusData {@link TivoStatusData} status data for the specific thing.
     * @param tivoHandler {@link TivoHandler} parent handler object for the TivoConfigStatusProvider.
     *
     */

    public TivoStatusProvider(TivoConfigData tivoConfigData, TiVoHandler tivoHandler, Boolean pausePolling) {
        this.tivoStatusData = new TivoStatusData(false, -1, "INITIALISING", false, ConnectionStatus.UNKNOWN);
        this.tivoConfigData = tivoConfigData;
        this.tivoHandler = tivoHandler;
    }

    /**
     * {@link statusRefresh} initiates a connection to the TiVo. When a new connection is made and the TiVo is online,
     * the current channel is always returned. The connection is then closed (allows the socket to be used by other
     * devices).
     *
     * * @return {@link TivoStatusData} object
     */
    public void statusRefresh() {
        if (tivoStatusData != null) {
            logger.debug(" statusRefresh '{}' - EXISTING status data - '{}'", tivoConfigData.getCfgIdentifier(),
                    tivoStatusData.toString());
        }
        connTivoConnect();
        doNappTime();
        connTivoDisconnect(false);
    }

    /**
     * {@link cmdTivoSend} sends a command to the Tivo.
     *
     * @param tivoCommand the complete command string (KEYWORD + PARAMETERS e.g. SETCH 102) to send.
     * @return {@link TivoStatusData} status data object, contains the result of the command.
     */
    public TivoStatusData cmdTivoSend(String tivoCommand) {
        if (!connTivoConnect()) {
            return new TivoStatusData(false, -1, "CONNECTION FAILED", false, ConnectionStatus.OFFLINE);
        }
        logger.info("TiVo '{}' - sending command: '{}'", tivoConfigData.getCfgIdentifier(), tivoCommand);
        int repeatCount = 1;
        // Handle special keyboard "repeat" commands
        if (tivoCommand.contains("*")) {
            repeatCount = Integer.parseInt(tivoCommand.substring(tivoCommand.indexOf("*") + 1));
            tivoCommand = tivoCommand.substring(0, tivoCommand.indexOf("*"));
            logger.info("TiVo '{}' - repeating command: '{}' for '{}' times", tivoConfigData.getCfgIdentifier(),
                    tivoCommand, repeatCount);
        }
        for (int i = 1; i <= repeatCount; i++) {
            // Send the command
            streamWriter.println(tivoCommand.toString() + "\r");
            if (streamWriter.checkError()) {
                logger.error("TiVo '{}' - called cmdTivoSend and encountered an IO error",
                        tivoConfigData.getCfgIdentifier(), tivoSocket.isConnected(), tivoSocket.isClosed());
                connTivoReconnect();
            }
        }
        return tivoStatusData;
    }

    /**
     * {@link statusParse} processes the {@link TivoStatusData} status message returned from the TiVo.
     *
     * For channel status messages form 'CH_STATUS channel reason' or 'CH_STATUS channel sub-channel reason' calls
     * {@link getParsedChannel} and returns the channel number (if a match is found in a valid formatted message).
     *
     * @param rawStatus string representing the message text returned by the TiVo
     * @return TivoStatusData object conditionally populated based upon the raw status message
     */

    private TivoStatusData statusParse(String rawStatus) {
        logger.debug(" statusParse '{}' - running on string '{}'", tivoConfigData.getCfgIdentifier(), rawStatus);

        if (rawStatus == null) {
            return new TivoStatusData(false, -1, "NO_STATUS_DATA_RETURNED", false,
                    tivoStatusData.getConnectionStatus());
        } else if (rawStatus.contentEquals("COMMAND_TIMEOUT")) {
            return new TivoStatusData(false, -1, "COMMAND_TIMEOUT", false, tivoStatusData.getConnectionStatus());
        } else {
            switch (rawStatus) {
                case "":
                    return new TivoStatusData(false, -1, "NO_STATUS_DATA_RETURNED", false,
                            tivoStatusData.getConnectionStatus());
                case "LIVETV_READY":
                    return new TivoStatusData(true, -1, "LIVETV_READY", true, ConnectionStatus.ONLINE);
                case "CH_FAILED NO_LIVE":
                    return new TivoStatusData(false, -1, "CH_FAILED NO_LIVE", true, ConnectionStatus.STANDBY);
                case "CH_FAILED RECORDING":
                    return new TivoStatusData(false, -1, "CH_FAILED RECORDING", true, ConnectionStatus.ONLINE);
                case "CH_FAILED MISSING_CHANNEL":
                    return new TivoStatusData(false, -1, "CH_FAILED MISSING_CHANNEL", true, ConnectionStatus.ONLINE);
                case "CH_FAILED MALFORMED_CHANNEL":
                    return new TivoStatusData(false, -1, "CH_FAILED MALFORMED_CHANNEL", true, ConnectionStatus.ONLINE);
                case "CH_FAILED INVALID_CHANNEL":
                    return new TivoStatusData(false, -1, "CH_FAILED INVALID_CHANNEL", true, ConnectionStatus.ONLINE);
                case "INVALID_COMMAND":
                    return new TivoStatusData(false, -1, "INVALID_COMMAND", false, ConnectionStatus.ONLINE);
                case "CONNECTION_RETRIES_EXHAUSTED":
                    return new TivoStatusData(false, -1, "CONNECTION_RETRIES_EXHAUSTED", true,
                            ConnectionStatus.OFFLINE);
            }
        }

        // Only other documented status is in the form 'CH_STATUS channel reason' or
        // 'CH_STATUS channel sub-channel reason'
        Pattern tivoStatusPattern = Pattern.compile("[0]+(\\d+)\\s+");
        Matcher matcher = tivoStatusPattern.matcher(rawStatus);
        Integer chNum = -1; // -1 used globally to indicate channel number error

        if (matcher.find()) {
            logger.debug(" statusParse '{}' - groups '{}' with group count of '{}'", tivoConfigData.getCfgIdentifier(),
                    matcher.group(), matcher.groupCount());
            if (matcher.groupCount() == 1 | matcher.groupCount() == 2) {
                chNum = new Integer(Integer.parseInt(matcher.group(1).trim()));
            }
            logger.debug(" statusParse '{}' - parsed channel '{}'", tivoConfigData.getCfgIdentifier(), chNum);
            rawStatus = rawStatus.replace(" REMOTE", "");
            rawStatus = rawStatus.replace(" LOCAL", "");
            return new TivoStatusData(true, chNum, rawStatus, true, ConnectionStatus.ONLINE);
        }
        logger.warn(" TiVo '{}' - Unhandled/unexpected status message: '{}'", tivoConfigData.getCfgIdentifier(),
                rawStatus);
        return new TivoStatusData(false, -1, rawStatus, false, tivoStatusData.getConnectionStatus());
    }

    /**
     * {@link connIsConnected} returns the connection state of the Socket, streamWriter and streamReader objects.
     *
     * @return true = connection exists and all objects look OK, false = connection does not exist or a problem has
     *         occurred
     *
     */

    private boolean connIsConnected() {
        if (tivoSocket == null) {
            logger.debug(" connIsConnected '{}' - FALSE: tivoSocket=null", tivoConfigData.getCfgIdentifier());
            return false;
        } else if (!tivoSocket.isConnected()) {
            logger.debug(" connIsConnected '{}' - FALSE: tivoSocket.isConnected=false",
                    tivoConfigData.getCfgIdentifier());
            return false;
        } else if (tivoSocket.isClosed()) {
            logger.debug(" connIsConnected '{}' - FALSE: tivoSocket.isClosed=true", tivoConfigData.getCfgIdentifier());
            return false;
        } else if (streamWriter == null) {
            logger.debug(" connIsConnected '{}' - FALSE: tivoIOSendCommand=null", tivoConfigData.getCfgIdentifier());
            return false;
        } else if (streamWriter.checkError()) {
            logger.debug(" connIsConnected '{}' - FALSE: tivoIOSendCommand.checkError()=true",
                    tivoConfigData.getCfgIdentifier());
            return false;
        } else if (streamReader == null) {
            logger.debug(" connIsConnected '{}' - FALSE: streamReader=null", tivoConfigData.getCfgIdentifier());
            return false;
        }
        return true;
    }

    /**
     * {@link connTivoConnect} manages the creation / retry process of the socket connection.
     *
     * @return true = connected, false = not connected
     */

    public boolean connTivoConnect() {
        for (int iL = 1; iL <= tivoConfigData.getCfgNumConnRetry(); iL++) {
            logger.debug(" connTivoConnect '{}' - starting connection process '{}' of '{}'.",
                    tivoConfigData.getCfgIdentifier(), iL, tivoConfigData.getCfgNumConnRetry());

            // Sort out the socket connection
            if (connSocketConnect()) {
                logger.debug(" connTivoConnect '{}' - Socket created / connection made.",
                        tivoConfigData.getCfgIdentifier());
                if (streamReader.isAlive()) {
                    return true;
                }
            } else {
                logger.debug(" connTivoConnect '{}' - Socket creation failed.", tivoConfigData.getCfgIdentifier());
            }
            // Sleep and retry
            doNappTime();
        }
        return false;
    }

    /**
     * {@link connTivoDisconnect} conditionally closes the Socket connection. When 'keep connection open' or 'channel
     * scanning' is true, the disconnection process is ignored. Disconnect can be forced by setting forceDisconnect to
     * true.
     *
     * @param forceDisconnect true = forces a disconnection , false = disconnects in specific situations
     */

    public void connTivoDisconnect(boolean forceDisconnect) {
        if (forceDisconnect) {
            connSocketDisconnect();
        } else {
            if (!tivoConfigData.isCfgKeepConnOpen() && !tivoConfigData.doChannelScan()) {
                doNappTime();
                connSocketDisconnect();
            }
        }
    }

    /**
     * {@link connTivoReconnect} disconnect and reconnect the socket connection to the TiVo.
     *
     * @return boolean true = connection succeeded, false = connection failed
     */

    public boolean connTivoReconnect() {
        connSocketDisconnect();
        doNappTime();
        return connTivoConnect();
    }

    /**
     * {@link connSocketDisconnect} cleanly closes the socket connection and dependent objects
     *
     */
    private void connSocketDisconnect() {
        logger.debug(" connTivoSocket '{}' - requested to disconnect/cleanup connection objects",
                tivoConfigData.getCfgIdentifier());
        try {
            if (streamReader != null) {
                while (streamReader.isAlive()) {
                    streamReader.stopReader();
                    // doNappTime();
                }
                streamReader = null;
            }
            if (streamWriter != null) {
                streamWriter.close();
                streamWriter = null;
            }
            if (tivoSocket != null) {
                tivoSocket.close();
                tivoSocket = null;
            }

        } catch (IOException e) {
            logger.error(" TiVo '{}' - I/O exception while disconnecting: '{}'.  Connection closed.",
                    tivoConfigData.getCfgIdentifier(), e.getMessage());
        }
    }

    /**
     * {@link connSocketConnect} opens a Socket connection to the TiVo. Creates a {@link StreamReader} (Input)
     * thread to read the responses from the TiVo and a PrintStream (Output) {@link cmdTivoSend}
     * to send commands to the device.
     *
     * @param pConnect true = make a new connection , false = close existing connection
     * @return boolean true = connection succeeded, false = connection failed
     */
    private boolean connSocketConnect() {
        logger.debug(" connSocketConnect '{}' - attempting connection to host '{}', port '{}'",
                tivoConfigData.getCfgIdentifier(), tivoConfigData.getCfgHost(), tivoConfigData.getCfgTcpPort());

        if (connIsConnected()) {
            logger.debug(" connSocketConnect '{}' - already connected to host '{}', port '{}'",
                    tivoConfigData.getCfgIdentifier(), tivoConfigData.getCfgHost(), tivoConfigData.getCfgTcpPort());
            return true;
        } else {
            // something is wrong, so force a disconnect/clean up so we can try again
            connTivoDisconnect(true);
        }

        try {

            tivoSocket = new Socket(tivoConfigData.getCfgHost(), tivoConfigData.getCfgTcpPort());
            tivoSocket.setKeepAlive(true);
            tivoSocket.setSoTimeout(CONFIG_SOCKET_TIMEOUT);
            tivoSocket.setReuseAddress(true);

            if (tivoSocket.isConnected() && !tivoSocket.isClosed()) {
                if (streamWriter == null) {
                    streamWriter = new PrintStream(tivoSocket.getOutputStream(), false);
                }
                if (streamReader == null) {
                    streamReader = new StreamReader(tivoSocket.getInputStream());
                    streamReader.start();
                }
            } else {
                logger.error(" connSocketConnect '{}' - socket creation failed to host '{}', port '{}'",
                        tivoConfigData.getCfgIdentifier(), tivoConfigData.getCfgHost(), tivoConfigData.getCfgTcpPort());
                return false;
            }

            return true;

        } catch (UnknownHostException e) {
            logger.error(" TiVo '{}' - while connecting, unexpected host error: '{}'",
                    tivoConfigData.getCfgIdentifier(), e.getMessage());
        } catch (IOException e) {
            if (tivoStatusData.getConnectionStatus() != ConnectionStatus.OFFLINE) {
                logger.error(" TiVo '{}' - I/O exception while connecting: '{}'", tivoConfigData.getCfgIdentifier(),
                        e.getMessage());
            }
        }
        return false;
    }

    /**
     * {@link doNappTime} sleeps for the period specified by the getCfgCmdWait parameter. Primarily used to allow the
     * TiVo time to process responses after a command is issued.
     */
    public void doNappTime() {
        try {
            logger.debug(" doNappTime '{}' - I feel like napping for '{}' milliseconds",
                    tivoConfigData.getCfgIdentifier(), tivoConfigData.getCfgCmdWait());
            TimeUnit.MILLISECONDS.sleep(tivoConfigData.getCfgCmdWait());
        } catch (Exception e) {
        }
    }

    public TivoStatusData getServiceStatus() {
        return tivoStatusData;
    }

    public void setServiceStatus(TivoStatusData tivoStatusData) {
        this.tivoStatusData = tivoStatusData;
    }

    public void setChScan(boolean chScan) {
        this.tivoStatusData.setChScan(chScan);
    }

    /**
     * {@link StreamReader} data stream reader that reads the status data returned from the TiVo.
     *
     */
    public class StreamReader extends Thread {
        private BufferedReader bufferedReader = null;
        private volatile boolean stopReader;

        private CountDownLatch stopLatch;

        /**
         * {@link StreamReader} construct a data stream reader that reads the status data returned from the TiVo via a
         * BufferedReader.
         *
         * @param inputStream socket input stream.
         * @throws IOException
         */
        public StreamReader(InputStream inputStream) {
            this.setName("tivoStreamReader-" + tivoConfigData.getCfgIdentifier());
            this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            this.stopLatch = new CountDownLatch(1);
            this.setDaemon(true);
        }

        @Override
        public void run() {
            try {
                logger.debug("streamReader {} is running. ", tivoConfigData.getCfgIdentifier());
                while (!stopReader && !Thread.currentThread().isInterrupted()) {

                    String receivedData = null;
                    try {
                        receivedData = bufferedReader.readLine();
                    } catch (SocketTimeoutException e) {
                        // Do nothing. Just allow the thread to check if it has to stop.
                    }

                    if (receivedData != null) {
                        logger.debug("TiVo {} data received: {}", tivoConfigData.getCfgIdentifier(), receivedData);
                        TivoStatusData commandResult = statusParse(receivedData);
                        tivoHandler.updateTivoStatus(tivoStatusData, commandResult);
                        tivoStatusData = commandResult;
                        // }
                    }
                }

            } catch (IOException e) {
                logger.warn("TiVo {} is disconnected. ", tivoConfigData.getCfgIdentifier(), e);
            }
            // Notify the stopReader method caller that the reader is stopped.
            this.stopLatch.countDown();
            logger.debug("streamReader {} is stopped. ", tivoConfigData.getCfgIdentifier());
        }

        /**
         * {@link stopReader} cleanly stops the {@link StreamReader} thread. Blocks until the reader is stopped.
         */

        public void stopReader() {
            this.stopReader = true;
            try {
                this.stopLatch.await(READ_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // Do nothing. The timeout is just here for safety and to be sure that the call to this method will not
                // block the caller indefinitely.
            }
        }

=======
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.tivo.handler.TiVoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class variables to hold our connection out to our Tivo managed by this instance of a Thing.
 *
 * @author Jayson Kubilis - Initial contribution
 * @author Andrew Black - Updates / compilation corrections
 */

public class TivoStatusProvider {

    private Socket tivoSocket = null;
    private PrintStream tivoIOSendCommand = null;
    private BufferedReader tivoIOReadStatus = null;
    private TivoStatusData tivoStatusData = null;
    private TivoConfigData tivoConfigData = null;
    private TiVoHandler tivoHandler = null;
    private Logger logger = LoggerFactory.getLogger(TivoStatusProvider.class);
    private Boolean pausePolling = false;
    private int errCount = 0;

    /**
     * Instantiates a new TivoConfigStatusProvider.
     *
     * @param tivoConfigData {@link TivoConfigData} configuration data for the specific thing.
     * @param tivoStatusData {@link TivoStatusData} status data for the specific thing.
     * @param tivoHandler {@link TivoHandler} parent handler object for the TivoConfigStatusProvider.
     * @param pausePolling boolean true=skip process normally run while other actions are completed.
     */

    public TivoStatusProvider(TivoConfigData tivoConfigData, TiVoHandler tivoHandler, Boolean pausePolling) {
        this.tivoStatusData = new TivoStatusData(false, -1, "INITIALISING", false);
        this.tivoConfigData = tivoConfigData;
        this.tivoHandler = tivoHandler;
        this.pausePolling = pausePolling;
    }

    /**
     * {@link statusRefresh} checks the current status data in tivoStatusData, attempts connection to the Tivo
     * to retrieve the current status. Updates the status with valid (useful) updated values or retains the
     * existing state data.
     *
     * @return {@link TivoStatusData} object
     */
    public TivoStatusData statusRefresh() {

        if (!pausePolling) {

            if (tivoStatusData != null) {
                logger.debug(" statusRefresh '{}' - existing status data FOUND in tivoStatusData class - '{}'",
                        tivoConfigData.getCfgIdentifier(), tivoStatusData.toString());
                if (tivoStatusData.isCmdOk() & tivoStatusData.getPubToUI()) {
                    tivoHandler.updateTivoStatus(tivoStatusData);
                }

            } else {
                logger.debug(" statusRefresh '{}' - existing status data NOT FOUND in tivoStatusData class",
                        tivoConfigData.getCfgIdentifier());
            }

            if (errCount > tivoConfigData.getCfgNumConnRetry()) {
                connTivoConnectRetry(false);
                errCount = 0;

            }

            TivoStatusData myTivo = statusGet();

            if (myTivo != null) {
                logger.debug(" statusRefresh '{}' - status data FOUND in CURRENT CHECK - '{}'",
                        tivoConfigData.getCfgIdentifier(), myTivo.toString());

            } else {
                logger.debug(" statusRefresh '{}' - status data NOT FOUND in CURRENT CHECK!  Attempt '{}'.",
                        tivoConfigData.getCfgIdentifier(), errCount + 1);
            }

            if (myTivo != null && myTivo.isCmdOk()) {
                // We found valid TiVo status values, updating
                tivoStatusData = myTivo;
                tivoHandler.updateTivoStatus(tivoStatusData);
            } else {
                errCount++;
                logger.debug(" statusRefresh '{}' - returned no new status data, the existing status will be retained",
                        tivoConfigData.getCfgIdentifier());
            }

            if (myTivo != null && tivoStatusData != null && tivoStatusData.getMsg().equals("NO_STATUS_DATA_RETURNED")
                    && myTivo.getMsg().equals("NO_STATUS_DATA_RETURNED")) {
                // Force a disconnect so we can get some decent status on next poll
                connTivoConnectRetry(false);
            }
            return tivoStatusData;
        }
        logger.debug(" statusRefresh '{}' - status polling actions are PAUSED/SKIPPED, while we execute a command",
                tivoConfigData.getCfgIdentifier());
        return tivoStatusData;
    }

    /**
     * {@link cmdTivoCommand} sends a command to the Tivo.
     *
     * @param pCmd the command string from the item.
     * @return {@link TivoStatusData} object.
     */
    public TivoStatusData cmdTivoSend(String pCmd) {

        boolean oC = false; // Tracks if this step caused a connection to be opened
        TivoStatusData tmpStatus = null;
        int retryCount = 2;

        pausePolling = true;

        // Open a connection if one does not already exist
        if (!connTestSocket()) {
            connTivoConnectRetry(true);
            oC = true;
        }

        tmpStatus = cmdTivoSendRetry(pCmd, retryCount);

        // Will close the connection after operation if we are not keeping one open.
        if (!tivoConfigData.isCfgKeepConnOpen() && oC && !tivoStatusData.getChScan()) {
            connTivoConnectRetry(false);
        }
        pausePolling = false;
        return tmpStatus;

    }

    /**
     * {@link cmdTivoSendRetry} sets the TiVo status, executes the specified number of retries.
     *
     * @param pCmd the command string from the item.
     * @param retryCount the retry count.
     * @return {@link TivoStatusData} object.
     */
    private TivoStatusData cmdTivoSendRetry(String pCmd, int retryCount) {

        logger.info(" TiVo '{}' - sending command: '{}' (retry @ '{}')", tivoConfigData.getCfgIdentifier(), pCmd,
                retryCount);
        int l = 1;

        if (!connTestSocket()) {
            logger.error(" TiVo '{}' - called setTivoStatus but not connected!", tivoConfigData.getCfgIdentifier());
            return new TivoStatusData(false, -1, "NOT_CONNECTED", false, 0);
        }

        try {
            // Handle special keyboard "repeat" commands
            if (pCmd.contains("*")) {
                l = Integer.parseInt(pCmd.substring(pCmd.indexOf("*") + 1));
                pCmd = pCmd.substring(0, pCmd.indexOf("*"));
                logger.info(" TiVo '{}' - repeating command: '{}' repeating '{}' times",
                        tivoConfigData.getCfgIdentifier(), pCmd, l);
            }
            for (int i = 1; i <= l; i++) {
                tivoIOSendCommand.println(pCmd.toString() + "\r");
                if (tivoIOSendCommand.checkError()) {

                    logger.error(" TiVo '{}' - called setTivoStatus and encountered an IO error",
                            tivoConfigData.getCfgIdentifier(), tivoSocket.isConnected(), tivoSocket.isClosed());
                    connTivoConnectRetry(false);
                    connTivoConnectRetry(true);

                    if (retryCount <= 0) {
                        // We have failed at life... Give up.
                        return new TivoStatusData(false, -1, "CONNECTION_RETRIES_EXHAUSTED", false, 0);

                    } else {
                        doNappTime();
                        return cmdTivoSendRetry(pCmd, retryCount - 1);
                    }
                }
            }

        } catch (Exception e) {
            logger.error(" TiVo '{}' - message send '{}' failed with error '{}'", tivoConfigData.getCfgIdentifier(),
                    pCmd.toString(), e.getMessage());

        }
        // Now read the response
        return statusGet();

    }

    /**
     * {@link getTivoStatus} checks if the current connection is "Keep Open" and connected. Connect, query and close as
     * required.
     *
     * @return {@link TivoStatusData} object.
     */

    private TivoStatusData statusGet() {

        boolean oC = false;
        TivoStatusData rtnVal = null;
        int retryCount = 2;

        // Will open a connection prior to operation if we are not keeping one open.
        if (!connTestSocket() || !tivoConfigData.isCfgKeepConnOpen()) {
            oC = connTivoConnectRetry(true);
        }

        if (tivoStatusData.getConnOK() > 1) {
            rtnVal = statusGetRetry(retryCount);
        }

        // Will close the connection after operation if we are not keeping one open.
        if (!tivoConfigData.isCfgKeepConnOpen() && oC && !tivoStatusData.getChScan()) {
            connTivoConnectRetry(false);
        }
        return rtnVal;
    }

    /**
     * {@link statusGetRetry} queries the TiVo device for the LAST status message and parses the resultant data.
     * <p>
     *
     * When a command is executed, the system either returns a new CH_STATUS message or any error. Most commands do not
     * return successful status codes (with the exception of a channel change).
     * <p>
     *
     * @param retryCount integer, number of times a message retrieval will be attempted if a communication error occurs
     * @return {@link TivoStatusData} object containing the result of the connection to the DVR.
     */
    private TivoStatusData statusGetRetry(int retryCount) {

        // Queries the TiVo device for the current status. Status of previous command is displayed for 3-4 seconds.
        // COMMAND_TIMEOUT is then displayed (equivalent to 'waiting for next command').

        logger.debug(" statusGetRetry '{}' - reading from TiVo (retry @ '{}')", tivoConfigData.getCfgIdentifier(),
                retryCount);

        String line = null;
        String tivoStatus = null;

        // If were not connected we can't provide a status
        if (!connTestSocket()) {
            logger.error(" TiVo '{}' - called statusGetRetry but not connected!", tivoConfigData.getCfgIdentifier());
            return new TivoStatusData(false, -1, "NOT_CONNECTED", false, 0);
        }

        try {
            while ((line = tivoIOReadStatus.readLine()) != null) {
                logger.debug(" statusGetRetry read: '{}'", line);
                if (!line.isEmpty() && !line.trim().equalsIgnoreCase("COMMAND_TIMEOUT")) {
                    // use this line
                    tivoStatus = line;
                } else {
                    logger.debug(" statusGetRetry ignored line: '{}'", line);
                }

            }
        } catch (IOException e) {
            logger.debug(" statusGetRetry '{}' - I/O exception: '{}'", tivoConfigData.getCfgIdentifier(), e);
            if (e.getMessage().equalsIgnoreCase("READ TIMED OUT")) {
                logger.debug(
                        " statusGetRetry '{}' - I/O exception: 'Read timed out' is expected/normal when there is nothing else to read from the stream",
                        tivoConfigData.getCfgIdentifier());

            } else if (e.getMessage().equalsIgnoreCase("Connection reset")) {
                logger.error(" TiVo '{}' - CONNECTION_RESET! '{}'", tivoConfigData.getCfgIdentifier(), e.getMessage());

                connTivoConnectRetry(false); // close connection
                connTivoConnectRetry(true); // open a new connection

                if (retryCount <= 0) {
                    // We have failed at life... Give up.
                    tivoStatus = "";
                    return new TivoStatusData(false, -1, "CONNECTION_RETRIES_EXHAUSTED", false, 0);
                } else {
                    doNappTime();
                    return statusGetRetry(retryCount - 1);
                }
            } else {
                logger.error(" statusGetRetry '{}' - couldn't read from the tivo - '{}'",
                        tivoConfigData.getCfgIdentifier(), e.getMessage());
            }
        }

        return statusParse(tivoStatus);

    }

    /**
     * {@link statusParse} processes the {@link TivoStatusData} status message returned from the TiVo. Calls
     * {@link getParsedChannel} and returns the channel number (if a match is found in a valid formatted message).
     *
     * @param strTivoStatus string representing the message text
     * @return TivoStatusData with updated status message
     */

    private TivoStatusData statusParse(String strTivoStatus) {

        logger.debug(" statusParse '{}' - running on string '{}'", tivoConfigData.getCfgIdentifier(), strTivoStatus);
        boolean pubMsg = true;

        if (strTivoStatus == null) {
            return new TivoStatusData(false, -1, "NO_STATUS_DATA_RETURNED", false);
        } else {
            // Determine if this is a repeat of an existing status msg
            logger.debug(" statusParse '{}' - current status '{}'", tivoConfigData.getCfgIdentifier(),
                    tivoStatusData.toString());
            if (tivoStatusData.getMsg().equals(strTivoStatus)) {
                pubMsg = false;
            }
            switch (strTivoStatus) {
                // COMMAND_TIMEOUT (more accurately waiting for next command) status is dropped earlier in the process
                // and returns ""
                case "":
                    return new TivoStatusData(false, -1, "NO_STATUS_DATA_RETURNED", false, tivoStatusData.getConnOK());
                case "CH_FAILED NO_LIVE":
                    return new TivoStatusData(false, -1, "CH_FAILED NO_LIVE", pubMsg, tivoStatusData.getConnOK());
                case "CH_FAILED RECORDING":
                    return new TivoStatusData(false, -1, "CH_FAILED RECORDING", pubMsg, tivoStatusData.getConnOK());
                case "CH_FAILED MISSING_CHANNEL":
                    return new TivoStatusData(false, -1, "CH_FAILED MISSING_CHANNEL", pubMsg,
                            tivoStatusData.getConnOK());
                case "CH_FAILED MALFORMED_CHANNEL":
                    return new TivoStatusData(false, -1, "CH_FAILED NO_LIVE", pubMsg, tivoStatusData.getConnOK());
                case "CH_FAILED INVALID_CHANNEL":
                    return new TivoStatusData(false, -1, "CH_FAILED INVALID_CHANNEL", pubMsg,
                            tivoStatusData.getConnOK());
                case "LIVETV_READY":
                    return new TivoStatusData(true, -1, "LIVETV_READY", pubMsg, tivoStatusData.getConnOK());
                case "INVALID_COMMAND":
                    return new TivoStatusData(false, -1, "INVALID_COMMAND", false, tivoStatusData.getConnOK());
                case "CONNECTION_RETRIES_EXHAUSTED":
                    return new TivoStatusData(false, -1, "CONNECTION_RETRIES_EXHAUSTED", pubMsg,
                            tivoStatusData.getConnOK());

            }
        }

        // Only other documented status is in the form 'CH_STATUS channel reason' or
        // 'CH_STATUS channel sub-channel reason'

        Pattern tivoStatusPattern = Pattern.compile("[0]+(\\d+)\\s+");
        Matcher matcher = tivoStatusPattern.matcher(strTivoStatus);
        Integer chNum = -1; // -1 used globally to indicate channel number error

        if (matcher.find()) {
            logger.debug(" statusParse '{}' - groups '{}' with group count of '{}'", tivoConfigData.getCfgIdentifier(),
                    matcher.group(), matcher.groupCount());
            if (matcher.groupCount() == 1 | matcher.groupCount() == 2) {
                chNum = new Integer(Integer.parseInt(matcher.group(1).trim()));
            }
            logger.debug(" statusParse '{}' - parsed channel '{}'", tivoConfigData.getCfgIdentifier(), chNum);
            if (tivoStatusData.getConnOK() != 3) {
                logger.debug(" connTivoConnectRetry '{}' - read test suceeded, we are ONLINE.",
                        tivoConfigData.getCfgIdentifier());
                tivoHandler.setTivoStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "");
            }
            return new TivoStatusData(true, chNum, strTivoStatus, pubMsg, 3);
        }
        logger.warn(" TiVo '{}' - Unhandled / unexpected status message recieved: '{}'",
                tivoConfigData.getCfgIdentifier(), strTivoStatus);
        return new TivoStatusData(false, -1, strTivoStatus, false, tivoStatusData.getConnOK());
    }

    /**
     * {@link connTestSocket} returns the connection state of the Socket connection object to the TiVo. This is only
     * partial answer to determining the state of the Tivo, see {@link connTestCmds}.
     *
     * @return true = connection exists, false = connection does not exist
     *
     */

    private boolean connTestSocket() {
        if (tivoSocket == null) {
            logger.debug(" connTestSocket '{}' - getTivoConnected: tivoSocket=null", tivoConfigData.getCfgIdentifier());
            return false;
        } else if (tivoSocket != null && !tivoSocket.isConnected()) {
            logger.debug(" connTestSocket '{}' - getTivoConnected: tivoSocket.isConnected=false",
                    tivoConfigData.getCfgIdentifier());
            return false;
        } else if (tivoSocket != null && tivoSocket.isClosed()) {
            logger.debug(" connTestSocket '{}' - getTivoConnected: tivoSocket.isClosed=true",
                    tivoConfigData.getCfgIdentifier());
            return false;
        } else if (tivoIOSendCommand == null) {
            logger.debug(" connTestSocket '{}' - getTivoConnected: tivoIOSendCommand=null",
                    tivoConfigData.getCfgIdentifier());
            return false;
        } else if (tivoIOReadStatus == null) {
            logger.debug(" connTestSocket '{}' - getTivoConnected: tivoIOReadStatus=null",
                    tivoConfigData.getCfgIdentifier());
            return false;
        }
        return true;
    }

    /**
     * {@link connTivoConnectRetry} makes / drops the Socket connection based upon the thing configuration. Performs
     * connection tests {@link connTestCmds} to determine if the Tivo is online or in standby mode.
     *
     * @param pConnect true = make a new connection , false = close existing connection
     * @return true = connected, false = not connected
     */

    public boolean connTivoConnectRetry(boolean pConnect) {

        if (tivoStatusData == null) {
            tivoStatusData = new TivoStatusData(false, -1, "NOT INITIALISED", false, -1);
        }

        if (pConnect == false) {
            // Disconnect, no retry required
            connTivoSocket(false);
            logger.debug(" connTivoConnectRetry '{}' - Socket dissconnected. ", tivoConfigData.getCfgIdentifier());

        } else {
            // Connect, with retry
            for (int iL = 1; iL <= tivoConfigData.getCfgNumConnRetry(); iL++) {
                logger.debug(" connTivoConnectRetry '{}' - starting connection process '{}' of '{}'.",
                        tivoConfigData.getCfgIdentifier(), iL, tivoConfigData.getCfgNumConnRetry());

                if (tivoStatusData.getConnOK() < 2) {
                    connTivoSocket(false);
                    logger.debug(" connTivoConnectRetry '{}' - Socket dissconnected, to allow re-test. ",
                            tivoConfigData.getCfgIdentifier());
                }

                // Sort out the socket connection
                if (!connTestSocket()) {
                    if (connTivoSocket(true)) {
                        logger.debug(" connTivoConnectRetry '{}' - Socket created / connection made.",
                                tivoConfigData.getCfgIdentifier());

                        int connState = connTestCmds();

                        if (connState == 3) {
                            if (tivoStatusData.getConnOK() != 3) {
                                tivoHandler.setTivoStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "");
                                tivoStatusData.setConnOK(3);
                            }
                            logger.debug(" connTivoConnectRetry '{}' - read test suceeded, we are ONLINE.",
                                    tivoConfigData.getCfgIdentifier());
                            return true;
                        } else if (connState == 2) {
                            if (tivoStatusData.getConnOK() != 2) {
                                tivoHandler.setTivoStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE,
                                        "STANDBY MODE - use 'IRCODE TIVO' to wake up.");
                                tivoStatusData.setConnOK(2);
                            }
                            logger.debug(
                                    "connTivoConnectRetry '{}' - only write test command suceeded, we are ONLINE (STANDBY).",
                                    tivoConfigData.getCfgIdentifier());
                            return true;
                        } else if (connState == 1 & tivoStatusData.getConnOK() != 1) {
                            connTivoSocket(false);
                            tivoStatusData.setConnOK(1);
                            logger.debug(" connTivoConnectRetry '{}' - both test commands failed, we are OFFLINE.",
                                    tivoConfigData.getCfgIdentifier());
                        }
                    } else {
                        // Socket creation failed...
                        if (tivoStatusData.getConnOK() != 0) {
                            tivoHandler.setTivoStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                    "Unable to connect or port already in-use");
                            tivoStatusData.setConnOK(0);
                            logger.warn(" connTivoConnectRetry '{}' - Socket creation failed.",
                                    tivoConfigData.getCfgIdentifier());
                        }
                    }

                    if (iL == tivoConfigData.getCfgNumConnRetry()) {
                        // Set offline if all retries failed to connect
                        if (tivoStatusData.getConnOK() != 0) {
                            tivoStatusData.setConnOK(0);
                            tivoHandler.setTivoStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                    "Connection tests (read and write) failed " + tivoConfigData.getCfgNumConnRetry()
                                            + " times");
                        }
                        logger.debug(" connTivoConnectRetry '{}' - failed all retries, we are OFFLINE.",
                                tivoConfigData.getCfgIdentifier());
                        return false;
                    } else {
                        // Sleep and retry
                        doNappTime();
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@link connTivoSocket} opens and closes the Socket connection to the DVR. Creates a BufferedReader (Input)
     * {@link tivoIOReadStatus} to read the responses from the TiVo and a PrintStream (Output) {@link tivoIOSendCommand}
     * to send commands to the device.
     *
     * @param pConnect true = make a new connection , false = close existing connection
     * @return true = connected, false = not connected
     */
    private boolean connTivoSocket(boolean pConnect) {

        // DISCONNECT
        if (!pConnect && connTestSocket()) {
            logger.debug(" connTivoSocket '{}' - requested to disconnect", tivoConfigData.getCfgIdentifier());
            try {
                tivoIOReadStatus.close();
                tivoIOSendCommand.close();
                tivoSocket.close();
                tivoSocket = null;

            } catch (IOException e) {
                logger.error(" TiVo '{}' - I/O exception while disconnecting: '{}'.  Connection closed.",
                        tivoConfigData.getCfgIdentifier(), e.getMessage());
            }
            return false;

        } else if (!pConnect) {
            logger.debug(" connTivoSocket '{}' -  requested to disconnect, but already not connected.  Ignoring.",
                    tivoConfigData.getCfgIdentifier());
            return false;

        } else {
            // CONNECT
            if (connTestSocket()) {
                logger.debug(" connTivoSocket '{}' - requested to connect while aready connected. Ignoring.",
                        tivoConfigData.getCfgIdentifier());
                return true;
            }

            logger.debug(" connTivoSocket '{}' - connTivoConnectRetry attempting connection to host '{}', port '{}'",
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
                logger.error(" TiVo '{}' - while connecting, unexpected host error: '{}'",
                        tivoConfigData.getCfgIdentifier(), e.getMessage());
            } catch (IOException e) {
                if (tivoStatusData.getConnOK() != 0) {
                    logger.error(" TiVo '{}' - I/O exception while connecting: '{}'", tivoConfigData.getCfgIdentifier(),
                            e.getMessage());
                }
            }
            return false;
        }
    }

    /**
     * {@link connTestCmds} tests the connection Tivo by a) connecting to the Tivo which returns the current channel
     * (when on) or nothing when in standby and b) sending a dummy command which returns INVALID_COMMAND (when on and in
     * standby). If both commands fail, then there is a problem with the Socket or the TiVo is powered down / port is
     * already in-use.
     *
     * @return true = TiVo is online (on or standby), false = Tivo is offline (powered down or invalid IP / port already
     *         in-use)
     */

    private int connTestCmds() {

        boolean getOk = false;
        boolean sendOk = false;

        // When we initially connect we should get the current channel status
        logger.debug(" connTestCmds '{}' - testing initial Channel status.", tivoConfigData.getCfgIdentifier());
        getOk = statusFindMatch(statusReadAll(), "CH_STATUS", true);
        if (!getOk) {
            logger.debug(" connTestCmds '{}' - testing read of initial channel state FAILED",
                    tivoConfigData.getCfgIdentifier());
        } else {
            return 3;
        }

        logger.debug(" connTestCmds '{}' - testing dummy write command 'DUMMY_CMD'.",
                tivoConfigData.getCfgIdentifier());

        // Send DUMMY command to see if the connection is really working (only reliable way)
        tivoIOSendCommand.print("DUMMY_CMD" + "\r");
        tivoIOSendCommand.flush();
        // flushes the command to the TiVo and checks for any error returned
        if (tivoIOSendCommand.checkError()) {
            logger.debug(" connTestCmds '{}' - testing dummy command failed.  Unable to send.",
                    tivoConfigData.getCfgIdentifier());
            sendOk = false;
        }

        // now see what we got back
        doNappTime();
        sendOk = statusFindMatch(statusReadAll(), "INVALID_COMMAND", false);
        if (!sendOk) {
            logger.debug(" connTestCmds '{}' - testing of dummy write command FAILED",
                    tivoConfigData.getCfgIdentifier());
        }

        if (!getOk & sendOk) {
            return 2;
        } else if (!getOk & !sendOk) {
            return 1;
        }
        return 0;
    }

    /**
     * {@link statusReadAll} reads all the status commands and returns these as an array of {@link TivoStatusData}
     * objects. These can then be searched using {@link statusFindMatch} to determine if a desired/expected status has
     * been returned.
     *
     * @return array list of {@link TivoStatusData} objects.
     */

    private ArrayList<TivoStatusData> statusReadAll() {

        ArrayList<String> msgArr = new ArrayList<String>();
        ArrayList<TivoStatusData> statusArr = new ArrayList<TivoStatusData>();
        String line = null;
        int iL = 0;

        try {
            while ((line = tivoIOReadStatus.readLine()) != null) {
                msgArr.add(line);
                logger.debug(" statusReadAll '{}' - while connecting read [{}]: '{}'",
                        tivoConfigData.getCfgIdentifier(), iL, msgArr.get(iL));
                iL++;
            }
        } catch (IOException e) {
            logger.debug(" statusReadAll '{}' - I/O exception while connecting: '{}'",
                    tivoConfigData.getCfgIdentifier(), e.getMessage());

        }

        if (msgArr.size() > 0) {
            for (iL = 0; iL < msgArr.size(); iL++) {
                statusArr.add(statusParse(msgArr.get(iL)));
                logger.debug(" statusReadAll '{}' - testing command returned status: {}.",
                        tivoConfigData.getCfgIdentifier(), statusArr.get(iL));
            }
        }
        return statusArr;

    }

    /**
     * {@link statusFindMatch} finds if the desired string/status is contained within an array of status messages.
     *
     * @param statusArr array list of {@link TivoStatusData} objects.
     * @param strMatch string containing the text to match within the array status messages.
     * @param updateStatus true = update the main thing status with the found status message (if a match is found).
     * @return true, if successful.
     */
    private boolean statusFindMatch(ArrayList<TivoStatusData> statusArr, String strMatch, boolean updateStatus) {

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

    /**
     * {@link doNappTime} sleeps for the period specified within the Thing configuration. Primarily used to allow the
     * TiVo time to respond after a command is issued.
     */
    public void doNappTime() {
        try {
            logger.debug("   doNappTime '{}' - I feel like napping for '{}' milliseconds",
                    tivoConfigData.getCfgIdentifier(), tivoConfigData.getCfgCmdWait());
            TimeUnit.MILLISECONDS.sleep(tivoConfigData.getCfgCmdWait());
        } catch (Exception e) {
        }
    }

    public TivoStatusData getServiceStatus() {
        return tivoStatusData;
    }

    public void setServiceStatus(TivoStatusData tivoStatusData) {
        this.tivoStatusData = tivoStatusData;
    }

    public void setChScan(boolean chScan) {
        this.tivoStatusData.setChScan(chScan);
>>>>>>> 73f9dc6 Initial Commit
    }
}
