# JSG-ZPM

JSG-ZPM is a Minecraft 1.20.1 Forge addon project for **Just Stargate Mod (JSG)**.

This repository is completely separate from Bulkhead Engineering, HBM-derived content, and MTR lifts/escalators.

## Current status — Phase 3

The Phase 1 foundation, Phase 2 Zero Point Module gameplay layer, and Phase 3 ZPM holder family are now implemented.

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
- Atlantis ZPM Hub with three independently controlled ZPM positions
- Ancient ZPM Array as a physical three-block horizontal structure
- Ancient ZPM Column as a physical three-block vertical structure
- ZPMs may only be inserted/removed while their slot is raised
- independent 20-tick raise/lower state machines for all three slots
- lowered ZPMs provide Forge Energy through the holder and drain sequentially instead of all modules being flattened together
- actively supplying lowered ZPMs enter a distinct supplying state for visual feedback
- holder inventory/state/animation data persists through NBT and synchronises to clients
- purpose-built first-pass Ancient-style holder textures and a new 3D ZPM item model; no JSG models/textures are copied
- compatibility tags support the current `jsg` / `jsg_core` registry split without bundling JSG code or assets
- dedicated-server-safe common/client separation
- automated GitHub Actions build validation

The Phase 3 visual assets are an original first-pass implementation and can be refined as the art direction develops. The holder mechanics are now separated cleanly from later power-management logic.

Not yet implemented: cross-holder/large-bank Phase 4 power management, the Zero Point Energy Generator, Atlantis Pegasus DHD, or alarm system.

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
