package eu.heros.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
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
import eu.heros.person.WorkerCountryToCity;
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
import nl.tudelft.simulation.medlabs.properties.Properties;
import nl.tudelft.simulation.medlabs.simulation.SimpleDEVSSimulatorInterface;

/**
 * HerosModel.java.
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

    /** the types of persons for the week pattern change. */
    private Map<Class<? extends Person>, String> personTypes = new HashMap<>();

    /** The file with nr of persons per location. */
    private PrintWriter locationNrWriter;

    /** The file with nr of persons per sublocation. */
    private PrintWriter sublocationNrWriter;

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
        getSimulator().scheduleEventNow(this, this, "scheduleLocationDump", null);
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

    protected void scheduleLocationDump() throws FileNotFoundException
    {
        if (getParameterValueBoolean("generic.WriteOutput"))
        {
            String outputPath = getParameterValue("generic.OutputPath");

            this.locationNrWriter = new PrintWriter(outputPath + "/locationNrPersons.csv");
            this.locationNrWriter.print("\"Time(h)\",\"LocationNr\",\"NrPersons\"\n");
            this.locationNrWriter.flush();

            this.sublocationNrWriter = new PrintWriter(outputPath + "/sublocationNrPersons.csv");
            this.sublocationNrWriter.print("\"Time(h)\",\"LocationNr\",\"SubLocationNr\",\"NrPersons\"\n");
            this.sublocationNrWriter.flush();

            locationDump();
        }
    }

    protected void locationDump()
    {
        double time = getSimulator().getSimulatorTime();
        for (LocationType locationType : this.locationTypeList)
        {
            for (Location location : locationType.getLocationMap().valueCollection())
            {
                if (location.getId() >= 0)
                {
                    this.locationNrWriter.print(time + "," + location.getId() + "," + location.getAllPersonIds().size() + "\n");
                    for (int subLocationIndex = 1; subLocationIndex < location.getNumberOfSubLocations(); subLocationIndex++)
                    {
                        int nrSub = this.diseaseTransmission.getNrPersonsInSublocation(location, (short) subLocationIndex);
                        this.sublocationNrWriter
                                .print(time + "," + location.getId() + "," + subLocationIndex + "," + nrSub + "\n");
                    }
                }
            }
        }
        this.locationNrWriter.flush();
        this.sublocationNrWriter.flush();
        getSimulator().scheduleEventRel(1.0, this, this, "locationDump", null);
    }

    /** {@inheritDoc} */
    @Override
    protected void constructModelFromSource()
    {
        new ConstructHerosModel(this);
        // XXX: hack -- this is not how we should do this...
        this.properties = new Properties(IdxPerson.class, getPersonMap().size());
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
        this.personTypes.put(WorkerCountryToCity.class, "worker country to city");
    }

    @Override
    protected void checkChangeWeekPattern()
    {
        // System.out.println("checkChangeWeekPattern @ " + getSimulator().getSimulatorTime() + " h");
        for (Person person : getPersonMap().valueCollection())
        {
            if (person.getDiseasePhase().isDead())
                continue;
            String oldWeekPatternName = person.getCurrentWeekPattern().getName();
            String newWeekPatternName = "0_" + person.getDiseasePhase().getName() + "_"
                    + this.personTypes.get(person.getClass());
            if (!getWeekPatternMap().containsKey(newWeekPatternName))
            {
                System.err.println("checkChangeWeekPattern - New week pattern " + newWeekPatternName + " not found");
                // throw new MedlabsRuntimeException("Week pattern " + newWeekPatternName + " not found");
            }
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
        genericMap.add(new InputParameterString("ProbRatioFilePath", "path and name for the probability based infections file",
                "blank means standard infection_ratio.csv file", "", 1.45));
        genericMap.add(new InputParameterString("osmControlFile", "path and name for the OSM control file",
                "blank means no map for animation", "", 1.5));
        genericMap.add(new InputParameterString("osmMapFile", "path and name for the OSM map file",
                "blank means no map for animation", "", 1.6));
        genericMap.add(new InputParameterString("diseasePropertiesModel", "area-based or distance-based transmission",
                "[R/O] has to match file, value = {area, distance}", "area", 1.7));
        genericMap.add(new InputParameterString("diseasePropertiesFile", "path and name for the disease properties file",
                "[R/O] can be resource, absolute or relative", "/alpha.properties", 1.8));

        InputParameterMap policyMap = (InputParameterMap) root.get("policies");
        policyMap.add(new InputParameterInteger("NumberInfected", "number of people infected at t=0", "(can be 0)", 0, 1.0));
        policyMap.add(new InputParameterInteger("MinAgeInfected", "lowest age of people infected at t=0", "between 0 and 100.",
                0, 0, 100, "%d", 2.0));
        policyMap.add(new InputParameterInteger("MaxAgeInfected", "highest age of people infected at t=0", "between 0 and 100.",
                100, 0, 100, "%d", 3.0));
        policyMap.add(new InputParameterString("LocationPolicyFile", "path and name for the location policies file",
                "(blank means no policies)", "", 4.0));
        policyMap.add(new InputParameterString("DiseasePolicyFile", "path and name for the disease policies file",
                "(blank means no policies)", "", 5.0));

        InputParameterMap covidTransmissionAreaMap = new InputParameterMap("covidT_area", "Covid Transmission by Area",
                "Covid Transmission parameters per area", 1.5);
        covidTransmissionAreaMap.add(new InputParameterDouble("contagiousness", "contagiousness as calculated from exp-formula",
                "value between 0.0 and 1.0", 0.5, 0.0, 1.0, true, true, "%f", 1.0));
        covidTransmissionAreaMap
                .add(new InputParameterDouble("beta", "initial personal protection factor (masks, other protection)",
                        "value between 0.0 and 1.0", 1.0, 0.0, 1.0, true, true, "%f", 1.5));
        covidTransmissionAreaMap
                .add(new InputParameterDouble("t_e_min", "first day of contagiousness of an exposed person (days)",
                        "Triangular.min, time in days", 3.0, 0.0, 60.0, true, true, "%f", 2.0));
        covidTransmissionAreaMap
                .add(new InputParameterDouble("t_e_mode", "peak day of contagiousness of an exposed person (days)",
                        "Triangular.mode, time in days", 7.0, 0.0, 60.0, true, true, "%f", 3.0));
        covidTransmissionAreaMap
                .add(new InputParameterDouble("t_e_max", "last day of contagiousness of an exposed person (days)",
                        "Triangular.max, time in days", 14.0, 0.0, 60.0, true, true, "%f", 4.0));
        covidTransmissionAreaMap.add(new InputParameterDouble("calculation_threshold",
                "threshold for the transmission contact calculation (sec)",
                "Below this contact duration, no infections will be calculated", 60, 0.0, 3600.0, true, true, "%f", 5.0));
        root.add(covidTransmissionAreaMap);

        InputParameterMap covidTransmissionDistMap = new InputParameterMap("covidT_dist", "Covid Transmission by Distance",
                "Covid Transmission parameters by distance", 1.51);
        covidTransmissionDistMap.add(new InputParameterDouble("L", "Latent period L (days)", "Viral load model", 2.0, 0.0,
                100.0, true, true, "%f", 1.0));
        covidTransmissionDistMap.add(new InputParameterDouble("I", "Incubation period I (days)",
                "Viral load model; includes L, so I > L", 3.4, 0.0, 100.0, true, true, "%f", 2.0));
        covidTransmissionDistMap.add(new InputParameterDouble("C", "Clinical disease period C (days)",
                "Viral load model; C starts after L", 3.0, 0.0, 100.0, true, true, "%f", 3.0));
        covidTransmissionDistMap.add(new InputParameterDouble("v_max", "Peak viral load v_max", "Viral load model; > 0", 7.23,
                0.0, 100.0, true, true, "%f", 4.0));
        covidTransmissionDistMap.add(new InputParameterDouble("v_0", "Reference viral load v_0",
                "Transmission probability; > 0", 4.0, 0.0, 100.0, true, true, "%f", 5.0));
        covidTransmissionDistMap.add(new InputParameterDouble("r", "Transmission rate r", "Transmission probability; > 0",
                2.294, 0.0, 100.0, true, true, "%f", 6.0));
        covidTransmissionDistMap.add(new InputParameterDouble("psi", "Social distancing factor psi",
                "Infection probability; > 0", 3.0, 0.0, 100.0, true, true, "%f", 7.0));
        covidTransmissionDistMap.add(new InputParameterDouble("alpha", "Calibraton factor alpha", "Infection probability; > 0",
                5.0, 0.0, 100.0, true, true, "%f", 8.0));
        covidTransmissionDistMap.add(new InputParameterDouble("mu", "Mask effectiveness factor mu",
                "Infection probability; [0-1]", 0.0, 0.0, 1.0, true, true, "%f", 9.0));
        covidTransmissionDistMap.add(new InputParameterDouble("calculation_threshold",
                "threshold for the transmission contact calculation (sec)",
                "Below this contact duration, no infections will be calculated", 60, 0.0, 3600.0, true, true, "%f", 10.0));
        root.add(covidTransmissionDistMap);

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
        covidProgressionMap.add(new InputParameterString("FractionSymptomaticToHospitalized",
                "Fraction symptomatic to hospitalized I(S)->I(H)", "Fraction, age{} map or gender{} map",
                "age{0-29: 0.02, 30-49: 0.1, 50-59: 0.3, 60-80: 0.6, 80-100: 0.2}", 5.0));
        covidProgressionMap.add(new InputParameterString("PeriodSymptomaticToHospitalized",
                "Period symptomatic to hospitalized I(S)->I(H)", "Distribution, time in days", "Triangular(5, 7.5, 10)", 6.0));
        covidProgressionMap.add(new InputParameterString("PeriodSymptomaticToRecovered",
                "Period symptomatic to recovered I(S)->R", "Distribution, time in days", "Triangular(5, 7.5, 10)", 7.0));
        covidProgressionMap.add(new InputParameterString("FractionHospitalizedToICU", "Fraction hospitalized to ICU I(H)->I(I)",
                "Fraction, age{} map or gender{} map", "age{0-29: 0.05, 30-49: 0.02, 50-59: 0.05, 60-80: 0.15, 80-100: 0.0}",
                8.0));
        covidProgressionMap.add(new InputParameterString("FractionHospitalizedToDead", "Fraction hospitalized to dead I(H)->D",
                "Fraction, age{} map or gender{} map", "0.0", 9.0));
        covidProgressionMap.add(new InputParameterString("PeriodHospitalizedToICU", "Period hospitalized to ICU I(H)->I(I)",
                "Distribution, time in days", "Triangular(3,8,14)", 10.0));
        covidProgressionMap.add(new InputParameterString("PeriodHospitalizedToDead", "Period hospitalized to dead I(H)->D",
                "Distribution, time in days", "Triangular(3,8,14)", 11.0));
        covidProgressionMap.add(new InputParameterString("PeriodHospitalizedToRecovered",
                "Period hospitalized to recovered I(H)->R", "Distribution, time in days", "Triangular(3,8,14)", 12.0));
        covidProgressionMap.add(new InputParameterString("FractionICUToDead", "Fraction ICU to dead I(I)->D",
                "Fraction, age{} map or gender{} map", "age{0-49: 0, 50-59: 0.015, 60-69: 0.08, 70-79: 0.4, 80-100: 0.6}",
                13.0));
        covidProgressionMap.add(new InputParameterString("PeriodICUToDead", "Period ICU to dead I(I)->D",
                "Distribution, time in days", "Triangular(1, 14, 30)", 14.0));
        covidProgressionMap.add(new InputParameterString("PeriodICUToRecovered", "Period ICU to recovered I(I)->R",
                "Distribution, time in days", "Triangular(10, 14, 30)", 15.0));
        root.add(covidProgressionMap);
    }

    @Override
    public void checkChangeActivityPattern(final Person person)
    {
        // System.out.println("checkChangeActivityPattern @ " + getSimulator().getSimulatorTime() + " h. for person " + person);
        if (person.getDiseasePhase().isDead())
            return;
        String diseasePhaseName = person.getDiseasePhase().getName();
        String personTypeName = this.personTypes.get(person.getClass());
        if (personTypeName == null)
        {
            System.err.println("checkChangeActivityPattern - Person type name " + person.getClass() + " not found");
            throw new MedlabsRuntimeException("Person type name " + person.getClass() + " not found");
        }
        String weekPatternKey = "0_" + diseasePhaseName + "_" + personTypeName;
        if (getWeekPatternMap().get(weekPatternKey) == null)
        {
            System.err.println(
                    "checkChangeActivityPattern - Week pattern key" + weekPatternKey + " not found in week pattern map");
            throw new MedlabsRuntimeException("Week pattern key" + weekPatternKey + " not found in week pattern map");
        }
        person.setCurrentWeekPattern(getWeekPatternMap().get(weekPatternKey));
    }

    /** {@inheritDoc} */
    @Override
    public LocationType getLocationTypeHouse()
    {
        return this.locationTypeIdMap.get((byte) 0);
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

}
