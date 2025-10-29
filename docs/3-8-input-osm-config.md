# medlabs-heros

## 3.8. Input files: OSM configuration

This file is only important for the interactive simulation, and can be left blank in a batch simulation. The file indicates what OSM nodes, ways and relations are plotted on the map, in which order, and with which color. An example is:

```
layerName,key,value,outlineColor,fillColor,display,transform
natural,natural,water,null,#99CCFF,true,true
natural,landuse,cemetery,"rgb(0,255,0)","rgb(127,255,197)",true,true
natural,landuse,grass,"rgb(0,255,0)","rgb(127,255,197)",true,true
natural,natural,*,"rgb(0,255,0)","rgb(127,255,197)",true,true
roads,highway,motorway,"rgb(127,0,0)",null,true,true
roads,highway,trunk,"rgb(255,0,0)",null,true,true
roads,highway,primary,#FF8000,null,true,true
roads,highway,secondary,#FFF800,null,true,true
roads,highway,pedestrian,#AAAAAA,null,true,true
roads,highway,sidewalk,#AAAAAA,null,true,true
roads,highway,cycleway,#999999,null,true,true
roads,highway,*,"rgb(127,127,127)",null,true,true
railways,railway,*,"rgb(0,19,127)",null,true,true
waterways,waterway,*,"rgb(0,148,255)",null,true,true
buildings,building,office,#FF00FF,#FF00FF,true,true
buildings,building,university,#7F00FF,#FF00FF,true,true
buildings,building,*,"rgb(200,200,200)","rgb(200,200,200)",true,true
```

- `layerName` is a key that can turn a set of features on the map on or off in the animation.
- `key` and `value` are the key and value in OSM that identify a feature. `value` can be `*` to include all.
- `outlineColor` and `fillColor` are the color of the line and area. It can be specified as a `"rgb(r,g,b)"` string or a hex string `#rrgbb`. When a line or fill is not wanted, use `null`.
- `display` should be `TRUE` to indicate it has to be drawn. Turn to `FALSE` when a certain feature should not be drawn, but you want to keep it in the OSM config file.
- `transform`: typically `TRUE` to indicate it should zoom and pan with the map. A legend, for instance does not zoom or pan. For all elements, the value is typically `TRUE`.
