package gg.kembel.dui.paper;

import java.util.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.profile.PlayerProfile;

/** Captured on the server thread. No profile lookup, inventory mutation or I/O while projecting. */
public final class PlayerAppearance {
  private final PlayerProfile profile;
  private final List<ItemStack> armor;
  private final net.kyori.adventure.key.Key texture;
  private final boolean slimResource;

  public PlayerAppearance(PlayerProfile profile, List<ItemStack> armor) {
    this(profile, armor, null, false);
  }

  private PlayerAppearance(
      PlayerProfile profile,
      List<ItemStack> armor,
      net.kyori.adventure.key.Key texture,
      boolean slimResource) {
    this.texture = texture;
    this.slimResource = slimResource;
    if (armor.size() != 4)
      throw new IllegalArgumentException("Armor order: head, chest, legs, feet");
    this.profile = profile == null ? null : profile.clone();
    this.armor = armor.stream().map(s -> s == null ? null : s.clone()).toList();
  }

  public static PlayerAppearance capture(Player player) {
    var i = player.getInventory();
    return new PlayerAppearance(
        player.getPlayerProfile(),
        Arrays.asList(i.getHelmet(), i.getChestplate(), i.getLeggings(), i.getBoots()));
  }

  /** A texture resource already available to the client, not a skin download URL. */
  public static PlayerAppearance skinResource(
      String resource, boolean slim, List<ItemStack> armor) {
    return new PlayerAppearance(null, armor, net.kyori.adventure.key.Key.key(resource), slim);
  }

  public net.kyori.adventure.key.Key texture() {
    return texture == null
        ? net.kyori.adventure.key.Key.key("minecraft:entity/player/wide/steve")
        : texture;
  }

  public PlayerProfile profile() {
    return profile == null ? null : profile.clone();
  }

  public List<ItemStack> armor() {
    return armor.stream().map(s -> s == null ? null : s.clone()).toList();
  }

  public boolean slim() {
    return slimResource
        || profile != null
            && profile.getTextures().getSkinModel()
                == org.bukkit.profile.PlayerTextures.SkinModel.SLIM;
  }

  public boolean fallback() {
    return texture == null && (profile == null || profile.getTextures().getSkin() == null);
  }
}
