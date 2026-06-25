# IRL Lights x Flashback

A [Flashback](https://modrinth.com/mod/flashback) addon that animates
[IR Lights](https://github.com/quaIett/irl-editor) light sources with Flashback keyframes.

It adds an **IRL Light** keyframe type to the Flashback timeline. Pick a placed light, drop
keyframes, and its position, colour, intensity, radius/range, spot direction, cone and volumetrics
animate in the editor preview and in the exported video.

This is a standalone addon. It does **not** bundle or fork Flashback, and it adds nothing to vanilla
Flashback when IR Lights is absent, so Flashback keeps working unchanged with or without it.

## Requirements

- Minecraft 1.21.1, Fabric Loader, Fabric API
- [Flashback](https://modrinth.com/mod/flashback)
- **IR Lights with the Flashback bridge.** This addon talks to IR Lights through a small bridge API
  that the public IR Lights builds do not ship. Build `irl-redactor` from the
  [`port/1.21.1-flashback`](https://github.com/zqicev/irl-editor-fixes-fork/tree/port/1.21.1-flashback)
  branch of `irl-editor-fixes-fork` (it also carries the Axiom and far-from-origin light fixes).
  Without IR Lights present the addon stays inert and the IRL Light entry is hidden.

## Usage

1. Place lights in the IR Lights editor (press `J`) in the replay world.
2. In the Flashback timeline, add an **IRL Light** track and pick the light to control.
3. The first keyframe selects the target light. Each later keyframe on that track snapshots the
   light's current live state, so you move the light in the IR Lights editor, scrub, and drop a
   keyframe, the same way a camera keyframe records the camera as it is now.
4. One track per light. Add several tracks to animate several lights independently.

## Building

The addon compiles against Flashback but does not bundle it. Put a Flashback jar (for example the
official one from Modrinth) at `libs/flashback.jar`, then:

```bash
./gradlew build      # Windows: gradlew.bat build
```

Output: `build/libs/irl-flashback-1.0.0.jar`. Since Flashback is compile-only, the jar stays tiny
and the user supplies Flashback themselves.

## How it works

Three Sponge mixins into Flashback (all `remap = false`, because the targets are Flashback's own
classes, not Minecraft):

- `Keyframe$TypeAdapter` — (de)serialise the IRL Light keyframe stored in a track.
- `FlashbackGson` — register the concrete keyframe adapter, because the editor history serialises a
  keyframe by its runtime type rather than through the hierarchy adapter; both paths are needed or
  editor-state load fails.
- `TimelineWindow#createNewKeyframe` — reuse the chosen light for later keyframes on a track.

IR Lights is reached purely by reflection (`org.qualet.irlredactor.api.IrlFlashbackBridge`), so the
addon has no compile-time dependency on it.

## License

MIT, see [LICENSE](LICENSE).
