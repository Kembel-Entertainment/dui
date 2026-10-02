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

flat out int playerArmorFlags;
out vec2 playerArmorPoint;
flat out vec2 playerArmorSize;
out vec2 clipPoint;
flat out vec4 itemClip;
out vec2 texCoord0;
out vec4 vertexColor;
out vec2 burstPoint;
flat out vec2 burstBounds;
flat out vec3 burstItem;
flat out float burstAge;
flat out int effectKind;
flat out int effectFlags;
flat out int effectCount;
flat out uint effectWords[20];

// PROTOCOL
uint motionCells[48];
uint track(int offset,int bits){uint value=0u;for(int i=0;i<bits;i++)value|=((motionCells[(offset+i)/3]>>uint((offset+i)%3))&1u)<<uint(i);return value;}
float easeMotion(float u,int easing){if(easing==1)return 1.0-pow(1.0-u,3.0);if(easing==2)return u*u*(3.0-2.0*u);if(easing==3){float v=u-1.0;return 1.0+2.70158*v*v*v+1.70158*v*v;}return u;}
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    playerArmorFlags=-1;playerArmorPoint=vec2(0);playerArmorSize=vec2(0);
    itemClip = vec4(0.0);
    clipPoint = vec2(0.0);
    texCoord0 = UV0;
    vertexColor = Color;
    burstAge = -1.0;
    burstPoint = vec2(0.0);
    burstBounds = vec2(0.0);
    burstItem = vec3(0.0);
    effectKind = 0;
    effectFlags = 0;
    effectCount = 0;
    for(int i=0;i<20;i++)effectWords[i]=0u;

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
    bool playerArmor=all(equal(ivec3(greaterThan(signature[1].rgb,threshold)),ivec3(1,1,0)))&&all(equal(ivec3(greaterThan(signature[2].rgb,threshold)),ivec3(0,1,0)));
    for(int i=0;i<4;i++) {
        if(signature[i].a<0.9 || any(notEqual(ivec3(greaterThan(signature[i].rgb,threshold)),playerArmor?magic[i==1?2:i==2?1:i]:magic[i]))) return;
    }
    uint data=0u;
    for (int i=0; i<10; i++) {
        vec3 bits=step(threshold,texture(Sampler0,vec2((float(i+4)*3.0+1.5)/48.0,1.0-1.5/48.0)).rgb);
        data |= (uint(bits.r) | (uint(bits.g)<<1u) | (uint(bits.b)<<2u)) << uint(i*3);
    }
    vec2 offset=vec2(int(duiRead(uvec3(data,0u,0u),DUI_BASE_DX_OFFSET,DUI_BASE_DX_BITS))-1024,int(duiRead(uvec3(data,0u,0u),DUI_BASE_DY_OFFSET,DUI_BASE_DY_BITS))-1024);
    float size=float(duiRead(uvec3(data,0u,0u),DUI_BASE_SIZE_OFFSET,DUI_BASE_SIZE_BITS));
    if (size < 1.0) return;
    vec2 corner=vec2(UV0.x,1.0-UV0.y);
    vec2 itemOrigin=Position.xy-corner*48.0+vec2(16.0);
    gl_Position=ProjMat*ModelViewMat*vec4(itemOrigin+offset+corner*size,Position.z,1.0);
    // Crop the data frame and retain the central 36px native render.
    texCoord0=mix(vec2(6.0/48.0),vec2(42.0/48.0),UV0);
    if ((data & (1u<<uint(DUI_BASE_EXPANDED_OFFSET))) == 0u) return;
    uint low=0u,high=0u;
    for(int i=0;i<18;i++) {
        vec2 uv=i<16?vec2((float(i)*3.0+1.5)/48.0,1.5/48.0):vec2((43.5+float(i-16)*3.0)/48.0,1.0-1.5/48.0);
        vec3 rgb=step(threshold,texture(Sampler0,uv).rgb);
        uint cell=uint(rgb.r)|(uint(rgb.g)<<1u)|(uint(rgb.b)<<2u);
        if(i<10)low|=cell<<uint(i*3);else high|=cell<<uint((i-10)*3);
    }
    uvec3 header=uvec3(low,high,0u);
    float started=float(duiRead(header,DUI_HEADER_TICK_OFFSET,DUI_HEADER_TICK_BITS));
    burstBounds=vec2(float(duiRead(header,DUI_HEADER_WIDTH_OFFSET,9)),float(duiRead(header,DUI_HEADER_HEIGHT_OFFSET,9)));
    burstItem=vec3(float(duiRead(header,DUI_HEADER_X_OFFSET,9)),float(duiRead(header,DUI_HEADER_Y_OFFSET,9)),size);
    effectKind=int(duiRead(header,DUI_HEADER_KIND_OFFSET,DUI_HEADER_KIND_BITS));
    effectFlags=int(size);
    if(any(lessThan(burstBounds,vec2(1.0))) || any(greaterThan(burstBounds,vec2(480.0,360.0))))return;
    if(playerArmor){
        uint params=0u;for(int i=0;i<6;i++){vec3 rgb=step(threshold,texture(Sampler0,vec2(1.5/48.0,(7.5+float(i)*3.0)/48.0)).rgb);params|=(uint(rgb.r)|(uint(rgb.g)<<1u)|(uint(rgb.b)<<2u))<<uint(i*3);}
        playerArmorFlags=int(params);playerArmorSize=burstBounds;playerArmorPoint=corner*burstBounds;
        gl_Position=ProjMat*ModelViewMat*vec4(itemOrigin+offset+playerArmorPoint,Position.z,1);return;
    }
    if(effectKind==4 || effectKind==5){
        for(int i=0;i<48;i++){int side=i/12,col=i%12;float t=7.5+float(col)*3.0;vec2 uv=side==0?vec2(t,4.5):side==1?vec2(t,43.5):side==2?vec2(4.5,t):vec2(43.5,t);vec3 rgb=step(threshold,texture(Sampler0,uv/48.0).rgb);motionCells[i]=uint(rgb.r)|(uint(rgb.g)<<1u)|(uint(rgb.b)<<2u);}
        float elapsed=mod(GameTime*24000.0-started+24000.0,24000.0)-float(track(DUI_DELAY_OFFSET,DUI_DELAY_BITS));
        float u=track(DUI_ENABLED_OFFSET,DUI_ENABLED_BITS)!=0u?clamp(elapsed/max(1.0,float(track(DUI_DURATION_OFFSET,DUI_DURATION_BITS))),0.0,1.0):1.0;
        float e=easeMotion(u,int(track(DUI_EASING_OFFSET,DUI_EASING_BITS)));
        vec2 shift=vec2(int(track(DUI_TX_OFFSET,DUI_TX_BITS))-256,int(track(DUI_TY_OFFSET,DUI_TY_BITS))-256)*(1.0-e);
        float scale=mix(float(track(DUI_SCALEFROM_OFFSET,DUI_SCALEFROM_BITS)),float(track(DUI_SCALETO_OFFSET,DUI_SCALETO_BITS)),e)/64.0;
        float angle=radians(mix(float(int(track(DUI_ROTATEFROM_OFFSET,DUI_ROTATEFROM_BITS))-256),float(int(track(DUI_ROTATETO_OFFSET,DUI_ROTATETO_BITS))-256),e));
        vec2 pivot=vec2(float(track(DUI_PIVOTX_OFFSET,DUI_PIVOTX_BITS)),float(track(DUI_PIVOTY_OFFSET,DUI_PIVOTY_BITS)))/255.0;
        vec2 delta=(corner-pivot)*size*scale;delta=mat2(cos(angle),sin(angle),-sin(angle),cos(angle))*delta;
        clipPoint=itemOrigin+offset+pivot*size+delta+shift;gl_Position=ProjMat*ModelViewMat*vec4(clipPoint,Position.z,1.0);
        vertexColor.a*=clamp(mix(float(track(DUI_OPACITYFROM_OFFSET,DUI_OPACITYFROM_BITS)),float(track(DUI_OPACITYTO_OFFSET,DUI_OPACITYTO_BITS)),u)/255.0,0.0,1.0);
        if(effectKind==5){uint a=0u,b=0u;for(int i=0;i<13;i++){vec3 rgb=step(threshold,texture(Sampler0,vec2(46.5/48.0,(7.5+float(i)*3.0)/48.0)).rgb);uint cell=uint(rgb.r)|(uint(rgb.g)<<1u)|(uint(rgb.b)<<2u);if(i<10)a|=cell<<uint(i*3);else b|=cell<<uint((i-10)*3);}vec2 minPoint=itemOrigin+offset+vec2(int(a&1023u)-512,int((a>>10u)&1023u)-512);vec2 bounds=vec2(float((a>>20u)&511u),float(((a>>29u)&1u)|(b<<1u)));itemClip=vec4(minPoint,minPoint+bounds);}
        return;
    }
    if(effectKind==1 || effectKind==6){
        effectCount=clamp(int(burstItem.x),0,DUI_CARRIER_EFFECTS);
        for(int i=0;i<DUI_SHADER_CARRIER_BITS/3;i++){
            vec2 uv=(vec2(float(i%14),float(i/14))*3.0+vec2(4.5))/48.0;
            vec3 rgb=step(threshold,texture(Sampler0,uv).rgb);
            uint cell=uint(rgb.r)|(uint(rgb.g)<<1u)|(uint(rgb.b)<<2u);
            effectWords[i/10]|=cell<<uint((i%10)*3);
        }
    }
    burstAge=mod(GameTime*24000.0-started+24000.0,24000.0)/20.0;
    burstPoint=corner*burstBounds;
    vec2 canvasOrigin=itemOrigin+offset-((effectKind==1 || effectKind==6)?vec2(0.0):burstItem.xy);
    gl_Position=ProjMat*ModelViewMat*vec4(canvasOrigin+burstPoint,Position.z,1.0);
}
