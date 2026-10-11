package gg.kembel.dui.runtime;

import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;

/** Portable vanilla input definitions, preserving all supported options. */
public sealed interface DialogInput {
  String key();
  static void validate(String key, Component label, int width) {
    if (key == null || !key.matches("[a-zA-Z0-9_]+")) throw new IllegalArgumentException("Input key");
    Objects.requireNonNull(label);
    if (width < 1 || width > 1024) throw new IllegalArgumentException("Input width");
  }
  record Boolean(String key, Component label, boolean initial, String onTrue, String onFalse) implements DialogInput {
    public Boolean { validate(key, label, 200); Objects.requireNonNull(onTrue); Objects.requireNonNull(onFalse); }
  }
  record NumberRange(String key, int width, Component label, String labelFormat,
                     float start, float end, Float initial, Float step) implements DialogInput {
    public NumberRange {
      validate(key, label, width); Objects.requireNonNull(labelFormat);
      if (!Float.isFinite(start) || !Float.isFinite(end) || start > end
          || initial != null && (!Float.isFinite(initial) || initial < start || initial > end)
          || step != null && (!Float.isFinite(step) || step <= 0)) throw new IllegalArgumentException("Input range");
    }
  }
  record SingleOption(String key, int width, List<Option> options, Component label, boolean labelVisible) implements DialogInput {
    public SingleOption {
      validate(key, label, width); options = List.copyOf(options);
      if (options.isEmpty() || options.stream().filter(Option::initial).count() > 1
          || options.stream().map(Option::id).distinct().count() != options.size()) throw new IllegalArgumentException("Input options");
    }
    public record Option(String id, Component display, boolean initial) {
      public Option { if (id == null || id.isBlank()) throw new IllegalArgumentException("Option id"); }
    }
  }
  record Text(String key, int width, Component label, boolean labelVisible,
              String initial, int maxLength, Multiline multiline) implements DialogInput {
    public Text {
      validate(key, label, width); Objects.requireNonNull(initial);
      if (maxLength < 1 || initial.length() > maxLength) throw new IllegalArgumentException("Input length");
    }
    public record Multiline(Integer maxLines, Integer height) {
      public Multiline { if (maxLines != null && maxLines < 1 || height != null && height < 1) throw new IllegalArgumentException("Multiline size"); }
    }
  }
  non-sealed interface Extension extends DialogInput {}
}
