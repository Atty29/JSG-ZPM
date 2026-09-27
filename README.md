# JSG-ZPM

JSG-ZPM is a Minecraft 1.20.1 Forge addon project for **Just Stargate Mod (JSG)**.

This repository is completely separate from Bulkhead Engineering, HBM-derived content, and MTR lifts/escalators.

## Current status — Phase 4

Phases 1-3 are implemented, and Phase 4 adds coordinated large-bank power management.

Current features:

- Minecraft 1.20.1 / Forge 47.4.x / Java 17
- mod id: `jsgzpm`
- explicit runtime dependency on Just Stargate Mod (`jsg`)
- server-configurable standard ZPM capacity, defaulting to `100,000,000,000 FE` (100 GFE)
- Zero Point Module item with percentage and human-readable stored-energy tooltip
- staged ZPM component recipes and custom final assembly with inherited JSG energy-crystal charge
- Atlantis ZPM Hub, Ancient ZPM Array and Ancient ZPM Column
- three independent ZPM slots per holder with raise/lower animation state and insertion/removal restrictions
- sequential discharge within each holder
- Ancient Power Controller for combining multiple holders into one managed power bank
- configurable controller radius (default 32 blocks) and holder limit (default 64 holders / 192 ZPM slots)
- controller scans only loaded chunks and never acts as a chunk loader
- holders persist their controller claim across save/reload and suppress their own external FE output while networked
- the controller becomes the single Forge Energy output for its claimed bank, preventing duplicate cable extraction paths
- bank-wide sequential discharge keeps as few ZPMs partially depleted as possible
- controller status readout reports linked/online holders, active/installed ZPMs and total available energy/capacity
- dedicated-server-safe common/client separation
- automated GitHub Actions build validation

Phase 3/4 visuals are original first-pass development assets. They will continue to be refined during the visual polish phase; no JSG models or textures are copied into this repository.

Not yet implemented: Zero Point Energy Generator, Atlantis Pegasus DHD, alarms, advanced power-management modes or final visual polish.

## Ancient Power Controller

Place one controller near the ZPM holders that should form a bank. Every 40 ticks it scans loaded chunks inside its configured radius and claims unclaimed JSG-ZPM holders, up to the configured holder limit. Claimed holders no longer expose their own FE output; connect your power network to the controller instead.

Right-click the controller to force an immediate rescan and show its current bank status.

The default 32-block radius and 64-holder limit allow one controller to manage up to 192 ZPM slots without loading chunks that are otherwise inactive.

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
