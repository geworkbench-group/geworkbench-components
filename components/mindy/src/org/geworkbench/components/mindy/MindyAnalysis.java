package org.geworkbench.components.mindy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.analysis.AbstractAnalysis;
import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.bison.datastructure.biocollections.microarrays.CSMicroarraySet;
import org.geworkbench.bison.datastructure.biocollections.views.DSMicroarraySetView;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMicroarray;
import org.geworkbench.bison.datastructure.bioobjects.microarray.CSMicroarray;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMarkerValue;
import org.geworkbench.bison.datastructure.bioobjects.microarray.CSExpressionMarkerValue;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.bison.datastructure.bioobjects.markers.CSGeneMarker;
import org.geworkbench.bison.datastructure.complex.panels.DSItemList;
import org.geworkbench.bison.model.analysis.AlgorithmExecutionResults;
import org.geworkbench.bison.model.analysis.ClusteringAnalysis;
import org.geworkbench.engine.management.Publish;
import org.geworkbench.engine.management.Subscribe;
import org.geworkbench.util.pathwaydecoder.mutualinformation.MindyDataSet;
import org.geworkbench.util.pathwaydecoder.mutualinformation.MindyData;
import org.geworkbench.builtin.projects.ProjectPanel;
import org.geworkbench.events.GeneSelectorEvent;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import edu.columbia.c2b2.mindy.Mindy;
import edu.columbia.c2b2.mindy.MindyResults;
import wb.data.MicroarraySet;
import wb.data.Microarray;
import wb.data.MarkerSet;
import wb.data.Marker;

import javax.swing.*;

/**
 * @author Matt Hall
 */
public class MindyAnalysis extends AbstractAnalysis implements ClusteringAnalysis {
    Log log = LogFactory.getLog(this.getClass());

    private static final String TEMP_DIR = "temporary.files.directory";
    private MindyParamPanel paramPanel;

    public MindyAnalysis() {
        setLabel("MINDY");
        paramPanel = new MindyParamPanel();
        setDefaultPanel(paramPanel);
    }

    // not used
    public int getAnalysisType() {
        return AbstractAnalysis.ZERO_TYPE;
    }

    public AlgorithmExecutionResults execute(Object input) {
        log.debug("input: " + input);
        // Use this to get params
        MindyParamPanel params = (MindyParamPanel) aspp;
        DSMicroarraySet<DSMicroarray> mSet = ((DSMicroarraySetView) input).getMicroarraySet();

//        MindyData loadedData = null;
//        try {
//            loadedData = MindyResultsParser.parseResults((CSMicroarraySet) mSet, new File(params.getCandidateModulatorsFile()));
//        } catch (IOException e) {
//            log.error(e);
//        }
        String paramDesc = "Generated by MINDY run with paramters: \n";
        paramDesc += "Candidate modulators: ";
        ArrayList<Marker> modulators = new ArrayList<Marker>();
        ArrayList<String> modulatorGeneList = params.getModulatorGeneList();
        for (String modGene : modulatorGeneList) {
            DSGeneMarker marker = mSet.getMarkers().get(modGene);
            if (marker == null) {
                log.info("Couldn't find marker " + modGene + " from modulator file in microarray set.");
                JOptionPane.showMessageDialog(null, modGene + " candidate modulator not found in loadad microarray set.");
                return null;
            } else {
                paramDesc += modGene + " ";
                modulators.add(new Marker(modGene));
            }
        }
        paramDesc += "\n";

        paramDesc += "DPI Annotated Genes: ";
        ArrayList<Marker> dpiAnnots = new ArrayList<Marker>();
        ArrayList<String> dpiAnnotList = params.getDPIAnnotatedGeneList();
        for (String modGene : dpiAnnotList) {
            DSGeneMarker marker = mSet.getMarkers().get(modGene);
            if (marker == null) {
                log.info("Couldn't find marker " + modGene + " from DPI annotation file in microarray set.");
                JOptionPane.showMessageDialog(null, modGene + " DPI annotated gene not found in loadad microarray set.");
                return null;
            } else {
                paramDesc += modGene + " ";
                dpiAnnots.add(new Marker(modGene));
            }
        }
        paramDesc += "\n";

        Mindy mindy = new Mindy();
        String transcriptionFactor = params.getTranscriptionFactor();
        DSGeneMarker transFac = mSet.getMarkers().get(transcriptionFactor);
        if (transFac == null) {
            JOptionPane.showMessageDialog(null, "Specified hub gene (" + transcriptionFactor + ") not found in loadad microarray set.");
            return null;
        }
        paramDesc += "Transcription Factor: " + transcriptionFactor + "\n";

        boolean fullSetMI = true;
        float fullSetThreshold = 0f;
        if (params.getFullSetMIThreshold() > 0) {
            fullSetThreshold = params.getFullSetMIThreshold();
            paramDesc += "Full Set MI Threshold: " + fullSetThreshold + "\n";
        } else {
            fullSetMI = false;
            fullSetThreshold = params.getFullsetPValueThreshold();
            paramDesc += "Full Set P-Value Threshold: " + fullSetThreshold + "\n";
        }

        boolean subsetMI = true;
        float subsetThreshold = 0f;
        if (params.getSubsetMIThreshold() > 0) {
            subsetThreshold = params.getSubsetMIThreshold();
            paramDesc += "Conditional MI Threshold: " + subsetThreshold + "\n";
        } else {
            subsetMI = false;
            subsetThreshold = params.getSubsetPValueThreshold();
            paramDesc += "Conditional P-Value Threshold: " + subsetThreshold + "\n";
        }

        log.info("Running MINDY analysis.");
        float setFraction = params.getSetFraction() / 100f;
        paramDesc += "Set Fraction: " + setFraction + "\n";
        MindyResults results = mindy.runMindy(convert(mSet), new Marker(params.getTranscriptionFactor()), modulators,
                dpiAnnots, fullSetMI, fullSetThreshold, subsetMI, subsetThreshold,
                setFraction, params.getDPITolerance());
        log.info("MINDY analysis complete.");
        List<MindyData.MindyResultRow> dataRows = new ArrayList<MindyData.MindyResultRow>();
        for (MindyResults.MindyResultForTarget result : results) {
            DSItemList<DSGeneMarker> markers = mSet.getMarkers();
            DSGeneMarker target = markers.get(result.getTarget().getName());
            for (MindyResults.MindyResultForTarget.ModulatorSpecificResult specificResult : result) {
                DSGeneMarker mod = markers.get(specificResult.getModulator().getName());
                dataRows.add(new MindyData.MindyResultRow(mod, transFac, target, specificResult.getScore(), 0f));
            }
        }

        MindyData loadedData = new MindyData((CSMicroarraySet) mSet, dataRows);
        log.info("Done converting MINDY results.");


        MindyDataSet dataSet = new MindyDataSet(mSet, "MINDY Results", loadedData, params.getCandidateModulatorsFile());
        ProjectPanel.addToHistory(dataSet, paramDesc);
        return new AlgorithmExecutionResults(true, "MINDY Results Loaded.", dataSet);

    }

    private MicroarraySet convert(DSMicroarraySet<DSMicroarray> inSet) {
        MarkerSet markers = new MarkerSet();
        for (DSGeneMarker marker : inSet.getMarkers()) {
            markers.addMarker(new Marker(marker.getLabel()));
        }
        MicroarraySet returnSet = new MicroarraySet(inSet.getDataSetName(), "ID", "ChipType", markers);
        for (DSMicroarray microarray : inSet) {
            returnSet.addMicroarray(new Microarray(microarray.getLabel(), microarray.getRawMarkerData()));
        }
        return returnSet;
    }

    private DSMicroarraySet<DSMicroarray> convert(MicroarraySet inSet) {
        DSMicroarraySet<DSMicroarray> microarraySet = new CSMicroarraySet<DSMicroarray>();
        microarraySet.setLabel(inSet.getName());

        for (int i = 0; i < inSet.getMarkers().size(); i++) {
            /* cagrid array */
            Microarray inArray = inSet.getMicroarray(i);
            float[] arrayData = inArray.getValues();
            String arrayName = inArray.getName();

            /* bison array */
            CSMicroarray microarray = new CSMicroarray(arrayData.length);
            microarray.setLabel(arrayName);
            for (int j = 0; j < arrayData.length; j++) {
                DSMarkerValue markerValue = new CSExpressionMarkerValue(
                        arrayData[j]);
                microarray.setMarkerValue(j, markerValue);
            }
            microarraySet.add(i, microarray);

            // Create marker
            microarraySet.getMarkers().add(new CSGeneMarker(inSet.getMarkers().getMarker(i).getName()));
        }

        return microarraySet;
    }

    @Subscribe public void receive(GeneSelectorEvent e, Object source) {
        DSGeneMarker marker = e.getGenericMarker(); // GeneselectorEvent can be a panel event therefore won't work here,
        if (marker != null) { //so added this check point--xuegong
            paramPanel.setTranscriptionFactor(marker.getLabel());
        }
    }


    @Publish
    public MindyDataSet publishMatrixReduceSet(MindyDataSet data) {
        return data;
    }
}
