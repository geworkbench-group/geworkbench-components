package org.geworkbench.components.mindy;

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.geworkbench.analysis.AbstractGridAnalysis;
import org.geworkbench.bison.datastructure.biocollections.microarrays.CSMicroarraySet;
import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.bison.datastructure.biocollections.views.DSMicroarraySetView;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMicroarray;
import org.geworkbench.bison.datastructure.complex.panels.DSItemList;
import org.geworkbench.bison.datastructure.complex.panels.DSPanel;
import org.geworkbench.bison.model.analysis.AlgorithmExecutionResults;
import org.geworkbench.bison.model.analysis.ClusteringAnalysis;
import org.geworkbench.builtin.projects.ProjectPanel;
import org.geworkbench.engine.management.Publish;
import org.geworkbench.engine.management.Subscribe;
import org.geworkbench.events.GeneSelectorEvent;
import org.geworkbench.events.ProjectNodeAddedEvent;
import org.geworkbench.util.ProgressBar;
import org.geworkbench.util.pathwaydecoder.mutualinformation.MindyData;
import org.geworkbench.util.pathwaydecoder.mutualinformation.MindyDataSet;
import org.geworkbench.util.pathwaydecoder.mutualinformation.MindyGeneMarker;

import wb.data.Marker;
import wb.data.MarkerSet;
import wb.data.Microarray;
import wb.data.MicroarraySet;
import edu.columbia.c2b2.mindy.Mindy;
import edu.columbia.c2b2.mindy.MindyResults;

/**
 * @author Matt Hall
 * @author ch2514
 * @author zji
 * @version $id$
 */
@SuppressWarnings("serial")
public class MindyAnalysis extends AbstractGridAnalysis implements
		ClusteringAnalysis {
	Log log = LogFactory.getLog(this.getClass());

	private MindyParamPanel paramPanel;

	private MindyDataSet mindyDataSet;

	private DSMicroarraySetView<DSGeneMarker, DSMicroarray> inputSetView;

	private final String analysisName = "Mindy";

	private ProgressBar progressBar = null;

	MindyResults results = null;

	/**
	 * Constructor. Creates MINDY parameter panel.
	 */
	public MindyAnalysis() {
		setLabel("MINDY");
		paramPanel = new MindyParamPanel();
		setDefaultPanel(paramPanel);
	}

	// not used - required to implement from interface ClusteringAnalysis
	public int getAnalysisType() {
		return AbstractGridAnalysis.ZERO_TYPE;
	}

	/**
	 * The execute method the framework calls to analyze parameters and create
	 * MINDY results.
	 * 
	 * @param input -
	 *            microarray set data coming from the framework
	 * @return analysis algorithm results
	 */
	@SuppressWarnings("unchecked")
	public AlgorithmExecutionResults execute(Object input) {
		if (input == null) {
			return new AlgorithmExecutionResults(false, "Invalid input.", null);
		}
		log.debug("input: " + input);
		inputSetView = (DSMicroarraySetView) input;
		DSPanel<DSMicroarray> arraySet = null;
		DSPanel<DSGeneMarker> markerSet = null;
		if (inputSetView.useItemPanel())
			arraySet = inputSetView.getItemPanel();
		if (inputSetView.useMarkerPanel())
			markerSet = inputSetView.getMarkerPanel();

		// Mindy parameter validation always returns true
		// (the method is not overrode from AbstractAnalysis)
		// so we can enter the execute() method and capture
		// both parameter and input errors.
		// The eventual error message dialog (if there are errors)
		// would look the same as the one created by the analysis panel

		stopAlgorithm = false;
		progressBar = ProgressBar.create(ProgressBar.INDETERMINATE_TYPE);
		progressBar.addObserver(this);
		progressBar.setTitle("MINDY");
		progressBar.setMessage("Processing Parameters");
		progressBar.start();

		// Use this to get params
		MindyParamPanel params = (MindyParamPanel) aspp;
		DSMicroarraySet<DSMicroarray> mSet = inputSetView.getMicroarraySet();
		StringBuilder paramDescB = new StringBuilder(
				"Generated by MINDY run with paramters: \n");
		StringBuilder errMsgB = new StringBuilder();

		int numMAs = mSet.size();
		paramDescB.append("Number of microarrays: ");
		paramDescB.append(numMAs);
		paramDescB.append("\n");
		if (numMAs < 4) {
			errMsgB
					.append("Not enough microarrays in the set.  MINDY requires at least 4 microarrays.\n");
		}

		int numMarkers = mSet.getMarkers().size();
		paramDescB.append("Number of markers: ");
		paramDescB.append(numMarkers);
		paramDescB.append("\n");
		if (numMarkers < 2) {
			errMsgB
					.append("Not enough markers in the microarrays. (Need at least 2)\n");
		}

		paramDescB.append("Modulator list: ");
		ArrayList<Marker> modulators = new ArrayList<Marker>();
		ArrayList<String> modulatorGeneList = params.getModulatorGeneList();
		if ((modulatorGeneList != null) && (modulatorGeneList.size() > 0)) {
			for (String modGene : modulatorGeneList) {
				DSGeneMarker marker = mSet.getMarkers().get(modGene);
				if (marker == null) {
					errMsgB.append("Couldn't find marker ");
					errMsgB.append(modGene);
					errMsgB.append(" from modulator file in microarray set.\n");
				} else {
					paramDescB.append(modGene);
					paramDescB.append(" ");
					modulators.add(new Marker(modGene));
				}
			}
			paramDescB.append("\n");
		} else {
			errMsgB.append("No modulator specified.\n");
		}

		paramDescB.append("Target list: ");
		ArrayList<Marker> targets = new ArrayList<Marker>();
		ArrayList<String> targetGeneList = params.getTargetGeneList();
		if ((targetGeneList != null) && (targetGeneList.size() > 0)) {
			for (String modGene : targetGeneList) {
				DSGeneMarker marker = mSet.getMarkers().get(modGene);
				if (marker == null) {
					errMsgB.append("Couldn't find marker ");
					errMsgB.append(modGene);
					errMsgB.append(" from tartet file in microarray set.\n");
				} else {
					paramDescB.append(modGene);
					paramDescB.append(" ");
					targets.add(new Marker(modGene));
				}
			}
		}
		paramDescB.append("\n");

		paramDescB.append("DPI Target List: ");
		ArrayList<Marker> dpiAnnots = new ArrayList<Marker>();
		ArrayList<String> dpiAnnotList = params.getDPIAnnotatedGeneList();
		for (String modGene : dpiAnnotList) {
			DSGeneMarker marker = mSet.getMarkers().get(modGene);
			if (marker == null) {
				errMsgB.append("Couldn't find marker ");
				errMsgB.append(modGene);
				errMsgB
						.append(" from DPI annotation file in microarray set.\n");
			} else {
				paramDescB.append(modGene);
				paramDescB.append(" ");
				dpiAnnots.add(new Marker(modGene));
			}
		}
		paramDescB.append("\n");

		paramDescB.append("DPI Tolerance: ");
		paramDescB.append(params.getDPITolerance());
		paramDescB.append("\n");

		String transcriptionFactor = params.getTranscriptionFactor();
		DSGeneMarker transFac = mSet.getMarkers().get(transcriptionFactor);
		if (!transcriptionFactor.trim().equals("")) {
			if (transFac == null) {
				errMsgB.append("Specified hub marker (");
				errMsgB.append(transcriptionFactor);
				errMsgB.append(") not found in loadad microarray set.\n");
			} else {
				paramDescB.append("Hub Marker: ");
				paramDescB.append(transcriptionFactor);
				paramDescB.append("\n");
			}
		} else {
			errMsgB.append("No hub marker specified.\n");
		}

		boolean fullSetMI = false;
		if (params.getUnconditional().trim().equals(MindyParamPanel.MI)) {
			fullSetMI = true;
		}
		float fullSetThreshold = params.getUnconditionalValue();
		if ((!fullSetMI)
				&& (params.getUnconditionalCorrection()
						.equals(MindyParamPanel.BONFERRONI))) {
			fullSetThreshold = fullSetThreshold / numMarkers;
		}

		boolean subsetMI = false;
		if (params.getConditional().trim().equals(MindyParamPanel.MI)) {
			subsetMI = true;
		}
		float subsetThreshold = params.getConditionalValue();
		if ((!subsetMI)
				&& (params.getUnconditionalCorrection()
						.equals(MindyParamPanel.BONFERRONI))) {
			subsetThreshold = subsetThreshold / numMarkers;
		}

		paramDescB.append("Conditional:\t\t");
		paramDescB.append(params.getConditional());
		paramDescB.append(" at ");
		paramDescB.append(params.getConditionalValue());
		paramDescB.append("\tCorrection: ");
		paramDescB.append(params.getConditionalCorrection());
		paramDescB.append("\n");

		paramDescB.append("Unconditional:\t");
		paramDescB.append(params.getUnconditional());
		paramDescB.append(" at ");
		paramDescB.append(params.getUnconditionalValue());
		paramDescB.append("\tCorrection: ");
		paramDescB.append(params.getUnconditionalCorrection());
		paramDescB.append("\n");

		float setFraction = params.getSetFraction() / 100f;
		paramDescB.append("Sample per Condition(%): ");
		paramDescB.append(setFraction);
		paramDescB.append("\n");
		if (Math.round(setFraction * 2 * numMarkers) < 2) {
			errMsgB
					.append("Not enough markers in the specified % sample.  MINDY requires at least 2 markers in the sample.\n");
		}
		paramDescB.append("Arrays:\n");
		if ((arraySet != null) && (arraySet.size() > 0)) {
			for (DSMicroarray ma : arraySet) {
				paramDescB.append("\t");
				paramDescB.append(ma.getLabel());
				paramDescB.append("\n");
			}
		} else {
			for (DSMicroarray ma : inputSetView.getMicroarraySet()) {
				paramDescB.append("\t");
				paramDescB.append(ma.getLabel());
				paramDescB.append("\n");
			}
		}
		paramDescB.append("Markers:\n");
		if ((markerSet != null) && (markerSet.size() > 0)) {
			for (DSGeneMarker m : markerSet) {
				paramDescB.append("\t");
				paramDescB.append(m.getShortName());
				paramDescB.append("\n");
			}

		} else {
			for (DSGeneMarker m : inputSetView.markers()) {
				paramDescB.append("\t");
				paramDescB.append(m.getShortName());
				paramDescB.append("\n");
			}
		}

		// If parameters or inputs have errors, alert the user and return from
		// execute()
		errMsgB.trimToSize();
		String s = errMsgB.toString();
		if (!s.equals("")) {
			log.info(errMsgB.toString());
			JOptionPane.showMessageDialog(null, s,
					"Parameter and Input Validation Error",
					JOptionPane.ERROR_MESSAGE);
			progressBar.stop();
			return null;
		}

		if (stopAlgorithm) {
			stopAlgorithm = false;
			progressBar.stop();
			log.warn("Cancelling Mindy Analysis.");
			return null;
		}
		progressBar.setMessage("Running MINDY Algorithm");
		MindyThread mt = new MindyThread(mSet, arraySet, markerSet, params
				.getTargetGeneList(), transFac, new Marker(params
				.getTranscriptionFactor()), modulators, dpiAnnots, fullSetMI,
				fullSetThreshold, subsetMI, subsetThreshold, setFraction,
				params.getDPITolerance(), paramDescB.toString(), params
						.getCandidateModulatorsFile());
		progressBar.addObserver(mt);
		mt.start();
		return new AlgorithmExecutionResults(true, "MINDY in progress.", null);
	}

	/**
	 * Receives GeneSelectorEvents from the framework (i.e. the Selector Panel)
	 * 
	 * @param e
	 * @param source
	 */
	@Subscribe
	public void receive(GeneSelectorEvent e, Object source) {
		DSGeneMarker marker = e.getGenericMarker(); // GeneselectorEvent can be
		// a panel event therefore
		// won't work here,
		if (marker != null) { // so added this check point--xuegong
			paramPanel.setTranscriptionFactor(marker.getLabel());
		}
	}

	/**
	 * Publish MINDY data to the framework.
	 * 
	 * @param data
	 * @return
	 */
	@Publish
	public MindyDataSet publishMatrixReduceSet(MindyDataSet data) {
		return data;
	}

	@Publish
	public ProjectNodeAddedEvent publishProjectNodeAddedEvent(
			ProjectNodeAddedEvent event) {
		return event;
	}

	class MindyThread extends Thread implements Observer {
		DSMicroarraySet<DSMicroarray> mSet;

		DSPanel<DSMicroarray> arraySet;

		DSPanel<DSGeneMarker> markerSet;

		List<String> chosenTargets;

		DSGeneMarker transFac;

		Marker tf;

		ArrayList<Marker> modulators;

		ArrayList<Marker> dpiAnnots;

		boolean fullSetMI;

		float fullSetThreshold;

		boolean subsetMI;

		float subsetThreshold;

		float setFraction;

		float dpiTolerance;

		String paramDesc;

		String candidateModFile;

		public MindyThread(DSMicroarraySet<DSMicroarray> mSet,
				DSPanel<DSMicroarray> arraySet,
				DSPanel<DSGeneMarker> markerSet, List<String> chosenTargets,
				DSGeneMarker transFac, Marker tf, ArrayList<Marker> modulators,
				ArrayList<Marker> dpiAnnots, boolean fullSetMI,
				float fullSetThreshold, boolean subsetMI,
				float subsetThreshold, float setFraction, float dpiTolerance,
				String paramDesc, String candidateModFile) {
			this.mSet = mSet;
			this.arraySet = arraySet;
			this.markerSet = markerSet;
			this.chosenTargets = chosenTargets;
			this.transFac = transFac;
			this.tf = tf;
			this.modulators = modulators;
			this.dpiAnnots = dpiAnnots;
			this.fullSetMI = fullSetMI;
			this.fullSetThreshold = fullSetThreshold;
			this.subsetMI = subsetMI;
			this.subsetThreshold = subsetThreshold;
			this.setFraction = setFraction;
			this.dpiTolerance = dpiTolerance;
			this.paramDesc = paramDesc;
			this.candidateModFile = candidateModFile;
		}

		public void run() {
			log.debug("Running MINDY algorithm...");
			Mindy mindy = new Mindy();
			results = mindy.runMindy(convert(mSet, arraySet, markerSet,
					chosenTargets), tf, modulators, dpiAnnots, fullSetMI,
					fullSetThreshold, subsetMI, subsetThreshold, setFraction,
					dpiTolerance);
			log.debug("Finished running MINDY algorithm.");

			progressBar.setMessage("Processing MINDY Results");

			int numWithSymbols = 0;
			List<MindyData.MindyResultRow> dataRows = new ArrayList<MindyData.MindyResultRow>();
			Collator myCollator = Collator.getInstance();
			HashMap<DSGeneMarker, MindyGeneMarker> mindyMap = new HashMap<DSGeneMarker, MindyGeneMarker>();
			for (MindyResults.MindyResultForTarget result : results) {
				DSItemList<DSGeneMarker> markers = mSet.getMarkers();
				DSGeneMarker target = markers.get(result.getTarget().getName());
				if (!StringUtils.isEmpty(target.getGeneName()))
					numWithSymbols++;
				if (!mindyMap.containsKey(target)) {
					mindyMap.put(target, new MindyGeneMarker(target, myCollator
							.getCollationKey(target.getShortName()), myCollator
							.getCollationKey(target.getDescription())));
				}
				for (MindyResults.MindyResultForTarget.ModulatorSpecificResult specificResult : result) {
					DSGeneMarker mod = markers.get(specificResult
							.getModulator().getName());
					if (!StringUtils.isEmpty(mod.getGeneName()))
						numWithSymbols++;
					if (!mindyMap.containsKey(mod)) {
						mindyMap
								.put(mod, new MindyGeneMarker(mod, myCollator
										.getCollationKey(mod.getShortName()),
										myCollator.getCollationKey(mod
												.getDescription())));
					}
					dataRows.add(new MindyData.MindyResultRow(mod, transFac,
							target, specificResult.getScore(), 0f, myCollator
									.getCollationKey(mod.getShortName()),
							myCollator.getCollationKey(target.getShortName())));
				}
			}

			if (dataRows.size() <= 0) {
				progressBar.stop();
				log.warn("MINDY obtained no results.");
				JOptionPane.showMessageDialog(paramPanel.getParent(),
						"MINDY obtained no results.", "MINDY Analyze Error",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			MindyData loadedData = new MindyData((CSMicroarraySet) mSet,
					dataRows, setFraction);
			loadedData.setMindyMap(mindyMap);

			// Pearson correlation
			ArrayList<DSMicroarray> maList = loadedData.getArraySetAsList();
			SimpleRegression sr;
			for (MindyData.MindyResultRow r : dataRows) {
				sr = new SimpleRegression();
				for (DSMicroarray ma : maList) {
					sr.addData(ma.getMarkerValue(r.getTarget()).getValue(), ma
							.getMarkerValue(r.getTranscriptionFactor())
							.getValue());
				}
				r.setCorrelation(sr.getR());
			}

			if (numWithSymbols > 0)
				loadedData.setAnnotated(true);
			mindyDataSet = new MindyDataSet(mSet, "MINDY Results", loadedData,
					candidateModFile);
			log.info("Done converting MINDY results.");

			if (mindyDataSet != null) {
				log.info(paramDesc);
				ProjectPanel.addToHistory(mindyDataSet, paramDesc);
				progressBar.stop();
				publishProjectNodeAddedEvent(new ProjectNodeAddedEvent(
						"Mindy Result Added", null, mindyDataSet));
			} else {
				JOptionPane.showMessageDialog(paramPanel.getParent(),
						"Cannot analyze data.", "MINDY Analyze Error",
						JOptionPane.WARNING_MESSAGE);
				log.warn("MINDY Analyze Error: Cannot analyze data.");
			}

			progressBar.stop();
		}

		public void update(java.util.Observable ob, Object o) {
			log.debug("initiated close");
			log.warn("Cancelling Mindy Analysis.");
			this.stop();
		}

		private MicroarraySet convert(DSMicroarraySet<DSMicroarray> inSet,
				DSPanel arraySet, DSPanel markerSet,
				List<String> chosenTargets) {
			MarkerSet markers = new MarkerSet();
			if ((markerSet != null) && (markerSet.size() > 0)) {
				log.debug("Processing marker panel: size=" + markerSet.size());
				int size = markerSet.size();
				for (int i = 0; i < size; i++) {
					markers.addMarker(new Marker(((DSGeneMarker) markerSet
							.get(i)).getLabel()));
				}
			}
			log.debug("markers size (post panel)=" + markers.size());
			if ((chosenTargets != null) && (chosenTargets.size() > 0)) {
				log.debug("Processing chosen targets: size=" + chosenTargets.size());
				int size = chosenTargets.size();
				List<String> alreadyIn = markers.getAllMarkerNames();
				for (int i = 0; i < size; i++) {
					String chosenName = chosenTargets.get(i);
					if (chosenName != null) {
						chosenName = chosenName.trim();
						if (!alreadyIn.contains(chosenName)) {
							markers.addMarker(new Marker(chosenName));
						}
					}
				}
			}
			log.debug("markers size (post chosen)=" + markers.size());
			if (markers.size() <= 0) {
				log.debug("adding all markers.");
				for (DSGeneMarker marker : inSet.getMarkers()) {
					markers.addMarker(new Marker(marker.getLabel()));
				}
			}

			MicroarraySet returnSet = new MicroarraySet(inSet.getDataSetName(),
					"ID", "ChipType", markers);
			if ((arraySet != null) && (arraySet.size() > 0)) {
				int size = arraySet.size();
				for (int i = 0; i < size; i++) {
					DSMicroarray ma = (DSMicroarray) arraySet.get(i);
					returnSet.addMicroarray(new Microarray(ma.getLabel(), ma
							.getRawMarkerData()));
				}
			} else {
				for (DSMicroarray microarray : inSet) {
					returnSet.addMicroarray(new Microarray(microarray
							.getLabel(), microarray.getRawMarkerData()));
				}
			}
			
			// debug only
			if(log.isDebugEnabled()){
				MarkerSet ms = returnSet.getMarkers();
				log.debug("Markers in converted set:");
				for(int i = 0; i < ms.size(); i++){
					log.debug("\t" + ms.getMarker(i).getName());
				}
			}

			return returnSet;
		}
	}

	// the following methods implemented for AbstractGridAnalysis
	@Override
	public String getAnalysisName() {
		return analysisName;
	}

	@Override
	protected Map<Serializable, Serializable> getBisonParameters() {
		Map<Serializable, Serializable> bisonParameters = new HashMap<Serializable, Serializable>();
		// protected AbstractSaveableParameterPanel aspp is defined in
		// AbstractAnalysis
		MindyParamPanel paramPanel = (MindyParamPanel) this.aspp;

		ArrayList<String> modulatorGeneList = paramPanel.getModulatorGeneList();
		bisonParameters.put("modulatorGeneList", modulatorGeneList);
		ArrayList<String> targetGeneList = paramPanel.getTargetGeneList();
		bisonParameters.put("targetGeneList", targetGeneList);
		String transcriptionFactor = paramPanel.getTranscriptionFactor(); // this
		// is
		// labeled
		// "Hub
		// marker"
		// on
		// GUI
		bisonParameters.put("transcriptionFactor", transcriptionFactor);
		int setFraction = paramPanel.getSetFraction();
		bisonParameters.put("setFraction", setFraction);

		String conditional = paramPanel.getConditional().trim();
		bisonParameters.put("conditional", conditional);
		float conditionalValue = paramPanel.getConditionalValue();
		bisonParameters.put("conditionalValue", conditionalValue);
		String conditionalCorrection = paramPanel.getConditionalCorrection();
		bisonParameters.put("conditionalCorrection", conditionalCorrection);
		String unconditional = paramPanel.getUnconditional().trim();
		bisonParameters.put("unconditional", unconditional);
		float unconditionalValue = paramPanel.getUnconditionalValue();
		bisonParameters.put("unconditionalValue", unconditionalValue);
		String unconditionalCorrection = paramPanel
				.getUnconditionalCorrection();
		bisonParameters.put("unconditionalCorrection", unconditionalCorrection);

		ArrayList<String> dpiAnnotList = paramPanel.getDPIAnnotatedGeneList();
		bisonParameters.put("dpiAnnotList", dpiAnnotList);
		float dpiTolerance = paramPanel.getDPITolerance();
		bisonParameters.put("dpiTolerance", dpiTolerance);

		bisonParameters.put("candidateModulatorsFile", paramPanel
				.getCandidateModulatorsFile());

		return bisonParameters;
	}

	@Override
	public Class<?> getBisonReturnType() {
		return MindyDataSet.class;
	}

	@Override
	protected boolean useMicroarraySetView() {
		return true;
	}

	@Override
	protected boolean useOtherDataSet() {
		return false;
	}

}
