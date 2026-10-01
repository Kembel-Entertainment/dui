package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.*;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.*;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

/** Embedded Paper adapter. It registers no commands and owns no application state. */
public final class Dui implements Listener, AutoCloseable {
  private final JavaPlugin plugin;
  private final PackDescriptor pack;
  private final PackMetadata metadata;
  private final String instance = UUID.randomUUID().toString();
  private final CanvasRenderer renderer = new CanvasRenderer();
  private final CallbackRegistry<Consumer<DialogResponseView>> callbacks = new CallbackRegistry<>();
  private final Map<UUID, DialogSession> sessions = new HashMap<>();
  private final Set<UUID> ready = new HashSet<>(), offered = new HashSet<>();
  private boolean stopped;

  private Dui(JavaPlugin plugin, PackDescriptor pack, PackMetadata metadata) {
    this.plugin = Objects.requireNonNull(plugin);
    this.pack = Objects.requireNonNull(pack);
    this.metadata = Objects.requireNonNull(metadata);
    if (!pack.sha1().equals(metadata.sha1()))
      throw new IllegalArgumentException("Pack descriptor and metadata have different hashes");
    mainThread();
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  public static Dui create(JavaPlugin plugin, PackDescriptor pack, PackMetadata metadata) {
    return new Dui(plugin, pack, metadata);
  }

  void mainThread() {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("dui Paper API must run on the server thread");
    if (stopped) throw new IllegalStateException("dui is closed");
  }

  public MenuTemplate compile(String source) throws Exception {
    return compile("<template>", source, ComponentRegistry.EMPTY);
  }

  public MenuTemplate compile(String name, String source, ComponentRegistry registry)
      throws Exception {
    return MenuTemplate.parse(source, metadata.font(), registry, name);
  }

  UiScheduler scheduler() {
    return new UiScheduler() {
      public Cancellation later(long ticks, Runnable task) {
        mainThread();
        var scheduled = plugin.getServer().getScheduler().runTaskLater(plugin, task, ticks);
        return scheduled::cancel;
      }

      public void execute(Runnable task) {
        // May be called by a provider thread after shutdown; dropping UI work is intentional.
        if (!plugin.isEnabled()) return;
        try {
          plugin
              .getServer()
              .getScheduler()
              .runTask(
                  plugin,
                  () -> {
                    if (!stopped) task.run();
                  });
        } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) {
          // Disable can race with enqueueing a completion. No provider is cancelled.
        }
      }
    };
  }

  public boolean packLoaded(Player player) {
    return ready.contains(player.getUniqueId());
  }

  public void offerPack(Player player) {
    mainThread();
    if (offered.add(player.getUniqueId()))
      player.addResourcePack(
          pack.id(),
          pack.uri().toString(),
          HexFormat.of().parseHex(pack.sha1()),
          "dui / Vanilla UI",
          pack.required());
  }

  public DialogSession open(
      Player player, MenuTemplate template, ViewModel model, ActionHandler handler) {
    return open(player, template, model, DialogOptions.notice("dui", "Close", "close"), handler);
  }

  public DialogSession open(
      Player player,
      MenuTemplate template,
      ViewModel model,
      DialogOptions options,
      ActionHandler handler) {
    return open(player, template.render(model.data(), model.images()), model, options, handler);
  }

  public DialogSession open(
      Player player, Canvas canvas, ViewModel model, DialogOptions options, ActionHandler handler) {
    mainThread();
    var previous = sessions.get(player.getUniqueId());
    if (previous != null) dismiss(previous, false);
    var session = new DialogSession(this, player);
    sessions.put(player.getUniqueId(), session);
    session.update(canvas, model, options, handler);
    return session;
  }

  private Key action(DialogSession session, Canvas.Hit hit, boolean closes) {
    long revision = session.revision;
    var token =
        callbacks.register(
            session.player.getUniqueId(),
            response -> {
              if (!session.active
                  || session.revision != revision
                  || sessions.get(session.player.getUniqueId()) != session) return;
              if (closes) dismiss(session, true);
              session.handler.handle(new ActionContext(session.player, session, hit, response));
            });
    return Key.key("dui", instance + "/" + token);
  }

  private ActionButton button(DialogSession s, DialogOptions.Button button, boolean closes) {
    var hit = new Canvas.Hit(button.action(), button.action(), "", button.label(), 0, 0, 1, 9);
    return ActionButton.create(
        Component.text(button.label()),
        null,
        button.width(),
        DialogAction.customClick(action(s, hit, closes), null));
  }

  void display(DialogSession s) {
    metadata.validate(s.canvas);
    callbacks.invalidate(s.player.getUniqueId());
    if (!ready.contains(s.player.getUniqueId())) {
      offerPack(s.player);
      return;
    }
    long renderStarted = System.nanoTime();
    var c = s.canvas;
    var model = s.model;
    var itemSnapshots = model.items();
    s.component =
        renderer.renderActions(
            c,
            hit ->
                model.links().containsKey(hit.id())
                    ? ClickEvent.openUrl(model.links().get(hit.id()).toString())
                    : ClickEvent.custom(action(s, hit, false), null),
            head -> NativeHeads.render(head, s.player),
            hit ->
                itemSnapshots.containsKey(hit.id())
                    ? itemSnapshots.get(hit.id()).asHoverEvent()
                    : HoverEvent.showText(
                        Component.text(hit.tooltip().isBlank() ? hit.id() : hit.tooltip())));
    var body = new ArrayList<DialogBody>();
    body.add(DialogBody.plainMessage(s.component, c.width + 12));
    body.addAll(
        ShaderItems.bodies(
            c, itemSnapshots, metadata.models(), metadata.supports("motion-tracks")));
    var o = s.options;
    var buttons = o.buttons().stream().map(b -> button(s, b, false)).toList();
    var exit = o.exit() == null ? null : button(s, o.exit(), true);
    s.player.showDialog(
        Dialog.create(
            builder -> {
              var base =
                  DialogBase.builder(o.title())
                      .pause(false)
                      .afterAction(DialogBase.DialogAfterAction.NONE)
                      .body(body)
                      .inputs(o.inputs())
                      .build();
              var empty = builder.empty().base(base);
              if (o.confirmation())
                empty.type(DialogType.confirmation(buttons.get(0), buttons.get(1)));
              else if (buttons.isEmpty()) empty.type(DialogType.notice(exit));
              else
                empty.type(
                    DialogType.multiAction(buttons).columns(o.columns()).exitAction(exit).build());
            }));
    s.renderNanos = System.nanoTime() - renderStarted;
    s.bodyCount = body.size();
  }

  void dismiss(DialogSession s, boolean closeScreen) {
    if (!s.active) return;
    s.active = false;
    s.revision++;
    s.disposeTasks();
    callbacks.invalidate(s.player.getUniqueId());
    sessions.remove(s.player.getUniqueId(), s);
    if (closeScreen && s.player.isOnline()) s.player.closeDialog();
    s.closed.run();
  }

  @EventHandler
  public void resourcePack(PlayerResourcePackStatusEvent event) {
    if (!event.getID().equals(pack.id())) return;
    var id = event.getPlayer().getUniqueId();
    if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
      ready.add(id);
      var session = sessions.get(id);
      if (session != null && session.active) display(session);
    } else if (Set.of("DECLINED", "FAILED_DOWNLOAD", "FAILED_RELOAD", "INVALID_URL", "DISCARDED")
        .contains(event.getStatus().name())) {
      ready.remove(id);
      offered.remove(id);
      var s = sessions.get(id);
      if (s != null) dismiss(s, false);
      event
          .getPlayer()
          .sendMessage(
              Component.text(
                  "This UI requires the dui resource pack. Run the menu command to try again."));
    }
  }

  @EventHandler
  public void click(PlayerCustomClickEvent event) {
    if (!event.getIdentifier().namespace().equals("dui")
        || !event.getIdentifier().value().startsWith(instance + "/")
        || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
    UUID token;
    try {
      token = UUID.fromString(event.getIdentifier().value().substring(instance.length() + 1));
    } catch (IllegalArgumentException e) {
      return;
    }
    var callback = callbacks.consume(connection.getPlayer().getUniqueId(), token);
    if (callback != null)
      plugin
          .getServer()
          .getScheduler()
          .runTask(
              plugin,
              () -> {
                if (!stopped && connection.getPlayer().isOnline())
                  callback.accept(event.getDialogResponseView());
              });
  }

  @EventHandler
  public void quit(PlayerQuitEvent event) {
    var id = event.getPlayer().getUniqueId();
    var s = sessions.get(id);
    if (s != null) dismiss(s, false);
    ready.remove(id);
    offered.remove(id);
    callbacks.invalidate(id);
  }

  @Override
  public void close() {
    if (stopped) return;
    mainThread();
    for (var s : List.copyOf(sessions.values())) dismiss(s, true);
    HandlerList.unregisterAll(this);
    callbacks.clear();
    ready.clear();
    offered.clear();
    stopped = true;
  }
}
