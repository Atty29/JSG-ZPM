# Classic Ancient visual contract

The user's supplied JSG 1.12 screenshots are the primary appearance target. They supersede the earlier cool-grey, simplified triangular interpretation: use warm brown, finely mottled metal; a notched three-wing hub; substantial raised emblems and dense ribs; recessed side panels; projecting consoles with embossed original glyphs and pale lights; and a ZPM with a broad collar, dense dark lattice and muted olive/red facets.

## Original authorship

The classic source at https://github.com/Tau-ri-Dev/Mod-JSG-1.12.2/tree/4f35bf464558c44f6f78506ca45430f54a3559e1 was inspected outside this repository for dimensions and visual reference. The user also supplied classic in-game screenshots, an MGM Tech Journal image and prop photographs. No source OBJ vertices, topology, UVs, texture pixels or files are copied, traced or packaged. The deterministic standard-library generator reads no reference files. The reconstruction aims for a close visual match; it is not asserted to be pixel-identical.

## Solid detail and dimensions

The previous near-planar surface overlays have been replaced with closed extruded geometry: layered side panels extend up to 0.077 blocks from their dark backing, top symbols stand roughly 0.03 blocks above the table, skirt ribs have distinct side faces and three control boxes project from notched gaps. The central six-spoke emblem has a dark plinth and a raised brown face. The console relief contains original geometric glyphs, not copied lettering. The item model declares its OBJ geometry explicitly for inventory rendering.

Hub bounds including relief: 1.530 × 1.470 × 1.168 blocks. Installed ZPM height remains 0.420 blocks with 0.300-block independent travel. Socket centres remain (-0.265,-0.204), (0,+0.246), (+0.265,-0.204) relative to the block centre facing north. Well radius is 0.136 blocks; mounted crown radius is approximately 0.110 blocks. Array/column/generator mounting compensation is preserved.

The hub has 5,807 authored faces and the module 1,484. Detail is static baked geometry with no new runtime renderer or gameplay logic. Collision/placement retain the original one-block footprint; the table visually overhangs it. Existing active-module brightness behaviour is unchanged.

## Materials

Eleven original 256×256 textures live in `textures/block/ancient/`, within the default Minecraft atlas coverage. Multi-scale deterministic grain supplies fine mineral/weathering variation; silhouettes, panel relief and motifs come from geometry rather than painted outlines. Future Array, Column, Controller, Generator, Pegasus DHD and Alarm Emitter models should reuse this material vocabulary. Their body models are not replaced in this pass.

| Material | Base RGB |
| --- | --- |
| panel | 105, 73, 55 |
| trim | 139, 101, 77 |
| recess | 43, 32, 28 |
| binder | 22, 24, 22 |
| crystal | 187, 116, 22 |
| crystal_warm | 151, 78, 17 |
| crystal_pale | 205, 139, 35 |
| regulator | 135, 37, 26 |
| light | 184, 207, 213 |
| crystal_olive | 99, 105, 29 |
| crystal_red | 139, 59, 23 |

## Preservation and verification

No recipes, energy logic, balance, block-entity state, bank modes, alarms, gate integration, runtime dependency or released-JSG hotfix code is changed by this relief revision. The previous independent slot animations and facing-aware click mapping remain intact.

Regenerate with `python tools/build_ancient_assets.py`. Validate with `python tools/validate_ancient_assets.py --check-vanilla-atlas`. CI compiles with Java 17 / Gradle 8.14.4 and adds `--jar` to check exact packaged resources. Checks cover OBJ references, normals, nondegenerate faces, dimensions, crown/well clearance, PNG integrity and actual atlas directory coverage against Mojang's SHA-checked 1.20.1 client.

The original f228a00 texture-folder error remains fixed: sprite files and references use `block/ancient/`. The validator rejects the former `ancient/` paths even when PNGs exist in the JAR. The user has confirmed textures render in-game after this correction.

Inspect the new relief at eye level and from above, all four facings, raised/lowered/transitioning independent bays, inventory and hand views, and F3+T. Software previews and CI do not establish identical in-game lighting or visual acceptance. TESTING.md retains the gameplay/runtime checklist.


## Crystal-course and console repair

The ZPM is now 36 separate faceted blades in three concentric courses. Their tip heights decrease from 1.00 to 0.90 to 0.79 model units toward the outside; small individual height variation exposes the crystal faces. The regulator remains at 1.025 and the bottom at -0.025, preserving the 0.42-block installed height and existing lift travel. All three courses clear the current sockets.

Console placement uses a reflected local basis. Its faces now reverse winding so the outward walls and glyphs remain visible under back-face culling. Solid rear/side recess walls conceal lowered modules. The skirt and cooling ribs stop around each control opening rather than intersecting the lights and glyph deck. No textures, renderer transforms, animation code or gameplay systems change in this repair.
