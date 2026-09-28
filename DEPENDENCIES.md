# Dependencies and attribution

## Just Stargate Mod (JSG)

JSG-ZPM is an independent addon for **Just Stargate Mod** by Tau'ri Development.

- Project/source: https://github.com/Tau-ri-Dev/Mod-JSG
- Runtime mod id: `jsg`
- Target platform: Minecraft 1.20.1 Forge
- JSG is separately licensed by its authors (currently published as All Rights Reserved).

JSG-ZPM does **not** bundle, redistribute, or claim ownership of JSG code, models, textures, sounds, or other assets.

JSG is required at runtime through Forge `mods.toml`, but JSG-ZPM deliberately does not compile against JSG private implementation classes. Integration uses registry IDs/tags plus a small runtime compatibility bridge so released JSG builds are not forced to expose private DHD package names used by newer development builds.

Compatibility code should prefer JSG's public/stable API surface where available and clean interoperability rather than copying JSG implementation code.
