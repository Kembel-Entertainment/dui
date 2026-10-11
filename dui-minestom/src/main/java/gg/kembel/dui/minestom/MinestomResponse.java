package gg.kembel.dui.minestom;

import gg.kembel.dui.runtime.DialogResponse;
import net.kyori.adventure.nbt.*;

/** Vanilla dynamic/custom replies carry text and numeric NBT; boolean inputs can emit strings. */
public record MinestomResponse(CompoundBinaryTag payload) implements DialogResponse {
  public MinestomResponse { if (payload == null) payload = CompoundBinaryTag.empty(); }
  public static MinestomResponse of(BinaryTag payload) { return new MinestomResponse(payload instanceof CompoundBinaryTag compound ? compound : null); }
  public String text(String key) { return payload.get(key) instanceof StringBinaryTag tag ? tag.value() : null; }
  public Float number(String key) { return payload.get(key) instanceof NumberBinaryTag tag ? tag.floatValue() : null; }
  public Boolean bool(String key) {
    return switch (payload.get(key)) {
      case ByteBinaryTag tag -> tag.value() != 0;
      case StringBinaryTag tag when tag.value().equals("true") -> true;
      case StringBinaryTag tag when tag.value().equals("false") -> false;
      case null, default -> null;
    };
  }
}
