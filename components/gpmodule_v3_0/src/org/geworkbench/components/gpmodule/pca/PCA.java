package org.geworkbench.components.gpmodule.pca;

import org.geworkbench.engine.management.AcceptTypes;
import org.geworkbench.engine.management.Subscribe;
import org.geworkbench.engine.management.Publish;
import org.geworkbench.events.ProjectEvent;
import org.geworkbench.events.ProjectNodeAddedEvent;
import org.geworkbench.events.PhenotypeSelectedEvent;
import org.geworkbench.events.MarkerSelectedEvent;
import org.geworkbench.util.microarrayutils.MicroarrayViewEventBase;
import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.biocollections.microarrays.CSExprMicroarraySet;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMicroarray;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.annotation.CSAnnotationContextManager;
import org.geworkbench.bison.annotation.DSAnnotationContext;
import org.jfree.chart.*;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.Range;
import org.tigr.util.FloatMatrix;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import org.geworkbench.components.gpmodule.pca.viewer.PCAContent3D;

import java.util.*;
import java.util.List;

/**
 * @author: Marc-Danie Nazaire
 */

@AcceptTypes({PCADataSet.class})
public class PCA extends MicroarrayViewEventBase
{
    private JTabbedPane tabbedPane;
    private JSplitPane compPanel;
    private JTable compResultsTable;
    private JSplitPane compGraphPanel;
    private JSplitPane projPanel;
    private JTable projResultsTable;
    private JScrollPane projGraphPanel;
    private JTextField perVar;
    private JButton createButton;
    private JButton clearPlotButton;
    private JButton imageSnapshotButton;

    private PCAData pcaData;
    private DSDataSet dataSet;

    private JScrollPane mainScrollPane;
    private Map dataLabelGroups;
    private PCAContent3D pcaContent3D;

    public PCA()
    {
        tabbedPane = new JTabbedPane();

        compPanel = new JSplitPane();
        compPanel.setOneTouchExpandable(true);
        compPanel.setDividerLocation(200);

        tabbedPane.addTab("Components", compPanel);
        tabbedPane.setSelectedComponent(compPanel);

        projPanel = new JSplitPane();
        projPanel.setOneTouchExpandable(true);
        projPanel.setDividerLocation(200);

        tabbedPane.addTab("Projection", projPanel);
        tabbedPane.addChangeListener( new PCAChangeListener());

        compGraphPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        compGraphPanel.setOneTouchExpandable(true);
        compGraphPanel.setDividerSize(8);
        compGraphPanel.setDividerLocation(0.6);
        compPanel.setRightComponent(compGraphPanel);

        compResultsTable = new JTable();
        compResultsTable.setColumnSelectionAllowed(false);
        compResultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        compPanel.setLeftComponent(compResultsTable);

        perVar = new JTextField();
        perVar.setMaximumSize(new Dimension(80, 100));

        createButton = new JButton("Create MA Set");
        createButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                int[] pcs = compResultsTable.getSelectedRows();

                if(pcs.length == 0)
                {
                    JOptionPane.showMessageDialog(null, "No principal components selected");
                    return;
                }

                CSExprMicroarraySet pcDataSet = new CSExprMicroarraySet();
                pcDataSet.readFromFile(dataSet.getFile());
                pcDataSet.setLabel("PCA_" + dataSet.getFile().getName());
                pcDataSet.clear();
                for(int i = 0; i < pcs.length; i++)
                {
                    pcDataSet.add((DSMicroarray)dataSet.get(i));
                }

                publishProjectNodeAddedEvent(new ProjectNodeAddedEvent("PCA_" + dataSet.getDataSetName(), pcDataSet, null));
            }
        });

        compResultsTable.setRowSelectionAllowed(true);
        compResultsTable.getSelectionModel().addListSelectionListener( new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent event)
            {
                if(!event.getValueIsAdjusting())
                {
                    int[] selectedRows = compResultsTable.getSelectedRows();

                    if(selectedRows.length == 0)
                    {
                        compResultsTable.getSelectionModel().setAnchorSelectionIndex(-1);
                        compResultsTable.getSelectionModel().setLeadSelectionIndex(-1);
                        compGraphPanel.removeAll();
                        return;
                    }
                    double sum = 0;

                    for(int i = 0; i < selectedRows.length; i++)
                    {
                        String value = ((String)compResultsTable.getValueAt(selectedRows[i], 2)).replace("%", "");
                        sum += Double.parseDouble(value);
                    }

                    perVar.setText(String.valueOf(sum));
                    buildComponentsPanel(selectedRows);
                }
            }
        });
       
        projGraphPanel = new JScrollPane();
        projPanel.setRightComponent(projGraphPanel);
        plotButton.setEnabled(false);
        plotButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                buildPlot(projResultsTable.getSelectedRows());
            }
        });

        clearPlotButton = new JButton("Clear Plot");
        clearPlotButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                projResultsTable.clearSelection();
                projGraphPanel.getViewport().removeAll();
                projGraphPanel.repaint();
            }
        });

        imageSnapshotButton = new JButton("Image Snapshot");
        imageSnapshotButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                Component component = projGraphPanel.getViewport().getComponent(0);

                BufferedImage graphImage = null; //new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
                if(component instanceof ChartPanel)
                {
                    graphImage = ((ChartPanel)component).getChart().createBufferedImage(component.getWidth(), component.getHeight());
                }
                else
                {
                    graphImage = ((PCAContent3D)component).createImage();  
                }

                ImageIcon newIcon = new ImageIcon(graphImage, "PCA Image");
                org.geworkbench.events.ImageSnapshotEvent imageEvent = new org.geworkbench.events.ImageSnapshotEvent("Color Mosaic View", newIcon, org.geworkbench.events.ImageSnapshotEvent.Action.SAVE);
                publishImageSnapshotEvent(imageEvent);
            }
        });

        projResultsTable = new JTable();
        projResultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        projResultsTable.setColumnSelectionAllowed(false);
        projPanel.setLeftComponent(projResultsTable);

        projResultsTable.getSelectionModel().addListSelectionListener( new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent event)
            {
                int[] selectedRows = projResultsTable.getSelectedRows();

                double sum = 0;

                for(int i = 0; i < selectedRows.length; i++)
                {
                    String value = ((String)projResultsTable.getValueAt(selectedRows[i], 2)).replace("%", "");
                    sum += Double.parseDouble(value);
                }

                perVar.setText(String.valueOf(sum));

                if(selectedRows.length == 4)
                {
                    projResultsTable.removeRowSelectionInterval(selectedRows[0], selectedRows[0]);
                }

                if(selectedRows.length >=2)
                {
                    plotButton.setEnabled(true);
                }
                else
                    plotButton.setEnabled(false);

            }
        });

        onlyActivatedMarkers = false;
        onlyActivatedArrays = false;

        jToolBar3.remove(chkAllArrays);
        jToolBar3.remove(chkAllMarkers);

        chkAllArrays.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                onlyActivatedMarkers = !chkAllArrays.isSelected();
            }
        });

        chkAllMarkers.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                onlyActivatedMarkers = !chkAllMarkers.isSelected();
            }
        });

        onlyActivatedMarkers = false;
        
        buildJToolBar3();

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        mainScrollPane = new JScrollPane();
        mainScrollPane.setViewportView(mainPanel);
    }
    /**
     * The component for the GUI engine.
     */
    public Component getComponent()
    {
        return mainScrollPane;
    }

    private void buildResultsTable()
    {
        String[] columnNames = {"Id", "Eigen Value", "% Var"};
        TableModel tableModel = new DefaultTableModel(columnNames, pcaData.getNumPCs());

        for(int i=1; i <= pcaData.getNumPCs(); i++)
        {
            tableModel.setValueAt(i, i-1, 0);
            Map eigenValues = pcaData.getEigenValues();
            tableModel.setValueAt(eigenValues.get(Integer.valueOf(i)), i-1, 1);

            Map percentVars = pcaData.getPercentVars();
            tableModel.setValueAt(percentVars.get(Integer.valueOf(i)), i-1, 2);
        }

        compResultsTable.removeAll();
        compResultsTable.setModel(tableModel);
        compPanel.setLeftComponent(new JScrollPane(compResultsTable));
        compPanel.setRightComponent(new JPanel());
        compPanel.setDividerLocation(200);

        projResultsTable.removeAll();
        projResultsTable.setModel(tableModel);
        projPanel.setDividerLocation(200);
        projPanel.setLeftComponent(new JScrollPane(projResultsTable));
        projPanel.setRightComponent(new JPanel());

        tabbedPane.setSelectedComponent(compPanel);
    }

    private void buildEigenVectorsTable(int[] pComp)
    {
        if(pComp == null || pComp.length == 0)
        {
            System.err.println("No principal components found");
            return;
        }

        JTable eigenVectorsTable = new JTable();
        DefaultTableModel tableModel = new DefaultTableModel();

        Map map = pcaData.getEigenVectors();
        tableModel.setColumnCount(((List)map.values().iterator().next()).size());
        for(int i = 0; i < pComp.length; i++)
        {
            int pc = pComp[i]+1;
            List eigenVector = new ArrayList((List)map.get(new Integer(pc)));
            eigenVector.add(0, "Comp " + pc);
            tableModel.addRow(new Vector(eigenVector));
        }

        Vector columnNames = new Vector();
        columnNames.addAll((Vector)tableModel.getDataVector().get(0));
        Collections.fill(columnNames, "");

        tableModel.setColumnIdentifiers(columnNames);
        eigenVectorsTable.setModel(tableModel);
        eigenVectorsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane scrollPane = new JScrollPane(eigenVectorsTable);
        compGraphPanel.setBottomComponent(scrollPane);
    }

    public void buildGraph(int[] pComp)
    {
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();

        Map map = pcaData.getEigenVectors();
        for(int i = 0; i < pComp.length; i++)
        {
            int pc = pComp[i]+1;
            List eigenVector = new ArrayList((List)map.get(new Integer(pc)));

            XYSeries xySeries = new XYSeries("Prin. Comp. " + pc);
            for(int n = 0; n < eigenVector.size(); n++)
            {
                xySeries.add(n+1, Double.parseDouble((String)eigenVector.get(n)));
            }

            xySeriesCollection.addSeries(xySeries);
        }

        JFreeChart lineGraph = ChartFactory.createXYLineChart
                (null, null, null, xySeriesCollection, PlotOrientation.VERTICAL, true, true, false);
        
        ChartPanel panel = new ChartPanel(lineGraph);
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setMinimumSize(new Dimension(420, 270));

        compGraphPanel.setTopComponent(scrollPane);
    }

    private void buildPlot(int[] pComp)
    {
        CSExprMicroarraySet maSet = (CSExprMicroarraySet)dataSet;
        List dataLabelList = new ArrayList();
        FloatMatrix u_Matrix = null;
        dataLabelGroups = new HashMap();

        if(pcaData.getClusterBy().equals("genes"))
        {
            for(int i = 0; i < maSet.size(); i++)
            {
                dataLabelList.add(maSet.get(i).getLabel());
            }
        }
        else
        {
            for(int i = 0; i < maSet.getMarkers().size(); i++)
            {
                dataLabelList.add(maSet.getMarkers().get(i).getLabel());
            }
        }

        if(pcaData.getClusterBy().equals("experiments") && onlyActivatedMarkers)
        {
            if(activatedMarkers == null || activatedMarkers.size() == 0)
            {
                JOptionPane.showMessageDialog(mainPanel, "No markers selected");
                return;
            }

            DSAnnotationContext<DSGeneMarker> context = CSAnnotationContextManager.getInstance().getCurrentContext(maSet.getMarkerVector());

            for(int i =0; i < activatedMarkers.size(); i++)
            {
                DSGeneMarker marker = activatedMarkers.get(i);
                String[] label = context.getLabelsForItem(marker);

                if(label != null && label.length > 0)
                {
                    Set set = (Set)dataLabelGroups.get(label[0]);
                    if(set == null)
                        set = new LinkedHashSet();

                    set.add(marker.getLabel());

                    dataLabelGroups.put(label[0], set);
                }
            }
        }
        else if(pcaData.getClusterBy().equals("genes") && onlyActivatedArrays)
        {
            if(activatedArrays == null || activatedArrays.size() == 0)
            {
                JOptionPane.showMessageDialog(mainPanel, "No arrays selected");
                return;
            }

             DSAnnotationContext<DSMicroarray> context = CSAnnotationContextManager.getInstance().getCurrentContext(maSet);

            for(int i =0; i < activatedArrays.size(); i++)
            {
                DSMicroarray array = (DSMicroarray)activatedArrays.get(i);
                String[] label = context.getLabelsForItem(array);

                if(label != null & label.length > 0)
                {
                    Set set = (Set)dataLabelGroups.get(label[0]);
                    if(set == null)
                        set = new LinkedHashSet();
                    set.add(array.getLabel());

                    dataLabelGroups.put(label[0], set);
                }
            }
        }
        else
        {
            dataLabelGroups.put("group 1", new LinkedHashSet(dataLabelList));
        }

        u_Matrix = pcaData.getUMatrix();

        if(pComp.length == 3)
        {
            int pc1 = pComp[0]+1;
            int pc2 = pComp[1]+1;
            int pc3 = pComp[2]+1;
            List data = new ArrayList();

            Set dataGroups = dataLabelGroups.keySet();
            Iterator it = dataGroups.iterator();
            while(it.hasNext())
            {
                String group = (String)it.next();

                Set labels = (Set)dataLabelGroups.get(group);
                Iterator labelsIt = labels.iterator();

                while(labelsIt.hasNext())
                {
                   String label = (String)labelsIt.next();
                    int row = dataLabelList.indexOf(label);

                    PCAContent3D.XYZData xyzData = new PCAContent3D.XYZData(u_Matrix.get(row, pc1-1),
                            u_Matrix.get(row, pc2-1), u_Matrix.get(row, pc3-1), label);
                    xyzData.setCluster(group);
                    data.add(xyzData);
                }
            }

            pcaContent3D = new PCAContent3D(data);
            pcaContent3D.setPointSize((float)1.4);
            pcaContent3D.setXAxisLabel("Prin. Comp. " + pc1);
            pcaContent3D.setYAxisLabel("Prin. Comp. " + pc2);
            pcaContent3D.setZAxisLabel("Prin. Comp. " + pc3);

            pcaContent3D.getComponent(0).addMouseListener(new PCA3DMouseListener());
            pcaContent3D.updateScene();
            projGraphPanel.setViewportView(pcaContent3D);

        }
        else
        {
            int pc1 = pComp[0]+1;

            int pc2 = pComp[1]+1;
            JFreeChart graph = ChartFactory.createScatterPlot
                       ("2D Projection", "Prin. Comp. " + pc1, "Prin. Comp. " + pc2, null, PlotOrientation.VERTICAL, true, false, false);

            XYSeriesCollection xySeriesCollection = new XYSeriesCollection();

            Set dataGroups = dataLabelGroups.keySet();
            Iterator it = dataGroups.iterator();
            while(it.hasNext())
            {
                String group = (String)it.next();
                XYSeries xySeries = new XYSeries("", false, true);                

                if(dataGroups.size() > 1)
                    xySeries.setKey(group);

                Set labels = (Set)dataLabelGroups.get(group);
                Iterator labelsIt = labels.iterator();

                while(labelsIt.hasNext())
                {
                    String label = (String)labelsIt.next();
                    int row = dataLabelList.indexOf(label);

                    XYDataItem item = new XYDataItem(u_Matrix.get(row, pc1-1), u_Matrix.get(row, pc2-1));
                    xySeries.add(item);
                }

                xySeriesCollection.addSeries(xySeries);
            }
         
            graph.getXYPlot().setDataset(xySeriesCollection);
            graph.getXYPlot().getRangeAxis().setTickMarksVisible(true);
            graph.getXYPlot().getRangeAxis().setTickMarkPaint(Color.BLACK);          

            Range domainRange = graph.getXYPlot().getDomainAxis().getRange();
            double maxDomainRange = Math.max(Math.abs(domainRange.getLowerBound()), domainRange.getUpperBound());
            graph.getXYPlot().getDomainAxis().setLowerBound(-maxDomainRange);
            graph.getXYPlot().getDomainAxis().setUpperBound(maxDomainRange);

            Range range = graph.getXYPlot().getRangeAxis().getRange();
            double maxRange = Math.max(Math.abs(range.getLowerBound()), range.getUpperBound());
            graph.getXYPlot().getRangeAxis().setLowerBound(-maxRange);
            graph.getXYPlot().getRangeAxis().setUpperBound(maxRange);

            graph.getXYPlot().addRangeMarker(new ValueMarker(0.0, Color.BLACK, new BasicStroke((float)1.4)));
            graph.getXYPlot().addDomainMarker(new ValueMarker(0.0, Color.BLACK, new BasicStroke((float)1.4)));


            graph.getXYPlot().setDomainGridlinesVisible(false);

            graph.getXYPlot().getRenderer().setToolTipGenerator( new StandardXYToolTipGenerator()
            {
                public String generateToolTip(XYDataset data, int series, int item)
                {
                    XYSeries xySeries = ((XYSeriesCollection)data).getSeries(series);
                    String key = (String)xySeries.getKey();

                    if(key.equals(""))
                        key = "group 1";
                    Set labels = (Set)dataLabelGroups.get(key);

                    Iterator it = labels.iterator();
                    int i = 0;
                    while(i < item)
                    {
                        it.next();
                        i++;
                    }

                   String result = "[" + xySeries.getDataItem(item).getX() + ", " + xySeries.getDataItem(item).getY() + "]";
                   if(it != null)
                   {
                       result = it.next() + " : " +  result;
                   }

                    return result;
                }
            });

            ChartPanel panel = new ChartPanel(graph);
            panel.addChartMouseListener(new PCAChartMouseListener());
            projGraphPanel.setViewportView(panel);
        }
        
        projPanel.setRightComponent(projGraphPanel);
        projPanel.setDividerLocation(200);
        projPanel.repaint();

        System.out.println("Activated Markers: " + super.activatedMarkers);
    }

    private void buildComponentsPanel(int[] pComp)
    {
        buildEigenVectorsTable(pComp);
        buildGraph(pComp);

        compPanel.setDividerLocation(200);
        compPanel.setRightComponent(compGraphPanel);
    }

    @Subscribe
    public void receive(ProjectEvent e, Object source)
    {        
        if(e.getDataSet() instanceof PCADataSet)
        {
            PCADataSet pcaDataSet = ((PCADataSet)e.getDataSet());
            pcaData = pcaDataSet.getData();
            dataSet = pcaDataSet.getParentDataSet();

            buildResultsTable();
        }
    }

    private void buildJToolBar3()
    {
        jToolBar3.removeAll();

        jToolBar3.add(new JLabel("% Var"));
        jToolBar3.add(perVar);
        jToolBar3.addSeparator();

        int viewIndex = tabbedPane.getSelectedIndex();
        if(tabbedPane.getTitleAt(viewIndex).equals("Projection"))
        {
            jToolBar3.add(plotButton);
            jToolBar3.addSeparator();
            jToolBar3.add(clearPlotButton);
            jToolBar3.addSeparator(new Dimension(75, 0));
            jToolBar3.add(imageSnapshotButton);
            jToolBar3.addSeparator(new Dimension(450, 0));

            if(pcaData.getClusterBy().equals("genes"))
            {
                chkAllArrays.setSelected(true);
                onlyActivatedMarkers = false;
                jToolBar3.add(chkAllArrays);
            }
            else
            {
                chkAllMarkers.setSelected(true);
                onlyActivatedArrays = false;
                jToolBar3.add(chkAllMarkers);
            }
        }
        else
            jToolBar3.add(createButton);

        jToolBar3.repaint();
    }

    private class PCAChangeListener implements ChangeListener
    {
        public void stateChanged(ChangeEvent event)
        {
            if(event.getSource() instanceof JTabbedPane)
            {
                buildJToolBar3();
            }
        }
    }

    private class PCA3DMouseListener implements MouseListener
    {
        public void mouseClicked(MouseEvent event)
        {
            String label = pcaContent3D.getSelectedPoint();
            if(label == null)
                return;
            if(pcaData.getClusterBy().equals("genes"))
            {
                DSMicroarray microarray = ((CSExprMicroarraySet)dataSet).getMicroarrayWithId(label);
                if (microarray != null)
                {
                    PhenotypeSelectedEvent pse = new PhenotypeSelectedEvent(microarray);
                    publishPhenotypeSelectedEvent(pse);
                }
            }
            else
            {
                DSGeneMarker marker = ((CSExprMicroarraySet)dataSet).getMarkers().get(label);
                if (marker != null)
                {
                    MarkerSelectedEvent mse = new org.geworkbench.events.MarkerSelectedEvent(marker);
                    publishMarkerSelectedEvent(mse);
                }
            }
        }

        public void mousePressed(MouseEvent event)
        {
        }

        public void mouseReleased(MouseEvent event)
        {
        }

        public void mouseEntered(MouseEvent event)
        {
        }

        public void mouseExited(MouseEvent event)
        {
        }
    }

    @Publish
    public ProjectNodeAddedEvent publishProjectNodeAddedEvent(ProjectNodeAddedEvent event)
    {
        return event;
    }

    @Publish
    public org.geworkbench.events.ImageSnapshotEvent publishImageSnapshotEvent
            (org.geworkbench.events.ImageSnapshotEvent
                    event) {
        return event;
    }

    private class PCAChartMouseListener implements ChartMouseListener
    {
        public void chartMouseClicked(ChartMouseEvent event)
        {
            ChartEntity entity = event.getEntity();
            if ((entity != null) && (entity instanceof XYItemEntity))
            {
                XYItemEntity xyEntity = (XYItemEntity) entity;
                int series = xyEntity.getSeriesIndex();
                int item = xyEntity.getItem();

                XYSeries xySeries = ((XYSeriesCollection)xyEntity.getDataset()).getSeries(series);
                String key = (String)xySeries.getKey();

                if(key.equals(""))
                    key = "group 1";
                Set labels = (Set)dataLabelGroups.get(key);

                Iterator it = labels.iterator();
                int i = 0;
                while(i < item)
                {
                    it.next();
                    i++;
                }

                String label = (String)it.next();

                if(pcaData.getClusterBy().equals("genes"))
                {
                    DSMicroarray microarray = ((CSExprMicroarraySet)dataSet).getMicroarrayWithId(label);
                    if (microarray != null)
                    {
                        PhenotypeSelectedEvent pse = new PhenotypeSelectedEvent(microarray);
                        publishPhenotypeSelectedEvent(pse);
                    }
                }
                else
                {
                    DSGeneMarker marker = ((CSExprMicroarraySet)dataSet).getMarkers().get(label);
                    if (marker != null)
                    {
                        MarkerSelectedEvent mse = new org.geworkbench.events.MarkerSelectedEvent(marker);
                        publishMarkerSelectedEvent(mse);
                    }
                }
            }
        }


        public void chartMouseMoved(ChartMouseEvent event)
        {
            // No-op
        }
    }

    @Publish
    public org.geworkbench.events.MarkerSelectedEvent publishMarkerSelectedEvent(
            MarkerSelectedEvent event) {
        return event;
    }

    @Publish
    public PhenotypeSelectedEvent publishPhenotypeSelectedEvent(
            PhenotypeSelectedEvent event) {
        return event;
    }
}
