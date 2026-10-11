#version 330
#extension GL_ARB_separate_shader_objects : require

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#include <minecraft:fog.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
#endif

layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;
#ifdef IS_GUI
layout(location = 4) flat in int focusGuard;
layout(location = 5) flat in int playerFlags;
layout(location = 6) in vec2 playerPoint;
layout(location = 7) flat in vec2 playerSize;
layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    float GlintAlpha;
    vec3 CameraOffset;
    float GameTime;
    vec2 ScreenSize;
    int MenuBlurRadius;
    int UseRgss;
};
// PLAYER_FUNCTIONS
layout(location = 8) flat in vec2 guardSize;
layout(location = 9) in vec2 guardPoint;
#endif

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

// All DUI and vanilla paths participate in the same 26.3 transparency passes.
void duiWriteColor(vec4 color, bool fogged) {
#ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
#else
#ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
#endif
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    if (fogged) {
#ifdef OIT_ACCUMULATE
        vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
#else
        vec4 fogColor = FogColor;
#endif
        color = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
    }
#endif
    fragColor = color;
#endif
}

void main() {
#ifdef IS_GUI
    if(playerFlags>=0){vec4 player=playerSkinPixel(Sampler0,playerPoint,playerSize,playerFlags,GameTime*1200.0);if(player.a<.1)discard;duiWriteColor(player*ColorModulator,false);return;}

    if(focusGuard==1){
        if(guardPoint.x>=2.0&&guardPoint.y>=2.0&&guardPoint.x<guardSize.x-2.0&&guardPoint.y<guardSize.y-2.0)discard;
        duiWriteColor(vec4(vertexColor.rgb,1.0)*ColorModulator,false);return;
    }

#endif
#ifdef IS_GRAYSCALE
    vec4 texColor = texture(Sampler0, texCoord0).rrrr;
#else
    vec4 texColor = texture(Sampler0, texCoord0);
#endif

    vec4 color = texColor * vertexColor * ColorModulator;
#ifdef IS_GUI
    if (color.a < 0.01) {
#else
    if (color.a < 0.1) {
#endif
        discard;
    }

    duiWriteColor(color, true);
}
