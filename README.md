# medlabs-heros

### Agent-Based Model of Covid-19 spread in European cities for the [HERoS project](https://www.heros-project.eu/)

#### Models and libraries

The models have been heavily based on the PhD thesis work of Minxing Zhang at TU Delft, which has been described in detail in the following literature references:

- Zhang, Mingxin. Large-scale agent-based social simulation. A study on epidemic prediction and control. Doctoral Thesis (2016). Delft University of Technology, The Netherlands. doi: https://doi.org/10.4233/uuid:8d0f67a3-d8e6-43ee-acc5-1633c617e023
- Zhang, Mingxin, Verbraeck, Alexander, Meng, Rongqing, Chen, Bin and Qiu, Xiaogang (2016) 'Modeling Spatial Contacts for Epidemic Prediction in a Large-Scale Artificial City' Journal of Artificial Societies and Social Simulation 19 (4) 3 <http://jasss.soc.surrey.ac.uk/19/4/3.html>. doi: 10.18564/jasss.3148


#### The build-up of the libraries is as follows:

<table>
  <tr><td>HERoS library for Covid-19 spread in cities (https://www.heros-project.eu/)</td></tr>
  <tr><td>MEDLABS disease transmission library (https://simulation.tudelft.nl/medlabs)</td></tr>
  <tr><td>DSOL simulation library (https://simulation.tudelft.nl)</td></tr>
  <tr><td>djutils (https://djutils.org)</td></tr>
  <tr><td>djunits (https://djunits.org)</td></tr>
  <tr><td>djutils-base (https://djutils.org)</td></tr>
</table>

Using Maven, all libraries are resolved automatically for the HERoS project library. There is also a 'fat jar' in the folder `jar` that contains the heros library plus all libraries on which it is dependent.


#### Java version

HERoS and the libraries on which it is dependent, use a Java version 17 or higher. The library is typically used with an openjdk version 17.
