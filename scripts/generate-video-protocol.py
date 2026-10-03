import subprocess,re,pathlib,argparse,os
parser=argparse.ArgumentParser()
parser.add_argument('--minecraft-jar',type=pathlib.Path,required=True)
parser.add_argument('--check',action='store_true')
args=parser.parse_args()
root=pathlib.Path(__file__).resolve().parent.parent
javap=str(pathlib.Path(os.environ['JAVA_HOME'])/'bin/javap') if os.environ.get('JAVA_HOME') else 'javap'
s=subprocess.check_output([javap,'-classpath',str(args.minecraft_jar),'-c','-p','net.minecraft.world.level.material.MapColor'],text=True).split('static {};')[1]
colors=[0]
def const(line):
 for p in [r'// int (-?\d+)',r'iconst_(\d)',r'(?:bipush|sipush)\s+(-?\d+)']:
  m=re.search(p,line)
  if m:return int(m.group(1))
 raise ValueError(line)
for chunk in s.split('new           #')[1:]:
 lines=chunk.splitlines()
 if 'MapColor' in lines[0] and len(lines)>4:
  ident,val=const(lines[2]),const(lines[3])
  if ident: assert ident==len(colors);colors.append(val)
rgb=[]
for c in colors:
 for level in [180,220,255,135]:
  rgb.append(sum(((((c>>shift)&255)*level)//255)<<shift for shift in [0,8,16]))
assert len(set(rgb[4:]))==244
f=root/'dui-core/src/main/java/gg/kembel/dui/core/video/MapVideoPalette.java'
def emit(path,content):
 if args.check:
  if path.read_text()!=content:raise SystemExit('Generated video protocol differs: '+str(path))
 else:path.write_text(content)
emit(f,'''package gg.kembel.dui.core.video;

/** Minecraft Java 26.2 protocol colors, verified against the official client MapColor table.
 * These are transport symbols. They do not constrain the source image palette. */
public final class MapVideoPalette {
  private MapVideoPalette() {}
  private static final int[] BASE = {'''+','.join(str(c) for c in colors)+'''};
  private static final int[] BRIGHTNESS = {180,220,255,135};
  public static int rgb(int packed) {
    int color = BASE[packed >>> 2], brightness = BRIGHTNESS[packed & 3];
    return (((color >>> 16 & 255)*brightness/255)<<16)
        | (((color >>> 8 & 255)*brightness/255)<<8) | ((color & 255)*brightness/255);
  }
}
''')
# Two-choice cuckoo table: exactly two bounded, constant-time shader reads.
def mix(x):
 x=((x^(x>>16))*0x45d9f3b)&0xffffffff
 x=((x^(x>>16))*0x45d9f3b)&0xffffffff
 return x^(x>>16)
entries=[(i<<24)|rgb[i+4] for i in range(192)]
table=[0]*512
for v in entries:
 at=mix(v&0xffffff)&511
 for step in range(2048):
  v,table[at]=table[at],v
  if not v:break
  a=mix(v&0xffffff)&511;b=mix((v&0xffffff)^0x9e3779b9)&511
  at=b if at==a else a
 else:raise RuntimeError('Cuckoo insertion failed')
for v in entries:
 assert v in (table[mix(v&0xffffff)&511],table[mix((v&0xffffff)^0x9e3779b9)&511])
resource=root/'dui-pack/src/main/resources/ui/shader/video-symbols.glsl'
emit(resource,'''uint duiVideoMix(uint v){v=((v>>16u)^v)*0x45d9f3bu;v=((v>>16u)^v)*0x45d9f3bu;return (v>>16u)^v;}
const uint duiVideoSymbols[512]=uint[512]('''+','.join(str(x)+'u' for x in table)+''');
uint duiVideoRgb(vec4 p){uvec3 c=uvec3(round(p.rgb*255.0));return (c.r<<16u)|(c.g<<8u)|c.b;}
int duiVideoSymbol(vec4 p){uint c=duiVideoRgb(p);uint a=duiVideoSymbols[duiVideoMix(c)&511u];if((a&0xffffffu)==c)return int(a>>24u);a=duiVideoSymbols[duiVideoMix(c^0x9e3779b9u)&511u];return (a&0xffffffu)==c?int(a>>24u):-1;}
int duiVideoWord(int at,int n){int v=0;for(int i=n-1;i>=0;i--)v=v*192+duiVideoSymbol(texelFetch(Sampler0,ivec2(at+i,0),0));return v;}
''')
print('PASS 26.2 palette + bounded shader symbol lookup')
