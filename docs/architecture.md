# dui architecture

Templates compile into a `MenuTemplate`. Rendering resolves read-only map bindings, expands repeats/conditions and produces a platform-independent `Canvas`: paints, click rectangles, heads, native-item placements, runtime rasters and bounded shader effects.

`dui-paper` turns the canvas into one Adventure text body. Rectangle/icon glyphs and negative advances position content; invisible positive advances carry hit regions. Shifted text is clipped into 9-pixel bands so later rows cannot paint over earlier glyphs. Player portraits use native Adventure object components.

Real item bodies preserve their ItemStack components, then receive a namespaced pack wrapper and an encoded placement payload. Shared resource-pack shaders move the native models into the canvas. A tagged glyph masks the native focus outline only on dui canvases. Effect rectangles and timing parameters use the same shared shader transport; no dialog-specific shader is generated.

Callbacks use random, single-use capabilities scoped to the dui instance and player. Updating or closing a session invalidates its capabilities. The main-thread dispatch checks the session revision again before running a handler. Business authorization and input validation remain application responsibilities.

The pack generator reads explicit verified vanilla inputs, derives shifted font images and item wrappers, and emits sorted ZIP entries with fixed timestamps. It also writes the font metrics, model registry, supported version and ZIP hash needed by the runtime adapter. The game JAR and generated textures stay outside source control.

`dui-demo` depends only on these public modules. Templates, procedural gift art, QR encoding, cart/ledger rules, video fetching, pack hosting and scenario diagnostics belong to that consumer. Its Fabric test client only automates input and reads ordinary widgets/screenshots; client mods are not required by the library.
