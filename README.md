# JSG-ZPM

JSG-ZPM is a Minecraft 1.20.1 Forge addon project for **Just Stargate Mod (JSG)**. It is completely separate from Bulkhead Engineering, HBM-derived content, and MTR lifts/escalators.

## Current status — Phase 5

Implemented systems now include:

- 100 GFE long-backed Zero Point Modules with readable charge tooltips
- staged ZPM crafting with charge inherited from JSG energy crystals
- Atlantis ZPM Hub, Ancient ZPM Array and Ancient ZPM Column with independent animated slots
- Ancient Power Controller for coordinated large ZPM banks and sequential discharge
- Zero Point Energy Generator 3×3 multiblock for recharging up to three ZPMs
- generator mounting on floor, ceiling or any wall
- manual Start/Stop charging sequence with shield sealing, cosmic-field state and vent/open sequence
- five JSG Efficiency Upgrade Crystal slots
- default generator efficiencies of 20%, 36%, 52%, 68%, 84% and 100%
- external Forge Energy input with no artificial transfer-rate cap beyond the connected network/API calls
- generator charging distributed across installed non-full ZPMs
- server configuration for ZPM capacity, bank range/size and generator efficiency
- automated GitHub Actions build validation

The generator is built from one **Zero Point Energy Generator Controller** in the centre of a 3×3 plane plus eight **Zero Point Generator Casings**. The plane follows the face the controller is mounted to, so the machine works on floors, ceilings and walls.

### Generator controls (development interaction)

- hold a ZPM and right-click the controller: insert into the next free ZPM slot
- hold a JSG Efficiency Upgrade Crystal and right-click: install an efficiency upgrade
- empty-hand right-click while idle: start charging
- empty-hand right-click while running: stop and vent the chamber
- sneak + empty-hand right-click while idle: remove an installed ZPM, then upgrades if no ZPM remains

Charging only accepts FE after the eight casings are present, the shield has sealed and the cosmic field has formed. Installed items are locked during an active cycle. When all installed ZPMs reach full charge the generator stops automatically, vents the field and opens the shield.

The current block models, shield and cosmic-field effects are first-pass original development visuals and will be refined after in-game visual testing. No JSG models or textures are copied into this repository.

Not yet implemented: Atlantis Pegasus DHD, Atlantis alarm system, advanced bank modes or final visual polish.

## Development dependency

JSG-ZPM is an independent addon and does not redistribute JSG code or assets. Development is compiled against the published JSG 1.20.1 artifact from Tau'ri Development. Users need a compatible JSG 1.20.1 installation at runtime.

JSG source/project: https://github.com/Tau-ri-Dev/Mod-JSG

## Build

Use Java 17 and Gradle 8.14.4:

```text
gradle build
```

The project follows JSG's current 1.20.1 development baseline: Forge 47.4.10 and Parchment 2023.09.03-1.20.1.

## License

Project licensing is not finalized yet. Just Stargate Mod remains separately owned and licensed by Tau'ri Development; nothing in this repository grants rights to JSG code or assets.
