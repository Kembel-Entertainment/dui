// Perspective-only, colour AND texture-stamp gated. Geometry is generated from pack metadata.
bool duiWorldScene() {
    ivec4 code=ivec4(round(Color*255.0));
    if(ProjMat[2][3]!=-1.0) return false;
    ivec2 atlasSize=textureSize(Sampler0,0);
    ivec3 stamp=ivec3(round(texelFetch(Sampler0,ivec2(UV0*vec2(atlasSize)),0).rgb*255.0));
    bool dynamicLayer=stamp.r==DUI_MAP_DYNAMIC_STAMP_COLOR;
    if(!dynamicLayer && (code.r!=DUI_MAP_WORLD_COLOR || length(Position)<.90 || length(Position)>1.10 || stamp.r!=DUI_MAP_WORLD_COLOR || stamp.g<DUI_MAP_CORNER_BASE || stamp.b<DUI_MAP_CORNER_BASE || stamp.b>DUI_MAP_CORNER_BASE+1))return false;
    if(dynamicLayer && (length(Position.xz)<.90 || length(Position.xz)>1.10 || Position.y<-.5 || Position.y>16777215.5))return false;
    int map=dynamicLayer?stamp.g/2:(stamp.g-DUI_MAP_CORNER_BASE)/2;
    vec2 corner=dynamicLayer?vec2(stamp.g&1,stamp.b&1):vec2((stamp.g-DUI_MAP_CORNER_BASE)%2,stamp.b-DUI_MAP_CORNER_BASE);
    vec2 mapSize;float pixels,minimum,step;vec4 opening;int easing;
    vec4 rect;int space;float depth;vec3 alpha;
    if(!duiMapDefinition(map,mapSize,pixels,minimum,step,opening,easing) || !duiMapLayer(map,dynamicLayer?stamp.b/2:code.b&127,rect,space,depth,alpha)) return false;
    uint geometry=dynamicLayer?uint(round(Position.y)):0u;
    int zoom=dynamicLayer?int((geometry>>18u)&31u):code.g>>3;
    bool reduced=dynamicLayer?(geometry&(1u<<23u))!=0u:(code.b&128)!=0;
    if(dynamicLayer){int rgb=(code.r<<16)|(code.g<<8)|code.b;rect=vec4(float((rgb>>12)&4095)-2048.0,float(rgb&4095)-2048.0,float((geometry&511u)+1u),float(((geometry>>9u)&511u)+1u));space=0;}
    vec3 forward=transpose(mat3(ModelViewMat))*vec3(0,0,-1);
    float yaw=degrees(atan(-forward.x,forward.z));
    float pitch=degrees(asin(clamp(-forward.y,-1.0,1.0)));
    float reference=degrees(atan(-Position.x,Position.z));
    float relative=mod(yaw-reference+180.0,360.0)-180.0;
    vec2 cursor=mapSize*.5+vec2(relative,pitch)*pixels;
    float time=GameTime*24000.0;
    float open=1.0,opacity=1.0;
    if(!dynamicLayer && code.a>=DUI_MAP_OPENING_OFFSET && code.a<DUI_MAP_OPENING_OFFSET+DUI_MAP_OPENING_MODULO) {
        float age=mod(time-float(code.a-DUI_MAP_OPENING_OFFSET)+float(DUI_MAP_OPENING_MODULO),float(DUI_MAP_OPENING_MODULO));
        float u=clamp(age/opening.x,0.0,1.0);open=u;
        if(easing==1)open=1.0-pow(1.0-u,3.0);else if(easing==2)open=u*u*(3.0-2.0*u);else if(easing==3){float v=u-1.0;open=1.0+2.70158*v*v*v+1.70158*v*v;}
        opacity=mix(opening.z,opening.w,u);
    }
    if(space==1)gl_Position=vec4((rect.xy+(corner-.5)*rect.zw)*vec2(2,-2)+vec2(-1,1),depth,1);
    else {
        float height=minimum+float(zoom)*step;float aspect=ProjMat[1][1]/ProjMat[0][0];
        vec2 ndc=(rect.xy+(corner-.5)*rect.zw-cursor)/vec2(height*aspect,height)*vec2(2,-2);
        ndc*=mix(opening.y,1.0,open);gl_Position=vec4(ndc,depth,1);
    }
    opacity*=(reduced||alpha.z==0.0)?alpha.y:mix(alpha.x,alpha.y,(sin(time*alpha.z)+1.0)*.5);
    if(dynamicLayer)opacity*=float(code.a)/255.0;
    vertexColor=vec4(1,1,1,opacity);
#if !defined(IS_SEE_THROUGH)
    sphericalVertexDistance=0;cylindricalVertexDistance=0;
#endif
    texCoord0=UV0+(1.0-2.0*corner)/vec2(atlasSize);
    return true;
}
