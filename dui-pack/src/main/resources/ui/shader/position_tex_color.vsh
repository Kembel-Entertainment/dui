#version 330

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl and projection.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};
// GUI_TEXTURED and the startup pipelines bind Globals in Minecraft 26.2.
layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform sampler2D Sampler0;

out vec2 texCoord0;
out vec4 vertexColor;
out vec2 burstPoint;
flat out vec2 burstBounds;
flat out vec3 burstItem;
flat out float burstAge;
flat out int effectKind;
flat out int effectFlags;
flat out int effectCount;
flat out uvec3 effectData[8];

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texCoord0 = UV0;
    vertexColor = Color;
    burstAge = -1.0;
    burstPoint = vec2(0.0);
    burstBounds = vec2(0.0);
    burstItem = vec3(0.0);
    effectKind = 0;
    effectFlags = 0;
    effectCount = 0;
    for(int i=0;i<8;i++)effectData[i]=uvec3(0u);

    // Oversized GUI items have their own 48 * GUI-scale square render target.
    // Read only our binary signature. Other menus, world textures and atlas blits pass through.
    ivec2 dimensions = textureSize(Sampler0, 0);
    if (dimensions.x != dimensions.y || dimensions.x < 48 || dimensions.x % 48 != 0) return;
    if (UV0.x != 0.0 && UV0.x != 1.0) return;
    if (UV0.y != 0.0 && UV0.y != 1.0) return;
    const ivec3 magic[4] = ivec3[4](ivec3(1,0,1), ivec3(0,1,0), ivec3(1,1,0), ivec3(0,0,1));
    vec3 lo=vec3(1.0), hi=vec3(0.0);
    vec4 signature[4];
    for(int i=0;i<4;i++) {
        signature[i]=texture(Sampler0,vec2((float(i)*3.0+1.5)/48.0,1.0-1.5/48.0));
        lo=min(lo,signature[i].rgb); hi=max(hi,signature[i].rgb);
    }
    if(any(lessThan(hi-lo,vec3(0.1)))) return;
    vec3 threshold=(hi+lo)*0.5;
    for(int i=0;i<4;i++) {
        if(signature[i].a<0.9 || any(notEqual(ivec3(greaterThan(signature[i].rgb,threshold)),magic[i]))) return;
    }
    uint data=0u;
    for (int i=0; i<10; i++) {
        vec3 bits=step(threshold,texture(Sampler0,vec2((float(i+4)*3.0+1.5)/48.0,1.0-1.5/48.0)).rgb);
        data |= (uint(bits.r) | (uint(bits.g)<<1u) | (uint(bits.b)<<2u)) << uint(i*3);
    }
    vec2 offset=vec2(int(data&2047u)-1024,int((data>>11u)&2047u)-1024);
    float size=float((data>>22u)&127u);
    if (size < 1.0) return;
    vec2 corner=vec2(UV0.x,1.0-UV0.y);
    vec2 itemOrigin=Position.xy-corner*48.0+vec2(16.0);
    gl_Position=ProjMat*ModelViewMat*vec4(itemOrigin+offset+corner*size,Position.z,1.0);
    // Crop the data frame and retain the central 36px native render.
    texCoord0=mix(vec2(6.0/48.0),vec2(42.0/48.0),UV0);
    if ((data & (1u<<29u)) == 0u) return;
    uint low=0u,high=0u;
    for(int i=0;i<18;i++) {
        vec2 uv=i<16?vec2((float(i)*3.0+1.5)/48.0,1.5/48.0):vec2((43.5+float(i-16)*3.0)/48.0,1.0-1.5/48.0);
        vec3 rgb=step(threshold,texture(Sampler0,uv).rgb);
        uint cell=uint(rgb.r)|(uint(rgb.g)<<1u)|(uint(rgb.b)<<2u);
        if(i<10)low|=cell<<uint(i*3);else high|=cell<<uint((i-10)*3);
    }
    float started=float(low&32767u);
    burstBounds=vec2(float((low>>15u)&511u),float(((low>>24u)&63u)|((high&7u)<<6u)));
    burstItem=vec3(float((high>>3u)&511u),float((high>>12u)&511u),size);
    effectKind=int((high>>21u)&7u);
    effectFlags=int(size);
    if(any(lessThan(burstBounds,vec2(1.0))) || any(greaterThan(burstBounds,vec2(480.0,360.0))))return;
    if(effectKind==1){
        effectCount=clamp(int(burstItem.x),0,8);
        for(int i=0;i<8;i++){
            if(i>=effectCount)break;
            for(int j=0;j<23;j++){
                int index=i*23+j;vec2 uv=(vec2(float(index%14),float(index/14))*3.0+vec2(4.5))/48.0;
                vec3 rgb=step(threshold,texture(Sampler0,uv).rgb);uint cell=uint(rgb.r)|(uint(rgb.g)<<1u)|(uint(rgb.b)<<2u);
                if(j<10)effectData[i].x|=cell<<uint(j*3);else if(j<20)effectData[i].y|=cell<<uint((j-10)*3);else effectData[i].z|=cell<<uint((j-20)*3);
            }
        }
    }
    burstAge=mod(GameTime*24000.0-started+24000.0,24000.0)/20.0;
    burstPoint=corner*burstBounds;
    vec2 canvasOrigin=itemOrigin+offset-(effectKind==1?vec2(0.0):burstItem.xy);
    gl_Position=ProjMat*ModelViewMat*vec4(canvasOrigin+burstPoint,Position.z,1.0);
}
