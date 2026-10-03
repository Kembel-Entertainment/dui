// Generic version-three shader invocation transport. No application drawing lives here.
uint duiShaderRead(int offset,int bits){
    int word=offset/30,shift=offset%30;uint value=effectWords[word]>>uint(shift);
    if(shift+bits>30)value|=effectWords[word+1]<<uint(30-shift);
    return value&((1u<<uint(bits))-1u);
}
float duiEase(float u,int easing){if(easing==1)return 1.0-pow(1.0-u,3.0);if(easing==2)return u*u*(3.0-2.0*u);if(easing==3){float v=u-1.0;return 1.0+2.70158*v*v*v+1.70158*v*v;}return u;}
// SHADER_DEFINITIONS
vec4 shaderEffects(vec2 point,float age,int flags){
    vec4 result=vec4(0);int cursor=0;bool live=(flags&8)!=0,moving=(flags&4)!=0;
    for(int i=0;i<DUI_CARRIER_EFFECTS;i++){
        if(i>=effectCount)break;
        int code=int(duiShaderRead(cursor,DUI_SHADER_DEFINITION_BITS));cursor+=DUI_SHADER_DEFINITION_BITS;
        vec2 origin=vec2(float(duiShaderRead(cursor,9)),float(duiShaderRead(cursor+9,9)));cursor+=18;
        vec2 size=vec2(float(duiShaderRead(cursor,9)),float(duiShaderRead(cursor+9,9)));cursor+=18;
        int count=duiShaderSize(code);if(count<0 || cursor+count>DUI_SHADER_CARRIER_BITS)break;
        int parameters=cursor;cursor+=count;vec2 q=point-origin;float alpha=1.0;
        if(effectKind==6){
            if(cursor+DUI_SHADER_MOTION_BITS>DUI_SHADER_CARRIER_BITS)break;
            float ticks=age*20.0-float(duiShaderRead(cursor+DUI_DELAY_OFFSET,DUI_DELAY_BITS));
            bool enabled=moving&&live&&duiShaderRead(cursor+DUI_ENABLED_OFFSET,DUI_ENABLED_BITS)!=0u;
            float u=enabled?clamp(ticks/max(1.0,float(duiShaderRead(cursor+DUI_DURATION_OFFSET,DUI_DURATION_BITS))),0.0,1.0):1.0;
            float e=duiEase(u,int(duiShaderRead(cursor+DUI_EASING_OFFSET,DUI_EASING_BITS)));
            vec2 shift=vec2(int(duiShaderRead(cursor+DUI_TX_OFFSET,DUI_TX_BITS))-256,int(duiShaderRead(cursor+DUI_TY_OFFSET,DUI_TY_BITS))-256)*(1.0-e);
            float scale=mix(float(duiShaderRead(cursor+DUI_SCALEFROM_OFFSET,DUI_SCALEFROM_BITS)),float(duiShaderRead(cursor+DUI_SCALETO_OFFSET,DUI_SCALETO_BITS)),e)/64.0;
            float angle=radians(mix(float(int(duiShaderRead(cursor+DUI_ROTATEFROM_OFFSET,DUI_ROTATEFROM_BITS))-256),float(int(duiShaderRead(cursor+DUI_ROTATETO_OFFSET,DUI_ROTATETO_BITS))-256),e));
            vec2 pivot=vec2(float(duiShaderRead(cursor+DUI_PIVOTX_OFFSET,DUI_PIVOTX_BITS)),float(duiShaderRead(cursor+DUI_PIVOTY_OFFSET,DUI_PIVOTY_BITS)))/255.0;
            alpha=mix(float(duiShaderRead(cursor+DUI_OPACITYFROM_OFFSET,DUI_OPACITYFROM_BITS)),float(duiShaderRead(cursor+DUI_OPACITYTO_OFFSET,DUI_OPACITYTO_BITS)),u)/255.0;
            if(scale<=0.0001){cursor+=DUI_SHADER_MOTION_BITS;continue;}
            q=mat2(cos(angle),-sin(angle),sin(angle),cos(angle))*(q-pivot*size-shift)/scale+pivot*size;
            cursor+=DUI_SHADER_MOTION_BITS;
        }
        // Each invocation owns its local rectangle, including after inverse motion.
        // Opaque consumer shaders must never cover adjacent components or controls.
        if(any(lessThan(q,vec2(0))) || any(greaterThanEqual(q,size)))continue;
        vec4 c=duiShaderPixel(q,size,code,parameters,moving&&live?max(0.0,age):100.0,moving&&live);c.a*=clamp(alpha,0.0,1.0);
        float a=c.a+result.a*(1.0-c.a);
        result=vec4((c.rgb*c.a+result.rgb*result.a*(1.0-c.a))/max(a,.00001),a);
    }
    return result;
}
