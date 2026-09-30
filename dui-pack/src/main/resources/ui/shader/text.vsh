#version 330

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#endif

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
in ivec2 UV2;
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;
#endif

out vec4 vertexColor;
out vec2 texCoord0;
#ifdef IS_GUI
uniform sampler2D Sampler0;
flat out int focusGuard;
flat out vec2 guardSize;
out vec2 guardPoint;
#endif

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
    focusGuard=0;guardSize=vec2(0);guardPoint=vec2(0);
    // Only our dedicated, colour-marked font glyph can activate this overlay.
    // No vanilla GUI rectangles or unmarked text are filtered.
    vec4 marker=texelFetch(Sampler0,ivec2(UV0*vec2(textureSize(Sampler0,0))),0);
    int cornerCode=int(round(marker.b*255.0))-224;
    if(all(lessThan(abs(marker.rga-vec3(248.0,4.0,255.0)/255.0),vec3(.001)))&&cornerCode>=0&&cornerCode<=3){
        uvec3 rgb=uvec3(round(Color.rgb*255.0));uint payload=(rgb.r<<16u)|(rgb.g<<8u)|rgb.b;
        vec2 bounds=vec2(float(payload&511u),float((payload>>9u)&511u));
        if(bounds.x>=120.0&&bounds.x<=480.0&&bounds.y>=9.0&&bounds.y<=360.0){
            vec2 corner=vec2(float(cornerCode&1),float((cornerCode>>1)&1));
            vec2 origin=Position.xy-corner*8.0;
            focusGuard=1;guardSize=bounds+vec2(14,10);guardPoint=corner*guardSize;
            gl_Position=ProjMat*ModelViewMat*vec4(origin-vec2(6,5)+guardPoint,Position.z,1);
        }
    }
#endif
}
