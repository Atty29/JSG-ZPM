# Ancient hardware visual contract

This pass replaces the Zero Point Module and Atlantis ZPM Hub placeholders with original, reproducible meshes and textures. All gameplay systems and the released-JSG runtime compatibility hotfix remain in place. The only interaction adjustment is mapping hub clicks to the visible, rotated bay centres. Slot numbering, insert/remove/toggle operations, independent animation progress, energy and bank selection are unchanged.

## Reference and authorship

Visual reference: [classic JSG 1.12.2](https://github.com/Tau-ri-Dev/Mod-JSG-1.12.2/tree/4f35bf464558c44f6f78506ca45430f54a3559e1), inspected outside this repository. Measurements came from the bounds of `models/tesr/zpm/zpm.obj`, `pg_zpm_hub.obj` and the transforms in `ZPMHubRenderer.java`. The old texture sheets and a local shape preview informed material and silhouette decisions only. No old OBJ vertices, topology, UVs, texture pixels, files or renderer code were copied into these assets.

`tools/build_ancient_assets.py` authors every mesh vertex, UV and texture pixel from primitives and a hand-selected palette, with no external input files or packages. The output is original artwork inspired by the reference; it is not a conversion, recolour or claim of pixel-perfect reproduction. Forge's built-in OBJ loader supports the faceted shapes and genuinely open wells without linking any JSG classes.

| Measurement | Classic reference | Recreated asset |
| --- | --- | --- |
| Hub width/depth/height | 1.487 / 1.424 / 1.169 blocks | 1.518 / 1.439 / 1.168 blocks, including skirt ribs |
| Installed ZPM width/depth/height | 0.220 / 0.216 / 0.420 blocks | 0.220 / 0.220 / 0.420 blocks |
| Hub lift travel | 0.30 blocks | 0.30 blocks per independent slot |
| Installed vertical extent, lowered | approximately 0.767–1.188 | 0.768–1.188 |
| Installed vertical extent, raised | approximately 1.067–1.488 | 1.068–1.488 |

The classic mesh has an off-centre origin. This recreation centres the item and uses a nearly symmetric triangular bay layout. Hub bay centres, relative to the block centre when facing north, are (-0.265,-0.204), (0,+0.246), (+0.265,-0.204). `HubGeometry` shares these positions between rendering and click selection. Table shaft radius is 0.145 blocks; the mounted crown radius is 0.110. The module item has identity FIXED transforms so holder scale has an explicit meaning. Array/column and generator renderer scales compensate for the new item height and retain their previous installed heights.

## Reusable materials

Seven 32×32 original textures live in `textures/ancient/`. New hardware can reference these names directly from its model JSON/MTL:

| Material | Base RGB | Use |
| --- | --- | --- |
| panel | 139,151,158 | Muted Ancient grey-blue stone-metal |
| trim | 177,186,190 | Raised edges, frame rails, socket lips |
| recess | 39,48,55 | Shafts, machinery, inset seams |
| binder | 24,26,28 | ZPM lattice and dark fittings |
| crystal | 225,144,39 | Faceted amber energy vessel |
| regulator | 137,43,34 | Restrained red crown regulator |
| light | 107,179,194 | Small cyan indicators |

The user-requested cool grey palette intentionally replaces the classic hub sheet's warmer brown cast. Pixel shading is restrained and deterministic. Geometry supplies the recessed panel frames, stepped feet and ribbed skirt. Existing active-module full-bright behaviour is preserved; the new textures do not introduce shader or emissive-layer dependencies.

Future Ancient ZPM Array, Ancient ZPM Column, Ancient Power Controller, Zero Point Energy Generator, Pegasus DHD and Alarm Emitter models should reuse this palette, scale their pixel density consistently, and use stepped frames and dark recesses with sparse cyan indicators. This pass does not replace their body models or legacy shared textures. They display the recreated ZPM wherever they already render the module item.

## Verification and remaining runtime checks

Run `python tools/build_ancient_assets.py` to regenerate, and `python tools/validate_ancient_assets.py` to check resource links, OBJ material/vertex/UV/normal references, nondegenerate faces, dimensions, socket clearance and PNG integrity. The generator requires only Python's standard library. CI runs the validator before compilation, builds with Java 17 / Gradle 8.14.4, then runs `--jar` to verify exact resource bytes in the test JAR.

These checks and the software asset preview do not replace a Forge client model bake or an in-game test. On a released-JSG installation, check inventory/hand/ground views, resource reload (F3+T), all four hub facings, each slot independently raised/lowered/transitioning, charged and empty modules, neighbour placement, and the existing array/column/generator mounts. Keep the full gameplay regression checklist in TESTING.md. Hub collision and placement remain the original single-block footprint; the classic-size visual table overhangs it. No recipes, balance, energy logic, alarms, bank modes or gate integration changed.
