"""Original JSG-ZPM geometry and pixels. Python standard library only.

No reference asset is read, converted, sampled, traced or bundled by this tool.
Units are blocks; OBJ positions use Minecraft's 0..1 block coordinate system.
"""
from pathlib import Path
import json
import math
import struct
import zlib
from lantian_materials import detail

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/jsgzpm'
PALETTE = {
    'panel': (105, 73, 55), 'trim': (139, 101, 77),
    'recess': (43, 32, 28), 'binder': (22, 24, 22),
    'crystal': (220, 143, 25), 'crystal_warm': (184, 81, 12),
    'crystal_pale': (249, 189, 53), 'regulator': (135, 37, 26),
    'light': (184, 207, 213),
    'crystal_olive': (45, 139, 42), 'crystal_red': (180, 35, 23),
    'crystal_core': (247, 198, 82),
}
TEXTURE_SIZE = 256


def png(path, name):
    size = TEXTURE_SIZE
    rows = []
    glass = name.startswith('crystal') and name != 'crystal_core'
    def noise(x,y,seed):
        n=(x*374761393+y*668265263+seed*1442695041)&0xffffffff
        n=((n^(n>>13))*1274126177)&0xffffffff
        return ((n^(n>>16))&65535)/65535-.5
    for y in range(size):
        row = bytearray([0])
        for x in range(size):
            # Original multi-scale mineral grain, without large painted panel outlines.
            shade=noise(x,y,11)*17+noise(x//3,y//3,23)*10+noise(x//13,y//13,7)*7
            shade+=4*math.sin(x*.043+y*.027)*math.cos(y*.061)
            if name.startswith('crystal'):
                # Clean angular internal planes and thin bright reflections.
                # No sinusoidal grain: that looked like wood stretched along blades.
                u,v=x/(size-1),y/(size-1)
                facet=math.floor(u*5+v*1.4)
                shade=(-24,12,-7,30,-16,5,18)[facet%7]
                shade+=18*(1-v)+noise(x//2,y//2,31)*2
                reflection=abs(u-(.22+.10*v))
                if reflection<.018:shade+=65*(1-reflection/.018)
                if abs(u-(.73-.16*v))<.009:shade+=32
            elif name=='binder':shade*=.3
            elif name=='regulator':shade=shade*.3+12*math.sin(x/size*math.pi)*math.sin(y/size*math.pi)
            elif name=='light':shade=8+18*math.sin(x/size*math.pi)
            rgb=[max(0,min(255,round(v+shade))) for v in PALETTE[name]]
            rgb=detail(name,x,y,rgb)
            if glass:
                # Coloured glass body with bright, more opaque edge reflections.
                u,v=x/(size-1),y/(size-1)
                glint=max(0,1-abs(u-(.22+.10*v))/.025)
                edge=max(0,1-min(u,1-u)/.045)
                reflection=max(glint,edge*.7)
                rgb=[round(c*(1-reflection*.72)+255*reflection*.72) for c in rgb]
                row.extend(rgb)
                row.append(round(135+95*reflection))
            else:row.extend(rgb)
        rows.append(bytes(row))
    def chunk(tag, data):
        return struct.pack('!I', len(data)) + tag + data + struct.pack('!I', zlib.crc32(tag + data))
    data = b'\x89PNG\r\n\x1a\n'
    data += chunk(b'IHDR', struct.pack('!2I5B', size, size, 8, 6 if glass else 2, 0, 0, 0))
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
            elif mat.startswith('crystal') and abs(normal[1])<.9:
                axis=0 if abs(normal[2])>abs(normal[0]) else 2
                uvs=[((p[axis]-.22)/.56,1-(p[1]+.025)/1.05) for p in pts]
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
        path.with_suffix('.mtl').write_text(''.join(f'newmtl {m}\nKa {"1 1 1" if m == "light" else "0 0 0"}\nKd 1 1 1\nd 1\nmap_Kd #{m}\n\n' for m in PALETTE))


def model(stem, **extra):
    data = {'loader':'forge:obj', 'model':f'jsgzpm:models/{stem}.obj',
            'automatic_culling':False, 'shade_quads':True, 'flip_v':False,
            'ambientocclusion':True, 'emissive_ambient':True,
            'textures':{m:f'jsgzpm:block/ancient/{m}' for m in PALETTE}}
    data['textures']['particle'] = 'jsgzpm:block/ancient/panel'
    data.update(extra)
    (ASSETS/'models'/f'{stem}.json').write_text(json.dumps(data,indent=2)+'\n')


def build_zpm():
    m = Mesh()
    # Gem-up orientation: a broad flat cap, with three concentric courses
    # extending DOWN from it. The inner blades are longest, the outer shortest.
    # Original independent blade geometry; reference renders supply no mesh data.
    def point(r,y,a):return (.5+r*math.cos(a),y,.5+r*math.sin(a))
    def line(a,b,width=.003):
        # Surface ribbons are double-sided so oblique views retain the binder.
        dx,dz=b[0]-a[0],b[2]-a[2]
        length=math.hypot(dx,dz)
        if length<1e-8:
            dx,dz=a[0]-.5,a[2]-.5;length=math.hypot(dx,dz)
        tx,tz=-dz/length*width,dx/length*width
        pts=[(a[0]-tx,a[1],a[2]-tz),(b[0]-tx,b[1],b[2]-tz),
             (b[0]+tx,b[1],b[2]+tz),(a[0]+tx,a[1],a[2]+tz)]
        m.face(pts,'binder');m.face(list(reversed(pts)),'binder')
    for course,(inside,outside,end) in enumerate([(.055,.119,-.025),(.121,.191,.12),(.193,.249,.31)]):
        for i in range(12):
            angle=(i+course*.24)*math.pi/6;half=math.pi/12-.014
            bottom=end+(.045*(.5+.5*math.sin(i*2.1+course)) if i else 0)
            polar=[(inside,angle-half),(inside,angle+half),
                   (outside-.010,angle+half),(outside,angle),(outside-.010,angle-half)]
            centre=point((inside+outside)*.5,bottom,angle)
            levels=[]
            for y,factor in [(bottom,.70),(bottom+.055,1.),(.85,1.),(.94,1.)]:
                # Bevel each individual end towards its own centre, not the axis.
                levels.append([(centre[0]+(point(r,y,a)[0]-centre[0])*factor,y,
                                centre[2]+(point(r,y,a)[2]-centre[2])*factor) for r,a in polar])
            # A narrow internal honey-coloured facet is visible through the glass.
            core_x,_,core_z=point((inside+outside)*.5,bottom,angle)
            m.lathe(core_x,core_z,[(bottom+.060,.006),(bottom+.10,.012),(.86,.010)],'crystal_core',n=5)
            # Unequal slanted tip planes produce the chipped crystal termination.
            levels[0]=[(x,y+.017*(.5+.5*math.sin(i+j*1.9)),z) for j,(x,y,z) in enumerate(levels[0])]
            material=['crystal','crystal_pale','crystal','crystal_warm'][i%4]
            if course==2 and i in (1,5,9):material='crystal_olive'
            elif course==2 and i in (3,7,11):material='crystal_red'
            elif course==1 and i in (2,8):material='crystal_olive'
            elif course==1 and i==5:material='crystal_red'
            for low,high in zip(levels,levels[1:]):
                for j in range(5):
                    k=(j+1)%5
                    m.face([low[k],high[k],high[j]],material)
                    m.face([low[k],high[j],low[j]],material)
            for j in range(5):
                k=(j+1)%5
                m.face([centre,levels[0][k],levels[0][j]],material)
                m.face([point((inside+outside)*.5,.94,angle),levels[-1][j],levels[-1][k]],material)
            for side in [2,4]:
                path=[(p[side][0]+.002*math.cos(angle),p[side][1],p[side][2]+.002*math.sin(angle)) for p in levels]
                for A,B in zip(path,path[1:]):line(A,B)
            for frac in [.34,.72]:
                y=bottom+.055+(.84-bottom-.055)*frac
                line(point(outside-.007,y,angle-half),point(outside+.003,y+.03,angle))
                line(point(outside+.003,y+.03,angle),point(outside-.007,y-.013,angle+half))
    # Core ends short of the longest inner blade tips.
    m.lathe(.5,.5,[(.020,.050),(.945,.050)],'crystal_warm',n=12)
    # Broad common flat gem face; the stepped ends are exclusively underneath.
    n=24
    lower=[point(.245 if i%2==0 else .238,.88,i*math.pi/12) for i in range(n)]
    upper=[point(.269 if i%2==0 else .257,1.012,i*math.pi/12) for i in range(n)]
    for i in range(n):
        j=(i+1)%n;mat=['crystal','crystal_pale','crystal_warm'][i//2%3]
        m.face([lower[i],upper[i],upper[j],lower[j]],mat)
        m.face([(.5,1.012,.5),upper[j],upper[i]],mat)
        m.face([(.5,.88,.5),lower[i],lower[j]],'crystal_warm')
        if i%2==0:line(lower[i],upper[i],.003)
    for radius in [.085,.168]:m.ring(.5,.5,1.014,radius-.004,radius+.004,'binder',n=24)
    for i in range(12):
        angle=i*math.pi/6
        line(point(.055,1.015,angle),point(.267,1.015,angle),.0025)
    m.lathe(.5,.5,[(1.012,.061),(1.020,.055)],'binder',n=16)
    m.lathe(.5,.5,[(1.020,.046),(1.025,.039)],'regulator',n=16)
    m.save('item/zero_point_module')
    model('item/zero_point_module',render_type='minecraft:translucent',gui_light='front',display={
        'gui':{'rotation':[25,35,0],'scale':[.85,.85,.85]},
        'fixed':{'scale':[1,1,1]},
        'ground':{'translation':[0,3,0],'scale':[.4,.4,.4]},
        'firstperson_righthand':{'rotation':[0,-90,15],'translation':[1,2,1],'scale':[.45,.45,.45]},
        'firstperson_lefthand':{'rotation':[0,90,-15],'translation':[1,2,1],'scale':[.45,.45,.45]},
        'thirdperson_righthand':{'translation':[0,2,0],'scale':[.3,.3,.3]},
        'thirdperson_lefthand':{'translation':[0,2,0],'scale':[.3,.3,.3]}})


def prism(mesh, poly, low, high, material, side=None):
    """Closed extruded polygon; use convex or star-shaped polygons with a central kernel."""
    area=sum(a[0]*b[1]-b[0]*a[1] for a,b in zip(poly,poly[1:]+poly[:1]))
    if area<0:poly=list(reversed(poly))
    cx=sum(p[0] for p in poly)/len(poly);cz=sum(p[1] for p in poly)/len(poly)
    for a,b in zip(poly,poly[1:]+poly[:1]):
        mesh.face([(a[0],low,a[1]),(a[0],high,a[1]),(b[0],high,b[1]),(b[0],low,b[1])],side or material)
        mesh.face([(cx,high,cz),(b[0],high,b[1]),(a[0],high,a[1])],material)
        mesh.face([(cx,low,cz),(a[0],low,a[1]),(b[0],low,b[1])],side or material)


def stroke(mesh,a,b,width,low,high,material,side=None):
    dx,dz=b[0]-a[0],b[1]-a[1];length=math.hypot(dx,dz)
    nx,nz=dz/length*width/2,-dx/length*width/2
    prism(mesh,[(a[0]+nx,a[1]+nz),(b[0]+nx,b[1]+nz),
                (b[0]-nx,b[1]-nz),(a[0]-nx,a[1]-nz)],low,high,material,side)


def build_hub():
    m=Mesh()
    # Three wing-shaped regions with notched gaps for the projecting controls.
    # Notch rear corners and cheeks are derived from the console's exact basis.
    def notch(angle,lateral,depth):
        return (math.cos(angle)*depth-math.sin(angle)*lateral,
                math.sin(angle)*depth+math.cos(angle)*lateral)
    left=[(0,0),notch(3*math.pi/4,0,.420),notch(3*math.pi/4,.14,.420),
          notch(3*math.pi/4,.14,.64),(-.72,.02),(-.72,-.34),
          (-.47,-.69),(-.17,-.69),(-.13,-.49),(0,-.49)]
    right=[(-x,z) for x,z in reversed(left)]
    back=[(0,0),notch(math.pi/4,0,.420),notch(math.pi/4,.14,.420),
          notch(math.pi/4,.14,.64),(.22,.69),(-.22,.69),
          notch(3*math.pi/4,-.14,.64),notch(3*math.pi/4,-.14,.420),notch(3*math.pi/4,0,.420)]
    bays=[(-.265,-.204),(0,.246),(.265,-.204)]
    def world(p):return(p[0]+.5,p[1]+.5)
    def console_opening(x,z):
        return any(abs(-math.sin(a)*x+math.cos(a)*z)<.163
                   and .40<math.cos(a)*x+math.sin(a)*z<.73
                   for a in [-math.pi/2,math.pi/4,3*math.pi/4])
    # Six broad, solid spokes. Two levels provide a dark plinth and raised brown face.
    star=[]
    for i in range(24):
        a=i*math.pi/12
        r=.175 if i%4 in (0,1) else .064
        star.append((.5+r*math.cos(a),.45+r*math.sin(a)))
    prism(m,star,1.145,1.161,'recess')
    inner=[(.5+(x-.5)*.9,.45+(z-.45)*.9) for x,z in star]
    prism(m,inner,1.160,1.184,'trim','panel')

    for poly,(cx,cz) in zip([left,back,right],bays):
        # Continuous closed lower shell behind the decorative plates. Short notch
        # edges must be present even when too narrow to carry a relief panel.
        for a,b in zip(poly,poly[1:]+poly[:1]):
            if a==(0,0) or b==(0,0):continue
            A=(.5+a[0]*.835,.5+a[1]*.835)
            B=(.5+b[0]*.835,.5+b[1]*.835)
            m.face([(A[0],.016,A[1]),(A[0],.925,A[1]),
                    (B[0],.925,B[1]),(B[0],.016,B[1])],'panel')
            # Shoulder seals the inset body to the overhanging skirt.
            m.face([(A[0],.924,A[1]),(B[0],.924,B[1]),
                    (b[0]+.5,.924,b[1]+.5),(a[0]+.5,.924,a[1]+.5)],'recess')
        # Lower closure only: do not put a ceiling across the animated sockets.
        for a,b in zip(poly,poly[1:]+poly[:1]):
            m.face([(.5,.016,.5),(.5+a[0]*.835,.016,.5+a[1]*.835),
                    (.5+b[0]*.835,.016,.5+b[1]*.835)],'recess') if a!=(0,0) and b!=(0,0) else None
        # Top skin has a true open socket and recessed dark shaft.
        outer=[]
        for a,b in zip(poly,poly[1:]+poly[:1]):
            for i in range(4):outer.append((a[0]+(b[0]-a[0])*i/4,a[1]+(b[1]-a[1])*i/4))
        for i,(x,z) in enumerate(outer):
            X,Z=outer[(i+1)%len(outer)]
            def inner(x,z):
                a=math.atan2(z-cz,x-cx)
                return (cx+.136*math.cos(a),cz+.136*math.sin(a))
            ix,iz=inner(x,z);jx,jz=inner(X,Z)
            m.face([(ix+.5,1.145,iz+.5),(jx+.5,1.145,jz+.5),(X+.5,1.145,Z+.5),(x+.5,1.145,z+.5)],'panel')
            m.face([(ix+.5,.76,iz+.5),(jx+.5,.76,jz+.5),(jx+.5,1.145,jz+.5),(ix+.5,1.145,iz+.5)],'recess')
        m.lathe(cx+.5,cz+.5,[(.75,.135),(.761,.135)],'recess')
        m.ring(cx+.5,cz+.5,1.146,.136,.148,'recess')
        # Subtle bevel lip around each well (not a large white ring).
        m.ring(cx+.5,cz+.5,1.148,.136,.140,'trim')
        for a,b in zip(poly,poly[1:]+poly[:1]):
            if a==(0,0) or b==(0,0):continue
            dx,dz=b[0]-a[0],b[1]-a[1];length=math.hypot(dx,dz)
            ux,uz=dx/length,dz/length;nx,nz=uz,-ux
            # The notches already follow the console outline. Keep their wall
            # backing continuous; only cooling ribs need the opening exclusion.
            stroke(m,world(a),world(b),.016,.905,1.145,'panel','recess')
            if length >= .25:
                count=max(2,round(length/.045))
                for i in range(count):
                    t=(i+.5)/count
                    if console_opening(a[0]+dx*t,a[1]+dz*t):continue
                    x,z=a[0]+dx*t+.5,a[1]+dz*t+.5
                    stroke(m,(x+nx*.017-ux*.011,z+nz*.017-uz*.011),
                           (x+nx*.017+ux*.011,z+nz*.017+uz*.011),.056,.91,1.158,'panel','panel')
            # Raised continuous top perimeter trim, deliberately several pixels high.
            stroke(m,world((a[0]*.93,a[1]*.93)),world((b[0]*.93,b[1]*.93)),.018,1.145,1.163,'trim','panel')
            # Each outer wall is a closed dark backing plus substantial inset plates.
            if length<.17:continue
            mx,mz=(a[0]+b[0])*.42+.5,(a[1]+b[1])*.42+.5
            L=length*.84
            def wall(poly2,low,high,mat,side=None):
                temp=Mesh();prism(temp,poly2,low,high,mat,side)
                for pts,material in temp.faces:
                    m.face([(mx+ux*u+nx*d,y,mz+uz*u+nz*d) for u,d,y in pts],material)
            wall([(-L/2,.016),(L/2,.016),(L/2,.923),(-L/2,.923)],-.02,.0,'recess')
            panel=[(-L*.46,.04),(L*.23,.04),(L*.46,.18),(L*.46,.73),(-L*.46,.885)]
            wall(panel,.002,.037,'panel','recess')
            # Angular relief channels and a smaller inset plate, with exposed depth.
            inner=[(-L*.39,.11),(L*.17,.11),(L*.35,.22),(L*.35,.66),(-L*.39,.79)]
            wall(inner,.038,.052,'trim','recess')
            inset=[(-L*.35,.14),(L*.14,.14),(L*.30,.24),(L*.30,.62),(-L*.35,.74)]
            wall(inset,.053,.059,'panel','recess')
            for sign in [-1,1]:
                u=sign*L*.47
                wall([(u-.012,.025),(u+.012,.025),(u+.012,.90),(u-.012,.90)],.005,.052,'panel','recess')
            # Stepped secondary rail, distinct from the inset plate.
            path=[(-L*.13,.16),(-L*.13,.33),(-L*.03,.39),(-L*.03,.51),(-L*.13,.56),(-L*.13,.70)]
            for A,B in zip(path,path[1:]):
                du,dy=B[0]-A[0],B[1]-A[1];le=math.hypot(du,dy)
                wu,wy=dy/le*.012,-du/le*.012
                wall([(A[0]+wu,A[1]+wy),(B[0]+wu,B[1]+wy),(B[0]-wu,B[1]-wy),(A[0]-wu,A[1]-wy)],.060,.077,'trim','panel')
        # Raised arrow emblems point away from the socket, with a separate terminal hexagon.
        length=math.hypot(cx,cz);ux,uz=cx/length,cz/length
        def mark(f,s):return(cx+.5+ux*f-uz*s,cz+.5+uz*f+ux*s)
        for A,B in [(mark(.11,-.18),mark(.29,-.12)),(mark(.29,-.12),mark(.34,0)),
                    (mark(.34,0),mark(.29,.12)),(mark(.29,.12),mark(.11,.18)),
                    (mark(.11,.18),mark(.19,.05))]:
            stroke(m,A,B,.037,1.145,1.174,'trim','panel')
        h=mark(.40,0)
        points=[(h[0]+.046*math.cos(i*math.pi/3),h[1]+.046*math.sin(i*math.pi/3)) for i in range(6)]
        for A,B in zip(points,points[1:]+points[:1]):stroke(m,A,B,.012,1.145,1.171,'trim','panel')

    # Three recessed consoles at the gaps: a real projecting box, framed hexagon,
    # cool white inward-facing light strips and original geometric glyph relief.
    for angle in [-math.pi/2,math.pi/4,3*math.pi/4]:
        nx,nz=math.cos(angle),math.sin(angle);ux,uz=-nz,nx
        def transform(pts):return[(.5+ux*x+nx*z,y,.5+uz*x+nz*z) for x,y,z in pts]
        temp=Mesh()
        temp.box((-.144,.016,.385),(.144,.851,.63),'panel')
        temp.box((-.13,.85,.43),(.13,1.05,.68),'panel')
        # Solid recess walls hide the lowered modules behind each console.
        temp.box((-.175,1.05,.405),(.175,1.143,.441),'panel')
        for lo,hi in [(-.175,-.128),(.128,.175)]:
            temp.box((lo,.85,.441),(hi,1.143,.675),'panel')
        temp.box((-.113,.867,.681),(.113,1.031,.687),'recess')
        # Frame and hexagonal centre on the vertical projecting face.
        for A,B in [((-.116,.871),(.116,.871)),((-.116,1.027),(.116,1.027)),
                    ((-.116,.871),(-.116,1.027)),((.116,.871),(.116,1.027))]:
            temp.box((min(A[0],B[0])-.005,min(A[1],B[1])-.005,.687),
                     (max(A[0],B[0])+.005,max(A[1],B[1])+.005,.700),'trim')
        hexagon=[(.053*math.cos(i*math.pi/3),.949+.039*math.sin(i*math.pi/3)) for i in range(6)]
        for A,B in zip(hexagon,hexagon[1:]+hexagon[:1]):
            strip=Mesh();stroke(strip,A,B,.008,.687,.701,'trim','panel')
            for pts,mat in strip.faces:temp.face([(x,z,y) for x,y,z in reversed(pts)],mat)
        for low,high in [((-.112,.945,.687),(-.053,.953,.701)),((.053,.945,.687),(.112,.953,.701)),
                         ((-.004,.988,.687),(.004,1.027,.701)),((-.004,.871,.687),(.004,.910,.701))]:
            temp.box(low,high,'trim')
        for x in [-.128,.116]:temp.box((x,1.055,.45),(x+.012,1.079,.57),'light')
        temp.box((-.12,1.055,.442),(.12,1.079,.454),'light')
        # Not traced glyphs: original short angular bars in two rows.
        for row in range(2):
            for col in range(6):
                x=-.10+col*.036;z=.585+row*.043
                temp.box((x,1.05,z),(x+.024,1.063,z+.006),'trim')
                temp.box((x+(col%2)*.018,1.05,z),(x+(col%2)*.018+.006,1.063,z+.026),'trim')
                if (col+row)%3!=0:temp.box((x+.009,1.05,z+.019),(x+.029,1.063,z+.025),'trim')
        # This local console basis is reflected; reverse winding for outward faces.
        for pts,mat in temp.faces:m.face(transform(list(reversed(pts))),mat)
    m.save('block/atlantis_zpm_hub')
    model('block/atlantis_zpm_hub',parent='minecraft:block/block',display={
        'gui':{'rotation':[30,225,0],'translation':[0,-1,0],'scale':[.5,.5,.5]},
        'ground':{'scale':[.25,.25,.25]},'fixed':{'scale':[.5,.5,.5]},
        'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.3,.3,.3]}})
    # Declare the custom geometry explicitly for the inventory item as well.
    model('item/atlantis_zpm_hub',**{'model':'jsgzpm:models/block/atlantis_zpm_hub.obj'},parent='minecraft:block/block',display={
        'gui':{'rotation':[30,225,0],'translation':[0,-1,0],'scale':[.5,.5,.5]},
        'ground':{'scale':[.25,.25,.25]},'fixed':{'scale':[.5,.5,.5]},
        'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.3,.3,.3]}})


def build_wall_holders():
    """One authored bay per occupied block; existing three-part structures repeat it."""
    for kind,tilt,socket_y,socket_z in [('array',45,.50,.50),('column',45,.50,.50)]:
        m=Mesh()
        # Genuine hollow receptacle: skin, inner bore, raised lip and a recessed floor.
        cup=Mesh()
        inner=Mesh();inner.lathe(0,0,[(-.525,.156),(0,.156)],'recess',n=16,cap=False)
        for pts,mat in inner.faces:cup.face(list(reversed(pts)),mat)
        cup.ring(0,0,0,.156,.211,'trim',n=16)
        cup.ring(0,0,.004,.158,.169,'binder',n=16)
        cup.lathe(0,0,[(-.529,.156),(-.525,.156)],'recess',n=16)
        # One full wedge: sloped front, vertical back, closed side/top/bottom.
        # Rectangular annulus leaves a real opening through the sloped face.
        def front_point(i):
            angle=i*2*math.pi/16
            x,z=math.cos(angle),math.sin(angle)
            radius=1/max(abs(x),abs(z))
            return (x*radius*.465,0,z*radius*.485*math.sqrt(2))
        for i in range(16):
            j=(i+1)%16
            fi,fj=front_point(i),front_point(j)
            ai,aj=i*2*math.pi/16,j*2*math.pi/16
            ci=(.211*math.cos(ai),0,.211*math.sin(ai))
            cj=(.211*math.cos(aj),0,.211*math.sin(aj))
            cup.face([ci,cj,fj,fi],'panel')
            # Inverse-rotated backing points preserve each front point's height.
            def back(p):
                wy=.5+p[2]/math.sqrt(2)
                return (p[0],((wy-.5)-.495)/math.sqrt(2),((wy-.5)+.495)/math.sqrt(2))
            bi,bj=back(fi),back(fj)
            cup.face([bi,fi,fj,bj],'panel')
            cup.face([back((0,0,0)),bi,bj],'recess')
        # Trim and indicators follow the full sloped face, not a vertical plate.
        for x in [-.435,.413]:
            cup.box((x,.001,-.61),(x+.022,.013,.61),'trim')
            cup.box((x+.005,.014,-.14),(x+.017,.019,.14),'light')
        for z in [-.63,.61]:
            cup.box((-.435,.001,z),(.435,.013,z+.020),'trim')
        for side in [-1,1]:
            x=side*.30
            for z in [-.45,.32]:
                cup.box((x-.009,.001,z),(x+.009,.012,z+.13),'trim')
                cup.box((min(x,side*.17),.001,z),
                        (max(x,side*.17),.012,z+.018),'trim')
        # Four raised socket lugs, outside the crystal's travel envelope.
        for j in range(4):
            a=j*math.pi/2
            x,z=.193*math.cos(a),.193*math.sin(a)
            cup.box((x-.018,-.035,z-.018),(x+.018,.020,z+.018),'trim')
        a=math.radians(tilt);c,t=math.cos(a),math.sin(a)
        for pts,mat in cup.faces:
            m.face([(.5+x,socket_y+y*c+z*t,socket_z-y*t+z*c) for x,y,z in pts],mat)
        if kind=='column':
            # Turn the entire wedge onto its side to bridge two perpendicular walls.
            m.faces=[([(1-y,x,z) for x,y,z in pts],mat) for pts,mat in m.faces]
        stem='block/ancient_zpm_'+kind
        m.save(stem)
        display={'gui':{'rotation':[25,225,0],'scale':[.75,.75,.75]},
                 'ground':{'scale':[.35,.35,.35]},'fixed':{'scale':[.65,.65,.65]}}
        model(stem,parent='minecraft:block/block',display=display)
        # Keep the item icon as one bay: placed hardware expands to three occupied blocks.
        model('item/ancient_zpm_'+kind,model='jsgzpm:models/'+stem+'.obj',
              parent='minecraft:block/block',display=display)
        state={'multipart':[{'when':{'facing':f},'apply':dict({'model':'jsgzpm:'+stem},**({'y':r} if r else {}))}
                            for f,r in [('north',0),('east',90),('south',180),('west',270)]]}
        (ASSETS/'blockstates'/('ancient_zpm_'+kind+'.json')).write_text(json.dumps(state,indent=2)+'\n')


if __name__ == '__main__':
    for name in PALETTE: png(ASSETS/'textures/block/ancient'/f'{name}.png',name)
    build_zpm()
    build_hub()
    build_wall_holders()
    print(f'Rebuilt original ZPM, hub and {len(PALETTE)} reusable Ancient materials.')

