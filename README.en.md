# AxSpawnerFarmerHook

[**Türkçe**](README.md) · **English**

![Java 21+](https://img.shields.io/badge/Java-21%2B-F4BD60?style=flat-square)
![Paper API 1.21.4](https://img.shields.io/badge/Paper_API-1.21.4-55C7F4?style=flat-square)
[![GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-4ADB9B?style=flat-square)](LICENSE)
[![Latest release](https://img.shields.io/github/v/release/SerhatDew/axspawners-uxmfarmer?display_name=tag&style=flat-square&color=F4BD60&label=Latest%20release)](https://github.com/SerhatDew/axspawners-uxmfarmer/releases/latest)

<p align="center"><img src="assets/hero.svg" alt="AxSpawnerFarmerHook product artwork" width="960"></p>

A separate Paper plugin that sends newly generated AxSpawners loot to the uxmFarmer in the spawner's region. Neither dependency needs to be modified.

[**Download the latest JAR**](https://github.com/SerhatDew/axspawners-uxmfarmer/releases/latest/download/AxSpawnerFarmerHook.jar) · [Release notes and SHA-256](https://github.com/SerhatDew/axspawners-uxmfarmer/releases/latest)

![New spawner loot is transferred to the farmer, with the remainder routed to AxSpawners storage](assets/collection-flow.gif)

*Illustration of the collection flow, not an in-game recording.*

[Download](#download) · [Highlights](#highlights) · [Requirements](#requirements) · [Installation](#installation) · [Build](#build) · [Collection rules](#collection-rules) · [Troubleshooting](#troubleshooting)

## Highlights

- Sends newly generated AxSpawners loot to the uxmFarmer assigned to the spawner's region.
- Captures the queued amount and performs the main-thread handoff; anything the farmer cannot take goes to AxSpawners storage.
- Acts as a bridge over the existing AxSpawners and uxmFarmer setup without maintaining a second product configuration.

## Download

Download the ready-to-use `AxSpawnerFarmerHook.jar` from the [latest GitHub Release](https://github.com/SerhatDew/axspawners-uxmfarmer/releases/latest). Unless building from source, use the JAR in the release assets rather than the **Source code** archive.

## Requirements

- Java 21 or newer.
- Paper; the build targets the 1.21.4 API. Paper 26.2 has not been verified. Folia is not supported.
- AxSpawners 1.12.1 and uxmFarmer 3.18.1-SNAPSHOT.
- Maven 3 to build from source.

Java 21 bytecode is compatible with Java 25, but that does not establish compatibility with a particular server version.

## Build

Obtain AxSpawners and uxmFarmer separately under their respective licenses. Their jars and sources are not distributed in this repository.

Place the jars in `AxSpawners-1.12.1.jar` and `uxmFarmer-3.18.1-SNAPSHOT.jar`, then install them into your local Maven repository:

```sh
mvn install:install-file -Dfile=AxSpawners-1.12.1.jar -DgroupId=com.artillexstudios -DartifactId=AxSpawners -Dversion=1.12.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=uxmFarmer-3.18.1-SNAPSHOT.jar -DgroupId=com.uxplima -DartifactId=uxmFarmer -Dversion=3.18.1-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
mvn clean verify
```

Output: `target/AxSpawnerFarmerHook.jar`. Paper, AxSpawners and uxmFarmer dependencies are provided and not bundled into the jar.
## Installation

1. Install and enable AxSpawners and uxmFarmer, including their required dependencies.
2. Put `AxSpawnerFarmerHook.jar` in the server's `plugins` directory.
   Remove the old JAR for this bridge; do not run old and new copies together.
3. Restart the server.

The hook creates `plugins/AxSpawnerFarmerHook/config.yml`:

```yaml
config-version: 1
collection:
  enabled: true
```

If an older `collect-spawner-drops: true|false` setting exists, the first startup migrates it to `collection.enabled` without changing its value. Set `collection.enabled: false` to disable collection and restart.

Startup prints the robot banner with the actual plugin name, Maven version and platform. Configuration/API failures include their exception and never report successful startup. Shutdown rejects new transfers, stops only the hook's own listener/task and drains queued loot. PlugMan unload/reload is unsupported; use a full server restart.

## Collection rules

- Only newly generated loot is handled. Existing spawner storage and XP are not imported.
- The farmer is selected by the spawner's location through uxmFarmer's configured region integration.
- The world must be allowed by uxmFarmer. The farmer and its natural-drop collection switch must be enabled.
- Only configured, enabled products are collected. Custom-item and model-data restrictions are respected.
- Products are defined under `collected-materials` in `uxmFarmer/collected-materials.yml`. The same rule applies to `SPIDER_EYE`, `PRISMARINE_SHARD`, `PRISMARINE_CRYSTALS` and `BLAZE_ROD`; there is no separate allowlist. Undefined products are left for AxSpawners storage.
- Collection uses uxmFarmer's existing storage service, including its events, statistics, production and auto-sell behavior.
- The stack count is captured at the loot event; changing the spawner count while a batch is queued does not change the generated quantity. The exact uncollected quantity is written to AxSpawners storage in one addition, then slots and GUIs are refreshed once.
- Loot that AxSpawners generates asynchronously is handled on the main thread on the next server tick; the AxSpawners spawner loop does not wait for it. Whatever the farmer cannot take is added to AxSpawners storage at the same time.
- Queued loot is delivered when the hook shuts down; loot generated after that goes straight to AxSpawners storage.
- No separate product configuration is maintained. After uxmFarmer reloads its configuration, the next transfer uses its current product definitions.
- AxSpawners 1.12.1 batch capacity rules apply: below the initial slot limit the remainder is added; at the limit only the same product's last partial stack is filled; above the limit the remainder is not stored. The hook never spawns ground items; a full depot is not unlimited storage.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| The farmer collects nothing | Check the hook's `collection.enabled` setting, the farmer's natural-drop switch and the farmer assigned to the spawner's region. |
| Only some products are skipped | Check the uxmFarmer product definition, its enabled state and any required model-data match. |
| Loot that does not fit the farmer disappears | Check the AxSpawners storage capacity. A full spawner retains AxSpawners' own overflow behavior. |
| The hook fails to enable | Inspect the first console error; `collection.enabled` must be a YAML boolean (use `true` or `false`). Ensure AxSpawners and uxmFarmer are enabled and the server meets the requirements. |
| Automatically generated loot falls on the ground | The inspected AxSpawners 1.12.1 generation/storage path does not spawn ground items. Inspect the running server's AxSpawners jar, other loot hooks and spawner settings; updating uxmFarmer alone does not establish a fix. |

When reporting a problem, include the server, Java, AxSpawners and uxmFarmer versions along with the relevant console error. Do not share tokens, passwords or player data.

1.0.1 review: fixed queued loot quantities changing with the live spawner count, and early slot refreshes discarding whole units after a partial farmer deposit. Starting with empty storage and a one-slot limit, the 100 generated − 13 collected = 87 remainder scenario retained 64 with the old commit's code and 87 with the fixed code.

1.1.0 verification: 24 tests passed. A standalone smoke program exercised disabled collection without listener/task creation, preserved legacy `false`, and safe shutdown after invalid configuration without false success output. An isolated run using the real `StorageService.addProduct` produced 13 Farmer + 27 Ax units from 40 generated units for each of four products; removing a live product definition routed all 40 to Ax storage. Closing the asynchronous queue delivered 18 units exactly once and left the next 13 units with the producer. World, region and persistent stock objects were test doubles; live world/region transfers and the reported ground-drop issue were not verified in this run.

## License

The hook source is licensed under **GPL-3.0-only**. See [LICENSE](LICENSE).

AxSpawners, uxmFarmer and their respective assets remain under their own licenses and are not part of this source distribution.
