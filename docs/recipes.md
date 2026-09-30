# Building applications with dui

These examples target **dui 0.1.0-SNAPSHOT, Java 25 and Paper/Minecraft 26.2**. Start with the [complete plugin quickstart](quickstart.md). The recipes add application behaviour through the public API; they do not require renderer changes. See [components](components.md) for exact attributes/defaults and [the LLM guide](llm-guide.md) for lifecycle rules.

## Checkbox and dropdown with application state

Save this template as `ui/preferences.html`. The popup has room for its two 18-pixel options below the field.

```xml
<dui-menu width="300" height="144" padding="9" theme="studio_dark">
  <dui-column gap="9">
    <dui-heading label="Preferences" height="18" />
    <dui-checkbox id="notifications" action="toggle_notifications"
                  label="Notifications" checked="{{enabled}}" height="27" />
    <dui-dropdown id="currency" action="toggle_currency" select="select_currency"
                  dismiss="dismiss_currency" value="{{currency}}"
                  open="{{dropdownOpen}}" label="Choose a demo currency" height="27">
      <dui-option value="copper" label="Copper" />
      <dui-option value="silver" label="Silver" />
    </dui-dropdown>
    <dui-text label="{{message}}" height="18" />
  </dui-column>
</dui-menu>
```

This complete consumer class compiles the supplied template and owns per-player state. Create it once after `Dui.create`, reading the resource as in the quickstart; call `preferences.open(player)` from your command. Declare `exampleui.preferences` in plugin.yml with the default access appropriate to your plugin. Call `forget(playerId)` on player quit and `clear()` on plugin shutdown, alongside `ui.close()`.

```java
package example.dui;

import gg.kembel.dui.core.MenuTemplate;
import gg.kembel.dui.paper.ActionContext;
import gg.kembel.dui.paper.DialogOptions;
import gg.kembel.dui.paper.Dui;
import gg.kembel.dui.paper.ViewModel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class PreferencesMenu {
    private static final Set<String> CURRENCIES = Set.of("copper", "silver");
    private final Dui ui;
    private final MenuTemplate template;
    private final DialogOptions options = DialogOptions.notice("Preferences", "Close", "close");
    private final Map<UUID, State> states = new HashMap<>();

    private static final class State {
        boolean enabled = true;
        boolean dropdownOpen;
        String currency = "copper";
        String message = "Changes are stored in memory.";
    }

    public PreferencesMenu(Dui ui, String templateSource) throws Exception {
        this.ui = ui;
        this.template = ui.compile(templateSource);
    }

    public void open(Player player) {
        if (!player.hasPermission("exampleui.preferences")) {
            player.sendMessage("You cannot open these preferences.");
            return;
        }
        var state = states.computeIfAbsent(player.getUniqueId(), id -> new State());
        state.dropdownOpen = false;
        ui.open(player, template, view(state), options, this::handle);
    }

    private ViewModel view(State state) {
        return ViewModel.data(Map.of("enabled", state.enabled, "currency", state.currency,
                "dropdownOpen", state.dropdownOpen, "message", state.message));
    }

    private void handle(ActionContext context) {
        if (context.action().equals("close")) return; // Native exit already closed the session.
        Player player = context.player();
        if (!player.hasPermission("exampleui.preferences")) {
            context.session().close();
            return;
        }
        var state = states.computeIfAbsent(player.getUniqueId(), id -> new State());
        switch (context.action()) {
            case "toggle_notifications" -> state.enabled = !state.enabled;
            case "toggle_currency" -> state.dropdownOpen = !state.dropdownOpen;
            case "dismiss_currency" -> state.dropdownOpen = false;
            case "select_currency" -> {
                if (CURRENCIES.contains(context.value())) state.currency = context.value();
                state.dropdownOpen = false;
            }
            default -> {
                context.session().close();
                return;
            }
        }
        state.message = "Preferences updated.";
        context.session().update(template, view(state), options, this::handle);
    }

    public void forget(UUID playerId) { states.remove(playerId); }
    public void clear() { states.clear(); }
}
```

All methods run on Paper's main thread. This state survives menu closing but is deliberately forgotten on quit; it is not persistent preferences. Supply your own storage if needed. Checkbox/dropdown state never changes automatically: the handler updates it and sends a new view. Even dismissal and unchanged selections refresh the session because custom clicks consume its previous capabilities.

## Repeated rows and optional content

Page/filter your own list before rendering. Business IDs make better hit IDs than row numbers. Bindings are literal values; a title containing `&` does not become another XML element.

```xml
<dui-menu width="300" height="108" padding="9">
  <dui-column gap="9">
    <dui-if test="{{showHeading}}">
      <dui-heading label="{{heading}}" height="18" />
    </dui-if>
    <dui-repeat items="rows" as="row">
      <dui-button id="row_{{row.id}}" action="inspect" payload="{{row.id}}"
                  label="{{row.title}}" height="27" />
    </dui-repeat>
  </dui-column>
</dui-menu>
```

```java
ViewModel rows = ViewModel.data(Map.of(
    "showHeading", true,
    "heading", "Sample entries",
    "rows", List.of(
        Map.of("id", "alpha", "title", "Alpha & friends"),
        Map.of("id", "beta", "title", "Beta"))));
```

The handler receives `action="inspect"` and `value="alpha"` or `"beta"`. Look up that ID in current application state and check access there. Add a paging action/view if the list outgrows its canvas. There is no built-in scroll container, boolean-expression evaluator or bean-property binding. Omit optional content with a boolean `dui-if`; do not rely on missing/null bindings.

## Native items and runtime image keys

An item placement ID selects an ItemStack. An image's resolved `source` selects a raster. A link selects a **hit ID**. These three keys serve different purposes; they are not download URLs or automatic commands.

```xml
<dui-menu width="240" height="81" padding="9" theme="studio">
  <dui-row gap="9">
    <dui-slot id="pickaxe" width="72" label="Sample" action="inspect" />
    <dui-image id="preview" width="141" source="{{imageKey}}" pixel-size="3" />
  </dui-row>
</dui-menu>
```

```java
RasterImage placeholder = new RasterImage(1, 1, new int[] {0x5A79E5});
ViewModel media = new ViewModel(
    Map.of("imageKey", "preview"),
    Map.of("preview", placeholder),
    Map.of("pickaxe", new ItemStack(Material.DIAMOND_PICKAXE)),
    Map.of("pickaxe", URI.create("https://kembel.gg")));
```

Here the slot opens vanilla's link confirmation because its hit has an explicit URL. Use an empty links map to route `inspect` to your custom handler instead. The raster is a placeholder; replace it with a decoded server-supplied image to show downloaded content without changing the pack. Always provide a raster for every referenced source, including while loading. Images crop to fill their rectangles; their sample budget is `ceil(width/cellSize) × ceil(height/cellSize)` each, summed across the canvas. A direct `dui-item` is visual-only; use a slot/control hit for clicks.

## Asynchronous image updates

Run HTTP, bounded file reads, decoding and cache work on your application's bounded worker executor. `RasterImage.decode` checks decoded dimensions, but your loader still owns encoded-byte limits, timeouts and cache policy. ItemStack/Bukkit work and session mutations belong on the server thread. Store results in an application image cache so later views can use them even when an old view's update is discarded.

Call this helper on the server thread with an already-running worker future and the exact model/options/handler used to display the current view. It replaces only its `preview` raster. Types are from `gg.kembel.dui.core`, `gg.kembel.dui.paper`, Bukkit, `java.util.concurrent`, `java.util.HashMap` and `java.util.logging.Level`.

```java
public static void applyPreview(JavaPlugin plugin, Player player, DialogSession session,
        MenuTemplate template, ViewModel snapshot, DialogOptions options,
        ActionHandler handler, CompletableFuture<RasterImage> imageFuture) {
    long revision = session.revision();
    imageFuture.whenComplete((image, error) -> {
        if (!plugin.isEnabled()) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !session.isActive() || session.revision() != revision) return;
            if (error != null || image == null) {
                plugin.getLogger().log(Level.WARNING, "Preview image failed", error);
                return; // Keep the placeholder and existing callbacks.
            }
            var images = new HashMap<>(snapshot.images());
            images.put("preview", image);
            var updated = new ViewModel(snapshot.data(), images, snapshot.items(), snapshot.links());
            session.update(template, updated, options, handler);
        });
    });
}
```

The captured revision prevents an old response from overwriting a newer view. It does not detect Escape or another plugin's screen: vanilla sends no general close notification to this adapter. For screens where late reopening would be disruptive, use an explicit user refresh action instead of automatic delayed updates. `onClose` sets one server-known cleanup handler rather than stacking handlers; combine your cleanup in one runnable. Close your executor/services and cancel your own scheduled work on plugin shutdown.

## Native form inputs

This snippet runs inside your plugin's player command with `ui` already initialized. Import Paper's `DialogInput`, Adventure `Component`, and the dui/public collection types. The form is outside the custom canvas; it keeps vanilla's layout and submits a `DialogResponseView`. It demonstrates validation and acknowledgement, not persistence.

```java
MenuTemplate form = ui.compile("""
    <dui-menu width="280" height="45">
      <dui-column><dui-text label="Enter a sample display name." height="18" /></dui-column>
    </dui-menu>
    """);
DialogOptions options = new DialogOptions(
    Component.text("Edit sample name"),
    List.of(DialogInput.text("name", Component.text("Name"))
        .initial("Example").maxLength(24).width(250).build()),
    List.of(new DialogOptions.Button("Save", "save")),
    new DialogOptions.Button("Cancel", "cancel"), 1, false);
ui.open(player, form, ViewModel.data(Map.of()), options, context -> {
    if (!context.action().equals("save")) return; // Native exit already closes Cancel.
    var response = context.response();
    String name = response == null ? null : response.getText("name");
    if (name == null || name.isBlank() || name.length() > 24
            || name.codePoints().anyMatch(Character::isISOControl)) {
        player.sendMessage("Please enter a valid name of 1–24 characters.");
    } else {
        player.sendMessage("Saved sample: " + name.strip());
    }
    context.session().close();
});
```

Validate nulls, length, allowed choices and numeric bounds/finite values on the server even when the input widget constrains them. Re-check permission when an action commits a change. Custom canvas confirmation can use ordinary buttons; native confirmation uses `DialogOptions` with `confirmation=true` and exactly two buttons. An exit action closes before calling its handler; a normal Save/Confirm button does not close automatically.

## An animated gift assembled from reusable components

```xml
<dui-menu width="300" height="144" animation-start="{{openedAt}}" motion="{{motion}}">
  <dui-layer height="fill">
    <dui-item id="gift_body" x="9" y="36" width="72" height="72" size="72"
      transition="bounce" transition-start="{{openedAt}}" transition-duration="24" />
    <dui-item id="gift_lid" x="9" y="36" width="72" height="72" size="72"
      transition="lift" transition-start="{{openedAt}}" transition-duration="24" transition-distance="24" />
    <dui-if test="{{revealed}}">
      <dui-item id="reward" x="27" y="54" width="36" height="36" size="36"
        transition="pop" transition-start="{{revealedAt}}" transition-duration="18" />
    </dui-if>
    <dui-particles id="party" effect="confetti" width="300" height="144"
      origin-x="45" origin-y="72" count="40" delay="24" />
  </dui-layer>
</dui-menu>
```

Supply map keys `openedAt`/`revealedAt` as world game-time ticks, booleans `motion`/`revealed`, and native ItemStacks keyed `gift_body`, `gift_lid`, and `reward` when visible. The body/lid are custom decorative models registered as pack additions; the reward is a normal Minecraft ItemStack. These are independent visual layers, not extra dialog controls. Give the user a separate button/hit for opening.

Keep the selected gift and a generation counter in the consumer's session. Start once in the action handler, schedule reveal after 24 ticks, and check that the same session/generation/menu is active before updating. Preserve `openedAt` and set `revealedAt` only at reveal. Cancel on explicit close/back/replay. After the finite burst, render the scene with `motion=false` to retain the lifted lid and full-size reward without future clock-wrap replay. This is demonstration state, not a reward claim transaction. See [Advent demo](https://github.com/Kembel-Entertainment/dui-demo/blob/master/docs/advent.md) for a complete consumer.

## A clipped horizontal carousel

```xml
<dui-menu width="320" height="108" motion="{{motion}}">
  <dui-layer height="fill">
    <dui-repeat items="cards" as="card">
      <dui-item id="{{card.id}}" x="{{card.x}}" y="18" size="63"
        width="63" height="63" clip-x="68" clip-y="18" clip-width="185" clip-height="63"
        transition="{{card.transition}}" transition-start="{{startedAt}}"
        transition-duration="24" transition-distance="{{distance}}" />
    </dui-repeat>
    <dui-button id="previous" action="previous" label="&lt;" locked="{{moving}}"
      x="12" y="36" width="27" height="27" />
    <dui-button id="next" action="next" label="&gt;" locked="{{moving}}"
      x="281" y="36" width="27" height="27" />
  </dui-layer>
</dui-menu>
```

The resting strip uses three models at X=63,129,195. To slide left, advance the selected index, then send four target placements X=-3,63,129,195 with `transition="slide"`, `distance=66`, one nonnegative `startedAt` world tick, and `moving=true`. Their first frame appears at X=63,129,195,256; the outgoing and incoming cards are clipped by the fixed viewport. To slide right, decrement the selection and target X=63,129,195,256 with `distance=-66`. Wrap business indices with `Math.floorMod` and keep placement IDs unique.

The extra card is only a visual carrier. Provide an ItemStack for every placement, including the one outside the viewport. Normal Minecraft models work; custom illustrated cards require their own registered model/texture additions. Rasters passed to `dui-image` do not gain this animation.

After 24 ticks, verify the current session, section and generation, render the three resting placements with `transition=""`, and unlock the controls. Close, menu changes, mode changes and motion toggles cancel the pending task. With motion off, render the final three-card layout immediately. The server receives arrows/card clicks; Vanilla does not expose drag/swipe gestures. Use separate hits under the visible cards, disable them during transit, and keep each in the fixed canvas/9-pixel grid. No per-frame server loop is needed. See [the complete warp consumer](https://github.com/Kembel-Entertainment/dui-demo/blob/master/docs/warps.md).

## Application ownership and update rules

dui renders state and transports clicks. The consumer owns permissions, inventory/economy updates, rewards, transactions, persistence and cancellation. A label, `locked` flag, tree status or a random callback token does not prove an action is still allowed. Re-read current state when committing a change; use your storage's own idempotency/transaction rules for rewards and payments. The single-use callback protects a view revision, not a business operation across multiple views or reconnects.

Compile templates once and reuse them with new view data. Compilation checks XML/tags; binding, geometry, media keys and some component constraints are checked during render/display. Log errors with the menu name and bound data keys, and recover at your command/handler boundary. If an accepted action fails, either re-render a valid fallback or close that session: its old custom callbacks have already been consumed. The library does not roll back application mutations or automatically catch all handler errors.

An update sends a complete replacement dialog, not a browser DOM patch. Avoid sending one every game tick. Shared shader effects animate between updates; preserve the original start tick through unrelated updates and settle results on the server. Cache downloaded images and compile templates outside frequently repeated display paths. Prefer small page-sized lists, clear text/tooltips and a visible close action. Offer Compact/Spacious and motion preferences; the server cannot automatically read client display settings or accessibility preferences.

## Deployment and iteration

- Embed dui-paper and runtime dependencies; Paper/Adventure remain server-provided. There is no standalone dui plugin or automatic command registration. See quickstart for packaging and notices.
- Use the generated ZIP and **its matching** `dui.json`. The URL must be reachable from the player's computer and return the ZIP bytes. `127.0.0.1` only works for a client on the same machine; the demo's loopback pack server is for local development.
- Template/data edits normally need no pack rebuild. Changes to models, textures, fonts or transport require regeneration, updated metadata, and adapter/plugin reinitialization. `Dui` does not watch files or provide a reload command; dui-demo implements template reload in its consumer.
- WIP artifacts currently use local Maven or Gradle's composite build (`dui-demo -PduiSource=../dui`). No remote Maven release is available. Pin and record the library commit used by your plugin, build the pack from that checkout, and test upgrades together; a shared SNAPSHOT coordinate does not imply stable APIs.
- Unit-test your state/business rules and render representative views. For transport or complex-layout changes, also run dui-demo's explicit coordinate/screenshot E2E scenarios on a real client. Normal builds do not start Minecraft.

## Common failures

| Symptom / error | Check |
| --- | --- |
| `Missing binding` | Every referenced path exists in nested maps; records/beans are not traversed. |
| `Column overflow`, `Component outside canvas` | Sum heights, gaps and padding; page the list or use a larger valid canvas. |
| `Hit must use full 9 px rows` | Interactive y/height and flow gaps use 9-pixel rows. |
| Dropdown popup does not fit | Reserve `18 × optionCount` above or below the header; closed geometry alone is not enough. |
| `Missing image source` | Supply the resolved raster key, including a placeholder before a download completes. |
| `Missing native stack`, `No native model wrapper registered` | Match placement IDs to ItemStacks and generate wrappers/metadata for custom definitions. |
| No menu after pack request | Check client acceptance/status, reachable direct ZIP URL, version and matching metadata/hash. |
| Buttons work only once | Update the active session or close it after accepted custom actions, including rejected business actions. |
| Setting an attribute has no effect | It may be globally recognized but unused by this tag; check the component reference. |
| Text becomes `?` or `...` | Check supported glyphs, control characters and available GUI width; labels are fitted single lines. |
| A thumbnail reopens an escaped view | Server-known session state does not detect Escape; use explicit refresh if this is unacceptable. |

For larger complete consumers, browse [dui-demo's templates](https://github.com/Kembel-Entertainment/dui-demo/tree/master/src/main/resources/ui) and [DuiDemoPlugin](https://github.com/Kembel-Entertainment/dui-demo/blob/master/src/main/java/gg/kembel/dui/demo/DuiDemoPlugin.java). Its demo balances and checkout are examples, not a production economy/payment service.
