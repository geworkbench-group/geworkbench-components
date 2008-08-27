package org.geworkbench.components.medusa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.analysis.AbstractGridAnalysis;
import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.biocollections.views.CSMicroarraySetView;
import org.geworkbench.bison.datastructure.biocollections.views.DSMicroarraySetView;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMicroarray;
import org.geworkbench.bison.datastructure.complex.panels.DSItemList;
import org.geworkbench.bison.model.analysis.AlgorithmExecutionResults;
import org.geworkbench.bison.model.analysis.ClusteringAnalysis;
import org.geworkbench.bison.model.analysis.ParamValidationResults;
import org.geworkbench.components.medusa.gui.MedusaParamPanel;
import org.geworkbench.util.ProgressBar;
import org.geworkbench.util.Util;
import org.ginkgo.labs.util.FileTools;

import edu.columbia.ccls.medusa.MedusaLoader;

/**
 * 
 * @author keshav
 * @version $Id: MedusaAnalysis.java,v 1.41 2008-08-27 18:33:12 chiangy Exp $
 */
public class MedusaAnalysis extends AbstractGridAnalysis implements
		ClusteringAnalysis {

	private Log log = LogFactory.getLog(this.getClass());

	private StringBuilder s = null;

	// TODO change name to inputdataset.labels
	String fileLabels = "data/medusa/dataset/web100_test.labels";

	private List<DSGeneMarker> regulators = null;

	private List<DSGeneMarker> targets = null;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * 
	 */
	public MedusaAnalysis() {
		setLabel("MEDUSA");
		setDefaultPanel(new MedusaParamPanel());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geworkbench.analysis.AbstractGridAnalysis#getAnalysisName()
	 */
	@Override
	public String getAnalysisName() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geworkbench.analysis.AbstractGridAnalysis#getBisonParameters()
	 */
	@Override
	protected Map<Serializable, Serializable> getBisonParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geworkbench.analysis.AbstractAnalysis#getAnalysisType()
	 */
	@Override
	public int getAnalysisType() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geworkbench.bison.model.analysis.Analysis#execute(java.lang.Object)
	 */
	public AlgorithmExecutionResults execute(Object input) {
		MedusaParamPanel params = (MedusaParamPanel) aspp;

		DSMicroarraySetView<DSGeneMarker, DSMicroarray> microarraySetView = (CSMicroarraySetView<DSGeneMarker, DSMicroarray>) input;
		
		//clone the microarraySetView, so we'll have new microarraySetView for each sessions.
		DSMicroarraySetView<DSGeneMarker, DSMicroarray> newMicroarraySetView = null;
		try{
			byte[] encodedInput;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(input);
			encodedInput = bos.toByteArray();
			oos.flush();
			oos.close();
			bos.close();
			ByteArrayInputStream bais = new ByteArrayInputStream(encodedInput);
			ObjectInputStream ois = new ObjectInputStream(bais);
			newMicroarraySetView = (DSMicroarraySetView<DSGeneMarker, DSMicroarray>) ois.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// now we have a new microarraySetView.
		
		ProgressBar pBar = Util.createProgressBar("Medusa Analysis");
		pBar.setMessage("Running Medusa");
		pBar.start();

		/* create output dir */
		FileTools.createDir("temp/medusa/dataset/output");

		/* cleanup other runs */
		//TODO: we'll need to find a way to delete outputDir/ 
//		MedusaUtil.deleteRunDir();

		/* PHASE 1 - discretize and create the labels file */

		// discretize
		DiscretizationUtil discretizationUtil = new DiscretizationUtil();
		DSMicroarraySetView<DSGeneMarker, DSMicroarray> discretizedInput = discretizationUtil
				.discretize(newMicroarraySetView, params.getIntervalBase(), params
						.getIntervalBound());

		// create labels file (and get targets & regulators)
		if (StringUtils.isEmpty(params.getLabelsFilePath()))
			params.setLabelsFilePath(this.fileLabels);

		createLabelsFile(discretizedInput, params);

		/* PHASE 2 - either read config file and update with user parameters */
		String configFile = params.getConfigFilePath();

		String updatedConfig = "data/medusa/dataset/config_hacked.xml";
		
		MedusaCommand command = new MedusaCommand();
		try {
			command = getParameters(input, params);
		} catch (Exception e) {
			pBar.stop();			
			log.error(e);
			return new AlgorithmExecutionResults(false,
					"Medusa analysis canceled due to error occurred while examing parameters: "+e.getMessage(), e);
		}
		String outputDir = MedusaUtil.updateConfigXml(configFile, updatedConfig, command);
		s = new StringBuilder();
		s.append("-i=" + updatedConfig);

		String[] args = StringUtils.split(s.toString(), " ");

		/* PHASE 3 - run MEDUSA */
		try {
			log.info("Running Medusa with: " + s.toString());
			MedusaLoader.main(args);			
		} catch (IllegalArgumentException iae) {
			pBar.stop();
			if (iae.getMessage().contains("MISSING FASTA ENTRY")){
				log.error(iae);
				return new AlgorithmExecutionResults(false,
						"Please check your Features File, and make sure it contains following fasta entry.\n"+
						"Error occurred while running MEDUSA: "+iae.getMessage(), iae);
			}else{
				log.error(iae);
				return new AlgorithmExecutionResults(false,
						"Error occurred while running MEDUSA: "+iae.getMessage(), iae);				
			}
		} catch (Exception e) {
			pBar.stop();
			log.error(e);
			//e.printStackTrace();
			//throw new RuntimeException("Error running medusa: " + e);
			return new AlgorithmExecutionResults(false,
					"Error occurred while running MEDUSA: "+e.getMessage(), e);
		}

		MedusaData medusaData = new MedusaData(discretizedInput
				.getMicroarraySet(), regulators, targets, command);
		MedusaDataSet dataSet = new MedusaDataSet(newMicroarraySetView
				.getMicroarraySet(), "MEDUSA Results", medusaData, null);
		dataSet.setAbsPath(outputDir);
		
		pBar.stop();
		return new AlgorithmExecutionResults(true, "MEDUSA Results Loaded.",
				dataSet);
	}

	/**
	 * Create the configuration file.
	 * 
	 * @param input
	 * @param params
	 */
	private void createLabelsFile(Object input, MedusaParamPanel params) {
		// TODO move me to the MedusaHelper
		DSMicroarraySetView<DSGeneMarker, DSMicroarray> microarraySetView = (CSMicroarraySetView<DSGeneMarker, DSMicroarray>) input;

		regulators = getRegulators(params, microarraySetView);

		targets = getTargets(params, microarraySetView);

		MedusaUtil.writeMedusaLabelsFile(microarraySetView, params
				.getLabelsFilePath(), regulators, targets);
	}

	/**
	 * Returns a List of markers to be used as the regulators.
	 * 
	 * @param params
	 * @param microarraySetView
	 * @param regulators
	 * @return {@link List}
	 */
	private List<DSGeneMarker> getRegulators(MedusaParamPanel params,
			DSMicroarraySetView<DSGeneMarker, DSMicroarray> microarraySetView) {

		List<DSGeneMarker> regulators = new ArrayList<DSGeneMarker>();

		DSGeneMarker marker = null;

		/* check if we should just use selected */
		if (params.isUseSelectedAsRegulators()) {
			DSItemList<DSGeneMarker> selectedMarkers = microarraySetView
					.getUniqueMarkers();
			for (int i = 0; i < selectedMarkers.size(); i++) {
				marker = selectedMarkers.get(i);
				regulators.add(marker);
				log.debug("added: " + marker.getLabel());
			}
		}

		/* else use either csv file or text field */
		else {
			String regulatorText = params.getRegulatorTextField().getText();
			String[] regs = StringUtils.split(regulatorText, ",");
			DSItemList<DSGeneMarker> itemList = microarraySetView.allMarkers();
			for (String reg : regs) {
				reg = StringUtils.trim(reg);
				marker = itemList.get(reg);
				regulators.add(marker);
			}
		}

		return regulators;
	}

	/**
	 * Returns a list of the marker labels to be used as targets.
	 * 
	 * @param params
	 * @param microarraySetView
	 * @param regulators
	 * @return {@link List}
	 */
	private List<DSGeneMarker> getTargets(MedusaParamPanel params,
			DSMicroarraySetView<DSGeneMarker, DSMicroarray> microarraySetView) {

		List<DSGeneMarker> targets = new ArrayList<DSGeneMarker>();

		DSGeneMarker marker = null;

		if (params.isUseAllAsTargets()) {

			DSItemList<DSGeneMarker> allMarkers = microarraySetView
					.allMarkers();
			for (int i = 0; i < allMarkers.size(); i++) {
				marker = allMarkers.get(i);
				targets.add(marker);
				log.debug("added: " + marker.getLabel());
			}
		}

		/* else use either csv file or text field */
		else {
			String targetText = params.getTargetTextField().getText();
			String[] targs = StringUtils.split(targetText, ",");
			DSItemList<DSGeneMarker> itemList = microarraySetView.allMarkers();
			for (String tar : targs) {
				tar = StringUtils.trim(tar);
				marker = itemList.get(tar);
				targets.add(marker);
			}
		}

		removeDuplicateTargetsAndRegulators(params, microarraySetView, targets);

		return targets;
	}

	/**
	 * If a marker has been selected as BOTH a target and regulator, it will be
	 * removed from the larger list. That is, if there are more targets, the
	 * marker will be removed from the list of targets. If there are more
	 * regulators, the marker will be removed frm the list of regulators.
	 * 
	 * @param params
	 * @param microarraySetView
	 * @param targets
	 */
	private void removeDuplicateTargetsAndRegulators(MedusaParamPanel params,
			DSMicroarraySetView<DSGeneMarker, DSMicroarray> microarraySetView,
			List<DSGeneMarker> targets) {
		if (regulators == null)
			regulators = this.getRegulators(params, microarraySetView);

		if (regulators.size() >= targets.size()) {
			for (DSGeneMarker m : targets) {
				log
						.debug("Marker "
								+ m.getLabel()
								+ " has been selected as both a target and regultator.  Since there are "
								+ "more regulators than targets, will remove this from the current list of targets.");
				regulators.remove(m);
			}
		}

		else {
			for (DSGeneMarker m : regulators) {
				log
						.debug("Marker "
								+ m.getLabel()
								+ " has been selected as both a target and regultator.  Since there are "
								+ "more targets than regulators, will remove this from the current list of targets.");
				targets.remove(m);
			}
		}
	}

	/**
	 * Read the parameters from the parameters panel.
	 * 
	 * @param params
	 */
	private MedusaCommand getParameters(Object input, MedusaParamPanel params) {

		MedusaCommand command = new MedusaCommand();

		/* input section of config file */
		if (params.getFeaturesFilePath() == ""){
			JOptionPane.showMessageDialog(null,
					"Features File has not been set yet.", "Error",
					JOptionPane.ERROR_MESSAGE);
			throw new RuntimeException("Features File has not been set yet.");
		}
		command.setFeaturesFile(params.getFeaturesFilePath());

		command.setMinKer(params.getMinKmer());

		command.setMaxKer(params.getMaxKmer());

		if (params.getMinKmer() > params.getMaxKmer()) {
			JOptionPane.showMessageDialog(null,
					"Min kmer cannot exceed max kmer.", "Error",
					JOptionPane.ERROR_MESSAGE);
			throw new RuntimeException("Min kmer cannot exceed max kmer.");
		}

		command.setBase(params.getIntervalBase());

		command.setBound(params.getIntervalBound());

		// medusa group has dimers_max_gap, dimers_smallest, dimers_largest
		if (params.isUsingDimers()) {
			command.setUsingDimers(true);
			command.setMinGap(params.getMinGap());
			command.setMaxGap(params.getMaxGap());

			if (params.getMinGap() > params.getMaxGap()) {
				JOptionPane.showMessageDialog(null,
						"Min gap cannot exceed max gap.", "Error",
						JOptionPane.ERROR_MESSAGE);
				throw new RuntimeException("Min gap cannot exceed max gap.");
			}
		}

		else {
			command.setUsingDimers(false);
		}

		if (params.isReverseComplement()) {
			command.setReverseComplement(true);
		} else {
			command.setReverseComplement(false);
		}

		/* parameters */
		command.setIter(params.getBoostingIterations());

		command.setPssmLength(params.getPssmLength());

		command.setAgg(params.getAgg());

		if (params.isReverseComplement()) {
			// s.append(" -revcompsame=T");
		} else {
			// s.append(" -revcompsame=F");
		}

		return command;
	}

	/**
	 * 
	 * 
	 */
	public void printhelp() {
		log.info(MedusaLoader.getHelpMessage());
	}

	@Override
	public Class getBisonReturnType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean useMicroarraySetView() {
		return true;
	}

	@Override
	protected boolean useOtherDataSet() {
		return false;
	}

	@Override
	public ParamValidationResults validInputData(
			DSMicroarraySetView<DSGeneMarker, DSMicroarray> maSetView,
			DSDataSet refMASet) {
		MedusaParamPanel params = (MedusaParamPanel) aspp;
		if (params.getFeaturesFilePath() == ""){
			return new ParamValidationResults(false,"Features File has not been set yet.");
		}
		if (params.getMinKmer() > params.getMaxKmer()) {
			return new ParamValidationResults(false,"Min kmer cannot exceed max kmer.");
		}
		if (params.isUsingDimers()) {
			if (params.getMinGap() > params.getMaxGap()) {
				return new ParamValidationResults(false,"Min gap cannot exceed max gap.");
			}
		}
		return new ParamValidationResults(true,"No Error");
	}

}
