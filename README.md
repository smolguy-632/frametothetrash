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
| `skipUnseenFrames` | `true` | Display decoupling: frames that will not be shown are not rendered at all (see below). |
| `guiRateDivisor` | `2` | Open GUI screens (menus, inventory, chat) render and show at `updatesPerSecond / divisor`; `1` = uncapped like before. Input stays fully responsive. |
| `disableFbo` | `false` | Renders the world straight into the window's back buffer — no intermediate framebuffer, no per-frame full-screen blit. Entity outline glow effects and screenshots may break; off by default. |

## "Skip unseen frames" (display decoupling)

The loop keeps ticking game logic and polling input at full speed, but the world is only drawn on a
loop iteration whose frame is about to be put on the screen. That frame is drawn from the newest
input and tick state, is shown immediately after it finishes, and nothing older is ever displayed.
In-game F3 FPS counts loop iterations and goes into the millions; the monitor still gets
`updatesPerSecond` frames, which is all it can show anyway.

* Open GUI screens (menus, the config screen) update at `updatesPerSecond / guiRateDivisor` by
  default (half the rate), so menus cost much less GPU; clicks and typing always stay responsive.
  Set `guiRateDivisor` to 1 for the old uncapped behaviour.
* With the option off, every loop iteration still renders a frame that is then thrown away (the
  original mod's behaviour — same on-screen result, much more CPU/GPU work).

## Building

You need a JDK to run Gradle (17 recommended). The Java 8 toolchain is downloaded automatically.

```
./gradlew build
```

The jar is `build/libs/frametothetrash-1.2.0.jar`. The first build takes a while because Gradle
downloads and decompiles Minecraft. `./gradlew runClient` starts a dev client.

Build scripts are GregTechCEu's Buildscripts (MIT, see `LICENSE-buildscripts.txt`), which use
RetroFuturaGradle. All settings are in `buildscript.properties`.

## How it works

`MinecraftMixin` redirects the `Display.update()` call inside `Minecraft.updateDisplay()`.
On a frame that should be shown it calls `Display.update()` as vanilla does (swap + input).
On a frame that should be discarded it calls only `Display.processMessages()`, so input and the
window stay responsive. `FramePacer` makes the decision. This replaces the original mod's
redirect of `RenderSystem.flipFrame` / `glfwSwapBuffers`, which doesn't exist in 1.12.2.

With `skipUnseenFrames` on, two further gates in `Minecraft.runGameLoop()` skip the world render
(`EntityRenderer.updateCameraAndRender`) and the framebuffer blit on iterations that will not
present, so only the frame per presentation slot is ever drawn.

## Credits and license

Original mod: NegativeOneHero, CC0-1.0 (`LICENSE`). This port is released under the same terms.
