# medlabs-heros

### Agent-Based Model of Covid-19 spread in European cities for the [HERoS project](https://www.heros-project.eu/)

#### The build-up of the libraries is as follows:

<table>
  <tr><td>HERoS library for Covid-19 spread in cities (https://www.heros-project.eu/)</td></tr>
  <tr><td>medlabs disease transmission library (https://simulation.tudelft.nl/medlabs)</td></tr>
  <tr><td>dsol simulation library (https://simulation.tudelft.nl)</td></tr>
  <tr><td>djunits (https://djunits.org)</td><td>djutils (https://djutils.org)</td></tr>
</table>

Using Maven, all libraries are resolved automatically for the HERoS project library. There is also a 'fat jar' in the folder `jar` that contains the heros library plus all libraries on which it is dependent.


#### Java version

HERoS and the libraries on which it is dependent, use a Java version 17 or higher. The library is typically used with an openjdk version 17.
