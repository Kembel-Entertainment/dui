package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class DialogSession {
  final Dui owner;
  final Player player;
  long revision;
  boolean active = true;
  Runnable closed = () -> {};
  Canvas canvas;
  ViewModel model;
  DialogOptions options;
  ActionHandler handler;
  Component component;

  DialogSession(Dui owner, Player player) {
    this.owner = owner;
    this.player = player;
  }

  public boolean isActive() {
    return active;
  }

  public long revision() {
    return revision;
  }

  public Canvas canvas() {
    return canvas;
  }

  public Component component() {
    return component;
  }

  public DialogSession onClose(Runnable handler) {
    owner.mainThread();
    this.closed = java.util.Objects.requireNonNull(handler);
    return this;
  }

  public void update(
      MenuTemplate template, ViewModel model, DialogOptions options, ActionHandler handler) {
    update(template.render(model.data(), model.images()), model, options, handler);
  }

  public void update(Canvas canvas, ViewModel model, DialogOptions options, ActionHandler handler) {
    owner.mainThread();
    if (!active) throw new IllegalStateException("Dialog session is closed");
    this.canvas = canvas;
    this.model = model;
    this.options = options;
    this.handler = handler;
    revision++;
    owner.display(this);
  }

  public void close() {
    owner.mainThread();
    owner.dismiss(this, true);
  }
}
