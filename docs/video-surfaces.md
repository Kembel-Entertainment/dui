# Dynamic video surfaces (experimental)

`dui` carries pixel buffers and logical inputs. It contains no emulator, ROM catalogue,
game button mapping, player save policy or application artwork. A consumer can stream
an emulator, remote framebuffer or generated animation through the same API.

## Consumer recipe

1. Generate a pack with `dui-pack` and load its matching metadata. The generated pack
   advertises `map-video-v1` and includes the shared video shader. Streamed pictures
   are **not resource-pack files** and introduce **no per-frame font providers**.
2. Compile a consumer XML template with `Dui.compileVideoSurface(name, xml, registry)`.
3. Render bindings to get `Presentation.specification()` and optional `Presentation.hud()`.
4. On the Paper thread call `Dui.openVideoSurface(player, spec, options, inputHandler)`.
   Only one world surface/dialog per viewer is active. A resource-pack prompt may defer
   startup: inspect `isStarted()` rather than assuming the screen exists immediately.
5. Any producer thread can call `session.submit(new VideoFrame(w, h, format, sequence, pixels))`.
   Frames own defensive pixel copies. Dimensions and format must match the specification.
   Call `hud`, `onClose` and `close` on the Paper thread. Dispose the producer in `onClose`.

```xml
<dui-video width="{{width}}" height="{{height}}" format="RGB888"
           fps="{{fps}}" max-tiles="{{tiles}}" bytes-per-second="{{budget}}"
           left="0" top="0" right="1" bottom="1"
           background="#000000" scale="fit">
  <dui-hud native-hud="cover" native-hud-color="#000000"/>
</dui-video>
```

All geometry, color, pacing and resource limits are consumer bindings. Normalized viewport
coordinates are in physical screen space, independently of GUI scale and FOV. `fit`
preserves source aspect ratio; `integer` rounds upscaling down to an integer factor.
Optional `dui-hud` children use the existing surface/component compiler and shared HUD fonts.
Shared HUD resources work even when a pack declares no world maps.

## Color transport

Minecraft's native map texture is 128×128 with a limited fixed palette. The codec uses
192 distinct opaque map colors as **data symbols**, rather than quantizing the picture
to those colors. The text shader recognizes a four-pixel signature plus versioned header,
reads adjacent symbols with `texelFetch`, and reconstructs the source RGB without lighting.
Its inverse palette is an exact two-probe hash table. The remaining 127 rows carry pixels.

`BGR555` carries the original 15-bit value in two symbols, with red in bits 0–4.
`RGB888` carries a 24-bit RGB value in four symbols. Choose RGB888 when reproducing an
existing core's expanded RGB565/XRGB8888 output exactly; do not silently quantize it to 555.
There are no GBA dimensions, artwork or control conventions in the shader.

The tile count including the background is
`1 + ceil(width / floor(128 / symbolsPerPixel)) * ceil(height / 127)`.
For 240×160 RGB888 this is 17 textures. Every texture is uploaded at least once, even for
an edge tile. The specification rejects budgets that cannot send an initial complete frame.
Map IDs and render entities are viewer-private packets; IDs are reused across sessions on
one connection. They are not persistent map objects or entities in a world.

## Input and lifecycle

Paper `PlayerInputEvent` supplies held logical forward/backward/left/right/jump/sneak/sprint
flags. Their physical bindings depend on the client's settings. Slot-change events are
pulses; the configured anchor slot is restored, and the consumer interprets slots itself.
For pointer-driven applications, opt in on the Paper thread:

```java
session.pointerInput(event -> {
  switch (event.type()) {
    case LOOK -> cursor.move(event.deltaYaw(), event.deltaPitch());
    case PRIMARY -> app.click();
    case SECONDARY -> app.controls();
    case SCROLL -> app.scroll(event.scrollSteps());
  }
});
```

`SurfacePointerInput` contains relative angles in degrees, a signed shortest-path slot
step, and a monotonic `nanoTime`. LOOK is sampled on the Paper tick; clicks/scroll flush
the latest look first. The library creates a viewer-private invisible Interaction target
and removes it with the session. Duplicate attack/interact callbacks in one tick are
coalesced per button. These are press pulses; releases and dragging are not captured.
The consumer owns pointer coordinates, sensitivity, scrolling distance and button policy.

Wheel capture replaces `SurfaceInput.SLOT`. Use `pointerInput(handler, false)` to retain
slot bindings alongside look/click events. Existing sessions without pointer registration
retain their input behavior and create no pointer target. Register before submitting
frames when the resource pack can defer startup. No new shader or pack is needed.

No Vanilla protocol provides arbitrary keyboard keys, an absolute OS pointer or raw
mouse-wheel deltas. Pitch remains camera-bounded. Wheel interpretation uses the shortest
path around the nine-slot ring (including 8→0 and 0→8); number keys are indistinguishable
and large bursts cannot be reconstructed exactly.

A private seat holds the player in place. The normal player camera stays active because Vanilla gates keyboard polling on it.
The map shader projects quads directly to the screen; consumer HUD surfaces can cover native
hotbar elements, and existing appearance masking hides held items. The crosshair remains a
Vanilla overlay. F1 on the unmodified client hides the native HUD for an unobstructed
fullscreen image; the server cannot toggle that client preference. This does not
change gamemode or server inventory. Closing, quit, death, teleport and plugin shutdown
release inputs, remove entities, hide the HUD and restore appearance/held slot.
External teleport and death keep their new location rather than teleporting back.

## Pacing and limitations

Producer speed, transport speed and visible frame rate are different quantities.
Paper ticks only run lifecycle/input handling. Encoders and the connection event loop
send frames independently of the 20 Hz game tick. Tile patches are sent in a bundle to
avoid displaying partial frames. Changes are computed against the last accepted network
frame, never a dropped frame. Under pressure a single latest frame replaces older ones;
there is no unbounded queue. An unchanged picture causes no new map packets.

Byte pacing charges each accepted frame's patch cost continuously. There are no
one-second budget windows that send a burst followed by a long wait. The configured
byte rate permits one initial frame and then spreads subsequent updates over time.
`VideoSurfaceSpec.maximumFrameBytes()` estimates a conservative full-update cost;
`sustainableFps()` is `min(maximumFps, bytesPerSecond / maximumFrameBytes)`. Producers
can use that planning ceiling to avoid decoding frames they cannot transmit. Small
patches can permit faster transport when a producer keeps the higher FPS limit.
For example, RGB888 1024x576 uses 161 maps and about 2.64 MB for a full update:
60 full updates/s need about 151 MiB/s before network compression. A 16 MiB/s
budget supports approximately 6.35 full updates/s, regardless of source codec.

The bridge writes only its private video bundles through the active connection's Netty
pipeline and waits for each write completion. Paper's usual off-thread `Connection.send`
queue would defer these packets to a server tick and limit the stream to about 20 Hz.
Bundling, compression and connection-local appearance masks still run in the normal pipeline.

`statistics()` reports submitted/sent/dropped frames, patch bytes, encoding nanoseconds
and last accepted sequence. Byte counts exclude transport compression and framing overhead.
Actual client texture uploads, render rate, bandwidth and latency still constrain performance.
This packet/reflection bridge is pinned to Paper 26.2; test it on version upgrades.
Audio transport and arbitrary keyboard/desktop mouse capture are outside this component.

The palette constants and two-probe inverse table can be reproduced against the official
client JAR with `python3 scripts/generate-video-protocol.py --minecraft-jar /path/to/client.jar
--check` (Java 25 `javap` on PATH or JAVA_HOME). Re-run this and real-client scenarios when
upgrading Minecraft. The shader also depends on Minecraft's current reverse-depth projection.
