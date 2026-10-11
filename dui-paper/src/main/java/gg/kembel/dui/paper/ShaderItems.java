package gg.kembel.dui.paper;
import gg.kembel.dui.core.*;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import java.util.*;
import org.bukkit.inventory.ItemStack;

/** Compatibility entry point; carrier generation is shared across platforms. */
public final class ShaderItems {
  private ShaderItems() {}
  public static List<DialogBody> bodies(Canvas canvas, Map<String, ItemStack> stacks, Set<String> models) { return bodies(canvas, stacks, models, true); }
  public static List<DialogBody> bodies(Canvas canvas, Map<String, ItemStack> stacks, Set<String> models, boolean motionTracks) { return bodies(canvas, stacks, models, motionTracks, Map.of(), Map.of()); }
  public static List<DialogBody> bodies(Canvas canvas, Map<String, ItemStack> stacks, Set<String> models, boolean motionTracks,
      Map<String, PlayerAppearance> appearances, Map<String, ShaderBinding> shaders) {
    var resources = new ViewModel(Map.of(), Map.of(), stacks, Map.of(), appearances).portable();
    return gg.kembel.dui.runtime.ShaderItems.bodies(canvas, resources, models, shaders,
        key -> PaperItem.of(new ItemStack(org.bukkit.Material.matchMaterial(key.asString())))).stream().map(PaperDialogs::body).toList();
  }
}
