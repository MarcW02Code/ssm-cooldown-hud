# SSM Cooldown HUD (debug build v0.1.0)

Client-side Fabric mod scaffold for showing Super Smash Mobs kit ability
cooldowns persistently, instead of only whatever the server's own transient
widget is currently displaying.

## What's confirmed vs. what's a guess

Minecraft moved off Yarn mappings entirely as of 26.1, so a lot of older
Fabric tutorials (and my own first instincts) point at dead APIs. Everything
in this scaffold is checked against the live Fabric docs and the real
FabricMC/fabric-example-mod repo for MC 26.2:

- `build.gradle` / `gradle.properties` - real values as of Chaos Cubed
  (Loader 0.19.3, Loom 1.17.11, Fabric API 0.152.1+26.2, Java 25).
- `ClientReceiveMessageEvents.GAME` for reading action bar/chat text - confirmed API.
- `HudElementRegistry.attachElementBefore(...)` for drawing my own HUD panel - confirmed API (replaced the old `HudRenderCallback`, which was removed in 26.1).
- `GuiGraphicsExtractor` as the type handed to the HUD callback - this is what the current Fabric docs show, but Fabric's rendering API has been rewritten more than once as Minecraft's renderer changed, so if your IDE can't resolve the import, that's the one thing worth re-checking against https://docs.fabricmc.net/develop/rendering/hud.
- Reading the actual boss bar list client-side - **not implemented**. There's no public API for it (the list lives in a private field on `BossHealthOverlay`), so it needs a small accessor mixin. I didn't want to guess the exact field name for 26.2 and ship something that silently fails to compile.

## What this build actually does right now

1. Logs every action-bar/chat message the server sends to your client log,
   prefixed `[SSM-HUD]`.
2. Tries a loose regex match for `<name> ... <seconds> Seconds` and, if it
   matches, starts tracking that ability's cooldown.
3. Draws a small debug panel in the top-left showing everything it's
   currently tracking.
   
## How to use it to figure out the real mechanism

1. `./gradlew build` (or open in your IDE and run the `runClient` task).
2. Join a private SSM match (MPS if you have Celestial+, or just a normal
   game) and fire an ability a couple of times.

## Project layout

```
ssm-cooldown-hud/
  build.gradle
  gradle.properties
  settings.gradle
  src/main/resources/fabric.mod.json
  src/client/java/dev/riftwatch/ssmcooldownhud/
    SSMCooldownHudClient.java   - entrypoint, message listener, HUD panel
    CooldownTracker.java        - persistent per-ability cooldown state
```
