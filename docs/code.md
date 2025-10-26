# medlabs-heros

## medlabs-heros code

The build-up of the libraries is as follows:

<table>
  <tr><td>HERoS library for Covid-19 spread in cities (https://www.heros-project.eu/)</td></tr>
  <tr><td>MEDLABS disease transmission library (https://github.com/averbraeck/medlabs)</td></tr>
  <tr><td>DSOL simulation library (https://github.com/averbraeck/dsol / https://simulation.tudelft.nl)</td></tr>
  <tr><td>djutils (https://github.com/averbraeck/djutils / https://djutils.org)</td></tr>
  <tr><td>djunits (https://github.com/averbraeck/djunits / https://djunits.org)</td></tr>
  <tr><td>djutils-base (https://github.com/averbraeck/djutils-base / https://djutils.org)</td></tr>
</table>

Using Maven, all libraries are resolved automatically for the HERoS project library. There is an 'executable jar' in the folder `jar` that contains the heros library plus all libraries on which it is dependent, as well as a set of input files for The Hague to run the model on 490,000 citizens.
