package org.geworkbench.components.medusa.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.File;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.components.medusa.MedusaUtil;
import org.ginkgo.labs.gui.SwingUtil;
import org.ginkgo.labs.psam.PsamUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.larvalabs.chart.PSAMPlot;

import edu.columbia.ccls.medusa.io.SerializedRule;
import edu.columbia.ccls.medusa.sequence.pssm.JensenShannonDivergence;

/**
 *
 * @author keshav
 * @version $Id: DiscreteHitOrMissHeatMapPanel.java,v 1.1 2007/05/23 17:31:22
 *          keshav Exp $
 */
public class DiscreteHitOrMissHeatMapPanel extends JPanel implements
        MouseListener, MouseMotionListener {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Log log = LogFactory.getLog(this.getClass());

    private String sequencePath = null;

    private String rulePath = null;

    private List<String> ruleFiles = null;

    private List<String> targetNames = null;

    private ArrayList<SerializedRule> srules = null;

    private boolean[][] hitOrMissMatrix = null;

    private JTabbedPane parentPanel = null;

    public ArrayList rectRule = new ArrayList();
    private static final int COLUMN_WIDTH = 80;
    private static final double thresholdDistance = 1.0;
    JPanel pssmPanel;
    List TFInfoBeanArr = new ArrayList();
    List matchedTFInfoBeanArr = new ArrayList();
    JScrollPane TFScrollPane = new JScrollPane();

    /**
     *
     * @param rulePath
     * @param ruleFiles
     * @param targetNames
     */
    public DiscreteHitOrMissHeatMapPanel(String rulePath,
                                         List<String> ruleFiles, List<String> targetNames,
                                         String sequencePath) {

        log.debug("Constructing " + this.getClass().getSimpleName());

        this.rulePath = rulePath;

        this.ruleFiles = ruleFiles;

        this.sequencePath = sequencePath;

        this.targetNames = targetNames;

        srules = MedusaUtil.getSerializedRules(ruleFiles, rulePath);

        hitOrMissMatrix = MedusaUtil.generateHitOrMissMatrix(targetNames,
                srules, sequencePath);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    /*
      * (non-Javadoc)
      *
      * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
      */
    @SuppressWarnings("unchecked")
    public void paintComponent(Graphics g) {

        clear(g);
        rectRule.clear();

        Graphics2D g2d = (Graphics2D) g;

        int row = 15;
        int col = 25;

        for (int i = 0; i < targetNames.size(); i++) {
            col = 15;
            for (int j = 0; j < ruleFiles.size(); j++) {
                boolean isHit = hitOrMissMatrix[i][j];

                Rectangle2D.Double rect = new Rectangle2D.Double(col, row, 15,
                        15);
                if (isHit)
                    g2d.setColor(Color.blue);
                else
                    g2d.setColor(Color.black);

                g2d.fill(rect);
                col = col + 15;
            }
            row = row + 15;
        }
        int x = 15, y = 15;
        for (SerializedRule srule : srules) {
            Rectangle2D.Double rect = new Rectangle2D.Double(x, y, 15,
                    15 * targetNames.size());
            x += 15;
            rectRule.add(new RectangleRule(rect, srule));
        }
    }

    /**
     * @param label
     *            The String to draw.
     * @param g2d
     * @param row
     *            The row from where to draw the text.
     * @param lcol
     *            The (incremental) column where the text should be drawn.
     */
    @SuppressWarnings("unchecked")
    private void drawColumnNames(SerializedRule srule, Graphics2D g2d, int row,
                                 int lcol) {

        String sequence = MedusaUtil.generateConsensusSequence(srule.getPssm());

        // TODO move method into a "gui util" class
        AffineTransform fontAT = new AffineTransform();

        /* slant text backwards */
        // fontAT.shear(0.2, 0.0);
        /* counter-clockwise 90 degrees */
        fontAT.setToRotation(Math.PI * 3.0f / 2.0f);
        Font font = new Font("Helvetica", Font.ITALIC, 12);
        Font theDerivedFont = font.deriveFont(fontAT);

        FontRenderContext frc = g2d.getFontRenderContext();
        TextLayout tstring = new TextLayout(sequence, theDerivedFont, frc);

        tstring.draw(g2d, lcol, row);
        Rectangle2D stringBound = tstring.getOutline(fontAT).getBounds2D();

        Rectangle2D.Double rect = new Rectangle2D.Double(lcol
                - stringBound.getHeight(), row - stringBound.getWidth(),
                stringBound.getHeight(), stringBound.getWidth());
        rectRule.add(new RectangleRule(rect, srule));
    }

    /**
     *
     * @param g
     */
    protected void clear(Graphics g) {
        super.paintComponent(g);
    }

    public void setParentPanel(JTabbedPane parentPanel) {
        this.parentPanel = parentPanel;
    }

    public void mouseDragged(MouseEvent mouseEvent) {
        // Do Nothing
    }

    public void mouseMoved(MouseEvent e) {
        // Graphics g = getGraphics();
        // ToDO: implement this.. highlist the rect area where the mouse is
        boolean check = false;
        for (Iterator itr = rectRule.iterator(); itr.hasNext();) {
            // getPreferredSize();
            RectangleRule rectangleRule = (RectangleRule) itr.next();
            if (rectangleRule.rect.contains(e.getX(), e.getY())) {
                check = true;
                break;
                // uncomment following, if border of the rectangle needs to be
                // shown on mouse-hover
                /*
                     * this.repaint(); //g.setXORMode(Color.black);
                     * g.setColor(Color.YELLOW);
                     * g.drawRect((int)rectangleRule.rect.getX(),
                     * (int)rectangleRule.rect.getY(),
                     * (int)rectangleRule.rect.getWidth(),
                     * (int)rectangleRule.rect.getHeight());
                     */
            }
        }
        if (check) {
            if (this.getCursor().getType() != Cursor.HAND_CURSOR)
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            if (this.getCursor().getType() != Cursor.DEFAULT_CURSOR)
                this.setCursor(Cursor
                        .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public void mouseClicked(MouseEvent e) {
        // TODO: implement this... open new PSSM Tab
        for (Iterator itr = rectRule.iterator(); itr.hasNext();) {
            // getPreferredSize();
            RectangleRule rectangleRule = (RectangleRule) itr.next();
            if (rectangleRule.rect.contains(e.getX(), e.getY())) {
                parentPanel.remove(pssmPanel);
                addTab(rectangleRule.rule);
                break;
            }
        }
    }

    private void addTab(final SerializedRule rule) {
        pssmPanel = new JPanel();
        JScrollPane mainScrollPane = new JScrollPane();
        TFInfoBeanArr = new ArrayList();
        matchedTFInfoBeanArr = new ArrayList();

        /*
		 * mainScrollPane
		 * .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		 * mainScrollPane
		 * .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		 */

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
                "center:default", // columns
                ""));// rows added dynamically

        PSAMPlot psamPlot = new PSAMPlot(PsamUtil.convertScoresToWeights(rule
                .getPssm(), true));
        psamPlot.setMaintainProportions(false);
        psamPlot.setAxisDensityScale(4);
        psamPlot.setAxisLabelScale(3);
        BufferedImage image = new BufferedImage(
                MedusaVisualComponent.IMAGE_WIDTH,
                MedusaVisualComponent.IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        psamPlot.layoutChart(MedusaVisualComponent.IMAGE_WIDTH,
                MedusaVisualComponent.IMAGE_HEIGHT, graphics
                        .getFontRenderContext());
        psamPlot.paint(graphics);
        ImageIcon psamImage = new ImageIcon(image);

        // add the image as a label
        builder.append(new JLabel(psamImage));

        // add the table
        JTable pssmTable = PsamUtil.createPssmTable(rule.getPssm(),
                "Nucleotides");

        TableColumn column = null;
        for (int k = 0; k < 5; k++) {
            column = pssmTable.getColumnModel().getColumn(k);
            if (k > 0) {
                column.setPreferredWidth(COLUMN_WIDTH);
            }
        }
        pssmTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scrollPane = new JScrollPane(pssmTable);
        scrollPane
                .setPreferredSize(new Dimension(psamImage.getIconWidth(), 120));

        builder.append(scrollPane);

        JPanel transFacButtonPanel = new JPanel();
        final List<JRadioButton> buttons = SwingUtil.createRadioButtonGroup("JASPAR",
                "Custom");
        for (JRadioButton b : buttons) {
            transFacButtonPanel.add(b);
        }


        // add search results table
        JPanel pssmButtonPanel = new JPanel();
        final JButton exportButton = SwingUtil.createButton("Export",
                "Export search results to file in PSSM file format.");
        exportButton.setEnabled(false);
        pssmButtonPanel.add(exportButton);
        final JButton searchButton = SwingUtil.createButton("Search",
                "Executes a database search.");
        pssmButtonPanel.add(searchButton);
        searchButton.setEnabled(false);


        JButton loadTransFacButton = SwingUtil
                .createButton("Load TF",
                        "Load file containing new transcription factors to add to the TF listing.");
        transFacButtonPanel.add(loadTransFacButton);
        builder.append(transFacButtonPanel);

        TFScrollPane.setPreferredSize(new Dimension(psamImage.getIconWidth(), 100));
        builder.append(TFScrollPane);
        TFScrollPane.setVisible(false);

        loadTransFacButton.addActionListener(new ActionListener(){
            /*
             * (non-Javadoc)
             *
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(ActionEvent e) {

                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(new File("."));

                int returnVal = chooser.showOpenDialog(DiscreteHitOrMissHeatMapPanel.this);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    File chosenFile = chooser.getSelectedFile();
                    if(buttons.get(0).isSelected()){
                        List loadedBeans = MedusaUtil.readPssmFromJasperFile(chosenFile.getAbsolutePath());
                        if(loadedBeans == null){
                            JOptionPane.showMessageDialog(null, "File could not be loaded.", "I/O Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if(loadedBeans.size() == 0){
                            JOptionPane.showMessageDialog(null, "No TF found in the selected file.", "No TF found", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        TFInfoBeanArr.addAll(loadedBeans);
                    }
                    else{
                        List loadedBeans = MedusaUtil.readPssmFromFile(chosenFile.getAbsolutePath());
                        if(loadedBeans == null){
                            JOptionPane.showMessageDialog(null, "File could not be loaded.", "I/O Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if(loadedBeans.size() == 0){
                            JOptionPane.showMessageDialog(null, "No TF found in the selected file.", "No TF found", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        TFInfoBeanArr.addAll(loadedBeans);
                    }
                    searchButton.setEnabled(true);
                    JOptionPane.showMessageDialog(null, "TF File loaded", "File Loaded", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });



        searchButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                matchedTFInfoBeanArr.clear();
                double[] backgroundPercents = {0.25, 0.25,0.25,0.25};
                JensenShannonDivergence jd = new JensenShannonDivergence(backgroundPercents, 17);
                boolean foundMatchedPSSM = false;
                for(Iterator itr=TFInfoBeanArr.iterator();itr.hasNext();){
                    TranscriptionFactorInfoBean nextBean = (TranscriptionFactorInfoBean)itr.next();
                    double[] distance = jd.getDistance(rule.getPssm(), nextBean.getPssm());
                    nextBean.setDistance(distance[0]);
                    if(distance[0] < thresholdDistance){
                        matchedTFInfoBeanArr.add(nextBean);
                        foundMatchedPSSM = true;
                    }
                }
                if(!foundMatchedPSSM){
                    JOptionPane.showMessageDialog(null, "No PSSM satisfied the maximum threshold criteria", "zero search results", JOptionPane.INFORMATION_MESSAGE);
                }
                TableModel dataModel = new AbstractTableModel(){
                    public int getColumnCount() { return 3; }
                    public int getRowCount() { return matchedTFInfoBeanArr.size();}
                    public Object getValueAt(int row, int col) {
                        TranscriptionFactorInfoBean infoBean = (TranscriptionFactorInfoBean)matchedTFInfoBeanArr.get(row);
                        if(col==0)
                            return infoBean.getName();
                        if(col==1)
                            return infoBean.getSource();
                        else
                            return infoBean.getDescription();
                    }
                    public String getColumnName(int col){
                        if(col==1)
                            return "TF";
                        if(col==2)
                            return "Source";
                        else
                            return "Description";
                    }
                };
                JTable table = new JTable(dataModel);
                TFScrollPane.setViewportView(table);

                TFScrollPane.setVisible(true);
                if(matchedTFInfoBeanArr.size() > 0){
                    exportButton.setEnabled(true);
                }
                pssmPanel.revalidate();
            }
        });

        exportButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(new File("."));

                int returnVal = chooser.showSaveDialog(DiscreteHitOrMissHeatMapPanel.this);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    File chosenFile = chooser.getSelectedFile();
                    boolean success = MedusaUtil.writeMatchedPssmsToFile(matchedTFInfoBeanArr, chosenFile);
                    if(!success)
                        JOptionPane.showMessageDialog(null, "Could not write the PSSMs to the file", "I/O Error !", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        builder.append(pssmButtonPanel);
        mainScrollPane.setPreferredSize(new Dimension((int) psamImage
                .getIconWidth() + 10, (int) parentPanel.getPreferredSize()
                .getHeight() - 30));
        builder.getPanel().setPreferredSize(
                new Dimension((int) mainScrollPane.getPreferredSize()
                        .getWidth() + 10, (int) mainScrollPane
                        .getPreferredSize().getHeight() + 110));

        mainScrollPane.setViewportView(builder.getPanel());
        JPanel mainPSSMPanel = new JPanel();
        mainPSSMPanel.add(mainScrollPane);

        pssmPanel.add(mainPSSMPanel);

        // end the main logic

        parentPanel.add("PSSM", pssmPanel);
        parentPanel.setSelectedComponent(pssmPanel);
    }

    public void mousePressed(MouseEvent mouseEvent) {
        // Do Nothing
    }

    public void mouseReleased(MouseEvent mouseEvent) {
        // Do Nothing
    }

    public void mouseEntered(MouseEvent mouseEvent) {
        // Do Nothing
    }

    public void mouseExited(MouseEvent mouseEvent) {
        // Do Nothing
    }

    public ArrayList getRectRule() {
        return rectRule;
    }

    public class RectangleRule {
        Rectangle2D rect;
        SerializedRule rule;

        public RectangleRule(Rectangle2D rect, SerializedRule rule) {
            this.rect = rect;
            this.rule = rule;
        }
    }
}