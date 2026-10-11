package gg.kembel.dui.minestom;

import gg.kembel.dui.runtime.DialogPresentation;
import java.util.ArrayList;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.dialog.*;
import net.minestom.server.entity.Player;
import net.minestom.server.network.*;
import net.minestom.server.network.packet.*;
import net.minestom.server.network.packet.server.FramedPacket;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;
import net.minestom.server.registry.*;

/**
 * The pinned snapshot's dialog component codec omits show-item components.
 * Retain the native dialog codec, but encode text with Minestom's complete network component writer.
 * FramedPacket retains the native packet identity and ordinary outgoing event/cancellation.
 */
final class MinestomDialogPackets {
  private MinestomDialogPackets() {}
  static BinaryTag component(Component value, Registries registries) {
    var bytes = NetworkBuffer.makeArray(NetworkBuffer.COMPONENT, value, registries);
    return NetworkBuffer.wrap(bytes, 0, bytes.length, registries).read(NetworkBuffer.NBT);
  }
  private static CompoundBinaryTag message(CompoundBinaryTag encoded, DialogBody.PlainMessage message, Registries registries) {
    return encoded.put("contents", component(message.contents(), registries));
  }
  static CompoundBinaryTag encode(DialogPresentation presentation, Registries registries) {
    var dialog = MinestomDialogs.lower(presentation);
    var encoded = (CompoundBinaryTag)Dialog.REGISTRY_CODEC.encode(new RegistryTranscoder<>(Transcoder.NBT, registries), dialog).orElseThrow();
    encoded = encoded.put("title", component(presentation.options().title(), registries));
    var bodies = new ArrayList<BinaryTag>();
    var nativeBodies = presentation.body().stream().map(MinestomDialogs::body).toList();
    for (int i = 0; i < nativeBodies.size(); i++) {
      // DialogMetadata uses listOrSingle: one body is encoded directly as a compound.
      var bodyTag = encoded.get("body");
      var body = (CompoundBinaryTag)(bodyTag instanceof ListBinaryTag list ? list.get(i) : bodyTag);
      body = switch (nativeBodies.get(i)) {
        case DialogBody.PlainMessage text -> message(body, text, registries);
        case DialogBody.Item item -> item.description() == null ? body
            : body.put("description", message(body.getCompound("description"), item.description(), registries));
      };
      bodies.add(body);
    }
    encoded = encoded.put("body", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, bodies));
    var inputs = new ArrayList<BinaryTag>();
    var nativeInputs = presentation.options().inputs().stream().map(MinestomDialogs::input).toList();
    for (int i = 0; i < nativeInputs.size(); i++) {
      var input = nativeInputs.get(i);
      var label = switch (input) {
        case DialogInput.Boolean value -> value.label();
        case DialogInput.NumberRange value -> value.label();
        case DialogInput.SingleOption value -> value.label();
        case DialogInput.Text value -> value.label();
      };
      var value = ((CompoundBinaryTag)encoded.getList("inputs").get(i)).put("label", component(label, registries));
      if (input instanceof DialogInput.SingleOption option) {
        var entries = new ArrayList<BinaryTag>();
        for (int at = 0; at < option.options().size(); at++) {
          var entry = (CompoundBinaryTag)value.getList("options").get(at);
          var display = option.options().get(at).display();
          entries.add(display == null ? entry : entry.put("display", component(display, registries)));
        }
        value = value.put("options", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, entries));
      }
      inputs.add(value);
    }
    return encoded.put("inputs", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, inputs));
  }
  static void show(Player player, DialogPresentation presentation) {
    var packet = new ShowDialogPacket(MinestomDialogs.lower(presentation));
    var encoded = encode(presentation, MinecraftServer.getRegistries());
    var serializer = new NetworkBuffer.Type<ShowDialogPacket>() {
      public void write(NetworkBuffer buffer, ShowDialogPacket value) { buffer.write(NetworkBuffer.VAR_INT, 0); buffer.write(NetworkBuffer.NBT, encoded); }
      public ShowDialogPacket read(NetworkBuffer buffer) { return ShowDialogPacket.SERIALIZER.read(buffer); }
    };
    var info = PacketVanilla.SERVER_PACKET_PARSER.stateRegistry(ConnectionState.PLAY).packetInfo(ShowDialogPacket.class);
    var buffer = NetworkBuffer.resizableBuffer(4096, MinecraftServer.getRegistries());
    PacketWriting.writeFramedPacket(buffer, serializer, info.id(), packet, MinecraftServer.getCompressionThreshold());
    player.sendPacket(new FramedPacket(packet, buffer.slice(0, buffer.writeIndex(), 0, buffer.writeIndex())));
  }
}
