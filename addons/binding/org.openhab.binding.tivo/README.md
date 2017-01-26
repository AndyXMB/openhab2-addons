# TiVo Binding

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

Thing file based creation has not been tested with this version.

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
| tivoStatus | String | Custom Command | Send any custom commands that are not documented within the official specification. Both the command and action string must be supplied. **Use at your own risk, support is not provided for undocumented commands!**   |

* Commands to each of the channels (except 'Custom Command') do not need to include the command keyword only the action/parameter.  So to change channel simply post/send the number of the channel **without** SETCH or  FORCECH.
* Custom Command is provided to allow the testing of any commands not documented within the official documentation.  In this instance the COMMAND and any parameters must be sent as a single string.
* Keyboard commands must currently be issued one character at a time to the item (this is how the natively supports these command).  
* Special characters must also be changed to the appropriate command e.g. the comma symbol( ,) must not be sent it should be replaced by 'COMMA'.

## Configuration Parameters Notes
The following notes may help to understand the correct configuration properties for your set-up:

 1. If openHAB is the only device or application that you have that makes use of the Network Remote Control functions of your Tivo, enable the **Keep Connection Open** option.  This will connect and lock the port in-use preventing any other device from connecting it.  If you use some other application, disable this option.
 2. Poll for Channel Changes only needs to be enabled if you also plan to use the TiVo remote control or other application to change channel.  If openHAB is your only method of control, you can disable this option.
 2. Set the correct Minimum and Maximum channel numbers BEFORE you run a full channel scan.  By default these are set at 101 and 999.   Consult your Tivo program guide to find these.
 3. The Tivo will lean channel numbers that are not available as part of your subscription as you navigate / change channel.  However channel changing  operations will be slower if there is a large gap between valid channels.  Any gap must not exceed 100.  If you have a gap larger than this you must add the range of excluded channels manually or enable the **Perform Channel Scan** option.
 4. The **Channels to Ignore** section allows you to exclude any channels that you do not want to see or are not part of your subscription.  Both individual channel numbers and ranges of channels are supported e.g. 109, 106, 801-850, 999.
 5.  Perform Channel Scan as the name suggest will systematically change the channels between the specified Minimum and Maximum, identifying which of these are valid.  At least one tuner must be available (not recording) while this operation completes.  If this process is interrupted e.g. by a configuration change or restart, the system will restart the scan at the beginning.  Any channels that are marked as being ignored will not be tested again.
 6. If your provider adds new channels to your subscription line-up, these will have to be manually removed from the list of **Channels to Ignore**.  You can always remove all the entries and do a full scan again.

## Full Example

####demo.items:

```
/* TIVO */
String      TiVo_Command_Result                             {channel="tivo:sckt:Living_Room:tivoStatus"}
String      TiVo_Fav_Channel        "Favs"                              {channel="tivo:sckt:Living_Room:tivoCommand", autoupdate="false"}
String      TiVo_ChangeScreen       "Screens"                           {channel="tivo:sckt:Living_Room:tivoTeleport", autoupdate="false"}
Number      TiVo_SetPoint           "Up/Down"                           {channel="tivo:sckt:Living_Room:tivoChannelSet"}
Number      TiVo_SetPointName       "Channel Name [MAP(tivo.map):%s]"   {channel="tivo:sckt:Living_Room:tivoChannelSet"}
String      TiVo_IRCmd              "Ir Cmd"                            {channel="tivo:sckt:Living_Room:tivoIRCommand", autoupdate="false"}
String      TiVo_KbdCmd             "Keyboard Cmd"                      {channel="tivo:sckt:Living_Room:tivoKBDCommand", autoupdate="false"}
```
* The item 'TiVo_SetPointName' depends upon a valid tivo.map file to translate channel numbers to channel names.

####TivoDemo.sitemap:

```
sitemap TivoDemo label="Main Menu"
{
    Frame label="Tivo" {
        Setpoint    item=TiVo_SetPoint          label="[CH]"            icon="television"   minValue=100 maxValue=999 step=1
        Text        item=TiVo_Command_Result    label="TiVo Status"     icon="television"
        Text        item=TiVo_SetPointName      label="Channel Name"    icon="television"
        Switch      item=TiVo_Fav_Channel       label="Fav TV"          icon="television"   mappings=["SETCH 101"="BBC1", "SETCH 104"="CH 4","SETCH 110"="SKY 1",  "SETCH 135"="SyFy", "SETCH 429"="Film 4"]
        Switch      item=TiVo_Fav_Channel       label="Fav Radio"       icon="television"   mappings=["SETCH 902"="BBC R2", "SETCH 904"="BBC R4 FM", "SETCH 905"="BBC R5","SETCH 951"="Abs 80s"]
        Switch      item=TiVo_ChangeScreen                              icon="television"   mappings=["TIVO"="Home", "LIVETV"="Tv", "GUIDE"="Guide", "NOWPLAYING"="My Shows" ]
        Switch      item=TiVo_IRCmd             label="Navigation"      icon="television"   mappings=["SELECT"="Select", "EXIT"="Exit" ]
        Switch      item=TiVo_IRCmd             label="Navigation"      icon="television"   mappings=["CHANNELUP"="CH +", "CANNELDOWN"="CH -" ]
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