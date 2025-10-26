# medlabs-heros

## 3. Input files

Several input files are used to configure a medlabs-heros experiment. Each of the input files is described in a separate document.

- 3.1. [Disease properties file](3-1-input-disease.md). In the example configuration: `alpha-distance.properties`.
- 3.2. [Activity schedules](3-2-input-activities.md). In the example configuration: `activities/activityschedules.xlsx`.
- 3.3. [Probabilistic infection rates](3-3-input-infection-rates.md). In the example configuration: `epidemiology/infection_rates.csv`.
- 3.4. [People](3-4-input-people.md). In the example configuration: `people/people.csv.gz`.
- 3.5. [Location types](3-5-input-location-types.md). In the example configuration: `locations/locationtypes.csv`.
- 3.6. [Locations](3-6-input-locations.md). In the example configuration: `locations/locations.csv.gz`.
- 3.7. [Policies](3-7-input-policies.md). Not turned on in the example configuration. Several examples available under folder `policcies`.
- 3.8. [OSM configuration file](3-8-input-osm-config.md). In the example configuration: `locations/thehague.osm.csv`.
- 3.9. [OSM map file](3-9-input-osm-map.md). In the example configuration: `locations/thehague.osm.pbf`.

Neither the folder locations, nor the folder names or file names have any limitations for naming. Do, however, stick to the file formats (.csv, .csv.gz, .pbf, .xlsx) since the program counts on receiving files of a certain type. If possible, use the proper extensions for the files, since it can be very confusing if the externsions do not match the file content.
