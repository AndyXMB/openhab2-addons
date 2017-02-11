/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tivo.handler;

import static org.openhab.binding.tivo.TiVoBindingConstants.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.tivo.internal.service.TivoConfigData;
import org.openhab.binding.tivo.internal.service.TivoStatusData;
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
import org.openhab.binding.tivo.internal.service.TivoStatusData.ConnectionStatus;
=======
>>>>>>> 73f9dc6 Initial Commit
import org.openhab.binding.tivo.internal.service.TivoStatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TiVoHandler} is the BaseThingHandler responsible for handling commands that are
 * sent to one of the Tivo's channels.
 *
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
 * @author Jayson Kubilis (DigitalBytes) - Initial contribution
=======
 * @author Jayson Kubilis - Initial contribution
>>>>>>> 73f9dc6 Initial Commit
 * @author Andrew Black (AndyXMB) - Updates / compilation corrections. Addition of channel scanning functionality.
 */

public class TiVoHandler extends BaseThingHandler {
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
    private final Logger logger = LoggerFactory.getLogger(TiVoHandler.class);
    private TivoConfigData tivoConfigData = null;
    private ConnectionStatus lastConnectionStatus = ConnectionStatus.UNKNOWN;
    private TivoStatusProvider tivoConnection = null;
    private ScheduledFuture<?> refreshJob = null;
    private ScheduledFuture<?> chScanJob = null;
=======
    private Logger logger = LoggerFactory.getLogger(TiVoHandler.class);
    private TivoConfigData tivoCfgData = null;
    private TivoStatusProvider myTivoService = null;
    private ScheduledFuture<?> refreshJob;
    private ScheduledFuture<?> chScanJob;
>>>>>>> 73f9dc6 Initial Commit

    /**
     * Instantiates a new TiVo handler.
     *
     * @param thing the thing
     */
    public TiVoHandler(Thing thing) {
        super(thing);
        logger.debug("TiVoHandler '{}' - creating", getThing().getUID());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        // Handles the commands from the various TiVo channel objects
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        logger.debug("handleCommand '{}', parameter: {}", channelUID, command);
=======
        logger.debug("handleCommand {}, command: {}", channelUID, command);
>>>>>>> 73f9dc6 Initial Commit

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        if (!isInitialized()) {
            logger.debug("handleCommand '{}' device is not intialised yet, command '{}' will be ignored.",
                    getThing().getUID(), channelUID + " " + command);
            return;
=======
        if (command != null && myTivoService != null) {

            TivoStatusData myTivo = myTivoService.getServiceStatus();
            String tivoCommand = null;

            if (!isInitialized()) {
                logger.debug("handleCommand '{}' device is not intialised yet, command '{}' will be ignored.",
                        getThing().getUID(), channelUID + " " + command);
                return;
            }

            // Check to see if we are running a channel scan, if so 'disable' UI commands, else chaos ensues...
            if (myTivo != null && myTivo.getChScan()) {
                logger.warn("TiVo '{}' channel scan is in progress, command '{}' will be ignored.", getThing().getUID(),
                        channelUID + " " + command);
                return;
            }

            String tmpAct = command.toString().toUpperCase();
            if (command instanceof RefreshType) {

                switch (channelUID.getId()) {

                    case CHANNEL_TIVO_STATUS:
                    case CHANNEL_TIVO_CHANNEL_FORCE:
                    case CHANNEL_TIVO_CHANNEL_SET:
                        if (myTivo != null && myTivo.isCmdOk()) {
                            updateTivoStatus(myTivo);
                        }
                        break;
                    default:
                        // Future enhancement, if we can come up with a sensible set of actions when a REFRESH is issued
                        logger.info("TiVo '{}' skipping REFRESH command for channel: '{}'.", getThing().getUID(),
                                channelUID.getId());
                }

                return;
            }

            switch (channelUID.getId())

            {
                case CHANNEL_TIVO_CHANNEL_FORCE:
                    tivoCommand = "FORCECH";
                case CHANNEL_TIVO_CHANNEL_SET:
                    logger.debug("handleCommand '{}' - CHANNEL_TIVO FORCECH or SETCH found!", getThing().getUID());

                    if (tivoCommand == null) {
                        tivoCommand = "SETCH";
                    }

                    myTivo = chChannelChange(tivoCommand, tmpAct);
                    if (myTivo != null && myTivo.isCmdOk()) {
                        updateTivoStatus(myTivo);
                    }
                    break;

                case CHANNEL_TIVO_COMMAND:
                    logger.debug("handleCommand '{}' - CHANNEL_TIVO COMMAND found!", getThing().getUID());

                    // Attempt to execute the command on the tivo
                    myTivo = myTivoService.cmdTivoSend(tmpAct);

                    // Check to see if the command was successful
                    if (myTivo != null && myTivo.isCmdOk()) {
                        logger.debug("handleCommand '{}' - returned Tivo Data Object: '{}'", getThing().getUID(),
                                myTivo.toString());
                        updateTivoStatus(myTivo);
                    } else {
                        logger.warn("handleCommand '{}' - command failed '{}'", getThing().getUID(), tmpAct);
                    }
                    break;

                case CHANNEL_TIVO_TELEPORT:
                    tivoCommand = "TELEPORT " + tmpAct;

                    logger.debug("handleCommand '{}' TELEPORT command to tivo: '{}'", getThing().getUID(), tivoCommand);

                    if (myTivoService.getServiceStatus().getConnOK() == 2 & tmpAct.equals("TIVO")) {
                        tivoCommand = "IRCODE " + tmpAct;
                        logger.debug("TiVo '{}' TELEPORT re-mapped to IRCODE as we are in standby: '{}'",
                                getThing().getUID(), tivoCommand);
                    }

                    // Attempt to execute the command on the TiVo
                    myTivo = myTivoService.cmdTivoSend(tivoCommand);

                    // Check to see if the command was successful
                    if (myTivo != null && myTivo.isCmdOk()) {
                        logger.debug("handleCommand '{}' - returned Tivo Data Object: '{}'", myTivo.toString());
                        updateTivoStatus(myTivo);
                    }

                    break;

                case CHANNEL_TIVO_IRCMD:
                    tivoCommand = "IRCODE";
                case CHANNEL_TIVO_KBDCMD:
                    logger.debug("handleCommand '{}' - CHANNEL_TIVO IRCODE/KBDCMD found!", getThing().getUID());

                    if (tivoCommand == null) {
                        tivoCommand = "KEYBOARD";
                    }

                    String tmpCommand = tivoCommand + " " + tmpAct;

                    logger.debug("handleCommand '{}' - IR/KBD command to tivo: '{}'", getThing().getUID(), tmpCommand);

                    // Attempt to execute the command on the TiVo
                    myTivo = myTivoService.cmdTivoSend(tmpCommand);

                    // Handle CHANNELUP and CHANNELDOWN which does not report a status change
                    if (tmpAct == "CHANNELUP" | tmpAct == "CHANNELDOWN") {
                        myTivoService.connTivoConnectRetry(false);
                        myTivoService.connTivoConnectRetry(true);
                    }

                    // Check to see if the command was successful
                    if (myTivo != null && myTivo.isCmdOk()) {
                        logger.debug(" Returned Tivo Data Object: '{}'", myTivo.toString());
                        updateTivoStatus(myTivo);
                    }

                    break;

            }
>>>>>>> 73f9dc6 Initial Commit
        }

        if (command == null || tivoConnection == null) {
            return;
        }
        TivoStatusData currentStatus = tivoConnection.getServiceStatus();

        String commandKeyword = null;
        // Check to see if we are running a channel scan, if so 'disable' UI commands, else chaos ensues...
        if (currentStatus != null && currentStatus.isChannelScanInProgress()) {
            logger.warn("TiVo '{}' channel scan is in progress, command '{}' will be ignored.", getThing().getUID(),
                    channelUID + " " + command);
            return;
        }

        String commandParameters = command.toString().toUpperCase();
        if (command instanceof RefreshType) {
            // Future enhancement, if we can come up with a sensible set of actions when a REFRESH is issued
            logger.info("TiVo '{}' skipping REFRESH command for channel: '{}'.", getThing().getUID(),
                    channelUID.getId());
            return;
        }

        switch (channelUID.getId()) {
            case CHANNEL_TIVO_CHANNEL_FORCE:
                commandKeyword = "FORCECH";
                break;
            case CHANNEL_TIVO_CHANNEL_SET:
                commandKeyword = "SETCH";
                break;
            case CHANNEL_TIVO_COMMAND:
                // Special case, user sends KEYWORD and PARAMETERS to Item
                commandKeyword = "";
                break;
            case CHANNEL_TIVO_TELEPORT:
                commandKeyword = "TELEPORT";
                break;
            case CHANNEL_TIVO_IRCMD:
                commandKeyword = "IRCODE";
                break;
            case CHANNEL_TIVO_KBDCMD:
                commandKeyword = "KEYBOARD";
                break;
        }
        sendCommand(commandKeyword, commandParameters, currentStatus);

    }

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
    private void sendCommand(String commandKeyword, String commandParameters, TivoStatusData currentStatus) {
        TivoStatusData commandResult = null;
        logger.debug("handleCommand '{}' - {} found!", getThing().getUID(), commandKeyword);
        // Re-write command keyword if we are in STANDBY, as only IRCODE TIVO will wake the unit from
        // standby mode
        if (tivoConnection.getServiceStatus().getConnectionStatus() == ConnectionStatus.STANDBY
                && commandKeyword.contentEquals("TELEPORT") && commandParameters.contentEquals("TIVO")) {
            commandKeyword = "IRCODE " + commandParameters;
            logger.debug("TiVo '{}' TELEPORT re-mapped to IRCODE as we are in standby: '{}'", getThing().getUID(),
                    commandKeyword);
        }
        // Execute command
        if (commandKeyword.contentEquals("FORCECH") || commandKeyword.contentEquals("SETCH")) {
            commandResult = chChannelChange(commandKeyword, commandParameters);
        } else {
            commandResult = tivoConnection.cmdTivoSend(commandKeyword + " " + commandParameters);
        }

        // Post processing
        if (commandParameters.contentEquals("STANDBY")) {
            // Force thing state into STANDBY as this command does not return a status when executed
            commandResult.setConnectionStatus(ConnectionStatus.STANDBY);
        }

        // Push status updates
        if (commandResult != null && commandResult.isCmdOk()) {
            updateTivoStatus(currentStatus, commandResult);
        }

        // return commandResult;
    }

    int convertValueToInt(Object value) {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).intValue();
        }
        if (value instanceof String) {
            return Integer.valueOf((String) value);
        }
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        return (Integer) value;
    }

    boolean convertValueToBoolean(Object value) {
=======
    public void invalidConfig() {
        updateStatus(ThingStatus.OFFLINE);
    }

    int confValueToInt(Object value) {
        if (value instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) value).intValue();
        }
        if (value instanceof String) {
            return Integer.valueOf((String) value);
        }
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }

        return Integer.valueOf((Integer) value);
    }

    boolean confValueToBoolean(Object value) {
>>>>>>> 73f9dc6 Initial Commit
        return value instanceof Boolean ? ((Boolean) value) : Boolean.valueOf((String) value);
    }

    @Override
    public void initialize() {
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
=======

>>>>>>> 73f9dc6 Initial Commit
        logger.debug("Initializing a TiVo '{}' with config options", getThing().getUID());
        Configuration conf = this.getConfig();
        TivoConfigData tivoConfig = new TivoConfigData();

        Object value;
        value = conf.get(CONFIG_ADDRESS);
        if (value != null) {
            tivoConfig.setCfgHost(String.valueOf(value));
        }

        value = conf.get(CONFIG_PORT);
        if (value != null) {
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
            tivoConfig.setCfgTcpPort(convertValueToInt(value));
        }

        value = conf.get(CONFIG_CONNECTION_RETRY);
        if (value != null) {
            tivoConfig.setCfgNumConnRetry(convertValueToInt(value));
        }

        value = conf.get(CONFIG_POLL_INTERVAL);
        if (value != null) {
            tivoConfig.setCfgPollInterval(convertValueToInt(value));
        }

        value = conf.get(CONFIG_POLL_FOR_CHANGES);
        if (value != null) {
            tivoConfig.setCfgPollChanges(convertValueToBoolean(value));
        }

        value = conf.get(CONFIG_KEEP_CONNECTION_OPEN);
        if (value != null) {
            tivoConfig.setCfgKeepConnOpen(convertValueToBoolean(value));
        }

        value = conf.get(CONFIG_CMD_WAIT_INTERVAL);
        if (value != null) {
            tivoConfig.setCfgCmdWait(convertValueToInt(value));
        }

        value = conf.get(CONFIG_CH_START);
        if (value != null) {
            tivoConfig.setCfgMinChannel(convertValueToInt(value));
        }

        value = conf.get(CONFIG_CH_END);
        if (value != null) {
            tivoConfig.setCfgMaxChannel(convertValueToInt(value));
        }

        value = conf.get(CONFIG_IGNORE_SCAN);
        if (value != null) {
            tivoConfig.setCfgIgnoreChannelScan(convertValueToBoolean(value));
=======
            tivoConfig.setCfgTcpPort(confValueToInt(value));
        }

        value = conf.get(CONFIG_CONNECTION_RETRY);
        if (value != null) {
            tivoConfig.setCfgNumConnRetry(confValueToInt(value));
        }

        value = conf.get(CONFIG_POLL_INTERVAL);
        if (value != null) {
            tivoConfig.setCfgPollInterval(confValueToInt(value));
        }

        value = conf.get(CONFIG_POLL_FOR_CHANGES);
        if (value != null) {
            tivoConfig.setCfgPollChanges(confValueToBoolean(value));
        }

        value = conf.get(CONFIG_KEEP_CONNECTION_OPEN);
        if (value != null) {
            tivoConfig.setCfgKeepConnOpen(confValueToBoolean(value));
        }

        value = conf.get(CONFIG_CMD_WAIT_INTERVAL);
        if (value != null) {
            tivoConfig.setCfgCmdWait(confValueToInt(value));
        }

        value = conf.get(CONFIG_CH_START);
        if (value != null) {
            tivoConfig.setCfgMinChannel(confValueToInt(value));
        }

        value = conf.get(CONFIG_CH_END);
        if (value != null) {
            tivoConfig.setCfgMaxChannel(confValueToInt(value));
        }

        value = conf.get(CONFIG_IGNORE_SCAN);
        if (value != null) {
            tivoConfig.setCfgIgnoreChannelScan(confValueToBoolean(value));
>>>>>>> 73f9dc6 Initial Commit
        }

        value = getThing().getUID();
        if (value != null) {
            tivoConfig.setCfgIdentifier(String.valueOf(value));
        }

        value = conf.get(CONFIG_IGNORE_CHANNELS);
        if (value != null) {
            tivoConfig.setCfgIgnoreChannels(chParseIgnored(String.valueOf(conf.get(CONFIG_IGNORE_CHANNELS)),
                    tivoConfig.getCfgMinChannel(), tivoConfig.getCfgMaxChannel()));
        }

        logger.debug("TivoConfigData Obj: '{}'", tivoConfig.toString());
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        tivoConfigData = tivoConfig;

        if (tivoConnection == null) {
            tivoConnection = new TivoStatusProvider(tivoConfigData, this, false);
=======
        tivoCfgData = tivoConfig;

        if (myTivoService == null) {
            myTivoService = new TivoStatusProvider(tivoCfgData, this, false);
>>>>>>> 73f9dc6 Initial Commit
        }
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        if (tivoConfig.doChannelScan()) {
=======

        if (tivoConfig.getCfgIgnoreChannelScan()) {
>>>>>>> 73f9dc6 Initial Commit
            startChannelScan();
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        } else {
=======

        } else if (tivoConfig.isCfgPollChanges()) {
>>>>>>> 73f9dc6 Initial Commit
            startPollStatus();
        }

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        updateStatus(ThingStatus.UNKNOWN);
        lastConnectionStatus = ConnectionStatus.UNKNOWN;
=======
        updateStatus(ThingStatus.OFFLINE);
        myTivoService.statusRefresh();
>>>>>>> 73f9dc6 Initial Commit
        logger.debug("Initializing a TiVo handler for thing '{}' - finished!", getThing().getUID());

    }

    @Override
    public void dispose() {
        logger.debug("Disposing of a TiVo handler for thing '{}'", getThing().getUID());

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        if (tivoConnection != null) {
            tivoConnection.connTivoDisconnect(true);
=======
        if (refreshJob != null) {
            refreshJob.cancel(false);
        }
        if (chScanJob != null) {
            logger.warn("'{}' - Channel Scan cancelled by dispose()", getThing().getUID());
            chScanJob.cancel(false);
>>>>>>> 73f9dc6 Initial Commit
        }

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        if (refreshJob != null) {
            logger.warn("'{}' - Polling cancelled by dispose()", getThing().getUID());
            refreshJob.cancel(false);
        }
        if (chScanJob != null) {
            logger.warn("'{}' - Channel Scan cancelled by dispose()", getThing().getUID());
            chScanJob.cancel(false);
        }

=======
>>>>>>> 73f9dc6 Initial Commit
        while (chScanJob != null && !chScanJob.isDone()) {
            try {
                TimeUnit.MILLISECONDS.sleep(tivoConfigData.getCfgCmdWait());
            } catch (InterruptedException e) {
                logger.debug("Disposing '{}' while waiting for 'channelScanJob' to end error: '{}' ",
                        getThing().getUID(), e.getMessage());
            }
        }

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        tivoConnection = null;
=======
        // Ensure we close any open socket connections
        if (myTivoService != null) {
            myTivoService.connTivoConnectRetry(false);
            myTivoService = null;
        }

>>>>>>> 73f9dc6 Initial Commit
    }

    /**
     * {@link startPollStatus} scheduled job to poll for changes in state.
     */
    private void startPollStatus() {
        int firstStartDelay = tivoConfigData.getCfgPollInterval();

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        Runnable runnable = new Runnable() {
=======
        int firstStartDelay = tivoCfgData.getCfgPollInterval();

        refreshJob = scheduler.scheduleAtFixedRate(new Runnable() {
>>>>>>> 73f9dc6 Initial Commit
            @Override
            public void run() {
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
                logger.debug("startPollStatus '{}' @ rate of '{}' seconds", getThing().getUID(),
                        tivoConfigData.getCfgPollInterval());
                tivoConnection.statusRefresh();
=======
                try {
                    logger.debug("refreshJob '{}' @ rate of '{}' seconds", getThing().getUID(),
                            tivoCfgData.getCfgPollInterval());

                    myTivoService.statusRefresh();

                } catch (Exception e) {
                    logger.debug("refreshJob '{}' -  exception occurred: {}", getThing().getUID(), e);
                }
>>>>>>> 73f9dc6 Initial Commit
            }
        };

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        if (tivoConfigData.isCfgKeepConnOpen()) {
            // Run once
            refreshJob = scheduler.schedule(runnable, firstStartDelay, TimeUnit.SECONDS);
            logger.info("Status collection '{}' will start in '{}' seconds.", getThing().getUID(), firstStartDelay);
        } else if (tivoConfigData.doPollChanges()) {
            // Run at intervals
            refreshJob = scheduler.scheduleWithFixedDelay(runnable, firstStartDelay,
                    tivoConfigData.getCfgPollInterval(), TimeUnit.SECONDS);
            logger.info("Status polling '{}' will start in '{}' seconds.", getThing().getUID(), firstStartDelay);
        } else {
            // Just update the status now
            tivoConnection.statusRefresh();
        }
=======
        logger.info("refreshJob '{}' will start in '{}' seconds", getThing().getUID(), firstStartDelay);

>>>>>>> 73f9dc6 Initial Commit
    }

    /**
     * {@link startChannelScan} starts a channel scan between the minimum and maximum channel numbers. Populates the
     * {@code cfgIgnoreChannels} list which improves the performance of channel changing operations.
     */
    private void startChannelScan() {
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        int firstStartDelay = tivoConfigData.getCfgPollInterval();
=======

        int firstStartDelay = tivoCfgData.getCfgPollInterval();
>>>>>>> 73f9dc6 Initial Commit
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int minCh = tivoConfigData.getCfgMinChannel();
                int maxCh = tivoConfigData.getCfgMaxChannel();
                TivoStatusData commandResult = tivoConnection.getServiceStatus();

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
                updateState(CHANNEL_TIVO_STATUS, new StringType("CHANNEL SCAN IN PROGRESS"));
                if (tivoConnection.connTivoConnect()) {
                    tivoConnection.setChScan(true);
=======
                    int minCh = tivoCfgData.getCfgMinChannel();
                    int maxCh = tivoCfgData.getCfgMaxChannel();

                    updateState(CHANNEL_TIVO_STATUS, new StringType("CHANNEL SCAN IN PROGRESS"));
                    myTivoService.setChScan(true);
                    TivoStatusData tmpStatus = myTivoService.getServiceStatus();

>>>>>>> 73f9dc6 Initial Commit
                    // change to first channel number, this forces the channel scan to run from Min# to Max#
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
                    tivoConnection.cmdTivoSend("TELEPORT LIVETV");
                    tivoConnection.cmdTivoSend("SETCH " + minCh);
=======
                    myTivoService.cmdTivoSend("TELEPORT LIVETV");
                    myTivoService.cmdTivoSend("SETCH " + minCh);
>>>>>>> 73f9dc6 Initial Commit

                    for (int i = minCh + 1; i <= maxCh;) {
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
                        if (chScanJob.isCancelled()) {
                            // job has been cancelled, so we need to exit
=======
                        // job has been cancelled, so we need to exit
                        if (chScanJob.isCancelled()) {
>>>>>>> 73f9dc6 Initial Commit
                            logger.warn("Channel Scan for '{}' has been cancelled by configuraition parameter change",
                                    getThing().getUID());
                            updateState(CHANNEL_TIVO_STATUS, new StringType("CHANNEL SCAN CANCELLED"));
                            break;
                        }
                        logger.info("Channel Scan for '{}' testing channel num: '{}'", getThing().getUID(), i);
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
                        commandResult = chChannelChange("SETCH", String.valueOf(i));
                        if (commandResult.getChannelNum() != -1) {
                            i = commandResult.getChannelNum() + 1;
=======

                        if (tmpStatus.getConnOK() >= 2) {
                            tmpStatus = chChannelChange("SETCH", String.valueOf(i));
                        } else {
                            logger.warn("Channel Scan for '{}' has been cancelled as we are offline",
                                    getThing().getUID());
                            updateState(CHANNEL_TIVO_STATUS, new StringType("CHANNEL SCAN CANCELLED (OFFLINE)"));
                            break;
                        }

                        if (tmpStatus.getChannelNum() != -1) {
                            i = tmpStatus.getChannelNum() + 1;
>>>>>>> 73f9dc6 Initial Commit
                        } else {
                            i++;
                        }

                        if (i >= maxCh) {
                            logger.info(
                                    "Perform Channel Scan for thing '{}' has been completed successfully.  Normal operation will now commence.",
                                    getThing().getUID());
                            updateState(CHANNEL_TIVO_STATUS, new StringType("CHANNEL SCAN COMPLETE"));
                        }

                    }

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
                    tivoConnection.cmdTivoSend("SETCH " + minCh);

                } else {
                    logger.warn("Channel Scan for '{}' failed - unable to connect (offline)", getThing().getUID());
                    updateState(CHANNEL_TIVO_STATUS, new StringType("CHANNEL SCAN CANCELLED (OFFLINE)"));
=======
                    myTivoService.cmdTivoSend("SETCH " + minCh);
                    Configuration conf = editConfiguration();
                    conf.put(CONFIG_IGNORE_SCAN, false);
                    updateConfiguration(conf);
                    myTivoService.setChScan(false);
                    thingUpdated(getThing());

                } catch (Exception e) {
                    logger.debug("Exception occurred during Channel Scan: {}", e);
>>>>>>> 73f9dc6 Initial Commit
                }
                Configuration conf = editConfiguration();
                conf.put(CONFIG_IGNORE_SCAN, false);
                updateConfiguration(conf);
                tivoConnection.setChScan(false);
                thingUpdated(getThing());
            }
        };
        chScanJob = scheduler.schedule(runnable, firstStartDelay, TimeUnit.SECONDS);
        logger.info("Channel Scanning job for thing '{}' will start in '{}' seconds.  TiVo will scan all channels!",
                getThing().getUID(), firstStartDelay);
    }

    /**
     * {@link chChannelChange} performs channel changing operations. Checks {@link chCheckIgnored} channel numbers to
     * improve performance, reads the response and adds any new invalid channels {@link chAddIgnored}. Calls
     * {@link chGetNext} to determine the direction of channel change.
     *
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
     * @param commandKeyword the TiVo command object.
=======
     * @param tivoCommand the TiVo command object.
>>>>>>> 73f9dc6 Initial Commit
     * @param command the command parameter.
     * @return int channel number.
     */
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
    private TivoStatusData chChannelChange(String commandKeyword, String command) {
        int chnl = tivoConfigData.getCfgMinChannel();
        TivoStatusData tmpStatus = tivoConnection.getServiceStatus();
=======
    private TivoStatusData chChannelChange(String tivoCommand, String command) {
>>>>>>> 73f9dc6 Initial Commit

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        // compare this to the current channel and determine the "direction" (up or down)
        int numTries = 10;
        chnl = Integer.valueOf(command.toString()).intValue();
=======
        int chnl = tivoCfgData.getCfgMinChannel();
        TivoStatusData tmpStatus = myTivoService.getServiceStatus();

>>>>>>> 73f9dc6 Initial Commit
        try {
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
=======
            // compare this to the current channel and determine the "direction" (up or down)
            int numTries = 10;
            chnl = Integer.valueOf(command.toString()).intValue();

>>>>>>> 73f9dc6 Initial Commit
            // check for ignored channels execute, check and learn new ignored channels
            while (numTries > 0 && chnl > 0) {
                numTries--;

                while (chCheckIgnored(chnl) && chGetNext(chnl, tmpStatus) > 0) {
                    logger.info("chChannelChange '{}' skipping channel: '{}'", getThing().getUID(), chnl);
                    chnl = chGetNext(chnl, tmpStatus);
                }

<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
                String tmpCommand = commandKeyword + " " + chnl;
=======
                String tmpCommand = tivoCommand + " " + chnl;

>>>>>>> 73f9dc6 Initial Commit
                logger.debug("chChannelChange '{}' sending command to tivo: '{}'", getThing().getUID(), tmpCommand);

                // Attempt to execute the command on the tivo
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
                tivoConnection.cmdTivoSend(tmpCommand);
                try {
                    TimeUnit.MILLISECONDS.sleep(tivoConfigData.getCfgCmdWait() * 2);
                } catch (Exception e) {
                }
                tmpStatus = tivoConnection.getServiceStatus();
=======
                tmpStatus = myTivoService.cmdTivoSend(tmpCommand);
>>>>>>> 73f9dc6 Initial Commit

                // Check to see if the command was successful
                if (tmpStatus != null && tmpStatus.isCmdOk()) {
                    numTries = 0;
                    if (tmpStatus.getMsg().contains("CH_STATUS")) {
                        return tmpStatus;
                    }

                } else if (tmpStatus != null) {
                    logger.warn("TiVo'{}' set channel command failed '{}' with msg '{}'", getThing().getUID(),
                            tmpCommand, tmpStatus.getMsg());
                    switch (tmpStatus.getMsg()) {
                        case "CH_FAILED INVALID_CHANNEL":
                            chAddIgnored(chnl);
                            chnl = chGetNext(chnl, tmpStatus);
                            if (chnl > 0) {
                                logger.debug("chChannelChange '{}' retrying next channel '{}'", getThing().getUID(),
                                        chnl);
                            } else {
                                numTries = 0;
                            }
                        case "CH_FAILED NO_LIVE":
                            tmpStatus.setChannelNum(chnl);
                            return tmpStatus;
                        case "CH_FAILED REORDING":
                        case "NO_STATUS_DATA_RETURNED":
                            tmpStatus.setChannelNum(-1);
                            return tmpStatus;
                    }

                    logger.info("TiVo'{}' retrying next channel '{}'", getThing().getUID(), chnl);
                }

            }

        } catch (NumberFormatException e) {
            logger.error("TiVo'{}' unable to parse channel integer from CHANNEL_TIVO_CHANNEL: '{}'",
                    getThing().getUID(), command.toString());
        }
        return tmpStatus;
    }

    /**
     * {@link chParseIgnored} parses the channels to ignore and populates {@link TivoConfigData}
     * {@code cfgIgnoreChannels} object with a sorted list of the channel numbers to ignore.
     *
     * @param pChannels source channel list.
     * @param chMin minimum channel number.
     * @param chMax maximum channel number.
     * @return the sorted set
     */
    private SortedSet<Integer> chParseIgnored(String pChannels, Integer chMin, Integer chMax) {
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
=======

>>>>>>> 73f9dc6 Initial Commit
        logger.debug("chParseIgnored '{}' called doCfgParseIgnoreChannel with list: '{}'", getThing().getUID(),
                pChannels);

        SortedSet<Integer> result = new TreeSet<Integer>();

        if (pChannels.equals("null") || pChannels.isEmpty()) {
            return result;
        }

        if (pChannels.contains("[") | pChannels.contains("]")) {
            pChannels = pChannels.replace("[", "");
            pChannels = pChannels.replace("]", "");
        }

        List<String> tmp = Arrays.asList(pChannels.split("\\s*,\\s*"));

        try {
            for (int i = 0; i < tmp.size(); i++) {
                // Determine if we have a range with a '-' in it.
                if (tmp.get(i).matches(".+-.+")) {
                    List<String> sTmp = Arrays.asList(tmp.get(i).split("-"));
                    if (sTmp != null && sTmp.size() == 2) {

                        Double ds = Double.valueOf(sTmp.get(0));
                        Integer is = Integer.valueOf(ds.intValue());

                        Double de = Double.valueOf(sTmp.get(1));
                        Integer ie = Integer.valueOf(de.intValue());

                        if (ie < is) {
                            ds = Double.valueOf(sTmp.get(1));
                            is = Integer.valueOf(ds.intValue());

                            de = Double.valueOf(sTmp.get(0));
                            ie = Integer.valueOf(de.intValue());
                        }
                        while (is <= ie) {
                            if (!result.contains(is)) {
                                result.add(is);
                            }
                            is++;
                        }
                    } else {
                        logger.warn(
                                " chParseIgnored '{}' - parser matched - on string but didn't have an expected size",
                                getThing().getUID());
                    }
                } else {

                    Double de = Double.valueOf(tmp.get(i));
                    Integer se = Integer.valueOf(de.intValue());

                    if (result.contains(se)) {
                        logger.debug(" chParseIgnored '{}' - element already in list - '{}'", se);
                    } else {
                        if (se > chMin && se < chMax) {
                            result.add(se);
                        } else {
                            result.remove(se);
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            logger.warn(
                    " chParseIgnored '{}' was unable to parse list of 'Channels to Ignore' from thing settings: {}, error '{}'",
                    getThing().getUID(), pChannels, e);
            return result;

        }

        logger.debug(" chParseIgnored '{}' result will be used as the channel ignore channel list: {}",
                getThing().getUID(), result);

        // Re-parse the list (if populated) to make this more manageable in the UI
        if (result.size() > 0) {
            Integer[] uiArr = result.toArray(new Integer[result.size()]);
            String uiResult;
            if (result.size() > 1) {
                uiResult = chParseRange(uiArr);
            } else {
                uiResult = uiArr[0].toString();
            }

            logger.debug(" chParseIgnored '{}' uiResult will be posted back to the consoles: {}", getThing().getUID(),
                    uiResult);

            Configuration conf = editConfiguration();
            conf.put(CONFIG_IGNORE_CHANNELS, uiResult);
            updateConfiguration(conf);
        }
        return result;
    }

    /**
     * {@link chParseRange} re-parses the channels to ignore in {@code cfgIgnoreChannels}. Replaces consecutive numbers
     * with a range to reduce size of list in UIs.
     *
     * @param Integer array of channel numbers
     * @return string list of channel numbers with consecutive numbers returned as ranges.
     */

    private String chParseRange(Integer[] nums) {
        StringBuilder sb = new StringBuilder();
        int rangeStart = nums[0];
        int previous = nums[0];
        int current;
        int expected = previous + 1;

        for (int i = 1; i < nums.length; i++) {
            current = nums[i];
            expected = previous + 1;
            if (current != expected || i == (nums.length - 1)) {
                if (current == rangeStart) {
                    sb.append(previous + ",");
                } else {
                    if (rangeStart != previous) {
                        if (i == nums.length - 1) {
                            sb.append(rangeStart + "-" + current);
                        } else {
                            sb.append(rangeStart + "-" + previous + ",");
                        }
                    } else {
                        if (i == nums.length - 1) {
                            sb.append(rangeStart + "," + current);
                        } else {
                            sb.append(rangeStart + ",");
                        }
                    }
                }
                rangeStart = current;
            }
            previous = current;
        }
        if (sb.length() > 1) {
            if (sb.charAt(sb.length() - 1) == ',') {
                sb.deleteCharAt(sb.length() - 1);
            }
        }
        return sb.toString();
    }

    /**
     * {@link updateTivoStatus} populates the items with the status / channel information.
     *
     * @param tivoStatusData the {@link TivoStatusData}
     */
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
    public void updateTivoStatus(TivoStatusData oldStatusData, TivoStatusData newStatusData) {
        if (newStatusData != null && !tivoConfigData.doChannelScan()) {
            // Update Item Status
            if (newStatusData.getPubToUI()) {
                if (oldStatusData == null || !(oldStatusData.getMsg().contentEquals(newStatusData.getMsg()))) {
                    updateState(CHANNEL_TIVO_STATUS, new StringType(newStatusData.getMsg()));
                }
=======
    public void updateTivoStatus(TivoStatusData tivoStatusData) {

        // This will update the TiVO status and channel numbers when a channel change command has been issued.
        if (tivoStatusData != null) {
            if (tivoStatusData.getPubToUI()) {
                updateState(CHANNEL_TIVO_STATUS, new StringType(tivoStatusData.getMsg()));

>>>>>>> 73f9dc6 Initial Commit
                // If the cmd was successful, publish the channel channel numbers
                if (newStatusData.isCmdOk() && newStatusData.getChannelNum() != -1) {
                    if (oldStatusData == null || oldStatusData.getChannelNum() != newStatusData.getChannelNum()) {
                        updateState(CHANNEL_TIVO_CHANNEL_FORCE, new DecimalType(newStatusData.getChannelNum()));
                        updateState(CHANNEL_TIVO_CHANNEL_SET, new DecimalType(newStatusData.getChannelNum()));
                    }
                }

                // Now set the pubToUI flag to false, as we have already published this status
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
                if (isLinked(CHANNEL_TIVO_STATUS) || isLinked(CHANNEL_TIVO_CHANNEL_FORCE)
                        || isLinked(CHANNEL_TIVO_CHANNEL_SET)) {
                    newStatusData.setPubToUI(false);
                    tivoConnection.setServiceStatus(newStatusData);
                }
            } else {
                if (newStatusData.getMsg().contentEquals("COMMAND_TIMEOUT")) {
                    tivoConnection.connTivoDisconnect(false);
=======
                if (isLinked(CHANNEL_TIVO_STATUS) | isLinked(CHANNEL_TIVO_CHANNEL_FORCE)
                        | isLinked(CHANNEL_TIVO_CHANNEL_SET)) {
                    tivoStatusData.setPubToUI(false);
                    myTivoService.setServiceStatus(tivoStatusData);
>>>>>>> 73f9dc6 Initial Commit
                }
            }
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
            // Update Thing status
            if (newStatusData.getConnectionStatus() != lastConnectionStatus) {
                switch (newStatusData.getConnectionStatus()) {
                    case OFFLINE:
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                "Power on device or check network configuration/connection.");
                        break;
                    case ONLINE:
                        updateStatus(ThingStatus.ONLINE);
                        break;
                    case STANDBY:
                        updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE,
                                "STANDBY MODE: Send command TIVO to Remote Control Button (IRCODE) item to wakeup.");
                        break;
                    case UNKNOWN:
                        updateStatus(ThingStatus.INITIALIZING);
                        break;
                }
                lastConnectionStatus = newStatusData.getConnectionStatus();
            }
=======
>>>>>>> 73f9dc6 Initial Commit
        }
    }

    /**
     * {@link chAddIgnored} adds a channel number to the list of Ignored Channels.
     *
     * @param pChannel the channel number.
     */
    private void chAddIgnored(Integer pChannel) {
        logger.info("chAddIgnored '{}' Adding new ignored channel '{}'", getThing().getUID(), pChannel);

        // if we already see this channel as being ignored there is no reason to ignore it again.
        if (chCheckIgnored(pChannel)) {
            return;
        }

        tivoConfigData.addCfgIgnoreChannels(pChannel);

        // Re-parse the sorted set and publish to UI
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        SortedSet<Integer> myConfig = tivoConfigData.getCfgIgnoreChannels();
=======
        SortedSet<Integer> myConfig = tivoCfgData.getCfgIgnoreChannels();
>>>>>>> 73f9dc6 Initial Commit
        Integer[] uiArr = myConfig.toArray(new Integer[myConfig.size()]);
        String uiResult = chParseRange(uiArr);

        Configuration conf = editConfiguration();
        conf.put(CONFIG_IGNORE_CHANNELS, uiResult);
        updateConfiguration(conf);

    }

    /**
     * {@link chGetNext} gets the next channel number, depending on the direction of navigation (based on the current
     * channel vs new proposed number).
     *
     * @param pChannel the channel number.
     * @return the next channel number.
     */
    private int chGetNext(int pChannel, TivoStatusData tivoStatusData) {
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
=======

>>>>>>> 73f9dc6 Initial Commit
        if (chCheckIgnored(pChannel)) {
            if (tivoStatusData != null && tivoStatusData.isCmdOk()) {
                // retry logic is allowed otherwise we only do the logic below once.

                if (tivoStatusData.getChannelNum() > pChannel) {
                    // we appear to be changing the channel DOWNWARD so we try to go down -1
                    if (pChannel < tivoConfigData.getCfgMinChannel()) {
                        pChannel = tivoConfigData.getCfgMinChannel();
                    } else {
                        pChannel--;
                    }
                } else if (tivoStatusData.getChannelNum() <= pChannel) {
                    // we appear to be changing the channel UPWARD so we try to go up +1
                    if (pChannel > tivoConfigData.getCfgMaxChannel()) {
                        pChannel = tivoConfigData.getCfgMaxChannel();
                    } else {
                        pChannel++;
                    }
                } else {
                    // either we are going to attempt to change a channel to less then 1 or
                    // its already on the same channel. we shouldn't retry this here.
                    return -1;
                }

            } else if (tivoStatusData != null) {
                pChannel++;
            }
        }
        logger.debug("chGetNext '{}' next proposed channel '{}'", getThing().getUID(), pChannel);
        return pChannel;

    }

    /**
     * {@link chCheckIgnored} checks if the passed TV channel number is contained within the list of stored
     * channels contained within {@link getCfgIgnoreChannels}.
     *
     * @param pChannel the TV channel number to test.
     * @return true= channel is contained within the list, false= channel number is not contained within the list.
     */
    private boolean chCheckIgnored(int pChannel) {
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
        if (tivoConfigData.getCfgIgnoreChannels() != null && tivoConfigData.getCfgIgnoreChannels().contains(pChannel)) {
=======

        if (tivoCfgData.getCfgIgnoreChannels() != null && tivoCfgData.getCfgIgnoreChannels().contains(pChannel)) {
>>>>>>> 73f9dc6 Initial Commit
            return true;
        } else {
            return false;
        }
    }
<<<<<<< Upstream, based on branch 'master' of https://github.com/AndyXMB/openhab2-addons.git
=======

    /**
     * {@link setTivoStatus} changed the thing status to online. Typically called from the child status polling job when
     * connections can be made to the TiVo and status codes are returned.
     *
     * @param thingStatusDetail the thingStatusDetail
     * @param strMsg the error message / reason why the device is offline (displayed in the GUI)
     */
    public void setTivoStatus(ThingStatus thingStatus, ThingStatusDetail thingStatusDetail, String strMsg) {

        updateStatus(thingStatus, thingStatusDetail, strMsg);

    }

>>>>>>> 73f9dc6 Initial Commit
}
