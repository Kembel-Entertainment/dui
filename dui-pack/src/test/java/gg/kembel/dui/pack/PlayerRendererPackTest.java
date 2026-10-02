package gg.kembel.dui.pack;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PlayerRendererPackTest {
  @Test
  void contributedCameraAndLimbPoseGenerateTheSameLookupAsMetadata() {
    var spec =
        new PlayerRenderSpec(
            "consumer:avatar",
            List.of(new PlayerRenderSpec.Viewport(80, 135)),
            List.of(new PlayerRenderSpec.Pose(30, 15, 42, List.of(0., 0., 90., 0., 0., 0.), 4, 2)));
    var source = PlayerRendererPack.glsl(PlayerRenderBinding.bind(List.of(spec)));
    assertTrue(source.contains("renderer==1&&viewport==0){size=vec2(80,135)"));
    assertTrue(source.contains("camera=vec4(0.523598776,0.261799388,42.000000000"));
    assertTrue(source.contains("limbs[2]=1.570796327"));
    assertEquals(source, PlayerRendererPack.glsl(PlayerRenderBinding.bind(List.of(spec))));
  }
}
