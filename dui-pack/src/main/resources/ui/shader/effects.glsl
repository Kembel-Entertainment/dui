// Reusable procedural primitives. Each component receives resolved bounds and typed parameters.
bool box(vec2 p,vec2 c,vec2 h){return all(lessThan(abs(p-c),h));}
float lineDistance(vec2 p,vec2 a,vec2 b){vec2 d=b-a;return length(p-a-d*clamp(dot(p-a,d)/dot(d,d),0.0,1.0));}
vec4 slotSymbol(vec2 q,int id){
    vec3 ink=vec3(.24,.10,.17),gold=vec3(1.0,.71,.16);vec4 c=vec4(0.0);
    if(id==0){
        float d=min(max(abs(q.x)-.68,abs(q.y+.58)-.18),lineDistance(q,vec2(.48,-.48),vec2(-.24,.73))-.20);
        if(d<.09)c=vec4(ink,1);if(d<.04)c=vec4(gold,1);if(d<-.02)c=vec4(.9,.13,.25,1);if(box(q,vec2(-.17,-.66),vec2(.47,.035)))c=vec4(1,.65,.55,1);
    } else if(id==1){
        float d=abs(q.x)*.88+abs(q.y)*.7;
        if(d<.79)c=vec4(ink,1);if(d<.70)c=vec4(q.x<0?vec3(.12,.72,.86):vec3(.13,.40,.80),1);
        if(d<.70&&q.y<-.10)c=vec4(.55,.98,1,1);if(d<.32)c=vec4(.25,.88,1,1);
    } else if(id==2){
        bool body=(length(vec2(q.x,q.y+.10))<.59&&q.y<.15)||box(q,vec2(0,.16),vec2(.58,.36));
        if(body||box(q,vec2(0,.5),vec2(.76,.16))||length(q-vec2(0,.72))<.16)c=vec4(ink,1);
        if((body&&abs(q.x)<.49)||box(q,vec2(0,.5),vec2(.66,.08)))c=vec4(q.x<0?vec3(1,.82,.22):vec3(.91,.49,.06),1);
        if(lineDistance(q,vec2(-.24,-.36),vec2(-.31,.13))<.05)c=vec4(1,1,.69,1);
    } else if(id==3){
        if(min(lineDistance(q,vec2(-.38,.28),vec2(.17,-.65)),lineDistance(q,vec2(.38,.38),vec2(.17,-.65)))<.055)c=vec4(.18,.48,.14,1);
        if(length((q-vec2(.36,-.60))*vec2(1,2.6))<.31)c=vec4(.26,.72,.28,1);
        for(int j=0;j<2;j++){vec2 v=q-vec2(j==0?-.37:.37,j==0?.32:.44);float d=length(v);if(d<.40)c=vec4(ink,1);if(d<.33)c=vec4(v.x<0?vec3(1,.18,.31):vec3(.69,.06,.19),1);if(length(v-vec2(-.1,-.12))<.075)c=vec4(1,.80,.67,1);}
    } else if(id==4){
        vec2 v=mat2(.93,-.36,.36,.93)*q;float d=length(v*vec2(1,1.45));
        if(d<.78)c=vec4(ink,1);if(d<.69)c=vec4(1,.79,.13,1);if(d<.53&&v.y<-.1)c=vec4(1,.98,.51,1);
    } else {
        if(box(q,vec2(0),vec2(.84,.48)))c=vec4(ink,1);if(box(q,vec2(0),vec2(.76,.40)))c=vec4(.38,.19,.37,1);
        // Three tiny 3x5 glyphs, B / A / R, embedded as bit masks.
        const int glyphs[3]=int[3](27566,23530,23470);
        for(int j=0;j<3;j++){vec2 v=(q-vec2(-.65+float(j)*.46,-.27))/.115;ivec2 cell=ivec2(floor(v));if(cell.x>=0&&cell.x<3&&cell.y>=0&&cell.y<5&&((glyphs[j]>>(cell.y*3+2-cell.x))&1)!=0)c=vec4(1,.90,.61,1);}
    }
    return c;
}
vec4 effectPixel(vec2 q,vec2 size,int kind,int a,int b,float age,bool motion,bool eventLive){
    if(any(lessThan(q,vec2(0)))||any(greaterThanEqual(q,size)))return vec4(0);
    float t=eventLive&&motion?max(0.0,age):100.0;vec4 c=vec4(0);
    if(kind==1){
        float shade=.92-.17*pow(abs(q.y/size.y-.5)*2.0,2.0);c=vec4(vec3(1,.96,.83)*shade,1);
        if(q.x<2.0||q.x>size.x-2.0)c=vec4(.70,.52,.35,1);
        int target=a&7,prev=(a>>3)&7;float duration=float(b&127)/20.0,symbolSize=float((b>>7)&127);
        float u=clamp(t/duration,0.0,1.0),distance=float((a>>6)&63)+mod(float(target-prev+6),6.0);
        float pos=float(prev)+distance*(1.0-pow(1.0-u,3.0));if(!eventLive||!motion||t>=duration)pos=float(target);
        float cell=symbolSize*2.4,yy=(q.y-size.y*.5)/cell+pos,row=floor(yy+.5);int id=int(mod(row,6.0));
        vec4 symbol=slotSymbol(vec2((q.x-size.x*.5)/symbolSize,(yy-row)*cell/symbolSize),id);if(symbol.a>0.0)c=symbol;
        c.rgb*=1.0-.20*pow(abs(q.y/size.y-.5)*2.0,6.0);
        if(abs(q.y-size.y*.5)<1.0&&(q.x<6.0||q.x>size.x-6.0))c=vec4(1,.63,.15,1);
    }else if(kind==2){
        vec2 pivot=size*vec2(.5,.86);float leverLength=size.y*.60,duration=float(a)/20.0;
        float pull=eventLive&&motion&&t<duration?sin(clamp(t/duration,0.0,1.0)*3.141593):0.0;
        vec2 knob=pivot+vec2(pull*size.x*.20,-leverLength+pull*leverLength*.76);
        float radius=min(size.x*.26,size.y*.15),stem=lineDistance(q,knob,pivot);
        if(stem<radius*.36)c=vec4(.18,.12,.20,1);if(stem<radius*.22)c=vec4(.70,.77,.80,1);if(stem<radius*.08)c=vec4(1,.96,.85,1);
        if(length((q-pivot)/(radius*vec2(.82,.45)))<1)c=vec4(.65,.42,.20,1);
        vec2 k=(q-knob)/radius;
        if(length(k)<1.15)c=vec4(.27,.07,.16,1);if(length(k)<1)c=vec4(mix(vec3(.66,.04,.16),vec3(1,.27,.32),clamp(.6-k.y*.4-k.x*.3,0.0,1.0)),1);
        if(length(k-vec2(-.3,-.35))<.23)c=vec4(1,.77,.62,1);
    }else if(kind==3){
        float delay=float((b>>9)&63)/10.0,elapsed=age-delay;int count=(a>>9)&63;
        if(eventLive&&motion&&elapsed>0.0&&elapsed<4.7){
            float scale=min(size.x/252.0,size.y/171.0);vec2 origin=vec2(float(a&511),float(b&511));
            for(int i=0;i<63;i++){
                if(i>=count)break;
                float seed=float(i),ct=elapsed-random(seed+84.0)*1.1;if(ct<0.0)continue;
                vec2 vel=vec2((random(seed+33.0)-.5)*220.0,-95.0-random(seed+16.0)*110.0)*scale;
                vec2 at=origin+vel*ct+vec2(0,85.0*scale*ct*ct),delta=q-at;float r=(3.8+random(seed+8.0)*1.8)*scale;
                if(any(greaterThan(abs(delta),vec2(r))))continue;
                vec2 coin=delta/vec2(r*max(.24,abs(cos(ct*8.0+seed))),r);float dist=length(coin);
                if(dist<1.0){vec3 paint=dist>.78?vec3(.66,.33,.07):dist>.63?vec3(1,.95,.55):vec3(1,.71,.16);if(abs(coin.x)<.12&&abs(coin.y)<.46)paint=vec3(1,.98,.66);c=vec4(paint,1.0-smoothstep(3.7,4.7,elapsed));}
            }
        }
    }else if(kind==5){
        float elapsed=age-float((b>>9)&63)/10.0;int count=(a>>9)&63;
        if(eventLive&&motion&&elapsed>0.0&&elapsed<3.6){
            vec2 origin=vec2(float(a&511),float(b&511));float scale=min(size.x/300.0,size.y/216.0);
            const vec3 colors[5]=vec3[5](vec3(.35,1,.73),vec3(1,.29,.56),vec3(1,.87,.39),vec3(.66,.55,1),vec3(1,.97,.85));
            for(int i=0;i<63;i++){
                if(i>=count)break;float seed=float(i),ct=elapsed-random(seed+31.0)*.18;if(ct<0.0)continue;
                vec2 vel=vec2((random(seed+23.0)-.5)*260.0,-110.0-random(seed+14.0)*90.0)*scale;
                vec2 at=origin+vel*ct+vec2(0,75.0*scale*ct*ct),d=q-at;
                if(any(greaterThan(abs(d),vec2(4.0*scale))))continue;
                float angle=ct*(3.0+random(seed+44.0)*8.0)+seed;
                d=mat2(cos(angle),sin(angle),-sin(angle),cos(angle))*d;
                vec2 halfSize=vec2(2.5*max(.3,abs(cos(ct*9.0+seed))),1.4)*scale;
                if(all(lessThan(abs(d),halfSize)))c=vec4(colors[i%5],1.0-smoothstep(2.8,3.6,elapsed));
            }
        }
    }else if(kind==4){
        float radius=float(b);for(int i=0;i<32;i++){
            if(i>=a)break;vec2 center=vec2(a==1?size.x*.5:radius+float(i)*(size.x-radius*2.0)/float(a-1),size.y*.5);
            if(length(q-center)<radius){float on=motion&&eventLive?step(.1,sin(t*9.0-float(i))):1.0;c=vec4(mix(vec3(.57,.30,.15),vec3(1,.90,.48),on),1);}
        }
    }
    return c;
}
vec4 shaderEffects(vec2 p,float age,int flags){
    vec4 color=vec4(0);for(int i=0;i<8;i++){
        if(i>=effectCount)break;uvec3 data=effectData[i];
        int kind=int(data.x&7u);vec2 origin=vec2(float((data.x>>3u)&511u),float((data.x>>12u)&511u));
        vec2 size=vec2(float((data.x>>21u)&511u),float(data.y&511u));
        int a=int((data.y>>9u)&32767u),b=int(((data.y>>24u)&63u)|(data.z<<6u));
        vec4 paint=effectPixel(p-origin,size,kind,a,b,age,(flags&4)!=0,(flags&8)!=0);
        if(paint.a>0.0){float alpha=paint.a+color.a*(1.0-paint.a);color=vec4((paint.rgb*paint.a+color.rgb*color.a*(1.0-paint.a))/max(alpha,.0001),alpha);}
    }
    return color;
}
