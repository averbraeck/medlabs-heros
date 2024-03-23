package eu.heros.disease;

import nl.tudelft.simulation.medlabs.common.MedlabsException;
import nl.tudelft.simulation.medlabs.disease.DiseasePhase;
import nl.tudelft.simulation.medlabs.disease.DiseaseProgression;
import nl.tudelft.simulation.medlabs.disease.DiseaseState;
import nl.tudelft.simulation.medlabs.disease.DurationDistribution;
import nl.tudelft.simulation.medlabs.model.MedlabsModelInterface;
import nl.tudelft.simulation.medlabs.parser.ConditionalProbability;
import nl.tudelft.simulation.medlabs.parser.DistributionParser;
import nl.tudelft.simulation.medlabs.person.Person;
import nl.tudelft.simulation.medlabs.simulation.TimeUnit;

/**
 * The Covid19Progression model implements a state machine for disease progression.
 * <p>
 * Copyright (c) 2020-2024 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights reserved. The
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

    /* ---------------------------- parameters for the progression model ---------------------------- */

    /** E -> I(A) probability. E -> I(S) probability = 1 - (E -> I(A) probability). */
    private final ConditionalProbability fractionAsymptomatic;

    /** E -> I(A) period: duration of the incubation period for asymptomatic cases. */
    private final DurationDistribution periodIncubationAsymptomatic;

    /** E -> I(S) period: duration of the incubation period for symptomatic cases. */
    private final DurationDistribution periodIncubationSymptomatic;

    /** I(A) -> R period. The probability is assumed to be 1 (all asymptomatic persons recover). */
    private final DurationDistribution periodAsymptomaticToRecovered;

    /** I(S) -> R period. */
    private final DurationDistribution periodSymptomaticToRecovered;

    /** I(C) -> I(H) fraction. */
    private final ConditionalProbability fractionSymptomaticToHospitalized;

    /** I(S) -> I(H) period. The probability is calculated by the getProbHospitalization(age) function. */
    private final DurationDistribution periodSymptomaticToHospitalized;

    /** I(H) -> I(I) fraction. */
    private final ConditionalProbability fractionHospitalizedToICU;

    /** I(H) -> D fraction. */
    private final ConditionalProbability fractionHospitalizedToDead;

    /** I(H) -> I(I) period. The probability is calculated by the getProbICU(age) function. */
    private final DurationDistribution periodHospitalizedToICU;

    /** I(H) -> R period. The probability is 1 - (I(H) -> D probability). */
    private final DurationDistribution periodHospitalizedToRecovered;

    /** I(H) -> D period. The probability is calculated by the getProbDeath(age) function. */
    private final DurationDistribution periodHospitalizedToDead;

    /** I(I) -> D fraction. */
    private final ConditionalProbability fractionICUToDead;

    /** I(I) -> D period. The probability is calculated by the getProbDeath(age) function. */
    private final DurationDistribution periodICUToDead;

    /** I(I) -> R period. The probability is 1 - (I(I) -> D probability). */
    private final DurationDistribution periodICUToRecovered;

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
     * @throws MedlabsException when the parsing of the parameters has a problem
     */
    public Covid19Progression(final MedlabsModelInterface model) throws MedlabsException
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
        // Progression model parameters/uncertainties
        // -------------------------------------------------------------

        this.fractionAsymptomatic = new ConditionalProbability(model.getParameterValue("covidP.FractionAsymptomatic"));
        this.periodIncubationAsymptomatic = new DurationDistribution(DistributionParser.parseDistContinuous(
                model.getParameterValue("covidP.IncubationPeriodAsymptomatic"), model.getDefaultStream()), TimeUnit.DAY);
        this.periodIncubationSymptomatic = new DurationDistribution(DistributionParser.parseDistContinuous(
                model.getParameterValue("covidP.IncubationPeriodSymptomatic"), model.getDefaultStream()), TimeUnit.DAY);
        this.periodAsymptomaticToRecovered = new DurationDistribution(DistributionParser.parseDistContinuous(
                model.getParameterValue("covidP.PeriodAsymptomaticToRecovered"), model.getDefaultStream()), TimeUnit.DAY);
        this.fractionSymptomaticToHospitalized =
                new ConditionalProbability(model.getParameterValue("covidP.FractionSymptomaticToHospitalized"));
        this.periodSymptomaticToHospitalized =
                new DurationDistribution(
                        DistributionParser.parseDistContinuous(
                                model.getParameterValue("covidP.PeriodSymptomaticToHospitalized"), model.getDefaultStream()),
                        TimeUnit.DAY);
        this.periodSymptomaticToRecovered = new DurationDistribution(DistributionParser.parseDistContinuous(
                model.getParameterValue("covidP.PeriodSymptomaticToRecovered"), model.getDefaultStream()), TimeUnit.DAY);
        this.fractionHospitalizedToICU =
                new ConditionalProbability(model.getParameterValue("covidP.FractionHospitalizedToICU"));
        this.fractionHospitalizedToDead =
                new ConditionalProbability(model.getParameterValue("covidP.FractionHospitalizedToDead"));
        this.periodHospitalizedToICU = new DurationDistribution(DistributionParser.parseDistContinuous(
                model.getParameterValue("covidP.PeriodHospitalizedToICU"), model.getDefaultStream()), TimeUnit.DAY);
        this.periodHospitalizedToDead = new DurationDistribution(DistributionParser.parseDistContinuous(
                model.getParameterValue("covidP.PeriodHospitalizedToDead"), model.getDefaultStream()), TimeUnit.DAY);
        this.periodHospitalizedToRecovered = new DurationDistribution(DistributionParser.parseDistContinuous(
                model.getParameterValue("covidP.PeriodHospitalizedToRecovered"), model.getDefaultStream()), TimeUnit.DAY);
        this.fractionICUToDead = new ConditionalProbability(model.getParameterValue("covidP.FractionICUToDead"));
        this.periodICUToDead = new DurationDistribution(DistributionParser.parseDistContinuous(
                model.getParameterValue("covidP.PeriodICUToDead"), model.getDefaultStream()), TimeUnit.DAY);
        this.periodICUToRecovered = new DurationDistribution(DistributionParser.parseDistContinuous(
                model.getParameterValue("covidP.PeriodICUToRecovered"), model.getDefaultStream()), TimeUnit.DAY);
    }

    // -------------------------------------------------------------
    // Progression model
    // -------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public boolean expose(final Person exposedPerson, final DiseasePhase exposurePhase)
    {
        exposedPerson.setDiseasePhase(exposed);
        exposed.addPerson();

        // Split into asymptomatic and symptomatic
        if (this.model.getU01().draw() < this.fractionAsymptomatic.probability(exposedPerson))
        {
            double incubationPeriod = this.periodIncubationAsymptomatic.getDuration();
            this.model.getSimulator().scheduleEventRel(incubationPeriod, this, "changeDiseasePhase",
                    new Object[] {exposedPerson, Covid19Progression.infected_asymptomatic});
            return true;
        }
        else
        {
            double incubationPeriod = this.periodIncubationSymptomatic.getDuration();
            this.model.getSimulator().scheduleEventRel(incubationPeriod, this, "changeDiseasePhase",
                    new Object[] {exposedPerson, Covid19Progression.infected_symptomatic});
            return true;
        }
    }

    /**
     * Update the disease phase to the provided next phase for the person.
     * @param person Person; the person to update the disease phase for
     * @param nextPhase DiseasePhase; the new disease phase for the person
     */
    protected void changeDiseasePhase(final Person person, final DiseasePhase nextPhase)
    {
        MedlabsModelInterface model = person.getModel();
        person.getDiseasePhase().removePerson();

        // -------------------------------------------------------------
        // Exposed
        // -------------------------------------------------------------

        if (nextPhase == exposed)
        {
            System.err.println("Should have been handled with expose(...) method");
            expose(person, exposed);
        }

        // -------------------------------------------------------------
        // Infected asymptomatic contagious
        // -------------------------------------------------------------

        else if (nextPhase == infected_asymptomatic)
        {
            person.setDiseasePhase(infected_asymptomatic);
            infected_asymptomatic.addPerson();

            model.getSimulator().scheduleEventRel(this.periodAsymptomaticToRecovered.getDuration(), TimeUnit.HOUR, this,
                    "changeDiseasePhase", new Object[] {person, recovered});
            return;
        }

        // -------------------------------------------------------------
        // Infected symptomatic contagious
        // -------------------------------------------------------------

        else if (nextPhase == infected_symptomatic)
        {
            person.setDiseasePhase(infected_symptomatic);
            infected_symptomatic.addPerson();

            if (this.model.getU01().draw() < this.fractionSymptomaticToHospitalized.probability(person))
                model.getSimulator().scheduleEventRel(this.periodSymptomaticToHospitalized.getDuration(), TimeUnit.HOUR, this,
                        "changeDiseasePhase", new Object[] {person, hospitalized});
            else
                model.getSimulator().scheduleEventRel(this.periodSymptomaticToRecovered.getDuration(), TimeUnit.HOUR, this,
                        "changeDiseasePhase", new Object[] {person, recovered});
            return;
        }

        // -------------------------------------------------------------
        // Infected hospitalized
        // -------------------------------------------------------------

        else if (nextPhase == hospitalized)
        {
            person.setDiseasePhase(hospitalized);
            hospitalized.addPerson();

            if (this.model.getU01().draw() < this.fractionHospitalizedToICU.probability(person))
            {
                // Person goes to ICU
                model.getSimulator().scheduleEventRel(this.periodHospitalizedToICU.getDuration(), TimeUnit.HOUR, this,
                        "changeDiseasePhase", new Object[] {person, icu});
                return;
            }

            // If person doesn't go to ICU, then they either recover or die
            else
            {
                if (this.model.getU01().draw() < this.fractionHospitalizedToDead.probability(person))
                {
                    model.getSimulator().scheduleEventRel(this.periodHospitalizedToDead.getDuration(), TimeUnit.HOUR, this,
                            "changeDiseasePhase", new Object[] {person, dead});
                    return;
                }
                else
                {
                    model.getSimulator().scheduleEventRel(this.periodHospitalizedToRecovered.getDuration(), TimeUnit.HOUR, this,
                            "changeDiseasePhase", new Object[] {person, recovered});
                    return;
                }
            }
        }

        // -------------------------------------------------------------
        // Infected ICU
        // -------------------------------------------------------------

        else if (nextPhase == icu)
        {
            person.setDiseasePhase(icu);
            icu.addPerson();

            // Recover or die at ICU
            if (this.model.getU01().draw() < this.fractionICUToDead.probability(person))
            {
                model.getSimulator().scheduleEventRel(this.periodICUToDead.getDuration(), TimeUnit.HOUR, this,
                        "changeDiseasePhase", new Object[] {person, dead});
                return;
            }

            else
            {
                model.getSimulator().scheduleEventRel(this.periodICUToRecovered.getDuration(), TimeUnit.HOUR, this,
                        "changeDiseasePhase", new Object[] {person, recovered});
                return;
            }
        }

        // -------------------------------------------------------------
        // Recovered
        // -------------------------------------------------------------

        else if (nextPhase == recovered)
        {
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
