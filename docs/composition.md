# Component composition and UI work

These APIs are implemented foundation features. See [application APIs](application-api.md) for the implemented controller, scene, resource and renderer layers. Existing MenuTemplate.parse and DialogSession.update calls continue to work. No resource-pack regeneration is required for a component that only emits existing primitives.

## Local template components

Declare a component as a direct child of dui-menu. Its name must have an owner prefix separated by a hyphen, and props lists its required properties. A definition has one visual root and may contain direct local styles before that root.

```xml
<dui-menu width="240" height="36">
  <dui-component name="acme-entry" props="entry">
    <dui-style id="ink" color="#FFFFFF" />
    <dui-layer height="18">
      <dui-text class="ink" width="fill-30" height="18"
                label="{{props.entry.label}}" />
      <dui-button id="choose_{{props.entry.id}}" action="choose"
                  payload="{{props.entry.id}}" label="+" width="24" height="18"
                  anchor-x="right" />
    </dui-layer>
  </dui-component>
  <dui-column>
    <dui-repeat items="entries" as="entry">
      <dui-acme-entry entry="{{entry}}" />
    </dui-repeat>
  </dui-column>
</dui-menu>
```

An exact property binding such as entry="{{entry}}" preserves its map/list value. Ordinary label bindings stay literal text; they are not parsed as markup. Data comes from read-only maps, not reflective access to game objects. Publish only information that may reach the client.

Supply stable IDs explicitly, normally using the component's item key. Placement attributes id, x, y, width, height, anchor-x and anchor-y on the component invocation override its visual root's placement attributes. Invocation classes override the visual root's resolved style properties. An invocation ID is also available as props.id for internal controls. No automatic prefixing changes existing action IDs or payloads.

## External component registry

Share components across templates with an immutable registry snapshot:

```java
var registry = ComponentRegistry.builder()
    .template("acme-title", Set.of("text"), """
        <dui-fragment>
          <dui-text label="{{props.text}}" color="#FFFFFF" />
        </dui-fragment>
        """)
    .renderer("acme-status", Set.of("message"), 18, context -> {
      context.canvas().text(context.x(), context.y(), context.width(),
          context.node().s("message", ""), 0xFFFFFF);
    })
    .build();

var template = dui.compile("ui/example.html", source, registry);
```

Use dui-acme-title or dui-acme-status in a template. Custom renderer properties are declared by that renderer; template component properties are required at each invocation. A custom renderer receives absolute allocated bounds, its node, image resources, child drawing and deferred overlays. Honour bounds, hit-row rules, unique IDs and resource limits exactly as when using Canvas directly. Custom renderers are caller-owned code; dui's expansion limits do not sandbox arbitrary Java renderer work. Keep renderers stateless or externally thread-confined.

Registry builders cannot replace existing names. Later builder mutations do not affect previously built snapshots or compiled templates. Templates retain bounded expansion and recursion. A registry does not install a new GPU opcode, arbitrary texture resource or shader; those require a supported pack extension.

## Named content

An external fragment or local definition can contain dui-outlet name="footer". Fill it at the call site using dui-content name="footer". Regular call-site children fill the default outlet.

```xml
<dui-component name="acme-box" props="title">
  <dui-column>
    <dui-text label="{{props.title}}" />
    <dui-outlet name="footer" />
  </dui-column>
</dui-component>

<dui-acme-box title="Overview">
  <dui-content name="footer">
    <dui-text label="{{footerText}}" />
  </dui-content>
</dui-acme-box>
```

Content bindings use the caller's data. Component properties use props. A referenced named outlet must be supplied; duplicates are rejected. Local style IDs are scoped to their component; literal class names resolve to those local styles. Parent styles remain available for intentional shared classes. Dynamic local style export and optional/default property declarations are not implemented.

## Relative positioned geometry and text

Inside dui-layer, width and height accept integer pixels, fill, a percentage, or a pixel subtraction such as fill-12 or 50%-6. Relative dimensions use the allocated parent extent minus the corresponding x/y inset. Anchors use that inset from the selected edge: anchor-x="left|center|right", anchor-y="top|center|bottom". Centered vertical anchoring uses the nine-pixel row grid; authored positioned text may still use pixel offsets. Bounds and hit alignment checks still apply.

dui-text supports align="left|center|right". With wrap="true", text starts at the top of its allocated box, wraps using its actual injected font metrics and fits at most max-lines and height/9 lines. Whitespace is normalized. The final line is truncated with a fitting ellipsis. Single-line text retains its existing vertical centering. This is a bounded layout language, not a CSS browser.

## Pagination and budgets

```java
var page = Page.of(entries, requestedPage, 3);
// page.items(), index(), pages(), total(), hasPrevious(), hasNext()

var slots = RenderBudget.allocate(8,
    new RenderBudget.Request("secondary", secondaryCount, 0),
    new RenderBudget.Request("required", requiredCount, Math.min(4, requiredCount)));
```

Pages clamp the requested index. Empty collections have index zero and one page. Resource allocation first reserves every minimum, then gives extra capacity to requests in their listed order. Minimums exceeding capacity fail with group diagnostics. The consumer decides how to page or otherwise represent demand beyond its allocation; allocation never silently renders missing content.

RenderReport.of(canvas) reports geometry, paints, hits, heads, native carriers, effects and sampled image pixels. Native-item counts include the internal effect carrier. Remaining capacities refer to the canvas effect budget (eight by default, up to 32 explicitly) and 16384 sampled image pixels. These counts do not measure packet bytes, server time or FPS.

## Session tasks

```java
session.viewTasks().later("animation-end", remainingTicks, () -> {
  // End the current presentation and update the view.
});

session.tasks().latest("thumbnail-load", providerFuture, (value, error) -> {
  // Already back on the server thread. Apply ready/fallback state and rerender.
});
```

View tasks expire whenever DialogSession.update or close runs. Session tasks survive updates, expire on close, and replace previous work with the same key. Switches between application sections within one DialogSession must cancel jobs belonging to the section being left. The demo explicitly cancels video-fetch when a command changes its section.

Closing here means a server-known close, replacement, quit or adapter shutdown. Task scopes do not add a general vanilla Escape/screen-visibility notification; see [lifecycle limits](llm-guide.md#public-api).

Call task-scope APIs on the UI owner thread. Async providers may complete on any thread; callbacks are dispatched through the platform scheduler. A completion that lost its key, scope or validity guard cannot run its handler. Closing a scope does not cancel the provider's shared future. Handle provider errors explicitly.

These scopes are for presentation work. Persistent transactions, stake reservation and exactly-once payouts must have an application-owned lifetime and must survive closing the UI. The demo keeps its slot settlement job outside the scope.

## Timing plans

```java
var opening = AnimationTimeline.sequence(
    AnimationTimeline.of("open", 24),
    AnimationTimeline.parallel(
        AnimationTimeline.of("confetti", 72),
        AnimationTimeline.of("reward", 48)));
long finishAt = opening.duration();
long remaining = opening.remaining(startedAt, currentWorldTick);
```

IDs are unique across a plan. Segments expose start, duration and end; delayed plans retain explicit offsets. Reuse the original start tick through rerenders. The timeline is pure timing data: it does not emit property tracks, schedule frames or invoke game decisions. Existing item/effect parameters still perform the actual client-rendered motion. Schedule only needed phase boundaries through the session, and keep gameplay decisions in the controller.

## Compatibility and verification

The migration now uses versioned protocol 2, generated codecs, native/effect Motion tracks and bounded effect batches. Existing low-level APIs and legacy aliases remain available. See [application APIs](application-api.md) and [compatibility](compatibility.md) for exact contracts and fixed-backend scene limits.

Unit tests cover custom registry schemas, immutable snapshots, recursive fragments, literal/typed bindings, caller content, local styles, relative bounds, font metrics, pagination, resource minimums, timing and stale jobs. The companion demo uses the same APIs and retains its gameplay tests and muted real-client scenarios.

## Combining component packages

Use `ComponentRegistry.builder().include(existingRegistry)` before adding consumer templates or renderers. Duplicate names fail immediately; each compiled template keeps an immutable snapshot. Install the optional `VisualComponents.registry()` for `dui-visual-*` and `dui-chrome-*` components.
