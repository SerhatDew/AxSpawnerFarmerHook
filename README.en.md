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

[Download](#download) · [Requirements](#requirements) · [Installation](#installation) · [Configuration guide](#configuration-guide) · [First test](#first-test) · [Build](#build) · [Collection rules](#collection-rules) · [Troubleshooting](#troubleshooting)

## Highlights

- Sends newly generated AxSpawners loot to the uxmFarmer assigned to the spawner's region.
- Captures the queued amount and performs the main-thread handoff; anything the farmer cannot take goes to AxSpawners storage.
- Acts as a bridge over the existing AxSpawners and uxmFarmer setup without maintaining a second product configuration.

## Download

Download the ready-to-use `AxSpawnerFarmerHook.jar` from the [latest GitHub Release](https://github.com/SerhatDew/axspawners-uxmfarmer/releases/latest). Unless building from source, use the JAR in the release assets rather than the **Source code** archive.

## Requirements

- Java 21 or newer.
- Paper; the build targets the 1.21.4 API. Startup and dependency-failure behavior were observed on Leaf 26.2 / Java 25; actual loot transfer could not be verified because uxmFarmer had no configured license. This is not a full compatibility guarantee for Paper 26.2 or every fork. Folia is not supported.
- AxSpawners 1.12.1 and uxmFarmer 3.18.1-SNAPSHOT enabled with a valid license. Both are required `depend` plugins; having their JARs on disk is not sufficient.
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

If the old file contains only `collect-spawner-drops: true|false`, the first startup migrates its value to `collection.enabled`. If both keys exist, the new `collection.enabled` key takes precedence; migration removes the old key. Migration reads the user file directly rather than overwriting it with packaged defaults. Set `collection.enabled: false` to disable collection and restart the server.

All three standalone hooks use the same lifecycle convention: a gold/yellow robot banner shows the actual plugin name, Maven version and platform, followed by configuration, dependency and integration steps. Timed success is reported only when enabled components are ready. `collection.enabled: false` is reported as inactive rather than successful startup; startup failures retain their exceptions. Shutdown rejects new transfers, stops only the hook's own listener/task and drains queued loot. Incomplete startup and cleanup failures are never reported as successful shutdown. The small logging helper is embedded as source in each JAR; no shared plugin or Maven module is required. PlugMan unload/reload is unsupported; use a full server restart.

## Configuration guide

### Which file owns each setting?

| File / section | Responsibility |
| --- | --- |
| `plugins/AxSpawnerFarmerHook/config.yml` | Only the bridge collection switch and schema version. [Packaged defaults](src/main/resources/config.yml). |
| `plugins/uxmFarmer/config.yml` | Farmer license, region/economy/NPC integrations and allowed worlds. The hook does not manage this file. |
| `plugins/uxmFarmer/collected-materials.yml` | Collectable materials, prices, levels and capacities. |
| `plugins/uxmFarmer/general-settings.yml` | Farmer-wide collection rules, including `collect-custom-items`. |
| AxSpawners' own configuration | Spawner loot generation and Ax storage capacity. This hook does not generate loot. |

### Complete bridge settings

| Key | Type / default | Effect |
| --- | --- | --- |
| `config-version` | Integer / `1` | Hook configuration schema, not the plugin version. Do not increment manually; this release accepts only `1`. Migration adds it if absent. |
| `collection.enabled` | YAML boolean / `true` | `true`: process new spawner loot under Farmer rules. `false`: create no listener/task and report inactive; existing stock is neither moved nor deleted. |

Use unquoted `true` and `false`. The string `"false"`, numbers and empty sections are not valid substitutes; incorrect types prevent startup. Prefer explicit `true/false` over version-dependent YAML spellings such as `yes/no`. `collection` must be a YAML section. The hook has no commands, reload command, player permission nodes, database configuration or Redis configuration of its own. Restart the server after changing these settings.

### Get uxmFarmer working first

1. Set `license-key` in uxmFarmer's generated `config.yml` to your own valid license. Never publish it in an issue or example config. Resolve `License key is not set! Plugin will not start.` before testing the bridge; the hook cannot bypass that prerequisite.
2. Set `hooks.region-hook` to an integration actually installed on the server. The default is `uxmClaims`; do not leave that value when using a different island/claim plugin. The spawner and Farmer must resolve to the same region, not merely be close together.
3. Match `hooks.money-hook` and `hooks.interaction-hook` to installed integrations. For example, selecting `Vault` also requires a working economy provider. Default `FancyNpcs` is not a requirement imposed by this hook; Farmer's supported `interaction-hook: default` is the command-access option when no NPC interaction is wanted.
4. Check the world filter. Farmer 3.18.1 ships with `list-type: BLACKLIST` and `world-list: [world]`, which **blocks** the world named `world`. To explicitly allow a test world, merge this **Farmer configuration** section into the existing file:

```yaml
world-management:
  list-type: WHITELIST
  world-list:
    - world
```

Do not put this fragment into the hook config. Replace the world name as needed; keep the rest of your Farmer settings.

### Products, Farmer switches and capacity

Enable the Farmer and its natural-drop collection switch in the Farmer menu. Enable collection for the individual product too. The hook looks up the live Farmer product definition by Bukkit `Material`; it does not maintain a second product allowlist.

For example, to collect `BLAZE_ROD`, merge the following product into the existing single `collected-materials` section of `plugins/uxmFarmer/collected-materials.yml`. Example prices/capacities are not economy recommendations; adapt them to your level policy:

```yaml
collected-materials:
  BLAZE_ROD:
    sellable: true
    unit-price: 1.0
    levels:
      0:
        price: 0
        capacity: 2500
```

Do not duplicate the top-level `collected-materials` key or replace other products. Use Farmer's supported reload flow or a full restart. The next transfer reads current product definitions; changing a definition does not automatically migrate existing Farmer stock.

Custom names/lore/enchantments can be rejected by `collect-custom-items: false` in `general-settings.yml`. A product's configured `model-data` must match; model-data items are also rejected when the product has no model-data definition. Avoid weakening collection rules server-wide just to permit one product.

If Farmer capacity is full, the remainder is routed to AxSpawners storage. Ax storage is finite too; the hook promises neither unlimited storage nor ground-drop overflow when both are full.

## First test

1. Restart the server. Confirm AxSpawners and uxmFarmer are enabled before the hook reports successful startup. The banner alone does not establish readiness.
2. Create a Farmer through a working region integration in an allowed world. Place a spawner in the same region, enable product and natural-drop collection, and leave room in both stores.
3. Record the product amounts in Farmer and Ax storage before new loot is generated. Existing Ax stock is not imported.
4. Wait for new loot and compare the Farmer increase with the remaining Ax increase. Account for any other sales/production/bonus hooks affecting quantities. No player-inventory reward is expected.
5. Fill Farmer storage in a controlled test and check that new remainder goes to Ax storage; leave Ax room before testing this boundary.
6. Restart with `collection.enabled: false` and verify inactive status and no bridge transfer into Farmer. Restore `true` and restart to resume.

Install this hook only on Paper backends, never Velocity. With multiple backends, validate dependencies and region ownership on each. Farmer's own multi-server/Redis setup is separate and is not configured by this hook.


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
| `License key is not set` / Farmer disables itself | Configure a valid license in `uxmFarmer/config.yml`, then fully restart. Auto-smelter waiting and Ax bridge startup failure follow from this missing dependency. |
| Nothing works in `world` | Check whether Farmer's default `BLACKLIST` contains `world`, and verify its region integration. |
| The plugin is listed but no transfer occurs | Check its ready message; a config-disabled hook can be inactive. Neither an installed JAR nor a banner establishes readiness. |
| Automatically generated loot falls on the ground | The inspected AxSpawners 1.12.1 generation/storage path does not spawn ground items. Inspect the running server's AxSpawners jar, other loot hooks and spawner settings; updating uxmFarmer alone does not establish a fix. |

When reporting a problem, include the server, Java, AxSpawners and uxmFarmer versions along with the relevant console error. Do not share tokens, passwords or player data.

1.0.1 review: fixed queued loot quantities changing with the live spawner count, and early slot refreshes discarding whole units after a partial farmer deposit. Starting with empty storage and a one-slot limit, the 100 generated − 13 collected = 87 remainder scenario retained 64 with the old commit's code and 87 with the fixed code.

1.1.0 verification: 24 tests passed. A standalone smoke program exercised disabled collection without listener/task creation, preserved legacy `false`, and safe shutdown after invalid configuration without false success output. An isolated run using the real `StorageService.addProduct` produced 13 Farmer + 27 Ax units from 40 generated units for each of four products; removing a live product definition routed all 40 to Ax storage. Closing the asynchronous queue delivered 18 units exactly once and left the next 13 units with the producer. World, region and persistent stock objects were test doubles; live world/region transfers and the reported ground-drop issue were not verified in this run.

## License

The hook source is licensed under **GPL-3.0-only**. See [LICENSE](LICENSE).

AxSpawners, uxmFarmer and their respective assets remain under their own licenses and are not part of this source distribution.
