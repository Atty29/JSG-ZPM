"""Solid Atlantis desk, tilted triangular keypad and atlas-safe original materials."""
from build_ancient_assets import Mesh,prism,stroke,model,ASSETS,ROOT,PALETTE
from build_pedestal_assets import MATERIALS
import json,math
PALETTE.update(MATERIALS)
TOP=.90;SLOPE=.22

def buttons():
 # A shared triangular lattice. Remove six cells whose union is exactly the core hexagon.
 result=[]
 w=.21;h=.125
 for row,count in enumerate([5,7,9,9,7,5]):
  for col in range(count):
   if row in (2,3) and col in (2,3,4):continue
   x=.63+(col-(count-1)/2)*w/2;z=(row-2.5)*h
   up=(col%2==0) if row<3 else (col%2!=0)
   poly=[(x-w/2,z+(h/2 if up else -h/2)),(x+w/2,z+(h/2 if up else -h/2)),(x,z+(-h/2 if up else h/2))]
   result.append(poly)
 cx=.525
 result.append([(cx-w/2,-h),(cx+w/2,-h),(cx+w,0),(cx+w/2,h),(cx-w/2,h),(cx-w,0)])
 return result

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
 m.box((-1.39,.76,-.44),(1.39,.99,-.35),'panel')
 m.box((-1.25,.84,-.347),(1.25,.965,-.335),'light')
 for sign in [-1,1]:m.box((sign*1.35-.045,.76,-.34),(sign*1.35+.045,.83,.37),'trim')
 # Sloped keypad face; all button and light vertices share this transform.
 pad=Mesh()
 prism(pad,[(.09,-.405),(1.16,-.405),(1.23,-.22),(1.23,.22),(1.08,.405),(.18,.405),(.03,.22),(.03,-.22)],-.075,-.017,'panel','binder')
 for i,poly in enumerate(buttons()):
  prism(pad,poly,-.017,-.005,'pedestal_metal')
  cx=sum(x for x,z in poly)/len(poly);cz=sum(z for x,z in poly)/len(poly)
  inner=[(cx+(x-cx)*.84,cz+(z-cz)*.84) for x,z in poly]
  prism(pad,inner,-.004,0,'crystal_warm','trim')
 # Triangle motif inside the central core.
 for a,b in zip([(.445,.052),(.605,.052),(.525,-.070)],[(.605,.052),(.525,-.070),(.445,.052)]):stroke(pad,a,b,.010,.001,.004,'light')
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
