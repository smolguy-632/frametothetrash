# Frame to the Trash — Minecraft 1.12.2 (Forge)

Port of NegativeOneHero's *Frame to the Trash* (CC0-1.0) to Minecraft 1.12.2 / Forge, built to run
alongside **Celeritas**.

The game keeps rendering as fast as it can, but only `updatesPerSecond` frames per second are
actually sent to the screen. Every other finished frame is thrown away, which cuts the cost of
excessive display updates.

## "Only show the latest frames"

* There is no queue and no background thread. A frame can only be presented **immediately after it
  finishes rendering**, so what reaches the screen is always the newest complete frame. An old frame
  can never be shown late.
* Skipped frames are overwritten in the back buffer by the next one.
* After a stall, missed presentation slots are **dropped**, never replayed as a burst.
* Of the frames around each deadline, the one that finishes *closest* to it is the one shown.

## Requirements

* Minecraft 1.12.2, Forge, client side only
* **MixinBooter** (Celeritas already requires it)
* For best results: **turn VSync off** (Celeritas Extra has its own VSync option; set it to Off).
  With VSync on, the swap itself waits for the monitor and the mod has little left to save.

## Config

`config/frametothetrash.cfg`, or Mods → Frame to the Trash → Config. Changes apply immediately.

| Option | Default | Meaning |
|---|---|---|
| `enabled` | `true` | `false` = vanilla behaviour |
| `updatesPerSecond` | `60` | Frames per second actually put on screen (1–1000). Match your monitor's refresh rate. |

## Building

You need a JDK to run Gradle (17 recommended). The Java 8 toolchain is downloaded automatically.

```
./gradlew build
```

The jar is `build/libs/frametothetrash-1.0.2.jar`. The first build takes a while because Gradle
downloads and decompiles Minecraft. `./gradlew runClient` starts a dev client.

Build scripts are GregTechCEu's Buildscripts (MIT, see `LICENSE-buildscripts.txt`), which use
RetroFuturaGradle. All settings are in `buildscript.properties`.

## How it works

`MinecraftMixin` redirects the `Display.update()` call inside `Minecraft.updateDisplay()`.
On a frame that should be shown it calls `Display.update()` as vanilla does (swap + input).
On a frame that should be discarded it calls only `Display.processMessages()`, so input and the
window stay responsive. `FramePacer` makes the decision. This replaces the original mod's
redirect of `RenderSystem.flipFrame` / `glfwSwapBuffers`, which doesn't exist in 1.12.2.

## Credits and license

Original mod: NegativeOneHero, CC0-1.0 (`LICENSE`). This port is released under the same terms.
