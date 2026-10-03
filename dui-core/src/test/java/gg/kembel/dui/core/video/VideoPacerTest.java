package gg.kembel.dui.core.video;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VideoPacerTest {
  @Test void overloadedStreamSpreadsFramesAcrossEverySecondInsteadOfBursting() {
    long frameBytes = 2_642_976, budget = 16_777_216;
    var pacer = new VideoPacer(60, budget);
    long now = 1_000_000_000L, first = now;
    long interval = (long) Math.ceil(frameBytes * 1_000_000_000d / budget);
    for (int i = 0; i < 120; i++) {
      pacer.accepted(now, frameBytes);
      assertEquals(interval, pacer.nextSendNanos() - now);
      assertTrue(pacer.nextSendNanos() - now < 200_000_000L, "No one-second budget stall");
      now = pacer.nextSendNanos();
    }
    assertTrue(120d * frameBytes / ((now - first) / 1e9) <= budget);
  }
  @Test void smallPatchesKeepTheFrameClockAndAnIdlePeriodDoesNotBankABurst() {
    var pacer = new VideoPacer(30, 16_777_216);
    long now = 1_000_000_000L, interval = Math.round(1e9 / 30);
    pacer.accepted(now, 1000); assertEquals(now + interval, pacer.nextSendNanos());
    // Scheduler jitter retains phase while the byte deadline is much shorter.
    pacer.accepted(now + interval + 100_000, 1000);
    assertEquals(now + interval * 2, pacer.nextSendNanos());
    now += 10_000_000_000L;
    pacer.accepted(now, 1000); assertEquals(now + interval, pacer.nextSendNanos());
    assertThrows(IllegalArgumentException.class, () -> pacer.accepted(0, -1));
  }
  @Test void producerPlanningDependsOnFormatGeometryAndConsumerBudget() {
    var spec = new VideoSurfaceSpec(1024, 576, PixelFormat.RGB888, 60,
        VideoSurfaceSpec.Viewport.FULL, new VideoSurfaceSpec.Budget(161, 16_777_216), 0, false);
    assertEquals(2_642_976, spec.maximumFrameBytes());
    assertEquals(6.34785105, spec.sustainableFps(), .00001);
    var fast = new VideoSurfaceSpec(256, 144, PixelFormat.RGB888, 60,
        VideoSurfaceSpec.Viewport.FULL, new VideoSurfaceSpec.Budget(17, 16_777_216), 0, false);
    assertEquals(60, fast.sustainableFps());
  }
}
