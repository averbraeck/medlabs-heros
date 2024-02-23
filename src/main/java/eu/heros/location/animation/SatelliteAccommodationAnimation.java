package eu.heros.location.animation;

import java.awt.Color;

import nl.tudelft.simulation.medlabs.location.Location;
import nl.tudelft.simulation.medlabs.location.animation.LocationAnimationSquare;
import nl.tudelft.simulation.medlabs.location.animation.LocationAnimationTemplate;

/**
 * SatelliteAccommodationAnimation.java.
 * <p>
 * Copyright (c) 2022-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 * @author <a href="https://www.tudelft.nl/tbm/resiliencelab/people/mikhail-sirenko">Mikhail Sirenko</a>
 */
public class SatelliteAccommodationAnimation extends LocationAnimationSquare
{
    /** */
    private static final long serialVersionUID = 20200919L;

    /** default template. */
    public static final LocationAnimationTemplate SATELLITE_HOUSE_TEMPLATE =
            new LocationAnimationTemplate("SatelliteHouse").setLineColor(Color.DARK_GRAY).setFillColor(Color.BLUE).setCharacter("S")
                    .setHalfShortSize(120).setNumberColor(Color.BLACK).setNumberSize(120);

    /**
     * Create the animation for this location object.
     * @param location the location belonging to this animation
     */
    public SatelliteAccommodationAnimation(final Location location)
    {
        super(location, SATELLITE_HOUSE_TEMPLATE);
    }
}
