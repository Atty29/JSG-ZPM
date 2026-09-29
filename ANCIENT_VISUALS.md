# Classic Ancient visual contract

The user's supplied JSG 1.12 screenshots are the primary appearance target. They supersede the earlier cool-grey, simplified triangular interpretation: use warm brown, finely mottled metal; a notched three-wing hub; substantial raised emblems and dense ribs; recessed side panels; projecting consoles with embossed original glyphs and pale lights; and a ZPM with a broad flat gem face, dark lattice and three concentric courses of amber crystal blades.

## Original authorship

The classic source at https://github.com/Tau-ri-Dev/Mod-JSG-1.12.2/tree/4f35bf464558c44f6f78506ca45430f54a3559e1 was inspected outside this repository for dimensions and visual reference. The user also supplied classic in-game screenshots, an MGM Tech Journal image and prop photographs. No source OBJ vertices, topology, UVs, texture pixels or files are copied, traced or packaged. The deterministic standard-library generator reads no reference files. The reconstruction aims for a close visual match; it is not asserted to be pixel-identical.

## Solid detail and dimensions

The previous near-planar surface overlays have been replaced with closed extruded geometry: layered side panels extend up to 0.077 blocks from their dark backing, top symbols stand roughly 0.03 blocks above the table, skirt ribs have distinct side faces and three control boxes project from notched gaps. The central six-spoke emblem has a dark plinth and a raised brown face. The console relief contains original geometric glyphs, not copied lettering. The item model declares its OBJ geometry explicitly for inventory rendering.

Hub bounds including relief: 1.530 × 1.470 × 1.168 blocks. Installed ZPM height remains 0.420 blocks with 0.300-block independent travel. Socket centres remain (-0.265,-0.204), (0,+0.246), (+0.265,-0.204) relative to the block centre facing north. Well radius is 0.136 blocks; mounted crown radius is approximately 0.110 blocks. Array/column/generator mounting compensation is preserved.

The hub has 5,956 authored faces and the module 3,180. Detail is static baked geometry with no new runtime renderer or gameplay logic. Collision/placement retain the original one-block footprint; the table visually overhangs it. Existing active-module brightness behaviour is unchanged.

## Materials

Twelve original 256×256 textures live in `textures/block/ancient/`, within the default Minecraft atlas coverage. Multi-scale deterministic grain supplies fine mineral/weathering variation; silhouettes, panel relief and motifs come from geometry rather than painted outlines. Future Array, Column, Controller, Generator, Pegasus DHD and Alarm Emitter models should reuse this material vocabulary. Their body models are not replaced in this pass.

| Material | Base RGB |
| --- | --- |
| panel | 105, 73, 55 |
| trim | 139, 101, 77 |
| recess | 43, 32, 28 |
| binder | 22, 24, 22 |
| crystal | 220, 143, 25 |
| crystal_warm | 184, 81, 12 |
| crystal_pale | 249, 189, 53 |
| regulator | 135, 37, 26 |
| light | 184, 207, 213 |
| crystal_core | 247, 198, 82 |
| crystal_olive | 45, 139, 42 |
| crystal_red | 180, 35, 23 |

## Preservation and verification

No recipes, energy logic, balance, block-entity state, bank modes, alarms, gate integration, runtime dependency or released-JSG hotfix code is changed by this relief revision. The previous independent slot animations and facing-aware click mapping remain intact.

Regenerate with `python tools/build_ancient_assets.py`. Validate with `python tools/validate_ancient_assets.py --check-vanilla-atlas`. CI compiles with Java 17 / Gradle 8.14.4 and adds `--jar` to check exact packaged resources. Checks cover OBJ references, normals, nondegenerate faces, dimensions, crown/well clearance, PNG integrity and actual atlas directory coverage against Mojang's SHA-checked 1.20.1 client.

The original f228a00 texture-folder error remains fixed: sprite files and references use `block/ancient/`. The validator rejects the former `ancient/` paths even when PNGs exist in the JAR. The user has confirmed textures render in-game after this correction.

Inspect the new relief at eye level and from above, all four facings, raised/lowered/transitioning independent bays, inventory and hand views, and F3+T. Software previews and CI do not establish identical in-game lighting or visual acceptance. TESTING.md retains the gameplay/runtime checklist.


## Crystal-course and console repair

The ZPM has 36 separate faceted blades in three concentric courses extending DOWN from the flat gem face. Inner blades extend furthest (nominal lower end -0.025), middle blades end at 0.12 and outer blades at 0.31 model units, with unequal bevelled ends. The broad amber gem face is coplanar at 1.012; only its fine black rings and red regulator sit slightly above it. The regulator remains at 1.025 and the lowest tip at -0.025, preserving the 0.42-block installed height and existing lift travel. All three courses clear the current sockets. This corrects the previous reversed interpretation, which stepped the gem end instead of the underside.

Console placement uses a reflected local basis. Its faces now reverse winding so the outward walls and glyphs remain visible under back-face culling. Solid rear/side recess walls conceal lowered modules. The skirt and cooling ribs stop around each control opening rather than intersecting the lights and glyph deck. No textures, renderer transforms, animation code or gameplay systems change in this repair.


## Crystal materials and sealed hub shell

Amber, green and red blades use clean angular internal shading, narrow baked highlights and very low grain. Physical-coordinate UVs keep this shading continuous across side triangles. The crystal surfaces now use RGBA textures and the translucent item render type, with opaque inner facets for depth. The warm brown hub textures are unchanged.

The hub has continuous backing around short perimeter edges, shoulders joining the inset body to the skirt, solid console supports and wider recess return walls. Backing is offset from existing panels to avoid coplanar overlap. The automated validator casts outward-face-only rays at 18 sample points across all three console areas; these checks fail on the prior hub and pass on the repair. They complement rather than replace live multi-angle inspection.


## Aligned side consoles and glass rendering

The two diagonal consoles and their surrounding notch geometry share exact 45/135-degree placement axes, replacing the previous 30/150-degree console arrangement. Matching cheek widths replace the oversized return-wall patches. The front console remains in place. Rays check wall coverage; light-face normals verify the side consoles use the intended diagonal axes.

Five crystal textures are now RGBA with semi-transparent bodies (alpha 135/255) and more opaque white-tinted reflections (up to 230/255). The item selects `minecraft:translucent`, while opaque honey-coloured internal facets give the glass visible thickness. Binder, regulator and hub materials remain opaque. This uses Forge's supported translucent item path; it does not implement physical refraction or environment reflections. Translucent sorting and appearance require live tests in Fast, Fancy and Fabulous graphics modes, inventory, hands and every holder.

Technical references: [Forge render types](https://docs.minecraftforge.net/en/1.20.x/rendering/modelextensions/rendertypes/), [OBJ model implementation](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/src/main/java/net/minecraftforge/client/model/obj/ObjModel.java), [geometry baking](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/src/main/java/net/minecraftforge/client/model/geometry/SimpleUnbakedGeometry.java).


## Console cheek seam closure

Keep the skirt backing continuous around the authored console notches; the opening exclusion applies only to cooling ribs. Return walls now extend to the end of each console cheek, joining the skirt with concealed overlap. This closes the narrow slits that exposed lowered modules beside the panels. An additional 120 outward-face ray samples sweep both cheek seams from front and side views at several heights. They fail on the previous short return walls and pass on the repair. The ZPM geometry, JSON, material table and all texture resources are unchanged from Build #66.


## Wall array and horizontal column

Both existing three-part structures use dedicated original bay OBJ models. Placement, occupied blocks, slot selection and block entities remain unchanged. The horizontal array is three adjacent complete 45-degree wedges, with insertion perpendicular to the upward sloped front. The vertical column uses the same wedge turned 90 degrees onto its side, bridging two perpendicular room walls. Its ZPMs insert horizontally along the diagonal corner axis. Brown panels, raised trim and pale indicators reuse the approved hub materials.

Both bores have radius 0.156 and depth 0.525 blocks. The broad end cap is flush with the socket plane when seated; its small existing regulator gem stands 0.0063 blocks proud. The complete crystal body is enclosed. Travel of 0.54 blocks clears the raised socket lugs when withdrawn. Scale remains 0.483333. Socket centres are array (0.5,0.50,0.50) and column (0.5,0.50,0.50); seated centre offset is -0.512 times module scale along the insertion axis.

WallHolderGeometry supplies renderer mounting centres and axes for every facing. Existing part indices and independent animation are retained. Hub and ZPM assets, state, energy, recipes, alarms and compatibility logic are unchanged. All bay geometry fits inside its existing occupied block. Inventory icons show one bay.

The column renderer applies a -90-degree local roll before its pitch and facing yaw; its centre travels sideways and forward at equal rates with no vertical motion. The static north-facing wedge is turned 90 degrees about its centre on Z. Existing cardinal rotations allow the housing to fit all four room corners.

## Single-ZPM pedestal

New original dark-metal pedestal with two hollow silver rings, cyan vents/status pads, angular white glyphs and a white front strip. Ring top is 1.02 blocks high. The cap is flush at that plane; the tip enters the 0.68-block deck by about 0.075 blocks. A generated emissive overlay lights only panel surfaces when the server-side charged state is true, also emitting block light level 12. Empty/depleted models use dim inlays. The single slot seats directly and removes with shift-right-click; it reuses existing FE/network energy paths. Existing holder visuals and lift behaviour remain unchanged.

Regenerate this asset separately with `python tools/build_pedestal_assets.py`; the approved hub/ZPM/array/column generator is unchanged. `PedestalGeometryCheck` validates cap alignment, shallow seating, bore clearance and generated emissive geometry. In-game charge/depletion/reload tests are listed in TESTING.md.

The recharger now has a deep circular tube on its existing 3x3 mounting frame, three central ZPM sockets, radial white lights and green outer indicators. The front containment shield seals before gas fills the chamber. Light patterns sweep around the rim while active; stopping vents gas before reopening the shield. Leave two blocks clear in front.

The Atlantis DHD is a waist-height bronze desk with 36 Pegasus diamond keys and a separate core. JSG glyph masks are loaded from the installed mod at runtime, preserving resource-pack changes. Page hint colors use JSG configuration.
