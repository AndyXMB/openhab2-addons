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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryListener;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceRegistry;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.tivo.internal.service.TivoConfigData;
import org.openhab.binding.tivo.internal.service.TivoConfigStatusProvider;
import org.openhab.binding.tivo.internal.service.TivoStatusData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * The {@link TiVoHandler} is the BaseThingHandler responsible for handling commands that are
 * sent to one of the Tivo's channels.
 *
 * @author Jayson Kubilis - Initial contribution
 * @author Andrew Black - Updates / compilation corrections. Addition of channel scanning functionality.
 */

public class TiVoHandler extends BaseThingHandler implements DiscoveryListener {
    private Logger logger = LoggerFactory.getLogger(TiVoHandler.class);
    private TivoStatusData tivoStatusData = null;
    private TivoConfigData tivoCfgData = null;
    private TivoConfigStatusProvider myTivoService = null;
    private ScheduledFuture<?> refreshJob;
    private ScheduledFuture<?> channelScanJob;
    private DiscoveryServiceRegistry discoveryServiceRegistry;

    /**
     * Instantiates a new TiVo handler.
     *
     * @param thing the thing
     * @param discoveryServiceRegistry the discovery service registry
     */
    // public TiVoHandler(Thing thing, DiscoveryServiceRegistry discoveryServiceRegistry) {
    public TiVoHandler(Thing thing, DiscoveryServiceRegistry discoveryServiceRegistry) {
        super(thing);
        if (discoveryServiceRegistry != null) {
            this.discoveryServiceRegistry = discoveryServiceRegistry;
        }
        logger.debug("Creating a TiVoHandler for thing '{}'", getThing().getUID());
    }

    /**
     * {@link handleCommand} handles and command actions processed by any of the channels.
     *
     * @see
     *      org.eclipse.smarthome.core.thing.binding.ThingHandler#handleCommand(org.eclipse.smarthome.core.thing.ChannelUID,
     *      org.eclipse.smarthome.core.types.Command)
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        // Handles the commands from the various TiVo channel objects
        logger.debug("Received command on channel: {}, command: {}", channelUID, command);

        if (command != null && myTivoService != null) {

            TivoStatusData myTivo = null;
            String tivoCommand = null;

            if (!isInitialized()) {
                logger.debug("TiVo '{}' device is not intialised yet, command '{}' will be ignored.",
                        getThing().getUID(), channelUID + " " + command);
                return;
            }

            // Check to see if we are running a channel scan, if so 'disable' UI commands, else chaos ensues...
            if (tivoStatusData != null && tivoStatusData.getChScan()) {
                logger.warn("TiVo '{}' channel scan is in progress, command '{}' will be ignored.", getThing().getUID(),
                        channelUID + " " + command);
                return;
            }

            String tmpAct = command.toString().toUpperCase();
            if (command instanceof RefreshType) {

                if (tivoStatusData != null) {
                    tivoStatusData.setPubToUI(true);
                } else {
                    tivoStatusData = myTivoService.doRefreshDeviceStatus();
                }

                switch (channelUID.getId()) {

                    case CHANNEL_TIVO_STATUS:
                    case CHANNEL_TIVO_CHANNEL_FORCE:
                    case CHANNEL_TIVO_CHANNEL_SET:
                        if (tivoStatusData != null) {
                            updateTivoStatus(tivoStatusData);
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
                    logger.debug("  CHANNEL_TIVO FORCECH/SETCH found!");

                    if (tivoCommand == null) {
                        tivoCommand = "SETCH";
                    }

                    doChannelChange(tivoCommand, tmpAct);
                    updateTivoStatus(tivoStatusData);

                    break;

                case CHANNEL_TIVO_COMMAND:
                    logger.debug("  CHANNEL_TIVO COMMAND found!");

                    logger.debug("TiVo '{}' sending CUSTOM command: '{}'", getThing().getUID(), tmpAct);

                    // Attempt to execute the command on the tivo
                    myTivo = myTivoService.setTivoCommand(tmpAct);

                    // Check to see if the command was successful
                    if (myTivo != null && myTivo.isCmdOk()) {
                        tivoStatusData = myTivo;
                        logger.debug(" Returned Tivo Data Object: '{}'", myTivo.toString());
                    } else {
                        logger.warn("TiVo '{}' command failed '{}'", getThing().getUID(), tmpAct);
                    }

                    myTivo = null;

                    updateTivoStatus(tivoStatusData);

                    break;
                case CHANNEL_TIVO_TELEPORT:
                    logger.debug("  CHANNEL_TIVO TELEPORT found!");

                    tivoCommand = "TELEPORT " + tmpAct;

                    logger.debug("TiVo '{}' TELEPORT command to tivo: '{}'", getThing().getUID(), tivoCommand);

                    // Attempt to execute the command on the tivo
                    myTivo = myTivoService.setTivoCommand(tivoCommand);

                    // Check to see if the command was successful
                    if (myTivo != null && myTivo.isCmdOk()) {
                        tivoStatusData = myTivo;
                        logger.debug("Returned Tivo Data Object: '{}'", myTivo.toString());
                    }

                    myTivo = null;

                    updateTivoStatus(tivoStatusData);

                    break;

                case CHANNEL_TIVO_IRCMD:
                    tivoCommand = "IRCODE";
                case CHANNEL_TIVO_KBDCMD:
                    logger.debug("CHANNEL_TIVO IRCODE/KBDCMD found!");

                    if (tivoCommand == null) {
                        tivoCommand = "KEYBOARD";
                    }

                    String tmpCommand = tivoCommand + " " + tmpAct;

                    logger.debug("TiVo '{}' IR/KBD command to tivo: '{}'", getThing().getUID(), tmpCommand);

                    // Attempt to execute the command on the tivo
                    myTivo = myTivoService.setTivoCommand(tmpCommand);

                    // Handle CHANNELUP and CHANNELDOWN which change channel, but do not report a status change
                    // New connection always returns the new channel details
                    if (tmpAct == "CHANNELUP" | tmpAct == "CHANNELDOWN") {
                        myTivoService.setTivoConnect(false);
                        myTivoService.setTivoConnect(true);
                    }

                    // Check to see if the command was successful
                    if (myTivo != null && myTivo.isCmdOk()) {
                        tivoStatusData = myTivo;
                        logger.debug(" Returned Tivo Data Object: '{}'", myTivo.toString());
                    }

                    myTivo = null;

                    updateTivoStatus(tivoStatusData);

                    break;

            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#initialize()
     */
    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);

        logger.debug("Initializing a TiVo '{}' with config options", getThing().getUID());

        Configuration conf = this.getConfig();
        TivoConfigData tivoConfig = new TivoConfigData();
        tivoConfig.setCfgHost(String.valueOf(conf.get(CONFIG_ADDRESS)));
        tivoConfig.setCfgTcpPort(Integer.parseInt(String.valueOf(conf.get(CONFIG_PORT))));
        tivoConfig.setCfgNumConnRetry(Integer.parseInt(String.valueOf(conf.get(CONFIG_CONNECTION_RETRY))));
        tivoConfig.setCfgPollInterval(Integer.parseInt(String.valueOf(conf.get(CONFIG_POLL_INTERVAL))));
        tivoConfig.setCfgPollChanges(Boolean.parseBoolean(String.valueOf(conf.get(CONFIG_POLL_FOR_CHANGES))));
        tivoConfig.setCfgKeepConnOpen(Boolean.parseBoolean(String.valueOf(conf.get(CONFIG_KEEP_CONNECTION_OPEN))));
        tivoConfig.setCfgCmdWait(Integer.parseInt(String.valueOf(conf.get(CONFIG_CMD_WAIT_INTERVAL))));
        tivoConfig.setCfgMinChannel(Integer.parseInt(String.valueOf(conf.get(CONFIG_CH_START))));
        tivoConfig.setCfgMaxChannel(Integer.parseInt(String.valueOf(conf.get(CONFIG_CH_END))));
        tivoConfig.setCfgIgnoreChannelScan(Boolean.parseBoolean(String.valueOf(conf.get(CONFIG_IGNORE_SCAN))));
        tivoConfig.setCfgIdentifier(String.valueOf(getThing().getUID()));
        tivoConfig.setCfgIgnoreChannels(doCfgParseIgnoreChannel(String.valueOf(conf.get(CONFIG_IGNORE_CHANNELS)),
                tivoConfig.getCfgMinChannel(), tivoConfig.getCfgMaxChannel()));

        logger.debug("TivoConfigData Obj: '{}'", tivoConfig.toString());

        tivoCfgData = tivoConfig;
        if (myTivoService == null) {
            myTivoService = new TivoConfigStatusProvider(tivoCfgData, tivoStatusData, this, false);
        }
        myTivoService.setTivoConnect(true);

        if (tivoConfig.getCfgIgnoreChannelScan()) {
            // We want to create a job to scan all of the channels
            if (refreshJob != null) {
                refreshJob.cancel(true);
            }
            startChannelScan();

        } else if (tivoConfig.isCfgPollChanges()) {
            // We want a regular status polling or an initial call to get the status
            if (channelScanJob != null) {
                channelScanJob.cancel(true);
            }
            startPollStatus();
        }

        if (this.discoveryServiceRegistry != null) {
            this.discoveryServiceRegistry.addDiscoveryListener(this);
        }

        logger.debug("Initializing a TiVo handler for thing '{}' - finished!", getThing().getUID());

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#dispose()
     */
    @Override
    public void dispose() {

        logger.debug("Disposing of a TiVo handler for thing '{}'", getThing().getUID());

        if (refreshJob != null) {
            refreshJob.cancel(false);
        }
        if (channelScanJob != null) {
            logger.warn("'{}' - Channel Scan cancelled by dispose()", getThing().getUID());
            channelScanJob.cancel(false);
        }

        while (channelScanJob != null && !channelScanJob.isDone()) {
            try {
                TimeUnit.MILLISECONDS.sleep(tivoCfgData.getCfgCmdWait());
            } catch (InterruptedException e) {
                logger.debug("Disposing '{}' while waiting for 'channelScanJob' to end error: '{}' ",
                        getThing().getUID(), e.getMessage());
            }
        }

        // Ensure we close any open socket connections
        if (myTivoService != null) {
            myTivoService.setTivoConnect(false);
            myTivoService = null;
        }

    }

    /**
     * Start poll status.
     */
    private void startPollStatus() {

        int firstStartDelay = tivoCfgData.getCfgPollInterval();
        // int firstStartDelay = 0;

        refreshJob = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.debug("Refreshing thing '{}' @ rate of '{}' seconds", getThing().getUID(),
                            tivoCfgData.getCfgPollInterval());

                    myTivoService.doRefreshDeviceStatus();

                } catch (Exception e) {
                    logger.debug("Exception occurred during Refresh: {}", e);
                }
            }
        }, firstStartDelay, tivoCfgData.getCfgPollInterval(), TimeUnit.SECONDS);

        logger.info("First polling job for thing '{}' will start in '{}' seconds", getThing().getUID(),
                firstStartDelay);

    }

    /**
     * Start channel scan.
     */
    private void startChannelScan() {

        int firstStartDelay = tivoCfgData.getCfgPollInterval();
        // int firstStartDelay = 30;
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {

                    int minCh = tivoCfgData.getCfgMinChannel();
                    int maxCh = tivoCfgData.getCfgMaxChannel();
                    int chNum = -1;

                    updateState(CHANNEL_TIVO_STATUS, new StringType("CHANNEL SCAN IN PROGRESS"));

                    // change to first channel number, this forces the channel scan to run from Min# to Max#
                    myTivoService.setTivoCommand("TELEPORT LIVETV");
                    myTivoService.setTivoCommand("SETCH " + minCh);

                    for (int i = minCh + 1; i <= maxCh;) {
                        // job has been cancelled, so we need to exit
                        if (channelScanJob.isCancelled()) {
                            logger.warn(
                                    "Perform Channel Scan for thing '{}' has been cancelled by configuraition parameter change",
                                    getThing().getUID());
                            return;
                        }
                        logger.info("Channel Scan for thing '{}' testing channel number - '{}'", getThing().getUID(),
                                i);
                        // tmpStatus = myTivoService.setTivoCommand("SETCH " + i);

                        if (myTivoService.setTivoConnect(true)) {
                            chNum = doChannelChange("SETCH", String.valueOf(i));
                        }

                        if (chNum != -1) {
                            i = chNum + 1;
                        } else {
                            i++;
                        }

                    }

                    myTivoService.setTivoCommand("SETCH " + minCh);
                    // tivoStatusData = new TivoStatusData(false, -1, "CHANNEL SCAN COMPLETED", true, false);
                    // updateTivoStatus(tivoStatusData);

                    logger.info("Channel scan for thing '{}' completed", getThing().getUID());
                    Configuration conf = editConfiguration();
                    conf.put(CONFIG_IGNORE_SCAN, false);
                    updateConfiguration(conf);

                    // Now resetting the Thing
                    thingUpdated(getThing());

                    logger.info(
                            "Perform Channel Scan for thing '{}' has been completed successfully.  Normal operation will now commence.",
                            getThing().getUID());

                } catch (Exception e) {
                    logger.debug("Exception occurred during Channel Scan: {}", e);
                }
            }
        };
        channelScanJob = scheduler.schedule(runnable, firstStartDelay, TimeUnit.SECONDS);
        logger.info("Channel Scanning job for thing '{}' will start in '{}' seconds.  TiVo will scan all channels!",
                getThing().getUID(), firstStartDelay);
    }

    /**
     * Channel change.
     *
     * @param tivoCommand the tivo command
     * @param command the command
     * @return the int
     */
    private int doChannelChange(String tivoCommand, String command) {

        int chnl = tivoCfgData.getCfgMinChannel();
        TivoStatusData tmpStatus = null;

        try {
            // if we can compare this to the current channel we will try and determine the "direction" (up or
            // down) we are going
            int numTries = 100;
            chnl = Integer.valueOf(command.toString()).intValue();

            // check for ignored channels execute, check and learn new ignored channels
            while (numTries > 0 && chnl > 0) {
                numTries--;

                while (getChkIgnoredChannel(chnl) && getNextChannel(chnl) > 0) {
                    logger.debug("TiVo'{}' skipping channel: '{}'", getThing().getUID(), chnl);
                    chnl = getNextChannel(chnl);
                }

                String tmpCommand = tivoCommand + " " + chnl;

                logger.debug("TiVo'{}' sending command to tivo: '{}'", getThing().getUID(), tmpCommand);

                // Attempt to execute the command on the tivo
                tmpStatus = myTivoService.setTivoCommand(tmpCommand);

                // Check to see if the command was successful
                if (tmpStatus != null && tmpStatus.isCmdOk()) {
                    numTries = 0;
                    if (tmpStatus.getMsg().contains("CH_STATUS")) {
                        tivoStatusData = tmpStatus;
                        return tivoStatusData.getChannelNum();
                    }

                } else if (tmpStatus != null) {
                    logger.warn("TiVo'{}' set channel command failed '{}' with msg '{}'", getThing().getUID(),
                            tmpCommand, tmpStatus.getMsg());
                    switch (tmpStatus.getMsg()) {
                        case "CH_FAILED INVALID_CHANNEL":
                            addIgnoredChannel(chnl);
                            chnl = getNextChannel(chnl);
                            if (chnl > 0) {
                                logger.info("TiVo'{}' retrying next channel '{}'", getThing().getUID(), chnl);
                            } else {
                                numTries = 0;
                            }
                        case "CH_FAILED NO_LIVE":
                        case "CH_FAILED REORDING":
                        case "NO_STATUS_DATA_RETURNED":
                            return -1;
                    }

                    logger.info("TiVo'{}' retrying next channel '{}'", getThing().getUID(), chnl);
                }

            }

        } catch (NumberFormatException e) {
            logger.error("TiVo'{}' unable to parse channel integer from CHANNEL_TIVO_CHANNEL: '{}'",
                    getThing().getUID(), command.toString());
        }

        return chnl;
    }

    /**
     * Do cfg parse ignore channel.
     *
     * @param pChannels the channels
     * @return the sorted set
     */
    private SortedSet<Integer> doCfgParseIgnoreChannel(String pChannels, Integer chMin, Integer chMax) {

        logger.debug("TiVo'{}' called doCfgParseIgnoreChannel with list: '{}'", getThing().getUID(), pChannels);

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
                // logger.debug(" parser parsing '{}'", tmp.get(i));

                // Determine if we have a string with a '-' in it.
                if (tmp.get(i).matches(".+-.+")) {
                    // logger.debug(" parser matched dash (-) on string '{}'", tmp.get(i));
                    List<String> sTmp = Arrays.asList(tmp.get(i).split("-"));
                    if (sTmp != null && sTmp.size() == 2) {

                        Double ds = Double.valueOf(sTmp.get(0));
                        Integer is = Integer.valueOf(ds.intValue());

                        Double de = Double.valueOf(sTmp.get(1));
                        Integer ie = Integer.valueOf(de.intValue());

                        // logger.debug(" found start '{}' and end '{}'", is, ie);

                        if (ie < is) { // some funny guy eh?

                            ds = Double.valueOf(sTmp.get(1));
                            is = Integer.valueOf(ds.intValue());

                            de = Double.valueOf(sTmp.get(0));
                            ie = Integer.valueOf(de.intValue());

                        }
                        while (is <= ie) {
                            if (result.contains(is)) {
                                logger.debug("  element already in list - '{}'", is);
                            } else {
                                logger.debug("  adding element to list - '{}'", is);
                                result.add(is);
                            }
                            is++;
                        }
                    } else {
                        // logger.debug(" parser matched - on string but didn't have an expected size");
                    }
                } else {

                    Double de = Double.valueOf(tmp.get(i));
                    Integer se = Integer.valueOf(de.intValue());

                    // logger.debug(" parser didn't match - on string '{}' ('{}' as Integer), must be singleton",
                    // tmp.get(i), se);

                    if (result.contains(se)) {
                        // logger.debug(" element already in list - '{}'", se);
                    } else {
                        if (se > chMin && se < chMax) {
                            // Checks the number is within the range of min and max channels
                            // logger.debug(" adding element to list - '{}'", se);
                            result.add(se);
                        } else {
                            // logger.debug(" remove element from list - '{}'", se);
                            result.remove(se);
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("TiVo'{}' was unable to parse list of 'Channels to Ignore' from thing settings: {}, error '{}'",
                    getThing().getUID(), pChannels, e);
            return result;

        }

        // Re-parse the list (if populated) to make this more manageable in the UI
        if (result.size() > 0) {
            Integer[] uiArr = result.toArray(new Integer[result.size()]);
            String uiResult = getParsedChannels(uiArr);

            logger.debug("TiVo'{}' uiResult will be posted back to the consoles: {}", getThing().getUID(), uiResult);

            Configuration conf = editConfiguration();
            conf.put(CONFIG_IGNORE_CHANNELS, uiResult);
            updateConfiguration(conf);
        }
        return result;
    }

    public static String getParsedChannels(Integer[] nums) {
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
     * Update tivo status.
     *
     * @param tivoStatusData the tivo status data
     */
    public void updateTivoStatus(TivoStatusData tivoStatusData) {

        // This will update the TiVO status and channel numbers when a channel change command has been issued.
        if (tivoStatusData != null) {

            // If the Publish to UI is true
            if (tivoStatusData.getPubToUI()) {
                updateState(CHANNEL_TIVO_STATUS, new StringType(tivoStatusData.getMsg()));

                // If the cmd was successful, publish the channel channel numbers
                if (tivoStatusData.isCmdOk() && tivoStatusData.getChannelNum() != -1) {
                    updateState(CHANNEL_TIVO_CHANNEL_FORCE, new DecimalType(tivoStatusData.getChannelNum()));
                    updateState(CHANNEL_TIVO_CHANNEL_SET, new DecimalType(tivoStatusData.getChannelNum()));
                }

                // Now set the pubToUI flag to false, as we have already published this status
                if (isLinked(CHANNEL_TIVO_STATUS) | isLinked(CHANNEL_TIVO_CHANNEL_FORCE)
                        | isLinked(CHANNEL_TIVO_CHANNEL_SET)) {
                    tivoStatusData.setPubToUI(false);
                }
            }

        } else {
            logger.warn(
                    "While refreshing thing '{}' no tivo status found to report! TiVo may be in standby (power saving) mode.",
                    getThing().getUID());
        }
    }

    /**
     * Adds the ignored channel.
     *
     * @param pChannel the channel
     */
    private void addIgnoredChannel(Integer pChannel) {
        logger.info("TiVo'{}' Adding new ignored channel '{}'", getThing().getUID(), pChannel);

        // if we already see this channel as being ignored there is no reason to ignore it again.
        if (getChkIgnoredChannel(pChannel)) {
            return;
        }

        tivoCfgData.addCfgIgnoreChannels(pChannel);

        // Reparse the sorted set and publish to UI
        SortedSet<Integer> myConfig = tivoCfgData.getCfgIgnoreChannels();
        Integer[] uiArr = myConfig.toArray(new Integer[myConfig.size()]);
        String uiResult = getParsedChannels(uiArr);

        Configuration conf = editConfiguration();
        conf.put(CONFIG_IGNORE_CHANNELS, uiResult);
        updateConfiguration(conf);

    }

    /**
     * Gets the next channel.
     *
     * @param pChannel the channel
     * @return the next channel
     */
    private int getNextChannel(int pChannel) {

        if (getChkIgnoredChannel(pChannel)) {
            if (tivoStatusData != null && tivoStatusData.isCmdOk()) {
                // retry logic is allowed otherwise we only do the logic below once.

                if (tivoStatusData.getChannelNum() > pChannel) {
                    // we appear to be changing the channel DOWNWARD so we try to go down -1
                    if (pChannel < tivoCfgData.getCfgMinChannel()) {
                        pChannel = tivoCfgData.getCfgMinChannel();
                    } else {
                        pChannel--;
                    }
                } else if (tivoStatusData.getChannelNum() <= pChannel) {
                    // we appear to be changing the channel UPWARD so we try to go up +1
                    if (pChannel > tivoCfgData.getCfgMaxChannel()) {
                        pChannel = tivoCfgData.getCfgMaxChannel();
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
        logger.info("TiVo'{}' next proposed channel - '{}'", getThing().getUID(), pChannel);
        return pChannel;

    }

    /**
     * {@link getChkIgnoredChannel} checks if the passed TV channel number is contained within the list of stored
     * channels contained within {@link getCfgIgnoreChannels}.
     *
     * @param pChannel the TV channel number
     * @return true= channel is contained within the list, false= channel number is not contained within the list
     */
    private boolean getChkIgnoredChannel(int pChannel) {

        if (tivoCfgData.getCfgIgnoreChannels() != null && tivoCfgData.getCfgIgnoreChannels().contains(pChannel)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@link setTivoOnline} changed the thing status to online. Typically called from the child status polling job when
     * connections can be made to the TiVo and status codes are returned.
     */
    public void setTivoOnline() {

        updateStatus(ThingStatus.ONLINE);

    }

    /**
     * {@link setTivoOffline} changed the thing status to offline. Typically called from the child status polling job
     * when
     * connections can not be made to the TiVo, includes the reason why the offline status has been set.
     *
     * @param thingStatusDetail the thingStatusDetail
     * @param strMsg the error message / reason why the device is offline (displayed in the GUI)
     */
    public void setTivoOffline(ThingStatusDetail thingStatusDetail, String strMsg) {

        updateStatus(ThingStatus.OFFLINE, thingStatusDetail, strMsg);

    }

    @Override
    public void thingUpdated(Thing thing) {
        logger.debug("TiVo handler for thing '{}' - thingUpdated", getThing().getUID());
        super.thingUpdated(thing);
    }

    @Override
    public void thingDiscovered(DiscoveryService source, DiscoveryResult result) {
        logger.debug("thingDiscovered thing '{}'", getThing().getUID());
        if (result.getThingUID().equals(this.getThing().getUID())) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void thingRemoved(DiscoveryService source, ThingUID thingUID) {
        logger.debug("thingRemoved thing '{}'", getThing().getUID());
        if (thingUID.equals(this.getThing().getUID())) {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public Collection<ThingUID> removeOlderResults(DiscoveryService source, long timestamp,
            Collection<ThingTypeUID> thingTypeUIDs) {
        logger.debug("removeOlderResults thing '{}' (ignored)", getThing().getUID());
        return null;
    }
}
