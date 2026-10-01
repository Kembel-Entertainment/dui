#version 330

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 clipPoint;
flat in vec4 itemClip;
in vec2 texCoord0;
in vec4 vertexColor;
in vec2 burstPoint;
flat in vec2 burstBounds;
flat in vec3 burstItem;
flat in float burstAge;
flat in int effectKind;
flat in int effectFlags;
flat in int effectCount;
flat in uvec3 effectData[8];
flat in uvec3 effectMotionA[2];
flat in uvec2 effectMotionB[2];

out vec4 fragColor;

float random(float n) { return fract(sin(n*127.1+311.7)*43758.5453); }
// PROTOCOL
// EFFECT_FUNCTIONS

void main() {
    if((effectKind==3 || effectKind==5) && (any(lessThan(clipPoint,itemClip.xy)) || any(greaterThanEqual(clipPoint,itemClip.zw))))discard;
    if(effectKind==1 || effectKind==6 || effectKind==7){vec4 panel=shaderEffects(burstPoint,burstAge,effectFlags);if(panel.a==0.0)discard;fragColor=panel*ColorModulator;return;}
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if(burstAge>=0.0) {
        // Repaint the native mascot inside an expanded, otherwise transparent canvas quad.
        vec2 local=(burstPoint-burstItem.xy)/burstItem.z;
        color=vec4(0.0);
        if(all(greaterThanEqual(local,vec2(0.0))) && all(lessThanEqual(local,vec2(1.0)))) {
            vec2 uv=mix(vec2(6.0/48.0),vec2(42.0/48.0),vec2(local.x,1.0-local.y));
            color=texture(Sampler0,uv)*vertexColor;
        }
        // Two cartoon confetti cannons. One finite burst, driven by world ticks, no per-frame packets.
        if(burstAge<4.8) {
            const vec3 palette[6]=vec3[6](vec3(1.0,.30,.51),vec3(1.0,.73,.14),vec3(.16,.79,.59),
                vec3(.22,.64,1.0),vec3(.65,.39,.97),vec3(1.0,.49,.20));
            float scale=burstBounds.x/480.0;
            for(int i=0;i<54;i++) {
                float seed=float(i),t=burstAge-random(seed+4.0)*.24;
                if(t<0.0)continue;
                float side=mod(seed,2.0),direction=side<.5?1.0:-1.0;
                vec2 origin=vec2(mix(.13,.87,side),.81)*burstBounds;
                vec2 velocity=vec2(direction*(.07+random(seed+13.0)*.24)*burstBounds.x,
                    -(.70+random(seed+27.0)*.44)*burstBounds.y);
                vec2 pos=origin+velocity*t+vec2(sin(t*3.0+seed)*5.0*scale,.27*burstBounds.y*t*t);
                vec2 delta=burstPoint-pos;
                // Reject distant fragments before the rotation and shape work.
                if(any(greaterThan(abs(delta),vec2(8.0*scale))))continue;
                float angle=seed+t*(2.0+random(seed+2.0)*5.0);
                vec2 q=mat2(cos(angle),-sin(angle),sin(angle),cos(angle))*delta;
                vec2 halfSize=vec2((2.2+random(seed)*1.2)*max(.25,abs(cos(t*5.0+seed))),3.8+random(seed+1.0)*1.8)*scale;
                if(any(greaterThan(abs(q),halfSize)))continue;
                float fade=(1.0-smoothstep(3.5,4.8,burstAge))*smoothstep(0.0,.08,t);
                vec3 paint=palette[i%6];
                if(any(greaterThan(abs(q),halfSize-vec2(.65*scale))))paint*=.65;
                // Straight-alpha composition also retains the mascot underneath falling paper.
                float alpha=fade+color.a*(1.0-fade);
                color=vec4((paint*fade+color.rgb*color.a*(1.0-fade))/max(alpha,.0001),alpha);
            }
        }
    }
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
