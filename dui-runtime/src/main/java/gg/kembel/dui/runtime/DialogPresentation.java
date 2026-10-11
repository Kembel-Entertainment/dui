package gg.kembel.dui.runtime;

import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Validated rendering result consumed by both adapters. */
public record DialogPresentation(DialogOptions options, List<DialogBody> body,
                                 List<Button> buttons, Button exit) {
  public record Button(DialogOptions.Button definition, Key action) {
    public Button { Objects.requireNonNull(definition); Objects.requireNonNull(action); }
  }
  public DialogPresentation { Objects.requireNonNull(options); body = List.copyOf(body); buttons = List.copyOf(buttons); }
}
