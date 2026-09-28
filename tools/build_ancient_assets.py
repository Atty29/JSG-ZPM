"""Original JSG-ZPM geometry and pixels. Python standard library only.

No reference asset is read, converted, sampled, traced or bundled by this tool.
Units are blocks; OBJ positions use Minecraft's 0..1 block coordinate system.
"""
from pathlib import Path
import json
import math
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/jsgzpm'
PALETTE = {
    'panel': (139, 151, 158), 'trim': (177, 186, 190),
    'recess': (39, 48, 55), 'binder': (24, 26, 28),
    'crystal': (225, 144, 39), 'regulator': (137, 43, 34),
    'light': (107, 179, 194),
}


def png(path, name):
    size = 32
    rows = []
    for y in range(size):
        row = bytearray([0])
        for x in range(size):
            noise = ((x * 17 + y * 29 + x * y * 3) % 7) - 3
            shade = noise
            if name in ('panel', 'trim', 'recess', 'binder'):
                if x in (0, 31) or y in (0, 31): shade -= 22
                if x == 1 or y == 1: shade += 15
                if y in (15, 16) and 5 < x < 26: shade -= 10
                if x == 7 and 7 < y < 13: shade -= 14
            elif name == 'crystal':
                shade += [12, 24, 34, 18, -8, -20, -10, 0][x // 4]
                shade += 12 if (y + x // 8 * 3) % 16 < 2 else 0
            elif name == 'regulator':
                shade += 22 if x in (2, 3, 28, 29) else -4
            else:
                shade += 25 if 8 < x < 23 else -25
            row.extend(max(0, min(255, v + shade)) for v in PALETTE[name])
        rows.append(bytes(row))
    def chunk(tag, data):
        return struct.pack('!I', len(data)) + tag + data + struct.pack('!I', zlib.crc32(tag + data))
    data = b'\x89PNG\r\n\x1a\n'
    data += chunk(b'IHDR', struct.pack('!2I5B', size, size, 8, 2, 0, 0, 0))
    data += chunk(b'IDAT', zlib.compress(b''.join(rows), 9)) + chunk(b'IEND', b'')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


class Mesh:
    def __init__(self):
        self.faces = []

    def face(self, pts, material):
        self.faces.append((pts, material))

    def lathe(self, x, z, levels, material, n=12, cap=True):
        rings = [[(x + r * math.cos(i * 2 * math.pi / n), y,
                   z + r * math.sin(i * 2 * math.pi / n)) for i in range(n)] for y, r in levels]
        for low, high in zip(rings, rings[1:]):
            for i in range(n):
                j = (i + 1) % n
                self.face([low[i], high[i], high[j], low[j]], material)
        if cap:
            for i in range(n):
                j = (i + 1) % n
                self.face([(x, levels[0][0], z), rings[0][i], rings[0][j]], material)
                self.face([(x, levels[-1][0], z), rings[-1][j], rings[-1][i]], material)

    def ring(self, x, z, y, inner, outer, material, n=12):
        for i in range(n):
            a, b = i * 2 * math.pi / n, (i + 1) * 2 * math.pi / n
            def p(r, t): return (x + r * math.cos(t), y, z + r * math.sin(t))
            self.face([p(inner, a), p(inner, b), p(outer, b), p(outer, a)], material)

    def box(self, lo, hi, material):
        x,y,z = lo; X,Y,Z = hi
        for pts in [ [(x,y,z),(x,Y,z),(X,Y,z),(X,y,z)],
                     [(X,y,Z),(X,Y,Z),(x,Y,Z),(x,y,Z)],
                     [(x,y,Z),(x,Y,Z),(x,Y,z),(x,y,z)],
                     [(X,y,z),(X,Y,z),(X,Y,Z),(X,y,Z)],
                     [(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],
                     [(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)]]:
            self.face(pts, material)

    def save(self, stem):
        path = ASSETS / 'models' / stem
        path.parent.mkdir(parents=True, exist_ok=True)
        lines = ['# Original parametric JSG-ZPM artwork; see tools/build_ancient_assets.py',
                 f'mtllib {path.name}.mtl', 'o ancient_hardware']
        tables = {'v': {}, 'vt': {}, 'vn': {}}
        def entry(kind, values):
            key = tuple(round(q, 6) for q in values)
            table = tables[kind]
            if key not in table:
                table[key] = len(table) + 1
                lines.append(kind + ' ' + ' '.join(f'{q:g}' for q in key))
            return table[key]
        active = None
        for pts, mat in self.faces:
            a,b,c = pts[:3]
            u = [b[i]-a[i] for i in range(3)]; v = [c[i]-a[i] for i in range(3)]
            normal = [u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
            length = math.sqrt(sum(q*q for q in normal))
            assert length > 1e-10, (stem, pts)
            normal = [q/length for q in normal]
            # Each authored face has its own UV island: no inherited reference UVs.
            uvs = [(0,1),(0,0),(1,0),(1,1)]
            if stem.startswith('block/'):
                # Continuous planar mapping prevents radial streaks on socket sectors.
                axis = max(range(3), key=lambda i: abs(normal[i]))
                uvs = [((p[2] if axis == 0 else p[0]) / 1.6 + .1875,
                        (p[2] / 1.6 + .1875) if axis == 1 else (1-p[1]/1.2)) for p in pts]
            indices = []
            for p,uv in zip(pts, uvs):
                vi,ti,ni = entry('v',p),entry('vt',uv),entry('vn',normal)
                indices.append(f'{vi}/{ti}/{ni}')
            if active != mat:
                lines.append(f'usemtl {mat}')
                active = mat
            lines.append('f ' + ' '.join(indices))
        path.with_suffix('.obj').write_text('\n'.join(lines)+'\n')
        path.with_suffix('.mtl').write_text(''.join(f'newmtl {m}\nKd 1 1 1\nd 1\nmap_Kd #{m}\n\n' for m in PALETTE))


def model(stem, **extra):
    data = {'loader':'forge:obj', 'model':f'jsgzpm:models/{stem}.obj',
            'automatic_culling':False, 'shade_quads':True, 'flip_v':False,
            'ambientocclusion':False,
            'textures':{m:f'jsgzpm:ancient/{m}' for m in PALETTE}}
    data['textures']['particle'] = 'jsgzpm:ancient/panel'
    data.update(extra)
    (ASSETS/'models'/f'{stem}.json').write_text(json.dumps(data,indent=2)+'\n')


def build_zpm():
    m = Mesh()
    # Tapered amber vessel with a flared twelve-sided crown, 0.55 x 1.05 blocks.
    m.lathe(.5,.5,[(-.025,.125),(.02,.16),(.27,.195),(.35,.225),(.88,.225),(.94,.265)],'crystal')
    for y,r in [(.035,.167),(.32,.225),(.87,.238)]:
        m.lathe(.5,.5,[(y,r),(y+.025,r)],'binder')
    m.lathe(.5,.5,[(.94,.265),(.97,.275),(1.025,.25)],'binder',cap=False)
    m.ring(.5,.5,1.025,.09,.25,'binder')
    m.ring(.5,.5,1.025,.035,.09,'regulator')
    m.lathe(.5,.5,[(1.0,.035),(1.024,.035)],'recess')
    # Narrow structural ribs, leaving the crystal faces readable.
    for i in range(12):
        a = i*math.pi/6
        for (y,r),(Y,R) in zip([(.04,.171),(.32,.227),(.88,.229)],[(.32,.227),(.88,.229),(.95,.269)]):
            pts=[(.5+s*math.cos(t),h,.5+s*math.sin(t)) for h,s,t in [(y,r,a-.025),(Y,R,a-.025),(Y,R,a+.025),(y,r,a+.025)]]
            m.face(pts,'binder')
        # Alternating diagonal lattice between the two main bands.
        a2 = a + (math.pi/6 if i%2==0 else -math.pi/6)
        # Segmented ribbons follow the faceted vessel rather than passing through it.
        for step in range(4):
            t0,t1=step/4,(step+1)/4
            pts=[]
            for t,edge in [(t0,-.014),(t1,-.014),(t1,.014),(t0,.014)]:
                ang=a+(a2-a)*t+edge
                # Facet intersection radius for a regular dodecagon.
                local=(ang%(math.pi/6))-math.pi/12
                r=.225*math.cos(math.pi/12)/math.cos(local)+.002
                pts.append((.5+r*math.cos(ang),.37+.47*t,.5+r*math.sin(ang)))
            m.face(pts,'binder')
    m.save('item/zero_point_module')
    model('item/zero_point_module',gui_light='front',display={
        'gui':{'rotation':[25,35,0],'scale':[.85,.85,.85]},
        'fixed':{'scale':[1,1,1]},
        'ground':{'translation':[0,3,0],'scale':[.4,.4,.4]},
        'firstperson_righthand':{'rotation':[0,-90,15],'translation':[1,2,1],'scale':[.45,.45,.45]},
        'firstperson_lefthand':{'rotation':[0,90,-15],'translation':[1,2,1],'scale':[.45,.45,.45]},
        'thirdperson_righthand':{'translation':[0,2,0],'scale':[.3,.3,.3]},
        'thirdperson_lefthand':{'translation':[0,2,0],'scale':[.3,.3,.3]}})


def build_hub():
    m=Mesh()
    # Three joined lobes. Every lobe has a real open shaft, not a painted socket.
    left=[(0,0),(-.20,.30),(-.744,.10),(-.744,-.35),(-.45,-.712),(0,-.45)]
    right=[(-x,z) for x,z in reversed(left)]
    back=[(0,0),(.20,.30),(.38,.48),(.28,.712),(-.28,.712),(-.38,.48),(-.20,.30)]
    bays=[(-.265,-.204),(.0,.246),(.265,-.204)]
    for poly,(cx,cz) in zip([left,back,right],bays):
        # External relief: raised frames, recessed seams and stepped foot plates.
        for a,b in zip(poly,poly[1:]+poly[:1]):
            if a == (0,0) or b == (0,0): continue
            dx,dz=b[0]-a[0],b[1]-a[1]
            length=math.hypot(dx,dz)
            nx,nz=dz/length,-dx/length
            def plate(t0,t1,y0,y1,depth,mat):
                x,z=a[0]*.84+.5+nx*depth,a[1]*.84+.5+nz*depth
                X,Z=dx*.84,dz*.84
                m.face([(x+X*t0,y0,z+Z*t0),(x+X*t0,y1,z+Z*t0),
                        (x+X*t1,y1,z+Z*t1),(x+X*t1,y0,z+Z*t1)],mat)
            plate(.08,.92,.05,.90,.004,'recess')
            plate(.14,.86,.10,.84,.007,'panel')
            plate(.12,.18,.08,.86,.010,'trim')
            plate(.82,.88,.08,.86,.010,'trim')
            plate(.22,.78,.18,.20,.011,'recess')
            plate(.22,.78,.70,.72,.011,'recess')
            plate(.36,.42,.25,.62,.011,'recess')
            plate(.42,.65,.60,.63,.011,'recess')
            plate(.0,1.0,.016,.065,.012,'trim')
        for a,b in zip(poly,poly[1:]+poly[:1]):
            if a == (0,0) or b == (0,0): continue
            m.face([(.5,.016,.5),(a[0]*.84+.5,.016,a[1]*.84+.5),
                    (b[0]*.84+.5,.016,b[1]*.84+.5)],'recess')
        # Subdivide straight outer edges; join to a circular inner shaft.
        outer=[]
        for a,b in zip(poly,poly[1:]+poly[:1]):
            for t in range(4): outer.append((a[0]+(b[0]-a[0])*t/4,a[1]+(b[1]-a[1])*t/4))
        for i,(x,z) in enumerate(outer):
            X,Z=outer[(i+1)%len(outer)]
            def inner(px,pz):
                a=math.atan2(pz-cz,px-cx)
                return (cx+.145*math.cos(a),cz+.145*math.sin(a))
            ix,iz=inner(x,z); jx,jz=inner(X,Z)
            def p(x,y,z):return (x+.5,y,z+.5)
            # Polygon winding is CCW in XZ; reverse for upward facing surfaces.
            m.face([p(ix,1.16,iz),p(jx,1.16,jz),p(X,1.16,Z),p(x,1.16,z)],'panel')
            m.face([p(ix,.76,iz),p(jx,.76,jz),p(jx,1.16,jz),p(ix,1.16,iz)],'recess')
            m.face([p(x,.94,z),p(x,1.16,z),p(X,1.16,Z),p(X,.94,Z)],'trim')
            # Recessed column follows the table's outline and keeps its three-lobed silhouette.
            m.face([p(x*.84,.016,z*.84),p(x*.84,.94,z*.84),p(X*.84,.94,Z*.84),p(X*.84,.016,Z*.84)],'panel')
            # Ribbed skirt around external perimeter only.
            if abs(x)+abs(z)>.55 and abs(X)+abs(Z)>.55:
                mx,mz=(x+X)/2,(z+Z)/2
                m.box((mx+.485,.935,mz+.485),(mx+.515,1.18,mz+.515),'recess')
        m.lathe(cx+.5,cz+.5,[(.75,.144),(.765,.144)],'recess')
        m.ring(cx+.5,cz+.5,1.181,.145,.176,'trim')
        # Three external vertical frame rails and stepped foot around each bay.
        angle=math.atan2(cz,cx)
        x,z=cx+.20*math.cos(angle)+.5,cz+.20*math.sin(angle)+.5
        m.box((x-.055,.02,z-.055),(x+.055,.945,z+.055),'recess')
        for y in [.10,.28,.46,.64,.82]:
            m.box((x-.062,y,z-.062),(x+.062,y+.025,z+.062),'trim')
        m.box((x-.07,1.162,z-.025),(x+.07,1.178,z+.025),'binder')
        m.box((x-.045,1.179,z-.012),(x+.045,1.184,z+.012),'light')
    m.save('block/atlantis_zpm_hub')
    model('block/atlantis_zpm_hub',parent='minecraft:block/block',display={
        'gui':{'rotation':[30,225,0],'translation':[0,-1,0],'scale':[.5,.5,.5]},
        'ground':{'scale':[.25,.25,.25]},
        'fixed':{'scale':[.5,.5,.5]},
        'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.3,.3,.3]}})


if __name__ == '__main__':
    for name in PALETTE: png(ASSETS/'textures/ancient'/f'{name}.png',name)
    build_zpm()
    build_hub()
    print('Rebuilt original ZPM, hub and seven reusable Ancient materials.')

