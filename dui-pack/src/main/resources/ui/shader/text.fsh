#version 330

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#moj_import <minecraft:fog.glsl>
#endif

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#endif

in vec4 vertexColor;
in vec2 texCoord0;
#ifdef IS_GUI
flat in int focusGuard;
flat in vec2 guardSize;
in vec2 guardPoint;
#endif

out vec4 fragColor;

void main() {
#ifdef IS_GUI
    if(focusGuard==1){
        // Cover the native padded focus stroke. The centre remains fully transparent.
        if(guardPoint.x>=2.0&&guardPoint.y>=2.0&&guardPoint.x<guardSize.x-2.0&&guardPoint.y<guardSize.y-2.0)discard;
        fragColor=vec4(22.0,23.0,29.0,255.0)/255.0*ColorModulator;return;
    }
#endif
#ifdef IS_GRAYSCALE
    vec4 texColor = texture(Sampler0, texCoord0).rrrr;
#else
    vec4 texColor = texture(Sampler0, texCoord0);
#endif

#ifdef IS_SEE_THROUGH
    vec4 color = texColor * vertexColor;
#else
    vec4 color = texColor * vertexColor * ColorModulator;
#endif
    if (color.a < 0.1) {
        discard;
    }

#ifdef IS_SEE_THROUGH
    fragColor = color * ColorModulator;
#elif defined(IS_GUI)
    fragColor = color;
#else
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
#endif
}
