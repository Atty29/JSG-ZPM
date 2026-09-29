# JSG-ZPM

JSG-ZPM is a Minecraft 1.20.1 Forge addon project for **Just Stargate Mod (JSG)**. It is completely separate from Bulkhead Engineering, HBM-derived content, and MTR lifts/escalators.

## Current status — Phase 9 + released-JSG compatibility hotfix

Implemented systems now include:

- 100 GFE long-backed Zero Point Modules with readable charge tooltips
- staged ZPM crafting with charge inherited from JSG energy crystals
- Atlantis ZPM Hub, Ancient ZPM Array and Ancient ZPM Column with independent animated slots
- Ancient Power Controller for coordinated large ZPM banks
- selectable bank modes: Sequential, Balanced, Highest Charge First, Reserve Bank and Emergency Reserve
- persistent bank-mode selection across save/reload
- Zero Point Energy Generator 3×3 multiblock for recharging up to three ZPMs
- generator mounting on floor, ceiling or any wall
- manual Start/Stop charging sequence with shield sealing, cosmic-field state and vent/open sequence
- five JSG Efficiency Upgrade Crystal slots with 20%, 36%, 52%, 68%, 84% and 100% efficiency progression
- external Forge Energy input with no artificial transfer-rate cap beyond the connected network/API calls
- Atlantis Pegasus DHD implemented as a solid five-block C/horseshoe floor console
- release-safe Pegasus Stargate discovery/linking and symbol dialling bridge
- 42-position physical Pegasus-symbol control grid with button-press feedback
- linked Stargate iris OPEN/CLOSE controls or shield ON/OFF controls as appropriate
- automatic Off-World Activation alarm state when the linked Pegasus gate reports an incoming connection
- manual Atlantis General Alarm and alarm-reset controls
- Atlantis Alarm Emitter blocks for distributed base-wide alarms
- Ancient Alarm Linker for explicit DHD-to-emitter binding without repeated area scans
- separate Off-World Activation and General Alarm states; General Alarm takes priority when both are active
- persistent emitter links that survive save/reload
- first-pass original placeholder alarm patterns using vanilla note/chime sounds; no TV audio is redistributed
- server configuration for ZPM capacity, bank range/size and generator efficiency
- automated GitHub Actions build validation with downloadable test JAR artifacts

### Ancient Power Controller / bank modes

The controller automatically claims compatible loaded ZPM holders within its configured range. Holders are sorted nearest-to-farthest from the controller.

- normal right-click: refresh the bank and show holder/ZPM/energy status plus the active mode
- sneak + right-click: cycle to the next bank mode

Modes:

- **Sequential** — original behaviour. Drain the nearest linked holder first, then continue through the bank in order.
- **Balanced** — share each power request across all online linked holders so the bank depletes more evenly.
- **Highest Charge First** — prefer the holder with the highest remaining charge percentage, then move on as its relative charge falls.
- **Reserve Bank** — keep the farthest linked holder unused until the nearer primary bank cannot satisfy the requested power.
- **Emergency Reserve** — never automatically discharge the farthest linked holder. Change the controller mode manually when you intentionally want to access that reserve.

The selected mode is saved in controller NBT and survives save/reload. Individual holders still control their own internal three-slot order and supplying animation.

### Zero Point Energy Generator

The generator is built from one **Zero Point Energy Generator Controller** in the centre of a 3×3 plane plus eight **Zero Point Generator Casings**. The plane follows the face the controller is mounted to, so the machine works on floors, ceilings and walls.

Development interaction:

- hold a ZPM and right-click the controller: insert into the next free ZPM slot
- hold a JSG Efficiency Upgrade Crystal and right-click: install an efficiency upgrade
- empty-hand right-click while idle: start charging
- empty-hand right-click while running: stop and vent the chamber
- sneak + empty-hand right-click while idle: remove an installed ZPM, then upgrades if no ZPM remains

### Atlantis Pegasus DHD

The DHD is placed as a five-block floor-integrated horseshoe with an open step-in position.

- centre console: Pegasus symbol controls for JSG gate dialling
- side protection controls: iris open/close or shield off/on depending on the linked Stargate
- alarm control: toggles the Atlantis General Alarm
- alarm reset: clears the General Alarm; the Off-World Activation alarm remains active while the linked gate is genuinely incoming
- sneak + right-click centre console: force a relink attempt to the nearest compatible Pegasus Stargate

The DHD no longer subclasses JSG's private DHD implementation. Released JSG 5.1.x builds do not expose the 6.0-development `dev.tauri.jsg.common.block.dialhomedevice` classes, so JSG-ZPM now owns the console block/entity and operates the installed Pegasus gate through a release-safe compatibility bridge.

The waist-height console uses installed JSG Pegasus glyphs and reads address pages from either hand. Guidance follows the linked gate's dialed address, then highlights origin and core. Released 5.1 notebook serialization and the newer Core page API are accessed through reflection; no JSG assets are bundled. Runtime testing across individual releases remains necessary.

### Atlantis alarm network

1. Craft an **Ancient Alarm Linker** and one or more **Atlantis Alarm Emitters**.
2. Use the Linker on the Atlantis Pegasus DHD to store that DHD.
3. Use the same Linker on each placed Alarm Emitter to bind it to the stored DHD.
4. The Linker may be reused for any number of emitters.
5. Sneak-use the Linker on an emitter to clear that emitter's link.
6. Sneak-use the Linker in the air to clear the Linker's stored DHD.

Each emitter only checks its explicitly linked DHD, so large bases can use many emitters without every speaker repeatedly scanning the surrounding world. The current alarm tones are development placeholders made from vanilla Minecraft sound events. Original custom alarm audio can replace them during the final polish pass.

The current block models, ZPM visuals, generator shield/cosmic field, DHD console and alarm emitter are first-pass original development visuals and will be refined after in-game visual testing. No JSG models or textures are copied into this repository.

Remaining validation: in-game DHD dialing, page guidance and final model/texture/audio polish across JSG releases.

## Testing development builds

Every successful GitHub Actions **Build** run uploads the generated JAR files as an artifact named `jsg-zpm-<commit SHA>`. Test artifacts are retained for 14 days.

Use the step-by-step validation plan in [`TESTING.md`](TESTING.md) when checking a development build. It covers ZPM energy/crafting, all holder types, all power-controller bank modes, generator orientations/charging, the Atlantis Pegasus DHD, iris/shield controls and the distributed alarm network.

Screenshots are especially useful for the next pass because the current 3D models and effects are intentionally first-pass development visuals.

## JSG runtime dependency

JSG-ZPM is an independent addon and does not redistribute JSG code or assets. Users need a compatible JSG 1.20.1 installation at runtime; Forge enforces this through `mods.toml`.

The addon deliberately does not use JSG as a Java compile dependency. JSG-facing recipes use registry IDs/tags, while DHD/gate interaction goes through the runtime compatibility bridge. This prevents private package changes between released JSG 5.1.x and newer development builds from crashing the entire addon during class loading.

JSG source/project: https://github.com/Tau-ri-Dev/Mod-JSG

## Build

Use Java 17 and Gradle 8.14.4:

```text
gradle build
```

The project uses Forge 47.4.10 and Parchment 2023.09.03-1.20.1.

## License

Project licensing is not finalized yet. Just Stargate Mod remains separately owned and licensed by Tau'ri Development; nothing in this repository grants rights to JSG code or assets.
