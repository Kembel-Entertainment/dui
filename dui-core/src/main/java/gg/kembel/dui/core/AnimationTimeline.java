package gg.kembel.dui.core;

import java.util.*;

/** Pure timing plan in world ticks. No frame polling, game decisions or automatic payouts. */
public record AnimationTimeline(List<Segment> segments, long duration) {
  public record Segment(String id, long start, long duration) {
    public Segment {
      if (id == null || id.isBlank() || start < 0 || duration < 1)
        throw new IllegalArgumentException("Timeline segment needs an id and positive duration");
      Math.addExact(start, duration);
    }

    public long end() {
      return Math.addExact(start, duration);
    }
  }

  public AnimationTimeline {
    segments = List.copyOf(segments);
    if (duration < 0
        || segments.stream().anyMatch(s -> s.end() > duration)
        || segments.stream().map(Segment::id).distinct().count() != segments.size())
      throw new IllegalArgumentException("Timeline IDs must be unique and fit its duration");
  }

  public static AnimationTimeline of(String id, long duration) {
    return new AnimationTimeline(List.of(new Segment(id, 0, duration)), duration);
  }

  public static AnimationTimeline sequence(AnimationTimeline... plans) {
    var result = new ArrayList<Segment>();
    long offset = 0;
    for (var plan : plans) {
      long start = offset;
      plan.segments.forEach(
          s -> result.add(new Segment(s.id, Math.addExact(start, s.start), s.duration)));
      offset = Math.addExact(offset, plan.duration);
    }
    return new AnimationTimeline(result, offset);
  }

  public static AnimationTimeline parallel(AnimationTimeline... plans) {
    var result = new ArrayList<Segment>();
    long duration = 0;
    for (var plan : plans) {
      result.addAll(plan.segments);
      duration = Math.max(duration, plan.duration);
    }
    return new AnimationTimeline(result, duration);
  }

  public AnimationTimeline delayed(long ticks) {
    if (ticks < 0) throw new IllegalArgumentException("Delay must be nonnegative");
    return new AnimationTimeline(
        segments.stream()
            .map(s -> new Segment(s.id, Math.addExact(s.start, ticks), s.duration))
            .toList(),
        Math.addExact(duration, ticks));
  }

  public Segment segment(String id) {
    return segments.stream()
        .filter(s -> s.id.equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown timeline segment: " + id));
  }

  public long remaining(long startedAt, long now) {
    if (startedAt < 0 || now < 0)
      throw new IllegalArgumentException("World ticks must be nonnegative");
    return duration - Math.min(duration, Math.max(0, now - startedAt));
  }
}
