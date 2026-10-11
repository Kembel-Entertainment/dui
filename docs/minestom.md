# Minestom and portable menus

DUI 0.3.0-SNAPSHOT targets Minecraft 26.3 and Java 25. `dui-runtime` owns presentation, pack readiness, callbacks, UI tasks, world maps and video pacing. `dui-paper` and `dui-minestom` supply native dialogs, items, profiles, entities and connections. The same generated ZIP and metadata serve both hosts.

The Minestom adapter currently requires the immutable 26.3 snapshot `26_3-20261007.224755-10` from the official 26.3 branch. The published Minestom release `2026.10.07-26.2` speaks 26.2 and cannot host this client. Update the pin only after running the adapter and actual-client checks. See [the upstream 26.3 work](https://github.com/Minestom/Minestom/pull/3369).

## Dependencies

Build DUI or publish its modules to Maven local first. Embed the runtime modules in the consumer; supply Minestom itself from the host.

```groovy
repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = 'https://central.sonatype.com/repository/maven-snapshots/'
        content { includeModule('net.minestom', 'minestom') }
    }
}
dependencies {
    implementation 'gg.kembel.dui:dui-minestom:0.3.0-SNAPSHOT'
    implementation 'net.minestom:minestom:26_3-20261007.224755-10'
}
```

DUI does not start Minestom, register commands or host HTTP. Initialize Minestom and host the pack in your application, then create one adapter:

```java
var metadata = gg.kembel.dui.runtime.PackMetadata.read(packDirectory.resolve("dui.json"));
var descriptor = gg.kembel.dui.runtime.PackDescriptor.of(packUrl, metadata);
var adapter = gg.kembel.dui.minestom.Dui.create(descriptor, metadata);
var runtime = adapter.runtime();
var template = runtime.compile("<dui-menu width=\"180\" height=\"27\"><dui-text width=\"180\" height=\"27\" color=\"#FFFFFF\" label=\"Hello {{name}}\" /></dui-menu>");

// An instance tick already owns the player. From elsewhere acquire explicitly.
player.acquirable().sync(p -> {
    var viewer = adapter.viewer(p);
    var model = gg.kembel.dui.runtime.ViewModel.data(Map.of("name", p.getUsername()));
    runtime.open(viewer, template, model, action -> {
        if (action.action().equals("close")) viewer.message(Component.text("Closed"));
    });
});
```

Call `adapter.close()` during shutdown. It removes its isolated event node and releases sessions and private presentation entities. Recreating an adapter after close creates independent callback identities.

## One presentation, two hosts

Put shared application state and presentation in a module depending only on `dui-runtime`. Use `UiViewer`, `ViewModel`, `DialogOptions`, `DialogInput`, `MenuView`, `MenuDefinition` and `MenuController` from `gg.kembel.dui.runtime`. Shared code receives a runtime and viewer:

```java
// Paper host, on the Bukkit main thread:
sharedMenu.open(paperAdapter.runtime(), paperAdapter.viewer(paperPlayer));

// Minestom host, with ownership of the player:
sharedMenu.open(minestomAdapter.runtime(), minestomAdapter.viewer(minestomPlayer));
```

Capture native resources at this boundary. `PaperItem.of(stack)` and `MinestomItem.of(stack)` retain original item components while generated model/color overrides operate on snapshots. Appearance snapshots are `paper.PlayerAppearance.capture(player).portable()` and `MinestomAppearance.capture(player)`. Supply these as `runtime.PlayerAppearance` values in the shared view model. Projection performs no profile lookups.

Portable dialog inputs cover text/multiline, boolean, number range and single option. Responses use nullable typed `text(key)`, `number(key)` and `bool(key)` accessors. Trusted `BodyBackend` implementations return portable `DialogBody` values. Native extension bodies/inputs require the corresponding adapter and should stay in host code.

Both hosts expose the same `runtime.spatial().openWorldMap(...)` and `openVideoSurface(...)` contracts, including pack gating, camera/click/slot input, HUDs and exclusive per-viewer ownership. Video producers may call `submit()` and `statistics()` outside the player context; all other session mutations require ownership.

## Threading and lifecycle

Minestom can move an entity's tick ownership between Java threads. DUI checks current ownership on each call; it does not bind a session to the Java thread that opened it. Use `viewer.execute(...)` for provider completions, or `player.acquirable().sync(...)` for synchronous host work. Provider code must not mutate sessions directly.

`session.tasks()` lasts until close; `viewTasks()` is replaced on update. Jobs are invalidated on replacement, pack failure, disconnect and adapter close. Cancelling a UI subscription does not cancel a shared application provider. Business transactions and durable work belong to the application.

Spatial UI masks the viewer's own appearance and hands through the native connection. It does not change the server inventory or other players' view of it. Minestom's public packet event can cancel immutable packets; masked self metadata/inventory updates are resynchronized from current server state on close. External pre-encoded packets bypass that event and must be coordinated by the host.

The Minestom video bridge bounds its queue to one pending frame. Acceptance means the native connection has serialized the frame, not that the peer has acknowledged it. Paper uses Netty write completion. The common mailbox, rate/byte budgets and delta codec are identical.

The separate demo provides `platform-showcase` and `minestom-server`. `/platform`, `/platform map` and `/platform video` use the same presentation and pack on both servers. Bukkit-specific shop, equipment and teleport business actions remain application host integrations.
