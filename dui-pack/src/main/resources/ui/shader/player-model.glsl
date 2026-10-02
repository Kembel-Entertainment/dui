// PLAYER_RENDERERS
// Shared orthographic cuboid renderer. Skin UVs use Minecraft's normalized 64x64 layout.
mat3 playerYaw(float a){float c=cos(a),s=sin(a);return mat3(c,0,-s,0,1,0,s,0,c);}
mat3 playerPitch(float a){float c=cos(a),s=sin(a);return mat3(1,0,0,0,c,s,0,-s,c);}
bool playerBox(vec3 ro,vec3 rd,vec3 halfSize,out float t,out vec3 normal,out vec3 point){
    vec3 safe=sign(rd)*max(abs(rd),vec3(.00001));safe=mix(vec3(.00001),safe,notEqual(rd,vec3(0)));
    vec3 a=(-halfSize-ro)/safe,b=(halfSize-ro)/safe;
    vec3 near=min(a,b),far=max(a,b);float enter=max(max(near.x,near.y),near.z),leave=min(min(far.x,far.y),far.z);
    if(leave<max(enter,0.0))return false;
    t=max(enter,0.0);point=ro+rd*t;
    normal=near.x>=near.y&&near.x>=near.z?vec3(-sign(rd.x),0,0):near.y>=near.z?vec3(0,-sign(rd.y),0):vec3(0,0,-sign(rd.z));return true;
}
vec2 playerUv(vec3 p,vec3 normal,vec3 dim,vec2 origin){
    vec3 f=p/dim+vec3(.5);float w=dim.x,h=dim.y,d=dim.z;
    if(normal.z>.5)return origin+vec2(d+f.x*w,d+(1.0-f.y)*h);
    if(normal.z<-.5)return origin+vec2(2.0*d+w+(1.0-f.x)*w,d+(1.0-f.y)*h);
    if(normal.x<-.5)return origin+vec2(f.z*d,d+(1.0-f.y)*h);
    if(normal.x>.5)return origin+vec2(d+w+(1.0-f.z)*d,d+(1.0-f.y)*h);
    if(normal.y>.5)return origin+vec2(d+f.x*w,(1.0-f.z)*d);
    return origin+vec2(d+w+f.x*w,f.z*d);
}
void playerPart(int i,bool slim,out vec3 center,out vec3 dim,out vec2 uv,out vec2 outer){
    if(i==0){center=vec3(0,28,0);dim=vec3(8);uv=vec2(0);outer=vec2(32,0);}
    else if(i==1){center=vec3(0,18,0);dim=vec3(8,12,4);uv=vec2(16,16);outer=vec2(16,32);}
    else if(i==2){center=vec3(slim?-5.5:-6.0,18,0);dim=vec3(slim?3:4,12,4);uv=vec2(40,16);outer=vec2(40,32);}
    else if(i==3){center=vec3(slim?5.5:6.0,18,0);dim=vec3(slim?3:4,12,4);uv=vec2(32,48);outer=vec2(48,48);}
    else if(i==4){center=vec3(-2,6,0);dim=vec3(4,12,4);uv=vec2(0,16);outer=vec2(0,32);}
    else{center=vec3(2,6,0);dim=vec3(4,12,4);uv=vec2(16,48);outer=vec2(0,48);}
}
void playerRay(vec2 q,vec2 size,int flags,out vec3 ro,out vec3 rd){
    vec4 view;vec2 idle;float limbs[6];if(!duiPlayerPose((flags>>10)&15,(flags>>2)&7,view,idle,limbs)){ro=vec3(0);rd=vec3(0,0,-1);return;}
    float scale=size.y/view.z;vec2 p=(q-size*.5)/scale;
    mat3 camera=playerYaw(view.x)*playerPitch(view.y);
    ro=camera*vec3(p.x,-p.y,60)+vec3(0,16,0);rd=camera*vec3(0,0,-1);
}
vec4 playerSkinPixel(sampler2D skin,vec2 q,vec2 size,int flags,float time){
    bool slim=(flags&32)!=0,outerLayer=(flags&64)!=0,idle=(flags&128)!=0;
    vec3 ro,rd;playerRay(q,size,flags,ro,rd);
    vec4 view;vec2 idleParams;float limbs[6];duiPlayerPose((flags>>10)&15,(flags>>2)&7,view,idleParams,limbs);
    float nearest=1e10;vec4 result=vec4(0);
    for(int i=0;i<6;i++){
        vec3 center,dim;vec2 uv,outer;playerPart(i,slim,center,dim,uv,outer);
        mat3 pose=playerPitch(limbs[i]+(idle&&i>=2&&i<=3?sin(time*idleParams.y)*idleParams.x*(i==2?1.0:-1.0):0.0));
        if(idle)center.y+=sin(time*idleParams.y)*.08;
        for(int layer=0;layer<2;layer++){
            if(layer==1&&!outerLayer)continue;
            vec3 expanded=dim+vec3(layer==1?(i==0?1.0:.5):0.0),n,p;float t;
            if(!playerBox(transpose(pose)*(ro-center),transpose(pose)*rd,expanded*.5,t,n,p)||t>=nearest)continue;
            vec2 tex=playerUv(p*dim/expanded,n,dim,layer==0?uv:outer);
            vec4 color=texelFetch(skin,ivec2(clamp(floor(tex),vec2(0),vec2(63))),0);
            if(color.a<.1)continue;
            nearest=t;float light=.65+.35*max(0.0,dot(pose*n,normalize(vec3(-.45,.8,1))));
            result=vec4(color.rgb*light,color.a);
        }
    }
    return result;
}
vec4 playerArmorPixel(sampler2D atlas,vec2 q,vec2 size,int flags,float time){
    bool slim=(flags&32)!=0,idle=(flags&128)!=0;int slot=(flags>>8)&3;
    vec3 ro,rd;playerRay(q,size,flags,ro,rd);
    vec4 view;vec2 idleParams;float limbs[6];duiPlayerPose((flags>>10)&15,(flags>>2)&7,view,idleParams,limbs);
    float nearest=1e10,bodyNearest=1e10;vec4 result=vec4(0);
    for(int i=0;i<6;i++){
        vec3 center,dim;vec2 uv,outer;playerPart(i,slim,center,dim,uv,outer);
        mat3 pose=playerPitch(limbs[i]+(idle&&i>=2&&i<=3?sin(time*idleParams.y)*idleParams.x*(i==2?1.0:-1.0):0.0));
        if(idle)center.y+=sin(time*idleParams.y)*.08;
        vec3 n,p;float t;if(playerBox(transpose(pose)*(ro-center),transpose(pose)*rd,dim*.5,t,n,p))bodyNearest=min(bodyNearest,t);
    }
    for(int i=0;i<6;i++){
        if(slot==0&&i!=0||slot==1&&(i<1||i>3)||slot==2&&i!=1&&i<4||slot==3&&i<4)continue;
        vec3 center,dim;vec2 uv,outer;playerPart(i,slim,center,dim,uv,outer);
        if(i==3){uv=vec2(40,16);}if(i>=4)uv=vec2(0,16);
        if(i==2||i==3)dim.x=4.0;
        mat3 pose=playerPitch(limbs[i]+(idle&&i>=2&&i<=3?sin(time*idleParams.y)*idleParams.x*(i==2?1.0:-1.0):0.0));
        if(idle)center.y+=sin(time*idleParams.y)*.08;
        vec3 expanded=dim+vec3(slot==2?1.0:2.0),n,p;float t;
        if(!playerBox(transpose(pose)*(ro-center),transpose(pose)*rd,expanded*.5,t,n,p)||t>=nearest||t>bodyNearest+.001)continue;
        if(i==3||i==5)p.x=-p.x;
        vec2 tex=playerUv(p*dim/expanded,n,dim,uv)/vec2(64,32);
        vec2 samplePoint=(vec2(6)+tex*36.0)/48.0;samplePoint.y=1.0-samplePoint.y;
        vec4 color=texture(atlas,samplePoint);if(color.a<.1)continue;
        nearest=t;float light=.65+.35*max(0.0,dot(pose*n,normalize(vec3(-.45,.8,1))));result=vec4(color.rgb*light,color.a);
    }
    return result;
}
