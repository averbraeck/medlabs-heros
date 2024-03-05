package eu.heros.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.Map;

import javax.naming.NamingException;

import org.djutils.draw.bounds.Bounds2d;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.animation.d2.RenderableScale;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameter;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterException;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterMap;
import nl.tudelft.simulation.dsol.model.inputparameters.reader.ReadInputParameters;
import nl.tudelft.simulation.dsol.swing.gui.DsolPanel;
import nl.tudelft.simulation.dsol.swing.gui.animation.DsolAnimationApplication;
import nl.tudelft.simulation.dsol.swing.gui.animation.DsolAnimationTab;
import nl.tudelft.simulation.dsol.swing.gui.control.RealTimeControlPanel;
import nl.tudelft.simulation.dsol.swing.gui.inputparameters.TabbedParameterDialog;
import nl.tudelft.simulation.language.DsolException;
import nl.tudelft.simulation.medlabs.common.MedlabsException;
import nl.tudelft.simulation.medlabs.location.Location;
import nl.tudelft.simulation.medlabs.simulation.SimpleAnimator;
import nl.tudelft.simulation.medlabs.simulation.SimpleDEVSSimulator;
import nl.tudelft.simulation.medlabs.simulation.gui.AnimationToggles;
import nl.tudelft.simulation.medlabs.simulation.gui.MedlabsAnimationTab;
import nl.tudelft.simulation.medlabs.simulation.gui.MedlabsClockPanel;
import nl.tudelft.simulation.medlabs.simulation.gui.MedlabsPanel;

/**
 * HerosApplication.java.
 * <p>
 * Copyright (c) 2020-2024 Delft University of Technology, Jaffalaan 5, 2628 BX
 * Delft, the Netherlands. All rights reserved. The code is part of the HERoS
 * project (Health Emergency Response in Interconnected Systems), which builds
 * on the MEDLABS project. The simulation tools are aimed at providing policy
 * analysis tools to predict and help contain the spread of epidemics. They make
 * use of the DSOL simulation engine and the agent-based modeling formalism.
 * This software is licensed under the BSD license. See license.txt in the main
 * project.
 * </p>
 * 
 * @author <a href="https://www.linkedin.com/in/mikhailsirenko">Mikhail
 *         Sirenko</a>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class HerosApplication extends DsolAnimationApplication {
	/** */
	private static final long serialVersionUID = 20201001L;

	/**
	 * Create a DSOL application with animation.
	 * 
	 * @param panel        DSOLPanel; this should be the tabbed panel of the
	 *                     simulation
	 * @param title        String; the title of the window
	 * @param animationTab DSOLAnimationTab; the animation tab to add, e.g. one
	 *                     containing GIS
	 * @throws RemoteException on network error
	 * @throws DSOLException   when simulator does not implement the
	 *                         AnimatorInterface
	 */
	public HerosApplication(final DsolPanel panel, final String title, final DsolAnimationTab animationTab)
			throws RemoteException, DsolException {
		super(panel, title, animationTab);
	}

	/**
	 * @param args String[]. args[0]: properties filename. args[1]: interactive /
	 *             batch. If parameters are missing, "/default.properties" is
	 *             assumed for args[0] and "interactive" is assumed for args[1].
	 * @throws DSOLException
	 * @throws NamingException
	 * @throws SimRuntimeException
	 * @throws IOException
	 * @throws InputParameterException
	 * @throws FileNotFoundException
	 * @throws URISyntaxException
	 * @throws MedlabsException
	 */
	public static void main(final String[] args) throws DsolException, SimRuntimeException, NamingException,
			FileNotFoundException, InputParameterException, IOException, URISyntaxException, MedlabsException {
		String propertyFilename = (args.length > 0) ? args[0] : "/default.properties";
		boolean interactive = (args.length < 2) || !args[1].toLowerCase().equals("batch");
		// boolean interactive = false;
		HerosModel model;
		if (interactive) {
			model = new HerosModel(new SimpleAnimator("SimHERoS"), propertyFilename);
			model.setInteractive(true);
			ReadInputParameters.loadfromProperties(propertyFilename, model.getInputParameterMap());
			ReadInputParameters.loadFromArgs(args, true, model.getInputParameterMap());
			// get the disease model
			String diseaseFilename = model.getParameterValue("generic.diseasePropertiesFile");
			ReadInputParameters.loadfromProperties(diseaseFilename, model.getInputParameterMap());
			setInputParametersDefaults(model.getInputParameterMap());
			InputParameterMap generic = (InputParameterMap) model.getInputParameterMap().get("generic");
			if (model.getParameterValue("generic.diseasePropertiesModel").equals("area"))
				model.getInputParameterMap().remove("covidT_dist");
			else
				model.getInputParameterMap().remove("covidT_area");
			if (TabbedParameterDialog.process(model.getInputParameterMap())) {
				double runLengthDays = (double) model.getParameterValueInt("generic.RunLength");
				long seed = model.getParameterValueLong("generic.Seed");
				model.getSimulator().initialize(0.0, 0.0, runLengthDays * 24.0, model, seed);
				Bounds2d mapBounds = /* model.getExtent(); */ new Bounds2d(4.202, 4.482, 52.011, 52.133);
				// DSOLAnimationGisTab gisTab =
				// new DSOLAnimationGisTab(mapBounds, (SileAnimator) modempl.getSimulator());
				MedlabsAnimationTab gisTab = new MedlabsAnimationTab(mapBounds, (SimpleAnimator) model.getSimulator());
				gisTab.getAnimationPanel().setRenderableScale(
						new RenderableScale(Math.cos(Math.toRadians(mapBounds.midPoint().getY())), 1.0 / 111319.24));
				RealTimeControlPanel rtControlPanel = new RealTimeControlPanel(model,
						(SimpleAnimator) model.getSimulator());
				rtControlPanel.setClockPanel(new MedlabsClockPanel(model.getSimulator()));
				MedlabsPanel panel = new MedlabsPanel(rtControlPanel, model);
				// AnimationToggles.setTextAnimationTogglesStandard(model, gisTab);
				AnimationToggles.setTextAnimationTogglesBasedOnName(model, gisTab);
				gisTab.hideNamedAnimationClass(Location.class, "Accommodation");
				panel.addTab(0, "animation", gisTab);
				panel.getTabbedPane().setSelectedIndex(0); // select the animation pane as default (can be overridden)
				gisTab.addAllToggleGISButtonText("MAP LAYERS", model.getGisMap(), "hide or show this GIS layer");
				HerosApplication app = new HerosApplication(panel, "HERoS", gisTab);
				app.setAppearance(app.getAppearance()); // update appearance of added objects
				panel.enableSimulationControlButtons();
			} else {
				System.exit(0);
			}
		} else {
			model = new HerosModel(new SimpleDEVSSimulator("SimHERoS"), propertyFilename);
			model.setInteractive(false);
			ReadInputParameters.loadfromProperties(propertyFilename, model.getInputParameterMap());
			ReadInputParameters.loadFromArgs(args, true, model.getInputParameterMap());
			String diseaseFilename = model.getParameterValue("generic.diseasePropertiesFile");
			ReadInputParameters.loadfromProperties(diseaseFilename, model.getInputParameterMap());
			setInputParametersDefaults(model.getInputParameterMap());
			InputParameterMap generic = (InputParameterMap) model.getInputParameterMap().get("generic");
			if (model.getParameterValue("generic.diseasePropertiesModel").equals("area"))
				model.getInputParameterMap().remove("covidT_dist");
			else
				model.getInputParameterMap().remove("covidT_area");
			double runLengthDays = (double) model.getParameterValueInt("generic.RunLength");
			long seed = model.getParameterValueLong("generic.Seed");
			model.getSimulator().initialize(0.0, 0.0, runLengthDays * 24.0, model, seed);
			model.getSimulator().start();
		}
	}

	/**
	 * Update the defaults for the parameters to be displayed to the loaded values
	 * from the properties file and the command line. This ensures that the editing
	 * takes place on the basis of the provided information in the properties file
	 * and the command line.
	 * 
	 * @param map InputParameterMap; the input parameters
	 */
	@SuppressWarnings("unchecked")
	private static void setInputParametersDefaults(final InputParameterMap map) {
		for (Map.Entry<String, InputParameter<?, ?>> entry : map.getValue().entrySet()) {
			@SuppressWarnings("rawtypes")
			InputParameter parameter = entry.getValue();
			if (parameter instanceof InputParameterMap) {
				setInputParametersDefaults((InputParameterMap) parameter);
			}
			try {
				parameter.setDefaultValue(parameter.getCalculatedValue());
			} catch (InputParameterException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

}
