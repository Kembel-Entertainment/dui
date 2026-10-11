package gg.kembel.dui.runtime;

import java.util.Objects;
import net.kyori.adventure.text.Component;

/** Bodies before lowering to the selected platform. */
public sealed interface DialogBody {
  record Text(Component contents, int width) implements DialogBody {
    public Text { Objects.requireNonNull(contents); if (width < 1 || width > 1024) throw new IllegalArgumentException("Body width"); }
  }
  record Item(NativeItem item, Text description, boolean decorations, boolean tooltip, int width, int height) implements DialogBody {
    public Item { Objects.requireNonNull(item); if (width < 1 || width > 1024 || height < 1 || height > 1024) throw new IllegalArgumentException("Body size"); }
    public Item(NativeItem item) { this(item, null, false, false, 1, 1); }
  }
  /** Trusted adapter extensions can retain a native body without exposing platform types here. */
  non-sealed interface Extension extends DialogBody {}
}
