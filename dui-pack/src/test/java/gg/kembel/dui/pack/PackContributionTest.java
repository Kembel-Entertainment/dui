package gg.kembel.dui.pack;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PackContributionTest {
  @Test
  void typedShaderGenerationIsOrderIndependentAndDoesNotTruncateWideParameters() {
    var color =
        new ShaderSpec(
            "example:color",
            List.of(
                ShaderSpec.Parameter.rgb("inner"),
                ShaderSpec.Parameter.rgb("outer"),
                ShaderSpec.Parameter.decimal("amount", 0, 1, .125)));
    var other = new ShaderSpec("other:shape", List.of(ShaderSpec.Parameter.bool("enabled")));
    var first =
        new PackContribution.Shader(
            color,
            "exampleColor",
            "vec4 exampleColor(vec2 q,vec2 size,DUI_PARAMETERS p,float t,bool live){return"
                + " vec4(mix(p.inner,p.outer,p.amount),1);}");
    var second =
        new PackContribution.Shader(
            other,
            "otherShape",
            "vec4 otherShape(vec2 q,vec2 size,DUI_PARAMETERS p,float t,bool live){return"
                + " vec4(p.enabled?1:0);}");
    var bindings = ShaderRegistry.bind(List.of(color, other));
    var modules =
        Map.of(
            "other:helpers",
            "float otherHelper(){return 1.;}",
            "example:helpers",
            "float exampleHelper(){return 0.;}");
    String forward = ShaderCompiler.compile(List.of(first, second), bindings, modules);
    assertEquals(
        forward,
        ShaderCompiler.compile(
            List.of(second, first), ShaderRegistry.bind(List.of(other, color)), modules));
    assertTrue(forward.contains("base+24,24"));
    assertTrue(forward.contains("base+48,4"));
    assertTrue(forward.contains("vec3 inner;"));
    assertTrue(forward.contains("float amount;"));
    assertTrue(forward.contains("bool enabled;"));
    assertTrue(forward.indexOf("exampleHelper") < forward.indexOf("otherHelper"));
    assertFalse(forward.contains("DUI_PARAMETERS"));
  }

  @Test
  void ownersAndCollisionsAreCheckedBeforePackEmission() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PackContribution(
                "example", Map.of("assets/minecraft/fonts/default.json", new byte[0]), List.of()));
    var effect =
        new PackContribution.Shader(
            new ShaderSpec("example:pulse", List.of(ShaderSpec.Parameter.integer("size", 0, 255))),
            "examplePulse",
            "vec4 examplePulse(vec2 q,vec2 size,DUI_PARAMETERS p,float t,bool live){return"
                + " vec4(1);}");
    var p =
        new PackContribution(
            "example", Map.of("assets/example/font/text.json", new byte[0]), List.of(effect));
    assertEquals(1, PackContribution.merge(List.of(p), new TreeMap<>()).size());
    assertThrows(
        IllegalArgumentException.class,
        () -> PackContribution.merge(List.of(p, p), new TreeMap<>()));
  }
}
