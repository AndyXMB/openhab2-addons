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
import org.openhab.binding.tivo.internal.service.TivoStatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TiVoHandler} is the BaseThingHandler responsible for handling commands that are
 * sent to one of the Tivo's channels.
 *
 * @author Jayson Kubilis - Initial contribution
 * @author Andrew Black (AndyXMB) - Updates / compilation corrections. Addition of channel scanning functionality.
 */

public class TiVoHandler extends BaseThingHandler {
    private Logger logger = LoggerFactory.getLogger(TiVoHandler.class);
    private TivoConfigData tivoCfgData = null;
    private TivoStatusProvider myTivoService = null;
    private ScheduledFuture<?> refreshJob;
    private ScheduledFuture<?> chScanJob;

    /**
     * Instantiates a new TiVo handler.
     *
     * @param thing the thing
     * @param discoveryServiceRegistry the discovery service registry
     */
    public TiVoHandler(Thing thing) {
        super(thing);
        logger.debug("TiVoHandler '{}' - creating", getThing().getUID());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        // Handles the commands from the various TiVo channel objects
        logger.debug("handleCommand {}, command: {}", channelUID, command);

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
        }
    }

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
        return value instanceof Boolean ? ((Boolean) value) : Boolean.valueOf((String) value);
    }

    @Override
    public void initialize() {

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
        tivoCfgData = tivoConfig;

        if (myTivoService == null) {
            myTivoService = new TivoStatusProvider(tivoCfgData, this, false);
        }

        if (tivoConfig.getCfgIgnoreChannelScan()) {
            startChannelScan();

        } else if (tivoConfig.isCfgPollChanges()) {
            startPollStatus();
        }

        updateStatus(ThingStatus.OFFLINE);
        myTivoService.statusRefresh();
        logger.debug("Initializing a TiVo handler for thing '{}' - finished!", getThing().getUID());

    }

    @Override
    public void dispose() {

        logger.debug("Disposing of a TiVo handler for thing '{}'", getThing().getUID());

        if (refreshJob != null) {
            refreshJob.cancel(false);
        }
        if (chScanJob != null) {
            logger.warn("'{}' - Channel Scan cancelled by dispose()", getThing().getUID());
            chScanJob.cancel(false);
        }

        while (chScanJob != null && !chScanJob.isDone()) {
            try {
                TimeUnit.MILLISECONDS.sleep(tivoCfgData.getCfgCmdWait());
            } catch (InterruptedException e) {
                logger.debug("Disposing '{}' while waiting for 'channelScanJob' to end error: '{}' ",
                        getThing().getUID(), e.getMessage());
            }
        }

        // Ensure we close any open socket connections
        if (myTivoService != null) {
            myTivoService.connTivoConnectRetry(false);
            myTivoService = null;
        }

    }

    /**
     * {@link startPollStatus} scheduled job to poll for changes in state.
     */
    private void startPollStatus() {

        int firstStartDelay = tivoCfgData.getCfgPollInterval();

        refreshJob = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.debug("refreshJob '{}' @ rate of '{}' seconds", getThing().getUID(),
                            tivoCfgData.getCfgPollInterval());

                    myTivoService.statusRefresh();

                } catch (Exception e) {
                    logger.debug("refreshJob '{}' -  exception occurred: {}", getThing().getUID(), e);
                }
            }
        }, firstStartDelay, tivoCfgData.getCfgPollInterval(), TimeUnit.SECONDS);

        logger.info("refreshJob '{}' will start in '{}' seconds", getThing().getUID(), firstStartDelay);

    }

    /**
     * {@link startChannelScan} starts a channel scan between the minimum and maximum channel numbers. Populates the
     * {@code cfgIgnoreChannels} list which improves the performance of channel changing operations.
     */
    private void startChannelScan() {

        int firstStartDelay = tivoCfgData.getCfgPollInterval();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {

                    int minCh = tivoCfgData.getCfgMinChannel();
                    int maxCh = tivoCfgData.getCfgMaxChannel();

                    updateState(CHANNEL_TIVO_STATUS, new StringType("CHANNEL SCAN IN PROGRESS"));
                    myTivoService.setChScan(true);
                    TivoStatusData tmpStatus = myTivoService.getServiceStatus();

                    // change to first channel number, this forces the channel scan to run from Min# to Max#
                    myTivoService.cmdTivoSend("TELEPORT LIVETV");
                    myTivoService.cmdTivoSend("SETCH " + minCh);

                    for (int i = minCh + 1; i <= maxCh;) {
                        // job has been cancelled, so we need to exit
                        if (chScanJob.isCancelled()) {
                            logger.warn("Channel Scan for '{}' has been cancelled by configuraition parameter change",
                                    getThing().getUID());
                            updateState(CHANNEL_TIVO_STATUS, new StringType("CHANNEL SCAN CANCELLED"));
                            break;
                        }
                        logger.info("Channel Scan for '{}' testing channel num: '{}'", getThing().getUID(), i);

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

                    myTivoService.cmdTivoSend("SETCH " + minCh);
                    Configuration conf = editConfiguration();
                    conf.put(CONFIG_IGNORE_SCAN, false);
                    updateConfiguration(conf);
                    myTivoService.setChScan(false);
                    thingUpdated(getThing());

                } catch (Exception e) {
                    logger.debug("Exception occurred during Channel Scan: {}", e);
                }
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
     * @param tivoCommand the TiVo command object.
     * @param command the command parameter.
     * @return int channel number.
     */
    private TivoStatusData chChannelChange(String tivoCommand, String command) {

        int chnl = tivoCfgData.getCfgMinChannel();
        TivoStatusData tmpStatus = myTivoService.getServiceStatus();

        try {
            // compare this to the current channel and determine the "direction" (up or down)
            int numTries = 10;
            chnl = Integer.valueOf(command.toString()).intValue();

            // check for ignored channels execute, check and learn new ignored channels
            while (numTries > 0 && chnl > 0) {
                numTries--;

                while (chCheckIgnored(chnl) && chGetNext(chnl, tmpStatus) > 0) {
                    logger.info("chChannelChange '{}' skipping channel: '{}'", getThing().getUID(), chnl);
                    chnl = chGetNext(chnl, tmpStatus);
                }

                String tmpCommand = tivoCommand + " " + chnl;

                logger.debug("chChannelChange '{}' sending command to tivo: '{}'", getThing().getUID(), tmpCommand);

                // Attempt to execute the command on the tivo
                tmpStatus = myTivoService.cmdTivoSend(tmpCommand);

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
    public void updateTivoStatus(TivoStatusData tivoStatusData) {

        // This will update the TiVO status and channel numbers when a channel change command has been issued.
        if (tivoStatusData != null) {
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
                    myTivoService.setServiceStatus(tivoStatusData);
                }
            }
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

        tivoCfgData.addCfgIgnoreChannels(pChannel);

        // Re-parse the sorted set and publish to UI
        SortedSet<Integer> myConfig = tivoCfgData.getCfgIgnoreChannels();
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

        if (chCheckIgnored(pChannel)) {
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

        if (tivoCfgData.getCfgIgnoreChannels() != null && tivoCfgData.getCfgIgnoreChannels().contains(pChannel)) {
            return true;
        } else {
            return false;
        }
    }

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

}
