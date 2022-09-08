package eu.heros.disease;

import nl.tudelft.simulation.jstats.distributions.DistTriangular;
import nl.tudelft.simulation.medlabs.disease.DiseasePhase;
import nl.tudelft.simulation.medlabs.disease.DiseaseProgression;
import nl.tudelft.simulation.medlabs.disease.DiseaseState;
import nl.tudelft.simulation.medlabs.disease.DurationDistribution;
import nl.tudelft.simulation.medlabs.model.MedlabsModelInterface;
import nl.tudelft.simulation.medlabs.person.Person;
import nl.tudelft.simulation.medlabs.simulation.TimeUnit;

/**
 * The Covid19Progression model implements a state machine for disease progression.
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
public class Covid19Progression extends DiseaseProgression
{
    /** */
    private static final long serialVersionUID = 1L;

    /** "S": Susceptible disease phase. This is the normal 'base' phase of every person. */
    public static DiseasePhase susceptible;

    /** "E": Exposed disease phase. Note: the exposure date is stored with the Person. Exposed means the person WILL get ill. */
    public static DiseasePhase exposed;

    /** "I(A)": Infected-Asymptomatic disease phase. */
    public static DiseasePhase infected_asymptomatic;

    /** "I(S)": Infected-Symptomatic disease phase. */
    public static DiseasePhase infected_symptomatic;

    /** "I(H)": Hospitalized disease phase. */
    public static DiseasePhase hospitalized;

    /** "I(I)": ICU disease phase. */
    public static DiseasePhase icu;

    /** "R": Recovered disease phase. */
    public static DiseasePhase recovered;

    /** "D": Dead disease phase. */
    public static DiseasePhase dead;

    /** E -> I(A) and E -> I(S) period: duration of the incubation period. */
    private DurationDistribution distIncubationPeriod;

    /**
     * E -> I(A) probability. E -> I(S) probability = 1 - (E -> I(A) probability). According to Nishiura et al. (2020), Ferguson
     * et al. (2020) % of asymptomatic population varies from 40 to 50.
     */
    private final double probabilityAsymptomatic;

    /** I(A) -> R period. The probability is assumed to be 1 (all asymptomatic persons recover). */
    private DurationDistribution distAsymptomaticToRecovery;

    /** I(S) -> R period. */
    private DurationDistribution distSymptomaticToRecovery;

    /** I(S) -> I(H) period. The probability is calculated by the getProbHospitalization(age) function. */
    private double periodSymptomaticToHospitalized = 216; // 9 * 24

    /** I(H) -> I(I) period. The probability is calculated by the getProbICU(age) function. */
    private double periodHospitalizedToICU = 72; // 3 * 24

    /** I(H) -> R period. The probability is 1 - (I(H) -> D probability). */
    private double periodHospitalizedToRecovery = 312; // 13 * 24

    /** I(H) -> D period. The probability is calculated by the getProbDeath(age) function. */
    private double periodHospitalizedToDeath = 72; // 3 * 24

    /** I(I) -> D period. The probability is calculated by the getProbDeath(age) function. */
    private double periodICUToDeath = 96; // 4 * 24

    /** I(I) -> R period. The probability is 1 - (I(I) -> D probability). */
    private double periodICUToRecovery = 720; // 30 * 24

    /**
     * Create the Covid19 Progression model. A state machine is instantiated with probabilities for the state transitions and
     * durations between states.
     * 
     * <pre>
     * The following parameters need to be specified for the Covid19 state machine:
     * 
     * The transition S -> E is determined by the Transmission model
     * Probability and Duration distribution E -> I(A)     we call this the incubation period 
     * Probability and Duration distribution E -> I(S)     we call this the incubation period
     *                                                     in this period the person is not ill and not contagious
     * Duration distribution I(A) -> R                     we assume I(A) always leads to R
     * Probability and Duration distribution I(S) -> R     a certain percentage recovers without going to the hospital
     * Probability and Duration distribution I(S) -> I(H)  a certain percentage of people gets hospitalized
     * Probability and Duration distribution I(H) -> R     a certain percentage of hospitalized people recover
     * Probability and Duration distribution I(H) -> I(I)  a certain percentage of hospitalized people go to the ICU
     * Probability and Duration distribution I(H) -> D     a certain percentage of hospitalized people die
     * Probability and Duration distribution I(I) -> R     a certain percentage of people in the ICU recover
     * Probability and Duration distribution I(I) -> D     a certain percentage of people in the ICU die
     * </pre>
     * 
     * @param model MedlabsModelInterface; the Medlabs model
     */
    public Covid19Progression(final MedlabsModelInterface model)
    {
        super(model, "Covid19");

        susceptible = addDiseasePhase("Susceptible", DiseaseState.SUSCEPTIBLE);
        exposed = addDiseasePhase("Exposed", DiseaseState.ILL);
        infected_asymptomatic = addDiseasePhase("Infected-Asymptomatic", DiseaseState.ILL);
        infected_symptomatic = addDiseasePhase("Infected-Symptomatic", DiseaseState.ILL);
        hospitalized = addDiseasePhase("Hospitalized", DiseaseState.ILL);
        icu = addDiseasePhase("ICU", DiseaseState.ILL);
        dead = addDiseasePhase("Dead", DiseaseState.DEAD);
        recovered = addDiseasePhase("Recovered", DiseaseState.RECOVERED);

        // -------------------------------------------------------------
        // Key parameters/uncertainties
        // -------------------------------------------------------------

        // around 40-50% for the early variants
        this.probabilityAsymptomatic = model.getParameterValueDouble("covidP.FractionAsymptomatic");

        // 2-8 days
        this.distIncubationPeriod = new DurationDistribution(new DistTriangular(this.model.getRandomStream(),
                24.0 * model.getParameterValueDouble("covidP.IncubationPeriod_min"),
                24.0 * model.getParameterValueDouble("covidP.IncubationPeriod_mode"),
                24.0 * model.getParameterValueDouble("covidP.IncubationPeriod_max")), TimeUnit.HOUR);

        // According Linton et al. (2020) = truncated lognormal distribution with parameters
        // meanlog = log(120), sdlog = log(24), min = 48, max = 336
        // See epi-params-modeling.R for details
        // For now, just a triangular with a=min, mode=mean, b=mean + min

        // FIXME: Change triangular to truncated log normal

        // All in days -- around 14-21 days
        this.distAsymptomaticToRecovery = new DurationDistribution(new DistTriangular(this.model.getRandomStream(),
                24.0 * model.getParameterValueDouble("covidP.AsymptomaticToRecover_min"),
                24.0 * model.getParameterValueDouble("covidP.AsymptomaticToRecover_mode"),
                24.0 * model.getParameterValueDouble("covidP.AsymptomaticToRecover_max")), TimeUnit.HOUR);

        // All in days -- around 14-21 days
        this.distSymptomaticToRecovery = new DurationDistribution(new DistTriangular(this.model.getRandomStream(),
                24.0 * model.getParameterValueDouble("covidP.SymptomaticToRecover_min"),
                24.0 * model.getParameterValueDouble("covidP.SymptomaticToRecover_mode"),
                24.0 * model.getParameterValueDouble("covidP.SymptomaticToRecover_max")), TimeUnit.HOUR);

    }

    // TODO: Add the reference
    /**
     * Get hospitalization probability.
     * @param age
     * @return probHospitalization
     */
    private double getProbHospitalization(final int age)
    {
        switch (age / 10)
        {
            case 0:
                return 0.00;
            case 1:
                return 0.08;
            case 2:
                return 0.04;
            case 3:
                return 0.09;
            case 4:
                return 0.18;
            case 5:
                return 0.25;
            case 6:
                return 0.46;
            case 7:
                return 0.55;
            case 8:
            case 9:
            case 10:
                return 0.23;
            default:
                return 0.0;
        }
    }

    /**
     * Get ICU probability.
     * @param age
     * @return probICU
     */
    private double getProbICU(final int age)
    {
        switch (age / 10)
        {
            case 0:
                return 0.00;
            case 1:
                return 0.02;
            case 2:
                return 0.01;
            case 3:
                return 0.02;
            case 4:
                return 0.05;
            case 5:
                return 0.08;
            case 6:
                return 0.18;
            case 7:
                return 0.15;
            case 8:
            case 9:
            case 10:
                return 0.01;
            default:
                return 0.0;
        }
    }

    /**
     * Get death probability
     * @param age
     * @return probDeath
     */
    private double getProbDeath(final int age)
    {
        switch (age / 10)
        {
            case 0:
                return 0.00002;
            case 1:
                return 0.00006;
            case 2:
                return 0.0003;
            case 3:
                return 0.0008;
            case 4:
                return 0.0015;
            case 5:
                return 0.02;
            case 6:
                return 0.08;
            case 7:
                return 0.24;
            case 8:
            case 9:
            case 10:
                return 0.31;
            default:
                return 0.0;
        }
    }

    // -------------------------------------------------------------
    // Progression model
    // -------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void changeDiseasePhase(final Person person, final DiseasePhase nextPhase)
    {
        MedlabsModelInterface model = person.getModel();

        // -------------------------------------------------------------
        // Exposed
        // -------------------------------------------------------------

        if (nextPhase == exposed)
        {
            person.getDiseasePhase().removePerson();
            person.setDiseasePhase(exposed);
            exposed.addPerson();
            this.model.getPersonMonitor().reportInfectPerson(person);
            this.model.getPersonMonitor().reportInfectionAtLocationType(person.getCurrentLocation().getLocationTypeId());
            double incubationPeriod = this.distIncubationPeriod.getDuration();

            // Split into asymptomatic and symptomatic
            if (model.getU01().draw() < this.probabilityAsymptomatic)
            {
                model.getSimulator().scheduleEventRel(incubationPeriod, this, person, "changePhase",
                        new Object[] {Covid19Progression.infected_asymptomatic});
                return;
            }
            else
            {
                model.getSimulator().scheduleEventRel(incubationPeriod, this, person, "changePhase",
                        new Object[] {Covid19Progression.infected_symptomatic});
                return;
            }
        }

        // -------------------------------------------------------------
        // Infected asymptomatic contagious
        // -------------------------------------------------------------

        else if (nextPhase == infected_asymptomatic)
        {
            person.getDiseasePhase().removePerson();
            person.setDiseasePhase(infected_asymptomatic);
            infected_asymptomatic.addPerson();

            model.getSimulator().scheduleEventRel(this.distAsymptomaticToRecovery.getDuration(), TimeUnit.HOUR, this, person,
                    "changePhase", new Object[] {recovered});
            return;
        }

        // -------------------------------------------------------------
        // Infected symptomatic contagious
        // -------------------------------------------------------------

        else if (nextPhase == infected_symptomatic)
        {
            person.getDiseasePhase().removePerson();
            person.setDiseasePhase(infected_symptomatic);
            infected_symptomatic.addPerson();

            if (this.model.getU01().draw() < getProbHospitalization(person.getAge()))
                model.getSimulator().scheduleEventRel(this.periodSymptomaticToHospitalized, TimeUnit.HOUR, this, person,
                        "changePhase", new Object[] {hospitalized});
            else
                model.getSimulator().scheduleEventRel(this.distSymptomaticToRecovery.getDuration(), TimeUnit.HOUR, this, person,
                        "changePhase", new Object[] {recovered});
            return;
        }

        // -------------------------------------------------------------
        // Infected hospitalized
        // -------------------------------------------------------------

        else if (nextPhase == hospitalized)
        {
            person.getDiseasePhase().removePerson();
            person.setDiseasePhase(hospitalized);
            hospitalized.addPerson();

            double probICU = getProbICU(person.getAge());

            if (this.model.getU01().draw() < probICU)
            {
                // Person goes to ICU
                model.getSimulator().scheduleEventRel(this.periodHospitalizedToICU, TimeUnit.HOUR, this, person, "changePhase",
                        new Object[] {icu});
                return;
            }

            // If person doesn't go to ICU, then they either recover or die
            else
            {
                double probDeath = getProbDeath(person.getAge());

                if (this.model.getU01().draw() < probDeath)
                {
                    model.getSimulator().scheduleEventRel(this.periodHospitalizedToDeath, TimeUnit.HOUR, this, person,
                            "changePhase", new Object[] {dead});
                    return;
                }

                else
                {
                    model.getSimulator().scheduleEventRel(this.periodHospitalizedToRecovery, TimeUnit.HOUR, this, person,
                            "changePhase", new Object[] {recovered});
                    return;
                }
            }
        }

        // -------------------------------------------------------------
        // Infected ICU
        // -------------------------------------------------------------

        else if (nextPhase == icu)
        {
            person.getDiseasePhase().removePerson();
            person.setDiseasePhase(icu);
            icu.addPerson();

            double probDeath = getProbDeath(person.getAge());

            // Recover or die at ICU
            if (this.model.getU01().draw() < probDeath)
            {
                model.getSimulator().scheduleEventRel(this.periodICUToDeath, TimeUnit.HOUR, this, person, "changePhase",
                        new Object[] {dead});
                return;
            }

            else
            {
                model.getSimulator().scheduleEventRel(this.periodICUToRecovery, TimeUnit.HOUR, this, person, "changePhase",
                        new Object[] {recovered});
                return;
            }
        }

        // -------------------------------------------------------------
        // Recovered
        // -------------------------------------------------------------

        else if (nextPhase == recovered)
        {
            person.getDiseasePhase().removePerson();
            person.setDiseasePhase(recovered);
            recovered.addPerson();
            return;
        }

        // -------------------------------------------------------------
        // Dead
        // -------------------------------------------------------------

        else if (nextPhase == dead)
        {
            this.model.getPersonMonitor().reportDeathPerson(person);

            person.getDiseasePhase().removePerson();
            person.setDiseasePhase(dead);
            dead.addPerson();
            return;
        }

        // -------------------------------------------------------------
        // ERROR
        // -------------------------------------------------------------

        else
        {
            System.err.println("ERROR: Person " + person + " has unknown next disease phase " + nextPhase);
        }
    }

}
