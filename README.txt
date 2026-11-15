DINOKID509S ZONE MOD (Fabric 1.21.1)

HOTKEYS (rebindable in Controls > dinokid509s Zone mod)
  Z   toggle zone on/off
  X   cycle mode
  ]   intensity up
  [   intensity down

MODES
  Full Red Tint, Red Border Vignette, Light Tint + Heavy Border,
  Pulsing Border, Heartbeat Pulse, Solid Red Frame

BUILD
  Requires JDK 21.
  Easiest: go to https://fabricmc.net/develop/template/ , generate a 1.21.1 project
  (package: com.zonemod, mod id: zonemod), then copy this project's src/ folder
  over the generated one (replace fabric.mod.json too).
  Then run:  ./gradlew build
  Jar appears in build/libs/ (use the one WITHOUT -sources).
  Drop it in .minecraft/mods along with Fabric API.
