## Classic relief acceptance

- Compare against the supplied 1.12 screenshots at eye level and from above: warm brown grain, notched wings, projecting control panels, thick raised emblems, close ribs and visibly recessed side panels.
- Inspect the flat ZPM gem face, dark lattice and stepped crystal underside. Check raised, lowered and moving modules for clearance.
- Check the hub inventory icon and reload resources with F3+T.
- This is original reconstructed artwork; record remaining appearance differences rather than treating a passing build as visual approval.

# JSG-ZPM In-Game Validation Checklist

This checklist is for development builds. Test in a disposable world first.

## Test environment

- Minecraft 1.20.1
- Forge 47.4.x (development baseline: 47.4.10)
- compatible Just Stargate Mod 1.20.1 installation and its required dependencies
- latest JSG-ZPM test JAR from the GitHub Actions `Build` workflow artifact

## 1. Zero Point Module

- Find **Zero Point Module** in the JSG-ZPM creative tab.
- Confirm a fresh ZPM tooltip shows `Charge: 0.0%` and approximately `0 / 100G FE`.
- Confirm the Crystal Binder, Zero-Point Containment Matrix and Central Power Regulator appear in the creative tab.
- Craft a ZPM using two Basic, two Advanced and two Ultimate JSG energy crystals plus the three custom components.
- If any input crystals are charged, confirm the crafted ZPM inherits their combined energy, capped at 100 GFE.

## 2. Atlantis ZPM Hub

- Place an **Atlantis ZPM Hub**.
- Insert three ZPMs individually.
- Confirm each slot can be raised/lowered independently.
- Confirm a lowered ZPM cannot be removed.
- Confirm a raised ZPM can be removed without affecting the other two.
- Save and reload the world and confirm slot contents, energy and positions survive.

## 3. Ancient ZPM Array

- Place an **Ancient ZPM Array** with enough clear horizontal space.
- Confirm the complete physical structure occupies three blocks.
- Repeat the three independent-slot tests from the Hub.
- Break the structure and confirm installed ZPMs are returned rather than deleted or duplicated.

## 4. Ancient ZPM Column

- Place an **Ancient ZPM Column** with enough vertical space.
- Confirm the physical structure occupies three blocks vertically.
- Repeat the insertion, animation, save/reload and removal tests.

## 5. Ancient Power Controller / large banks

- Build at least three Hubs/Arrays/Columns near one **Ancient Power Controller** and place them at visibly different distances from the controller.
- Lower multiple charged ZPMs.
- Attach an FE consumer/storage network to the controller.
- Normal right-click should show current mode, holder count, online holder count, active/installed ZPM counts and total energy.
- Sneak + right-click should cycle through all five modes and return to Sequential after Emergency Reserve.
- Save/reload in a non-default mode and confirm the selected mode persists.

### Sequential

- Select **Sequential**.
- Confirm the nearest linked holder drains first.
- Confirm later holders remain untouched until earlier holders can no longer satisfy demand.
- Confirm an empty ZPM naturally falls out of useful supply and the next available ZPM takes over.

### Balanced

- Select **Balanced** with at least two charged online holders.
- Apply a sustained FE load.
- Confirm multiple holders show supplying activity and their charge levels fall broadly together instead of only the nearest holder draining.
- Confirm a depleted holder drops out while the remaining holders continue supplying.

### Highest Charge First

- Give linked holders clearly different charge percentages.
- Select **Highest Charge First**.
- Confirm the most highly charged holder is preferred.
- Continue drawing power until relative charge levels change and confirm priority follows whichever holder now has the highest percentage.

### Reserve Bank

- Select **Reserve Bank**.
- Remember that the controller sorts holders nearest-to-farthest; the farthest linked holder is the reserve.
- Confirm the farthest holder remains untouched while any nearer primary holder can satisfy the load.
- Exhaust the primary bank and confirm the farthest reserve holder then begins supplying automatically.

### Emergency Reserve

- Select **Emergency Reserve**.
- Confirm the farthest linked holder never supplies automatically, even after the primary bank is exhausted.
- Change to Sequential or Reserve Bank and confirm the held-back energy immediately becomes available again.
- With only one linked holder, confirm Emergency Reserve intentionally exposes no automatic output because that sole holder is the reserve.

## 6. Zero Point Energy Generator

- Place the **Zero Point Energy Generator Controller** and surround it with eight **Zero Point Generator Casings** in a 3Ã—3 plane.
- Test the 3Ã—3 plane on a floor, wall and ceiling.
- Insert one, two and three partially empty ZPMs.
- Start charging and confirm the shield closes before charging begins.
- Confirm the cosmic-field state appears during charging.
- Stop manually and confirm partial ZPM charge is preserved.
- Let a cycle complete and confirm the field vents, shield opens and ZPMs become removable.
- Test 0 through 5 JSG Efficiency Upgrade Crystals and confirm progressively less FE is wasted.

## 7. Atlantis Pegasus DHD

- Leave enough room for the three-block straight footprint and place the **Atlantis Pegasus DHD**.
- Confirm the centre/front remains open so a player can step into the console.
- Place/link a compatible Pegasus Stargate and sneak-right-click the centre console to force a relink if necessary.
- Confirm the console links only to a compatible Pegasus gate.
- Dial manually using the console symbol controls.
- Hold a JSG notebook containing a Pegasus address and confirm the next required symbol is highlighted.

### Protection controls

- With no iris/shield installed, confirm protection controls report that none is installed.
- Install an iris and test OPEN/CLOSE.
- Separately test a gate configured with a shield and confirm the same controls become OFF/ON appropriately.
- Confirm the console never treats one gate as having both an iris and a shield simultaneously.

## 8. Atlantis alarm network

- Place several **Atlantis Alarm Emitters** around the test base.
- Craft/get an **Ancient Alarm Linker**.
- Use the Linker on the Atlantis Pegasus DHD; confirm the DHD position is stored.
- Use the Linker on each Alarm Emitter; confirm each reports successful binding.
- Trigger the DHD General Alarm and confirm all linked emitters enter General Alarm state and sound.
- Cause an incoming Pegasus Stargate connection and confirm linked emitters enter Off-World Activation state automatically.
- If both states are active, confirm General Alarm takes priority.
- Reset the General Alarm while the gate remains incoming; confirm the emitters return to Off-World Activation rather than becoming silent.
- Sneak-use the Linker on one emitter and confirm only that emitter stops following the DHD.
- Save/reload and confirm emitter links persist.

## Visual checks to screenshot/report

These are intentionally first-pass visuals and should be judged in-game:

- ZPM proportions, amber crystal body, black binder and red regulator
- raised versus lowered ZPM height
- active ZPM glow visibility
- Hub proportions
- Array alignment across all three blocks
- Column alignment across all three blocks
- Ancient Power Controller appearance and whether mode/status interaction feels obvious enough
- Generator shield position on all six mounting orientations
- generator cosmic-field scale and clipping
- Atlantis DHD straight desk, console height and tilted keypad
- Pegasus symbol spacing/readability
- notebook guidance highlight visibility
- Alarm Emitter scale and mounting appearance

Screenshots of anything that looks wrong are enough to drive the next visual/model pass.
# Classic Ancient visual pass

See [ANCIENT_VISUALS.md](ANCIENT_VISUALS.md) for provenance, exact dimensions, reusable materials and regeneration commands.

- Replace the previous test JAR (do not keep duplicate addon JARs), restart, then confirm no magenta/black surfaces or missing-model/texture messages after loading the world and F3+T. Check the ZPM in all holder types and the hub body: they share the repaired atlas materials.
- Inspect the ZPM in inventory, both hands, item frame, dropped form, each holder and generator orientation.
- Place hubs facing north/east/south/west. Click each visible bay, insert/remove a ZPM and raise/lower it independently; verify the clicked bay moves and the others retain their state.
- Check empty, partially populated and full hubs; charged/depleted modules; mixed raised/lowered/transitioning slots; active brightness in daylight and darkness.
- Check the table overhang beside neighbouring blocks. Collision and placement intentionally retain the existing one-block footprint.
- Re-run the gameplay and released-JSG compatibility checks below. Resource validation and a successful compile do not establish in-game runtime compatibility.


### Crystal-course / console regression pass

- Inspect all three consoles from above, the front, and both sides: their box faces, glyph deck, recess walls and lights must remain visible, with no cooling ribs crossing the opening.
- Lower all three modules: the shafts must not be visible through the console walls.
- Inspect the ZPM in inventory, in hand and raised: the gem face must be broad and flat; underneath it, the inner crystal course must extend furthest, with the outer courses shorter and their ends irregularly bevelled.
- Exercise each bay independently through the full lift travel; check socket clearance at every position.
- Automated resource validation checks the outer/middle/inner course heights and unchanged installed height. Live Minecraft validation remains required.


### Coloured crystal and hub shell repair

- Confirm green and red crystal blades appear among the amber from multiple angles, with clear facet highlights and no stretched wood-like grain.
- Inspect below each of the three consoles and around its light recess from both sides: no ground, sky or blocks behind the hub should show through its body.
- Confirm no flickering at the backing/relief seams while moving the camera, in daylight and darkness.
- Verify the flat gem face, stepped underside and independent bay travel are unchanged.


### Diagonal alignment and translucent crystal pass

- View both side consoles from directly above and from either side: panel, light strips, recess and notch should share the same 45-degree axes, with symmetric cheek widths and no overlap.
- Check the crystal's tinted semi-transparent surfaces and visible inner facets in Fast, Fancy and Fabulous graphics modes, including inventory, hands, dropped items and installed modules.
- Orbit populated holders and place them near water/glass: look for transparency sorting or disappearing layers. Verify binder and regulator remain opaque.
- Confirm the front console, flat gem face, crystal lengths, installed size, independent lift travel and gameplay remain unchanged.


### Console cheek seam check

- Lower all three modules, then inspect both sides of every console near eye level and from above. Crystal shafts must not show through the narrow seams between the console cheeks and ribbed skirt.
- Orbit each hub facing and repeat with mixed lift positions. The sockets remain open; only unintended side-wall gaps are closed.
- The approved ZPM assets are unchanged from Build #66.


### Array / column visual acceptance

- Place an array and column facing each cardinal direction. Confirm three adjacent array bays and three stacked column bays, correct slot selection and original occupied blocks.
- Array crystals travel perpendicular to their 45-degree upward slope; column crystals travel horizontally into the room corner, perpendicular to their diagonal face. Gem faces point outward.
- Toggle each bay independently through seated, moving and withdrawn states, including mixed positions. Check cup clearance, supports and wall panels for clipping.
- Inspect empty/populated bays, inventory icons, hand/drop display and F3+T; confirm no missing textures/models.
- Recheck insertion/removal, energy output and released-JSG compatibility using the existing checklist. The approved hub and ZPM must retain their appearance.

- Deep sockets: verify each seated module is fully enclosed with a flush end cap, each released tip clears the lip, and the floor and sloped casing never cut through the glass throughout travel. Check all four facings and mixed slot states.

- Inspect the column from both sides: the entire bay is a closed wedge, and its ZPM travels perpendicular to the front slope. Verify seated cap alignment and bore floor clearance on both holder types.

- Layout correction: the ARRAY is the horizontal row of upward-sloped wedges. The COLUMN is a vertical stack of sideways wedges spanning a room corner. Check all four corner rotations; column insertion must remain horizontal and normal to the diagonal face, while array insertion follows its upward slope. Both caps stay flush.

### Single-ZPM pedestal

- Find Ancient ZPM Pedestal in the creative tab or craft with iron, glass, redstone, a central power regulator and polished deepslate.
- Place and rotate in all four directions. Confirm one-block footprint, waist-high rings, a flush cap and a tip only slightly inside the deck.
- Insert a charged ZPM: it seats immediately, supplies energy, emits level 12 light, and illuminates cyan vents/status pads and white glyphs/front strip. Other holders keep their existing lift interactions.
- Try adding a second ZPM: reject it without consuming the item. Shift-right-click with an empty hand to remove the installed module; panels and world light turn off immediately.
- Repeat with an empty-charge ZPM: panels stay off. Extract the last energy unit through FE or the network and confirm immediate darkness. Simulated extraction must not affect charge or lighting.
- Save/reload with charged, depleted and empty sockets; verify state, charge and light recovery. Test on a dedicated server with two clients.
- Break the pedestal in survival, creative and by explosion. The ZPM must drop once, with its stored charge preserved; no neighbouring blocks are removed.
- Connect to an Ancient Power Controller and confirm installed/active counts increase by one only. Recheck hub, array and column placement, animation and extraction.

Recharger visual checks: assemble the controller and eight casings in the existing 3x3 plane. Leave two blocks clear in front. Insert three ZPMs, start charging with the existing interaction, and verify shield closure, gradual mist fill, sweeping lights, and visible central modules. Stop and check gas drains before the shield opens. Repeat wall/floor/ceiling mounting, chunk reload, full modules, empty slots, and casing removal. FE accounting and charging rules are unchanged.

DHD: test every button in all four facings; gaps and side faces must not dial. Hold a Pegasus page in either hand and advance the correct address through origin and core. Wrong-type pages, missing symbols and mismatched prefixes must not guide. Test notebook selected-page changes, resource reload, chunk reload, another player dialing, outgoing closure, incoming rejection, gate replacement/removal, and nearby Milky Way/Universe gates. Check the left-side shield and alarm controls. Compatibility was inspected against public 5.0.5.0-Beta and 5.1.0.0-Dev01102025 bytecode and the newer 1.20.1 source layout; these are not substitutes for runtime tests.

DHD revision: rapidly enter an entire address and core while the first chevron is still moving. Each accepted key must stay lit and the page hint must advance immediately; native JSG must finish the queue in order. Test duplicates, overlong addresses, abort/incoming interruption and another DHD, checking that pending lights clear with the native queue. Inspect the complete desk for atlas texture bleed at all facings. Reload an old C-shaped console and confirm only its two legacy front wings disappear.

DHD reach/snow regression: stand directly in front at normal survival eye height and press every rear-row key in all four facings. All key surfaces and the selection box now fit inside the one-block vertical cell. Check the continuous triangular tiling and central triangle emblem. In snowfall, clear pre-existing snow layers once, then confirm no new layers form on any addon block (including DHD side sections, all holders, controllers, alarm emitters and recharger casings). Surrounding terrain must still accumulate snow normally.

Equilateral keypad: all 37 cells, including the central core, must have equal side lengths on the tilted surface and alternate up/down with shared seams. Confirm all 36 installed JSG symbols remain individually clickable after the layout change.
