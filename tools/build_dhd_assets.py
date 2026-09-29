"""Original waist-height Atlantis desk; runtime glyphs come from installed JSG."""
from build_ancient_assets import Mesh,prism,stroke,model,ASSETS
import json

def build():
 from build_pedestal_assets import MATERIALS
 from build_ancient_assets import PALETTE
 PALETTE.update(MATERIALS)
 m=Mesh()
 # Coordinates local x right, z front; convert to north-facing model at the end.
 def box(a,b,mat='panel'):m.box(a,b,mat)
 # Rear desktop, two projecting arm wings and angled open legs.
 prism(m,[(-1.43,-.43),(1.43,-.43),(1.43,.43),(-1.43,.43)],.80,.88,'panel','binder')
 for sign in [-1,1]:
  lo,hi=sorted([sign*.62,sign*1.43])
  prism(m,[(lo,.25),(hi,.25),(hi,1.43),(lo,1.20)],.80,.90,'panel','binder')
  x=sign*1.08
  box((x-.10,.025,-.30),(x+.10,.10,1.37),'binder')
  for z in [-.18,1.15]:
   # Slender tilted supports beneath each arm, preserving the open knee space.
   for y0,y1 in [(.10,.80)]:
    pts=[(x-.065,y0,z),(x+.065,y0,z),(x+.065,y1,z-.15),(x-.065,y1,z-.15)]
    for offset in [-.045,.045]:m.face([(a,b,c+offset) for a,b,c in (pts if offset<0 else list(reversed(pts)))],'panel')
    for a,b in zip(pts,pts[1:]+pts[:1]):m.face([(a[0],a[1],a[2]-.045),(b[0],b[1],b[2]-.045),(b[0],b[1],b[2]+.045),(a[0],a[1],a[2]+.045)],'binder')
  for k in range(12):
   z=-.3+k*.135
   box((x-.28,.90,z),(x-.19,.912,z+.026),'trim')
   box((x+.19,.90,z),(x+.28,.912,z+.026),'trim')
 # Raised rear white bar, capped bronze shoulders.
 box((-1.4,.88,-.45),(1.4,1.035,-.35),'binder')
 box((-1.22,.915,-.348),(1.22,1.01,-.335),'light')
 # Raised bevelled central keyboard.
 prism(m,[(-.47,-.42),(.47,-.42),(.49,.30),(.35,.48),(-.35,.48),(-.49,.30)],.88,.925,'trim','binder')
 for i in range(36):
  x=(i%6-2.5)*.145;z=(i//6-2.5)*.115-.035
  prism(m,[(x-.065,z),(x,z-.05),(x+.065,z),(x,z+.05)],.925,.94,'panel','trim')
 m.lathe(0,.405,[(.925,.067),(.94,.064)],'pedestal_metal',n=24)
 # Crystal utility pads and circular side controls.
 for sign in [-1,1]:
  for row in range(3):
   for col in range(2):
    x=sign*(.67+col*.25);z=-.23+row*.20
    prism(m,[(x-.10,z-.06),(x+.08,z-.06),(x+.11,z+.04),(x-.08,z+.06)],.89,.935,'crystal_pale','trim')
 # Flip local front to north, maintaining outward winding.
 m.faces=[([(x+.5,y,.5-z) for x,y,z in reversed(pts)],mat) for pts,mat in m.faces]
 m.save('block/atlantis_pegasus_dhd')
 from build_pedestal_assets import MATERIALS
 from build_ancient_assets import PALETTE
 PALETTE.update(MATERIALS)
 display={'gui':{'rotation':[25,225,0],'scale':[.38,.38,.38]},'ground':{'scale':[.2,.2,.2]}}
 model('block/atlantis_pegasus_dhd',display=display)
 model('item/atlantis_pegasus_dhd',model='jsgzpm:models/block/atlantis_pegasus_dhd.obj',display=display)
 (ASSETS/'models/block/atlantis_pegasus_dhd_part.json').write_text(json.dumps({'textures':{'particle':'jsgzpm:block/ancient/panel'},'elements':[]},indent=2)+'\n')
if __name__=='__main__':build()
