# openHAB Blue Riiot Binding

Unofficial [openHAB](https://www.openhab.org) binding for [Blue Riiot](https://www.blueriiot.com) **Blue Connect** pool analysers.
Water temperature, pH, ORP (redox), conductivity or salinity, test strip values and battery state from the Blue Riiot cloud, plus fetching the newest measurement from a Blue Extender.

**[Documentation of the binding](bundles/org.openhab.binding.blueriiot/README.md)**

> Not affiliated with or supported by Blue Riiot.
> Blue Riiot has no public API; the binding uses the same interface as the Blue Connect app, which may change at any time.

## Installation

Requires openHAB 5.0 or newer (developed and used with 5.2.4).

1. Download `org.openhab.binding.blueriiot-<version>.jar` from the [latest release](https://github.com/simon-scherer/openhab-binding-blueriiot/releases/latest).
2. Copy it into the `addons` folder of your openHAB installation (e.g. `/usr/share/openhab/addons`).
3. Create the account bridge, either in the UI (Settings → Things → + → Blue Riiot Binding) or with a `.things` file, see [examples/conf/things](examples/conf/things/blueriiot.things).
4. When the bridge is online, the Blue Connect devices appear in the inbox; the bridge property `devices` lists their serial numbers.
5. Optional: items, a map transformation and a Main UI page are in [examples/conf](examples/conf) (texts in German).

## Repository Layout

| Path                                    | Content                                                                 |
|-----------------------------------------|-------------------------------------------------------------------------|
| `bundles/org.openhab.binding.blueriiot` | The binding, structured like a bundle in openhab-addons                 |
| `examples/conf`                         | Things, items, map transformation and a Main UI page (German)           |
| `tools/standalone-build`                | Build without Maven against API stubs (for quick local builds)          |
| `.github/workflows/build.yml`           | Maven build inside an openhab-addons checkout, releases on tags `v*`    |

## Building

The regular build runs on GitHub Actions: it copies the bundle into a checkout of [openhab-addons](https://github.com/openhab/openhab-addons) and builds it with Maven, including the null analysis, static code analysis and formatting checks of openHAB.

Locally the same works with:

```shell
git clone --depth 1 --branch 5.2.4 https://github.com/openhab/openhab-addons
cp -r bundles/org.openhab.binding.blueriiot openhab-addons/bundles/
# add <module>org.openhab.binding.blueriiot</module> to openhab-addons/bundles/pom.xml
cd openhab-addons && mvn spotless:apply install -pl :org.openhab.binding.blueriiot
```

Without Maven, `tools/standalone-build/build.sh` compiles the bundle with plain `javac` against API stubs (JDK 21; gson, slf4j-api and the Jetty 9.4 jars `jetty-client`, `jetty-http`, `jetty-io`, `jetty-util` from an openHAB installation in `tools/standalone-build/lib`).

## Privacy

The binding only reads data. The only write action is asking the cloud to take over the newest measurement of a Blue Extender.
Credentials stay in your openHAB configuration.
Please remove serial numbers and pool IDs from logs before posting them in issues.

## License

[Eclipse Public License 2.0](LICENSE), like openHAB.
The API endpoints are known from the projects listed in [NOTICE](bundles/org.openhab.binding.blueriiot/NOTICE); no code was copied.
