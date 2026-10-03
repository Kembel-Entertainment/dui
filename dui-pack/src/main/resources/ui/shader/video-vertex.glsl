#moj_import <dui:video-symbols.glsl>
bool duiVideoVertex(){
 if(textureSize(Sampler0,0)!=ivec2(128)||ProjMat[3][3]>.5)return false;
 if(duiVideoRgb(texelFetch(Sampler0,ivec2(0,0),0))!=5862761u||duiVideoRgb(texelFetch(Sampler0,ivec2(1,0),0))!=7180417u||duiVideoRgb(texelFetch(Sampler0,ivec2(2,0),0))!=8365974u||duiVideoRgb(texelFetch(Sampler0,ivec2(3,0),0))!=4413519u)return false;
 if(duiVideoWord(4,1)!=1)return false;
 int fmt=duiVideoWord(5,1);if(fmt<0||fmt>2)return false;
 duiVideoFormat=fmt;duiVideoBackground=duiVideoWord(27,4);
 duiVideoSize=ivec2(duiVideoWord(14,2),duiVideoWord(16,2));
 vec2 corner=UV0;
 if(fmt==2){duiVideoPoint=vec2(0);gl_Position=vec4(corner.x*2.0-1.0,1.0-corner.y*2.0,.95,1);}
 else{
  vec2 source=vec2(duiVideoWord(6,2),duiVideoWord(8,2));
  vec2 lo=vec2(duiVideoWord(18,2),duiVideoWord(20,2))/4095.0*ScreenSize;
  vec2 hi=vec2(duiVideoWord(22,2),duiVideoWord(24,2))/4095.0*ScreenSize;
  float scale=min((hi.x-lo.x)/source.x,(hi.y-lo.y)/source.y);
  if(duiVideoWord(26,1)==1&&scale>=1.0)scale=floor(scale);
  vec2 origin=(lo+hi-source*scale)*.5;
  duiVideoPoint=corner*vec2(duiVideoSize);
  vec2 p=origin+(vec2(duiVideoWord(10,2),duiVideoWord(12,2))+duiVideoPoint)*scale;
  gl_Position=vec4(p.x/ScreenSize.x*2.0-1.0,1.0-p.y/ScreenSize.y*2.0,.96,1);
 }
 vertexColor=vec4(1);texCoord0=UV0;
 #if !defined(IS_SEE_THROUGH)
 sphericalVertexDistance=0;cylindricalVertexDistance=0;
 #endif
 return true;
}
