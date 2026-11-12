# Blue Riiot Binding

This binding reads the water measurements of [Blue Riiot](https://www.blueriiot.com) **Blue Connect** pool analysers (Blue Connect Go, Plus, Gold and others) from the Blue Riiot cloud: water temperature, pH, ORP (redox), conductivity or salinity, the values of manually entered test strips and the battery state.
With a **Blue Extender** the binding can also ask the extender to hand over its newest stored measurement, like the "Wi-Fi" option of the app.

> Blue Riiot has no public API.
> The binding uses the same interface as the Blue Connect app, which may change at any time.
> It is not affiliated with or supported by Blue Riiot.

## Supported Things

| Thing type    | Description                                                          |
|---------------|----------------------------------------------------------------------|
| `account`     | Bridge for a Blue Connect account (same login as in the app)         |
| `blueconnect` | One Blue Connect device of the account                               |

## Discovery

When the `account` bridge is online, all Blue Connect devices of the account appear in the inbox.
The bridge also lists them in its property `devices`.

## Thing Configuration

### `account` Bridge

| Name              | Type    | Description                                                        | Default | Required | Advanced |
|-------------------|---------|--------------------------------------------------------------------|---------|----------|----------|
| `email`           | text    | E-mail address of the Blue Connect account                         | N/A     | yes      | no       |
| `password`        | text    | Password of the Blue Connect account                               | N/A     | yes      | no       |
| `refreshInterval` | integer | Polling interval in minutes (minimum 5)                            | 30      | no       | no       |
| `language`        | text    | Language for texts returned by the cloud                           | en      | no       | yes      |

If the cloud rejects the login, the bridge goes OFFLINE and stops polling to avoid locking the account.
It starts again when the configuration is changed.

### `blueconnect` Thing

| Name            | Type    | Description                                                                                      | Default | Required |
|-----------------|---------|--------------------------------------------------------------------------------------------------|---------|----------|
| `serial`        | text    | Serial number as used by the cloud. In the app it is shown as "key" (e.g. `0A1B2C3D`), not the printed serial number. | N/A | yes |
| `fetchInterval` | integer | Every n hours, ask the Blue Extender to hand over its newest measurement (0 = off)              | 0       | no       |

## Channels

| Channel                | Type                     | Read/Write | Description                                                                      |
|------------------------|--------------------------|------------|----------------------------------------------------------------------------------|
| `temperature`          | Number:Temperature       | R          | Water temperature                                                                |
| `temperature-status`   | String                   | R          | `OK`, `WARNING` or `CRITICAL`, based on the ranges sent by the cloud             |
| `ph`                   | Number                   | R          | pH value                                                                         |
| `ph-status`            | String                   | R          | Status of the pH value                                                           |
| `orp`                  | Number:ElectricPotential | R          | ORP (redox) in mV                                                                |
| `orp-status`           | String                   | R          | Status of the ORP value                                                          |
| `conductivity`         | Number                   | R          | Conductivity in µS/cm (advanced)                                                 |
| `salinity`             | Number                   | R          | Salinity in g/l (advanced)                                                       |
| `salt`                 | Number                   | R          | Salt in ppm (advanced)                                                           |
| `tds`                  | Number                   | R          | Total dissolved solids in ppm (advanced)                                         |
| `free-chlorine`        | Number                   | R          | Free chlorine in mg/l from test strips (advanced)                                |
| `free-chlorine-status` | String                   | R          | Status of free chlorine                                                          |
| `combined-chlorine`    | Number                   | R          | Combined chlorine in mg/l from test strips (advanced)                            |
| `total-chlorine`       | Number                   | R          | Total chlorine in mg/l from test strips (advanced)                               |
| `alkalinity`           | Number                   | R          | Total alkalinity in mg/l from test strips (advanced)                             |
| `alkalinity-status`    | String                   | R          | Status of the alkalinity                                                         |
| `cyanuric-acid`        | Number                   | R          | Cyanuric acid in mg/l from test strips (advanced)                                |
| `calcium-hardness`     | Number                   | R          | Calcium hardness in mg/l from test strips (advanced)                             |
| `last-measurement`     | DateTime                 | R          | Time of the newest measurement known to the cloud (when it was measured)         |
| `battery-low`          | Switch                   | R          | Battery low                                                                      |
| `fetch-measurement`    | Switch                   | W          | `ON` asks the Blue Extender to hand over its newest stored measurement; resets to `OFF` |

If a value exists twice (device and test strip), the binding uses the value that is not expired, then the device value, then the newest one.
Values that the cloud does not know are `UNDEF`.

### About `fetch-measurement`

A Blue Connect Go only measures by itself and sends its values via Bluetooth.
A Blue Extender receives these measurements several times a day but does not always forward them to the cloud.
`fetch-measurement` (and `fetchInterval`) asks the cloud to take over the newest measurement stored by the extender.
It does **not** start a new measurement; that is only possible via Bluetooth close to the device.
About 2 and 6 minutes after the request the binding polls the cloud again, so new values show up quickly.

## Full Example

### `blueriiot.things`

```java
Bridge blueriiot:account:konto "Blue Riiot Account" [ email="me@example.com", password="secret", refreshInterval=30 ] {
    Thing blueconnect pool "Blue Connect" [ serial="0A1B2C3D", fetchInterval=12 ]
}
```

### `blueriiot.items`

```java
Group                    gPool              "Pool"
Number:Temperature       Pool_Temperature   "Water Temperature [%.1f °C]" <temperature> (gPool) ["Measurement", "Temperature"] { channel="blueriiot:blueconnect:konto:pool:temperature" }
Number                   Pool_pH            "pH [%.2f]"                   <water>       (gPool) ["Measurement", "Water"]       { channel="blueriiot:blueconnect:konto:pool:ph" }
Number:ElectricPotential Pool_ORP           "ORP [%.0f mV]"               <energy>      (gPool) ["Measurement", "Voltage"]     { channel="blueriiot:blueconnect:konto:pool:orp", unit="mV" }
String                   Pool_pH_Status     "pH Status [%s]"                            (gPool)                                { channel="blueriiot:blueconnect:konto:pool:ph-status" }
DateTime                 Pool_LastMeasured  "Last Measurement [%1$tF %1$tR]" <time>     (gPool) ["Status", "Timestamp"]        { channel="blueriiot:blueconnect:konto:pool:last-measurement" }
Switch                   Pool_BatteryLow    "Battery Low"                 <lowbattery>  (gPool) ["Alarm", "LowBattery"]        { channel="blueriiot:blueconnect:konto:pool:battery-low" }
Switch                   Pool_Fetch         "Fetch Newest Measurement"                  (gPool)                                { channel="blueriiot:blueconnect:konto:pool:fetch-measurement" }
```

Set `unit="mV"` for the ORP item, otherwise openHAB stores the value in V.

## Troubleshooting

```text
log:set DEBUG org.openhab.binding.blueriiot
log:set TRACE org.openhab.binding.blueriiot.internal.api.BlueRiiotApi
```

TRACE logs the complete responses of the cloud.
Please remove serial numbers and pool IDs before posting them.
