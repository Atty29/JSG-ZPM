# JSG-ZPM

JSG-ZPM is a Minecraft 1.20.1 Forge addon project for **Just Stargate Mod (JSG)**.

This repository is completely separate from Bulkhead Engineering, HBM-derived content, and MTR lifts/escalators.

## Phase 1 status

Current foundation scope:

- Minecraft 1.20.1 / Forge 47.4.x / Java 17
- mod id: `jsgzpm`
- explicit runtime dependency on Just Stargate Mod (`jsg`)
- DeferredRegister scaffolding for items, blocks, block entities and menu types
- server config scaffolding with default future ZPM capacity of `100,000,000,000 FE`
- long-backed energy storage abstraction with safe Forge Energy compatibility
- dedicated-server-safe common/client separation

Phase 1 intentionally does **not** add the ZPM item, recipes, models, hubs, charger, DHD, or alarms yet.

## Development dependency

JSG-ZPM is an independent addon and does not redistribute JSG code or assets. Development is compiled against the published JSG 1.20.1 artifact from Tau'ri Development. Users will need a compatible JSG 1.20.1 installation at runtime.

JSG source/project: https://github.com/Tau-ri-Dev/Mod-JSG

## Build

Use Java 17 and Gradle 8.14.4 or the Gradle wrapper once generated locally:

```text
gradle build
```

The project follows JSG's current 1.20.1 development baseline: Forge 47.4.10 and Parchment 2023.09.03-1.20.1.

## License

Project licensing is not finalized yet. Just Stargate Mod remains separately owned and licensed by Tau'ri Development; nothing in this repository grants rights to JSG code or assets.
