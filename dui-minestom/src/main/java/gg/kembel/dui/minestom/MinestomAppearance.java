package gg.kembel.dui.minestom;

import gg.kembel.dui.runtime.PlayerAppearance;
import java.util.List;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;

/** Captures native profile and armor without profile lookup or I/O. */
public final class MinestomAppearance {
  private MinestomAppearance() {}
  public static PlayerAppearance capture(Player player) {
    var viewer = new MinestomViewer(player); viewer.checkThread();
    boolean slim = false;
    if (player.getSkin() != null) {
      try {
        var json = com.google.gson.JsonParser.parseString(new String(java.util.Base64.getDecoder().decode(player.getSkin().textures()), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        var texture = json.getAsJsonObject("textures").getAsJsonObject("SKIN");
        slim = texture.has("metadata") && texture.getAsJsonObject("metadata").has("model")
            && texture.getAsJsonObject("metadata").get("model").getAsString().equals("slim");
      } catch (IllegalArgumentException | IllegalStateException | NullPointerException ignored) { }
    }
    return new PlayerAppearance(viewer.head(), slim, List.of(MinestomItem.of(player.getEquipment(EquipmentSlot.HELMET)),
        MinestomItem.of(player.getEquipment(EquipmentSlot.CHESTPLATE)), MinestomItem.of(player.getEquipment(EquipmentSlot.LEGGINGS)), MinestomItem.of(player.getEquipment(EquipmentSlot.BOOTS))));
  }
}
