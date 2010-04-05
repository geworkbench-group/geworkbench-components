package org.geworkbench.components.cytoscape;

import giny.model.Node;
import giny.util.ForceDirectedLayout;
import giny.util.SpringEmbeddedLayouter;
import giny.view.GraphViewChangeEvent;
import giny.view.GraphViewChangeListener;
import giny.view.NodeView;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.datastructure.bioobjects.markers.annotationparser.AnnotationParser;
import org.geworkbench.bison.datastructure.bioobjects.markers.annotationparser.GeneOntologyUtil;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMicroarray;
import org.geworkbench.bison.datastructure.complex.panels.CSPanel;
import org.geworkbench.bison.datastructure.complex.panels.DSPanel;
import org.geworkbench.engine.config.VisualPlugin;
import org.geworkbench.engine.management.AcceptTypes;
import org.geworkbench.engine.management.Publish;
import org.geworkbench.engine.management.Subscribe;
import org.geworkbench.events.GeneTaggedEvent;
import org.geworkbench.events.ProjectNodeRemovedEvent;
import org.geworkbench.util.Util;
import org.geworkbench.util.annotation.Gene;
import org.geworkbench.util.pathwaydecoder.mutualinformation.AdjacencyMatrix;
import org.geworkbench.util.pathwaydecoder.mutualinformation.AdjacencyMatrixDataSet;

import com.jgoodies.plaf.FontSizeHints;
import com.jgoodies.plaf.Options;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.giny.FingCyNetwork;
import cytoscape.init.CyInitParams;
import cytoscape.util.CytoscapeToolBar;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.NetworkPanel;
import cytoscape.view.cytopanels.BiModalJSplitPane;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.ShapeNodeRealizer;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.calculators.Calculator;
import cytoscape.visual.mappings.DiscreteMapping;
import ding.view.DNodeView;

/**
 * 
 * Visual component to display graph using cytoscape.
 * 
 * @author manjunath
 * @author yc2480
 * @version $Id$
 */
@SuppressWarnings("deprecation")
@AcceptTypes( { AdjacencyMatrixDataSet.class })
public class CytoscapeWidget implements VisualPlugin {

	private class GenewaysNetworkListener implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent evnt) {
			if (evnt.getPropertyName() == cytoscape.view.CytoscapeDesktop.NETWORK_VIEW_CREATED) {
				Cytoscape.getCurrentNetworkView().addNodeContextMenuListener(
						new ExpandMenuListener(CytoscapeWidget.this));
			}
			log.debug(evnt.getPropertyName());

			/*
			 * This particular case in cytoscape needs context class loader to
			 * be set. It is in a separate thread, we cannot and don't need to
			 * reset the context class loader.
			 */
			StackTraceElement[] stackTrace = Thread.currentThread()
					.getStackTrace();
			boolean isTaskWrapperRun = false;
			boolean isWriteSessionToDisk = false;
			for (StackTraceElement e : stackTrace) {
				if (e
						.toString()
						.equals(
								"cytoscape.data.writers.CytoscapeSessionWriter.writeSessionToDisk(CytoscapeSessionWriter.java:240)"))
					isWriteSessionToDisk = true;
				if (e
						.toString()
						.equals(
								"cytoscape.task.util.TaskWrapper.run(TaskManager.java:125)"))
					isTaskWrapperRun = true;
			}
			/* to make sure it does not happen for unintended cases */
			if (isTaskWrapperRun && isWriteSessionToDisk) {
				Thread.currentThread().setContextClassLoader(
						CytoscapeDesktop.class.getClassLoader());
			}
		}

	}

	static private Log log = LogFactory.getLog(CytoscapeWidget.class);
	private AdjacencyMatrixDataSet adjSet = null;
	private HashSet<String> dataSetIDs = new HashSet<String>();
	private DiscreteMapping nodeDm = null, edgeDm = null;

	private VisualStyle sample1VisualStyle;

	private int shapeIndex = 0;
	private byte[] shapes = { ShapeNodeRealizer.RECT,
			ShapeNodeRealizer.DIAMOND, ShapeNodeRealizer.HEXAGON,
			ShapeNodeRealizer.OCTAGON, ShapeNodeRealizer.PARALLELOGRAM,
			ShapeNodeRealizer.ROUND_RECT, ShapeNodeRealizer.TRIANGLE,
			ShapeNodeRealizer.TRAPEZOID_2 };

	private boolean uiSetup = false;

	private CyNetworkView view = null;

	// these are default because ExpandMenuListener needs access
	AdjacencyMatrix adjMatrix = null;
	CyNetwork cytoNetwork = null;
	JProgressBar jProgressBar = new JProgressBar();
	DSMicroarraySet<? extends DSMicroarray> maSet = null;
	boolean publishEnabled = true;
	MultiMap<String, Integer> geneIdToMarkerIdMap = new MultiHashMap<String, Integer>();

	public CytoscapeWidget() {
		UIManager.put(Options.USE_SYSTEM_FONTS_APP_KEY, Boolean.TRUE);
		Options.setGlobalFontSizeHints(FontSizeHints.MIXED);
		Options.setDefaultIconSize(new Dimension(18, 18));

		init();
		publishEnabled = true;
	}

	/**
	 * <code>VisualPlugin</code> method
	 * 
	 * @return <code>Component</code> the view for this component
	 */
	public Component getComponent() {
		JMenuBar menuBar = Cytoscape.getDesktop().getCyMenus().getMenuBar();
		JMenu fileMenu = menuBar.getMenu(0);
		fileMenu.remove(fileMenu.getItemCount() - 1); // remove the last item
		// quit
		Container contentPane = Cytoscape.getDesktop().getContentPane();
		if (!uiSetup) {
			Component[] components = contentPane.getComponents();
			contentPane.removeAll();
			Box box = Box.createVerticalBox();
			Component comp1 = menuBar;
			BiModalJSplitPane comp2 = (BiModalJSplitPane) components[0];
			CytoscapeToolBar comp3 = (CytoscapeToolBar) components[1];
			Component comp4 = jProgressBar;

			menuBar.setAlignmentX(Component.LEFT_ALIGNMENT);
			comp2.setAlignmentX(Component.LEFT_ALIGNMENT);
			comp3.setAlignmentX(Component.LEFT_ALIGNMENT);
			box.add(comp1);
			box.add(comp2);
			box.add(comp3);
			box.add(comp4);

			contentPane.add(box);
			jProgressBar.setVisible(false);
			uiSetup = true;
		}

		return contentPane;
	}

	@Publish
	public org.geworkbench.events.SubpanelChangedEvent<DSGeneMarker> publishSubpanelChangedEvent(
			org.geworkbench.events.SubpanelChangedEvent<DSGeneMarker> event) {
		return event;
	}

	/**
	 * Update selection in visualization when the gene selection is changed
	 * 
	 * @param e
	 *            GeneSelectorEvent
	 */
	@Subscribe
	public void receive(GeneTaggedEvent e, Object source) {
		DSPanel<DSGeneMarker> panel = e.getPanel();
		if (panel == null) {
			log.error("panel is null in "
					+ new Exception().getStackTrace()[0].getMethodName());
			return;
		}

		List<String> selected = new ArrayList<String>();
		for (DSGeneMarker m : panel) {
			String name = m.getShortName().trim().toUpperCase();
			if (name.contains(Gene.genesSeparator)) {
				String[] names = name.split(Gene.genesSeparator);
				for (int i = 0; i < names.length; i++) {
					selected.add(names[i]);
				}
			} else {
				selected.add(m.getShortName().trim().toUpperCase());
			}
			log.debug(m.getShortName().trim().toUpperCase());
		}

		Iterator<?> iter = Cytoscape.getCurrentNetworkView()
				.getNodeViewsIterator();
		publishEnabled = false;
		while (iter.hasNext()) {
			NodeView nodeView = (NodeView) iter.next();
			String nodeLabel = nodeView.getLabel().getText().trim()
					.toUpperCase();
			log.debug("Check if " + selected + " contains " + nodeLabel);
			if (selected.contains(nodeLabel)) {
				nodeView.select();
				log.debug("^^^Select^^^");
			} else
				nodeView.unselect();
		}
		this.getComponent().repaint();
		publishEnabled = true;
	}

	/**
	 * receiveProjectSelection
	 * 
	 * @param e
	 *            ProjectEvent
	 */
	@Subscribe
	public void receive(org.geworkbench.events.ProjectEvent e, Object source) {

		DSDataSet<?> dataSet = e.getDataSet();
		if (dataSet instanceof AdjacencyMatrixDataSet) {
			adjSet = (AdjacencyMatrixDataSet) dataSet;
			maSet = adjSet.getMatrix().getMicroarraySet();
			getGeneIdToNameMap();

			if (maSet != null) {				 
				geneIdToMarkerIdMap = AnnotationParser
						.getGeneIdToMarkerIDMapping(maSet);
			}
			boolean found = false;
			String foundID = null;
			if (!dataSetIDs.contains(adjSet.getID())) {
				dataSetIDs.add(adjSet.getID());
			} else {
				Set<?> networks = Cytoscape.getNetworkSet();
				for (Iterator<?> iterator = networks.iterator(); iterator
						.hasNext();) {
					Object next = iterator.next();
					// String id = (String) iterator.next();
					String id = "";
					if (next instanceof String) {
						id = (String) next;
					} else if (next instanceof FingCyNetwork) {
						id = ((FingCyNetwork) next).getIdentifier();
					}
					CyNetwork network = Cytoscape.getNetwork(id);
					String title = network.getTitle();
					log.debug("compare: " + title + " AND "
							+ adjSet.getNetworkName());
					if (title.equals(adjSet.getNetworkName())) {
						found = true;
						foundID = id;
						break;
					}
				}
			}
			if (!found) {
				receiveMatrix();
			} else {
				Cytoscape.getDesktop().getNetworkPanel().focusNetworkNode(
						foundID);
			}

		}
	}

	@SuppressWarnings("unchecked")
	@Subscribe
	public void receive(ProjectNodeRemovedEvent event, Object source) {
		DSDataSet dataSet = event.getAncillaryDataSet();
		if (!(dataSet instanceof AdjacencyMatrixDataSet)) {
			// if the event is published by other types, do nothing.
			return;
		}

		AdjacencyMatrixDataSet adjMatrixDataSet = (AdjacencyMatrixDataSet) dataSet;
		Set<?> networkSet = Cytoscape.getNetworkSet();
		for (Object network : networkSet) {
			String id = null;
			if (network instanceof FingCyNetwork) {
				id = ((FingCyNetwork) network).getIdentifier();
				CyNetwork cyNetwork = Cytoscape.getNetwork(id);
				if (cyNetwork.getTitle().equals(
						adjMatrixDataSet.getNetworkName())) {
					Cytoscape.destroyNetwork(cyNetwork);
					return;
				}
			} else {
				log
						.error("Cytoscape network set contains something that is not FingCyNetwork.");
			}
		}
		log
				.error("No network in the Cytoscape network set matches ProjectNodeRemovedEvent's dataSet name.");
	}

	// this static method is copied from cutenetManager so we don't depend on
	// that class any more
	static private Set<String> getSwissProtIDsForMarkers(Set<String> markerIDs) {
		HashSet<String> results = new HashSet<String>();
		if (markerIDs != null) {
			for (String id : markerIDs) {
				try {
					results.addAll(AnnotationParser.getSwissProtIDs(id));
				} catch (Exception e) {
					continue;
				}
			}
		}
		return results;
	}

	public void memoryUsage() {
		Runtime rtime = Runtime.getRuntime();
		System.out.println("Total Memory---->" + rtime.totalMemory());
		System.out.println("Free Memory---->" + rtime.freeMemory());
		System.out.println("Used Memory---->"
				+ (rtime.totalMemory() - rtime.freeMemory()));
	}

	private void createEdge(CyNode n1, CyNode n2, String geneId1,
			String geneId2, String type) {
		// process the edge connecting geneId and key
		CyEdge e = null;

		if (type != null) {
			/*
			 * if (geneId > id2) { e =
			 * Cytoscape.getCyEdge(String.valueOf(geneId),
			 * String.valueOf(geneId) + "." + type + "." + String.valueOf(id2),
			 * String.valueOf(id2), type); } else { e =
			 * Cytoscape.getCyEdge(String.valueOf(id2), String.valueOf(id2) +
			 * "." + type + "." + String.valueOf(geneId),
			 * String.valueOf(geneId), type); }
			 */

			e = Cytoscape.getCyEdge(n1.getIdentifier(), geneId1 + "." + type
					+ "." + geneId2, n2.getIdentifier(), type);
			// Aracne result will not have an type, so we should
			// not need to check it here.
			if (!cytoNetwork.edgeExists(n2, n1))
				cytoNetwork.addEdge(e);

			e.setIdentifier(geneId1 + "." + type + "." + geneId2 + "/"
					+ n1.getIdentifier() + "-" + n2.getIdentifier());

		} else {
			/*
			 * if (geneId < id2) { e =
			 * Cytoscape.getCyEdge(String.valueOf(geneId),
			 * String.valueOf(geneId) + ".pp." + String.valueOf(id2),
			 * String.valueOf(id2), ""); } else { e =
			 * Cytoscape.getCyEdge(String.valueOf(geneId),
			 * String.valueOf(geneId) + ".pp." + String.valueOf(id2),
			 * String.valueOf(id2), ""); }
			 */
			// e = Cytoscape.getCyEdge(String.valueOf(geneId),
			// String.valueOf(geneId) + ".pp." +
			// String.valueOf(id2), String.valueOf(id2), "");
			e = Cytoscape.getCyEdge(n1.getIdentifier(), geneId1 + ".pp."
					+ geneId2, n2.getIdentifier(), "");
			// For Aracne edges, if a reverse edge exist, we
			// skip it.
			// For Geneways edges,
			if (!cytoNetwork.edgeExists(n2, n1)) // Aracne
				// result
				// will not
				// have an
				// type, so
				// we should
				// check it
				// here.
				cytoNetwork.addEdge(e);
			e.setIdentifier(geneId1 + ".pp." + geneId2 + "/"
					+ n1.getIdentifier() + "-" + n2.getIdentifier());

		}
		try {

			/*
			 * Cytoscape.getEdgeAttributes() .setAttribute(e.getIdentifier(),
			 * "attr1", new Double(v12));
			 */
			log.debug("edge " + geneId1 + "-" + geneId2 + ":"
					+ n1.getIdentifier() + "-" + n2.getIdentifier());

			if (type != null) {
				Cytoscape.getEdgeAttributes().setAttribute(e.getIdentifier(),
						"type", type);
				// Cytoscape.getEdgeAttributes().setAttribute(
				// e.getIdentifier(), "interaction", type);
				// getRandomCorlor();
				// edgeDm.putMapValue(type, getRandomCorlor());
				if (edgeDm.getMapValue(type) == null) {
					edgeDm.putMapValue(type, getRandomCorlor());

				}

			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private CyNode createNode(String geneIdStr,
			HashMap<String, String> geneIdToNameMap) {
		boolean isInSelectedMicroarray = true;
		Integer serialId = null;

		if (!geneIdToNameMap.containsKey(geneIdStr)) {
			serialId = new Integer(geneIdStr);
		} else {
			isInSelectedMicroarray = false;

		}

		boolean n1new = true;
		CyNode cyNode = Cytoscape.getCyNode(geneIdStr);

		String cp1 = null;

		DSGeneMarker marker1 = null;

		if (isInSelectedMicroarray == true) {
			marker1 = (DSGeneMarker) maSet.getMarkers().get(serialId);
			if (marker1.getGeneName().equals(""))
				cp1 = marker1.getLabel();
			else
				cp1 = marker1.getGeneName();
		}

		/*
		 * //if we already have this node in name, we use that one if (n1 ==
		 * null) if(cp1 != null) n1 = Cytoscape.getCyNode(cp1.getName()); //if
		 * it doesn't exist, we create one. if we get name, we create by name,
		 * if not, we create by number if (n1 == null) if(cp1 == null) n1 =
		 * Cytoscape.getCyNode(String.valueOf(geneId), true); else n1 =
		 * Cytoscape.getCyNode(cp1.getName(), true); else n1new = false;
		 */
		// if there's some node has the same name, even we
		// already got a node, we need to use only one node.
		if (cp1 != null) {
			if (cyNode != null)
				if (cytoNetwork.containsNode(cyNode)) {
					cytoNetwork.removeNode(serialId, true);
					log.debug("I removed " + serialId);
				}
			cyNode = Cytoscape.getCyNode(cp1);
		}

		if (cyNode == null) {
			cyNode = Cytoscape.getCyNode(geneIdStr, true);
			log.debug("I create " + cyNode.getIdentifier());
			if (cp1 == null) {
				String n1GeneName = geneIdToNameMap.get(geneIdStr);
				if (n1GeneName != null && !n1GeneName.trim().equals(""))
					cyNode.setIdentifier(n1GeneName);
				else {
					cyNode.setIdentifier(geneIdStr);
				}
			} else {
				cyNode.setIdentifier(cp1);
				log.debug("I name it " + serialId + cyNode.getIdentifier());
			}
		} else {
			n1new = false;
			if (cp1 != null) {
				cyNode.setIdentifier(cp1);
				log.debug("I got " + cyNode.getIdentifier() + " for "
						+ geneIdStr);
			} else {
				String n1GeneName = geneIdToNameMap.get(geneIdStr);
				if (n1GeneName != null && !n1GeneName.trim().equals(""))
					cyNode.setIdentifier(n1GeneName);
				else
					cyNode.setIdentifier(geneIdStr);
			}
		}

		try {

			
			if (marker1 != null) {
				Cytoscape.getNodeAttributes().setAttribute(
						cyNode.getIdentifier(), "geneID", marker1.getGeneId());
                
                Cytoscape.getNodeAttributes().setAttribute(cyNode.getIdentifier(),
					"geneName", cyNode.getIdentifier());
                
				Cytoscape.getNodeAttributes().setAttribute(
						cyNode.getIdentifier(), "markerName",
						marker1.getLabel());
			} else {
				try {
					Cytoscape.getNodeAttributes().setAttribute(
							cyNode.getIdentifier(), "geneID",
							new Integer(geneIdStr));
				    Cytoscape.getNodeAttributes().setAttribute(cyNode.getIdentifier(),
					"geneName", cyNode.getIdentifier());
				    
				} catch (NumberFormatException ne) {
					// if geneIdStr is not an integer, just ignore it.
				}
			}

			List<String> spIDs = new Vector<String>();

			if (isInSelectedMicroarray == true) {
				try {
					DSGeneMarker marker = (DSGeneMarker) maSet.getMarkers()
							.get(serialId);
					HashSet<String> selectedMarkerIDs = new HashSet<String>();
					selectedMarkerIDs.add(marker.getLabel());
					Set<String> swissProtIDs = getSwissProtIDsForMarkers(selectedMarkerIDs);
					spIDs.addAll(swissProtIDs);
				} catch (NullPointerException npe) {
					log
							.error("unexpected null pointer in createNode of CytoscapeWidget");
				}
			}

			if (marker1 != null) {
				String geneType = GeneOntologyUtil.getOntologyUtil()
						.checkMarkerFunctions(marker1);

				if (geneType == null || geneType.trim().equals(""))
					geneType = "non K/P/TF, in microarray set";

				Cytoscape.getNodeAttributes().setAttribute(
						cyNode.getIdentifier(), "geneType", geneType);
				if (nodeDm.getMapValue(geneType.trim()) == null) {
					nodeDm.putMapValue(geneType.trim(), shapes[++shapeIndex]);

				}

			} else {
				// Cytoscape.getNodeAttributes().setAttribute(n1.getIdentifier(),
				// "markerName", "not in our marker list");
				// System.out.println(geneIdStr + " is not in our marker list");
			}
			Cytoscape.getNodeAttributes().setListAttribute(
					cyNode.getIdentifier(), "swissprotIDs", spIDs);
			Cytoscape.getNodeAttributes().setAttribute(cyNode.getIdentifier(),
					"FromActionsCount", 0);
			Cytoscape.getNodeAttributes().setAttribute(cyNode.getIdentifier(),
					"ToActionsCount", 0);
			Cytoscape.getNodeAttributes().setAttribute(cyNode.getIdentifier(),
					"ActionsCount", 0);
		} catch (Exception e) {
			// we only try to add what we can add, if there's no
			// data, we add nothing
		}
		if (n1new) {
			cytoNetwork.addNode(cyNode);
			log.debug("I add " + cyNode.getIdentifier());
		}
		return cyNode;
	}

	private void createSubNetwork(int serialId,
			HashMap<String, String> geneIdToNameMap, double threshold, int level) {

		if (level == 0) {
			return;
		}

		HashMap<Integer, HashMap<Integer, Float>> geneRows = adjMatrix
				.getGeneRows();
		HashMap<Integer, String> interactionGeneRows = adjMatrix
				.getInteractionMap().get(serialId);
		HashMap<Integer, Float> map = geneRows.get((Integer) serialId);

		if (map == null || map.size() == 0)
			return;

		for (Integer key : map.keySet()) {
			if (key.intValue() == serialId)
				continue;

			String type = null;
			if (interactionGeneRows != null)
				type = interactionGeneRows.get(key);

			if (map.get(key) <= threshold)
				continue;

			// process the two nodes
			CyNode n1 = createNode(String.valueOf(serialId), geneIdToNameMap);
			CyNode n2 = createNode(key.toString(), geneIdToNameMap);

			createEdge(n1, n2, String.valueOf(serialId), String.valueOf(key),
					type);

			createSubNetwork(key, geneIdToNameMap, threshold, level - 1);

		} // end of the loop for map
	}

	private void createSubNetwork(String geneId,
			HashMap<String, String> geneIdToNameMap, double threshold, int level) {

		if (level == 0) {
			return;
		}

		HashMap<String, HashMap<String, Float>> geneRowsNotInMicroarray = adjMatrix
				.getGeneRowsNotInMicroarray();
		HashMap<String, String> interactionGeneRowsNotInMicroarray = adjMatrix
				.getInteractionNotInMicroarrayMap().get(geneId);
		HashMap<String, Float> map = geneRowsNotInMicroarray
				.get((String) geneId);

		if (map == null || map.size() == 0)
			return;

		for (String key : map.keySet()) {
			if (key.equals(geneId))
				continue;

			String type = null;
			if (interactionGeneRowsNotInMicroarray != null)
				type = interactionGeneRowsNotInMicroarray.get(key);

			if (map.get(key) <= threshold)
				continue;

			// process the two nodes
			CyNode n1 = createNode(geneId, geneIdToNameMap);
			CyNode n2 = createNode(key, geneIdToNameMap);

			// process the edge connecting geneId and key
			createEdge(n1, n2, geneId, key, type);

			createSubNetwork(key, geneIdToNameMap, threshold, level - 1);

		} // end of the loop for map
	}

	private void cyNetWorkView_graphViewChanged(GraphViewChangeEvent gvce) {
		if (Cytoscape.getCurrentNetworkView() != null
				&& Cytoscape.getCurrentNetwork() != null) {
			java.util.List<?> nodes = Cytoscape.getCurrentNetworkView()
					.getSelectedNodes();
			java.util.List<?> edges = Cytoscape.getCurrentNetworkView()
					.getSelectedEdges();
			CytoscapeDesktop test = Cytoscape.getDesktop();
			/*
			 * if (newFrame==null){ newFrame = new JFrame(); for (int cx=0;cx<test
			 * .getCytoPanel(SwingConstants.SOUTH).getCytoPanelComponentCount
			 * ();cx++){ try{ Component
			 * comp=test.getCytoPanel(SwingConstants.SOUTH).getComponentAt(cx);
			 * newFrame.add(comp.getParent()); }catch (Exception ex){ } } }
			 * newFrame.setVisible(true); newFrame.pack();
			 */
			if (edges.size() > 0) {
				test.getCytoPanel(SwingConstants.SOUTH).setSelectedIndex(1);
			} else {
				test.getCytoPanel(SwingConstants.SOUTH).setSelectedIndex(0);
			}
			DSPanel<DSGeneMarker> selectedMarkers = new CSPanel<DSGeneMarker>(
					"Selected Genes", "Cytoscape");
			for (int i = 0; i < nodes.size(); i++) {
				DNodeView pnode = (DNodeView) nodes.get(i);
				Node node = pnode.getNode();
				if (node instanceof CyNode) {
					String id = node.getIdentifier();
					// System.out.println("id = "+id);
					Integer geneId = Cytoscape.getNodeAttributes()
							.getIntegerAttribute(id, "geneID");
					if (geneId != null) {
						Collection<Integer> markerIds = geneIdToMarkerIdMap
								.get(geneId.toString());
						if (markerIds != null) {
							for (Integer markerId : markerIds) {
								selectedMarkers.add(maSet.getMarkers().get(
										markerId));
							}
						}

					}
					if (geneIdToMarkerIdMap.size() == 0)
						// probably user doesn't load annotation file, so
						// swissprotIdToMarkerIdMap contains nothing
						selectedMarkers.add(maSet.getMarkers().get(id));

					// int serial = ((Integer)
					// Cytoscape.getCurrentNetworkView().getNetwork().getNodeAttributeValue((CyNode)
					// node, "Serial")).intValue();
					// selectedMarkers.add(maSet.getMarkers().get(serial));
				}
			}
			selectedMarkers.setActive(true);
			if (publishEnabled) // skip if GeneTaggedEvent is being processed to
				// avoid event cycle
				publishSubpanelChangedEvent(new org.geworkbench.events.SubpanelChangedEvent<DSGeneMarker>(
						DSGeneMarker.class,
						selectedMarkers,
						org.geworkbench.events.SubpanelChangedEvent.SET_CONTENTS));
		}
	}

	private Color getRandomCorlor() {
		Random rand = new Random();
		int r = rand.nextInt(255);
		int g = rand.nextInt(255);
		int b = rand.nextInt(255);

		Color c = new Color(r, g, b);

		return c;
	}

	private void init() {
		Cytoscape.getDesktop().setVisible(false);

		CytoscapeInit initializer = new CytoscapeInit();

		CyInitParams param = new InitParam();
		// previously 'this' is passed to init method at the time when this is
		// not constructed yet. not better than this
		if (!initializer.init(param)) {
			log.warn("cytoscape initialization failed");
			Cytoscape.exit(1);
		}

		Cytoscape.getSwingPropertyChangeSupport().addPropertyChangeListener(
				new GenewaysNetworkListener());
		VisualMappingManager manager = Cytoscape.getVisualMappingManager();
		CalculatorCatalog catalog = manager.getCalculatorCatalog();
		sample1VisualStyle = catalog.getVisualStyle("Sample1");

		Calculator nc = catalog.getCalculator(
				cytoscape.visual.ui.VizMapUI.NODE_SHAPE, "BasicDiscrete");
		Vector<?> v = nc.getMappings();
		for (int i = 0; i < v.size(); i++) {
			if (v.get(i) instanceof DiscreteMapping)
				nodeDm = (DiscreteMapping) v.get(i);
			break;
		}
		nodeDm.setControllingAttributeName("geneType", cytoNetwork, false);

		nodeDm.putMapValue("K", shapes[shapeIndex]);
		nodeDm.putMapValue("P", shapes[++shapeIndex]);
		nodeDm.putMapValue("TF", shapes[++shapeIndex]);
		// non K/E/TF, in microarray set
		nodeDm.putMapValue("non K/P/TF, in microarray set",
				shapes[++shapeIndex]);

		sample1VisualStyle.getNodeAppearanceCalculator().setCalculator((nc));

		Calculator ec = catalog.getCalculator(
				cytoscape.visual.ui.VizMapUI.EDGE_COLOR, "BasicDiscrete");
		v = ec.getMappings();
		for (int i = 0; i < v.size(); i++) {
			if (v.get(i) instanceof DiscreteMapping)
				edgeDm = (DiscreteMapping) v.get(i);
			break;
		}
		edgeDm.setControllingAttributeName("type", cytoNetwork, false);

		sample1VisualStyle.setName("geneways-interactions");
	}

	private void receiveMatrix() {
		// 1) RECEIVE event
		String name = adjSet.getNetworkName();
		String tmpname = adjSet.getMatrix().getLabel();
		if ((tmpname != null) && (!name.contains(tmpname))) {
			name = tmpname + " [" + name + "]";
		}
		Set<?> networks = Cytoscape.getNetworkSet();
		HashSet<String> names = new HashSet<String>();
		for (Iterator<?> iterator = networks.iterator(); iterator.hasNext();) {
			FingCyNetwork network = (FingCyNetwork) iterator.next();
			String title = network.getTitle();
			names.add(title);
		}
		name = Util.getUniqueName(name, names);
		adjSet.setNetworkName(name);
		cytoNetwork = Cytoscape.createNetwork(name);

		// 2) DRAW NETWORK event

		drawCompleteNetwork(adjSet.getMatrix(), getGeneIdToNameMap(), adjSet
				.getThreshold());
		// if (adjSet.getGeneId() == -1) {
		// drawCompleteNetwork(adjSet.getMatrix(), adjSet.getThreshold());
		// } else {
		// drawCentricNetwork(adjSet.getMatrix(), adjSet.getGeneId(), adjSet
		// .getThreshold(), adjSet.getDepth());
		// }

		Cytoscape.getCurrentNetworkView().applyVizmapper(sample1VisualStyle);

		// Cytoscape.getCurrentNetworkView().applyVizmapper(sample1VisualStyle);

		try {
			JInternalFrame[] frames = Cytoscape.getDesktop()
					.getNetworkViewManager().getDesktopPane().getAllFrames();
			for (int i = 0; i < frames.length; i++) {
				frames[i].setMaximum(true);
			}
		} catch (Exception e) {
			log.error(e, e);
			// we just try to maximize the window, if failed, no big deal
		}
		CytoPanel temp = Cytoscape.getDesktop().getCytoPanel(
				SwingConstants.WEST);
		NetworkPanel tempp = null;
		for (int cx = 0; cx < temp.getCytoPanelComponentCount(); cx++) {
			if (temp.getComponentAt(cx) instanceof NetworkPanel)
				tempp = (NetworkPanel) temp.getComponentAt(cx);
		}
		tempp.hide(); // this line fixed wrong node number and wrong edge
		// number.
		// 3) FINISH event
		if (maSet != null) {
			view = Cytoscape.createNetworkView(cytoNetwork, maSet.getLabel());
			view.addGraphViewChangeListener(new GraphViewChangeListener() {
				public void graphViewChanged(
						GraphViewChangeEvent graphViewChangeEvent) {
					cyNetWorkView_graphViewChanged(graphViewChangeEvent);
				}
			});

		}
	}

	@SuppressWarnings("unchecked")
	// only because adjSet.getValuesForName("GENEMAP");
	private HashMap<String, String> getGeneIdToNameMap() {
		HashMap<String, String> map = new HashMap<String, String>();

		Object[] list = null;
		if (adjSet != null) {
			list = adjSet.getValuesForName("GENEMAP");
			if (list != null && list.length > 0 && list[0] != null) {
				map = (HashMap<String, String>) list[0];
			}

		}
		return map;

	}

	void drawCompleteNetwork(AdjacencyMatrix adjMatrix,
			HashMap<String, String> geneIdToNameMap, double threshold) {

		for (int cx = 0; cx < Cytoscape.getCurrentNetwork().getEdgeCount(); cx++) {
			Cytoscape.getCurrentNetwork().removeEdge(cx, true);
		}
		this.adjMatrix = adjMatrix;

		Iterator<Integer> keysIt = adjMatrix.getGeneRows().keySet().iterator();
		int i = 0;
		while (keysIt.hasNext()) {
			i++;
			int key = keysIt.next().intValue();
			createSubNetwork(key, geneIdToNameMap, threshold, 1);
			log.debug("iteration: " + i);
		}

		Iterator<String> keysNotInMicroarray = adjMatrix
				.getGeneRowsNotInMicroarray().keySet().iterator();

		while (keysNotInMicroarray.hasNext()) {
			i++;
			String key = keysNotInMicroarray.next();
			createSubNetwork(key, geneIdToNameMap, threshold, 1);
			log.debug("iteration: " + i);
		}

		// I use cytoscape's build-in layout instead of yfiles layout to avoid
		// duplicates entries in yfiles's menu.
		new ForceDirectedLayout(Cytoscape.getCurrentNetworkView()).doLayout();
		memoryUsage();
		new SpringEmbeddedLayouter(Cytoscape.getCurrentNetworkView())
				.doLayout();

		Cytoscape.getCurrentNetworkView().fitContent();

	}
}
