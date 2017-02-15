![Tivo Logo](/doc/TiVo_lockup_BLK.png)
## TiVo Binding

This binding integrates [TiVo](https://www.tivo.com/) Digital Video Recorders (DVR) that support the Tivo [TiVo TCP Control Protocol v1.1](https://www.tivo.com/assets/images/abouttivo/resources/downloads/brochures/TiVo_TCP_Network_Remote_Control_Protocol.pdf).

## Supported Things
Most TiVo DVRs that support network remote control can be managed supported by this binding.  Check the web site of your service provider for the precise specification of the TiVo box they have provided.

All TiVo devices must:

 1. be connected to a local area TCP/IP network that can be reached by the openHAB instance (this is not the WAN network interface used by cable service providers to provide the TV signal).    
 2. have the Network Remote Control function enabled to support discovery and control of the device.  This setting can be found using the remote control at:

* Tivo branded boxes - Go to TiVo Central > Messages & Settings > Settings > Remote, CableCARD & Devices > Network Remote Control.  Choose Enabled, press Select.
* Virgin Media branded boxes - using the remote select Home, from the menu select, Help and Settings, Settings, Devices, Network Remote Control.  Select the option Allow network based remote controls.

## Binding Configuration
The binding requires no manual configuration.  Tivo devices with the network remote control interface enabled, will be displayed within the Inbox.  

You can also add these manually, you will need to specify the LAN IP address of your Tivo Device.

## Thing Configuration

Auto-discovery is recommended for discovery and creation of TiVo devices, however they can also be created using the .things file format.  The following minimum parameters should be used:

```
Thing tivo:sckt:test_device [ deviceName = "Test Device", address="192.168.0.19" ]
```

Where:
* **test_device** is the unique thing ID for the device (alpha numeric, no spaces)
* **device name** is the name of the device (if omitted the name of the device will be specified as 'My Tivo') 
* **address** the IP address or host name of the device

See the Parameters section below, for the definition of the parameter fields / values.

## Channels

All devices support the following channels (non exhaustive):

| Channel Type ID | Item Type    | Display Name | Description  |
|-----------------|------------------------|--------------|----------------- |------------- |
| tivoChannelForce | Number (1-9999)| Current Channel - Forced (FORCECH) | Displays the current channel number. When changed, tunes the DVR to the specified channel, **cancelling any recordings in progress if necessary** i.e. when all tuners are already in use / recording. The TiVo must be in Live TV mode for this command to work. |
| tivoChannelSet | Number (1-9999)| Current Channel - Request (SETCH) | Displays the current channel number. When changed, tunes the DVR to the specified channel (unless a recording is in progress on all available tuners). The TiVo must be in Live TV mode for this command to work. |
| tivoTeleport | String | Change Special/Menu Screen (TELEPORT) |Change to one of the following TiVo menu screens: TIVO (Home), LIVE TV, GUIDE, NOW PLAYING (My Shows). |
| tivoIRCommand | String | Remote Control Button (IRCOMMAND) | Send a simulated button push from the remote control to the TiVo. See Appendix A in document TCP Remote Protocol 1.1 for supported codes. |
| tivoKBDCommand | String | Keyboard Command (KEYBOARD) | Sends a code corresponding to a keyboard key press to the TiVo e.g. A-Z. See Appendix A in document TCP Remote Protocol 1.1 for supported characters and special character codes. |
| tivoStatus | String | TiVo Status | Action return code / channel information returned by the TiVo.  |
| tivoCommand | String | Custom Command | Send any custom commands that are not documented within the official specification. Both the command and action string must be supplied. **Note: support is not provided for undocumented commands!**   |

* Commands to each of the channels (except 'Custom Command') do not need to include the command keyword only the action/parameter.  So to change channel simply post/send the number of the channel **without** SETCH or  FORCECH.
* Custom Command is provided to allow the testing of any commands not documented within the official documentation.  In this instance the COMMAND and any parameters must be sent as a single string.
* Keyboard commands must currently be issued one character at a time to the item (this is how the natively supports these command).  
* Special characters must also be changed to the appropriate command e.g. the comma symbol( ,) must not be sent it should be replaced by 'COMMA'.


## Parameters
| Parameter  | Display Name | Description  |
|------------|--------------|-------------|
| deviceName | Device Name | A friendly name to refer to this device. Default: Device name specified on the TiVo or 'My Tivo' if no connection can be made at initial device configuration. |
| address | Address | The IP address or hostname of your TiVo box |
| tcpPort | TCP Port | The TCP port number used to connect to the TiVo. Default: 31339 |
| numRetry | Connection Retries | The number of times to attempt reconnection to the TiVo box, if there is a connection failure. Default: 5 |
| keepConActive | Keep Connection Open | Keep connection to the TiVo open. Recommended for monitoring the TiVo for changes in TV channels. Disable if other applications that use the Remote Control Protocol port will also be used e.g. mobile remote control applications. Default: True (Enabled) |
| pollForChanges | Poll for Channel Changes | Check TiVo for channel changes. Enable if openHAB and a physical remote control (or other services use the Remote Control Protocol) will be used. Default: True (Enabled)|
| pollInterval | Polling Interval (Seconds) | Number of seconds between polling jobs to update status information from the TiVo.  Default: 10" |
| cmdWaitInterval | Command Wait Interval (Milliseconds) |  Period to wait AFTER a command is sent to the TiVo in milliseconds, before checking that the command has completed. Default: 200 |
| ignoreChannels | Channels to Ignore | Used in channel up / down operations to avoid invalid or channels that are not part of your subscription. Skips the channels you list here in a comma separated list e.g. 109, 111, 999. You can exclude a range of channel numbers by using a hyphen between the lower and upper numbers e.g. 109, 101, 800-850, 999. <br><br>New entries do not have to be added in numeric order, these are sorted when saved. During normal channel changing operations, the maximum gap between valid channels is 10. <br><br>Any gap larger than 10 will not be learnt as you change channels under normal operation. If your service has a gap larger than 10 channels you should exclude these manually or **Perform Channel Scan**. |
| minChannel | Min Channel Number | The first valid channel number available on the TiVo. Default: 100 (min 1) |
| maxChannel | Max Channel Number | The last valid channel number available on the TiVo. Default: 999 (max 9999) |
| ignoreChannelScan |  Channels to Ignore |Performs a channel scan between Min Channel Number and Max Channel Number, populates the **Channels to Ignore** settings any channels that are not accessible / part of your subscription.

Note: Existing Channels to Ignore settings are retained, you will need to manually remove any entries for new channels added to your service (or remove all existing Channels to Ignore and run a new scan).|


## Configuration Parameters Notes
The following notes may help to understand the correct configuration properties for your set-up:

###Connection Performance###
 1. If openHAB is the only device or application that you have that makes use of the Network Remote Control functions of your Tivo, enable the **Keep Connection Open** option.  This will connect and lock the port in-use preventing any other device from connecting it.  If you use some other application, disable this option.  Performance is improved if the connection is kept open.
 2. **Poll for Channel Changes** only needs to be enabled if you also plan to use the TiVo remote control or other application to change channel.  If openHAB is your only method of control, you can disable this option.  Turning polling off, minimises the periodic overhead on your hardware.
 
 ###Channel Changing###
 2. Set the correct Minimum and Maximum channel numbers BEFORE you run a full channel scan.  By default these are set at 100 and 999.   Consult your Tivo program guide to find these.
 3. The TiVo will learn channel numbers that are not available as part of your subscription as you navigate / change channel.  Channel changing  operations will be slower if there is a large gap between valid channels.  Any gap must not exceed 10.  If you have a gap larger than this, you must add the range of **Channels to Ignore** manually or use the **Perform Channel Scan** option to pre-populate the ignored channels (recommended).
 4. The **Channels to Ignore** section allows you to exclude any channels that you do not want to see or are not part of your subscription.  Both individual channel numbers and ranges of channels are supported e.g. 109, 106, 801-850, 999.
 5.  **Perform Channel Scan** will systematically change the channels between the specified Minimum and Maximum, identifying which of these are valid.  At least one tuner must be available (not recording) while this operation completes.  If this process is interrupted e.g. by a configuration change or restart, the system will restart the scan at the beginning.  Any channels that are marked as being ignored will not be tested again.  
 6. You can run a channel scan while the system is in Standby mode.
 6. The channel scanning process will take approximately 1 second per channel.  With the default channel range a scan will therefore take between 15 and 20 minutes!  The screen will change to the specified channel while the scan is being run.
 6. If your provider adds new channels to your subscription line-up, these will have to be manually removed from the list of **Channels to Ignore**.  You can always remove all the entries and do a full scan again.

## Full Example

####demo.items:

```
/* TIVO */
String      TiVo_Status                                                                                                         {channel="tivo:sckt:Living_Room:tivoStatus"}
String      TiVo_MenuScreen         "Menu Screens"                                                                              {channel="tivo:sckt:Living_Room:tivoTeleport", autoupdate="false"}
Number      TiVo_SetPoint           "Up/Down"                                                                                   {channel="tivo:sckt:Living_Room:tivoChannelSet"}
String      TiVo_SetPointName       "Channel Name"                                                           
String      TiVo_IRCmd              "Ir Cmd"                                                                                    {channel="tivo:sckt:Living_Room:tivoIRCommand", autoupdate="false"}
String      TiVo_KbdCmd             "Keyboard Cmd"                                                                              {channel="tivo:sckt:Living_Room:tivoKBDCommand", autoupdate="false"}

String      TiVo_KeyboardStr        "Search String"
Switch      TiVo_Search
```
* The item 'TiVo_SetPointName' depends upon a valid tivo.map file to translate channel numbers to channel names.

####TivoDemo.sitemap:

```
sitemap TivoDemo label="Main Menu"
            Frame label="Tivo" {
                Setpoint    item=TiVo_SetPoint          label="[CH %n]"         icon="television"   minValue=100 maxValue=999 step=1
                Text        item=TiVo_SetPointName      label="Channel"         icon="television"
                Text        item=TiVo_Status            label="Status"          icon="television"
                Switch      item=TiVo_IRCmd             label="Media"           icon="television"   mappings=["REVERSE"="⏪", "PAUSE"="⏸", "PLAY"="⏵", "STOP"="⏹", "FORWARD"="⏩" ]
                Switch      item=TiVo_SetPoint          label="Fav TV"          icon="television"   mappings=[101="BBC1", 104="CH 4", 110="SKY 1", 135="SyFy", 429="Film 4"]            
                Switch      item=TiVo_SetPoint          label="Fav Radio"       icon="television"   mappings=[902="BBC R2", 904="BBC R4 FM", 905="BBC R5", 951="Abs 80s"]
                Switch      item=TiVo_MenuScreen        label="Menus"           icon="television"   mappings=["TIVO"="Home", "LIVETV"="Tv", "GUIDE"="Guide", "NOWPLAYING"="My Shows", "INFO"="Info" ]
                Switch      item=TiVo_IRCmd             label="Navigation"      icon="television"   mappings=["UP"="⏶", "DOWN"="⏷", "LEFT"="⏴", "RIGHT"="⏵", "SELECT"="Select", "EXIT"="Exit" ]
                Switch      item=TiVo_IRCmd             label="Likes"           icon="television"   mappings=["THUMBSUP"="Thumbs Up", "THUMBSDOWN"="Thumbs Down"]
                Switch      item=TiVo_IRCmd             label="Actions"         icon="television"   mappings=["ACTION_A"="Red","ACTION_B"="Green","ACTION_C"="Yellow","ACTION_D"="Blue"]
                Switch      item=TiVo_IRCmd             label="Standby"         icon="television"   mappings=["STANDBY"="Standby","TIVO"="Wake Up"]
                
            }
}
```

* Amend the minValue / maxValue to reflect the minimum and maximum channel numbers of your device.
* This example does not use the 'Current Channel - Forced (FORCECH)' channel.  This method will interrupt your recordings in progress when all you tuners are busy, so is obmitted for safety's sake.
* The item 'TiVo_SetPointName' depends upon a valid tivo.map file to translate channel numbers to channel names.

####tivo.map:
```
NULL=Unknown
100=Virgin Media Previews
101=BBC One
102=BBC Two
103=ITV
104=Channel 4
105=Channel 5

etc...

```

####tivo.rules:
The following rule uses the `tivo.map` file to translate the channel number to channel names (populating the `TiVo_SetPointName` item).
```
rule "MapChannel"
when
    Item TiVo_SetPoint changed
then
    var chName = ""
    chName = transform("MAP", "tivo.map", TiVo_SetPoint.state.toString)
    postUpdate(TiVo_SetPointName, chName)

end

```
