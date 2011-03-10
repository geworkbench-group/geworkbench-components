package org.geworkbench.components.mindy;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.biocollections.views.CSMicroarraySetView;
import org.geworkbench.bison.datastructure.biocollections.views.DSMicroarraySetView;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMicroarray;
import org.geworkbench.bison.datastructure.complex.panels.DSPanel;
import org.geworkbench.builtin.projects.ProjectPanel;
import org.geworkbench.builtin.projects.ProjectSelection;
import org.geworkbench.builtin.projects.ProjectTreeNode;
import org.geworkbench.engine.config.VisualPlugin;
import org.geworkbench.engine.management.AcceptTypes;
import org.geworkbench.engine.management.Asynchronous;
import org.geworkbench.engine.management.Publish;
import org.geworkbench.engine.management.Subscribe;
import org.geworkbench.events.GeneSelectorEvent;
import org.geworkbench.events.ImageSnapshotEvent;
import org.geworkbench.events.ProjectEvent;
import org.geworkbench.events.SubpanelChangedEvent;
import org.geworkbench.util.ProgressBar;
import org.geworkbench.util.pathwaydecoder.mutualinformation.MindyData;
import org.geworkbench.util.pathwaydecoder.mutualinformation.MindyDataSet;

/**
 * @author mhall
 * @author ch2514
 * @author oshteynb
 * @version $Id$
 */
@AcceptTypes(MindyDataSet.class)
public class MindyVisualComponent implements VisualPlugin, java.util.Observer {
	static Log log = LogFactory.getLog(MindyVisualComponent.class);

	private MindyDataSet dataSet;

	private JPanel plugin;

	private List<DSGeneMarker> selectedMarkers;

	private DSPanel<DSGeneMarker> selectorPanel;

	private HashMap<ProjectTreeNode, MindyPlugin> ht;

	private ProgressBar progressBar = null;

	private boolean stopDrawing = false;

	/**
	 * Constructor. Includes a place holder for a MINDY result view (i.e. class
	 * MindyPlugin).
	 *
	 */
	public MindyVisualComponent() {
		// Just a place holder
		ht = new HashMap<ProjectTreeNode, MindyPlugin>(5);
		plugin = new JPanel(new BorderLayout());
		selectorPanel = null;
		selectedMarkers = null;
	}

	/**
	 * @return The MINDY result view component (of class MindyPlugin)
	 */
	public Component getComponent() {
		return plugin;
	}

	/**
	 * Receives the general ProjectEvent from the framework. Creates MINDY's
	 * data set based on data from the ProjectEvent.
	 *
	 * @param projectEvent
	 * @param source -
	 *            source of the ProjectEvent
	 */
	@SuppressWarnings("unchecked")
	@Subscribe(Asynchronous.class)
	public void receive(ProjectEvent projectEvent, Object source) {
		/*  added to preconditions checks,
		 *  until preconditions issue is resolved  */
        if (projectEvent == null) {
        	return;
        }

        if (source == null) {
        	return;
        }

		DSDataSet data = projectEvent.getDataSet();
		if ( data == null || !(data instanceof MindyDataSet) ) 
			return;
		
        log.debug("MINDY received project event.");

		MindyPlugin mindyPlugin = null;

			/*
			 * moved some code inside if statement and after checks for preconditions
			 * to leave code in usable state in case there were null pointers
			 */

			if (plugin == null) {
				return;
			}

			ProjectSelection ps = ((ProjectPanel) source).getSelection();
			if (ps == null) {
				return;
			}

			plugin.removeAll();

			ProjectTreeNode node = ps.getSelectedNode();

			log.debug("event is from node [" + node.toString() + "]");
			// Check to see if the hashtable has a mindy plugin associated with
			// the selected project tree node
			if (ht.containsKey(node)) {
				// if so, set mindyPlugin to the one stored in the hashtable
				mindyPlugin = ht.get(node);
				log.debug("plugin already exists for node ["
						+ node.toString() + "]");
			} else {
				// if not, create a brand new mindyPlugin, add to the hashtable
				// (with key=selected project tree node)
				if (dataSet != data) {
					dataSet = ((MindyDataSet) data);

					log.debug("Creating new mindy plugin for node ["
							+ node.toString() + "]");
					// Create mindy gui and display an indeterminate progress
					// bar in the foreground
					progressBar = ProgressBar
							.create(ProgressBar.INDETERMINATE_TYPE);
					progressBar.addObserver(this);
					progressBar.setTitle("MINDY");
					progressBar.setMessage("Creating Display");
					progressBar.start();

					MindyData mindyData = dataSet.getData();
					mindyPlugin = new MindyPlugin(mindyData, this);
					mindyPlugin.populateTableTab();
					mindyPlugin.populateModulatorModel();

					// Incorporate selections from marker set selection panel
					DSMicroarraySetView<DSGeneMarker, DSMicroarray> maView = new CSMicroarraySetView<DSGeneMarker, DSMicroarray>(
							dataSet.getData().getArraySet());
					if (stopDrawing) {
						stopDrawing = false;
						progressBar.stop();
						log.warn("Cancelling Mindy GUI.");
						return;
					}
					maView.useMarkerPanel(true);

					// Register the mindy plugin with our hashtable for keeping
					// track
					if (mindyPlugin != null)
						ht.put(node, mindyPlugin);
					else {
						log
								.error("Failed to create MINDY plugin for project node: "
										+ node);
						return;
					}
					if (stopDrawing) {
						stopDrawing = false;
						progressBar.stop();
						log.warn("Cancelling Mindy GUI.");
						return;
					}
				}

			}
			// Display the plugin
			plugin.add(mindyPlugin, BorderLayout.CENTER);
			progressBar.stop();
			plugin.revalidate();
			plugin.repaint();
	}

	/**
	 * Receives GeneSelectorEvent from the framework. Extracts markers in the
	 * selected marker sets from the Selector Panel.
	 *
	 * @param e -
	 *            GeneSelectorEvent
	 * @param source -
	 *            source of the GeneSelectorEvent
	 */
	@Subscribe(Asynchronous.class)
	public void receive(GeneSelectorEvent e, Object source) {
		if (dataSet != null) {

			if (e.getPanel() != null) {
				log.debug("Received gene selector event ");
				selectorPanel = e.getPanel();

				Iterator<MindyPlugin> it = ht.values().iterator();
				while (it.hasNext()) {
					log
							.debug("***received gene selector event::calling resetTargetSetModel: ");
					MindyPlugin p = it.next();
					p.setFilteringSelectorPanel(selectorPanel);
					p.getTableTab().setFirstColumnWidth(30);
//					((MindyPlugin) it.next()).resetTargetSetModel(selectorPanel);
				}
			} else
				log

		.debug("Received Gene Selector Event: Selection panel sent was null");

			{

		}

		} else {
			log
					.debug("Received Gene Selector Event: Dataset in this component is null");
		}

	}

	/**
	 * Publish SubpanelChangedEvent to the framework to add selected markers to
	 * the marker set(s) on the Selector Panel.
	 *
	 * @param e -
	 *            SubpanelChangedEvent
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Publish
	public SubpanelChangedEvent publishSubpanelChangedEvent(
			SubpanelChangedEvent e) {
		return e;
	}

	/**
	 * Publish ImageSnapshotEvent to the framework to capture an image of the
	 * heat map.
	 *
	 * @param heatmap -
	 *            MINDY's heat map panel
	 * @return ImageSnapshotEvent
	 */
	@Publish
	public ImageSnapshotEvent createImageSnapshot(Component heatmap) {
		try{
			Dimension panelSize = heatmap.getSize();
			BufferedImage image = new BufferedImage(panelSize.width,
					panelSize.height, BufferedImage.TYPE_INT_RGB);
			Graphics g = image.getGraphics();
			heatmap.print(g);
			ImageIcon icon = new ImageIcon(image, "MINDY Heat Map");
			org.geworkbench.events.ImageSnapshotEvent event = new org.geworkbench.events.ImageSnapshotEvent(
					"MINDY Heat Map", icon,
					org.geworkbench.events.ImageSnapshotEvent.Action.SAVE);
			return event;
		} catch (OutOfMemoryError err){
			JOptionPane.showMessageDialog(null, "There is not enough memory for this operation.",
					"Warning", JOptionPane.WARNING_MESSAGE);
			log.error("Not enough memory for image snapshot:" + err.getMessage());
			return null;
		}
	}

	DSPanel<DSGeneMarker> getSelectorPanel() {
		return this.selectorPanel;
	}

	List<DSGeneMarker> getSelectedMarkers() {
		return this.selectedMarkers;
	}

	public void update(java.util.Observable ob, Object o) {
		log.debug("initiated close");
		stopDrawing = true;
		progressBar.stop();
	}
}