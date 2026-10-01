"""Lantian letterforms transcribed from the supplied alphabet, and machined surfaces.
The panel inscriptions spell ENERGY, POWER, CONTROL and ATLANTIS.
"""
FONT={
'a':['111','001','001','111','001','001','001'],
'c':['111','101','101','101','101','101','101'],
'e':['101','100','100','111','101','101','101'],
'g':['101','101','101','100','111','111','111'],
'i':['111','111','000','000','000','111','111'],
'l':['111','110','110','011','110','110','111'],
'n':['100','100','110','110','111','101','101'],
'o':['101','101','111','001','111','010','101'],
'p':['101','100','100','100','100','101','101'],
'r':['111','111','000','000','101','101','111'],
's':['101','101','111','111','101','100','100'],
't':['101','101','000','000','111','101','101'],
'w':['101','101','111','001','001','101','111'],
'y':['111','100','100','100','110','100','100']}
LABELS={'panel':'energy','trim':'power','recess':'control','binder':'atlantis','pedestal_dark':'control','pedestal_metal':'power'}
def glyph_at(word,x,y):
    if y<0 or y>=14 or x<0:return False
    letter=x//8
    return letter<len(word) and (x%8)//2<3 and FONT[word[letter]][y//2][(x%8)//2]=='1'
def detail(name,x,y,rgb):
    if name not in LABELS:return rgb
    # Recessed rectangular channels, lit upper bevel and dark lower bevel.
    gx=x%128;gy=y%128
    shift=0
    if gx in (8,119) or gy in (8,119):shift=-29
    elif gx in (9,118) or gy in (9,118):shift=19
    if (22<=gx<=105 and gy in (27,99)) or (gx in (22,105) and 27<=gy<=99):shift=-19
    if (22<=gx<=105 and gy==28):shift=12
    # Sparse bolt heads, rings and screwdriver cuts.
    for cx,cy in ((15,15),(112,15),(15,112),(112,112)):
        d=(gx-cx)**2+(gy-cy)**2
        if d<=12:shift=35 if gy<cy else -15
        if abs(gx-cx)<=2 and gy==cy:shift=-35
    word=LABELS[name];tx=gx-(128-len(word)*8)//2;ty=gy-105
    if glyph_at(word,tx,ty):shift=-35
    elif glyph_at(word,tx-1,ty-1):shift=23
    # Small cyan instrument inserts, never paint over an entire panel.
    if 47<=gx<=81 and gy in (34,35):return (42,136,149)
    if 50<=gx<=78 and gy==34:return (104,193,201)
    # Directional machining marks keep flat surfaces from looking like wood.
    shift+=((y*7)%5-2)*.5
    return [max(0,min(255,round(c+shift))) for c in rgb]
