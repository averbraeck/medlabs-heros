package eu.heros.disease;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.set.TIntSet;
import nl.tudelft.simulation.medlabs.disease.DiseaseTransmission;
import nl.tudelft.simulation.medlabs.disease.InfectionRecord;
import nl.tudelft.simulation.medlabs.location.Location;
import nl.tudelft.simulation.medlabs.location.LocationType;
import nl.tudelft.simulation.medlabs.model.MedlabsModelInterface;
import nl.tudelft.simulation.medlabs.person.Person;

public class Covid19TransmissionArea extends DiseaseTransmission
{
    /** */
    private static final long serialVersionUID = 1L;

    /**
     * Parameter p_B of the formula.<br>
     * According to Chu et al. (2020) infection probability varies from 0.09 to 0.38 if the distance is lower than 1 m. This
     * works for symptomatic.
     */
    private final double contagiousness;

    /**
     * Parameter beta of the formula.<br>
     * Beta is a correction factor for mask wearing and other personal protection; beta is in [0, 1].
     */
    private double beta;

    /**
     * Parameter t_e_min of the formula (converted to hours).<br>
     * t_e_min is the day after exposure when a person becomes contagious for the first time.
     */
    private final double t_e_min;

    /**
     * Parameter t_e_mode of the formula (converted to hours).<br>
     * t_e_mode is the day after exposure when a person is the most contagious.
     */
    private final double t_e_mode;

    /**
     * Parameter t_e_max of the formula (converted to hours).<br>
     * t_e_max is the day after exposure when a person is not contagious anymore.
     */
    private final double t_e_max;

    /**
     * Parameter to indicate when the duration is too short to make an infection calculation (converted to hours). Note that for
     * public transport and shops the interval should not be set too short since people get in and out frequently.
     */
    private final double calculationThreshold;

    /**
     * Create the Covid19 Transmission model.
     * @param model MedlabsModelInterface; the Medlabs model
     */
    public Covid19TransmissionArea(final MedlabsModelInterface model)
    {
        super(model, "Covid19");

        this.contagiousness = model.getParameterValueDouble("covidT_area.contagiousness");
        this.beta = model.getParameterValueDouble("covidT_area.beta");
        this.t_e_min = model.getParameterValueDouble("covidT_area.t_e_min") * 24.0;
        this.t_e_mode = model.getParameterValueDouble("covidT_area.t_e_mode") * 24.0;
        this.t_e_max = model.getParameterValueDouble("covidT_area.t_e_max") * 24.0;
        this.calculationThreshold = model.getParameterValueDouble("covidT_area.calculation_threshold") / 3600.0;
    }

    // -------------------------------------------------------------
    // Transmission model
    // -------------------------------------------------------------

    /**
     * Calculate the disease spread for all persons present in this (sub)location during the 'duration' in hours. The method
     * could return quickly when the delta-time is very short (e.g, less than a minute but be aware that spread in public
     * transport and shops might suffer since many people arrive and depart with short intervals).<br>
     * <br>
     * The formula to use to see if infectious persons j = 1..N_K infect another person i in location K is: <br>
     * 
     * <pre>
     * <code>                        
     *                                /  beta . p_B . p_j(t_e) . t_i,j  \
     *                   - SUM        | ------------------------------- |
     *  infected              j=1:N_K \          sigma_T . A_K          /
     * p         = 1 - e
     *  i
     * 
     * Since beta, p_B, t_i,j, sigma_T, and A_K are constant in one calculation, the formula simplifies to:
     * 
     *                      beta . p_B . t_i,j
     *                   - --------------------   SUM   p_j(t_e)
     *  infected               sigma_T . A_K    j=1:N_K
     * p         = 1 - e
     *  i
     * 
     * where:
     *   T        is the location type of the location
     *   K        is the indicator of the (sub)location
     *   N_K      is the number of infectious persons in (sub)location K which is of location type T
     *   sigma_T  is a correction factor for ventilation and social distancing for location type T 
     *            (making the location seem larger (or smaller) than it actually is); sigma_T is typically in (0, 1]
     *   beta     is a correction factor for mask wearing and other personal protection; beta is in [0, 1]
     *   p_B      is the base contagiousness of the variant (base transmission without ventilation, masks, etc.)
     *   p_j(t_e) is the infectiousness of person j for the number of days since the exposure date t_e of person j; 
     *            e.g., the infectiousness can be 0 for the first 3 days, then climb to 1 in 4 days, and then decrease to 0
     *            in for instance a week. A person would then be contagious between day 3 and 14, with a peak at day 7
     *            after exposure. p_j(t_e) is therefore in [0, 1]
     *   t_i,j    is the time that contagious person j and susceptible person i have spent together in location K (in hours)
     * 
     * Suppose that 1 infectious person and 1 susceptible person spend 1 hour together in a room of 10 m2, with the 
     * infectiousness of person j at its peak. When there are no corrective measures, the formula simplifies to:
     * 
     * p_i = 1 - e ^ (-p_B / 10)
     * 
     * Suppose the chance of getting infected is 0.05 (5%) in that case. Then e ^ - (p_B / 10) = 0.95, so 
     * p_B = -10 ln(0.95) = 0.51. When the room is 5 m^2, the probability becomes 9.6%. When the persons spend 2 hours
     * together,the probability also becomes 9.6%. When the persons wear masks, reducing transmission by 50% (beta = 0.5),
     * the transmission probability becomes 2.5%. When the room is extremely large (e.g., outside) the formula becomes 
     * e.g., for 1000 m^2: 1 - e ^ (-0.001) = 1E-3. This all feels correct. 
     * 
     * </code>
     * </pre>
     * 
     * @param location Location; the location for which to calculate infectious spread
     * @param subLocationIndex int; the sublocation for which to calculate possible infection(s)
     * @param duration double; the time for which the calculation needs to take place, in hours
     * @return InfectionRecord; record containing information whether a calculation took place and the last calculation time in
     *         DiseaseTransmission can be updated, and on the infected and infectious persons in case a calculation was made.
     */
    @Override
    public InfectionRecord infectPeople(final Location location, final TIntSet personsInSublocation, final double duration)
    {
        InfectionRecord infectionRecord = new InfectionRecord(Covid19Progression.exposed, location);

        // has contact been too short?
        if (duration < this.calculationThreshold)
            return infectionRecord;
        infectionRecord.setCalculated(true);

        // find the infectious persons in the sublocation
        LocationType lt = location.getLocationType();
        double area = location.getTotalSurfaceM2();

        TIntObjectMap<Person> personMap = this.model.getPersonMap();
        double now = this.model.getSimulator().getSimulatorTime().doubleValue();

        if (lt.isInfectInSublocation() || location.getNumberOfSubLocations() < 2)
        {
            // INFECTION TAKES PLACE JUST IN THE SUBLOCATION
            // NOTE: WHEN THE LOCATION HAS ONLY 1 SUBLOCATION THIS PART OF THE METHOD IS USED (MUCH FASTER)

            // calculate (beta . p_B . t_i,j) / (sigma_T . A_K)
            area /= location.getNumberOfSubLocations();
            double factor = -this.beta * this.contagiousness * duration / (lt.getCorrectionFactorArea() * area);
            if (factor == 0.0)
                return infectionRecord;

            // find the infectious persons in the sublocation
            double sumTij = 0.0;
            for (TIntIterator it = personsInSublocation.iterator(); it.hasNext();)
            {
                Person person = personMap.get(it.next());
                if (person.getDiseasePhase().isIll())
                {
                    double te = now - person.getExposureTime();
                    double contribution = 0.0;
                    if (te >= this.t_e_min && te < this.t_e_mode)
                        contribution += (te - this.t_e_min) / (this.t_e_mode - this.t_e_min);
                    else if (te >= this.t_e_mode && te <= this.t_e_max)
                        contribution += (this.t_e_max - te) / (this.t_e_max - this.t_e_mode);
                    // else the person is infected, but not yet or not anymore contagious
                    sumTij += contribution;
                    infectionRecord.addInfectiousPerson(person.getId());
                }
            }
            if (sumTij == 0.0)
                return infectionRecord;

            // calculate the probability for all persons present in the sublocation
            double pInfection = 1.0 - Math.exp(factor * sumTij);
            // check if we infect others
            for (TIntIterator it = personsInSublocation.iterator(); it.hasNext();)
            {
                Person person = personMap.get(it.next());
                if (person.getDiseasePhase().isSusceptible())
                {
                    // roll the dice
                    if (this.model.getU01().draw() < pInfection)
                    {
                        infectionRecord.addInfectedPerson(person.getId());
                    }
                }
            }
        }

        else

        {
            // INFECTION TAKES PLACE IN THE TOTAL LOCATION
            // TRY TO AVOID CALLING THIS -- IT IS EXPENSIVE

            // calculate (beta . p_B . t_i,j) / (sigma_T . A_K)
            double factor = this.beta * this.contagiousness * duration / (lt.getCorrectionFactorArea() * area);
            if (factor == 0.0)
                return infectionRecord;

            // find the infectious persons in the TOTAL location
            double sumTij = 0.0;
            for (TIntIterator it = location.getAllPersonIds().iterator(); it.hasNext();)
            {
                Person person = personMap.get(it.next());
                if (person.getDiseasePhase().isIll())
                {
                    double te = now - person.getExposureTime();
                    double contribution = 0.0;
                    if (te >= this.t_e_min && te < this.t_e_mode)
                        contribution += te / (this.t_e_mode - this.t_e_min);
                    else if (te >= this.t_e_mode && te <= this.t_e_max)
                        contribution += 1.0 - te / (this.t_e_max - this.t_e_mode);
                    // else the person is infected, but not contagious
                    sumTij += contribution;
                    infectionRecord.addInfectiousPerson(person.getId());
                }
            }
            if (sumTij == 0.0)
                return infectionRecord;

            // calculate the probability for all persons present
            double pInfection = 1.0 - Math.exp(factor * sumTij);

            // check if we infect others
            for (TIntIterator it = location.getAllPersonIds().iterator(); it.hasNext();)
            {
                Person person = personMap.get(it.next());
                if (person.getDiseasePhase().isSusceptible())
                {
                    // roll the dice
                    if (this.model.getU01().draw() < pInfection)
                    {
                        infectionRecord.addInfectedPerson(person.getId());
                    }
                }
            }
        }
        return infectionRecord;
    }

    /** {@inheritDoc} */
    @Override
    public void setParameter(final String parameterName, final double value)
    {
        System.err.println("Unrecognized variable name " + parameterName);
    }
}
