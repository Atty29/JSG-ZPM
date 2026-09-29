"""Solid Atlantis desk, tilted triangular keypad and atlas-safe original materials."""
from build_ancient_assets import Mesh,prism,stroke,model,ASSETS,ROOT,PALETTE
from build_pedestal_assets import MATERIALS
import json,math
PALETTE.update(MATERIALS)
TOP=.90;SLOPE=.22

def buttons():
 # A seven-unit equilateral triangle with its three two-unit corners removed.
 # This gives 37 identical cells: 36 glyphs and the central triangular core.
 # Compensate for the slope so the actual 3D faces, not just their XZ projection,
 # have equal side lengths.
 w=.18;h=w*math.sqrt(3)/2/math.sqrt(1+SLOPE*SLOPE)
 result=[];core=None
 for row in range(2,7):
  count=2*row+1
  for col in range(count):
   if row==5 and col in (0,10):continue
   if row==6 and (col<3 or col>9):continue
   x=.63+(col-row)*w/2
   z=(row-14/3+.5)*h
   up=col%2==0
   poly=[(x-w/2,z+(h/2 if up else -h/2)),(x+w/2,z+(h/2 if up else -h/2)),(x,z+(-h/2 if up else h/2))]
   if row==4 and col==4:core=poly
   else:result.append(poly)
 assert len(result)==36 and core is not None
 return result+[core]

def build():
 m=Mesh()
 # Straight, continuous three-block desk with an enclosed chamfered pedestal.
 outline=[(-1.42,-.45),(1.42,-.45),(1.46,-.25),(1.46,.36),(1.27,.48),(-1.27,.48),(-1.46,.36),(-1.46,-.25)]
 prism(m,[(x*.91,z*.88) for x,z in outline],.04,.13,'binder')
 prism(m,[(x*.86,z*.8) for x,z in outline],.13,.68,'panel','recess')
 prism(m,outline,.68,.76,'panel','binder')
 for x in [-1.20,-.62,0,.62,1.20]:
  m.box((x-.022,.16,-.367),(x+.022,.67,-.337),'trim')
 # Raised back light and beveled perimeter.
 m.box((-1.39,.76,-.49),(1.39,.99,-.45),'panel')
 m.box((-1.25,.84,-.447),(1.25,.965,-.435),'light')
 for sign in [-1,1]:m.box((sign*1.35-.045,.76,-.34),(sign*1.35+.045,.83,.37),'trim')
 # Sloped keypad face; all button and light vertices share this transform.
 pad=Mesh()
 prism(pad,[(.09,-.425),(1.16,-.425),(1.23,-.22),(1.23,.22),(1.08,.405),(.18,.405),(.03,.22),(.03,-.22)],-.075,-.017,'panel','binder')
 for i,poly in enumerate(buttons()):
  prism(pad,poly,-.017,-.005,'pedestal_metal')
  cx=sum(x for x,z in poly)/len(poly);cz=sum(z for x,z in poly)/len(poly)
  inner=[(cx+(x-cx)*.84,cz+(z-cz)*.84) for x,z in poly]
  prism(pad,inner,-.004,0,'crystal_warm','trim')
 # Triangle motif inside the central core.
 for a,b in zip([(.585,.026),(.675,.026),(.63,-.052)],[(.675,.026),(.63,-.052),(.585,.026)]):stroke(pad,a,b,.010,.001,.004,'light')
 for pts,mat in pad.faces:m.face([(x,TOP+y-SLOPE*z,z) for x,y,z in pts],mat)
 # Stepped clear control crystals to the left; etched original circuit traces.
 for row,count in enumerate([3,2,2]):
  for col in range(count):
   x=-1.06+col*.31+row*.06;z=-.20+row*.20;y=.79+(.04 if row==0 else 0)
   poly=[(x-.12,z-.063),(x+.12,z-.063),(x+.12,z+.025),(x+.065,z+.063),(x-.065,z+.063),(x-.12,z+.025)]
   prism(m,poly,y,y+.022,'pedestal_metal','trim')
   for k in range(4):
    xx=x-.075+k*.043
    stroke(m,(xx,z-.045),(xx+.025,z+.015),.006,y+.023,y+.026,'light')
    stroke(m,(xx+.025,z+.015),(xx+.025,z+.043),.006,y+.023,y+.026,'light')
 for x,z in [(-1.04,.33),(-.48,.33),(-.20,-.22)]:
  prism(m,[(x-.13,z-.047),(x+.13,z-.047),(x+.16,z),(x+.13,z+.047),(x-.13,z+.047),(x-.16,z)],.762,.774,'crystal_warm','trim')
 m.faces=[([(.5-x,y,.5-z) for x,y,z in pts],mat) for pts,mat in m.faces]
 m.save('block/atlantis_pegasus_dhd')
 # Forge OBJ UVs address a stitched sprite, not a repeating standalone texture.
 # Fit continuous UVs into its interior; out-of-range UVs sample other atlas sprites.
 path=ASSETS/'models/block/atlantis_pegasus_dhd.obj';lines=path.read_text().splitlines()
 uv=[tuple(map(float,l.split()[1:])) for l in lines if l.startswith('vt ')]
 mins=[min(p[i] for p in uv) for i in range(2)];maxs=[max(p[i] for p in uv) for i in range(2)]
 fixed=[]
 for line in lines:
  if line.startswith('vt '):
   v=list(map(float,line.split()[1:]));line='vt '+' '.join(str(.015+.97*(v[i]-mins[i])/(maxs[i]-mins[i])) for i in range(2))
  fixed.append(line)
 path.write_text('\n'.join(fixed)+'\n')
 display={'gui':{'rotation':[25,225,0],'scale':[.38,.38,.38]},'ground':{'scale':[.2,.2,.2]}}
 model('block/atlantis_pegasus_dhd',display=display)
 model('item/atlantis_pegasus_dhd',model='jsgzpm:models/block/atlantis_pegasus_dhd.obj',display=display)
 (ASSETS/'models/block/atlantis_pegasus_dhd_part.json').write_text(json.dumps({'textures':{'particle':'jsgzpm:block/ancient/panel'},'elements':[]},indent=2)+'\n')
 # One generated layout drives drawing, hit testing and the authored model.
 java='package uk.co.atty29.jsgzpm.holder;\npublic final class DHDLayout {\n private DHDLayout(){}\n public static final double[][][] BUTTONS={\n'
 java+=',\n'.join('  {'+','.join('{'+f'{x:.6f},{z:.6f}'+'}' for x,z in poly)+'}' for poly in buttons())
 java+='\n };\n}\n'
 (ROOT/'src/main/java/uk/co/atty29/jsgzpm/holder/DHDLayout.java').write_text(java)
if __name__=='__main__':build()
