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

float roundRect(vec2 p,vec2 halfSize,float radius){vec2 d=abs(p)-halfSize+radius;return length(max(d,vec2(0)))+min(max(d.x,d.y),0.0)-radius;}
vec3 cardPalette(int id){return id==1?vec3(.19,.64,.53):id==2?vec3(.89,.38,.43):id==3?vec3(.56,.42,.77):vec3(.15,.29,.37);}
bool rankGlyph(vec2 p,int rank){
    // Compact 3x5 lettering; T denotes ten, preserving legibility at small GUI scales.
    int bits=0;
    if(rank==2)bits=7|(4<<3)|(7<<6)|(1<<9)|(7<<12);
    if(rank==3)bits=7|(4<<3)|(7<<6)|(4<<9)|(7<<12);
    if(rank==4)bits=5|(5<<3)|(7<<6)|(4<<9)|(4<<12);
    if(rank==5)bits=7|(1<<3)|(7<<6)|(4<<9)|(7<<12);
    if(rank==6)bits=7|(1<<3)|(7<<6)|(5<<9)|(7<<12);
    if(rank==7)bits=7|(4<<3)|(4<<6)|(2<<9)|(2<<12);
    if(rank==8)bits=7|(5<<3)|(7<<6)|(5<<9)|(7<<12);
    if(rank==9)bits=7|(5<<3)|(7<<6)|(4<<9)|(7<<12);
    if(rank==10)bits=7|(2<<3)|(2<<6)|(2<<9)|(2<<12);
    if(rank==11)bits=4|(4<<3)|(4<<6)|(5<<9)|(7<<12);
    if(rank==12)bits=7|(5<<3)|(5<<6)|(3<<9)|(6<<12);
    if(rank==13)bits=5|(5<<3)|(3<<6)|(5<<9)|(5<<12);
    if(rank==14)bits=2|(5<<3)|(7<<6)|(5<<9)|(5<<12);
    ivec2 cell=ivec2(floor(p));return cell.x>=0&&cell.x<3&&cell.y>=0&&cell.y<5&&((bits>>(cell.y*3+cell.x))&1)!=0;
}
bool cardSuit(vec2 p,int suit){
    if(suit==1)return abs(p.x)*.8+abs(p.y)<.83;
    if(suit==2)return (length(p-vec2(-.32,-.25))<.43||length(p-vec2(.32,-.25))<.43||(p.y>=-.18&&p.y<.78&&abs(p.x)<(.78-p.y)*.85));
    if(suit==3){vec2 h=vec2(p.x,-p.y);return (length(h-vec2(-.32,-.12))<.39||length(h-vec2(.32,-.12))<.39||(h.y>=-.12&&h.y<.85&&abs(h.x)<(.85-h.y)*.75))||box(p,vec2(0,.60),vec2(.13,.25));}
    return length(p-vec2(0,-.40))<.34||length(p-vec2(-.34,.12))<.35||length(p-vec2(.34,.12))<.35||box(p,vec2(0,.56),vec2(.12,.28));
}
vec4 playingCard(vec2 q,vec2 size,int a,int b,float t,bool live){
    int id=a&63,mode=(a>>13)&3,palette=(b>>13)&3;bool down=((a>>6)&1)!=0,highlighted=((a>>7)&1)!=0;
    float delay=float((a>>8)&31)/10.0,duration=max(.05,float(b&127)/20.0),lift=float((b>>7)&63);
    float u=live?clamp((t-delay)/duration,0.0,1.0):1.0;
    if(mode==1&&live&&t<delay)return vec4(0);
    float h=size.y-lift-6.0,w=min(size.x-4.0,h*.66),compression=1.0,angle=0.0;
    vec2 center=vec2(size.x*.5,lift+h*.5+1.0);
    bool back=down;
    if(mode==1){center.y-=lift*pow(1.0-u,3.0);angle=(1.0-u)*-.14;}
    if(mode==2){compression=max(.045,abs(cos(u*3.141593)));if(u<.5)back=!down;center.y-=sin(u*3.141593)*3.0;}
    vec2 p=q-center;p=mat2(cos(angle),sin(angle),-sin(angle),cos(angle))*p;
    p.x/=compression;vec2 halfSize=vec2(w,h)*.5;float r=max(1.1,w*.075),dist=roundRect(p,halfSize,r);
    vec4 c=vec4(0);
    float shadow=roundRect((q-center-vec2(1.3,2.8))/vec2(compression,1),halfSize,r);
    if(shadow<1.0)c=vec4(.025,.04,.055,.55*(1.0-smoothstep(-1.0,1.0,shadow)));
    if(dist<=0.0){
        c=vec4(mix(vec3(.96,.92,.83),vec3(1,.985,.94),clamp(.6-p.y/h,0.0,1.0)),1);
        if(dist>-.8)c.rgb=highlighted?vec3(1,.80,.39):vec3(.72,.69,.60);
        if(back){
            c.rgb=cardPalette(palette);if(dist> -1.6)c.rgb=vec3(.94,.78,.46);
            float weave=mod(floor((p.x+p.y)/3.0)+floor((p.x-p.y)/3.0),2.0);
            if(dist< -2.5)c.rgb*=.88+weave*.15;
            float diamond=abs(p.x)*.8+abs(p.y)*.6;
            if(diamond<w*.25&&diamond>w*.19)c.rgb=vec3(.95,.82,.55);
            if(length(p)<w*.08)c.rgb=vec3(.93,.96,.86);
        }else if(id<52){
            int rank=id%13+2,suit=id/13;vec3 ink=(suit==1||suit==2)?vec3(.86,.25,.33):vec3(.13,.22,.29);
            float unit=max(.85,w/27.0);vec2 corner=p+halfSize-vec2(2.4,2.4);
            if(rankGlyph(corner/unit,rank)||cardSuit((corner-vec2(1.5,7.0)*unit)/(1.9*unit),suit))c.rgb=ink;
            vec2 bottom=-p+halfSize-vec2(2.4,2.4);
            if(rankGlyph(bottom/unit,rank)||cardSuit((bottom-vec2(1.5,7.0)*unit)/(1.9*unit),suit))c.rgb=ink;
            if(cardSuit(p/(w*.24),suit))c.rgb=ink;
            if(rank>=11&&rank<=13){float ring=abs(p.x)*.8+abs(p.y)*.55;if(ring>w*.31&&ring<w*.34)c.rgb=vec3(.79,.58,.27);}
        }else{
            c.rgb=vec3(.06,.20,.21);if(dist>-.7)c.rgb=vec3(.28,.44,.40);
            if(abs(p.x)*.8+abs(p.y)*.5<w*.15)c.rgb=vec3(.32,.49,.43);
        }
        if(highlighted&&dist<-.9&&dist>-2.0)c.rgb=vec3(1,.82,.43);
    }
    return c;
}
vec2 chipAnchor(int id,vec2 size){
    vec2 v=id==0?vec2(.13,.14):id==1?vec2(.87,.14):id==2?vec2(.13,.86):id==3?vec2(.87,.86):id==4?vec2(.5,.14):id==5?vec2(.5,.86):id==6?vec2(.13,.5):vec2(.87,.5);return v*size;
}
vec4 chipDisc(vec2 p,float radius,vec3 paint){
    vec4 c=vec4(0);float d=length(p/vec2(radius,radius*.44));
    float edge=length((p-vec2(0,1.5))/vec2(radius,radius*.44));
    if(edge<1.06)c=vec4(paint*.42,1);
    if(d<1.0){c=vec4(paint,1);float angle=atan(p.y/.44,p.x);if(d>.73&&d<.95&&cos(angle*6.0)>.0)c.rgb=vec3(1,.94,.79);if(d<.54&&d>.42)c.rgb=vec3(1,.94,.79);if(d<.32)c.rgb=mix(paint,vec3(1,.96,.85),.23);}
    return c;
}
vec4 chipStack(vec2 q,vec2 size,int a,int b,float t,bool live){
    int count=a&31,palette=(a>>5)&3,mode=(b>>7)&3;float duration=max(.05,float(b&127)/20.0),delay=float((b>>9)&63)/10.0;
    if(count==0)return vec4(0);
    vec3 paint=palette==1?vec3(.35,.83,.66):palette==2?vec3(.94,.36,.43):palette==3?vec3(.60,.45,.87):vec3(.96,.73,.33);
    vec2 dest=chipAnchor((a>>10)&7,size),source=chipAnchor((a>>7)&7,size);float radius=clamp(min(size.x,size.y)*.085,2.2,5.2);vec4 c=vec4(0);
    int layers=min(count,7);
    for(int j=0;j<7;j++){
        if(j>=layers)break;
        float u=live&&mode==1?clamp((t-delay-float(j)*.035)/duration,0.0,1.0):1.0;
        vec2 at=mode==1?mix(source,dest,1.0-pow(1.0-u,3.0)):size*.5;
        if(mode==1)at.y-=sin(u*3.141593)*min(18.0,size.y*.25);
        at+=vec2(sin(float(j)*2.0)*(1.0-u)*radius*.5,-float(j)*1.1);
        vec4 chip=chipDisc(q-at,radius,paint);if(chip.a>0)c=chip;
    }
    return c;
}

vec4 effectPixel(vec2 q,vec2 size,int kind,int a,int b,float age,bool motion,bool eventLive){
    if(any(lessThan(q,vec2(0)))||any(greaterThanEqual(q,size)))return vec4(0);
    float t=eventLive&&motion?max(0.0,age):100.0;vec4 c=vec4(0);
    if(kind==6)return playingCard(q,size,a,b,t,eventLive&&motion);
    if(kind==7)return chipStack(q,size,a,b,t,eventLive&&motion);
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
