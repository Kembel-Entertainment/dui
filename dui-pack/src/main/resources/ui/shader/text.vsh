#version 330
#extension GL_ARB_separate_shader_objects : require

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#include <minecraft:fog.glsl>
#include <minecraft:sample_lightmap.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <dui:protocol.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
layout(location = 3) in ivec2 UV2;
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
#endif

layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;
#ifdef IS_GUI
uniform sampler2D Sampler0;
layout(location = 4) flat out int focusGuard;
layout(location = 5) flat out int playerFlags;
layout(location = 6) out vec2 playerPoint;
layout(location = 7) flat out vec2 playerSize;
layout(location = 8) flat out vec2 guardSize;
layout(location = 9) out vec2 guardPoint;
#endif

// PLAYER_RENDERERS

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
#else
    vertexColor = Color;
#endif
    texCoord0 = UV0;
#ifdef IS_GUI
    focusGuard=0;guardSize=vec2(0);guardPoint=vec2(0);playerFlags=-1;playerPoint=vec2(0);playerSize=vec2(0);
    uvec3 playerRgb=uvec3(round(Color.rgb*255.0));
    uint playerCode=(playerRgb.r<<16u)|(playerRgb.g<<8u)|playerRgb.b;
    vec2 faceCorner=(UV0*64.0-vec2(8))/8.0;
    if((playerCode&DUI_PLAYER_SIGNATURE_MASK)==DUI_PLAYER_SIGNATURE&&textureSize(Sampler0,0)==ivec2(64)&&all(greaterThanEqual(faceCorner,vec2(0)))&&all(lessThanEqual(faceCorner,vec2(1)))){
        playerFlags=int(playerCode&255u)|int(((playerCode>>14u)&15u)<<10u);
        if(!duiPlayerViewport((playerFlags>>10)&15,playerFlags&3,playerSize)){playerFlags=-1;return;}
        float h=playerSize.y;float band=float((playerCode>>8u)&63u);playerPoint=vec2(faceCorner.x*playerSize.x,(band+faceCorner.y)*9.0);
        vec2 origin=Position.xy-faceCorner*8.0;
        gl_Position=ProjMat*ModelViewMat*vec4(origin+vec2(playerPoint.x,faceCorner.y*9.0),Position.z,1);return;
    }

    // Geometry is encoded in a size-addressed technical font glyph, not its runtime RGB.
    vec4 marker=texelFetch(Sampler0,ivec2(UV0*vec2(textureSize(Sampler0,0))),0);
    uvec4 data=uvec4(round(marker*255.0));int cornerCode=int(data.b)-224;
    if(data.a==248u&&cornerCode>=0&&cornerCode<=3){
        vec2 bounds=vec2(float(data.r+((data.g&1u)<<8u)),float(data.g>>1u)*9.0);
        if(bounds.x>=120.0&&bounds.x<=480.0&&bounds.y>=9.0&&bounds.y<=360.0){
            vec2 corner=vec2(float(cornerCode&1),float((cornerCode>>1)&1));
            vec2 origin=Position.xy-corner*8.0;
            focusGuard=1;guardSize=bounds+vec2(14,10);guardPoint=corner*guardSize;
            gl_Position=ProjMat*ModelViewMat*vec4(origin-vec2(6,5)+guardPoint,Position.z,1);
        }
    }

#endif
}
