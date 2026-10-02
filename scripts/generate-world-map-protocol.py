#!/usr/bin/env python3
"""Generate/check the separate world-map protocol; ordinary dialog protocol remains version 2."""
import argparse, hashlib, json, re
from pathlib import Path
root=Path(__file__).resolve().parent.parent
raw=(root/'protocol/world-map.json').read_bytes()
spec=json.loads(raw)
def constant(name):
    return re.sub(r'(?<!^)([A-Z])',r'_\1',name).upper()
digest=hashlib.sha256(raw).hexdigest()
java=root/'dui-core/src/main/java/gg/kembel/dui/core/world/WorldMapProtocol.java'
glsl=root/'dui-pack/src/main/resources/ui/shader/world-map-protocol.glsl'
shader='// Generated from protocol/world-map.json.\n'+''.join(f'const int DUI_MAP_{constant(k)} = {v};\n' for k,v in spec.items())
parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
if args.check:
    text=java.read_text()
    assert f'"{digest}"' in text, 'WorldMapProtocol hash is stale'
    for key,value in spec.items():
        assert re.search(rf'\b{constant(key)}\s*=\s*{value}\s*;',text), key
    assert glsl.read_text()==shader,'World-map GLSL constants are stale'
    print('PASS world-map protocol Java/GLSL/hash')
else:
    java.write_text('package gg.kembel.dui.core.world;\n/** Generated from protocol/world-map.json. */\npublic final class WorldMapProtocol {\n'
        '  private WorldMapProtocol() {}\n  public static final String CAPABILITY = "world-map-v1";\n'
        +''.join(f'  public static final int {constant(k)} = {v};\n' for k,v in spec.items())
        +f'  public static final String SHA256 = "{digest}";\n'+'}\n')
    glsl.write_text(shader)
