"""Check the resource links and mesh data that Gradle compilation cannot check."""
from pathlib import Path
import argparse
import hashlib
import json
import math
import struct
import sys
import zipfile
import zlib
import urllib.request

ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--assets-root',type=Path,default=ROOT/'src/main/resources/assets/jsgzpm')
parser.add_argument('--jar',action='store_true')
parser.add_argument('--check-vanilla-atlas',action='store_true')
options=parser.parse_args()
ASSETS=options.assets_root
required=set()

# This project's model textures must use vanilla's block/item directory sources.
# Presence in the JAR is insufficient: sprites outside the atlas appear magenta.
atlas_prefixes=('block/','item/')
if options.check_vanilla_atlas:
    cache=ROOT/'build/asset-validation'
    cache.mkdir(parents=True,exist_ok=True)
    client=cache/'minecraft-1.20.1-client.jar'
    manifest=json.load(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json',timeout=60))
    version=next(v for v in manifest['versions'] if v['id']=='1.20.1')
    metadata=json.load(urllib.request.urlopen(version['url'],timeout=60))
    download=metadata['downloads']['client']
    if not client.exists() or hashlib.sha1(client.read_bytes()).hexdigest()!=download['sha1']:
        client.write_bytes(urllib.request.urlopen(download['url'],timeout=60).read())
    assert hashlib.sha1(client.read_bytes()).hexdigest()==download['sha1'], 'Invalid vanilla client download'
    with zipfile.ZipFile(client) as archive:
        sources=json.loads(archive.read('assets/minecraft/atlases/blocks.json'))['sources']
    atlas_prefixes=tuple(s['prefix'] for s in sources if s['type'] in ('directory','minecraft:directory')
                         and s['source'] in ('block','item') and s['prefix']==s['source']+'/')
    assert set(atlas_prefixes)=={'block/','item/'}, sources
    print('Verified block/item atlas directory sources against SHA-checked Minecraft 1.20.1 client.')

def resource(ref, prefix='', suffix=''):
    namespace, name=ref.split(':',1)
    if namespace!='jsgzpm': return None
    if prefix=='textures':
        assert name.startswith(atlas_prefixes), f'Texture exists but is not covered by the block/item atlas: {ref}'
    path=ASSETS/prefix/(name+suffix)
    assert path.is_file(), f'Missing resource: {ref} ({path})'
    required.add(path)
    return path

for path in (ASSETS/'models').rglob('*.json'):
    data=json.loads(path.read_text())
    if 'parent' in data: resource(data['parent'],'models','.json')
    for ref in data.get('textures',{}).values():
        if not ref.startswith('#'): resource(ref,'textures','.png')
    if data.get('loader')!='forge:obj': continue
    required.add(path)
    obj=resource(data['model'])
    vertices=[]; uvs=[]; normals=[]; faces=[]; mats=set(); active=None
    for line in obj.read_text().splitlines():
        parts=line.split()
        if not parts: continue
        cmd,*args=parts
        if cmd in ('v','vt','vn'):
            vals=list(map(float,args))
            assert all(math.isfinite(v) for v in vals), obj
            {'v':vertices,'vt':uvs,'vn':normals}[cmd].append(vals)
        elif cmd=='mtllib':
            mtl=obj.parent/args[0]; assert mtl.is_file(), mtl
            required.add(mtl)
            for ml in mtl.read_text().splitlines():
                if ml.startswith('newmtl '): mats.add(ml.split()[1])
                if ml.startswith('map_Kd '):
                    key=ml.split()[1]; assert key.startswith('#'), ml
                    resource(data['textures'][key[1:]],'textures','.png')
        elif cmd=='usemtl': active=args[0]; assert active in mats, active
        elif cmd=='f':
            assert active and len(args) in (3,4), line
            face=[]
            for v in args:
                ids=list(map(int,v.split('/')))
                assert len(ids)==3, line
                for i,items in zip(ids,[vertices,uvs,normals]): assert 0<i<=len(items), line
                face.append(vertices[ids[0]-1])
            a,b,c=face[:3]
            u=[b[i]-a[i] for i in range(3)]; v=[c[i]-a[i] for i in range(3)]
            cross=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
            assert sum(q*q for q in cross)>1e-14, 'Degenerate face'
            faces.append(face)
    assert faces and all(abs(sum(x*x for x in n)-1)<1e-4 for n in normals), obj
    bounds=[(min(v[i] for v in vertices),max(v[i] for v in vertices)) for i in range(3)]
    if obj.stem=='zero_point_module':
        assert abs(bounds[1][1]-bounds[1][0]-1.05)<1e-5
        # Three courses must remain stepped, not collapse into one flat crown.
        for low,high,ceiling in [(.20,.28,.81),(.13,.195,.92)]:
            tips=[v[1] for v in vertices if low<math.hypot(v[0]-.5,v[2]-.5)<high]
            assert tips and max(tips)<ceiling, 'Crystal ring height regression'
        assert max(v[1] for v in vertices if .07<math.hypot(v[0]-.5,v[2]-.5)<.12)>.98
        # Installed module clears a 0.136-radius well at every height.
        assert max(math.hypot(v[0]-.5,v[2]-.5)*.4 for v in vertices)<.136
        assert abs((bounds[1][1]-bounds[1][0])*.4-.42)<1e-5
    else:
        assert 1.45 < bounds[0][1]-bounds[0][0] < 1.56
        assert 1.38 < bounds[2][1]-bounds[2][0] < 1.50
        assert 1.16 < bounds[1][1]-bounds[1][0] < 1.19
    print(f'{obj.name}: {len(faces)} faces, bounds {bounds}')

textures=list((ASSETS/'textures/block/ancient').glob('*.png'))
assert len(textures)==11, 'Expected eleven Ancient material textures in the stitched block directory'
for path in textures:
    raw=path.read_bytes(); assert raw[:8]==b'\x89PNG\r\n\x1a\n'
    width,height=struct.unpack('!II',raw[16:24]); assert width==height==256
    offset=8; compressed=b''
    while offset<len(raw):
        length=struct.unpack('!I',raw[offset:offset+4])[0]
        tag=raw[offset+4:offset+8]; payload=raw[offset+8:offset+8+length]
        crc=struct.unpack('!I',raw[offset+8+length:offset+12+length])[0]
        assert zlib.crc32(tag+payload)==crc, path
        if tag==b'IDAT': compressed+=payload
        offset+=12+length
    assert len(zlib.decompress(compressed))==256*(1+256*3), path

if options.jar:
    jars=list((ROOT/'build/libs').glob('*.jar')); assert jars, 'No built JAR'
    for jar in jars:
        with zipfile.ZipFile(jar) as archive:
            for path in required:
                name=path.relative_to(ROOT/'src/main/resources').as_posix()
                assert archive.read(name)==path.read_bytes(), f'Absent/stale JAR resource: {name}'
        print(f'{jar.name}: verified {len(required)} packaged resources')
print('Ancient resource validation passed.')
