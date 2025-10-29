# medlabs-heros

## Reproducing the results of *Urban Maladaptation in Times of Epidemics* study

This note explains what to change to reproduce the **baseline (no-response)** and the **hard-lockdown** results used in *Urban Maladaptation in Times of Epidemics*. It assumes you've already followed the project's README to install Java and run the model, and that you're using the provided The Hague data bundle. See the [repo's](https://github.com/averbraeck/medlabs-heros) README sections on *Installing Java and running the code*, *Configuration files*, *Policies*, and *Output files* for context.

> Tip: the repository already organises documentation by topic (disease properties, schedules, people/locations, policies, etc.), and ships prebuilt JAR releases (see Releases). Use those rather than rebuilding unless you're modifying code.
---

## 1) Edit the main configuration (single file)

Open your run configuration (the one that contains the `generic.*` and `policies.*` keys). Make **only** these changes:

```ini
# --- run length ---
generic.RunLength = 120            # was 60

# --- infection model: use AREA-based model for the study ---
generic.diseasePropertiesModel = area

# and point to the area-based properties file
generic.diseasePropertiesFile = /alpha-area.properties   # swap from /alpha-distance.properties

# --- seeds: you will run this config multiple times with different seeds (111..120) ---
# You can keep a default here; we'll override per run on the command line.
generic.Seed = 111
```

Why: the study used an **area-based infection formulation**, a **120-day horizon**, and **multiple random seeds** (we'll loop seeds 111–120 below). The repo docs call out where the disease properties file and model switch live.

---

## 2) Baseline (no-response) run

Use **no location policy file** so nothing closes:

```ini
policies.LocationPolicyFile =
```

> Tip: by default, the `policies.LocationPolicyFile` is already blank, so you don't need to set it. However, to be explicit, let's still set it to an empty string.

Run the JAR once for each seed 111–120 (adjust the JAR name/path to the current release):

```bash
for s in $(seq 111 120); do
  java -Xmx8g -jar jar/medlabs-heros-*.jar \
    -Dgeneric.Seed=$s \
    -Dgeneric.RunLength=120 \
    -Dgeneric.diseasePropertiesModel=area \
    -Dgeneric.diseasePropertiesFile=/alpha-area.properties \
    -Dpolicies.LocationPolicyFile="" \
    -Doutput.OutputPath=./out/seed-baseline-$s	
done
```

Outputs go to `./out` by default, as described in the README's *Output files* section. To keep track of the results, we keep a per-seed subfolder. The naming convention is `seed-baseline-<seed>`.

---

## 3) Hard-lockdown policy (close from day 20)

Create or use a pre-made CSV policy file (e.g., [HardLockdown_20.csv](https://github.com/averbraeck/medlabs-heros/tree/main?tab=readme-ov-file)) with **closures starting day 20**. In a hard lockdown **everything closes**, except for essential services: **Supermarket**, **FireStation**, **Police**, **Pharmacy**, and **Hospital** remain *open* (1.0).

```csv
Time(d)	LocationType	FractionOpen	FractionActivities	AlternativeLocation	ReportAsLocation
20.0	Workplace	0.0	0.0	Accommodation	WorkToHome
20.0	Retail	0.0	0.0	Accommodation	StayHome
20.0	Mall	0.0	0.0	Accommodation	StayHome
20.0	BarRestaurant	0.0	0.0	Accommodation	StayHome
20.0	FoodBeverage	0.0	0.0	Accommodation	StayHome
20.0	Kindergarten	0.0	0.0	Accommodation	KindergartenToHome
20.0	PrimarySchool	0.0	0.0	Accommodation	PrimarySchoolToHome
20.0	SecondarySchool	0.0	0.0	Accommodation	SecondarySchooToHome
20.0	College	0.0	0.0	Accommodation	CollegeToHome
20.0	University	0.0	0.0	Accommodation	UniversityToHome
20.0	Religion	0.0	0.0	Accommodation	StayHome
20.0	Recreation	0.0	0.0	Accommodation	StayHome
20.0	Park	0.0	0.0	Accommodation	StayHome
20.0	Satellite workplace	0.0	0.0	Accommodation	SatelliteWorkToHome
```

Place it under `config` folder and point the config file to it:

```ini
policies.LocationPolicyFile = config/HardLockdown_20.csv
```

Run the same **multi-seed loop**:

```bash
for s in $(seq 111 120); do
  java -Xmx8g -jar jar/medlabs-heros-*.jar \
    -Dgeneric.Seed="$s" \
    -Dgeneric.RunLength=120 \
    -Dgeneric.diseasePropertiesModel=area \
    -Dgeneric.diseasePropertiesFile=/alpha-area.properties \
    -Dpolicies.LocationPolicyFile=config/HardLockdown_20.csv \
    -Doutput.OutputPath=./out/seed-hardlockdown-$s	
done
```

Similarly, we keep a per-seed subfolder. The naming convention is `seed-hardlockdown-<seed>`.

---

## 4) TL;DR — exactly what to change

1. `generic.RunLength = 120`
2. `generic.diseasePropertiesModel = area` and `generic.diseasePropertiesFile = /alpha-area.properties`
3. **Baseline:** `policies.LocationPolicyFile =` *(blank)*
4. **Hard lockdown:** use/create `HardLockdown_20.csv` (all closed from day 20; essential services remain open), set `policies.LocationPolicyFile` to it
5. Run seeds **111..120** for both scenarios and aggregate results
