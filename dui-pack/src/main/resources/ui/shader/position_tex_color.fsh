#version 330

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform Globals {ivec3 CameraBlockPos;vec3 CameraOffset;vec2 ScreenSize;float GlintAlpha;float GameTime;int MenuBlurRadius;int UseRgss;};
uniform sampler2D Sampler0;

in vec2 clipPoint;
flat in vec4 itemClip;
in vec2 texCoord0;
in vec4 vertexColor;
in vec2 burstPoint;
flat in vec2 burstBounds;
flat in vec3 burstItem;
flat in float burstAge;
flat in int effectKind;
flat in int effectFlags;
flat in int effectCount;
flat in uint effectWords[20];

flat in int playerArmorFlags;
in vec2 playerArmorPoint;
flat in vec2 playerArmorSize;
// PLAYER_FUNCTIONS
out vec4 fragColor;

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
