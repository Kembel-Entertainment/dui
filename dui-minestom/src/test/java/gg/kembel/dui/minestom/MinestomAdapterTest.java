package gg.kembel.dui.minestom;

import static org.junit.jupiter.api.Assertions.*;
import gg.kembel.dui.core.*;
import gg.kembel.dui.core.world.*;
import gg.kembel.dui.runtime.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.*;
import net.minestom.server.item.*;
import net.minestom.server.item.component.CustomModelData;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.common.*;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.testing.*;
import org.junit.jupiter.api.Test;

@EnvTest
class MinestomAdapterTest {
  private PackMetadata metadata(boolean world) {
    var caps = new HashSet<>(Set.of("native", "shader-components-v1", "clips", "motion-tracks", "effects-32"));
    Map<String, WorldMapMetadata> maps = Map.of();
    if (world) {
      caps.add(WorldMapProtocol.CAPABILITY);
      var definition = new WorldMapDefinition("fixture:map", 600, 300, 5, 100, 10, 3, 12,
          List.of(new WorldMapDefinition.Layer("layer", "image", 0, 0, 600, 300, WorldMapDefinition.Space.MAP, .9, 1, 1, 0)),
          new WorldMapDefinition.Opening(12, .9, 0, 1, Motion.Easing.LINEAR));
      maps = Map.of(definition.id(), new WorldMapMetadata(definition, "dui:fixture", Map.of("image", new WorldMapMetadata.Glyph(0xE000, 1)), definition.geometryHash(), WorldMapProtocol.SHA256));
    }
    return new PackMetadata(MinecraftTarget.VERSION, MinecraftTarget.LIBRARY_VERSION, "a".repeat(40), Set.of("minecraft:paper"),
        Map.of("?", 6), RendererProtocol.VERSION, caps, RendererProtocol.SCHEMA_SHA256, maps, world ? Map.of("?", 6) : Map.of(), Map.of(), Map.of());
  }
  @Test void nativeItemsRetainComponentsAndCustomData(Env env) {
    assertEquals(Key.key("minecraft:iron"), MinestomItem.of(ItemStack.of(Material.IRON_HELMET)).armorAsset());
    assertEquals(Key.key("minecraft:diamond"), MinestomItem.of(ItemStack.of(Material.DIAMOND_BOOTS)).armorAsset());
    var original = ItemStack.of(Material.PAPER).withCustomName(Component.text("Fixture")).withAmount(3)
        .with(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(2f), List.of(true), List.of("data"), List.of(new net.minestom.server.color.Color(0x123456))));
    var carrier = MinestomItem.of(original).withModel(Key.key("dui:live/minecraft/paper")).withColors(List.of(0xABCDEF)).withoutGlint();
    assertEquals(Component.text("Fixture"), carrier.stack().get(DataComponents.CUSTOM_NAME));
    assertEquals(3, carrier.stack().amount());
    var data = carrier.stack().get(DataComponents.CUSTOM_MODEL_DATA);
    assertEquals(List.of(2f), data.floats()); assertEquals(List.of(true), data.flags()); assertEquals(List.of("data"), data.strings());
    assertEquals(List.of(0xABCDEF), carrier.colors()); assertEquals(List.of(0x123456), MinestomItem.of(original).colors());
    assertEquals(Key.key("minecraft:paper"), MinestomItem.of(original).model());
  }
  @Test void portableInputsAndBodiesSerializeAsInlineVanillaDialogs(Env env) {
    var label = Component.text("Label");
    var options = new DialogOptions(label, List.of(new DialogInput.Text("text", 220, label, true, "initial", 64, new DialogInput.Text.Multiline(2, 24)),
        new DialogInput.Boolean("bool", label, true, "yes", "no"),
        new DialogInput.NumberRange("number", 200, label, "%s", 0, 10, 3f, 1f),
        new DialogInput.SingleOption("option", 180, List.of(new DialogInput.SingleOption.Option("one", null, true)), label, false)),
        List.of(new DialogOptions.Button("Accept", "accept")), new DialogOptions.Button("Close", "close"), 2, false);
    var key = Key.key("fixture:callback");
    var content = Component.text("canvas").clickEvent(net.kyori.adventure.text.event.ClickEvent.custom(key, BinaryTagHolder.binaryTagHolder("{}")));
    var presentation = new DialogPresentation(options, List.of(new DialogBody.Text(content, 132),
        new DialogBody.Item(MinestomItem.of(ItemStack.of(Material.PAPER)))), List.of(new DialogPresentation.Button(options.buttons().getFirst(), key)),
        new DialogPresentation.Button(options.exit(), Key.key("fixture:exit")));
    var lowered = (Dialog.MultiAction)MinestomDialogs.lower(presentation);
    assertFalse(lowered.metadata().pause()); assertEquals(4, lowered.metadata().inputs().size());
    assertInstanceOf(DialogAction.DynamicCustom.class, lowered.actions().getFirst().action());
    var bytes = NetworkBuffer.makeArray(ShowDialogPacket.INLINE_SERIALIZER, new ShowDialogPacket(lowered), env.process().registries());
    var decoded = ShowDialogPacket.INLINE_SERIALIZER.read(NetworkBuffer.wrap(bytes, 0, bytes.length, env.process().registries()));
    var decodedDialog = (Dialog.MultiAction)decoded.dialog();
    assertEquals(lowered.actions(), decodedDialog.actions());
    assertEquals(lowered.metadata().inputs(), decodedDialog.metadata().inputs());
    var click = (net.kyori.adventure.text.event.ClickEvent.Payload.Custom)((net.minestom.server.dialog.DialogBody.PlainMessage)decodedDialog.metadata().body().getFirst()).contents().clickEvent().payload();
    assertEquals(key, click.key()); assertEquals("{}", click.nbt().string());
  }
  @Test void repliesRejectWrongNbtTypesAndPreserveValues(Env env) {
    var reply = MinestomResponse.of(CompoundBinaryTag.builder().putString("text", "hello").putFloat("number", 2.5f).putByte("bool", (byte)1).build());
    assertEquals("hello", reply.text("text")); assertEquals(2.5f, reply.number("number")); assertTrue(reply.bool("bool"));
    assertNull(reply.text("number")); assertNull(reply.number("text")); assertNull(reply.bool("missing"));
    assertNull(MinestomResponse.of(StringBinaryTag.stringBinaryTag("foreign")).text("text"));
    var stringBooleans = MinestomResponse.of(CompoundBinaryTag.builder().putString("yes", "true").putString("no", "false").putString("foreign", "value").build());
    assertTrue(stringBooleans.bool("yes")); assertFalse(stringBooleans.bool("no")); assertNull(stringBooleans.bool("foreign"));
  }
  @Test void dialogWireRetainsNativeItemHoverComponents(Env env) {
    var stack = ItemStack.of(Material.DIAMOND).withCustomName(Component.text("Named diamond"));
    var content = Component.text("Item").hoverEvent(MinestomItem.of(stack)
        .withModel(Key.key("dui:live/minecraft/diamond")).withColors(List.of(0xABCDEF)).hover());
    var options = DialogOptions.notice("Fixture", "Close", "close");
    var presentation = new DialogPresentation(options, List.of(new DialogBody.Text(content, 180)), List.of(),
        new DialogPresentation.Button(options.exit(), Key.key("fixture:close")));
    var wire = MinestomDialogPackets.encode(presentation, env.process().registries());
    var hover = ((CompoundBinaryTag)wire.getList("body").get(0)).getCompound("contents").getCompound("hover_event");
    assertEquals("minecraft:diamond", hover.getString("id"));
    assertEquals("Named diamond", hover.getCompound("components").getString("minecraft:custom_name"));
    assertEquals("dui:live/minecraft/diamond", hover.getCompound("components").getString("minecraft:item_model"));
    assertNotNull(hover.getCompound("components").get("minecraft:custom_model_data"));
  }
  @Test void packEventsAndNativeClicksUseTheSharedViewerLifecycle(Env env) {
    var instance = env.createFlatInstance(); var connection = env.createConnection(); var player = connection.connect(instance, new Pos(0, 41, 0));
    var metadata = metadata(false); var pack = PackDescriptor.of(URI.create("https://example.org/dui.zip"), metadata);
    int nodes = env.process().eventHandler().getChildren().size(); var calls = new AtomicInteger();
    try (var dui = Dui.create(pack, metadata)) {
      var shown = connection.trackIncoming(ShowDialogPacket.class);
      var offered = connection.trackIncoming(ResourcePackPushPacket.class);
      var session = player.acquirable().applySync(p -> dui.open(p, new Canvas(120, 18, RenderEnvironment.plain(metadata.font())),
          ViewModel.data(Map.of()), DialogOptions.notice("Fixture", "Close", "close"), c -> calls.incrementAndGet()));
      assertEquals(1, offered.collect().size()); assertNull(session.component());
      EventDispatcher.call(new PlayerResourcePackStatusEvent(player, pack.id(), ResourcePackStatus.SUCCESSFULLY_LOADED));
      var nativeDialog = (Dialog.Notice)shown.collect().getFirst().dialog();
      var key = ((DialogAction.DynamicCustom)nativeDialog.action().action()).key();
      EventDispatcher.call(new PlayerCustomClickEvent(player, key, CompoundBinaryTag.empty()));
      env.tick(); assertEquals(1, calls.get()); assertFalse(session.isActive());
      EventDispatcher.call(new PlayerCustomClickEvent(player, key, CompoundBinaryTag.empty())); env.tick(); assertEquals(1, calls.get());
    }
    assertEquals(nodes, env.process().eventHandler().getChildren().size());
  }
  @Test void privateMapEntitiesAndInventoryAreRestoredOnClose(Env env) {
    var instance = env.createFlatInstance(); var player = env.createPlayer(instance, new Pos(0, 41, 0));
    var bystander = env.createPlayer(instance, new Pos(2, 41, 0));
    var metadata = metadata(true); var pack = PackDescriptor.of(URI.create("https://example.org/dui.zip"), metadata);
    player.acquirable().sync(p -> p.getInventory().setItemStack(0, ItemStack.of(Material.DIAMOND)));
    var items = player.getInventory().getItemStacks(); var position = player.getPosition();
    try (var dui = Dui.create(pack, metadata)) {
      EventDispatcher.call(new PlayerResourcePackStatusEvent(player, pack.id(), ResourcePackStatus.SUCCESSFULLY_LOADED));
      var session = player.acquirable().applySync(p -> dui.openWorldMap(p, "fixture:map",
          new WorldMapFrame("map", List.of("layer"), List.of(), WorldHud.EMPTY), WorldMapOptions.still(), e -> {}));
      assertTrue(session.isStarted()); assertNotNull(player.getVehicle()); assertArrayEquals(items, player.getInventory().getItemStacks());
      assertTrue(player.getVehicle().getViewers().contains(player));
      assertFalse(player.getVehicle().getViewers().contains(bystander));
      player.acquirable().sync(p -> session.close());
      assertNull(player.getVehicle()); assertArrayEquals(items, player.getInventory().getItemStacks()); assertEquals(position, player.getPosition());
      assertTrue(instance.getEntities().stream().allMatch(e -> e instanceof Player));
    }
  }
  @Test void outgoingPresentationMaskOnlyTargetsTheViewerAndHandSlots(Env env) {
    assertTrue(MinestomAppearanceMask.hides(new SetPlayerInventorySlotPacket(0, ItemStack.AIR), 7));
    assertFalse(MinestomAppearanceMask.hides(new SetPlayerInventorySlotPacket(20, ItemStack.AIR), 7));
    assertTrue(MinestomAppearanceMask.hides(new EntityMetaDataPacket(7, Map.of()), 7));
    assertFalse(MinestomAppearanceMask.hides(new EntityMetaDataPacket(8, Map.of()), 7));
    assertFalse(MinestomAppearanceMask.hides(new SetSlotPacket(1, 0, (short)36, ItemStack.AIR), 7));
  }
}
