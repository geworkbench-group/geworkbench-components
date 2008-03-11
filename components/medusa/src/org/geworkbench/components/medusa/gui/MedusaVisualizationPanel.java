package org.geworkbench.components.medusa.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.datastructure.complex.panels.CSPanel;
import org.geworkbench.bison.datastructure.complex.panels.DSPanel;
import org.geworkbench.components.medusa.MedusaData;
import org.geworkbench.components.medusa.MedusaUtil;
import org.geworkbench.events.SubpanelChangedEvent;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import edu.columbia.ccls.medusa.io.SerializedRule;

/**
 * This plugin sets the layout for the MEDUSA visualization.
 *
 * @author keshav
 * @version $Id: MedusaVisualizationPanel.java,v 1.1 2007/06/15 17:12:31 keshav
 *          Exp $
 */
public class MedusaVisualizationPanel extends JPanel {

	private Log log = LogFactory.getLog(MedusaVisualizationPanel.class);

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private String defaultPath = "temp/medusa/dataset/output/";
	private String path = defaultPath + "run1/";

	private String rulesPath = path + "rules/";

	private List<String> rulesFiles = null;

	private Map<String, DSGeneMarker> dirtySelectedMarkerMap = null;
	private Map<String, DSGeneMarker> selectedMarkerMap = null;

	private JButton addSelectionsToSetButton = new JButton();
	private String addToSetButtonLabel = "Add To Set ";
	private JButton exportMotifsButton = new JButton();
	private JButton imageSnapshotButton = new JButton();
	private JButton screenSnapshotButton = new JButton();

	// private static final int COLUMN_WIDTH = 80;

	/**
	 *
	 * @param medusaData
	 */
	public MedusaVisualizationPanel(
			MedusaVisualComponent medusaVisualComponent, MedusaData medusaData, String outputDir) {
		super();
		path = defaultPath + outputDir +"/";
		rulesPath = path + "rules/";
		
		final MedusaVisualComponent visualComponent = medusaVisualComponent;

		JTabbedPane tabbedPane = new JTabbedPane();

		/* MOTIF PANEL */
		JPanel motifPanel = new JPanel();

		int i = 0;
		List<DSGeneMarker> targets = medusaData.getTargets();

		List<DSGeneMarker> regulators = medusaData.getRegulators();

		initSelectedMarkerMap(targets, regulators);

		addSelectionsToSetButton.setText(addToSetButtonLabel + "["
				+ dirtySelectedMarkerMap.size() + "]");
		addSelectionsToSetButton.setToolTipText(addToSetButtonLabel);

		exportMotifsButton.setText("Export Motifs");
		exportMotifsButton.setToolTipText("Export motifs discovered by Medusa");
		imageSnapshotButton.setText("Image Snapshot");
		imageSnapshotButton.setToolTipText("Image snapshot");

		screenSnapshotButton.setText("Screen Snapshot");
		screenSnapshotButton.setToolTipText("Screen snapshot");
		
		//if you want to active screen snapshot function again, you can comment following line.
		screenSnapshotButton.setVisible(false);

		double[][] targetMatrix = new double[targets.size()][];
		for (DSGeneMarker target : targets) {
			double[] data = medusaData.getArraySet().getRow(target);
			targetMatrix[i] = data;
			i++;
		}

		int j = 0;
		double[][] regulatorMatrix = new double[regulators.size()][];
		for (DSGeneMarker regulator : regulators) {
			double[] data = medusaData.getArraySet().getRow(regulator);
			regulatorMatrix[j] = data;
			j++;
		}

		List<String> targetNames = new ArrayList<String>();
		for (DSGeneMarker marker : targets) {
			targetNames.add(marker.getLabel());
		}

		List<String> regulatorNames = new ArrayList<String>();
		for (DSGeneMarker marker : regulators) {
			regulatorNames.add(marker.getLabel());
		}

		motifPanel.setLayout(new GridLayout(3, 2));

		/* dummy panel at position 0,0 of the grid */

		/* discrete hit or miss heat map at 1,0 */
		this.rulesFiles = new ArrayList<String>();

		for (int k = 0; k < medusaData.getMedusaCommand().getIter(); k++) {
			rulesFiles.add("rule_" + k + ".xml");
		}
		
		DiscreteHitOrMissHeatMapNamePanel hitOrMissNamePanel = new DiscreteHitOrMissHeatMapNamePanel(
				rulesPath, rulesFiles, targetNames, path);
		// motifPanel.add(hitOrMissPanel);
		hitOrMissNamePanel.setPreferredSize(new Dimension(rulesFiles.size()*15+25, 200));
		hitOrMissNamePanel.setParentPanel(tabbedPane);
		JScrollPane hitOrMissScrollNamePane = new JScrollPane();
		hitOrMissScrollNamePane.setPreferredSize(new Dimension(rulesFiles.size()*15+25, 200));
		hitOrMissScrollNamePane.getViewport().add(hitOrMissNamePanel);
		hitOrMissScrollNamePane.setVisible(true);
		motifPanel.add(hitOrMissScrollNamePane);	//0,0
		
//		JPanel dummyPanel0 = new JPanel();
//		motifPanel.add(new JScrollPane());

		/* regulator heat map at postion 0,1 */
		DiscreteHeatMapPanel regulatorHeatMap = new DiscreteHeatMapPanel(
				regulatorMatrix, 1, 0, -1, regulatorNames, false);
		// motifPanel.add(regulatorHeatMap);
		//TODO width of regulatorHeatMap should be more accurate.
		regulatorHeatMap.setPreferredSize(new Dimension(regulatorMatrix[0].length*15+25, regulatorMatrix.length*15+25));

		/* regulator labels at position 0,2 */
		FormLayout regulatorLabelLayout = new FormLayout("pref,60dlu", // columns
				"5dlu"); // add rows dynamically
		DefaultFormBuilder regulatorLabelBuilder = new DefaultFormBuilder(
				regulatorLabelLayout);
		regulatorLabelBuilder.nextRow();

		for (String name : regulatorNames) {
			final JCheckBox checkBox = new JCheckBox();
			checkBox.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					log.info("Regulator " + e.getActionCommand()
							+ " is selected? " + checkBox.isSelected());

					String markerLabel = e.getActionCommand();

					DSGeneMarker reg = selectedMarkerMap.get(markerLabel);

					if (checkBox.isSelected()) {
						dirtySelectedMarkerMap.put(markerLabel, reg);
						addSelectionsToSetButton.setText(addToSetButtonLabel
								+ "[" + dirtySelectedMarkerMap.size() + "]");
					}

					else {
						dirtySelectedMarkerMap.remove(markerLabel);
						addSelectionsToSetButton.setText(addToSetButtonLabel
								+ "[" + dirtySelectedMarkerMap.size() + "]");
					}
				}
			});
			checkBox.setText(name);
			checkBox.setSelected(true);
			regulatorLabelBuilder.append(checkBox);
			regulatorLabelBuilder.appendRow("10dlu");
		}

        JPanel regulatorHeatPanel = new JPanel();
        regulatorHeatPanel.setLayout(new BoxLayout(regulatorHeatPanel, BoxLayout.X_AXIS));
        regulatorHeatPanel.add(regulatorHeatMap);
        regulatorHeatPanel.setPreferredSize(regulatorHeatMap.getPreferredSize());
//        JPanel dummyPanel1 = new JPanel();
//        dummyPanel1.setPreferredSize(new Dimension(10, 100));
//        regulatorHeatPanel.add(dummyPanel1);
//        regulatorHeatPanel.add(regulatorLabelBuilder.getPanel());

/*
		JPanel regulatorHeatPanel = new JPanel(new BorderLayout());
		regulatorHeatPanel.add(regulatorHeatMap, BorderLayout.WEST);
		regulatorHeatPanel.add(regulatorLabelBuilder.getPanel(),
				BorderLayout.EAST);
*/

		JScrollPane regulatorHeatScrollPane = new JScrollPane();
		regulatorHeatScrollPane.setPreferredSize(regulatorHeatPanel.getPreferredSize());
//		regulatorHeatScrollPane.setPreferredSize(new Dimension(100, 200));
		/*regulatorHeatScrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);*/
		regulatorHeatScrollPane.getViewport().add(regulatorHeatPanel);
		regulatorHeatScrollPane.setVisible(true);
		regulatorHeatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);	//always activate this scroll bar, so 0,1 and 0,2 can sync without the differences of the scroll bar.
		motifPanel.add(regulatorHeatScrollPane);	//0,1

		JScrollPane regulatorCheckBoxScrollPane = new JScrollPane();
		regulatorCheckBoxScrollPane.setPreferredSize(new Dimension(100, 200));
		regulatorCheckBoxScrollPane.getViewport().add(regulatorLabelBuilder.getPanel());
		regulatorCheckBoxScrollPane.setVisible(true);
		regulatorCheckBoxScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);	//always activate this scroll bar, so 0,1 and 0,2 can sync without the differences of the scroll bar.
		regulatorCheckBoxScrollPane.getVerticalScrollBar().setModel(regulatorHeatScrollPane.getVerticalScrollBar().getModel());
		motifPanel.add(regulatorCheckBoxScrollPane);	//0,2
		
//		motifPanel.add(regulatorLabelBuilder.getPanel());
		// motifPanel.add(new JPanel());

		DiscreteHitOrMissHeatMapPanel hitOrMissPanel = new DiscreteHitOrMissHeatMapPanel(
				rulesPath, rulesFiles, targetNames, path);
		// motifPanel.add(hitOrMissPanel);
		hitOrMissPanel.setPreferredSize(new Dimension(rulesFiles.size()*15+25, targetMatrix.length*15+25));
		hitOrMissPanel.setParentPanel(tabbedPane);
		JScrollPane hitOrMissScrollPane = new JScrollPane();
		hitOrMissScrollPane.setPreferredSize(new Dimension(rulesFiles.size()*15+25, targetMatrix.length*15+25));
		hitOrMissScrollPane.getViewport().add(hitOrMissPanel);
		hitOrMissScrollPane.setVisible(true);
		hitOrMissScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);	//always activate this scroll bar, so 1,0 and 1,1 can sync without the differences of the scroll bar. 
		motifPanel.add(hitOrMissScrollPane);	//1,0

		/* target heat map at postion 1,1 */
		DiscreteHeatMapPanel targetHeatMap = new DiscreteHeatMapPanel(
				targetMatrix, 1, 0, -1, targetNames, false, 15);
		// motifPanel.add(targetHeatMap);
		//TODO: width of targetHeatMap should be more accurate
		targetHeatMap.setPreferredSize(new Dimension(regulatorMatrix[0].length*15+25, targetMatrix.length*15+25));

		/* target labels at position 1,2 */
		FormLayout targetLabelLayout = new FormLayout("pref,60dlu", // columns
				"5dlu"); // add rows dynamically
		DefaultFormBuilder targetLabelBuilder = new DefaultFormBuilder(
				targetLabelLayout);
		targetLabelBuilder.nextRow();

		for (String name : targetNames) {
			final JCheckBox checkBox = new JCheckBox();
			checkBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					log.info("Target " + e.getActionCommand()
							+ " is selected? " + checkBox.isSelected());

					String markerLabel = e.getActionCommand();

					DSGeneMarker target = selectedMarkerMap.get(markerLabel);

					if (checkBox.isSelected()) {
						dirtySelectedMarkerMap.put(markerLabel, target);
						addSelectionsToSetButton.setText(addToSetButtonLabel
								+ "[" + dirtySelectedMarkerMap.size() + "]");
					} else {
						dirtySelectedMarkerMap.remove(markerLabel);
						addSelectionsToSetButton.setText(addToSetButtonLabel
								+ "[" + dirtySelectedMarkerMap.size() + "]");
					}
				}
			});
			checkBox.setText(name);
			checkBox.setSelected(true);
			targetLabelBuilder.append(checkBox);
			targetLabelBuilder.appendRow("10dlu");
		}
		// motifPanel.add(targetLabelBuilder.getPanel());
		JPanel targetHeatPanel = new JPanel(new BorderLayout());
        targetHeatPanel.setLayout(new BoxLayout(targetHeatPanel, BoxLayout.X_AXIS));
        targetHeatPanel.add(targetHeatMap);
        targetHeatPanel.setPreferredSize(targetHeatMap.getPreferredSize());
//        JPanel dummyPanel2 = new JPanel();
//        dummyPanel2.setPreferredSize(new Dimension(10, 100));
//        targetHeatPanel.add(dummyPanel2);
//		targetHeatPanel.add(targetLabelBuilder.getPanel());

		JScrollPane targetHeatScrollPane = new JScrollPane();
		// scrollPane1.setLayout(new BorderLayout());
		targetHeatScrollPane.setPreferredSize(new Dimension(100, 200));
		/*targetHeatScrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);*/
		targetHeatScrollPane.getViewport().add(targetHeatPanel);
		targetHeatScrollPane.setVisible(true);
		targetHeatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);	//always activate this scroll bar, so 1,0 and 1,1 can sync without the differences of the scroll bar.
		motifPanel.add(targetHeatScrollPane);	//1,1

		JScrollPane targetCheckBoxScrollPane = new JScrollPane();
		targetCheckBoxScrollPane.setPreferredSize(new Dimension(100, 200));
		targetCheckBoxScrollPane.getViewport().add(targetLabelBuilder.getPanel());
		targetCheckBoxScrollPane.setVisible(true);
		targetCheckBoxScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);	//always activate this scroll bar, so 1,1 and 1,2 can sync without the differences of the scroll bar.
		targetCheckBoxScrollPane.getVerticalScrollBar().setModel(hitOrMissScrollPane.getVerticalScrollBar().getModel());
		motifPanel.add(targetCheckBoxScrollPane);	//1,2

		// motifPanel.add(new JPanel());
		hitOrMissScrollNamePane.getHorizontalScrollBar().setModel(hitOrMissScrollPane.getHorizontalScrollBar().getModel());
		regulatorHeatScrollPane.getHorizontalScrollBar().setModel(targetHeatScrollPane.getHorizontalScrollBar().getModel());
		targetHeatScrollPane.getVerticalScrollBar().setModel(hitOrMissScrollPane.getVerticalScrollBar().getModel());
		/* dummy panel at 2,0 so we can align the buttons (below) */
		JPanel dummyPanel11 = new JPanel();
		motifPanel.add(dummyPanel11);

		/* add buttons at 2,1 */
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(imageSnapshotButton);
		buttonPanel.add(screenSnapshotButton);
		
		buttonPanel.add(exportMotifsButton);
		buttonPanel.add(addSelectionsToSetButton);

		imageSnapshotButton.addActionListener(new ActionListener() {

			/*
			 * (non-Javadoc)
			 *
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(ActionEvent e) {
				visualComponent.publishImageSnapshot();
			}

		});

		screenSnapshotButton.addActionListener(new ActionListener() {

			/*
			 * (non-Javadoc)
			 *
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(ActionEvent e) {
				visualComponent.publishScreenSnapshot();
			}

		});

		exportMotifsButton.addActionListener(new ActionListener() {

			/*
			 * (non-Javadoc)
			 *
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(ActionEvent e) {
				ArrayList<SerializedRule> srules = MedusaUtil
						.getSerializedRules(rulesFiles, rulesPath);

				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File("."));

				int returnVal = chooser.showSaveDialog(MedusaVisualizationPanel.this);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    File chosenFile = chooser.getSelectedFile();

                    visualComponent.exportMotifs(srules, chosenFile
                            .getAbsolutePath());
                }
			}

		});

		addSelectionsToSetButton.addActionListener(new ActionListener() {

			/*
			 * (non-Javadoc)
			 *
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				// add all values in selected selectedMarkerMap
				Collection<DSGeneMarker> selectedMarkers = dirtySelectedMarkerMap
						.values();
				DSPanel<DSGeneMarker> panel = new CSPanel<DSGeneMarker>();
				panel.addAll(selectedMarkers);
				visualComponent
						.publishSubpanelChangedEvent(new SubpanelChangedEvent(
								DSGeneMarker.class, panel,
								org.geworkbench.events.SubpanelChangedEvent.NEW));

			}
		});

		motifPanel.add(buttonPanel);

		tabbedPane.add("Motif", motifPanel);

		setLayout(new BorderLayout());
		add(tabbedPane, BorderLayout.CENTER);
		this.revalidate();

	}

	/**
	 * 
	 * @param targets
	 * @param regulators
	 * @return
	 */
	private void initSelectedMarkerMap(List<DSGeneMarker> targets,
			List<DSGeneMarker> regulators) {

		dirtySelectedMarkerMap = new HashMap<String, DSGeneMarker>();
		selectedMarkerMap = new HashMap<String, DSGeneMarker>();
		for (DSGeneMarker reg : regulators) {
			dirtySelectedMarkerMap.put(reg.getLabel(), reg);
			selectedMarkerMap.put(reg.getLabel(), reg);
		}
		for (DSGeneMarker tar : targets) {
			dirtySelectedMarkerMap.put(tar.getLabel(), tar);
			selectedMarkerMap.put(tar.getLabel(), tar);
		}
	}
}
