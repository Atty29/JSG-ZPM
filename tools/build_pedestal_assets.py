"""Original single-ZPM pedestal, with separate charge-controlled emissive faces."""
from pathlib import Path
import math,json,struct,zlib
from build_ancient_assets import Mesh,prism,model,png,PALETTE,ASSETS,ROOT

MATERIALS={'pedestal_dark':(23,28,31),'pedestal_metal':(118,132,139),
           'pedestal_cyan_off':(13,49,55),'pedestal_white_off':(65,73,78)}
PALETTE.update(MATERIALS)

def build():
    # Fine, low-contrast metal grain rather than the hub's coarse mineral surface.
    for name,base in MATERIALS.items():
        def chunk(tag,data):return struct.pack('!I',len(data))+tag+data+struct.pack('!I',zlib.crc32(tag+data))
        rows=[]
        for y in range(256):
            row=bytearray([0])
            for x in range(256):
                grain=((x*31+y*97+x*y*7)%11-5)*.3
                row.extend(max(0,min(255,round(c+grain))) for c in base)
            rows.append(bytes(row))
        data=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',256,256,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows),9))+chunk(b'IEND',b'')
        (ASSETS/'textures/block/ancient'/f'{name}.png').write_bytes(data)
    m=Mesh();lights=Mesh()
    def lamp(lo,hi,white=False):
        temp=Mesh();temp.box(lo,hi,'pedestal_white_off' if white else 'pedestal_cyan_off')
        m.faces.extend(temp.faces)
        for pts,mat in temp.faces:
            a,b,c=pts[:3];u=[b[i]-a[i] for i in range(3)];v=[c[i]-a[i] for i in range(3)]
            n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
            length=math.sqrt(sum(q*q for q in n))
            lights.face([tuple(p[i]+n[i]/length*.0008 for i in range(3)) for p in pts],mat)
    outline=[(.14,.08),(.86,.08),(.94,.20),(.94,.80),(.82,.92),(.18,.92),(.06,.80),(.06,.20)]
    prism(m,outline,.025,.095,'pedestal_dark','binder')
    prism(m,[(.5+(x-.5)*.94,.5+(z-.5)*.94) for x,z in outline],.095,.59,'pedestal_dark')
    # Tall inset body panels and slim metal edge bands.
    for x in [.11,.86]:m.box((x,.12,.13),(x+.03,.59,.84),'binder')
    m.box((.20,.12,.096),(.80,.53,.109),'binder')
    m.box((.22,.14,.084),(.78,.51,.098),'pedestal_dark')
    m.box((.47,.14,.078),(.49,.51,.09),'binder')
    # Broad upper deck with a shallow open socket. The longest tip sinks 0.075 blocks.
    for i in range(32):
        a=i*math.pi/16;b=(i+1)*math.pi/16
        def pt(r,t,y):return(.5+r*math.cos(t),y,.5+r*math.sin(t))
        # Octagonal perimeter, no plate across the central bore.
        def edge(t,y):
            # Regular clipped corners; full deck remains inside the body footprint.
            r=.40/(math.cos((t+math.pi/8)%(math.pi/4)-math.pi/8))
            return pt(r,t,y)
        m.face([pt(.115,a,.68),pt(.115,b,.68),edge(b,.68),edge(a,.68)],'pedestal_dark')
        m.face([edge(a,.59),edge(a,.68),edge(b,.68),edge(b,.59)],'binder')
    inner=Mesh();inner.lathe(.5,.5,[(.59,.115),(.68,.115)],'binder',n=24,cap=False)
    for pts,mat in inner.faces:m.face(list(reversed(pts)),mat)
    m.ring(.5,.5,.681,.115,.135,'binder',n=24)
    # Two actual hollow metal rings, not solid discs through the glass.
    for low,high in [(.785,.825),(.980,1.020)]:
        m.lathe(.5,.5,[(low,.153),(high,.153)],'pedestal_metal',n=24,cap=False)
        inner=Mesh();inner.lathe(.5,.5,[(low,.112),(high,.112)],'pedestal_metal',n=24,cap=False)
        for pts,mat in inner.faces:m.face(list(reversed(pts)),mat)
        m.ring(.5,.5,high,.112,.153,'pedestal_metal',n=24)
        bottom=Mesh();bottom.ring(.5,.5,low,.112,.153,'pedestal_metal',n=24)
        for pts,mat in bottom.faces:m.face(list(reversed(pts)),mat)
    # Discreet rear struts carry both rings.
    for x in [.38,.60]:m.box((x,.68,.635),(x+.020,.997,.665),'pedestal_metal')
    # Raised rear electronics bank and its cyan vents.
    m.box((.16,.68,.755),(.84,.895,.87),'binder')
    m.box((.18,.70,.74),(.82,.875,.758),'pedestal_dark')
    for x in [.215,.265,.315,.665,.715,.765]:lamp((x,.72,.729),(x+.020,.856,.741))
    for i in range(16):
        x=.20+i*.039
        lamp((x,.891,.794),(x+.015,.916,.840))
    # Cyan chevrons and status pads on the upper deck.
    for x in [.415,.515]:lamp((x,.682,.305),(x+.07,.690,.36))
    for x in [.17,.80]:
        lamp((x,.682,.39),(x+.024,.690,.46))
        lamp((x-.025,.682,.39),(x+.024,.690,.409))
    # Original angular glyph clusters in a horseshoe around the crystal.
    for k in range(9):
        angle=math.pi+(k+1)*math.pi/10
        cx=.5+.29*math.cos(angle);cz=.5+.29*math.sin(angle)
        for row in range(2):
            x=cx-.024;z=cz+row*.022
            lamp((x,.682,z),(x+.040,.686,z+.007),True)
            lamp((x+(k%2)*.023,.682,z),(x+(k%2)*.023+.007,.686,z+.018),True)
    m.box((.26,.555,.068),(.74,.63,.09),'binder')
    lamp((.28,.567,.061),(.72,.617,.069),True)
    m.save('block/ancient_zpm_pedestal')
    display={'gui':{'rotation':[25,225,0],'scale':[.8,.8,.8]},'ground':{'scale':[.4,.4,.4]}}
    model('block/ancient_zpm_pedestal',parent='minecraft:block/block',display=display)
    model('item/ancient_zpm_pedestal',model='jsgzpm:models/block/ancient_zpm_pedestal.obj',parent='minecraft:block/block',display=display)
    for stem in ['block','item']:
        path=ASSETS/'models'/stem/'ancient_zpm_pedestal.json'
        data=json.loads(path.read_text());data['textures']['particle']='jsgzpm:block/ancient/pedestal_dark'
        path.write_text(json.dumps(data,indent=2)+'\n')
    state={'multipart':[{'when':{'facing':f},'apply':dict({'model':'jsgzpm:block/ancient_zpm_pedestal'},**({'y':r} if r else {}))} for f,r in [('north',0),('east',90),('south',180),('west',270)]]}
    (ASSETS/'blockstates/ancient_zpm_pedestal.json').write_text(json.dumps(state,indent=2)+'\n')
    # Bounded generated methods avoid Java's method-size limit. Each row is a quad + RGB.
    rows=[]
    for pts,mat in lights.faces:
        color=(1.,1.,1.) if mat=='pedestal_white_off' else (.08,1.,1.)
        rows.append('            {'+', '.join(f'{q:.6f}F' for p in pts for q in p)+', '+', '.join(f'{q:.3f}F' for q in color)+'}')
    chunks=[rows[i:i+48] for i in range(0,len(rows),48)]
    java='package uk.co.atty29.jsgzpm.client;\n\n// Generated by tools/build_pedestal_assets.py; only emissive panel surfaces.\npublic final class PedestalLightGeometry {\n    private PedestalLightGeometry() {}\n    public static final float[][][] GROUPS = {'+', '.join(f'group{i}()' for i in range(len(chunks)))+'};\n'
    for i,chunk in enumerate(chunks):java+=f'    private static float[][] group{i}() {{\n        return new float[][] {{\n'+',\n'.join(chunk)+'\n        };\n    }\n'
    java+='}\n'
    (ROOT/'src/main/java/uk/co/atty29/jsgzpm/client/PedestalLightGeometry.java').write_text(java)
    print(f'Pedestal: {len(m.faces)} faces, {len(lights.faces)} emissive quads, two metal rings.')

if __name__=='__main__':build()
