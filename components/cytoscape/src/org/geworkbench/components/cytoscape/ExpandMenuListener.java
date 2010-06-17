package org.geworkbench.components.cytoscape;

import giny.model.Node;
import giny.view.NodeView;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMicroarray;
import org.geworkbench.bison.datastructure.complex.panels.CSPanel;
import org.geworkbench.bison.datastructure.complex.panels.DSPanel;
import org.geworkbench.events.SubpanelChangedEvent;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import ding.view.DNodeView;
import ding.view.NodeContextMenuListener;

public class ExpandMenuListener implements NodeContextMenuListener {
	final static Log log = LogFactory.getLog(ExpandMenuListener.class);
	private CytoscapeWidget cytoscapeWidget = null;

	// following fields are a temporary solution for refactoring
	protected Map<String, List<Integer>> geneIdToMarkerIdMap = null;
	protected DSMicroarraySet<? extends DSMicroarray> maSet = null;
	protected JProgressBar jProgressBar = null;
	protected CyNetwork cytoNetwork = null;

	protected List<Long> runningThreads = null;

	public ExpandMenuListener(CytoscapeWidget cytoscapeWidget) {

		this.cytoscapeWidget = cytoscapeWidget;
		geneIdToMarkerIdMap = cytoscapeWidget.geneIdToMarkerIdMap;
		maSet = cytoscapeWidget.maSet;
		jProgressBar = cytoscapeWidget.jProgressBar;
		cytoNetwork = cytoscapeWidget.cytoNetwork;

		runningThreads = new ArrayList<Long>();
	}

	/**
	 * @param nodeView
	 *            The clicked NodeView
	 * @param menu
	 *            popup menu to add the Bypass menu
	 */
	public void addNodeContextMenuItems(final NodeView nodeView, JPopupMenu menu) {

		if (menu == null) {
			menu = new JPopupMenu();
		}

		JMenu addToSetMenu = new JMenu("Add to set ");
		JMenuItem menuItemIntersection = new JMenuItem(new IntersectionAction(
				"Intersection"));
		JMenuItem menuItemUnion = new JMenuItem(new UnionAction("Union"));
		addToSetMenu.add(menuItemIntersection);
		addToSetMenu.add(menuItemUnion);
		menu.add(addToSetMenu);
	}

	private class IntersectionAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -1559843540544628381L;

		public IntersectionAction(String name) {
			super(name);
		}

		@SuppressWarnings( { "unchecked" })
		public void actionPerformed(ActionEvent actionEvent) {
			if (Cytoscape.getCurrentNetworkView() != null
					&& Cytoscape.getCurrentNetwork() != null) {
				java.util.List nodes = Cytoscape.getCurrentNetworkView()
						.getSelectedNodes();

				if (nodes.size() == 0)
					return;

				log.debug(nodes.size() + " node(s) selected");

				DSPanel<DSGeneMarker> IntersectionMarkers = new CSPanel<DSGeneMarker>(
						"Intersection Genes", "Cytoscape");
				Set<Node> neighborsOfAllNodes = new HashSet<Node>();
				/*
				 * If we have N nodes, we'll need N lists to hold their
				 * neighbors
				 */
				List[] neighborsOfNodes = new ArrayList[nodes.size()];
				for (int i = 0; i < nodes.size(); i++) {
					DNodeView pnode = (DNodeView) nodes.get(i);
					Node node = pnode.getNode();
					List<Node> neighbors = Cytoscape.getCurrentNetworkView()
							.getNetwork().neighborsList(node);
					neighborsOfNodes[i] = neighbors;
				}
				/* Then, we'll need to get the intersection from those lists. */
				/*
				 * The logic here is, if a node does not existing in one of the
				 * lists, it does not exist in the intersection.
				 */
				for (int i = 0; i < neighborsOfNodes[0].size(); i++) {
					boolean atListOneNotContains = false;
					for (int n = 0; n < nodes.size(); n++) {
						if (!neighborsOfNodes[n].contains(neighborsOfNodes[0]
								.get(i))) {
							atListOneNotContains = true;
						}
					}
					if (!atListOneNotContains)// this node exist in all lists
						neighborsOfAllNodes.add((Node) neighborsOfNodes[0]
								.get(i));
				}

				log.debug("neighborsOfAllNodes:#" + neighborsOfAllNodes.size());
				IntersectionMarkers.addAll(nodesToMarkers(neighborsOfAllNodes));
				IntersectionMarkers.setActive(true);
				/*
				 * skip if GeneTaggedEvent is being processed, to avoid event
				 * cycle.
				 */
				if (cytoscapeWidget.publishEnabled)
					publishSubpanelChangedEvent(new org.geworkbench.events.SubpanelChangedEvent<DSGeneMarker>(
							DSGeneMarker.class,
							IntersectionMarkers,
							org.geworkbench.events.SubpanelChangedEvent.SET_CONTENTS));

			}
		}

	}

	private void publishSubpanelChangedEvent(
			SubpanelChangedEvent<DSGeneMarker> subpanelChangedEvent) {
		cytoscapeWidget.publishSubpanelChangedEvent(subpanelChangedEvent);

	}

	private class UnionAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5057482753345747180L;

		public UnionAction(String name) {
			super(name);
		}

		@SuppressWarnings( { "unchecked" })
		public void actionPerformed(ActionEvent actionEvent) {
			if (Cytoscape.getCurrentNetworkView() != null
					&& Cytoscape.getCurrentNetwork() != null) {
				java.util.List nodes = Cytoscape.getCurrentNetworkView()
						.getSelectedNodes();
				log.debug(nodes.size() + " node(s) selected");

				DSPanel<DSGeneMarker> UnionMarkers = new CSPanel<DSGeneMarker>(
						"Union Genes", "Cytoscape");
				Set<Node> neighborsOfAllNodes = new HashSet<Node>();
				/* Add all neighbors */
				for (int i = 0; i < nodes.size(); i++) {
					DNodeView pnode = (DNodeView) nodes.get(i);
					Node node = pnode.getNode();
					List<Node> neighbors = Cytoscape.getCurrentNetworkView()
							.getNetwork().neighborsList(node);
					if (neighbors != null) {
						neighborsOfAllNodes.addAll(neighbors);
					}
				}
				/* Remove selected nodes if exist in neighbor nodes. */
				for (int i = 0; i < nodes.size(); i++) {
					neighborsOfAllNodes.remove(((DNodeView) nodes.get(i))
							.getNode());
				}
				log.debug("neighborsOfAllNodes:#" + neighborsOfAllNodes.size());
				UnionMarkers.addAll(nodesToMarkers(neighborsOfAllNodes));
				UnionMarkers.setActive(true);
				/*
				 * Skip if GeneTaggedEvent is being processed, to avoid event
				 * cycle.
				 */
				if (cytoscapeWidget.publishEnabled)
					publishSubpanelChangedEvent(new org.geworkbench.events.SubpanelChangedEvent<DSGeneMarker>(
							DSGeneMarker.class,
							UnionMarkers,
							org.geworkbench.events.SubpanelChangedEvent.SET_CONTENTS));

			}
		}
	}

	private DSPanel<DSGeneMarker> nodesToMarkers(Set<Node> nodes) {
		DSPanel<DSGeneMarker> selectedMarkers = new CSPanel<DSGeneMarker>(
				"Selected Genes", "Cytoscape");
		for (Node node : nodes) {
			if (node instanceof CyNode) {
				String id = node.getIdentifier();
				Integer geneId = Cytoscape.getNodeAttributes()
						.getIntegerAttribute(id, "geneID");
				if (geneId != null) {
					Collection<Integer> markerIds = geneIdToMarkerIdMap
							.get(geneId.toString());
					if (markerIds != null) {
						for (Integer markerId : markerIds) {
							selectedMarkers.add(maSet.getMarkers()
									.get(markerId));
						}
					}

				}
				if (geneIdToMarkerIdMap.size() == 0)

					selectedMarkers.add(maSet.getMarkers().get(id));

			}
		}
		return selectedMarkers;
	}

}