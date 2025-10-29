# medlabs-heros

### Agent-Based Model of Covid-19 spread in European cities for the [HERoS project](https://www.heros-project.eu/)

HERoS is a Horizon 2020 project funded under the 'SC1-PHE-CORONAVIRUS-2020 – Advancing knowledge for the clinical and public health response to the 2019-nCoV epidemic' call. As a social sciences research project, HERoS addresses the need to understand the social dynamics of the outbreak and the related public health response. The work was conducted with several partners between April 2020 and 2023.

The overall objective of HERoS was to improve the effectiveness and efficiency of the response to the Covid-19 outbreak by generating actionable knowledge four areas: governance, models in epidemics, medical supply chains, and online misinformation. This model contributes to the 'models in epidemics' part and focuses on epidemiological and behavioural modelling. See the [literature](docs/6-literature.md) section for output related to the agent-based models.


This documentation explains how to run a medlabs-heros model, what configuration files and input files are used, and what output is created during the run.

1. [Installing Java and running the code](docs/1-install.md)
2. [Configuration files](docs/2-configure.md)
3. [Input files](docs/3-input.md)<br>
   3.1. [Disease properties file](docs/3-1-input-disease.md).<br>
   3.2. [Activity schedules](docs/3-2-input-activities.md).<br>
   3.3. [Probabilistic infection rates](docs/3-3-input-infection-rates.md).<br>
   3.4. [People](docs/3-4-input-people.md).<br>
   3.5. [Location types](docs/3-5-input-location-types.md).<br>
   3.6. [Locations](docs/3-6-input-locations.md).<br>
   3.7. [Policies](docs/3-7-input-policies.md).<br>
   3.8. [OSM configuration file](docs/3-8-input-osm-config.md).<br>
   3.9. [OSM map file](docs/3-9-input-osm-map.md).
4. [Output files](docs/4-output.md)
5. [Exploring and adapting the code](docs/5-code.md)
6. [Literature](docs/6-literature.md)

There is also a [separate section](docs/9-maladaptation.md) for replicating the experiment results of Sirenko, M., Verbraeck, A., & Comes, T. (2025). _On Urban Maladaptation in Times of Epidemics_ which is currently under review.


