#version 330
#extension GL_ARB_separate_shader_objects : require

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    float GlintAlpha;
    vec3 CameraOffset;
    float GameTime;
    vec2 ScreenSize;
    int MenuBlurRadius;
    int UseRgss;
};
uniform sampler2D Sampler0;

layout(location = 5) in vec2 clipPoint;
layout(location = 6) flat in vec4 itemClip;
layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;
layout(location = 7) in vec2 burstPoint;
layout(location = 8) flat in vec2 burstBounds;
layout(location = 9) flat in vec3 burstItem;
layout(location = 10) flat in float burstAge;
layout(location = 11) flat in int effectKind;
layout(location = 12) flat in int effectFlags;
layout(location = 13) flat in int effectCount;
layout(location = 14) flat in uvec4 effectWords[5];

layout(location = 2) flat in int playerArmorFlags;
layout(location = 3) in vec2 playerArmorPoint;
layout(location = 4) flat in vec2 playerArmorSize;
// PLAYER_FUNCTIONS
layout(location = 0) out vec4 fragColor;

float random(float n) { return fract(sin(n*127.1+311.7)*43758.5453); }
// PROTOCOL
// EFFECT_FUNCTIONS

void main() {
    if(playerArmorFlags>=0){vec4 c=playerArmorPixel(Sampler0,playerArmorPoint,playerArmorSize,playerArmorFlags,GameTime*1200.0);if(c.a<.1)discard;fragColor=c*ColorModulator;return;}
    if(effectKind==5 && (any(lessThan(clipPoint,itemClip.xy)) || any(greaterThanEqual(clipPoint,itemClip.zw))))discard;
    if(effectKind==1 || effectKind==6){vec4 panel=shaderEffects(burstPoint,burstAge,effectFlags);if(panel.a==0.0)discard;fragColor=panel*ColorModulator;return;}
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
