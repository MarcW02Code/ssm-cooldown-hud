# SSM Cooldown HUD (debug build v0.1.0)

Client-side Fabric mod scaffold for showing Super Smash Mobs kit ability
cooldowns persistently, instead of only whatever the server's own transient
widget is currently displaying.

## Before you build: check the mod-allowlist question

Mineplex's rules ban client modifications that give "an advantage over
others." A cooldown-visibility HUD is a genuine gray area - it's not reading
anything the server didn't already send you, but it does make info that's
normally only visible while holding a specific item (or, per your
screenshots, only for the ability you most recently fired) persistently
visible for everything. Worth a quick check of Mineplex's current
Discord/forum mod policy before you use this outside of a private/MPS match.

## What's confirmed vs. what's a guess

Minecraft moved off Yarn mappings entirely as of 26.1, so a lot of older
Fabric tutorials (and my own first instincts) point at dead APIs. Everything
in this scaffold is checked against the live Fabric docs and the real
FabricMC/fabric-example-mod repo for MC 26.2:

- `build.gradle` / `gradle.properties` - real values as of Chaos Cubed
  (Loader 0.19.3, Loom 1.17.11, Fabric API 0.152.1+26.2, Java 25).
- `ClientReceiveMessageEvents.GAME` for reading action bar/chat text - confirmed API.
- `HudElementRegistry.attachElementBefore(...)` for drawing our own HUD panel - confirmed API (replaced the old `HudRenderCallback`, which was removed in 26.1).
- `GuiGraphicsExtractor` as the type handed to our HUD callback - this is what the current Fabric docs show, but Fabric's rendering API has been rewritten more than once as Minecraft's renderer changed, so if your IDE can't resolve the import, that's the one thing worth re-checking against https://docs.fabricmc.net/develop/rendering/hud.
- Reading the actual boss bar list client-side - **not implemented**. There's no public API for it (the list lives in a private field on `BossHealthOverlay`), so it needs a small accessor mixin. I didn't want to guess the exact field name for 26.2 and ship something that silently fails to compile - once we know we need it, I'll look up the real name and add it properly.

## What this build actually does right now

1. Logs every action-bar/chat message the server sends to your client log,
   prefixed `[SSM-HUD]`.
2. Tries a loose regex match for `<name> ... <seconds> Seconds` and, if it
   matches, starts tracking that ability's cooldown.
3. Draws a small debug panel in the top-left showing everything it's
   currently tracking, plus the last 5 raw messages it saw - so you can see
   directly whether it's picking up the cooldown widget at all.

## How to use it to figure out the real mechanism

1. `./gradlew build` (or open in your IDE and run the `runClient` task).
2. Join a private SSM match (MPS if you have Celestial+, or just a normal
   game) and fire an ability a couple of times.
3. Watch the debug panel. If lines show up with the ability name and a
   countdown, it's action-bar/chat based and step 2 above is basically
   already working - just needs the regex tightened against real data.
4. If nothing shows up in the panel or the log, it's almost certainly the
   real vanilla boss bar mechanism (which would match the screenshots you
   sent - that's the classic position/style for it). Tell me and I'll add
   the accessor mixin to read it directly, which is actually more reliable
   than text parsing anyway since we'd get the exact percentage instead of
   reading it back out of rendered text.

Either way, send me a few raw `[SSM-HUD]` log lines (or confirm "nothing showed up") and I'll wire up the real, persistent multi-ability tracker.

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
