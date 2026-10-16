# AxSpawnerFarmerHook

[**Türkçe**](README.md) · **English**

![Java 21+](https://img.shields.io/badge/Java-21%2B-F4BD60?style=flat-square)
![Paper API 1.21.4](https://img.shields.io/badge/Paper_API-1.21.4-55C7F4?style=flat-square)
[![GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-4ADB9B?style=flat-square)](LICENSE)

A separate Paper plugin that sends newly generated AxSpawners loot to the uxmFarmer in the spawner's region. Neither dependency needs to be modified.

![New spawner loot is transferred to the farmer, with the remainder routed to AxSpawners storage](assets/collection-flow.gif)

*Illustration of the collection flow, not an in-game recording.*

[Requirements](#requirements) · [Installation](#installation) · [Build](#build) · [Collection rules](#collection-rules) · [Troubleshooting](#troubleshooting)

## Requirements

- Java 21 or newer.
- Paper; the build targets the 1.21.4 API. Paper 26.2 has not been verified. Folia is not supported.
- AxSpawners 1.12.1 and uxmFarmer 3.18.1-SNAPSHOT.
- Maven 3 to build from source.

Java 21 bytecode is compatible with Java 25, but that does not establish compatibility with a particular server version.

## Build

Obtain AxSpawners and uxmFarmer separately under their respective licenses. Their jars and sources are not distributed in this repository.

Place the jars in `lib/AxSpawners-1.12.1.jar` and `uxmFarmer-3.18.1-SNAPSHOT.jar`, then install them into your local Maven repository:

```sh
mvn install:install-file -Dfile=lib/AxSpawners-1.12.1.jar -DgroupId=com.artillexstudios -DartifactId=AxSpawners -Dversion=1.12.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=uxmFarmer-3.18.1-SNAPSHOT.jar -DgroupId=com.uxplima -DartifactId=uxmFarmer -Dversion=3.18.1-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
mvn clean verify
```

Output: `target/AxSpawnerFarmerHook.jar`. Dependency jars are not bundled into the output.

## Installation

1. Install and enable AxSpawners and uxmFarmer, including their required dependencies.
2. Put `AxSpawnerFarmerHook.jar` in the server's `plugins` directory.
3. Restart the server.

The hook creates `plugins/AxSpawnerFarmerHook/config.yml`:

```yaml
collect-spawner-drops: true
```

Set it to `false` to disable collection. Restart the server after changing the setting.

## Collection rules

- Only newly generated loot is handled. Existing spawner storage and XP are not imported.
- The farmer is selected by the spawner's location through uxmFarmer's configured region integration.
- The world must be allowed by uxmFarmer. The farmer and its natural-drop collection switch must be enabled.
- Only configured, enabled products are collected. Custom-item and model-data restrictions are respected.
- Products are defined under `collected-materials` in `uxmFarmer/collected-materials.yml`. The same rule applies to `SPIDER_EYE`, `PRISMARINE_SHARD`, `PRISMARINE_CRYSTALS` and `BLAZE_ROD`; there is no separate allowlist. Undefined products are left for AxSpawners storage.
- Collection uses uxmFarmer's existing storage service, including its events, statistics, production and auto-sell behavior.
- Stacked spawner quantities are accounted for. Uncollected loot remains subject to AxSpawners storage limits and overflow behavior.
- Loot that AxSpawners generates asynchronously is handled on the main thread on the next server tick; the AxSpawners spawner loop does not wait for it. Whatever the farmer cannot take is added to AxSpawners storage at the same time.
- Queued loot is delivered when the hook shuts down; loot generated after that goes straight to AxSpawners storage.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| The farmer collects nothing | Check the hook's `collect-spawner-drops` setting, the farmer's natural-drop switch and the farmer assigned to the spawner's region. |
| Only some products are skipped | Check the uxmFarmer product definition, its enabled state and any required model-data match. |
| Loot that does not fit the farmer disappears | Check the AxSpawners storage capacity. A full spawner retains AxSpawners' own overflow behavior. |
| The hook fails to enable | Inspect the first console error. Ensure AxSpawners and uxmFarmer are enabled and the server meets the requirements. |
| Automatically generated loot falls on the ground | The inspected AxSpawners 1.12.1 generation/storage path does not spawn ground items. Inspect the running server's AxSpawners jar, other loot hooks and spawner settings; updating uxmFarmer alone does not establish a fix. |

When reporting a problem, include the server, Java, AxSpawners and uxmFarmer versions along with the relevant console error. Do not share tokens, passwords or player data.

1.0.1 verification: 20 tests passed against the supplied uxmFarmer 3.18.1-SNAPSHOT jar. An isolated smoke run transferred each of the four products through the real `StorageService.addProduct`; removing its definition left it in the input list for AxSpawners. Paper world/region objects were test doubles; the live-server ground-drop issue remains unverified.

## License

The hook source is licensed under **GPL-3.0-only**. See [LICENSE](LICENSE).

AxSpawners, uxmFarmer and their respective assets remain under their own licenses and are not part of this source distribution.
