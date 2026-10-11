package gg.kembel.dui.minestom;

import gg.kembel.dui.runtime.DialogPresentation;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.*;

/** Lossless lowering of portable vanilla dialogs. */
public final class MinestomDialogs {
  private MinestomDialogs() {}
  public record Body(DialogBody value) implements gg.kembel.dui.runtime.DialogBody.Extension {}
  public record Input(String key, DialogInput value) implements gg.kembel.dui.runtime.DialogInput.Extension {}
  static DialogBody body(gg.kembel.dui.runtime.DialogBody body) {
    return switch (body) {
      case Body b -> b.value();
      case gg.kembel.dui.runtime.DialogBody.Text b -> new DialogBody.PlainMessage(b.contents(), b.width());
      case gg.kembel.dui.runtime.DialogBody.Item b -> new DialogBody.Item(((MinestomItem)b.item()).stack(),
          b.description() == null ? null : new DialogBody.PlainMessage(b.description().contents(), b.description().width()),
          b.decorations(), b.tooltip(), b.width(), b.height());
      default -> throw new IllegalArgumentException("Unsupported Minestom body: " + body.getClass());
    };
  }
  static DialogInput input(gg.kembel.dui.runtime.DialogInput input) {
    return switch (input) {
      case Input i -> i.value();
      case gg.kembel.dui.runtime.DialogInput.Boolean i -> new DialogInput.Boolean(i.key(), i.label(), i.initial(), i.onTrue(), i.onFalse());
      case gg.kembel.dui.runtime.DialogInput.NumberRange i -> new DialogInput.NumberRange(i.key(), i.width(), i.label(), i.labelFormat(), i.start(), i.end(), i.initial(), i.step());
      case gg.kembel.dui.runtime.DialogInput.SingleOption i -> new DialogInput.SingleOption(i.key(), i.width(), i.options().stream().map(o -> new DialogInput.SingleOption.Option(o.id(), o.display(), o.initial())).toList(), i.label(), i.labelVisible());
      case gg.kembel.dui.runtime.DialogInput.Text i -> new DialogInput.Text(i.key(), i.width(), i.label(), i.labelVisible(), i.initial(), i.maxLength(),
          i.multiline() == null ? null : new DialogInput.Text.Multiline(i.multiline().maxLines(), i.multiline().height()));
      default -> throw new IllegalArgumentException("Unsupported Minestom input: " + input.getClass());
    };
  }
  private static DialogActionButton button(DialogPresentation.Button b) {
    return b == null ? null : new DialogActionButton(Component.text(b.definition().label()), null, b.definition().width(), new DialogAction.DynamicCustom(b.action(), null));
  }
  public static Dialog lower(DialogPresentation presentation) {
    var o = presentation.options();
    var metadata = new DialogMetadata(o.title(), null, true, false, DialogAfterAction.NONE,
        presentation.body().stream().map(MinestomDialogs::body).toList(), o.inputs().stream().map(MinestomDialogs::input).toList());
    var buttons = presentation.buttons().stream().map(MinestomDialogs::button).toList();
    var exit = button(presentation.exit());
    return o.confirmation() ? new Dialog.Confirmation(metadata, buttons.get(0), buttons.get(1))
        : buttons.isEmpty() ? new Dialog.Notice(metadata, exit) : new Dialog.MultiAction(metadata, buttons, exit, o.columns());
  }
}
