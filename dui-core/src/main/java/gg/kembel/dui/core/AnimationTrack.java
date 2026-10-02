package gg.kembel.dui.core;

import java.util.*;

/** Pure world-tick keyframes; reusable for component data, geometry and model parameters. */
public record AnimationTrack(List<Keyframe> frames, Playback playback) {
  public enum Playback {
    ONCE,
    LOOP,
    PING_PONG
  }

  public record Curve(double x1, double y1, double x2, double y2) {
    public Curve {
      if (!Double.isFinite(x1)
          || !Double.isFinite(x2)
          || !Double.isFinite(y1)
          || !Double.isFinite(y2)
          || x1 < 0
          || x1 > 1
          || x2 < 0
          || x2 > 1) throw new IllegalArgumentException("Bezier curve");
    }

    public double sample(double progress) {
      double lo = 0, hi = 1, t = progress;
      for (int i = 0; i < 32; i++) {
        t = (lo + hi) / 2;
        if (bezier(t, x1, x2) < progress) lo = t;
        else hi = t;
      }
      return bezier(t, y1, y2);
    }

    private static double bezier(double t, double a, double b) {
      return 3 * (1 - t) * (1 - t) * t * a + 3 * (1 - t) * t * t * b + t * t * t;
    }
  }

  public record Keyframe(long tick, double value, Curve curve) {
    public Keyframe {
      if (tick < 0 || !Double.isFinite(value)) throw new IllegalArgumentException("Keyframe");
    }

    public Keyframe(long tick, double value) {
      this(tick, value, null);
    }
  }

  public AnimationTrack {
    frames = List.copyOf(frames);
    Objects.requireNonNull(playback);
    if (frames.size() < 2 || frames.size() > 128 || frames.getFirst().tick() != 0)
      throw new IllegalArgumentException("Track needs 2..128 frames starting at zero");
    long previous = -1;
    for (var f : frames) {
      if (f.tick() <= previous)
        throw new IllegalArgumentException("Increasing keyframe times required");
      previous = f.tick();
    }
    if (previous > Long.MAX_VALUE / 2)
      throw new IllegalArgumentException("Track duration overflow");
  }

  public long duration() {
    return frames.getLast().tick();
  }

  public double sample(long age, boolean enabled) {
    if (!enabled) return frames.getLast().value();
    age = Math.max(0, age);
    long time =
        switch (playback) {
          case ONCE -> Math.min(age, duration());
          case LOOP -> age % duration();
          case PING_PONG -> {
            long t = age % (duration() * 2);
            yield t > duration() ? duration() * 2 - t : t;
          }
        };
    for (int i = 1; i < frames.size(); i++) {
      var a = frames.get(i - 1);
      var b = frames.get(i);
      if (time <= b.tick()) {
        double u = (double) (time - a.tick()) / (b.tick() - a.tick());
        if (b.curve() != null) u = b.curve().sample(u);
        return a.value() + (b.value() - a.value()) * u;
      }
    }
    return frames.getLast().value();
  }

  /** Creates an uninterrupted transition from the current sampled value. */
  public AnimationTrack retarget(long age, double target, long ticks, Curve curve) {
    return new AnimationTrack(
        List.of(new Keyframe(0, sample(age, true)), new Keyframe(ticks, target, curve)),
        Playback.ONCE);
  }
}
