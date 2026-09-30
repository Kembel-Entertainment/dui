# dui

**Work in Progress.** A template-driven UI library for Paper servers and unmodified Minecraft clients. API and pack formats may change before a stable release.

Built by [Kembel Entertainment](https://kembel.gg). Java packages and Maven group: `gg.kembel.dui`.

Start with [your first plugin](docs/quickstart.md), a complete buildable example, then [application recipes](docs/recipes.md) for stateful controls, media and forms. For coding agents, supply [the LLM guide](docs/llm-guide.md); [llms.txt](llms.txt) indexes the documentation.

| Module | Purpose |
| --- | --- |
| `dui-core` | Strict templates, data binding, layout, hit regions, themes, runtime images and effect parameters |
| `dui-paper` | Embedded Paper API, player sessions, callbacks, Adventure components, heads and native item carriers |
| `dui-pack` | Build-time fonts, native-model wrappers, reusable assets and shared GUI shaders |

The example application and real-client tests live in the separate [dui-demo](https://github.com/Kembel-Entertainment/dui-demo) repository. dui has no commands, shop state, balances, reward logic, network feed service or built-in pack HTTP server.

## Build

Use Java 25:

```sh
./gradlew build
./gradlew publishToMavenLocal
```

Builds and unit tests do not start Minecraft. Pack tests generate their own synthetic inputs. Generated pack files and game inputs are not checked into this repository.

## Embed in a Paper plugin

```groovy
repositories {
    mavenLocal() // local WIP distribution; no remote Maven release yet
    mavenCentral()
    maven { url = 'https://repo.papermc.io/repository/maven-public/' }
}
dependencies {
    implementation 'gg.kembel.dui:dui-paper:0.1.0-SNAPSHOT'
    compileOnly 'io.papermc.paper:paper-api:26.2.build.129-stable'
}
```

Package dui and its runtime dependencies inside your plugin. Paper, Adventure and the Paper API remain provided by the server. `dui-demo` demonstrates a self-contained plugin JAR. For production builds, a shading tool may relocate `gg.kembel.dui` and Gson; preserve `/ui` resources and the `dui:` pack namespace. No separate dui server plugin is installed.

```java
PackMetadata metadata = PackMetadata.read(dataFolder.resolve("pack/dui.json"));
PackDescriptor pack = PackDescriptor.of(URI.create("https://example.com/dui.zip"), metadata);
Dui ui = Dui.create(this, pack, metadata);
MenuTemplate menu = ui.compile(Files.readString(dataFolder.resolve("menu.html")));
DialogSession session = ui.open(player, menu,
    ViewModel.data(Map.of("name", player.getName())),
    context -> {
        if (context.action().equals("hello")) {
            player.sendMessage("Hello!");
            context.session().close();
        }
    });
```

```xml
<dui-menu width="300" height="90">
  <dui-column gap="9">
    <dui-heading label="Hello {{name}}" height="27" />
    <dui-button id="hello" action="hello" label="Say hello" height="27" />
  </dui-column>
</dui-menu>
```

The session waits for the matching resource pack to load. It can update its template/view/options/handler, report a server-known close with `onClose`, and close explicitly. The example closes after **Say hello**. Accepted custom actions consume the displayed revision's callbacks; update the session if it should remain interactive. Call `ui.close()` from your plugin's `onDisable`. All Paper-facing mutations must run on the main server thread. See the LLM guide for Escape handling and asynchronous update limits.

Use `DialogOptions` for titles, native form inputs, confirmation buttons and exit actions. `ViewModel` supplies data, image keys, real ItemStacks and explicit HTTP(S) links keyed by hit ID. Applications validate form values and authorize their own business actions.

## Resource pack

Build from a verified official Minecraft 26.2 client JAR:

```sh
./gradlew :dui-pack:generatePack -PminecraftJar=/path/to/minecraft-26.2-client.jar
```

Output: `dui-pack/build/pack/dui.zip` and `dui.json`. Host the ZIP yourself and deploy the matching metadata with the plugin. The generator verifies the client JAR against the pinned official SHA-1. Its download URL and checksum are in the pack module's `dui/minecraft.json` manifest.

`PackGenerator.generate(clientJar, additionsDirectory, outputDirectory)` supports your own `assets/<namespace>/...` files. Item definitions under `items/` receive native-render wrappers and appear in the metadata registry. Keep application assets outside `dui:`. Repeated builds from identical inputs produce identical ZIP bytes and hashes.

Runtime images use RGB-tinted rectangle glyphs. They do not add thumbnails to the pack or require a pack reload. Supply a bounded `RasterImage` map and reference a key with `<dui-image source="{{imageKey}}" .../>`.

## Status and limits

- Paper/Minecraft **26.2**, Java **25**; locally verified on macOS ARM64 with the vanilla renderer.
- HTML-like XML component DSL, not a browser: no CSS engine, DOM or JavaScript.
- Canvas widths 120–480 GUI pixels; heights up to 360 in 9-pixel rows. Click regions follow that row grid.
- At most eight shared shader-effect components and 16,384 sampled image pixels per canvas.
- GUI scale and window dimensions are not available to a vanilla server. Applications offer Compact/Spacious choices.
- Native form inputs retain Minecraft's native dialog layout. Resource packs that replace the same core shaders require integration work.
- Pack/API versions are matched explicitly; multi-version support and shared-pack coordination across independently versioned plugins are future work.

See [quickstart](docs/quickstart.md), [LLM guide](docs/llm-guide.md), [components](docs/components.md), [architecture](docs/architecture.md), [rendering protocol](docs/rendering.md) and [third-party notices](THIRD_PARTY_NOTICES.md). Own code is licensed under [MIT](LICENSE).
