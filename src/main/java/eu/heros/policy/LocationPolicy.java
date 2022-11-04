package eu.heros.policy;

import eu.heros.model.HerosModel;
import nl.tudelft.simulation.medlabs.location.LocationType;

/**
 * LocationPolicy.java.
 * <p>
 * Copyright (c) 2022-2022 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 * @author <a href="https://www.tudelft.nl/tbm/resiliencelab/people/mikhail-sirenko">Mikhail Sirenko</a>
 */
public class LocationPolicy
{
    private final HerosModel model;

    public LocationPolicy(final HerosModel model, final double time, final String locationTypeName, final double fractionOpen,
            final double fractionActivities, final String alternativeLocationName, final String reportAsLocationName)
    {
        this.model = model;
        LocationType locationType = this.model.getLocationTypeNameMap().get(locationTypeName);
        if (locationType == null)
        {
            System.err.println("LocationPolicy: Did not recognize locationType " + locationTypeName);
            return;
        }
        LocationType alternativeLocationType = this.model.getLocationTypeNameMap().get(alternativeLocationName);
        if (alternativeLocationType == null)
        {
            System.err.println("LocationPolicy: Did not recognize alternativeLocationType " + alternativeLocationName);
            return;
        }
        model.getSimulator().scheduleEventAbs(time, this, this, "openCloseLocationType",
                new Object[] {locationType, fractionOpen, fractionActivities, alternativeLocationType, reportAsLocationName});
    }

    protected void openCloseLocationType(final LocationType locationType, final double fractionOpen,
            final double fractionActivities, final LocationType alternativeLocationType, final String reportAsLocationName)
    {
        locationType.setClosurePolicy(fractionOpen, fractionActivities, alternativeLocationType, reportAsLocationName);
    }
}
