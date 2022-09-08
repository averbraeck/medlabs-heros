package eu.heros.person;

import nl.tudelft.simulation.medlabs.model.MedlabsModelInterface;
import nl.tudelft.simulation.medlabs.person.index.IdxPerson;

/**
 * Infant.java.
 * <p>
 * Copyright (c) 2020-2020 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights reserved. The
 * code is part of the HERoS project (Health Emergency Response in Interconnected Systems), which builds on the MEDLABS project.
 * The simulation tools are aimed at providing policy analysis tools to predict and help contain the spread of epidemics. They
 * make use of the DSOL simulation engine and the agent-based modeling formalism. This software is licensed under the BSD
 * license. See license.txt in the main project.
 * </p>
 * @author <a href="https://www.linkedin.com/in/mikhailsirenko">Mikhail Sirenko</a>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class Infant extends IdxPerson
{
    /** */
    private static final long serialVersionUID = 20211001L;

    /**
     * Create a Person with a number of basic properties, including the scool type and school that the student attends. The
     * init() method has to be called after the student has been created to make sure the disease state machine is started for
     * the person if needed. The week pattern starts at day 0 and activity index 0.
     * @param model MedlabsModelInterface; the model
     * @param id int; unique id number of the person in the Model.getPersons() array
     * @param genderFemale boolean; whether gender is female or not.
     * @param age byte; the age of the person
     * @param homeLocationId int; the location of the home
     * @param weekPatternIndex short; the index of the standard week pattern for the person; this is also the initial week
     *            pattern that the person will use
     */
    public Infant(final MedlabsModelInterface model, final int id, final boolean genderFemale, final byte age,
            final int homeLocationId, final short weekPatternIndex)
    {
        super(model, id, genderFemale, age, homeLocationId, weekPatternIndex);
    }

}
