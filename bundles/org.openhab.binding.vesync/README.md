# VeSync Binding

Its current support is for the Air Purifiers & Humidifer's branded as Levoit which utilise the VeSync app based on the V2 protocol.

## Verified Models

Air Filtering models verified are Core300S, Core400S, Vital 100S.
Air Humidifier models verified are Dual 200S, Classic 300S, 600S, OasisMist Smart Humidifier

## Awaiting User Verification Models

Air Filtering models supported are Core200S, Core600S, 131S models and the Vital 100S, 200S.
Air Humidifier Classic 200S (Same as 300S without the nightlight from initial checks), OasisMist 1000 Smart Humidifier

## Supported Things

This binding supports the follow thing types:

| Thing          | Thing Type | Thing Type UID | Discovery | Description                                                           |
|----------------|------------|----------------|-----------|-----------------------------------------------------------------------|
| Bridge         | Bridge     | bridge         | Manual    | A single connection to the VeSync API                                 |
| Air Purifier   | Thing      | airPurifier    | Automatic | An Air Purifier supporting V2 e.g. Core200S/Core300S or Core400S unit |
| Air Humidifier | Thing      | airHumidifier  | Automatic | An Air Humidifier supporting V2 e.g. Classic300S or 600s              |
| Outlet         | Thing      | outlet         | Automatic | An Outlet supporting V2 eg WHOGPLUG                                   |

This binding was developed from the great work in the listed projects.

The binding has been tested against the following Air Filter unit's: Core400S and Vital 100S, **I'm looking for others to confirm** my queries regarding **the Core200S and Core300S** units.
The **Classic 300S Humidifier** has been tested, and **600S with current warm mode restrictions**.

## Discovery

Once the bridge is configured auto discovery will discover supported devices from the VeSync API.

## Thing Configuration

### Bridge configuration parameters

| Name                             | Type   | Description                                               | Recommended Values |
|----------------------------------|--------|-----------------------------------------------------------|--------------------|
| username                         | String | The username as used in the VeSync mobile application     |                    |
| password                         | String | The password as used in the VeSync mobile application     |                    |
| airPurifierPollInterval          | Number | The poll interval (seconds) for air filters / humidifiers | 60                 |
| outletPollInterval               | Number | The poll interval (seconds) for outlets                   | 60                 |
| backgroundDeviceDiscovery        | Switch | Should the system scan periodically for new devices       | ON                 |
| refreshBackgroundDeviceDiscovery | Number | Frequency (seconds) of scans for new new devices          | 120                |

- Note Air PM Levels don't usually change quickly - 60s seems reasonable if openHAB is controlling it and your don't want near instant feedback of physical interactions with the devices.

### AirPurifier configuration parameters

It is recommended to use the device name, for locating devices. For this to work all the devices should have a unique
name in the VeSync mobile application.

The mac address from the VeSync mobile application may not align to the one the API
uses, therefore it's best left not configured or taken from auto-discovered information.

Device's will be found communicated with via the MAC Id first and if unsuccessful then by the deviceName.

| Name                   | Type                    | Description                                                         |
|------------------------|-------------------------|---------------------------------------------------------------------|
| deviceName             | String                  | The name given to the device under Settings -> Device Name          |
| macId                  | String                  | The mac for the device under Settings -> Device Info -> MAC Address |

## Channels

Channel names in **bold** are read/write, everything else is read-only

### AirPurifier Thing

| Channel              | Type                 | Description                                                | Model's Supported                              | Controllable Values        | Unit  |
|----------------------|----------------------|------------------------------------------------------------|------------------------------------------------|----------------------------|-------|
| **enabled**          | Switch               | Whether the hardware device is enabled (Switched on)       | 131S, 600S, 400S, 300S, Vital 100S, Vital 200S | [ON, OFF]                  |       |
| **childLock**        | Switch               | Whether the child lock (display lock is enabled)           | 600S, 400S, 300S, Vital 100S, Vital 200S       | [ON, OFF]                  |       |
| **display**          | Switch               | Whether the display is enabled (display is shown)          | 131S, 600S, 400S, 300S, Vital 100S, Vital 200S | [ON, OFF]                  |       |
| **fanMode**          | String               | The operation mode of the fan                              | 131S, 600S, 400S, 300S, Vital 100S             | [auto, manual, sleep]      |       |
| **fanMode**          | String               | The operation mode of the fan                              | 200S,                                          | [manual, sleep]            |       |
| **fanMode**          | String               | The operation mode of the fan                              | Vital 200S                                     | [auto, manual, sleep, pet] |       |
| **manualFanSpeed**   | Number:Dimensionless | The speed of the fan when in manual mode                   | 600S, 400S                                     | [1...4]                    |       |
| **manualFanSpeed**   | Number:Dimensionless | The speed of the fan when in manual mode                   | 131S, 300S                                     | [1...3]                    |       |
| **manualFanSpeed**   | Number:Dimensionless | The speed of the fan when in manual mode                   | Vital 100S,Vital 200S                          | [1...5]                    |       |
| **nightLightMode**   | String               | The night lights mode                                      | 200S, 300S                                     | [on, dim, off]             |       |
| filterLifePercentage | Number:Dimensionless | The remaining filter life as a percentage                  | 131S, 600S, 400S, 300S, Vital 100S, Vital 200S |                            |       |
| airQuality           | Number:Dimensionless | The air quality as represented by the Core200S / Core300S  | 131S, 600S, 400S, 300S, Vital 100S, Vital 200S |                            |       |
| airQualityPM25       | Number:Density       | The air quality as represented by the Core400S             | 600S, 400S, 300S, Vital 100S, Vital 200S       |                            | µg/m³ |
| errorCode            | Number:Dimensionless | The error code reported by the device                      | 600S, 400S, 300S, Vital 100S, Vital 200S       |                            |       |
| timerExpiry          | DateTime             | The expected expiry time of the current timer              | 600S, 400S                                     |                            |       |
| schedulesCount       | Number:Dimensionless | The number of schedules which are configured               | 600S, 400S                                     |                            | one   |
| configDisplayForever | Switch               | Config: Whether the display will disable when not active   | 600S, 400S, 300S                               |                            |       |
| configAutoMode       | String               | Config: The mode of operation when auto is active          | 600S, 400S, 300S                               |                            |       |
| configAutoRoomSize   | Number:Area          | Config: The room size set when auto utilises the room size | 600S, 400S, 300S                               |                            |       |

### AirHumidifier Thing

| Channel                    | Type                 | Description                                                   | Model's Supported                                    | Controllable Values   | Unit |
|----------------------------|----------------------|---------------------------------------------------------------|------------------------------------------------------|-----------------------|------|
| **enabled**                | Switch               | Whether the hardware device is enabled (Switched on)          | 200S, Dual200S, 300S, 600S, OasisMist, OasisMist1000 | [ON, OFF]             |      |
| **display**                | Switch               | Whether the display is enabled (display is shown)             | 200S, Dual200S, 300S, 600S, OasisMist, OasisMist1000 | [ON, OFF]             |      |
| waterLacking               | Switch               | Indicator whether the unit is lacking water                   | 200S, Dual200S, 300S, 600S, OasisMist, OasisMist1000 |                       |      |
| humidityHigh               | Switch               | Indicator for high humidity                                   | 200S, Dual200S, 300S, 600S, OasisMist                |                       |      |
| waterTankLifted            | Switch               | Indicator for whether the water tank is removed               | 200S, Dual200S, 300S, 600S, OasisMist, OasisMist1000 |                       |      |
| **stopAtHumiditySetpoint** | Switch               | Whether the unit is set to stop when the set point is reached | 200S, Dual200S, 300S, 600S, OasisMist, OasisMist1000 | [ON, OFF]             |      |
| humidity                   | Number:Dimensionless | Indicator for the currently measured humidity % level         | 200S, Dual200S, 300S, 600S, OasisMist, OasisMist1000 |                       |      |
| **mistLevel**              | Number:Dimensionless | The current mist level set                                    | 300S                                                 | [1...2]               | one  |
| **mistLevel**              | Number:Dimensionless | The current mist level set                                    | 200S, Dual200S, 600S, OasisMist, OasisMist1000       | [1...3]               | one  |
| **humidifierMode**         | String               | The current mode of operation                                 | 200S, Dual200S, OasisMist (EU Model)                 | [auto, manual]        |      |
| **humidifierMode**         | String               | The current mode of operation                                 | 300S, 600S, OasisMist1000, OasisMist (Non EU Models) | [auto, manual, sleep] |      |
| **nightLightMode**         | String               | The night light mode                                          | 200S, Dual200S, 300S, OasisMist (EU Model)           | [on, dim, off]        |      |
| **humiditySetpoint**       | Number:Dimensionless | Humidity % set point to reach                                 | 200S, Dual200S, 300S, 600S, OasisMist, OasisMist1000 | [30...80]             |      |
| warmEnabled                | Switch               | Indicator for warm mist mode                                  | 600S, OasisMist                                      |                       |      |
| **warmLevel**              | Number:Dimensionless | The current warm mist level set                               | 600S, OasisMist                                      | [0..3]                | one  |
| errorCode                  | Number:Dimensionless | The error code reported by the device                         | OasisMist1000                                        |                       | one  |
| timerExpiry                | DateTime             | The expected expiry time of the current timer                 | OasisMist1000                                        |                       |      |
| schedulesCount             | Number:Dimensionless | The number schedules configured                               | OasisMist1000                                        |                       | one  |

### Outlet Thing

| Channel         | Type                   | Description                                          | Model's Supported | Controllable Values |
|-----------------|------------------------|------------------------------------------------------|-------------------|---------------------|
| **enabled**     | Switch                 | Whether the hardware device is enabled (Switched on) | WHOGPLUG          | [ON, OFF]           |
| current         | Number:ElectricCurrent | Actual current in A                                  | WHOGPLUG          |                     |
| energy          | Number:Energy          | Today's energy in kWh                                | WHOGPLUG          |                     |
| power           | Number:Power           | Current power in W                                   | WHOGPLUG          |                     |
| voltage         | ElectricPotential      | Current Voltage                                      | WHOGPLUG          |                     |
| highestVoltage  | ElectricPotential      | Highest Voltage ever measured by the outlet          | WHOGPLUG          |                     |
| energyWeek      | Number:Energy          | Total energy of week in kWh                          | WHOGPLUG          |                     |
| energyMonth     | Number:Energy          | Total energy of month in kWh                         | WHOGPLUG          |                     |
| energyYear      | Number:Energy          | Total energy of year in kWh                          | WHOGPLUG          |                     |

## Full Example

### Configuration (*.things)

#### Air Purifiers Core 200S/300S/400S Models & Air Humidifier Classic300S/600S Models

```java
Bridge vesync:bridge:vesyncServers [username="<USERNAME>", password="<PASSWORD>", airPurifierPollInterval=60] {
    airPurifier loungeAirFilter [deviceName="<DEVICE NAME FROM APP>"]
    airPurifier bedroomAirFilter [deviceName="<DEVICE NAME FROM APP>"]
    airHumidifier loungeHumidifier [deviceName="<DEVICE NAME FROM APP>"]
}
```

### Configuration (*.items)

#### Air Purifier Core 400S / 600S Model

```java
Switch               LoungeAPPower                 "Lounge Air Purifier Power"                                  { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:enabled" }
Switch               LoungeAPDisplay               "Lounge Air Purifier Display"                                { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:display" }
Switch               LoungeAPControlsLock          "Lounge Air Purifier Controls Locked"                        { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:childLock" }
Number:Dimensionless LoungeAPFilterRemainingUse    "Lounge Air Purifier Filter Remaining [%.0f %unit%]"         { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:filterLifePercentage" }
String               LoungeAPMode                  "Lounge Air Purifier Mode [%s]"                              { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:fanMode" }
Number:Dimensionless LoungeAPManualFanSpeed        "Lounge Air Purifier Manual Fan Speed"                       { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:manualFanSpeed" }
Number:Density       LoungeAPAirQuality            "Lounge Air Purifier Air Quality [%.0f% %unit%]"             { unit="µg/m³",channel="vesync:airPurifier:vesyncServers:loungeAirFilter:airQualityPM25" }
Number               LoungeAPErrorCode             "Lounge Air Purifier Error Code"                             { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:errorCode" }
String               LoungeAPAutoMode              "Lounge Air Purifier Auto Mode"                              { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:configAutoMode" }
Number               LoungeAPAutoRoomSize          "Lounge Air Purifier Auto Room Size [%.0f% sqft]"            { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:configAutoRoomSize" }
DateTime             LoungeAPTimerExpiry           "Lounge Air Purifier Timer Expiry [%1$tA %1$tI:%1$tM %1$Tp]" { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:timerExpiry" }
Number               LoungeAPSchedulesCount        "Lounge Air Purifier Schedules Count"                        { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:schedulesCount" }
```

#### Air Purifier Core 200S / 300S Model

```java
Switch               LoungeAPPower                 "Lounge Air Purifier Power"                                  { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:enabled" }
Switch               LoungeAPDisplay               "Lounge Air Purifier Display"                                { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:display" }
String               LoungeAPNightLightMode        "Lounge Air Purifier Night Light Mode"                       { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:nightLightMode" }
Switch               LoungeAPControlsLock          "Lounge Air Purifier Controls Locked"                        { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:childLock" }
Number:Dimensionless LoungeAPFilterRemainingUse    "Lounge Air Purifier Filter Remaining [%.0f %unit%]"         { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:filterLifePercentage" }
String               LoungeAPMode                  "Lounge Air Purifier Mode [%s]"                              { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:fanMode" }
Number:Dimensionless LoungeAPManualFanSpeed        "Lounge Air Purifier Manual Fan Speed"                       { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:manualFanSpeed" }
Number:Density       LoungeAPAirQuality            "Lounge Air Purifier Air Quality [%.0f%]"                    { unit="µg/m³",channel="vesync:airPurifier:vesyncServers:loungeAirFilter:airQuality" }
Number               LoungeAPErrorCode             "Lounge Air Purifier Error Code"                             { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:errorCode" }
String               LoungeAPAutoMode              "Lounge Air Purifier Auto Mode"                              { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:configAutoMode" }
Number               LoungeAPAutoRoomSize          "Lounge Air Purifier Auto Room Size [%.0f% sqft]"            { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:configAutoRoomSize" }
DateTime             LoungeAPTimerExpiry           "Lounge Air Purifier Timer Expiry [%1$tA %1$tI:%1$tM %1$Tp]" { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:timerExpiry" }
Number               LoungeAPSchedulesCount        "Lounge Air Purifier Schedules Count"                        { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:schedulesCount" }
```

#### Air Purifier 131s Models

```java
Switch               LoungeAPPower                 "Lounge Air Purifier Power"                                  { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:enabled" }
Switch               LoungeAPDisplay               "Lounge Air Purifier Display"                                { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:display" }
Number:Dimensionless LoungeAPFilterRemainingUse    "Lounge Air Purifier Filter Remaining [%.0f %unit%]"         { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:filterLifePercentage" }
String               LoungeAPMode                  "Lounge Air Purifier Mode [%s]"                              { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:fanMode" }
Number:Dimensionless LoungeAPManualFanSpeed        "Lounge Air Purifier Manual Fan Speed"                       { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:manualFanSpeed" }
Number:Dimensionless LoungeAPAirQuality            "Lounge Air Purifier Air Quality"                            { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:airQuality" }
```

#### Air Purifier Vital 100s / 200s Models

```java
Switch               LoungeAPPower                 "Lounge Air Purifier Power"                                  { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:enabled" }
Switch               LoungeAPDisplay               "Lounge Air Purifier Display"                                { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:display" }
Switch               LoungeAPControlsLock          "Lounge Air Purifier Controls Locked"                        { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:childLock" }
Number:Dimensionless LoungeAPFilterRemainingUse    "Lounge Air Purifier Filter Remaining [%.0f %unit%]"         { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:filterLifePercentage" }
String               LoungeAPMode                  "Lounge Air Purifier Mode [%s]"                              { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:fanMode" }
Number:Dimensionless LoungeAPManualFanSpeed        "Lounge Air Purifier Manual Fan Speed"                       { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:manualFanSpeed" }
Number:Density       LoungeAPAirQuality            "Lounge Air Purifier Air Quality [%.0f% %unit%]"             { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:airQualityPM25" }
Number               LoungeAPErrorCode             "Lounge Air Purifier Error Code"                             { channel="vesync:airPurifier:vesyncServers:loungeAirFilter:errorCode" }
```

#### Air Humidifier Classic 200S / Dual 200S Model

```java
Switch               LoungeAHPower             "Lounge Air Humidifier Power"                                  { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:enabled" }
Switch               LoungeAHDisplay           "Lounge Air Humidifier Display"                                { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:display" }
String               LoungeAHMode              "Lounge Air Humidifier Mode"                                   { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidifierMode" }
Switch               LoungeAHWaterLacking      "Lounge Air Humidifier Water Lacking"                          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:waterLacking" }
Switch               LoungeAHHighHumidity      "Lounge Air Humidifier High Humidity"                          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidityHigh" }
Switch               LoungeAHWaterTankRemoved  "Lounge Air Humidifier Water Tank Removed"                     { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:waterTankLifted" }
Number:Dimensionless LoungeAHHumidity          "Lounge Air Humidifier Measured Humidity [%.0f %unit%]"        { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidity" }
Switch               LoungeAHTargetStop        "Lounge Air Humidifier Stop at target"                         { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:stopAtTargetLevel" }
Number:Dimensionless LoungeAHTarget            "Lounge Air Humidifier Target Humidity [%.0f %unit%]"          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humiditySetpoint" }
Number:Dimensionless LoungeAHMistLevel         "Lounge Air Humidifier Mist Level"                             { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:mistLevel" }
```

#### Air Humidifier Classic 300S Model

```java
Switch               LoungeAHPower             "Lounge Air Humidifier Power"                                  { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:enabled" }
Switch               LoungeAHDisplay           "Lounge Air Humidifier Display"                                { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:display" }
String               LoungeAHNightLightMode    "Lounge Air Humidifier Night Light Mode"                       { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:nightLightMode" }
String               LoungeAHMode              "Lounge Air Humidifier Mode"                                   { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidifierMode" }
Switch               LoungeAHWaterLacking      "Lounge Air Humidifier Water Lacking"                          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:waterLacking" }
Switch               LoungeAHHighHumidity      "Lounge Air Humidifier High Humidity"                          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidityHigh" }
Switch               LoungeAHWaterTankRemoved  "Lounge Air Humidifier Water Tank Removed"                     { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:waterTankLifted" }
Number:Dimensionless LoungeAHHumidity          "Lounge Air Humidifier Measured Humidity [%.0f %unit%]"        { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidity" }
Switch               LoungeAHTargetStop        "Lounge Air Humidifier Stop at target"                         { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:stopAtTargetLevel" }
Number:Dimensionless LoungeAHTarget            "Lounge Air Humidifier Target Humidity [%.0f %unit%]"          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humiditySetpoint" }
Number:Dimensionless LoungeAHMistLevel         "Lounge Air Humidifier Mist Level"                             { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:mistLevel" }
```

#### Air Humidifier 600S Model

```java
Switch               LoungeAHPower             "Lounge Air Humidifier Power"                                  { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:enabled" }
Switch               LoungeAHDisplay           "Lounge Air Humidifier Display"                                { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:display" }
String               LoungeAHMode              "Lounge Air Humidifier Mode"                                   { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidifierMode" }
Switch               LoungeAHWaterLacking      "Lounge Air Humidifier Water Lacking"                          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:waterLacking" }
Switch               LoungeAHHighHumidity      "Lounge Air Humidifier High Humidity"                          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidityHigh" }
Switch               LoungeAHWaterTankRemoved  "Lounge Air Humidifier Water Tank Removed"                     { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:waterTankLifted" }
Number:Dimensionless LoungeAHHumidity          "Lounge Air Humidifier Measured Humidity [%.0f %unit%]"        { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidity" }
Switch               LoungeAHTargetStop        "Lounge Air Humidifier Stop at target"                         { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:stopAtTargetLevel" }
Number:Dimensionless LoungeAHTarget            "Lounge Air Humidifier Target Humidity [%.0f %unit%]"          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humiditySetpoint" }
Number:Dimensionless LoungeAHMistLevel         "Lounge Air Humidifier Mist Level"                             { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:mistLevel" }
Number:Dimensionless LoungeAHWarmMistLevel     "Lounge Air Humidifier Warm Mist Level"                        { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:warmLevel" }
```

#### Air Humidifier Oasis Mist Smart Model

```java
Switch               LoungeAHPower             "Lounge Air Humidifier Power"                                  { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:enabled" }
Switch               LoungeAHDisplay           "Lounge Air Humidifier Display"                                { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:display" }
String               LoungeAHMode              "Lounge Air Humidifier Mode"                                   { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidifierMode" }
Switch               LoungeAHWaterLacking      "Lounge Air Humidifier Water Lacking"                          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:waterLacking" }
Switch               LoungeAHHighHumidity      "Lounge Air Humidifier High Humidity"                          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidityHigh" }
Switch               LoungeAHWaterTankRemoved  "Lounge Air Humidifier Water Tank Removed"                     { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:waterTankLifted" }
Number:Dimensionless LoungeAHHumidity          "Lounge Air Humidifier Measured Humidity [%.0f %unit%]"        { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidity" }
Switch               LoungeAHTargetStop        "Lounge Air Humidifier Stop at target"                         { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:stopAtTargetLevel" }
Number:Dimensionless LoungeAHTarget            "Lounge Air Humidifier Target Humidity [%.0f %unit%]"          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humiditySetpoint" }
Number:Dimensionless LoungeAHMistLevel         "Lounge Air Humidifier Mist Level"                             { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:mistLevel" }
Number:Dimensionless LoungeAHWarmMistLevel     "Lounge Air Humidifier Warm Mist Level"                        { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:warmLevel" }
```

#### Air Humidifier Oasis Mist 1000 Smart Model

```java
Switch               LoungeAHPower             "Lounge Air Humidifier Power"                                  { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:enabled" }
Switch               LoungeAHDisplay           "Lounge Air Humidifier Display"                                { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:display" }
String               LoungeAHMode              "Lounge Air Humidifier Mode"                                   { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidifierMode" }
Switch               LoungeAHWaterLacking      "Lounge Air Humidifier Water Lacking"                          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:waterLacking" }
Switch               LoungeAHHighHumidity      "Lounge Air Humidifier High Humidity"                          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidityHigh" }
Switch               LoungeAHWaterTankRemoved  "Lounge Air Humidifier Water Tank Removed"                     { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:waterTankLifted" }
Number:Dimensionless LoungeAHHumidity          "Lounge Air Humidifier Measured Humidity [%.0f %unit%]"        { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humidity" }
Switch               LoungeAHTargetStop        "Lounge Air Humidifier Stop at target"                         { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:stopAtTargetLevel" }
Number:Dimensionless LoungeAHTarget            "Lounge Air Humidifier Target Humidity [%.0f %unit%]"          { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:humiditySetpoint" }
Number:Dimensionless LoungeAHMistLevel         "Lounge Air Humidifier Mist Level"                             { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:mistLevel" }
Number:Dimensionless LoungeAHWarmMistLevel     "Lounge Air Humidifier Warm Mist Level"                        { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:warmLevel" }
DateTime             LoungeAHTimerExpiry       "Lounge Air Humidifier Timer Expiry [%1$tA %1$tI:%1$tM %1$Tp]" { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:timerExpiry" }
Number               LoungeAHSchedulesCount    "Lounge Air Humidifier Schedules Count"                        { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:schedulesCount" }
Number               LoungeAHErrorCode         "Lounge Air Humidifier Error Code"                             { channel="vesync:airHumidifier:vesyncServers:loungeHumidifier:errorCode" }
```

### Configuration (*.sitemap)

#### Air Purifier Core 400S / 600S Model

```perl
Frame {
   Switch item=LoungeAPPower label="Power"
   Text   item=LoungeAPFilterRemainingUse label="Filter Remaining"
   Switch item=LoungeAPDisplay label="Display"
   Text   item=LoungeAPAirQuality label="Air Quality [%.0f (PM2.5)]"
   Switch item=LoungeAPControlsLock label="Controls Locked"
   Text   item=LoungeAPTimerExpiry label="Timer Shutdown @" icon="clock"
   Switch item=LoungeAPMode label="Mode" mappings=[auto="Auto", manual="Manual Fan Control", sleep="Sleeping"] icon="settings"
   Text   item=LoungeAPErrorCode label="Error Code [%.0f]"
   Switch item=LoungeAPManualFanSpeed label="Manual Fan Speed [%.0f]" mappings=[1="1", 2="2", 3="3", 4="4"] icon="settings"
}
```

#### Air Purifier Core 200S / 300S Model

```perl
Frame {
   Switch item=LoungeAPPower label="Power"
   Text   item=LoungeAPFilterRemainingUse label="Filter Remaining"
   Switch item=LoungeAPDisplay label="Display"
   Switch item=LoungeAPNightLightMode label="Night Light Mode" mappings=[on="On", dim="Dimmed", off="Off"] icon="settings"
   Text   item=LoungeAPAirQuality label="Air Quality [%.0f]"
   Switch item=LoungeAPControlsLock label="Controls Locked"
   Text   item=LoungeAPTimerExpiry label="Timer Shutdown @" icon="clock"
   Switch item=LoungeAPMode label="Mode" mappings=[manual="Manual Fan Control", sleep="Sleeping"] icon="settings"
   Text   item=LoungeAPErrorCode label="Error Code [%.0f]"
   Switch item=LoungeAPManualFanSpeed label="Manual Fan Speed [%.0f]" mappings=[1="1", 2="2", 3="3"] icon="settings"
}
```

#### Air Purifier 131s Models

```perl
Frame {
   Switch item=LoungeAPPower label="Power"
   Text   item=LoungeAPFilterRemainingUse label="Filter Remaining"
   Switch item=LoungeAPDisplay label="Display"
   Text   item=LoungeAPAirQuality label="Air Quality [%.0f]"                
   Switch item=LoungeAPMode label="Mode" mappings=[auto="Auto",manual="Manual Fan Control", sleep="Sleeping"] icon="settings"
   Switch item=LoungeAPManualFanSpeed label="Manual Fan Speed [%.0f]" mappings=[1="1", 2="2", 3="3"] icon="settings"                               
}
```

#### Air Purifier Vital 100S Models

```perl
Frame {
   Switch item=LoungeAPPower label="Power"
   Text   item=LoungeAPFilterRemainingUse label="Filter Remaining"
   Switch item=LoungeAPDisplay label="Display"
   Text   item=LoungeAPAirQuality label="Air Quality [%.0f (PM2.5)]"                
   Switch item=LoungeAPControlsLock label="Controls Locked"
   Switch item=LoungeAPMode label="Mode" mappings=[auto="Auto", manual="Manual Fan Control", sleep="Sleeping"] icon="settings"
   Switch item=LoungeAPManualFanSpeed label="Manual Fan Speed [%.0f]" mappings=[1="1", 2="2", 3="3", 4="4", 5="5"] icon="settings"                               
}
```

#### Air Purifier Vital 200S Models

```perl
Frame {
   Switch item=LoungeAPPower label="Power"
   Text   item=LoungeAPFilterRemainingUse label="Filter Remaining"
   Switch item=LoungeAPDisplay label="Display"
   Text   item=LoungeAPAirQuality label="Air Quality [%.0f (PM2.5)]"                
   Switch item=LoungeAPControlsLock label="Controls Locked"
   Switch item=LoungeAPMode label="Mode" mappings=[auto="Auto", manual="Manual Fan Control", sleep="Sleeping", pet="Pet"] icon="settings"
   Switch item=LoungeAPManualFanSpeed label="Manual Fan Speed [%.0f]" mappings=[1="1", 2="2", 3="3", 4="4", 5="5"] icon="settings"                               
}
```

#### Air Humidifier Classic 200S / Dual 200S Model

```perl
Frame {
   Switch item=LoungeAHPower
   Switch item=LoungeAHDisplay
   Switch item=LoungeAHMode label="Mode" mappings=[auto="Auto", sleep="Sleeping"] icon="settings"
   Text   icon="none" item=LoungeAHWaterLacking
   Text   icon="none" item=LoungeAHHighHumidity
   Text   icon="none" item=LoungeAHWaterTankRemoved
   Text   icon="none" item=LoungeAHHumidity
   Switch item=LoungeAHTargetStop
   Slider item=LoungeAHTarget minValue=30 maxValue=80
   Slider item=LoungeAHMistLevel minValue=1 maxValue=3
}
```

#### Air Humidifier Classic 300S Model

```perl
Frame {
   Switch item=LoungeAHPower
   Switch item=LoungeAHDisplay
   Switch item=LoungeAHNightLightMode label="Night Light Mode" mappings=[on="On", dim="Dimmed", off="Off"] icon="settings"
   Switch item=LoungeAHMode label="Mode" mappings=[auto="Auto", sleep="Sleeping"] icon="settings"
   Text   icon="none" item=LoungeAHWaterLacking
   Text   icon="none" item=LoungeAHHighHumidity
   Text   icon="none" item=LoungeAHWaterTankRemoved
   Text   icon="none" item=LoungeAHHumidity
   Switch item=LoungeAHTargetStop
   Slider item=LoungeAHTarget minValue=30 maxValue=80
   Slider item=LoungeAHMistLevel minValue=0 maxValue=3
}
```

#### Air Humidifier 600S Model

```perl
Frame {
   Switch item=LoungeAHPower
   Switch item=LoungeAHDisplay
   Switch item=LoungeAHMode label="Mode" mappings=[auto="Auto", manual="Manual Control", sleep="Sleeping"] icon="settings"
   Text   icon="none" item=LoungeAHWaterLacking
   Text   icon="none" item=LoungeAHHighHumidity
   Text   icon="none" item=LoungeAHWaterTankRemoved
   Text   icon="none" item=LoungeAHHumidity
   Switch item=LoungeAHTargetStop
   Slider item=LoungeAHTarget minValue=30 maxValue=80
   Slider item=LoungeAHMistLevel minValue=1 maxValue=3
   Slider item=LoungeAHWarmMistLevel minValue=0 maxValue=3
}
```

#### Air Humidifier Oasis Mist Smart Model

```perl
Frame {
   Switch item=LoungeAHPower
   Switch item=LoungeAHDisplay
   Switch item=LoungeAHMode label="Mode" mappings=[auto="Auto", sleep="Sleeping"] icon="settings"
   Text   icon="none" item=LoungeAHWaterLacking
   Text   icon="none" item=LoungeAHHighHumidity
   Text   icon="none" item=LoungeAHWaterTankRemoved
   Text   icon="none" item=LoungeAHHumidity
   Switch item=LoungeAHTargetStop
   Slider item=LoungeAHTarget minValue=30 maxValue=80
   Slider item=LoungeAHMistLevel minValue=1 maxValue=3
   Slider item=LoungeAHWarmMistLevel minValue=1 maxValue=3
}
```

#### Air Humidifier Oasis Mist 1000 Smart Model

```perl
Frame {
   Switch item=LoungeAHPower
   Switch item=LoungeAHDisplay
   Switch item=LoungeAHMode label="Mode" mappings=[auto="Auto", sleep="Sleeping"] icon="settings"
   Text   icon="none" item=LoungeAHWaterLacking
   Text   icon="none" item=LoungeAHHighHumidity
   Text   icon="none" item=LoungeAHWaterTankRemoved
   Text   icon="none" item=LoungeAHHumidity
   Switch item=LoungeAHTargetStop
   Slider item=LoungeAHTarget minValue=30 maxValue=80
   Slider item=LoungeAHMistLevel minValue=1 maxValue=3
   Slider item=LoungeAHWarmMistLevel minValue=1 maxValue=3
   Text   item=LoungeAHTimerExpiry label="Timer Shutdown @" icon="clock"
   Text   item=LoungeAHErrorCode label="Error Code [%.0f]"
}
```

### Credits

The binding code is based on a lot of work done by other developers:

- Contributors of (<https://github.com/webdjoe/pyvesync>) - Python interface for VeSync
- Rene Scherer, Holger Eisold - (<https://www.openhab.org/addons/bindings/surepetcare>) Sure Petcare Binding for openHAB as a reference point for the starting blocks of this code
