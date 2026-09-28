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
    'crystal': (244, 174, 31), 'crystal_warm': (219, 119, 22), 'crystal_pale': (255, 214, 79), 'regulator': (210, 43, 24),
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
            elif name.startswith('crystal'):
                shade += [0, 12, 30, 18, -6, -20, -10, 0][x // 4]
                shade += round(12 * math.sin(y * .19 + x * .09))
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
            elif normal[1] > .9 and min(p[1] for p in pts) > .95:
                uvs = [((p[0]-.2)/.6,(p[2]-.2)/.6) for p in pts]
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
            'textures':{m:f'jsgzpm:block/ancient/{m}' for m in PALETTE}}
    data['textures']['particle'] = 'jsgzpm:block/ancient/panel'
    data.update(extra)
    (ASSETS/'models'/f'{stem}.json').write_text(json.dumps(data,indent=2)+'\n')


def build_zpm():
    m = Mesh()
    # Twelve irregular crystal lobes; inset alternating vertices make real grooves.
    # Authored from the supplied prop references, not sampled or traced.
    n=24
    heights=[.025,.19,.41,.68,.91,1.0]
    radii=[.14,.19,.211,.228,.253,.269]
    rings=[]
    for k,(y,r) in enumerate(zip(heights,radii)):
        ring=[]
        for i in range(n):
            angle=i*2*math.pi/n
            groove=1.0 if i%2==0 else .86
            variation=1+.025*math.sin(i*2.3+k*.9)
            ry=y
            if k==0: ry=-.025 if i==0 else .012+.043*(1+math.sin(i*1.7))/2
            elif k==len(heights)-1: ry=1.0+.015*math.sin(i*1.8)**2
            else: ry+=.023*math.sin(i*1.3+k*2.1)
            radius=min(.275,r*groove*variation)
            ring.append((.5+radius*math.cos(angle),ry,.5+radius*math.sin(angle)))
        rings.append(ring)
    for k,(low,high) in enumerate(zip(rings,rings[1:])):
        for i in range(n):
            j=(i+1)%n
            material=['crystal','crystal_warm','crystal','crystal_pale'][(i//2+k)%4]
            # Triangles preserve the crystalline, deliberately non-planar facets.
            m.face([low[i],high[i],high[j]],material)
            m.face([low[i],high[j],low[j]],material)
    for i in range(n):
        j=(i+1)%n
        m.face([(.5,-.015,.5),rings[0][i],rings[0][j]],'crystal_warm')
        m.face([(.5,1.014,.5),rings[-1][j],rings[-1][i]],'crystal_pale' if i%3 else 'crystal')

    # Follow the actual piecewise-linear vessel surface for the dark binder paths.
    def surface(t, a, lift=.0025):
        k=min(len(rings)-2,int(t))
        frac=t-k
        sector=(a%(2*math.pi))/(2*math.pi)*n
        i=int(sector)%n; j=(i+1)%n; f=sector-int(sector)
        # Match the exact triangle split above (bilinear interpolation can bury a wire).
        if frac >= f:
            p=[rings[k][i][v]*(1-frac)+rings[k+1][i][v]*(frac-f)+rings[k+1][j][v]*f for v in range(3)]
        else:
            p=[rings[k][i][v]*(1-f)+rings[k+1][j][v]*frac+rings[k][j][v]*(f-frac) for v in range(3)]
        p[0]+=lift*math.cos(a); p[2]+=lift*math.sin(a)
        return p
    def seam(points, width=.023):
        for (t,a),(T,A) in zip(points,points[1:]):
            steps=max(2,math.ceil(abs(T-t)*5+abs(A-a)*12))
            for step in range(steps):
                f,g=step/steps,(step+1)/steps
                t0,t1=t+(T-t)*f,t+(T-t)*g
                a0,a1=a+(A-a)*f,a+(A-a)*g
                # Split at the surface to avoid ribbons tunnelling through grooves.
                pts=[surface(t0,a0-width),surface(t1,a1-width),
                     surface(t1,a1+width),surface(t0,a0+width)]
                m.face(pts if t1 >= t0 else list(reversed(pts)),'binder')
    for i in range(12):
        a=i*math.pi/6
        seam([(0,a),(.9,a+.05),(1.8,a-.05),(3.0,a+.07),(4,a),(5,a)])
        # Offset branch junctions form unequal polygon cells, not uniform X bands.
        if i%2==0:
            seam([(1.0+(i%3)*.12,a),(1.35+(i%3)*.12,a+math.pi/12),
                  (1.1+(i%3)*.12,a+math.pi/6)])
        seam([(3.4+(i%3)*.1,a),(3.65+(i%3)*.1,a+math.pi/12),
              (3.5+(i%3)*.1,a+math.pi/6)])
    # Six long, pointed dark fittings between the amber lobes.
    for i in range(6):
        a=i*math.pi/3+math.pi/12
        start=1.45+(i%2)*.15
        for step in range(8):
            f,g=step/8,(step+1)/8
            t,T=start+1.65*f,start+1.65*g
            w=.06*min(1,f*5,(1-f)*5)
            W=.06*min(1,g*5,(1-g)*5)
            if step==0:
                pts=[surface(t,a,.006),surface(T,a-W,.006),surface(T,a+W,.006)]
            elif step==7:
                pts=[surface(t,a-w,.006),surface(T,a,.006),surface(t,a+w,.006)]
            else:
                pts=[surface(t,a-w,.006),surface(T,a-W,.006),surface(T,a+W,.006),surface(t,a+w,.006)]
            m.face(pts,'binder')
    # Glowing amber crown divided by fine concentric rings and radial binder spokes.
    for radius in [.087,.17,.238]:
        m.ring(.5,.5,1.018,radius-.0035,radius+.0035,'binder',n=24)
    for i in range(12):
        a=i*math.pi/6
        def crown(r,theta):return (.5+r*math.cos(theta),1.019,.5+r*math.sin(theta))
        m.face([crown(.056,a-.018),crown(.056,a+.018),
                crown(.265,a+.018),crown(.265,a-.018)],'binder')
    m.lathe(.5,.5,[(1.015,.062),(1.022,.057)],'binder',n=16)
    m.lathe(.5,.5,[(1.022,.047),(1.025,.039)],'regulator',n=16)
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
    # Clipped triangular table, partitioned into three independently occupied bays.
    left=[(0,0),(-.385,.13),(-.744,-.50),(-.61,-.712),(0,-.712)]
    right=[(-x,z) for x,z in reversed(left)]
    back=[(0,0),(.385,.13),(.12,.62),(0,.712),(-.12,.62),(-.385,.13)]
    bays=[(-.265,-.204),(.0,.246),(.265,-.204)]
    def top_line(a,b,width,y,material):
        dx,dz=b[0]-a[0],b[1]-a[1]
        length=math.hypot(dx,dz); nx,nz=dz/length*width/2,-dx/length*width/2
        m.face([(a[0]+.5+nx,y,a[1]+.5+nz),(a[0]+.5-nx,y,a[1]+.5-nz),
                (b[0]+.5-nx,y,b[1]+.5-nz),(b[0]+.5+nx,y,b[1]+.5+nz)],material)
    # A small six-spoke junction and angular inset tracks, authored as geometry.
    for radius,y,material in [(.126,1.162,'recess'),(.110,1.164,'trim')]:
        star=[]
        for i in range(12):
            a=i*math.pi/6
            r=radius if i%2==0 else radius*.38
            star.append((r*math.cos(a)+.5,y,r*math.sin(a)+.445))
        for i in range(12):m.face([(.5,y,.445),star[(i+1)%12],star[i]],material)
    for poly,(cx,cz) in zip([left,back,right],bays):
        for a,b in zip(poly,poly[1:]+poly[:1]):
            if a == (0,0) or b == (0,0):continue
            top_line((a[0]*.94,a[1]*.94),(b[0]*.94,b[1]*.94),.016,1.162,'recess')
            top_line((a[0]*.905,a[1]*.905),(b[0]*.905,b[1]*.905),.009,1.164,'trim')
        # Geometric arrow detailing points outwards from each socket.
        length=math.hypot(cx,cz); ux,uz=cx/length,cz/length
        def mark(forward,side):return (cx+ux*forward-uz*side,cz+uz*forward+ux*side)
        for a,b in [(mark(.20,-.065),mark(.30,-.045)),(mark(.30,-.045),mark(.36,0)),
                    (mark(.36,0),mark(.30,.045)),(mark(.30,.045),mark(.20,.065))]:
            top_line(a,b,.019,1.163,'recess')
            top_line(a,b,.009,1.165,'trim')
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
        # Restrained socket status accent, not a large luminous tabletop panel.
        m.box((cx+.475,1.179,cz+.662),(cx+.525,1.184,cz+.668),'light')
    m.save('block/atlantis_zpm_hub')
    model('block/atlantis_zpm_hub',parent='minecraft:block/block',display={
        'gui':{'rotation':[30,225,0],'translation':[0,-1,0],'scale':[.5,.5,.5]},
        'ground':{'scale':[.25,.25,.25]},
        'fixed':{'scale':[.5,.5,.5]},
        'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.3,.3,.3]}})


if __name__ == '__main__':
    for name in PALETTE: png(ASSETS/'textures/block/ancient'/f'{name}.png',name)
    build_zpm()
    build_hub()
    print(f'Rebuilt original ZPM, hub and {len(PALETTE)} reusable Ancient materials.')

