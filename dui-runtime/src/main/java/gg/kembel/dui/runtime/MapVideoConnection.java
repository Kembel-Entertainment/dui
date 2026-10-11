package gg.kembel.dui.runtime;

import gg.kembel.dui.core.video.MapVideoCodec;
import java.util.List;
import java.util.function.Consumer;

/** Bounded per-connection packet sink. May be called by the encoding worker. */
public interface MapVideoConnection extends AutoCloseable {
  boolean ready() throws Exception;
  Object framePacket(int tile, MapVideoCodec.Patch patch) throws Exception;
  boolean sendFrame(List<Object> packets, Runnable accepted, Consumer<Throwable> failure) throws Exception;
  void close();
}
