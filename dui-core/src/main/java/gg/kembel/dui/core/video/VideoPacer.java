package gg.kembel.dui.core.video;

/** Single-writer stream clock. Bytes are paced continuously, without periodic budget resets. */
public final class VideoPacer {
  private final long frameInterval, bytesPerSecond;
  private long frameDeadline, byteDeadline;
  public VideoPacer(double maximumFps, long bytesPerSecond) {
    if (!Double.isFinite(maximumFps) || maximumFps <= 0 || bytesPerSecond <= 0)
      throw new IllegalArgumentException("Video pacing limits");
    frameInterval = Math.max(1, Math.round(1_000_000_000d / maximumFps));
    this.bytesPerSecond = bytesPerSecond;
  }
  public long nextSendNanos() { return Math.max(frameDeadline, byteDeadline); }
  /** Charge only an accepted network write, using its actual uncompressed patch bytes. */
  public void accepted(long now, long bytes) {
    if (bytes < 0) throw new IllegalArgumentException("Negative frame bytes");
    frameDeadline = frameDeadline == 0 || now - frameDeadline > frameInterval * 2
        ? now + frameInterval : frameDeadline + frameInterval;
    byteDeadline = now + (long) Math.ceil(bytes * 1_000_000_000d / bytesPerSecond);
  }
}
