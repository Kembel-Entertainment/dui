package gg.kembel.dui.runtime;

import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import net.kyori.adventure.text.Component;

public record DialogOptions(Component title, List<DialogInput> inputs, List<Button> buttons,
                            Button exit, int columns, boolean confirmation) {
  public record Button(String label, String action, int width) {
    public Button { Objects.requireNonNull(label); if (action == null || action.isBlank() || width < 1 || width > 1024) throw new IllegalArgumentException("Invalid dialog button"); }
    public Button(String label, String action) { this(label, action, 150); }
  }
  public DialogOptions {
    Objects.requireNonNull(title); inputs = List.copyOf(inputs); buttons = List.copyOf(buttons);
    if (columns < 1 || confirmation && buttons.size() != 2 || !confirmation && buttons.isEmpty() && exit == null)
      throw new IllegalArgumentException("Invalid dialog options");
    var keys = new HashSet<String>();
    for (var input : inputs) if (!keys.add(input.key())) throw new IllegalArgumentException("Duplicate input key: " + input.key());
  }
  public static DialogOptions notice(String title, String closeLabel, String closeAction) {
    return new DialogOptions(Component.text(title), List.of(), List.of(), new Button(closeLabel, closeAction), 1, false);
  }
}
