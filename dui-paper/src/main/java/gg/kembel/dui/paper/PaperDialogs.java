package gg.kembel.dui.paper;

import gg.kembel.dui.runtime.DialogPresentation;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;

/** Native extensions preserve the existing Paper API; portable bodies and inputs work on both adapters. */
final class PaperDialogs {
  record Input(DialogInput value) implements gg.kembel.dui.runtime.DialogInput.Extension { public String key() { return value.key(); } }
  record Body(DialogBody value) implements gg.kembel.dui.runtime.DialogBody.Extension {}
  static DialogBody body(gg.kembel.dui.runtime.DialogBody body) {
    return switch (body) {
      case Body b -> b.value();
      case gg.kembel.dui.runtime.DialogBody.Text b -> DialogBody.plainMessage(b.contents(), b.width());
      case gg.kembel.dui.runtime.DialogBody.Item b -> DialogBody.item(((PaperItem)b.item()).stack(),
          b.description() == null ? null : DialogBody.plainMessage(b.description().contents(), b.description().width()),
          b.decorations(), b.tooltip(), b.width(), b.height());
      default -> throw new IllegalArgumentException("Unsupported Paper body: " + body.getClass());
    };
  }
  static DialogInput input(gg.kembel.dui.runtime.DialogInput input) {
    return switch (input) {
      case Input i -> i.value();
      case gg.kembel.dui.runtime.DialogInput.Boolean i -> DialogInput.bool(i.key(), i.label(), i.initial(), i.onTrue(), i.onFalse());
      case gg.kembel.dui.runtime.DialogInput.NumberRange i -> DialogInput.numberRange(i.key(), i.width(), i.label(), i.labelFormat(), i.start(), i.end(), i.initial(), i.step());
      case gg.kembel.dui.runtime.DialogInput.SingleOption i -> DialogInput.singleOption(i.key(), i.width(), i.options().stream().map(o -> io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput.OptionEntry.create(o.id(), o.display(), o.initial())).toList(), i.label(), i.labelVisible());
      case gg.kembel.dui.runtime.DialogInput.Text i -> DialogInput.text(i.key(), i.width(), i.label(), i.labelVisible(), i.initial(), i.maxLength(), i.multiline() == null ? null : io.papermc.paper.registry.data.dialog.input.TextDialogInput.MultilineOptions.create(i.multiline().maxLines(), i.multiline().height()));
      default -> throw new IllegalArgumentException("Unsupported Paper input: " + input.getClass());
    };
  }
  static ActionButton button(DialogPresentation.Button b) {
    return b == null ? null : ActionButton.create(Component.text(b.definition().label()), null, b.definition().width(), DialogAction.customClick(b.action(), null));
  }
  static Dialog lower(DialogPresentation presentation) {
    var o = presentation.options(); var buttons = presentation.buttons().stream().map(PaperDialogs::button).toList(); var exit = button(presentation.exit());
    return Dialog.create(builder -> {
      builder.empty().base(DialogBase.builder(o.title()).pause(false).afterAction(DialogBase.DialogAfterAction.NONE)
          .body(presentation.body().stream().map(PaperDialogs::body).toList()).inputs(o.inputs().stream().map(PaperDialogs::input).toList()).build())
          .type(o.confirmation() ? DialogType.confirmation(buttons.get(0), buttons.get(1)) : buttons.isEmpty() ? DialogType.notice(exit) : DialogType.multiAction(buttons, exit, o.columns()));
    });
  }
}
