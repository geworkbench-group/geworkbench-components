/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2007) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/
package org.geworkbench.components.analysis.classification.wv;

import org.geworkbench.algorithms.AbstractTrainingPanel;
import org.geworkbench.util.ClassifierException;
import org.geworkbench.bison.algorithm.classification.CSClassifier;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.datastructure.complex.panels.DSItemList;
import org.geworkbench.bison.model.analysis.ParamValidationResults;
import org.geworkbench.builtin.projects.LoadData;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.ColumnSpec;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.io.ObjectStreamException;

/**
 * @author Marc-Danie Nazaire
 */
public class WVTrainingPanel extends AbstractTrainingPanel
{
    private static final int DEFAULT_NUM_FEATURES = 10;

    private String featureFile = null;
    private javax.swing.JTextField featureFileTextBox;
    private JButton loadFeatureFileButton;
    private JFileChooser featureFileChooser = new JFileChooser();
    private JFormattedTextField numFeatures;
    private JComboBox statistic;
    private JFormattedTextField minStdDev;
    private JCheckBox medianCheckbox;
    private JCheckBox minStdDevCheckbox;
    private JRadioButton numFeatureMethod;
    private JRadioButton featureFileMethod;
    private WVTraining wvTraining;

    public WVTrainingPanel(WVTraining wv)
    {
        this.wvTraining = wv;
        try
        {   jbInit(); }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected void initUI()
    {
        numFeatureMethod = new JRadioButton();
        numFeatureMethod.setText("num features");
        numFeatureMethod.setSelected(true);
        numFeatureMethod.addItemListener( new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                if(e.getStateChange() == 1)
                {
                    numFeatures.setEnabled(true);
                    statistic.setEnabled(true);
                    medianCheckbox.setEnabled(true);
                    minStdDevCheckbox.setEnabled(true);
                }
                else
                {
                    numFeatures.setEnabled(false);
                    statistic.setEnabled(false);
                    medianCheckbox.setEnabled(false);
                    minStdDevCheckbox.setEnabled(false);
                }
            }
        });

        numFeatures = new JFormattedTextField();
        numFeatures.setValue(DEFAULT_NUM_FEATURES);

        featureFileMethod = new JRadioButton();
        featureFileMethod.setText("feature filename");
        featureFileMethod.addItemListener( new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                if(e.getStateChange() == 1)
                {
                    featureFileTextBox.setEnabled(true);
                    loadFeatureFileButton.setEnabled(true);
                }
                else
                {
                    featureFileTextBox.setEnabled(false);
                    loadFeatureFileButton.setEnabled(false);
                }
            }
        });

        featureFileTextBox = new JTextField();
        featureFileTextBox.setEnabled(false);
        loadFeatureFileButton = new JButton("Load");
        loadFeatureFileButton.setEnabled(false);
        loadFeatureFileButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                featureFileLoadHandler();
            }
        });

        ButtonGroup group = new ButtonGroup();
        group.add(numFeatureMethod);
        group.add(featureFileMethod);

        statistic = new JComboBox();
        statistic.addItem("SNR");
        statistic.addItem("T-Test");

        medianCheckbox = new JCheckBox();
        medianCheckbox.setText("median") ;

        minStdDevCheckbox = new JCheckBox();
        minStdDevCheckbox.setText("min std dev");
        minStdDevCheckbox.addItemListener( new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                if(e.getStateChange() == 1)
                {   minStdDev.setEnabled(true);     }
                else
                {   minStdDev.setEnabled(false);    }
            }
        });

        minStdDev = new JFormattedTextField("");
        minStdDev.setEnabled(false);
    }

    public int getNumFeatures()
    {
        return ((Integer)numFeatures.getValue()).intValue();
    }

    public String getFeatureFile()
    {
        return this.featureFile;
    }

    public String getStatistic()
    {
        return (String)statistic.getSelectedItem();
    }

    public String getMinStdDev()
    {
        return (String)minStdDev.getValue();
    }

    public boolean useMinStdDev()
    {
        return minStdDevCheckbox.isSelected();
    }

    public boolean useMedian()
    {
        return medianCheckbox.isSelected();
    }

    public boolean useFeatureFileMethod()
    {
        if(featureFileMethod.isSelected())
            return true;

        return false;
    }

    public DSItemList<DSGeneMarker> getFeatures()
    {
        return maSet.getMarkers();
    }

    private JLabel getGPLogo()
    {
        java.net.URL imageURL = WVTrainingPanel.class.getResource("/images/gp-logo.jpg");
        ImageIcon image = new ImageIcon(imageURL);

        JLabel label = new JLabel();
        label.setIcon(image);

        return label;
    }

    protected void addParameters(DefaultFormBuilder builder)
    {
        builder.appendSeparator("Weighted Voting Parameters");
        builder.nextRow();

        builder.appendColumn(new ColumnSpec("9dlu"));
        builder.append(numFeatureMethod, numFeatures);

        // add the GenePattern logo
        builder.setColumn(7);
        builder.add(getGPLogo());
        builder.nextRow();

        builder.append("feature selection statistic", statistic);
        builder.nextRow();

        builder.append(minStdDevCheckbox, minStdDev, medianCheckbox);
        builder.nextRow();

        builder.append(featureFileMethod, featureFileTextBox, loadFeatureFileButton);
    }

    protected CSClassifier trainForValidation(java.util.List<float[]> trainingCaseData, java.util.List<float[]> trainingControlData) throws ClassifierException
    {
        setTrainingTask(this.wvTraining);
        return wvTraining.trainClassifier(trainingCaseData, trainingControlData);
    }

    public ParamValidationResults validateParameters()
    {
        if(getNumFeatures() <= 0)
            return new ParamValidationResults(false, "num features must be greater than 0");
        else if(getNumFeatures() > getFeatures().size())
            return new ParamValidationResults(false, "num features can not be greater than " + getFeatures().size());
        else if(useMinStdDev() && getMinStdDev() == null)
            return new ParamValidationResults(false, "min std dev not provided");
        else if(useMinStdDev() && Double.parseDouble(getMinStdDev()) <= 0)
            return new ParamValidationResults(false, "min std dev must be greater than 0");
        else
            return new ParamValidationResults(true, "WV Parameter validations passed");
    }

    private void featureFileLoadHandler()
    {
        String lwd = LoadData.getLastDataDirectory();
        featureFileChooser.setCurrentDirectory(new File(lwd));
        featureFileChooser.showOpenDialog(this);
        File file = featureFileChooser.getSelectedFile();
        if (file != null)
        {
            featureFile = featureFileChooser.getSelectedFile().getAbsolutePath();
            featureFileTextBox.setText(featureFile);
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializedInstance(numFeatureMethod.isSelected(), (Integer)numFeatures.getValue(), (String)statistic.getSelectedItem(), medianCheckbox.isSelected(),
                minStdDevCheckbox.isSelected(), (String)minStdDev.getValue(), featureFileMethod.isSelected(), featureFile);
    }

    private static class SerializedInstance implements Serializable
    {
        private boolean numFeatureMethod;
        private int numFeatures;
        private String statistic;
        private boolean useMedian;
        private boolean useMinStdDev;
        private String minStdDev;
        private boolean featureFileMethod;
        private String featureFile;

        public SerializedInstance(Boolean numFeatureMethod, Integer numFeatures, String statistic, Boolean useMedian,
                                  Boolean useMinStdDev, String minStdDev, Boolean featureFileMethod, String featureFile)
        {
            this.numFeatureMethod = numFeatureMethod;
            this.numFeatures = numFeatures;
            this.statistic = statistic;
            this.useMedian = useMedian;
            this.minStdDev = minStdDev;
            this.useMinStdDev = useMinStdDev;
            this.featureFileMethod = featureFileMethod;
            this.featureFile = featureFile;
        }

        Object readResolve() throws ObjectStreamException
        {
            WVTrainingPanel panel = new WVTrainingPanel(null);
            panel.numFeatureMethod.setSelected(numFeatureMethod);
            panel.numFeatures.setValue(numFeatures);
            panel.statistic.setSelectedItem(statistic);
            panel.medianCheckbox.setSelected(useMedian);
            panel.minStdDevCheckbox.setSelected(useMinStdDev);
            panel.minStdDev.setValue(minStdDev);
            panel.featureFileMethod.setSelected(featureFileMethod);
            panel.featureFileTextBox.setText(featureFile);

            return panel;
        }
    }
}

                                                    

