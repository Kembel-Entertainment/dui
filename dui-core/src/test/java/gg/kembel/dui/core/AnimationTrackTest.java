package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class AnimationTrackTest {
  @Test
  void keyframesLoopPingPongAndInterruptionHaveDefinedEndpoints() {
    var frames =
        List.of(
            new AnimationTrack.Keyframe(0, 0),
            new AnimationTrack.Keyframe(10, 100),
            new AnimationTrack.Keyframe(20, 50));
    var once = new AnimationTrack(frames, AnimationTrack.Playback.ONCE);
    assertEquals(50, once.sample(5, true));
    assertEquals(50, once.sample(100, true));
    assertEquals(50, once.sample(0, false));
    assertEquals(0, new AnimationTrack(frames, AnimationTrack.Playback.LOOP).sample(20, true));
    assertEquals(
        100, new AnimationTrack(frames, AnimationTrack.Playback.PING_PONG).sample(30, true));
    var resumed = once.retarget(5, 200, 10, null);
    assertEquals(50, resumed.sample(0, true));
    assertEquals(200, resumed.sample(10, true));
    assertEquals(.5, new AnimationTrack.Curve(0, 0, 1, 1).sample(.5), 1e-8);
  }

  @Test
  void samplingDeliversFinalFrameAndStaleJobsDoNotContinue() {
    var scheduler = new TaskScopeTest.Scheduler();
    var scope = new TaskScope(scheduler);
    var seen = new ArrayList<Long>();
    scope.sample("frames", 5, 2, seen::add);
    for (int i = 0; i < 4; i++) scheduler.drain();
    assertEquals(List.of(0L, 2L, 4L, 5L), seen);
    var stop = scope.sample("loop", 0, 1, seen::add);
    scheduler.drain();
    stop.cancel();
    scheduler.drain();
    assertEquals(5, seen.size());
    scope.sample("loop", 0, 1, age -> fail("closed animation"));
    scope.close();
    scheduler.drain();
  }
}
