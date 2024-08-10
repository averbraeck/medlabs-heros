package eu.heros.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.djutils.exceptions.Throw;
import org.djutils.io.URLResource;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import eu.heros.disease.Covid19Progression;
import eu.heros.disease.Covid19TransmissionArea;
import eu.heros.disease.Covid19TransmissionDistance;
import eu.heros.model.HerosModel;
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
import eu.heros.policy.DiseasePolicy;
import eu.heros.policy.LocationPolicy;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import nl.tudelft.simulation.jstats.distributions.DistContinuous;
import nl.tudelft.simulation.jstats.distributions.DistTriangular;
import nl.tudelft.simulation.jstats.distributions.DistUniform;
import nl.tudelft.simulation.medlabs.activity.Activity;
import nl.tudelft.simulation.medlabs.activity.FixedDurationActivity;
import nl.tudelft.simulation.medlabs.activity.StochasticDurationActivity;
import nl.tudelft.simulation.medlabs.activity.TravelActivityBike;
import nl.tudelft.simulation.medlabs.activity.TravelActivityCar;
import nl.tudelft.simulation.medlabs.activity.TravelActivityDistanceBased;
import nl.tudelft.simulation.medlabs.activity.TravelActivityWalk;
import nl.tudelft.simulation.medlabs.activity.UntilFixedTimeActivity;
import nl.tudelft.simulation.medlabs.activity.locator.BikeLocator;
import nl.tudelft.simulation.medlabs.activity.locator.CarLocator;
import nl.tudelft.simulation.medlabs.activity.locator.CurrentLocator;
import nl.tudelft.simulation.medlabs.activity.locator.DistanceBasedTravelLocator;
import nl.tudelft.simulation.medlabs.activity.locator.HomeLocator;
import nl.tudelft.simulation.medlabs.activity.locator.LocatorInterface;
import nl.tudelft.simulation.medlabs.activity.locator.NearestLocator;
import nl.tudelft.simulation.medlabs.activity.locator.NearestLocatorCap;
import nl.tudelft.simulation.medlabs.activity.locator.NearestLocatorChoice;
import nl.tudelft.simulation.medlabs.activity.locator.NearestLocatorChoiceCap;
import nl.tudelft.simulation.medlabs.activity.locator.RandomLocator;
import nl.tudelft.simulation.medlabs.activity.locator.RandomLocatorCap;
import nl.tudelft.simulation.medlabs.activity.locator.RandomLocatorChoice;
import nl.tudelft.simulation.medlabs.activity.locator.RandomLocatorChoiceCap;
import nl.tudelft.simulation.medlabs.activity.locator.SchoolLocator;
import nl.tudelft.simulation.medlabs.activity.locator.WalkLocator;
import nl.tudelft.simulation.medlabs.activity.locator.WorkLocator;
import nl.tudelft.simulation.medlabs.activity.pattern.DayPattern;
import nl.tudelft.simulation.medlabs.activity.pattern.WeekDayPattern;
import nl.tudelft.simulation.medlabs.common.MedlabsException;
import nl.tudelft.simulation.medlabs.disease.DiseaseMonitor;
import nl.tudelft.simulation.medlabs.disease.DiseasePhase;
import nl.tudelft.simulation.medlabs.disease.DiseaseProgression;
import nl.tudelft.simulation.medlabs.disease.DiseaseTransmission;
import nl.tudelft.simulation.medlabs.excel.ExcelUtil;
import nl.tudelft.simulation.medlabs.location.Location;
import nl.tudelft.simulation.medlabs.location.LocationProbBased;
import nl.tudelft.simulation.medlabs.location.LocationType;
import nl.tudelft.simulation.medlabs.location.animation.LocationAnimation;
import nl.tudelft.simulation.medlabs.output.ResultWriter;
import nl.tudelft.simulation.medlabs.person.Person;
import nl.tudelft.simulation.medlabs.person.PersonMonitor;
import nl.tudelft.simulation.medlabs.person.PersonType;
import nl.tudelft.simulation.medlabs.person.index.IdxPerson;
import nl.tudelft.simulation.medlabs.simulation.TimeUnit;

/**
 * ConstructHerosModel.java.
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
public class ConstructHerosModel
{
    /** the model. */
    private final HerosModel model;

    /** temporary storage of the weekday patterns while building. */
    private Map<String, Map<String, DayPattern>> weekDayPattern = new HashMap<>();

    /** map to allocate households to the right sublocation. The map maps homeId via householdId to sublocationIndex. */
    private Map<Integer, Map<Integer, Short>> householdMap = new HashMap<>();

    /** map to temporarily store the probability-based infection locations. */
    private Map<Integer, double[]> probBasedInfectLoc = new HashMap<>();

    /**
     * Constructor of the model reader.
     * @param model the model
     */
    public ConstructHerosModel(final HerosModel model)
    {
        this.model = model;
        File file = getPathFromParam("generic.InputPath", true);
        model.setBasePath(file.getAbsolutePath());
        try
        {
            DiseaseProgression covidProgression = new Covid19Progression(this.model);
            DiseaseTransmission covidTransmission =
                    (this.model.getParameterValue("generic.diseasePropertiesModel").equals("area"))
                            ? new Covid19TransmissionArea(this.model) : new Covid19TransmissionDistance(this.model);
            readLocationTypeTable();
            makePersonTypes();
            readProbabilityBasedInfectionLocations();
            this.model.setDiseaseProgression(covidProgression);
            this.model.setDiseaseTransmission(covidTransmission);
            this.model.setDiseaseMonitor(new DiseaseMonitor(this.model, covidProgression, 0.5));
            this.model.setPersonMonitor(new PersonMonitor(this.model));
            readLocationTable();
            readWeekpatternData();
            checkBasicWeekPatterns();
            readPersonTable();
            makeFamilies();
            scheduleLocationPolicies();
            scheduleDiseasePolicies();
            infectPersons();
            makeResultWriter();
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
            System.exit(0);
        }
    }

    private File getPathFromParam(final String param, final boolean dir)
    {
        String paramValue = this.model.getParameterValue(param);
        URL baseURL = URLResource.getResource(paramValue);
        File file = null;
        if (baseURL != null)
        {
            file = new File(baseURL.getPath());
        }
        if (file == null || !file.exists())
        {
            file = new File(paramValue);
        }
        if (file == null || !file.exists())
        {
            file = new File(URLResource.getResource("/").getPath() + paramValue);
        }
        if (file == null || !file.exists())
        {
            System.err.println("could not find path as specified in parameter " + param + " with value: " + paramValue);
            System.exit(-1);
        }
        if (file.isDirectory() && !dir)
        {
            System.err.println(
                    "parameter " + param + " with value: " + paramValue + " should point to a file, but it is a directory");
            System.exit(-1);
        }
        if (!file.isDirectory() && dir)
        {
            System.err.println(
                    "parameter " + param + " with value: " + paramValue + " should point to a directory, but it is a file");
            System.exit(-1);
        }
        return file;
    }

    private File getFileFromParam(final String param, final String defaultFileName)
    {
        String paramValue = this.model.getParameterValue(param).trim();
        String basePath = this.model.getBasePath();
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
        File file = new File(basePath + "/" + defaultFileName);
        if (file != null && file.exists() && !file.isDirectory())
        {
            System.out.println("Used " + param + " = " + file.getAbsolutePath());
            return file.getAbsoluteFile();
        }
        throw new RuntimeException("could not find path as specified in parameter " + param + " with value: " + paramValue);
    }

    @SuppressWarnings("unchecked")
    private void readLocationTypeTable() throws Exception
    {
        byte id = 0;
        // House or Accommodation should be #0, the first in the list (see HerosModel, method getLocationTypeHouse.
        File path = getFileFromParam("generic.LocationTypesFilePath", "/locationtypes.csv");
        Reader reader = new InputStreamReader(new FileInputStream(path));
        CsvReader csvReader = CsvReader.builder().fieldSeparator(',').quoteCharacter('"').build(reader);
        CsvRow row;
        List<String> data;
        Iterator<CsvRow> it = csvReader.iterator();
        if (it.hasNext())
        {
            row = it.next(); // test header for capacity constraints
            data = row.getFields();
            int capConstrainedIndex = data.indexOf("capConstrained");
            int capIndex = data.indexOf("capPersonsPerM2");
            int sizeFactorIndex = data.indexOf("sizeFactor");
            while (it.hasNext())
            {
                row = it.next();
                data = row.getFields();
                String ltName = data.get(0);
                String ltAniClassName = data.get(1);
                boolean reproducible = data.get(2).toLowerCase().equals("true");
                boolean infectSub = data.get(3).toLowerCase().equals("true");
                double contagiousRateFactor = Double.parseDouble(data.get(5));
                // Class<? extends Location> ltClass = (Class<? extends Location>)
                // Class.forName(ltClassName);
                Class<? extends LocationAnimation> ltAniClass =
                        (Class<? extends LocationAnimation>) Class.forName(ltAniClassName);
                double cap = capIndex < 0 ? 0.5 : Double.parseDouble(data.get(capIndex));
                boolean capConstrained =
                        capConstrainedIndex < 0 ? false : data.get(capConstrainedIndex).toLowerCase().equals("true");
                double sizeFactor = sizeFactorIndex < 0 ? 1.0 : Double.parseDouble(data.get(sizeFactorIndex));
                new LocationType(this.model, id, ltName, Location.class, ltAniClass, reproducible, infectSub,
                        contagiousRateFactor, capConstrained, cap, sizeFactor);
                id++;
            }
        }
    }

    /**
     * Read the probability-based infection locations.
     */
    private void readProbabilityBasedInfectionLocations() throws Exception
    {
        File path = getFileFromParam("generic.ProbRatioFilePath", "/infection_rates.csv");
        Reader reader = new InputStreamReader(new FileInputStream(path));
        CsvReader csvReader = CsvReader.builder().fieldSeparator(',').quoteCharacter('"').build(reader);
        CsvRow row;
        List<String> data;
        Iterator<CsvRow> it = csvReader.iterator();
        if (it.hasNext())
        {
            row = it.next(); // skip header
            while (it.hasNext())
            {
                row = it.next();
                data = row.getFields();
                int locationId = Integer.parseInt(data.get(0));
                double infectionRateFactor = Double.parseDouble(data.get(1));
                double infectionRate = Double.parseDouble(data.get(2));
                this.probBasedInfectLoc.put(locationId, new double[] {infectionRateFactor, infectionRate});
            }
        }
    }

    /**
     * @throws Exception
     */
    private void readLocationTable() throws Exception
    {
        // reference groups for satellite workers
        Map<PersonType, PersonType> referenceGroupMap = new HashMap<>();
        PersonType workerPT = this.model.getPersonTypeClassMap().get(Worker.class);
        referenceGroupMap.put(this.model.getPersonTypeClassMap().get(WorkerCityToSatellite.class), workerPT);
        referenceGroupMap.put(this.model.getPersonTypeClassMap().get(WorkerSatelliteToCity.class), workerPT);
        referenceGroupMap.put(this.model.getPersonTypeClassMap().get(WorkerSatelliteToSatellite.class), workerPT);
        referenceGroupMap.put(this.model.getPersonTypeClassMap().get(WorkerCountryToCity.class), workerPT);

        File path = getFileFromParam("generic.LocationsFilePath", "locations.csv.gz");
        Reader reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(path)));
        CsvReader csvReader = CsvReader.builder().fieldSeparator(',').quoteCharacter('"').build(reader);
        CsvRow row;
        List<String> header;
        List<String> data;
        Iterator<CsvRow> it = csvReader.iterator();
        if (it.hasNext())
        {
            row = it.next();
            Throw.when(row == null, IOException.class, "file does not contain header row");
            header = row.getFields();
            int iLocationId = 0;
            int iNbSublocations = 0;
            int iLon = 0;
            int iLat = 0;
            int iArea = 0;
            int iLocationCategory = 0;
            int found = 0;
            for (int i = 0; i < header.size(); i++)
            {
                found++;
                if (header.get(i).toLowerCase().equals("location_id"))
                    iLocationId = i;
                else if (header.get(i).toLowerCase().equals("nb_sublocations"))
                    iNbSublocations = i;
                else if (header.get(i).toLowerCase().equals("location_category"))
                    iLocationCategory = i;
                else if (header.get(i).toLowerCase().equals("lat"))
                    iLat = i;
                else if (header.get(i).toLowerCase().equals("lon"))
                    iLon = i;
                else if (header.get(i).toLowerCase().equals("area"))
                    iArea = i;
                else
                    found--;
            }
            if (found != 6)
                throw new MedlabsException("Location csv-file header row did not contain all column headers");

            // Read the data file records
            while (it.hasNext())
            {
                row = it.next();
                data = row.getFields();
                int locationId = Integer.parseInt(data.get(iLocationId));
                float lon = Float.parseFloat(data.get(iLon));
                float lat = Float.parseFloat(data.get(iLat));
                short nbSublocations = (short) Integer.parseInt(data.get(iNbSublocations));
                float subArea = Float.parseFloat(data.get(iArea));
                String locationCategory = data.get(iLocationCategory);
                if (locationCategory.trim().length() == 0 || locationCategory.contains("/"))
                {
                    System.err.println("\nIllegal location category (skipped) -- line " + row.getOriginalLineNumber());
                    System.err.println("Row: " + row.toString());
                    continue;
                }
                LocationType locationType = this.model.getLocationTypeNameMap().get(locationCategory);
                if (locationType == null)
                {
                    for (byte b = (byte) 0; b < 128; b++)
                    {
                        if (!this.model.getLocationTypeIndexMap().containsKey(b))
                        {
                            System.err.println("Warning: LocationType added - " + locationCategory);
                            locationType = new LocationType(this.model, b, locationCategory, Location.class, null, false, true,
                                    1.0, false, 0.25, 1.0);
                            break;
                        }
                    }
                }
                float area = subArea * nbSublocations;
                if (this.probBasedInfectLoc.containsKey(locationId))
                {
                    double infectionRateFactor = this.probBasedInfectLoc.get(locationId)[0];
                    double infectionRate = this.probBasedInfectLoc.get(locationId)[1];
                    new LocationProbBased(this.model, locationId, locationType, lat, lon, nbSublocations, area,
                            infectionRateFactor, infectionRate, referenceGroupMap, Covid19Progression.exposed);
                }
                else
                {
                    new Location(this.model, locationId, locationType, lat, lon, nbSublocations, area);
                }
            }
        }
    }

    private void checkBasicWeekPatterns() throws MedlabsException
    {
        boolean err = false;
        for (String policy : new String[] {"0"}) // {"0", "1", "2"})
        {
            DiseaseProgression disease = this.model.getDiseaseProgression();
            for (DiseasePhase diseasePhase : disease.getDiseasePhases())
            {
                if (diseasePhase.getName() != "Dead")
                {
                    for (String personType : new String[] {"infant", "kindergarten student", "primary school student",
                            "secondary school student", "college student", "university student", "worker", "pensioner",
                            "unemployed job-seeker", "weekend worker", "essential worker", "worker satellite to city",
                            "worker city to satellite", "worker satellite to satellite", "worker country to city"})
                    {
                        if (this.model.getWeekPatternMap()
                                .get(policy + "_" + diseasePhase.getName() + "_" + personType) == null)
                        {
                            System.err.println(
                                    "Week pattern " + policy + "_" + diseasePhase.getName() + "_" + personType + " not found");
                            err = true;
                        }
                    }
                }
            }
        }
        if (err)
            throw new MedlabsException("One of the week patterns is missing");
    }

    /**
     * make the PersonTypes and register in the Model.
     */
    @SuppressWarnings("unchecked")
    private void makePersonTypes()
    {
        int nr = 1;
        for (Class<? extends Person> pc : new Class[] {Infant.class, KindergartenStudent.class, PrimarySchoolStudent.class,
                SecondarySchoolStudent.class, CollegeStudent.class, UniversityStudent.class, Worker.class, Pensioner.class,
                Unemployed.class, WeekendWorker.class, EssentialWorker.class, WorkerSatelliteToCity.class,
                WorkerCityToSatellite.class, WorkerSatelliteToSatellite.class, WorkerCountryToCity.class})
        {
            PersonType pt = new PersonType(this.model, nr, pc);
            this.model.getPersonTypeList().add(pt);
            this.model.getPersonTypeClassMap().put(pc, pt);
            nr++;
        }
    }

    /**
     * @param maxNumberOfPersons
     * @throws Exception
     */
    private void readPersonTable() throws Exception
    {
        File path = getFileFromParam("generic.PersonFilePath", "people.csv.gz");
        Reader reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(path)));
        CsvReader csvReader = CsvReader.builder().fieldSeparator(',').quoteCharacter('"').build(reader);
        CsvRow row;
        List<String> header;
        List<String> data;
        Iterator<CsvRow> it = csvReader.iterator();
        if (it.hasNext())
        {
            row = it.next();
            Throw.when(row == null, IOException.class, "file does not contain header row");
            header = row.getFields();

            int iPersonId = 0;
            int iHouseholdId = 0;
            int iAge = 0;
            int iHomeId = 0;
            int iWorkplaceId = 0;
            int iSocialRole = 0;

            int found = 0;
            for (int i = 0; i < header.size(); i++)
            {
                found++;
                if (header.get(i).toLowerCase().equals("person_id"))
                    iPersonId = i;
                else if (header.get(i).toLowerCase().equals("household_id"))
                    iHouseholdId = i;
                else if (header.get(i).toLowerCase().equals("age"))
                    iAge = i;
                else if (header.get(i).toLowerCase().equals("home_id"))
                    iHomeId = i;
                else if (header.get(i).toLowerCase().equals("workplace_id"))
                    iWorkplaceId = i;
                else if (header.get(i).toLowerCase().equals("social_role"))
                    iSocialRole = i;
                else
                    found--;
            }
            if (found != 6)
                throw new MedlabsException(
                        "Person csv-file header row did not contain all column headers\n" + header.toString());

            // Read the data file records
            while (it.hasNext())
            {
                row = it.next();
                data = row.getFields();
                int personId = (int) Double.parseDouble(data.get(iPersonId));
                int householdId = (int) Double.parseDouble(data.get(iHouseholdId));
                byte age = (byte) (int) Double.parseDouble(data.get(iAge));
                int homeId = (int) Double.parseDouble(data.get(iHomeId));
                int workSchoolId = data.get(iWorkplaceId) == "" ? -1 : (int) Double.parseDouble(data.get(iWorkplaceId));
                int socialRole = (int) Double.parseDouble(data.get(iSocialRole));

                if (age < 0 || age > 120)
                {
                    System.err.println("Person " + personId + " has age " + age + " on row " + row.getOriginalLineNumber()
                            + "\n" + row.toString());
                }

                boolean genderFemale = this.model.getU01().draw() < 0.5;

                // check homeId
                if (!this.model.getLocationMap().containsKey(homeId))
                {
                    System.err.println("homeId " + homeId + " not found in the location map on line "
                            + row.getOriginalLineNumber() + "\n" + row.toString());
                    continue;
                }
                // if (this.model.getLocationMap().get(homeId).getLocationTypeId() != this.model.getLocationTypeHouse()
                // .getLocationTypeId())
                // {
                // System.err.println("homeId " + homeId + " not an Accommodation in the location map on line "
                // + row.getOriginalLineNumber() + "\n" + row.toString());
                // continue;
                // }

                // create sublocationIndex for the home
                short homeSubLocationIndex;
                Map<Integer, Short> householdSublocationMap = this.householdMap.get(homeId);
                if (householdSublocationMap == null)
                {
                    householdSublocationMap = new HashMap<>();
                    this.householdMap.put(homeId, householdSublocationMap);
                }
                if (householdSublocationMap.containsKey(householdId))
                {
                    homeSubLocationIndex = householdSublocationMap.get(householdId);
                }
                else
                {
                    homeSubLocationIndex = (short) householdSublocationMap.size();
                    if (homeSubLocationIndex + 1 > this.model.getLocationMap().get(homeId).getNumberOfSubLocations())
                    {
                        System.err.println("Person " + personId + ". The homeId " + homeId + " with householdId " + householdId
                                + " has more sublocations (" + (homeSubLocationIndex + 1) + ") than defined. Record"
                                + " on row " + row.getOriginalLineNumber() + "\n" + row.toString());
                    }
                    householdSublocationMap.put(householdId, homeSubLocationIndex);
                }

                if (socialRole >= 2 && socialRole <= 6)
                {
                    if (workSchoolId == -1 || !this.model.getLocationMap().containsKey(workSchoolId))
                    {
                        System.err.println("No school location [" + workSchoolId + "] for Student on line "
                                + row.getOriginalLineNumber() + "\n" + row.toString());
                        continue;
                    }
                }

                IdxPerson person;
                switch (socialRole)
                {
                    case 1:
                        person = new Infant(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_infant").getId());
                        break;

                    case 2:
                        if (!this.model.getLocationMap().get(workSchoolId).getLocationType().getName().toLowerCase()
                                .equals("kindergarten"))
                        {
                            System.err
                                    .println("workSchoolId " + workSchoolId + " not a Kindergarten in the location map on line "
                                            + row.getOriginalLineNumber() + "\n" + row.toString());
                            continue;
                        }
                        person = new KindergartenStudent(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_kindergarten student").getId(),
                                workSchoolId);
                        break;

                    case 3:
                        if (!this.model.getLocationMap().get(workSchoolId).getLocationType().getName().toLowerCase()
                                .equals("primaryschool"))
                        {
                            System.err.println(
                                    "workSchoolId " + workSchoolId + " not a primary school in the location map on line "
                                            + row.getOriginalLineNumber() + "\n" + row.toString());
                            continue;
                        }
                        person = new PrimarySchoolStudent(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_primary school student").getId(),
                                workSchoolId);
                        break;

                    case 4:
                        if (!this.model.getLocationMap().get(workSchoolId).getLocationType().getName().toLowerCase()
                                .equals("secondaryschool"))
                        {
                            System.err.println(
                                    "workSchoolId " + workSchoolId + " not a secondary school in the location map on line "
                                            + row.getOriginalLineNumber() + "\n" + row.toString());
                            continue;
                        }
                        person = new SecondarySchoolStudent(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_secondary school student").getId(),
                                workSchoolId);
                        break;

                    case 5:
                        if (!this.model.getLocationMap().get(workSchoolId).getLocationType().getName().toLowerCase()
                                .equals("college"))
                        {
                            System.err.println("workSchoolId " + workSchoolId + " not a College in the location map on line "
                                    + row.getOriginalLineNumber() + "\n" + row.toString());
                            continue;
                        }
                        person = new CollegeStudent(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_college student").getId(),
                                workSchoolId);
                        break;

                    case 6:
                        if (!this.model.getLocationMap().get(workSchoolId).getLocationType().getName().toLowerCase()
                                .equals("university"))
                        {
                            System.err.println("workSchoolId " + workSchoolId + " not a University in the location map on line "
                                    + row.getOriginalLineNumber() + "\n" + row.toString());
                            continue;
                        }
                        person = new UniversityStudent(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_university student").getId(),
                                workSchoolId);
                        break;
                    case 7:
                        if (workSchoolId == -1 || !this.model.getLocationMap().containsKey(workSchoolId))
                        {
                            System.err.println("No work location [" + workSchoolId + "] for Worker on line "
                                    + row.getOriginalLineNumber() + "\n" + row.toString());
                            continue;
                        }
                        // note: work location can be anything: school, retail, office, park, ...
                        person = new Worker(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_worker").getId(), workSchoolId);
                        break;

                    case 8:
                        person = new Pensioner(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_pensioner").getId());
                        break;

                    case 9:
                        person = new Unemployed(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_unemployed job-seeker").getId());
                        break;

                    case 10:
                        if (workSchoolId == -1 || !this.model.getLocationMap().containsKey(workSchoolId))
                        {
                            System.err.println("No work location [" + workSchoolId + "] for WeekendWorker on line "
                                    + row.getOriginalLineNumber() + "\n" + row.toString());
                            continue;
                        }
                        // note: work location can be anything: school, retail, office, park, ...
                        person = new WeekendWorker(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_weekend worker").getId(),
                                workSchoolId);
                        break;

                    case 11:
                        if (workSchoolId == -1 || !this.model.getLocationMap().containsKey(workSchoolId))
                        {
                            System.err.println("No work location [" + workSchoolId + "] for EssentialWorker on line "
                                    + row.getOriginalLineNumber() + "\n" + row.toString());
                            continue;
                        }
                        // note: work location can be anything: school, retail, office, park, ...
                        person = new EssentialWorker(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_essential worker").getId(),
                                workSchoolId);
                        break;

                    case 12:
                        if (workSchoolId == -1 || !this.model.getLocationMap().containsKey(workSchoolId))
                        {
                            System.err.println("No work location [" + workSchoolId + "] for WorkerSatelliteToCity on line "
                                    + row.getOriginalLineNumber() + "\n" + row.toString());
                            continue;
                        }
                        // note: work location can be anything: school, retail, office, park, ...
                        person = new WorkerSatelliteToCity(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_worker satellite to city").getId(),
                                workSchoolId);
                        break;

                    case 13:
                        if (workSchoolId == -1 || !this.model.getLocationMap().containsKey(workSchoolId))
                        {
                            System.err.println("No work location [" + workSchoolId + "] for WorkerCityToSatellite on line "
                                    + row.getOriginalLineNumber() + "\n" + row.toString());
                            continue;
                        }
                        // note: work location can be anything: school, retail, office, park, ...
                        person = new WorkerCityToSatellite(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_worker city to satellite").getId(),
                                workSchoolId);
                        break;

                    case 14:
                        if (workSchoolId == -1 || !this.model.getLocationMap().containsKey(workSchoolId))
                        {
                            System.err.println("No work location [" + workSchoolId + "] for WorkerSatelliteToSatellite on line "
                                    + row.getOriginalLineNumber() + "\n" + row.toString());
                            continue;
                        }
                        // note: work location can be anything: school, retail, office, park, ...
                        person = new WorkerSatelliteToSatellite(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_worker satellite to satellite")
                                        .getId(),
                                workSchoolId);
                        break;

                    case 15:
                        if (workSchoolId == -1 || !this.model.getLocationMap().containsKey(workSchoolId))
                        {
                            System.err.println("No work location [" + workSchoolId + "] for WorkerCountryToCity on line "
                                    + row.getOriginalLineNumber() + "\n" + row.toString());
                            continue;
                        }
                        // note: work location can be anything: school, retail, office, park, ...
                        person = new WorkerCountryToCity(this.model, personId, genderFemale, age, homeId,
                                (short) this.model.getWeekPatternMap().get("0_Susceptible_worker country to city").getId(),
                                workSchoolId);
                        break;

                    default:
                        throw new MedlabsException(
                                "social role " + socialRole + " no recognized on row " + row.getOriginalLineNumber());
                }
                person.setHomeSubLocationIndex(homeSubLocationIndex);
                person.setExposureTime(0.0f);
                person.setDiseasePhase(Covid19Progression.susceptible);
                Covid19Progression.susceptible.addPerson();
            }
        }

        // Write how many we have per person type
        System.out.println("\nNumber of persons per type:");
        for (PersonType pt : this.model.getPersonTypeList())
        {
            System.out.println(pt.getName() + ": " + pt.getNumberPersons());
        }
        System.out.println();
    }

    /**
     * @throws Exception
     */
    private void readWeekpatternData() throws Exception
    {
        File path = getFileFromParam("generic.ActivityFilePath", "activityschedules.xlsx");
        InputStream fis = URLResource.getResourceAsStream(path.getAbsolutePath());
        XSSFWorkbook wbST = new XSSFWorkbook(fis);
        XSSFSheet sheet = wbST.getSheet("activityschedules");

        for (Row row : sheet)
        {
            if (row.getRowNum() > 0)
            {
                int policy = (int) ExcelUtil.cellValueDoubleNull(row, "A");
                String epidemiologicalState = ExcelUtil.cellValue(row, "B");
                String dayOfWeek = ExcelUtil.cellValue(row, "C");
                String socialRole = ExcelUtil.cellValue(row, "D");
                String activityString = ExcelUtil.cellValue(row, "E");
                String activityType = ExcelUtil.cellValue(row, "F");
                double untilHour = ExcelUtil.cellValueDoubleNull(row, "G");
                String distributionString = ExcelUtil.cellValue(row, "H").trim();
                double mode = ExcelUtil.cellValueDoubleNull(row, "I");
                double min = ExcelUtil.cellValueDoubleNull(row, "J");
                double max = ExcelUtil.cellValueDoubleNull(row, "K");
                // double std = ExcelUtil.cellValueDoubleNull(row, "L");
                String activityLocator = ExcelUtil.cellValue(row, "M");
                String fromLocator = ExcelUtil.cellValue(row, "N");
                String toLocator = ExcelUtil.cellValue(row, "O");
                double maxDistance = ExcelUtil.cellValueDoubleNull(row, "P");
                String locationType = ExcelUtil.cellValue(row, "Q");

                String weekPatternKey = policy + "_" + epidemiologicalState + "_" + socialRole;
                Map<String, DayPattern> dayActivitiesMap = this.weekDayPattern.get(weekPatternKey);
                if (dayActivitiesMap == null)
                {
                    dayActivitiesMap = new HashMap<>();
                    this.weekDayPattern.put(weekPatternKey, dayActivitiesMap);
                }
                DayPattern dayPattern = dayActivitiesMap.get(dayOfWeek);
                if (dayPattern == null)
                {
                    List<Activity> activityList = new ArrayList<>();
                    dayPattern = new DayPattern(this.model, activityList);
                    dayActivitiesMap.put(dayOfWeek, dayPattern);
                }

                Activity activity = null;
                DistContinuous durationDistribution = null;
                double estimatedDuration = 0;
                if (distributionString != "")
                {
                    switch (distributionString)
                    {
                        case "triangular":
                            if (min > mode)
                                throw new MedlabsException(
                                        "triangular distribution with min > mode in activity xlsx file at row "
                                                + (row.getRowNum() + 1));
                            if (max < mode)
                                throw new MedlabsException(
                                        "triangular distribution with max < mode in activity xlsx file at row "
                                                + (row.getRowNum() + 1));
                            durationDistribution = new DistTriangular(this.model.getRandomStream(), min, mode, max);
                            estimatedDuration = (min + mode + max) / 3.0;
                            break;
                        case "uniform":
                            if (min > max)
                                throw new MedlabsException("uniform distribution with min > max in activity xlsx file at row "
                                        + (row.getRowNum() + 1));
                            durationDistribution = new DistUniform(this.model.getRandomStream(), min, max);
                            estimatedDuration = (min + max) / 2.0;
                            break;
                        default:
                            throw new MedlabsException("unknown distribution in activity xlsx file: " + distributionString
                                    + " at row " + (row.getRowNum() + 1));
                    }
                }

                switch (activityType)
                {
                    case "FixedDurationActivity":
                        activity = new FixedDurationActivity(this.model, activityString,
                                makeLocator(activityLocator, locationType, maxDistance), mode);
                        break;

                    case "StochasticDurationActivity":
                        if (durationDistribution == null)
                        {
                            throw new MedlabsException("missing duration distribution for stochastic activity"
                                    + " in activity xlsx file at row " + (row.getRowNum() + 1));
                        }
                        activity = new StochasticDurationActivity(this.model, activityString,
                                makeLocator(activityLocator, locationType, maxDistance), estimatedDuration,
                                durationDistribution, TimeUnit.HOUR);
                        break;

                    case "TravelActivity":
                    case "TravelActivityDistanceBased":
                        activity = new TravelActivityDistanceBased(this.model, activityString, makeLocator(activityLocator),
                                makeLocator(fromLocator), makeLocator(toLocator, locationType, maxDistance));
                        break;

                    case "TravelActivityWalk":
                        activity = new TravelActivityWalk(this.model, activityString, makeLocator(activityLocator),
                                makeLocator(fromLocator), makeLocator(toLocator, locationType, maxDistance));
                        break;

                    case "TravelActivityBike":
                        activity = new TravelActivityBike(this.model, activityString, makeLocator(activityLocator),
                                makeLocator(fromLocator), makeLocator(toLocator, locationType, maxDistance));
                        break;

                    case "TravelActivityCar":
                        activity = new TravelActivityCar(this.model, activityString, makeLocator(activityLocator),
                                makeLocator(fromLocator), makeLocator(toLocator, locationType, maxDistance));
                        break;

                    case "UntilFixedTimeActivity":
                        activity = new UntilFixedTimeActivity(this.model, activityString,
                                makeLocator(activityLocator, locationType, maxDistance), untilHour);
                        break;

                    default:
                        throw new MedlabsException("unknown activityType in activity xlsx file: " + activityType);
                }

                dayPattern.getActivities().add(activity);
            }
        }

        // for (String weekPatternKey : this.weekDayPattern.keySet())
        // {
        // Map<String, DayPattern> dayActivitiesMap = this.weekDayPattern.get(weekPatternKey);
        // if (!dayActivitiesMap.containsKey("Tuesday"))
        // System.err.println("Tuesday missing for week pattern " + weekPatternKey);
        // if (!dayActivitiesMap.containsKey("Friday"))
        // System.err.println("Friday missing for week pattern " + weekPatternKey);
        // if (!dayActivitiesMap.containsKey("Saturday"))
        // System.err.println("Saturday missing for week pattern " + weekPatternKey);
        // if (!dayActivitiesMap.containsKey("Sunday"))
        // System.err.println("Sunday missing for week pattern " + weekPatternKey);
        // DayPattern[] dayPatterns = new DayPattern[7];
        //
        // dayPatterns[0] = dayActivitiesMap.get("Tuesday");
        //
        // dayPatterns[0] = dayActivitiesMap.get("Tuesday");
        //
        // dayPatterns[1] = dayPatterns[0];
        // dayPatterns[2] = dayPatterns[0];
        // dayPatterns[3] = dayPatterns[0];
        // dayPatterns[4] = dayActivitiesMap.get("Friday") == null ? dayPatterns[0] : dayActivitiesMap.get("Friday");
        // dayPatterns[5] = dayActivitiesMap.get("Saturday") == null ? dayPatterns[0] : dayActivitiesMap.get("Saturday");
        // dayPatterns[6] = dayActivitiesMap.get("Sunday") == null ? dayPatterns[0] : dayActivitiesMap.get("Sunday");
        // new WeekDayPattern(this.model, weekPatternKey, dayPatterns);
        // // patterns are stored automatically in the maps of the model
        // }

        // 1. Change Tuesday to Monday.
        // 2. Remove duplicated assignment of dayPatterns[0]
        for (String weekPatternKey : this.weekDayPattern.keySet())
        {
            Map<String, DayPattern> dayActivitiesMap = this.weekDayPattern.get(weekPatternKey);
            if (!dayActivitiesMap.containsKey("Monday"))
                System.err.println("Monday missing for week pattern " + weekPatternKey);
            // if (!dayActivitiesMap.containsKey("Friday"))
            // System.err.println("Friday missing for week pattern " + weekPatternKey);
            // if (!dayActivitiesMap.containsKey("Saturday"))
            // System.err.println("Saturday missing for week pattern " + weekPatternKey);
            // if (!dayActivitiesMap.containsKey("Sunday"))
            // System.err.println("Sunday missing for week pattern " + weekPatternKey);
            DayPattern[] dayPatterns = new DayPattern[7];

            dayPatterns[0] = dayActivitiesMap.get("Monday");
            dayPatterns[1] = dayPatterns[0];
            dayPatterns[2] = dayPatterns[0];
            dayPatterns[3] = dayPatterns[0];
            dayPatterns[4] = dayActivitiesMap.get("Friday") == null ? dayPatterns[0] : dayActivitiesMap.get("Friday");
            dayPatterns[5] = dayActivitiesMap.get("Saturday") == null ? dayPatterns[0] : dayActivitiesMap.get("Saturday");
            dayPatterns[6] = dayActivitiesMap.get("Sunday") == null ? dayPatterns[0] : dayActivitiesMap.get("Sunday");
            new WeekDayPattern(this.model, weekPatternKey, dayPatterns);
        }
    }

    private LocatorInterface makeLocator(final String locatorString) throws MedlabsException
    {
        switch (locatorString)
        {
            case "HomeLocator":
                return new HomeLocator();
            case "CurrentLocator":
                return new CurrentLocator();
            case "SchoolLocator":
                return new SchoolLocator();
            case "WorkLocator":
                return new WorkLocator();
            case "WalkLocator":
                return new WalkLocator();
            case "BikeLocator":
                return new BikeLocator();
            case "CarLocator":
                return new CarLocator();
            case "DistanceBasedTravelLocator":
                return new DistanceBasedTravelLocator();

            default:
                throw new MedlabsException("unknown locatorString in activity xlsx file: " + locatorString);
        }
    }

    private LocatorInterface makeLocator(final String locatorString, final String locationType, final double maxDistance)
            throws MedlabsException
    {
        switch (locatorString)
        {
            case "HomeLocator":
                return new HomeLocator();
            case "CurrentLocator":
                return new CurrentLocator();
            case "SchoolLocator":
                return new SchoolLocator();
            case "WorkLocator":
                return new WorkLocator();
            case "WalkLocator":
                return new WalkLocator();
            case "BikeLocator":
                return new BikeLocator();
            case "CarLocator":
                return new CarLocator();
            case "DistanceBasedTravelLocator":
                return new DistanceBasedTravelLocator();
            case "NearestLocator":
            case "NearestLocatorChoice":
                if (locationType.contains(":"))
                {
                    return new NearestLocatorChoice(new CurrentLocator(), resolveLocationTypeChoice(locationType), false);
                }
                return new NearestLocator(new CurrentLocator(), resolveLocationType(locationType));
            case "NearestLocatorCap":
            case "NearestLocatorChoiceCap":
                if (locationType.contains(":"))
                {
                    return new NearestLocatorChoiceCap(new CurrentLocator(), resolveLocationTypeChoice(locationType), false);
                }
                return new NearestLocatorCap(new CurrentLocator(), resolveLocationType(locationType));
            case "RandomLocator":
            case "RandomLocatorChoice":
                if (locationType.contains(":"))
                {
                    return new RandomLocatorChoice(new CurrentLocator(), resolveLocationTypeChoice(locationType), maxDistance,
                            false);
                }
                return new RandomLocator(new CurrentLocator(), resolveLocationType(locationType), maxDistance, false);
            case "RandomLocatorCap":
            case "RandomLocatorChoiceCap":
                if (locationType.contains(":"))
                {
                    return new RandomLocatorChoiceCap(new CurrentLocator(), resolveLocationTypeChoice(locationType),
                            maxDistance, false);
                }
                return new RandomLocatorCap(new CurrentLocator(), resolveLocationType(locationType), maxDistance, false);

            default:
                throw new MedlabsException("unknown locatorString in activity xlsx file: " + locatorString);
        }
    }

    private LocationType resolveLocationType(final String lt) throws MedlabsException
    {
        String ltClean = lt.trim().replace("LocationType.", "");
        LocationType locationType = this.model.getLocationTypeNameMap().get(ltClean);
        if (locationType == null)
        {
            throw new MedlabsException("Parsing activities. Location type " + ltClean + " not found");
        }
        return locationType;
    }

    private SortedMap<Double, LocationType> resolveLocationTypeChoice(final String types) throws MedlabsException
    {
        String[] entries = types.split(";");
        SortedMap<Double, LocationType> map = new TreeMap<>();
        double cumulativeProb = 0.0;
        for (String entry : entries)
        {
            if (entry.trim().length() == 0)
                continue;
            String[] probLoc = entry.split(":");
            if (probLoc.length != 2)
                throw new MedlabsException("Error in resolveLocationTypeChoice for types " + types);
            double prob = Double.parseDouble(probLoc[1].trim());
            if (prob == 0.0)
                continue;
            Throw.when(prob < 0.0, MedlabsException.class, "probability <= 0 in resolveLocationTypeChoice for types " + types);
            String lt = probLoc[0].trim().replace("LocationType.", "");
            cumulativeProb += prob;
            Throw.when(cumulativeProb > 1.0001, MedlabsException.class,
                    "Sum of probabilities > 1 in resolveLocationTypeChoice for types " + types);
            LocationType locationType = resolveLocationType(lt);
            map.put(cumulativeProb, locationType);
        }

        // make sure the last entry has a probability of exact 1.0, give an error if
        // it's more than 0.0001 away
        double lastProb = map.lastKey();
        LocationType lastLT = map.get(lastProb);
        Throw.when(Math.abs(lastProb - 1.0) > 0.0001, MedlabsException.class,
                "Sum of probabilities is not equal to 1 in resolveLocationTypeChoice for types " + types);
        map.remove(lastProb);
        map.put(1.0, lastLT);

        return map;
    }

    private void makeFamilies()
    {
        TIntObjectMap<TIntSet> families = this.model.getFamilyMembersByHomeLocation();
        for (TIntObjectIterator<Person> it = this.model.getPersonMap().iterator(); it.hasNext();)
        {
            it.advance();
            Person person = it.value();
            TIntSet family = families.get(person.getHomeLocation().getId());
            if (family == null)
            {
                family = new TIntHashSet();
                families.put(person.getHomeLocation().getId(), family);
            }
            family.add(person.getId());
        }

        for (int homeLocationId : families.keys())
        {
            Location homeLocation = this.model.getLocationMap().get(homeLocationId);
            int sub = homeLocation.getNumberOfSubLocations();
            TIntSet family = families.get(homeLocationId);
            if (family.size() == 0)
                System.out.println("Home Location " + homeLocation.getId() + ": family size 0");
            else if (family.size() < sub)
                System.out
                        .println("Home Location " + homeLocation.getId() + ": family size " + family.size() + ", sub = " + sub);
            else if (family.size() / sub > 7)
                System.out.println("Home Location " + homeLocation.getId() + ": family size > 7: " + family.size() / sub);

        }
    }

    /**
     * Infections based on the inputParameterMap.
     */
    private void infectPersons()
    {
        int numberToInfect = this.model.getParameterValueInt("policies.NumberInfected");
        int ageMin = this.model.getParameterValueInt("policies.MinAgeInfected");
        int ageMax = this.model.getParameterValueInt("policies.MaxAgeInfected");
        List<Person> persons = new ArrayList<>(this.model.getPersonMap().valueCollection());
        int nrPersons = persons.size() - 1;
        while (numberToInfect > 0)
        {
            Person person = persons.get(this.model.getRandomStream().nextInt(0, nrPersons));
            if (person.getDiseasePhase().isSusceptible())
            {
                if (person.getAge() >= ageMin && person.getAge() <= ageMax)
                {
                    this.model.getDiseaseProgression().expose(person, Covid19Progression.exposed);
                    numberToInfect--;
                }
            }
        }
    }

    /**
     * Create the ResultWriter to write the output files.
     */
    private void makeResultWriter()
    {
        if (this.model.getParameterValueBoolean("generic.WriteOutput"))
        {
            String outputPath = this.model.getParameterValue("generic.OutputPath");
            new ResultWriter(this.model, outputPath);
        }
    }

    /**
     * Read and schedule the location policies.
     */
    private void scheduleLocationPolicies() throws Exception
    {
        if (this.model.getParameterValue("policies.LocationPolicyFile").trim().length() == 0)
            return;
        File path = getFileFromParam("policies.LocationPolicyFile", "");
        Reader reader = new InputStreamReader(new FileInputStream(path));
        CsvReader csvReader = CsvReader.builder().fieldSeparator(',').quoteCharacter('"').build(reader);
        CsvRow row;
        List<String> data;
        Iterator<CsvRow> it = csvReader.iterator();
        if (it.hasNext())
        {
            row = it.next(); // skip header
            while (it.hasNext())
            {
                // Time(d),LocationType,FractionOpen,FractionActivities,AlternativeLocation,ReportAsLocation
                row = it.next();
                data = row.getFields();
                double time = 24.0 * Double.parseDouble(data.get(0));
                String locationTypeName = data.get(1);
                double fractionOpen = Double.parseDouble(data.get(2));
                double fractionActivities = Double.parseDouble(data.get(3));
                String alternativeLocationName = data.get(4);
                String reportAsLocationName = data.get(5);
                new LocationPolicy(this.model, time, locationTypeName, fractionOpen, fractionActivities,
                        alternativeLocationName, reportAsLocationName);
            }
        }
        csvReader.close();
    }

    /**
     * Read and schedule the disease policies.
     */
    private void scheduleDiseasePolicies() throws Exception
    {
        if (this.model.getParameterValue("policies.DiseasePolicyFile").trim().length() == 0)
            return;
        File path = getFileFromParam("policies.DiseasePolicyFile", "");
        Reader reader = new InputStreamReader(new FileInputStream(path));
        CsvReader csvReader = CsvReader.builder().fieldSeparator(',').quoteCharacter('"').build(reader);
        CsvRow row;
        List<String> data;
        Iterator<CsvRow> it = csvReader.iterator();
        if (it.hasNext())
        {
            row = it.next(); // skip header
            while (it.hasNext())
            {
                // Time(d),Parameter,Value
                row = it.next();
                data = row.getFields();
                double time = 24.0 * Double.parseDouble(data.get(0));
                String parameterName = data.get(1);
                double value = Double.parseDouble(data.get(2));
                new DiseasePolicy(this.model, time, parameterName, value);
            }
        }
        csvReader.close();
    }

}
