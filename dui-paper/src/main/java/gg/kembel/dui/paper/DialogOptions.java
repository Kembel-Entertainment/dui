package gg.kembel.dui.paper;

import io.papermc.paper.registry.data.dialog.input.DialogInput;
import java.util.*;
import net.kyori.adventure.text.Component;

public record DialogOptions(
    Component title,
    List<DialogInput> inputs,
    List<Button> buttons,
    Button exit,
    int columns,
    boolean confirmation) {
  public record Button(String label, String action, int width) {
    public Button {
      if (action == null || action.isBlank() || width < 1 || width > 1024)
        throw new IllegalArgumentException("Invalid dialog button");
    }

    public Button(String label, String action) {
      this(label, action, 150);
    }
  }

  public DialogOptions {
    Objects.requireNonNull(title);
    inputs = List.copyOf(inputs);
    buttons = List.copyOf(buttons);
    if (columns < 1
        || confirmation && buttons.size() != 2
        || !confirmation && buttons.isEmpty() && exit == null)
      throw new IllegalArgumentException("Invalid dialog options");
  }

  public static DialogOptions notice(String title, String closeLabel, String closeAction) {
    return new DialogOptions(
        Component.text(title), List.of(), List.of(), new Button(closeLabel, closeAction), 1, false);
  }

  public gg.kembel.dui.runtime.DialogOptions portable() {
    return new gg.kembel.dui.runtime.DialogOptions(title,
        inputs.stream().map(i -> (gg.kembel.dui.runtime.DialogInput) new PaperDialogs.Input(i)).toList(),
        buttons.stream().map(DialogOptions::portableButton).toList(),
        exit == null ? null : portableButton(exit), columns, confirmation);
  }
  private static gg.kembel.dui.runtime.DialogOptions.Button portableButton(Button b) {
    return new gg.kembel.dui.runtime.DialogOptions.Button(b.label(), b.action(), b.width());
  }
}
