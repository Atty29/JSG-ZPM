# JSG-ZPM

JSG-ZPM is a Minecraft 1.20.1 Forge addon project for **Just Stargate Mod (JSG)**.

This repository is completely separate from Bulkhead Engineering, HBM-derived content, and MTR lifts/escalators.

## Current status — Phase 2

The Phase 1 foundation and Phase 2 Zero Point Module gameplay layer are now implemented.

Current features:

- Minecraft 1.20.1 / Forge 47.4.x / Java 17
- mod id: `jsgzpm`
- explicit runtime dependency on Just Stargate Mod (`jsg`)
- long-backed energy storage for capacities above the normal 32-bit Forge Energy range
- server-configurable standard ZPM capacity, defaulting to `100,000,000,000 FE` (100 GFE)
- Zero Point Module item with percentage and human-readable stored-energy tooltip
- loose ZPMs expose their stored-energy state but cannot be charged/discharged through arbitrary generic FE item chargers; charging/discharging is reserved for JSG-ZPM infrastructure
- Crystal Binder
- Zero-Point Containment Matrix
- Central Power Regulator
- staged component recipes using JSG and vanilla materials
- custom final ZPM assembly recipe using two Basic, two Advanced and two Ultimate JSG energy crystals
- energy stored in those six JSG crystals is inherited by the crafted ZPM, capped at the ZPM's configured capacity
- compatibility tags support the current `jsg` / `jsg_core` registry split without bundling JSG code or assets
- dedicated-server-safe common/client separation
- automated GitHub Actions build validation

The current Phase 2 inventory models are deliberately temporary vanilla-backed placeholders. Original ZPM and component models/textures will replace them during the visual asset phase; no JSG models or textures are copied into this repository.

Not yet implemented: ZPM hubs/arrays/columns, the Zero Point Energy Generator, Atlantis Pegasus DHD, alarms, or final custom models/textures.

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
