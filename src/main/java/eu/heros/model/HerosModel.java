package eu.heros.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.djutils.draw.bounds.Bounds2d;
import org.djutils.io.URLResource;

import eu.heros.factory.ConstructHerosModel;
import eu.heros.person.CollegeStudent;
import eu.heros.person.EssentialWorker;
import eu.heros.person.Infant;
import eu.heros.person.KindergartenStudent;
import eu.heros.person.Pensioner;
import eu.heros.person.PrimarySchoolStudent;
import eu.heros.person.SecondarySchoolStudent;
import eu.heros.person.Unemployed;
import eu.heros.person.UniversityStudent;
import eu.heros.person.WeekendWorker;
import eu.heros.person.Worker;
import eu.heros.person.WorkerCityToSatellite;
import eu.heros.person.WorkerSatelliteToCity;
import eu.heros.person.WorkerSatelliteToSatellite;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.animation.gis.GisRenderable2D;
import nl.tudelft.simulation.dsol.animation.gis.osm.OsmFileCsvParser;
import nl.tudelft.simulation.dsol.animation.gis.osm.OsmRenderable2D;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterDouble;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterException;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterInteger;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterMap;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterString;
import nl.tudelft.simulation.medlabs.MedlabsRuntimeException;
import nl.tudelft.simulation.medlabs.location.Location;
import nl.tudelft.simulation.medlabs.location.LocationType;
import nl.tudelft.simulation.medlabs.model.AbstractMedlabsModel;
import nl.tudelft.simulation.medlabs.person.Person;
import nl.tudelft.simulation.medlabs.person.index.IdxPerson;
import nl.tudelft.simulation.medlabs.policy.ClosurePolicy;
import nl.tudelft.simulation.medlabs.policy.Policy;
import nl.tudelft.simulation.medlabs.properties.Properties;
import nl.tudelft.simulation.medlabs.simulation.SimpleDEVSSimulatorInterface;

/**
 * HerosModel.java.
 * <p>
 * Copyright (c) 2020-2022 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights reserved. The
 * code is part of the HERoS project (Health Emergency Response in Interconnected Systems), which builds on the MEDLABS project.
 * The simulation tools are aimed at providing policy analysis tools to predict and help contain the spread of epidemics. They
 * make use of the DSOL simulation engine and the agent-based modeling formalism. This software is licensed under the BSD
 * license. See license.txt in the main project.
 * </p>
 * @author <a href="https://www.linkedin.com/in/mikhailsirenko">Mikhail Sirenko</a>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class HerosModel extends AbstractMedlabsModel
{
    /** */
    private static final long serialVersionUID = 20200919L;

    /** the base path for the input files. */
    private String basePath;

    /** the GIS map. */
    private GisRenderable2D gisMap;

    /** the cached extent. */
    private Bounds2d extent = null;

    /** the extra person properties. */
    private Properties properties;

    /** the currently activated policy. TODO: has to be changed as more policies can be active at the same time. */
    private Policy currentPolicy;

    /** the types of persons for the week pattern change. */
    private Map<Class<? extends Person>, String> personTypes = new HashMap<>();

    /**
     * Construct the model.
     * @param simulator SimpleDEVSSimulatorInterface; the simulator
     * @param propertyFilename String; the path of the property file name to use
     */
    public HerosModel(final SimpleDEVSSimulatorInterface simulator, final String propertyFilename)
    {
        super(simulator, propertyFilename);
    }

    /** {@inheritDoc} */
    @Override
    public Serializable getSourceId()
    {
        return "MEDLABS HERoS Model";
    }

    /** {@inheritDoc} */
    @Override
    public Bounds2d getExtent()
    {
        if (this.extent == null)
        {
            double minX = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            for (Location location : this.getLocationMap().valueCollection())
            {
                double x = location.getLongitude();
                double y = location.getLatitude();
                if (x == 0.0 || y == 0.0)
                    continue;
                if (x < minX)
                    minX = x;
                if (x > maxX)
                    maxX = x;
                if (y < minY)
                    minY = y;
                if (y > maxY)
                    maxY = y;
            }
            this.extent = new Bounds2d(minX, maxX, minY, maxY);
        }
        return this.extent;
    }

    private File getFileFromParam(final String param)
    {
        String paramValue = getParameterValue(param).trim();
        String basePath = getBasePath();
        if (paramValue.length() > 0)
        {
            File file = new File(basePath + "/" + paramValue);
            if (file != null && file.exists() && !file.isDirectory())
            {
                System.out.println("Used " + param + " = " + file.getAbsolutePath());
                return file.getAbsoluteFile();
            }
            file = new File(paramValue);
            if (file != null && file.exists() && !file.isDirectory())
            {
                System.out.println("Used " + param + " = " + file.getAbsolutePath());
                return file.getAbsoluteFile();
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void constructModel() throws SimRuntimeException
    {
        super.constructModel();
        schedulePolicies();
        makePersonTypes();

        if (isInteractive())
        {
            // read the GIS map
            File csvFile = getFileFromParam("generic.osmControlFile");
            if (csvFile == null)
            {
                System.err.println("Map control file not found -- no map");
            }
            else
            {
                URL csvUrl = URLResource.getResource(csvFile.getAbsolutePath());
                System.out.println("GIS definitions file: " + csvUrl.toString());
                File osmFile = getFileFromParam("generic.osmMapFile");
                if (osmFile == null)
                {
                    System.err.println("Map file not found -- no map");
                }
                else
                {
                    URL osmUrl = URLResource.getResource(osmFile.getAbsolutePath());
                    System.out.println("GIS map file: " + osmUrl.toString());
                    try
                    {
                        this.gisMap = new OsmRenderable2D(getSimulator().getReplication(),
                                OsmFileCsvParser.parseMapFile(csvUrl, osmUrl, "The Hague"));
                    }
                    catch (IOException exception)
                    {
                        throw new SimRuntimeException(exception);
                    }
                }
            }
        }
        else
        {
            getSimulator().scheduleEventNow(this, this, "hourTick", null);
        }
    }

    /**
     * @return gisMap
     */
    public GisRenderable2D getGisMap()
    {
        return this.gisMap;
    }

    protected void hourTick()
    {
        int hour = (int) Math.round(getSimulator().getSimulatorTime());
        if (hour % 24 == 0)
        {
            System.out.print("\nDay " + (hour / 24) + "  ");
        }
        else
        {
            System.out.print(".");
        }
        getSimulator().scheduleEventRel(1.0, this, this, "hourTick", null);
    }

    /** {@inheritDoc} */
    @Override
    protected void constructModelFromSource()
    {
        new ConstructHerosModel(this);
        // XXX: hack -- this is not how we should do this...
        this.properties = new Properties(IdxPerson.class, getPersonMap().size());
        this.currentPolicy = new Policy(this, "0");
    }

    /**
     * Make the person types for the week pattern names.
     */
    private void makePersonTypes()
    {
        this.personTypes.put(CollegeStudent.class, "college student");
        this.personTypes.put(EssentialWorker.class, "essential worker");
        this.personTypes.put(Infant.class, "infant");
        this.personTypes.put(KindergartenStudent.class, "kindergarten student");
        this.personTypes.put(Pensioner.class, "pensioner");
        this.personTypes.put(PrimarySchoolStudent.class, "primary school student");
        this.personTypes.put(SecondarySchoolStudent.class, "secondary school student");
        this.personTypes.put(Unemployed.class, "unemployed job-seeker");
        this.personTypes.put(UniversityStudent.class, "university student");
        this.personTypes.put(WeekendWorker.class, "weekend worker");
        this.personTypes.put(Worker.class, "worker");
        this.personTypes.put(WorkerCityToSatellite.class, "worker city to satellite");
        this.personTypes.put(WorkerSatelliteToCity.class, "worker satellite to city");
        this.personTypes.put(WorkerSatelliteToSatellite.class, "worker satellite to satellite");
    }

    @Override
    protected void checkChangeWeekPattern()
    {
        for (Person person : getPersonMap().valueCollection())
        {
            if (person.getDiseasePhase().isDead())
                continue;
            String oldWeekPatternName = person.getCurrentWeekPattern().getName();
            String newWeekPatternName = getCurrentPolicy().getName() + "_" + person.getDiseasePhase().getName() + "_"
                    + this.personTypes.get(person.getClass());
            if (!getWeekPatternMap().containsKey(newWeekPatternName))
                throw new MedlabsRuntimeException("Week pattern " + newWeekPatternName + " not found");
            if (!oldWeekPatternName.equals(newWeekPatternName))
            {
                person.setCurrentWeekPattern(getWeekPatternMap().get(newWeekPatternName));
            }
        }
        getSimulator().scheduleEventRel(24.0, this, this, "checkChangeWeekPattern", null);
    }

    @Override
    public Properties getPersonProperties()
    {
        return this.properties;
    }

    /** {@inheritDoc} */
    @Override
    protected void extendInputParameterMap() throws InputParameterException
    {
        InputParameterMap root = this.inputParameterMap;

        InputParameterMap genericMap = (InputParameterMap) root.get("generic");
        genericMap.add(new InputParameterString("PersonFilePath", "path and name for the person file",
                "blank means standard people.csv.gz file", "", 1.1));
        genericMap.add(new InputParameterString("LocationsFilePath", "path and name for the locations file",
                "blank means standard locations.csv.gz file", "", 1.2));
        genericMap.add(new InputParameterString("LocationTypesFilePath", "path and name for the locationtypes file",
                "blank means standard locationtypes.csv file", "", 1.3));
        genericMap.add(new InputParameterString("ActivityFilePath", "path and name for the activitypatterns file",
                "blank means standard activityschedules.xlsx file", "", 1.4));
        genericMap.add(new InputParameterString("osmControlFile", "path and name for the OSM control file",
                "blank means no map for animation", "", 1.5));
        genericMap.add(new InputParameterString("osmMapFile", "path and name for the OSM map file",
                "blank means no map for animation", "", 1.6));
        genericMap
                .add(new InputParameterString("generic.diseasePropertiesFile", "path and name for the disease properties file",
                        "can be resource, absolute or relative", "/alpha.properties", 1.7));

        InputParameterMap policyMap = (InputParameterMap) root.get("policies");
        policyMap.add(new InputParameterInteger("NumberInfected", "number of people infected at t=0", "(can be 0)", 0, 1.0));
        policyMap.add(new InputParameterInteger("MinAgeInfected", "lowest age of people infected at t=0", "between 0 and 100.",
                0, 0, 100, "%d", 2.0));
        policyMap.add(new InputParameterInteger("MaxAgeInfected", "highest age of people infected at t=0", "between 0 and 100.",
                100, 0, 100, "%d", 3.0));
        policyMap.add(new InputParameterInteger("DayStartSoftLockdown", "day Soft Lockdown is effected (-1 is not)",
                "number between -1 and 90", -1, -1, 90, "%d", 4.0));
        policyMap.add(new InputParameterInteger("DayStartHardLockdown", "day Hard Lockdown is effected (-1 is not)",
                "number between -1 and 90", -1, -1, 90, "%d", 5.0));
        policyMap.add(new InputParameterInteger("DayStartSocialDist", "day Social Distancing is effected (-1 is not)",
                "number between -1 and 90", -1, -1, 90, "%d", 6.0));
        policyMap.add(new InputParameterDouble("SocialDistFactor", "Social Distancing factor sigma (between 0 and 1)",
                "number between 0.0 and 1.0", 1.0, 0.0, 1.0, true, true, "%f.2", 6.5));
        policyMap.add(new InputParameterInteger("DayStartWearMasks", "day Wear Masks is effected (-1 is not)",
                "number between -1 and 90", -1, -1, 90, "%d", 7.0));

        InputParameterMap covidTransmissionMap =
                new InputParameterMap("covidT", "Covid Transmission", "Covid Transmission parameters", 1.5);

        covidTransmissionMap.add(new InputParameterDouble("contagiousness", "contagiousness as calculated from exp-formula",
                "value between 0.0 and 1.0", 0.5, 0.0, 1.0, true, true, "%f", 1.0));
        covidTransmissionMap
                .add(new InputParameterDouble("beta", "initial personal protection factor (masks, other protection)",
                        "value between 0.0 and 1.0", 1.0, 0.0, 1.0, true, true, "%f", 1.0));

        covidTransmissionMap.add(new InputParameterDouble("t_e_min", "first day of contagiousness of an exposed person (days)",
                "Triangular.min, time in days", 3.0, 0.0, 60.0, true, true, "%f", 2.0));
        covidTransmissionMap.add(new InputParameterDouble("t_e_mode", "peak day of contagiousness of an exposed person (days)",
                "Triangular.mode, time in days", 7.0, 0.0, 60.0, true, true, "%f", 3.0));
        covidTransmissionMap.add(new InputParameterDouble("t_e_max", "last day of contagiousness of an exposed person (days)",
                "Triangular.max, time in days", 14.0, 0.0, 60.0, true, true, "%f", 4.0));

        covidTransmissionMap.add(new InputParameterDouble("calculation_threshold",
                "threshold for the transmission contact calculation (sec)",
                "Below this contact duration, no infections will be calculated", 60, 0.0, 3600.0, true, true, "%f", 5.0));

        root.add(covidTransmissionMap);

        InputParameterMap covidProgressionMap =
                new InputParameterMap("covidP", "Covid Progression", "Covid Progression parameters", 1.6);

        covidProgressionMap.add(new InputParameterDouble("FractionAsymptomatic", "Fraction that is asymptomatic E->I(A)",
                "Probability between 0.0 and 1.0", 0.46, 0.0, 1.0, true, true, "%f", 1.0));
        covidProgressionMap.add(new InputParameterString("IncubationPeriodAsymptomatic",
                "Incubation period asymptomatic E->I(A)", "Distribution, time in days", "Triangular(2.5, 3.4, 3.8)", 2.0));
        covidProgressionMap.add(new InputParameterString("IncubationPeriodSymptomatic", "Incubation period symptomatic E->I(S)",
                "Distribution, time in days", "Triangular(2.5, 3.4, 3.8)", 3.0));
        covidProgressionMap.add(new InputParameterString("PeriodAsymptomaticToRecovered",
                "Period asymptomatic to recovered I(A)->R", "Distribution, time in days", "Triangular(7, 12, 14)", 4.0));
        covidProgressionMap.add(
                new InputParameterString("FractionSymptomaticToHospitalized", "Fraction symptomatic to hospitalized I(S)->I(H)",
                        "Distribution, time in days", "age{0-29: 0.02, 30-49: 0.1, 50-59: 0.3, 60-80: 0.6, 80-100: 0.2", 5.0));
        covidProgressionMap.add(new InputParameterString("PeriodSymptomaticToHospitalized",
                "Period symptomatic to hospitalized I(S)->I(H)", "Distribution, time in days", "Triangular(5, 7.5, 10)", 6.0));
        covidProgressionMap.add(new InputParameterString("PeriodSymptomaticToRecovered",
                "Period symptomatic to recovered I(S)->R", "Distribution, time in days", "Triangular(5, 7.5, 10)", 7.0));
        covidProgressionMap.add(new InputParameterString("FractionHospitalizedToICU", "Fraction hospitalized to ICU I(H)->I(I)",
                "Distribution, time in days", "age{0-29: 0.05, 30-49: 0.02, 50-59: 0.05, 60-80: 0.15, 80-100: 0.0", 8.0));
        covidProgressionMap.add(new InputParameterString("FractionHospitalizedToDead", "Fraction hospitalized to dead I(H)->D",
                "Distribution, time in days", "0.0", 9.0));
        covidProgressionMap.add(new InputParameterString("PeriodHospitalizedToICU",
                "Period hospitalized to ICU I(H)->I(I)", "Distribution, time in days", "Triangular(3,8,14)", 10.0));
        covidProgressionMap.add(new InputParameterString("PeriodHospitalizedToDead",
                "Period hospitalized to dead I(H)->D", "Distribution, time in days", "Triangular(3,8,14)", 11.0));
        covidProgressionMap.add(new InputParameterString("PeriodHospitalizedToRecovered",
                "Period hospitalized to recovered I(H)->R", "Distribution, time in days", "Triangular(3,8,14)", 12.0));
        covidProgressionMap.add(new InputParameterString("FractionICUToDead", "Fraction ICU to dead I(I)->D",
                "Distribution, time in days", "age{0-49: 0, 50-59: 0.015, 60-69: 0.08, 70-79: 0.4, 80-100: 0.6}", 13.0));
        covidProgressionMap.add(new InputParameterString("PeriodICUToDead",
                "Period ICU to dead I(I)->D", "Distribution, time in days", "Triangular(1, 14, 30)", 14.0));
        covidProgressionMap.add(new InputParameterString("PeriodICUToRecovered",
                "Period ICU to recovered I(I)->R", "Distribution, time in days", "Triangular(10, 14, 30)", 15.0));

        root.add(covidProgressionMap);
    }

    /**
     * Schedule the policies at the start of day "n".
     */
    private void schedulePolicies()
    {
        int dayPolicy1 = getParameterValueInt("policies.DayStartSoftLockdown");
        if (dayPolicy1 > 0)
        {
            getSimulator().scheduleEventAbs(24.0 * dayPolicy1, this, this, "startSoftLockdown", null);
        }

        int dayPolicy2 = getParameterValueInt("policies.DayStartHardLockdown");
        if (dayPolicy2 > 0)
        {
            getSimulator().scheduleEventAbs(24.0 * dayPolicy2, this, this, "startHardLockdown", null);
        }

        int dayPolicy3 = getParameterValueInt("policies.DayStartSocialDist");
        if (dayPolicy3 > 0)
        {
            getSimulator().scheduleEventAbs(24.0 * dayPolicy3, this, this, "startSocialDist", null);
        }

        int dayPolicy4 = getParameterValueInt("policies.DayStartWearMasks");
        if (dayPolicy4 > 0)
        {
            getSimulator().scheduleEventAbs(24.0 * dayPolicy4, this, this, "startWearMask", null);
        }

    }

    /**
     * Policy 1 - Schools shut, bars and restaurants shut, recreation shut, parks open, all retail categories open and
     * workplaces open.
     */
    protected void startSoftLockdown()
    {
        System.out.println("\nHour " + getSimulator().getSimulatorTime() + ": Start of policy 1 execution");
        List<LocationType> closureLocations = new ArrayList<>();
        closureLocations.add(getLocationTypeNameMap().get("BarRestaurant"));
        closureLocations.add(getLocationTypeNameMap().get("Recreation"));
        closureLocations.add(getLocationTypeNameMap().get("Kindergarten"));
        closureLocations.add(getLocationTypeNameMap().get("PrimarySchool"));
        closureLocations.add(getLocationTypeNameMap().get("SecondarySchool"));
        closureLocations.add(getLocationTypeNameMap().get("College"));
        closureLocations.add(getLocationTypeNameMap().get("University"));
        ClosurePolicy policy1 = new ClosurePolicy(this, "1", closureLocations);
        this.currentPolicy = policy1;
        activatePolicy(policy1);
        policy1.close(); // implement the policy
    }

    /**
     * Policy 2 - All shut except pharmacy, supermarket and parks.
     */
    protected void startHardLockdown()
    {
        System.out.println("\nHour " + getSimulator().getSimulatorTime() + ": Start of policy 2 execution");
        List<LocationType> closureLocations = new ArrayList<>();
        closureLocations.add(getLocationTypeNameMap().get("Workplace"));
        closureLocations.add(getLocationTypeNameMap().get("BarRestaurant"));
        closureLocations.add(getLocationTypeNameMap().get("Retail"));
        closureLocations.add(getLocationTypeNameMap().get("Mall"));
        closureLocations.add(getLocationTypeNameMap().get("FoodBeverage"));
        closureLocations.add(getLocationTypeNameMap().get("Kindergarten"));
        closureLocations.add(getLocationTypeNameMap().get("PrimarySchool"));
        closureLocations.add(getLocationTypeNameMap().get("SecondarySchool"));
        closureLocations.add(getLocationTypeNameMap().get("College"));
        closureLocations.add(getLocationTypeNameMap().get("University"));
        closureLocations.add(getLocationTypeNameMap().get("Religion"));
        closureLocations.add(getLocationTypeNameMap().get("Recreation"));
        closureLocations.add(getLocationTypeNameMap().get("Healthcare"));
        ClosurePolicy policy2 = new ClosurePolicy(this, "2", closureLocations);
        this.currentPolicy = policy2;
        activatePolicy(policy2);
        policy2.close(); // implement the policy
    }

    /**
     * Policy 3 - Social Distancing
     */
    protected void startSocialDist()
    {
        System.out.println("\nHour " + getSimulator().getSimulatorTime() + ": Start of SocialDist policy execution");
        // Python - for lt:LocationType in locationTypeIdMap.values():
        for (LocationType lt : this.locationTypeIdMap.values())
        {
            if (!lt.getName().equals("Accommodation"))
                lt.setCorrectionFactorArea(getParameterValueDouble("policies.SocialDistFactor"));
        }
    }

    @Override
    public void checkChangeActivityPattern(final Person person)
    {
        if (person.getDiseasePhase().isDead())
            return;
        String diseasePhaseName = person.getDiseasePhase().getName();
        String personTypeName = this.personTypes.get(person.getClass());
        if (personTypeName == null)
        {
            throw new MedlabsRuntimeException("Person type name " + person.getClass() + " not found");
        }
        String weekPatternKey = getCurrentPolicy().getName() + "_" + diseasePhaseName + "_" + personTypeName;
        if (getWeekPatternMap().get(weekPatternKey) == null)
        {
            throw new MedlabsRuntimeException("Week pattern key" + weekPatternKey + " not found in week pattern map");
        }
        person.setCurrentWeekPattern(getWeekPatternMap().get(weekPatternKey));
    }

    /**
     * @return the basePath
     */
    public String getBasePath()
    {
        return this.basePath;
    }

    /**
     * @param basePath the basePath to set
     */
    public void setBasePath(final String basePath)
    {
        this.basePath = basePath;
    }

    /**
     * @return the currentPolicy
     */
    public Policy getCurrentPolicy()
    {
        return this.currentPolicy;
    }

}
