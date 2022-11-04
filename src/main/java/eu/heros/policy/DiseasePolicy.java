package eu.heros.policy;

import eu.heros.model.HerosModel;

/**
 * DiseasePolicy implements a parameter change in the Disease Transmission model.
 * <p>
 * Copyright (c) 2022-2022 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 * @author <a href="https://www.tudelft.nl/tbm/resiliencelab/people/mikhail-sirenko">Mikhail Sirenko</a>
 */
public class DiseasePolicy
{
    private final HerosModel model;

    public DiseasePolicy(final HerosModel model, final double time, final String parameterName, final double value)
    {
        this.model = model;
        if (!parameterName.equals("covidT_dist.psi") && !parameterName.equals("covidT_dist.mu"))
        {
            System.err.println("Unrecognized variable name " + parameterName);
            return;
        }
        model.getSimulator().scheduleEventAbs(time, this, this, "changeParameter", new Object[] {parameterName, value});
    }

    protected void changeParameter(final String parameterName, final double value)
    {
        System.out.println(String.format("DiseasePolicy changed parameter %s to %f", parameterName, value));
        if (parameterName.equals("covidT_dist.psi"))
        {
            this.model.getDiseaseTransmission().setParameter("psi", value);
        }
        else if (parameterName.equals("covidT_dist.mu"))
        {
            this.model.getDiseaseTransmission().setParameter("mu", value);
        }
        else
            System.err.println("Unrecognized variable name " + parameterName);
    }
}
