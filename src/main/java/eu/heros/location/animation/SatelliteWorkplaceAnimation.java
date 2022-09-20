package eu.heros.location.animation;

import java.awt.Color;

import nl.tudelft.simulation.medlabs.location.Location;
import nl.tudelft.simulation.medlabs.location.animation.LocationAnimationSquare;
import nl.tudelft.simulation.medlabs.location.animation.LocationAnimationTemplate;

/**
 * SatelliteWorkplaceAnimation.java.
 * <p>
 * Copyright (c) 2022-2022 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 * @author <a href="https://www.tudelft.nl/tbm/resiliencelab/people/mikhail-sirenko">Mikhail Sirenko</a>
 */
public class SatelliteWorkplaceAnimation extends LocationAnimationSquare
{
    /** */
    private static final long serialVersionUID = 20200919L;

    /** default template. */
    public static final LocationAnimationTemplate SATELLITE_WORKPLACE_TEMPLATE =
            new LocationAnimationTemplate("SatelliteHouse").setLineColor(Color.BLACK).setFillColor(Color.RED).setCharacter("W")
                    .setHalfShortSize(120).setNumberColor(Color.BLACK).setNumberSize(120);

    /**
     * Create the animation for this location object.
     * @param location the location belonging to this animation
     */
    public SatelliteWorkplaceAnimation(final Location location)
    {
        super(location, SATELLITE_WORKPLACE_TEMPLATE);
    }

}
