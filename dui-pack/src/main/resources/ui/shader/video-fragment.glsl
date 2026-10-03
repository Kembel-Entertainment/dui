#moj_import <dui:video-symbols.glsl>
vec4 duiVideoPixel(){
 if(duiVideoFormat==2){uint c=uint(duiVideoBackground);return vec4(vec3(float(c>>16u),float((c>>8u)&255u),float(c&255u))/255.0,1);}
 ivec2 p=ivec2(clamp(floor(duiVideoPoint),vec2(0),vec2(duiVideoSize)-vec2(1)));
 int n=duiVideoFormat==0?2:4;uint v=0u;
 for(int i=n-1;i>=0;i--){int d=duiVideoSymbol(texelFetch(Sampler0,ivec2(p.x*n+i,p.y+1),0));if(d<0)return vec4(1,0,1,1);v=v*192u+uint(d);}
 uvec3 rgb;
 if(duiVideoFormat==0){rgb=uvec3(v&31u,(v>>5u)&31u,(v>>10u)&31u);rgb=(rgb<<3u)|(rgb>>2u);}
 else rgb=uvec3(v>>16u,(v>>8u)&255u,v&255u);
 return vec4(vec3(rgb)/255.0,1);
}
