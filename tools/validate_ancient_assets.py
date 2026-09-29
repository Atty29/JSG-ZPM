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

def ray_hit(origin,direction,face):
    # Moller-Trumbore, outward faces only: disappearing backfaces do not count.
    def sub(a,b):return [a[i]-b[i] for i in range(3)]
    def cross(a,b):return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
    def dot(a,b):return sum(x*y for x,y in zip(a,b))
    for i in range(1,len(face)-1):
        a,b,c=face[0],face[i],face[i+1]
        e1,e2=sub(b,a),sub(c,a);h=cross(direction,e2);det=dot(e1,h)
        if det<1e-9:continue
        delta=sub(origin,a);u=dot(delta,h)/det
        if not 0<=u<=1:continue
        q=cross(delta,e1);v=dot(direction,q)/det
        if v<0 or u+v>1:continue
        t=dot(e2,q)/det
        if 0<t<.8:return True
    return False

for path in (ASSETS/'models').rglob('*.json'):
    data=json.loads(path.read_text())
    if 'parent' in data: resource(data['parent'],'models','.json')
    for ref in data.get('textures',{}).values():
        if not ref.startswith('#'): resource(ref,'textures','.png')
    if data.get('loader')!='forge:obj': continue
    required.add(path)
    obj=resource(data['model'])
    vertices=[]; uvs=[]; normals=[]; faces=[]; light_faces=[]; mats=set(); used_mats=set(); active=None
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
        elif cmd=='usemtl':
            active=args[0]; assert active in mats, active
            used_mats.add(active)
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
            if active=='light':light_faces.append(face)
    assert faces and all(abs(sum(x*x for x in n)-1)<1e-4 for n in normals), obj
    bounds=[(min(v[i] for v in vertices),max(v[i] for v in vertices)) for i in range(3)]
    if obj.stem=='zero_point_module':
        assert abs(bounds[1][1]-bounds[1][0]-1.05)<1e-5
        assert data.get('render_type')=='minecraft:translucent', 'Glass requires translucent item render type'
        assert {'crystal_olive','crystal_red','crystal_core'} <= used_mats, 'Missing coloured crystal blades'
        # The gem face is flat; stepped crystal ends belong underneath it.
        for low,high,floor,ceiling in [(.07,.12,-.03,.04),(.13,.19,.11,.17),(.20,.25,.30,.37)]:
            ends=[v[1] for v in vertices if low<math.hypot(v[0]-.5,v[2]-.5)<high]
            assert ends and floor<min(ends)<ceiling, 'Crystal end orientation regression'
        crown=[v[1] for v in vertices if .25<math.hypot(v[0]-.5,v[2]-.5)<.28 and v[1]>.98]
        assert crown and all(abs(y-1.012)<1e-5 or abs(y-1.015)<1e-5 for y in crown), 'Gem face must stay flat'
        # Installed module clears a 0.136-radius well at every height.
        assert max(math.hypot(v[0]-.5,v[2]-.5)*.4 for v in vertices)<.136
        assert abs((bounds[1][1]-bounds[1][0])*.4-.42)<1e-5
    elif obj.stem=='ancient_zpm_pedestal':
        assert 1.019<bounds[1][1]<1.021 and bounds[1][0]>=0, 'Waist-high pedestal'
        assert {'pedestal_dark','pedestal_metal','pedestal_cyan_off','pedestal_white_off'}<=used_mats
        assert not any(mat.startswith('crystal') for mat in used_mats), 'ZPM must be a removable item'
        # Both rings have an open bore; their metal must not intersect the crystal.
        for y in [.80,1.0]:
            ring=[v for v in vertices if abs(v[1]-y)<.025 and v[2]<.63]
            assert ring and min(math.hypot(v[0]-.5,v[2]-.5) for v in ring)>.110
        assert any(ray_hit((.5,.69,.5),(0,-1,0),f) for f in faces), 'Socket needs a floor'
    elif obj.stem in ('ancient_zpm_array','ancient_zpm_column'):
        assert all(-.001<=lo<hi<=1.001 for lo,hi in bounds), ('Wall bay exceeds occupied block',bounds)
        assert {'panel','trim','recess','light'} <= used_mats
        assert 'crystal' not in used_mats, 'Crystals must remain independently animated items'
        column=obj.stem=='ancient_zpm_column'
        angle=math.radians(45)
        axis=(-math.cos(angle),0,-math.sin(angle)) if column else (0,math.cos(angle),-math.sin(angle))
        centre=(.5,.5,.5)
        def axial(v):return sum((v[i]-centre[i])*axis[i] for i in range(3))
        # The insertion path must stay open until its recessed floor.
        walls=[f for f in faces if max(axial(v) for v in f)>-.49]
        for x in [-.09,0,.09]:
            origin=tuple(centre[i]+axis[i]*.04+(x if i==(1 if column else 0) else 0) for i in range(3))
            inward=tuple(-v for v in axis)
            assert not any(ray_hit(origin,inward,f) for f in walls), 'Obstructed socket bore'
            assert any(ray_hit(origin,inward,f) for f in faces), 'Missing socket floor'
        if column:
            for x in [.06,.25,.75,.94]:
                for y in [.06,.25,.75,.94]:
                    origin=(1-y-.03,x,y-.03)
                    assert any(ray_hit(origin,tuple(-v for v in axis),f) for f in faces), 'Open wedge face'
    elif obj.stem=='atlantis_pegasus_dhd':
        assert 1.0<bounds[1][1]<1.20, 'DHD must be waist height'
        assert bounds[0][0]>=-1 and bounds[0][1]<=2
        assert {'panel','trim','light'}<=used_mats
        from build_dhd_assets import buttons
        for poly in buttons():
            x=.5-sum(p[0] for p in poly)/len(poly);z=.5-sum(p[1] for p in poly)/len(poly)
            assert any(ray_hit((x,1.3,z),(0,-1,0),f) for f in faces), 'Missing DHD button'
        for line in obj.read_text().splitlines():
            if line.startswith('vt '):assert all(0<=float(v)<=1 for v in line.split()[1:]), 'DHD atlas UV bleed'
    elif obj.stem=='atlantis_zpm_hub':
        # Side-console light walls must share exact 45-degree axes with the notches.
        diagonal_lights=0
        for f in light_faces:
            a,b,c=f[:3]
            u=[b[i]-a[i] for i in range(3)];v=[c[i]-a[i] for i in range(3)]
            n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
            length=math.sqrt(sum(x*x for x in n))
            if abs(n[1]/length)<1e-5 and abs(abs(n[0]/length)-math.sqrt(.5))<1e-4 and abs(abs(n[2]/length)-math.sqrt(.5))<1e-4:
                diagonal_lights+=1
        assert diagonal_lights>=16, 'Side consoles must sit on matching 45-degree axes'
        # Rays into each console's lower gap and upper recess sides must hit an
        # outward-facing wall before reaching the hub centre or opposite side.
        for angle in [-math.pi/2,math.pi/4,3*math.pi/4]:
            nx,nz=math.cos(angle),math.sin(angle);ux,uz=-nz,nx
            for lateral,y in [(0,.40),(-.12,.40),(.12,.40),(-.14,1.09),(.14,1.09),(0,1.09)]:
                origin=(.5+nx+ux*lateral,y,.5+nz+uz*lateral)
                assert any(ray_hit(origin,(-nx,0,-nz),f) for f in faces), ('Open console wall',angle,lateral,y)
        # Sweep along BOTH cheek seams, including the former unbacked strips
        # beside the rib exclusion and beyond the short return walls.
        for angle in [-math.pi/2,math.pi/4,3*math.pi/4]:
            nx,nz=math.cos(angle),math.sin(angle);ux,uz=-nz,nx
            for sign in [-1,1]:
                for lateral in [.150,.156,.162]:
                    for y in [.95,1.04,1.10,1.135]:
                        origin=(.5+nx+ux*lateral*sign,y,.5+nz+uz*lateral*sign)
                        assert any(ray_hit(origin,(-nx,0,-nz),f) for f in faces), ('Open cheek seam',angle,sign,lateral,y)
                for depth in [.46,.56,.62,.66]:
                    for y in [.95,1.10]:
                        origin=(.5+nx*depth+ux*.40*sign,y,.5+nz*depth+uz*.40*sign)
                        assert any(ray_hit(origin,(-ux*sign,0,-uz*sign),f) for f in faces), ('Open cheek return',angle,sign,depth,y)
        assert 1.45 < bounds[0][1]-bounds[0][0] < 1.56
        assert 1.38 < bounds[2][1]-bounds[2][0] < 1.50
        assert 1.16 < bounds[1][1]-bounds[1][0] < 1.19
    print(f'{obj.name}: {len(faces)} faces, bounds {bounds}')

# Both wall structures must resolve their own new bay in every facing.
for kind in ('array','column','pedestal'):
    state=ASSETS/'blockstates'/('ancient_zpm_'+kind+'.json')
    data=json.loads(state.read_text());required.add(state)
    parts=data['multipart']
    assert len(parts)==4
    for part,facing,rotation in zip(parts,['north','east','south','west'],[0,90,180,270]):
        assert part['when']=={'facing':facing}
        assert part['apply'].get('y',0)==rotation
        ref='jsgzpm:block/ancient_zpm_'+kind
        assert part['apply']['model']==ref
        resource(ref,'models','.json')

# New pedestal must be obtainable, named, and packaged with its crafting/drop resources.
for rel in ['data/jsgzpm/recipes/ancient_zpm_pedestal.json', 'data/jsgzpm/loot_tables/blocks/ancient_zpm_pedestal.json', 'data/minecraft/tags/blocks/mineable/pickaxe.json']:
    path=ROOT/'src/main/resources'/rel
    json.loads(path.read_text());required.add(path)
lang=ASSETS/'lang/en_us.json';required.add(lang)
assert json.loads(lang.read_text(encoding='utf-8'))['block.jsgzpm.ancient_zpm_pedestal']=='Ancient ZPM Pedestal'
textures=list((ASSETS/'textures/block/ancient').glob('*.png'))
assert len(textures)==16, 'Expected twelve original materials and four pedestal materials'
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
    decoded=zlib.decompress(compressed)
    glass=path.stem.startswith('crystal') and path.stem!='crystal_core'
    channels=4 if glass else 3
    assert raw[25]==(6 if glass else 2), 'Incorrect RGB/RGBA format'
    assert len(decoded)==256*(1+256*channels), path
    if glass:
        alphas=[decoded[y*(1+256*4)+1+x*4+3] for y in range(256) for x in range(256)]
        assert 90<min(alphas)<180 and max(alphas)>200, 'Missing glass transparency/highlights'

for name in ('recharger_mist','recharger_shield'):
    path=ASSETS/'textures/entity'/(name+'.png');required.add(path)
    raw=path.read_bytes()
    assert struct.unpack('!II',raw[16:24])==(128,128) and raw[25]==6
    offset=8; compressed=b''
    while offset<len(raw):
        length=struct.unpack('!I',raw[offset:offset+4])[0]
        tag=raw[offset+4:offset+8];payload=raw[offset+8:offset+8+length]
        assert zlib.crc32(tag+payload)==struct.unpack('!I',raw[offset+8+length:offset+12+length])[0]
        if tag==b'IDAT': compressed+=payload
        offset+=12+length
    decoded=zlib.decompress(compressed)
    assert len(decoded)==128*513
    alpha=[decoded[y*513+1+x*4+3] for y in range(128) for x in range(128)]
    assert min(alpha)==0 and max(alpha)>90
    assert all(decoded[y*513]==0 for y in range(128))
    assert all(alpha[y*128+x]==0 for x,y in [(0,0),(127,0),(0,127),(127,127)])

if options.jar:
    jars=list((ROOT/'build/libs').glob('*.jar')); assert jars, 'No built JAR'
    for jar in jars:
        with zipfile.ZipFile(jar) as archive:
            for path in required:
                name=path.relative_to(ROOT/'src/main/resources').as_posix()
                assert archive.read(name)==path.read_bytes(), f'Absent/stale JAR resource: {name}'
        print(f'{jar.name}: verified {len(required)} packaged resources')
print('Ancient resource validation passed.')
