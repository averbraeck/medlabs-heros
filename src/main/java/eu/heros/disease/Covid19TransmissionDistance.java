package eu.heros.disease;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.set.TIntSet;
import nl.tudelft.simulation.medlabs.disease.DiseaseTransmission;
import nl.tudelft.simulation.medlabs.location.Location;
import nl.tudelft.simulation.medlabs.location.LocationType;
import nl.tudelft.simulation.medlabs.model.MedlabsModelInterface;
import nl.tudelft.simulation.medlabs.person.Person;

public class Covid19TransmissionDistance extends DiseaseTransmission
{
    /** */
    private static final long serialVersionUID = 1L;

    /** Latent period L (days). */
    private final double L;

    /** Incubation period I (days). includes L, so I larger than L */
    private double I;

    /** Clinical disease period C (days). C starts after L. */
    private final double C;

    /** Peak viral load v_max (positive). */
    private final double v_max;

    /** Reference viral load v_0 (positive). */
    private final double v_0;

    /** Transmission rate r (positive). */
    private final double r;

    /** Social distancing factor psi (positive). */
    private final double psi;

    /** Calibraton factor alpha (positive). */
    private final double alpha;

    /** Mask effectiveness mu from interval [0, 1]. */
    private final double mu;

    /**
     * Parameter to indicate when the duration is too short to make an infection calculation (converted to hours). Note that for
     * public transport and shops the interval should not be set too short since people get in and out frequently.
     */
    private final double calculationThreshold;

    /**
     * Create the Covid19 Transmission model.
     * @param model MedlabsModelInterface; the Medlabs model
     */
    public Covid19TransmissionDistance(final MedlabsModelInterface model)
    {
        super(model, "Covid19");

        this.L = model.getParameterValueDouble("covidT_dist.L") * 24.0;
        this.I = model.getParameterValueDouble("covidT_dist.I") * 24.0;
        this.C = model.getParameterValueDouble("covidT_dist.C") * 24.0;
        this.v_max = model.getParameterValueDouble("covidT_dist.v_max");
        this.v_0 = model.getParameterValueDouble("covidT_dist.v_0");
        this.r = model.getParameterValueDouble("covidT_dist.r");
        this.psi = model.getParameterValueDouble("covidT_dist.psi");
        this.alpha = model.getParameterValueDouble("covidT_dist.alpha");
        this.mu = model.getParameterValueDouble("covidT_dist.mu");
        this.calculationThreshold = model.getParameterValueDouble("covidT_dist.calculation_threshold") / 3600.0;
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
     * 
     * p_i = 1 - exp[ SUM_j=i:M_k [- (1 - mu)^2 . P_j(d) . t_i,j . sigma (max(DELTA(A_k, N_k), psi)) . alpha ]
     * 
     * where:
     * 
     * k        is the (sub)location index
     * i        is the index of a susceptible person in (sub)location k
     * j        is the index of an infectious person in (sub)location k
     * M_k      is the number of infectious persons in (sub)location k
     * N_k      is the number of persons in (sub)location K which is of location type T
     * p_j(d)   is the infectiousness of person j for the number of days (d) since the exposure date of person j; 
     *          e.g., the infectiousness can be 0 for the first 3 days, then climb to 7 in 4 days, and then decrease to 0
     *          in for instance a week.
     * mu       is the masking factor between 0 (no masks) and 1 (fully protected) 
     * t_i,j    is the time that contagious person j and susceptible person i have spent together in location K (in hours)
     * sigma    is the function that translates average distance to transmission probability
     * DELTA    is the function that transforms area A_k to average distance
     * A_k      is the area of (sub)location k
     * psi      is the social distancing factor, as the minimum distance that people keep
     * alpha    is a calibration factor
     *  
     * </code>
     * </pre>
     * 
     * @param location Location; the location for which to calculate infectious spread
     * @param subLocationIndex int; the sublocation for which to calculate possible infection(s)
     * @param duration double; the time for which the calculation needs to take place, in hours
     * @return boolean; whether a calculation took place and the last calculation time in DiseaseTransmission can be updated
     */
    @Override
    public boolean infectPeople(final Location location, final TIntSet personsInSublocation, final double duration)
    {
        // has contact been too short?
        if (duration < this.calculationThreshold)
            return false;

        // find the infectious persons in the sublocation
        LocationType lt = location.getLocationType();
        double area = location.getTotalSurfaceM2();

        TIntObjectMap<Person> personMap = this.model.getPersonMap();
        double now = this.model.getSimulator().getSimulatorTime().doubleValue();

        if (lt.isInfectInSublocation() || location.getNumberOfSubLocations() < 2)
        {
            // INFECTION TAKES PLACE JUST IN THE SUBLOCATION
            // NOTE: WHEN THE LOCATION HAS ONLY 1 SUBLOCATION THIS PART OF THE METHOD IS USED (MUCH FASTER)

            // Calculate Delta. Delta = sqrt(A_k / N_k)
            area /= location.getNumberOfSubLocations();
            double Delta = Math.sqrt(area / personsInSublocation.size());
            // calculate sigma(max(Delta, psi))
            // sigma is ~ 100% transmission probability at 0 m, 50% at 1.5 m, and ~ 0% at 3 m.
            // Shape of sigma function is a sigmoid, with above parameters 1 - 1 / exp(-3 * (d - 1.5))
            double sigma = 1.0 - 1.0 / (1.0 + Math.exp(-3.0 * (Math.max(Delta, this.psi) - 1.5)));
            double factor = sigma * this.alpha * (1.0 - this.mu) * (1.0 - this.mu);
            if (factor == 0.0)
                return true;

            // find the infectious persons in the sublocation
            double sum = 0.0;
            double maxVt = 0.0;
            Person mostInfectiousPerson = null;
            for (TIntIterator it = personsInSublocation.iterator(); it.hasNext();)
            {
                Person person = personMap.get(it.next());
                if (person.getDiseasePhase().isIll())
                {
                    double v_t = 0.0;
                    double t = now - person.getExposureTime();
                    if (t >= this.L && t < this.I)
                        v_t = this.v_max * ((t - this.L) / (this.I - this.L));
                    else if (t >= this.I && t < this.I + this.C)
                        v_t = this.v_max * ((this.I + this.C - t) / this.C);
                    // else the person is infected, but not yet or not anymore contagious
                    if (v_t > 0)
                    {
                        double Pt = 1 / (1 + Math.exp(-this.r * (v_t - this.v_0)));
                        sum += factor * duration * Pt;
                        if (v_t > maxVt)
                        {
                            maxVt = v_t;
                            mostInfectiousPerson = person;
                        }
                    }
                }
            }
            if (sum == 0.0)
                return true;

            // calculate the probability for all persons present in the sublocation
            double pInfection = 1.0 - Math.exp(-sum);
            // check if we infect others
            for (TIntIterator it = personsInSublocation.iterator(); it.hasNext();)
            {
                Person person = personMap.get(it.next());
                if (person.getDiseasePhase().isSusceptible())
                {
                    // roll the dice
                    if (this.model.getU01().draw() < pInfection)
                    {
                        person.setExposureTime((float) now);
                        this.model.getPersonMonitor().reportExposure(person, location, mostInfectiousPerson);
                        this.model.getDiseaseProgression().changeDiseasePhase(person, Covid19Progression.exposed);
                    }
                }
            }
        }

        else

        {
            // INFECTION TAKES PLACE IN THE TOTAL LOCATION
            // TRY TO AVOID CALLING THIS -- IT IS EXPENSIVE

            // Calculate Delta. Delta = sqrt(A_k / N_k)
            double Delta = Math.sqrt(area / personsInSublocation.size());
            // calculate sigma(max(Delta, psi))
            // sigma is ~ 100% transmission probability at 0 m, 50% at 1.5 m, and ~ 0% at 3 m.
            // Shape of sigma function is a sigmoid, with above parameters 1 - 1 / exp(-3 * (d - 1.5))
            double sigma = 1.0 - 1.0 / (1.0 + Math.exp(-3.0 * (Math.max(Delta, this.psi) - 1.5)));
            double factor = sigma * this.alpha * (1.0 - this.mu) * (1.0 - this.mu);
            if (factor == 0.0)
                return true;

            // find the infectious persons in the sublocation
            double sum = 0.0;
            double maxVt = 0.0;
            Person mostInfectiousPerson = null;
            for (TIntIterator it = location.getAllPersonIds().iterator(); it.hasNext();)
            {
                Person person = personMap.get(it.next());
                if (person.getDiseasePhase().isIll())
                {
                    double v_t = 0.0;
                    double t = now - person.getExposureTime();
                    if (t >= this.L && t < this.I)
                        v_t = this.v_max * ((t - this.L) / (this.I - this.L));
                    else if (t >= this.I && t < this.I + this.C)
                        v_t = this.v_max * ((this.I + this.C - t) / this.C);
                    // else the person is infected, but not yet or not anymore contagious
                    if (v_t > 0)
                    {
                        double Pt = 1 / (1 + Math.exp(-this.r * (v_t - this.v_0)));
                        sum += factor * duration * Pt;
                        if (v_t > maxVt)
                        {
                            maxVt = v_t;
                            mostInfectiousPerson = person;
                        }
                    }
                }
            }
            if (sum == 0.0)
                return true;

            // calculate the probability for all persons present
            double pInfection = 1.0 - Math.exp(-sum);

            // check if we infect others
            for (TIntIterator it = location.getAllPersonIds().iterator(); it.hasNext();)
            {
                Person person = personMap.get(it.next());
                if (person.getDiseasePhase().isSusceptible())
                {
                    // roll the dice
                    if (this.model.getU01().draw() < pInfection)
                    {
                        person.setExposureTime((float) now);
                        this.model.getPersonMonitor().reportExposure(person, location, mostInfectiousPerson);
                        this.model.getDiseaseProgression().changeDiseasePhase(person, Covid19Progression.exposed);
                    }
                }
            }
        }
        return true;
    }

}
