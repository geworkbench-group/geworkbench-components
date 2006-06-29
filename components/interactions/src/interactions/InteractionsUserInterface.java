/*
 * Interactions.java
 *
 * Created on May 26, 2006, 2:56 PM
 */

package interactions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.table.DefaultTableModel;
import javax.xml.rpc.ServiceException;
import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.datastructure.complex.panels.DSPanel;
import org.geworkbench.events.GeneSelectorEvent;
import org.geworkbench.events.ProjectEvent;
import org.geworkbench.engine.management.AcceptTypes;
import org.geworkbench.engine.management.Subscribe;
import org.geworkbench.engine.config.VisualPlugin;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.BasicClientConfig;
import org.geworkbench.bison.datastructure.bioobjects.markers.CSGeneMarker;
import org.geworkbench.bison.datastructure.complex.panels.DSItemList;
import org.geworkbench.engine.management.Publish;
import org.geworkbench.events.AdjacencyMatrixEvent;
import org.geworkbench.events.ProjectNodeAddedEvent;
import org.geworkbench.util.pathwaydecoder.mutualinformation.AdjacencyMatrix;
import org.geworkbench.util.pathwaydecoder.mutualinformation.AdjacencyMatrixDataSet;

/**
 * @author manjunath at genomecenter dot columbia dot edu
 */
@AcceptTypes ({DSMicroarraySet.class})
public class InteractionsUserInterface extends javax.swing.JScrollPane implements VisualPlugin{

    /** Creates new form Interactions */
    public InteractionsUserInterface() {
        initComponents();
        initConnections();
    }

    public Component getComponent(){
        return this;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
	  mainPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        allGeneList = new javax.swing.JList();
        jScrollPane2 = new javax.swing.JScrollPane();
        selectedGenesList = new javax.swing.JList();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();

        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 255)));
        jPanel2.setMaximumSize(new java.awt.Dimension(587, 233));
        jPanel2.setMinimumSize(new java.awt.Dimension(587, 233));
        jPanel2.setPreferredSize(new java.awt.Dimension(587, 233));
        jLabel2.setText("Obtain Interactions for Gene(s):");

        allGeneList.setToolTipText("Available Genes");
        allGeneList.setModel(allGeneModel);
        allGeneList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                allGeneListHandler(evt);
            }
        });

        jScrollPane1.setViewportView(allGeneList);

        selectedGenesList.setToolTipText("Selected Genes");
        selectedGenesList.setModel(selectedGenesModel);
        selectedGenesList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                selectedGenesListHandler(evt);
            }
        });

        jScrollPane2.setViewportView(selectedGenesList);

        addButton.setText(">>");
        addButton.setToolTipText("Add to selection");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonHandler(evt);
            }
        });

        removeButton.setText("<<");
        removeButton.setToolTipText("Remove From Selection");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonHandler(evt);
            }
        });

        jButton1.setText("Preview Selections...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previewSelectionsHandler(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 193, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 75, Short.MAX_VALUE)
                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(addButton)
                                    .add(removeButton))
                                .add(54, 54, 54)
                                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 194, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jLabel2))
                        .addContainerGap())
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                        .add(jButton1)
                        .add(217, 217, 217))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(56, 56, 56)
                        .add(addButton)
                        .add(33, 33, 33)
                        .add(removeButton))
                    .add(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jLabel2)
                        .add(14, 14, 14)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 18, Short.MAX_VALUE)
                .add(jButton1)
                .add(20, 20, 20))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 255)));
        jPanel1.setMaximumSize(new java.awt.Dimension(587, 382));
        jPanel1.setMinimumSize(new java.awt.Dimension(587, 382));
        jPanel1.setPreferredSize(new java.awt.Dimension(587, 382));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jTable1.setModel(previewTableModel);
        jScrollPane3.setViewportView(jTable1);

        jLabel1.setText("Preview:");

        jButton2.setText("Load From DB");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadfromDBHandler(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 560, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel1)))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(242, 242, 242)
                        .add(jButton2)))
                .addContainerGap(15, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .add(21, 21, 21)
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 277, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(15, 15, 15)
                .add(jButton2)
                .addContainerGap(19, Short.MAX_VALUE))
        );

	  this.getViewport().add(mainPanel);
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 232, Short.MAX_VALUE)
                .add(15, 15, 15)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void loadfromDBHandler(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadfromDBHandler
        int row = jTable1.getSelectedRow();
        if (row >= 0){
            BigDecimal entrezId = entrezIds.get(row);
            Vector<Object> data = cachedPreviewData.get(row);
            try {
                Vector<Object> neighbors = new Vector<Object>();
                neighbors.addAll(Arrays.asList(interactionsService.getFIRSTNEIGHBORS(entrezId, "protein-protein")));
                neighbors.addAll(Arrays.asList(interactionsService.getFIRSTNEIGHBORS(entrezId, "protein-dna")));
                AdjacencyMatrix matrix = new AdjacencyMatrix();
                matrix.setMicroarraySet((DSMicroarraySet)dataset);
                int eid = entrezId.intValue();
                CSGeneMarker marker = new CSGeneMarker();
                marker.setGeneId(eid);
                DSItemList<DSGeneMarker> markers = dataset.getMarkers();
                EntrezIdComparator eidc = new EntrezIdComparator();
                Collections.sort(markers, eidc);
                int index = Collections.binarySearch(markers, marker, eidc);
                int serial = markers.get(index).getSerial();
                matrix.addGeneRow(serial);
                for (Object neighbor: neighbors){
                    marker = new CSGeneMarker();
                    marker.setGeneId(((BigDecimal)neighbor).intValue());
                    index = Collections.binarySearch(markers, marker, eidc);
                    if (index >=0 && index < markers.size()){
                        int serial2 = markers.get(index).getSerial();
                        matrix.add(serial, serial2, 0.8f);
                    }
                }
                AdjacencyMatrixDataSet dataSet = new AdjacencyMatrixDataSet(matrix, serial, 0.5f, 2, "Adjacency Matrix", dataset.getLabel(), dataset);
                publishProjectNodeAddedEvent(new ProjectNodeAddedEvent("Adjacency Matrix Added", null, dataSet));
                publishAdjacencyMatrixEvent(new AdjacencyMatrixEvent(matrix, "Initiate", serial, 2, 1.0, AdjacencyMatrixEvent.Action.RECEIVE));
                publishAdjacencyMatrixEvent(new AdjacencyMatrixEvent(matrix, "Interactions from knowledgebase", serial, 2, 1.0, AdjacencyMatrixEvent.Action.DRAW_NETWORK));
            } catch (RemoteException re){
                re.printStackTrace();
            }
        }
    }//GEN-LAST:event_loadfromDBHandler

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JList allGeneList;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JButton removeButton;
    private javax.swing.JList selectedGenesList;
    // End of variables declaration//GEN-END:variables

    private void allGeneListHandler(MouseEvent evt){
        if (evt.getClickCount() == 2){
            int index = allGeneList.locationToIndex(evt.getPoint());
            DSGeneMarker m = allGenes.get(index);
            selectedGenes.add(m);
            allGenes.remove(m);
            allGeneList.setModel(new DefaultListModel());
            allGeneList.setModel(allGeneModel);
            selectedGenesList.setModel(new DefaultListModel());
            selectedGenesList.setModel(selectedGenesModel);
        }
    }

    private void selectedGenesListHandler(MouseEvent evt){
        if (evt.getClickCount() == 2){
            int index = selectedGenesList.locationToIndex(evt.getPoint());
            DSGeneMarker m = selectedGenes.get(index);
            allGenes.add(m);
            selectedGenes.remove(m);
            allGeneList.setModel(new DefaultListModel());
            allGeneList.setModel(allGeneModel);
            selectedGenesList.setModel(new DefaultListModel());
            selectedGenesList.setModel(selectedGenesModel);
        }
    }

    private void addButtonHandler(ActionEvent e){
        int[] indices = allGeneList.getSelectedIndices();
        if (indices != null && indices.length > 0){
            Vector<DSGeneMarker> markers = new Vector<DSGeneMarker>();
            for (int index : indices){
                DSGeneMarker marker = allGenes.get(index);
                selectedGenes.add(marker);
                markers.add(marker);
            }
            for (DSGeneMarker marker : markers){
                allGenes.remove(marker);
            }
            allGeneList.setModel(new DefaultListModel());
            allGeneList.setModel(allGeneModel);
            selectedGenesList.setModel(new DefaultListModel());
            selectedGenesList.setModel(selectedGenesModel);
        }
    }

    private void removeButtonHandler(ActionEvent e){
        int[] indices = selectedGenesList.getSelectedIndices();
        if (indices != null && indices.length > 0){
            Vector<DSGeneMarker> markers = new Vector<DSGeneMarker>();
            for (int index : indices){
                DSGeneMarker marker = selectedGenes.get(index);
                allGenes.add(marker);
                markers.add(marker);
            }
            for (DSGeneMarker marker : markers){
                selectedGenes.remove(marker);
            }
            allGeneList.setModel(new DefaultListModel());
            allGeneList.setModel(allGeneModel);
            selectedGenesList.setModel(new DefaultListModel());
            selectedGenesList.setModel(selectedGenesModel);
        }
    }

    private void previewSelectionsHandler(ActionEvent e){
        entrezIds.clear();
        cachedPreviewData.clear();
        for (DSGeneMarker marker : selectedGenes){
            BigDecimal id = new BigDecimal(marker.getGeneId());
            if (id != null && !entrezIds.contains(id)){
                geneNames.add(marker.getGeneName());
                entrezIds.add(id);
                cachedPreviewData.add(new Vector<Object>());
            }
        }
        jTable1.setModel(new DefaultTableModel());
        jTable1.setModel(previewTableModel);
    }

    private void initConnections(){
        EngineConfiguration ec = new BasicClientConfig();
        interactions.INTERACTIONSServiceLocator service =
                new interactions.INTERACTIONSServiceLocator(ec);
        service.setinteractionsEndpointAddress(System.getProperty("interactions.endpoint"));
        try {
            interactionsService = service.getinteractions();
        } catch (ServiceException se){
            se.printStackTrace();
        }
    }

    ListModel allGeneModel = new AbstractListModel(){
        public Object getElementAt(int index){
            return allGenes.get(index);
        }

        public int getSize(){
            return allGenes.size();
        }
    };

    ListModel selectedGenesModel = new AbstractListModel(){
        public Object getElementAt(int index){
            return selectedGenes.get(index);
        }

        public int getSize(){
            return selectedGenes.size();
        }
    };

    DefaultTableModel previewTableModel = new DefaultTableModel(){

        public int getColumnCount(){
            return 3;
        }

        public int getRowCount(){
            if (entrezIds != null)
                return entrezIds.size();
            return 0;
        }

        public String getColumnName(int index){
            switch (index){
                case 0: return "Gene Name";
                case 1: return "# of Protein-Protein Interactions";
                case 2: return "# of Protein-DNA Interactions";
                default: return "";
            }
        }

        synchronized public Object getValueAt(int row, int column){
            Thread.currentThread().setContextClassLoader(InteractionsUserInterface.this.getClass().getClassLoader());
            if (interactionsService != null){
                try {
                    Object value = null;
                    switch (column){
                        case 0: {
                            if (cachedPreviewData.get(row).size() == 0){
                                String gn = geneNames.get(row);
                                cachedPreviewData.get(row).add(0, gn);
                                return gn;
                            }
                            return cachedPreviewData.get(row).get(0);
                        }
                        case 1: {
                            if (cachedPreviewData.get(row).size() <= 1){
                                BigDecimal ic = interactionsService.getINTERACTIONCOUNT(entrezIds.get(row), "protein-protein");
                                cachedPreviewData.get(row).add(1, ic);
                                return ic;
                            }
                            return cachedPreviewData.get(row).get(1);
                        }
                        case 2: {
                            if (cachedPreviewData.get(row).size() <= 2){
                                BigDecimal ic = interactionsService.getINTERACTIONCOUNT(entrezIds.get(row), "protein-dna");
                                cachedPreviewData.get(row).add(2, ic);
                                return ic;
                            }
                            return cachedPreviewData.get(row).get(2);
                        }
                        default: return "loading ...";
                    }
                } catch (RemoteException re){
                    re.printStackTrace();
                }
            }
//            TableWorker worker = new TableWorker(row, column);
//            worker.start();
            return "loading ...";
        }
    };

    class TableWorker extends SwingWorker{
        int row = 0;
        int column = 0;
        public TableWorker(int r, int c){
            row = r;
            column = c;
        }

        synchronized public Object construct(){
            Thread.currentThread().setContextClassLoader(InteractionsUserInterface.this.getClass().getClassLoader());
            if (interactionsService != null){
                try {
                    Object value = null;
                    switch (column){
                        case 0: value = entrezIds.get(row); break;
                        case 1: value = interactionsService.getGENECOUNT(); break;
                        case 2: value = interactionsService.getGENECOUNT(); break;
//                        case 1: value =  interactionsService.getINTERACTIONCOUNT(translatedNames.get(row).toUpperCase(), new BigDecimal(1));
//                        case 2: value = interactionsService.getINTERACTIONCOUNT(translatedNames.get(row).toUpperCase(), new BigDecimal(2));
                        default: value = "loading ...";
                    }
                    cachedPreviewData.get(row).add(column, value);
                    previewTableModel.fireTableDataChanged();
                } catch (RemoteException re){
                    re.printStackTrace();
                }
            }
            return "loading ...";
        }
    }

    private INTERACTIONS interactionsService = null;

    private Vector<DSGeneMarker> allGenes = new Vector<DSGeneMarker>();
    private Vector<DSGeneMarker> selectedGenes = new Vector<DSGeneMarker>();
    private Vector<BigDecimal> entrezIds = new Vector<BigDecimal>();
    private Vector<String> geneNames = new Vector<String>();
    private Vector<Vector<Object>> cachedPreviewData = new Vector<Vector<Object>>();
    private DSMicroarraySet dataset = null;

    @Subscribe public void receive(GeneSelectorEvent gse, Object source){
        DSPanel<DSGeneMarker> panel = gse.getPanel();
        if (panel != null){
            allGenes.clear();
            for (DSGeneMarker marker : panel){
                allGenes.add(marker);
            }
            allGeneList.setModel(new DefaultListModel());
            allGeneList.setModel(allGeneModel);
            selectedGenesList.setModel(new DefaultListModel());
            selectedGenesList.setModel(selectedGenesModel);
        }
    }

    @Subscribe public void receive(ProjectEvent pe, Object source){
        DSDataSet ds = pe.getDataSet();
        if (ds != null && ds instanceof DSMicroarraySet){
            dataset = (DSMicroarraySet)ds;
        }
    }

    @Publish public AdjacencyMatrixEvent publishAdjacencyMatrixEvent(AdjacencyMatrixEvent ae){
        return ae;
    }    
    
    @Publish public ProjectNodeAddedEvent publishProjectNodeAddedEvent(ProjectNodeAddedEvent pe){
        return pe;
    }
    
    class EntrezIdComparator implements Comparator<DSGeneMarker>{
        public int compare(DSGeneMarker m1, DSGeneMarker m2){
            return (new Integer(m1.getGeneId())).compareTo(new Integer(m2.getGeneId()));
        }
    }
}