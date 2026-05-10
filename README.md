# 🎭 Player Ragdoll — NeoForge 1.21.1

A self-contained player ragdoll mod with articulated physics and unique locomotion.

## Features

- **10-segment articulated ragdoll**: head, torso, upper arms, forearms, thighs, shins
- **Self-contained physics** — no Physics Mod, no Sable required. Uses Verlet integration + XPBD constraints
- **Unique movement system:**
  - `W/A/S/D` alone = slow floppy drag crawl
  - `W + Space` = launch forward
  - `A/D + Space` = fling sideways
  - `Space` alone = weak upward flail
- **Toggle anytime** with `R` (rebindable in Controls)
- **Client-side prediction** for smooth rendering at any framerate
- **No external runtime dependencies** — drop in and play

## Installation

1. Download the `.jar` from [Releases](../../releases)
2. Place in your `mods/` folder alongside NeoForge 1.21.1
3. Launch!

## Build from Source

```bash
git clone https://github.com/yourname/playerragdoll
cd playerragdoll
./gradlew build
# Output: build/libs/playerragdoll-1.0.0.jar
```

### GitHub Actions

Every push to `main`/`master`/`dev` automatically builds and uploads the JAR as a workflow artifact. Pushing a tag like `v1.0.0` creates a GitHub Release.

## Architecture

```
RagdollPhysics          — Verlet + XPBD simulation (10 segments, 11 constraints)
  └─ RagdollSegment     — One rigid body: position, prev-position, half-extents
  └─ DistanceConstraint — Joint connecting two segments

RagdollCapability       — Per-player state (server-side map)
RagdollCommonEvents     — Server tick → physics.tick() → player.moveTo()
RagdollNetwork          — Toggle + Input packets (C→S), Sync packet (S→C)
ClientRagdollHandler    — Key polling, client-side physics mirror, input send
RagdollPlayerRenderer   — Segment-by-segment player model draw
```

## Physics Notes

- Gravity is `−0.08` blocks/tick² (matches vanilla)
- Drag coefficient `0.98` per tick
- Constraint solver runs **8 iterations** per tick for near-rigid joints
- Block collisions resolved per-segment with least-penetration-axis push-out
- Restitution (bounciness) `0.35`, floor friction `0.75`

## License

MIT — do whatever you want.
