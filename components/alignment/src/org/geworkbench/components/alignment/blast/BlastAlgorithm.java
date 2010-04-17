package org.geworkbench.components.alignment.blast;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.bison.datastructure.biocollections.sequences.CSSequenceSet;
import org.geworkbench.bison.datastructure.biocollections.sequences.DSSequenceSet;
import org.geworkbench.bison.datastructure.bioobjects.sequence.CSAlignmentResultSet;
import org.geworkbench.bison.datastructure.bioobjects.sequence.CSSequence;
import org.geworkbench.bison.datastructure.bioobjects.sequence.DSSequence;
import org.geworkbench.bison.util.RandomNumberGenerator;
import org.geworkbench.builtin.projects.ProjectPanel;
import org.geworkbench.components.alignment.blast.RemoteBlast.NcbiResponseException;
import org.geworkbench.components.alignment.panels.AlgorithmMatcher;
import org.geworkbench.components.alignment.panels.BlastAppComponent;
import org.geworkbench.components.alignment.panels.ParameterSetting;
import org.geworkbench.events.ProjectNodeAddedEvent;
import org.geworkbench.util.BrowserLauncher;
import org.geworkbench.util.FilePathnameUtils;

/**
 * BlastAlgorithm.
 *
 * @author XZ
 * @author zji
 * @version $Id$
 */
public class BlastAlgorithm extends SwingWorker<CSAlignmentResultSet, Integer> {

	static private Log log = LogFactory.getLog(RemoteBlast.class);

	private BlastAppComponent blastAppComponent = null;

	private boolean startBrowser;
	private boolean jobFinished = false;

	private ParameterSetting parameterSetting;

	private CSSequenceSet<CSSequence> sequenceDB;
	private DSSequenceSet<? extends DSSequence> parentSequenceDB;
	
	private final static int TIMEGAP = 4000;
	private final static String LINEBREAK = System.getProperty("line.separator");
	
	public void setBlastAppComponent(BlastAppComponent _blastAppComponent) {
		blastAppComponent = _blastAppComponent;
	}

	/**
	 * Update Progress with finished percentage and related information.
	 *
	 * @param percent
	 *            double
	 * @param text
	 *            String
	 */
	void updateProgressStatus(final double percent, final String text) {
		if (blastAppComponent != null) {
			blastAppComponent.updateProgressBar(percent, text);
		}
	}

	/**
	 * Update progress only with String information.
	 *
	 * @param text
	 *            String
	 */
	void updateStatus(String text) {
		if (blastAppComponent != null) {
			blastAppComponent.updateProgressBar(text);
		}
	}

	/**
	 * Update the component's progressBar with information and reset the
	 * ProgressBar.
	 *
	 * @param boo
	 *            boolean
	 * @param text
	 *            String
	 */

	void updateStatus(boolean boo, String text) {
		if (blastAppComponent != null) {
			blastAppComponent.updateProgressBar(boo, text);
		}
	}

	/**
	 * Show error message and update status
	 *
	 * @param e
	 */
	private void processExceptionFromNcbi(Exception e, CSSequence sequence) {
		if (blastAppComponent != null) {
			String exceptionName = e.getClass().getName();
			blastAppComponent.reportError("Sequence " + sequence
					+ " cannot be blasted due to " + exceptionName + ":\n"
					+ e.getMessage(), "Error: " + exceptionName);

		}
		updateStatus(false, "NCBI Blast is stopped at " + new Date());
	}

	/**
	 * The workhorse to run Blast program.
	 *
	 * This method is invoked by from the working thread.
	 */
	@Override
	protected CSAlignmentResultSet doInBackground() throws Exception {
		if (sequenceDB == null || parentSequenceDB == null) {
			throw new Exception("null sequenceDB or null parentSequenceDB");
		}

		String tempFolder = FilePathnameUtils.getTemporaryFilesDirectoryPath();

		/* generate a new file name for the coming output file. */
		String outputFile = tempFolder + "Blast"
				+ RandomNumberGenerator.getID() + ".html";

		RemoteBlast blast;
		DSSequenceSet<? extends DSSequence> parentSequenceSet = parentSequenceDB;

		for (CSSequence sequence : sequenceDB) {
			updateStatus("Uploading sequence: " + sequence);
			blast = new RemoteBlast(sequence.getSequence(), outputFile, AlgorithmMatcher
					.translateToCommandline(parameterSetting));
			String BLAST_rid = null;
			try {
				BLAST_rid = blast.submitBlast();
			} catch (NcbiResponseException e1) {
				processExceptionFromNcbi(e1, sequence);
				return null;
			}
			if (isCancelled()) { 
				return null;
			}
			if (BLAST_rid == null) {
				if (blastAppComponent != null) {
					blastAppComponent
							.reportError(
									"Sequence "
											+ sequence
											+ " cannot be blasted, please check your parameters.",
									"Parameter Error");

				}
				updateStatus(false, "NCBI Blast is stopped at "
						+ new Date());
				return null;

			}
			updateStatus("The Request ID is : " + BLAST_rid);

			String resultURLString = "http://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Get&FORMAT_TYPE=HTML&RID="
					+ BLAST_rid;
			while(!blast.retrieveResult(resultURLString) && !isCancelled()) {
				updateStatus("For sequence " + sequence
						+ ",  the blast job is running. ");
				try {
					Thread.sleep(TIMEGAP);
				} catch (InterruptedException e) {
					// do nothing
				}
			}
		}
		if(isCancelled()){
			updateStatus(false, "NCBI Blast is canceled at " + new Date());
			return null;
		}
		updateStatus(false, "NCBI Blast is finisheded at " + new Date());
		String outputFilePath = "file://"
				+ new File(outputFile).getAbsolutePath();
		if (parameterSetting.isViewInBrowser()) {
			if ((new File(outputFile)).canRead()) {
				try {
					String osName = System.getProperty("os.name");
					if (osName.startsWith("Mac OS")) {
						BrowserLauncher.openURL(outputFilePath);
					} else {
						BrowserLauncher.openURL("file:///"+new File(outputFile)
								.getAbsolutePath().replace("\\", "/").replace(" ", "%20"));
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(null,
							"No web browser can be launched, the result is saved at "
									+ outputFile, "No Web Browser",
							JOptionPane.ERROR_MESSAGE);

				}
			} else {

				JOptionPane.showMessageDialog(null,
						"The result cannot be read at " + outputFile,
						"File cannot be read", JOptionPane.ERROR_MESSAGE);

			}

		}

		CSAlignmentResultSet blastResult = new CSAlignmentResultSet(outputFile, sequenceDB
				.getFASTAFileName(), sequenceDB, parentSequenceSet);
		blastResult.setLabel(BlastAppComponent.NCBILABEL);
		ProjectNodeAddedEvent event = new ProjectNodeAddedEvent(null, null,
				blastResult);
		if(isCancelled())return null;
		blastAppComponent.publishProjectNodeAddedEvent(event);
		log.debug("blast result node added");
		return blastResult;
	}
	
	@Override
    protected void done() {
		if(isCancelled())return;
		
		CSAlignmentResultSet blastResult;
		try {
			blastResult = get();
			if (blastResult==null)return;
			
			String historyStr = generateHistoryStr(sequenceDB);
			ProjectPanel.addToHistory(blastResult, historyStr);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	private String generateHistoryStr(CSSequenceSet<CSSequence> activeSequenceDB) {
		String histStr = "";
		if(parameterSetting!=null){

			// Header
			histStr += "Blast run with the following parameters:\n";
			histStr += "----------------------------------------\n\n";


				histStr += "Database: " + parameterSetting.getDbName() + LINEBREAK;
				histStr += "BLAST Program: " + parameterSetting.getProgramName() + LINEBREAK;
				histStr += "Expect: " + parameterSetting.getExpect() + LINEBREAK;
				histStr += "Matrix: " + parameterSetting.getMatrix() + LINEBREAK;
				histStr += "Gap Cost: " + parameterSetting.getGapCost() + LINEBREAK;
				histStr += "Word Size: " + parameterSetting.getWordsize() + LINEBREAK;
				histStr += "Low Complexity Filter On: " + parameterSetting.isLowComplexityFilterOn() + LINEBREAK;
				histStr += "Human Repeat Filter On: " + parameterSetting.isHumanRepeatFilterOn() + LINEBREAK;
				histStr += "Mask Low Case: " + parameterSetting.isMaskLowCase() + LINEBREAK;
				histStr += "Mask Lookup Table: " + parameterSetting.isMaskLookupTable() + LINEBREAK + LINEBREAK;

				histStr += "Number of Sequences: " + activeSequenceDB.size() + LINEBREAK;
				for (CSSequence marker : activeSequenceDB) {
					histStr += "\t" + marker.getLabel() + "\n";
				}
		}
		return histStr;
	}

	public boolean isStartBrowser() {
		return startBrowser;
	}

	public BlastAppComponent getBlastAppComponent() {
		return blastAppComponent;
	}

	public ParameterSetting getParameterSetting() {
		return parameterSetting;
	}

	public boolean isJobFinished() {
		return jobFinished;
	}

	public void setStartBrowser(boolean startBrowser) {
		this.startBrowser = startBrowser;
	}

	public void setParameterSetting(ParameterSetting parameterSetting) {
		this.parameterSetting = parameterSetting;
	}

	public void setJobFinished(boolean jobFinished) {
		this.jobFinished = jobFinished;
	}

	public void setSequenceDB(CSSequenceSet<CSSequence> sequenceDB) {
		this.sequenceDB = sequenceDB;
		
	}

	public void setParentSequenceDB(DSSequenceSet<? extends DSSequence> parentSequenceDB) {
		this.parentSequenceDB = parentSequenceDB;
		
	}

}
