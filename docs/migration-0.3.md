# Migrating to 0.3 / Minecraft 26.3

Upgrade all DUI artifacts together to `0.3.0-SNAPSHOT`. Use Java 25, Gradle 9.6.0 and Paper `26.3.build.170-beta`, or Minestom `26_3-20261007.224755-10`. Both server pins are pre-release dependencies; the Minestom Maven snapshot is pinned to an immutable timestamp.

Rebuild the resource pack from the verified official 26.3 client JAR (SHA-1 `e877b6a07acd633fb3bb475002175cec036e7b87`). Deploy the new ZIP and its generated metadata together with the application. Minecraft 26.3 uses resource-pack format 97.1. Old 26.2 metadata is rejected even though the DUI renderer/world-map payload remains protocol 4.

```sh
./gradlew build :dui-pack:componentDocs
./gradlew :dui-pack:generatePack -PminecraftJar=/path/to/minecraft-26.3-client.jar
./gradlew :dui-pack:verifyClientShaders -PminecraftJar=/path/to/minecraft-26.3-client.jar
```

For a consumer pack pass `-PshaderPack=/path/to/consumer/dui.zip` to the shader check. This compiles generated text and position/texture/color stages against exact vanilla include files, including GUI, see-through and OIT variants. An actual client is still required to verify geometry, draw order and shader uniforms.

## Existing Paper applications

Existing public `gg.kembel.dui.paper` entry points and session wrappers remain available. Change dependency and plugin `api-version` pins to 26.3, rebuild the application and pack, and rerun native dialog, equipment, map and video checks. The wrappers delegate shared rendering and lifecycle to `dui-runtime`.

Custom pack shaders must use 26.3 `#include` syntax, the new dynamic/global uniform layouts and explicit varying locations. Native item models use `shade_direction_override`. The pack generator supplies these changes for its technical assets; consumer-owned snippets must be compatible with the new includes and OIT outputs.

The separate Fabric test client uses Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3 and Loom 1.17.21. Minecraft replaced GLFW window/input with SDL3 and moved GPU APIs into RenderPearl. Test harness input hooks and pixel readback must be updated accordingly.

## Sharing an application with Minestom

Extract presentation and state projection into a consumer module depending on `dui-runtime`. Change imports for view models, dialog options/inputs, controllers and sessions to `gg.kembel.dui.runtime`. Replace a native `Player` in shared presentation with `UiViewer`. Native items and appearance snapshots enter through host adapters.

Keep inventory transactions, permission checks, teleport/world access and application providers in host code. Supply these through consumer-owned interfaces; a Bukkit `Player` cannot be cast into a Minestom `Player`.

Use `paperAdapter.runtime()/viewer(player)` and `minestomAdapter.runtime()/viewer(player)` to call the same shared presentation. Close the adapter in the host lifecycle. See [Minestom setup and portable API](minestom.md) for dependencies, examples and threading rules.

## Review gates

Run library tests, consumer tests, Studio build/checks and generated-contract checks. Generate the combined pack and compile its shaders. Run actual 26.3 clients against both hosts to check callbacks, native input responses, pack readiness, world-map cleanup, private entities, video frames and inventory restoration. Keep historical acceptance records tied to the version and artifacts they actually tested.
