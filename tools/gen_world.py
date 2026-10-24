import sys
sys.path.insert(0,'.')
from coast_src import LAND, WATER
out=[]
for name,pts in LAND.items():
    out.append("L - "+" ".join(f"{x:g} {y:g}" for x,y in pts))
for name,pts in WATER.items():
    out.append("W - "+" ".join(f"{x:g} {y:g}" for x,y in pts))
open(sys.argv[1],'w').write("\n".join(out)+"\n")
print(len(LAND),"land polygons,",len(WATER),"water polygons,",sum(len(p) for p in LAND.values())+sum(len(p) for p in WATER.values()),"points")
