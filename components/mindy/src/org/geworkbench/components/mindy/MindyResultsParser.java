package org.geworkbench.components.mindy;

import org.geworkbench.bison.datastructure.biocollections.microarrays.CSMicroarraySet;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.util.pathwaydecoder.mutualinformation.MindyData;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * For reading in data file and creating Mindy Data.
 * @author mhall
 * @version $ID$
 */
public class MindyResultsParser {

	/**
	 * Reads in results file (data) to create Mindy Data.
	 * @param maSet - microarray set
	 * @param resultsFile - data file
	 * @return - Data formatted for MINDY a la MindyData class.
	 * @throws IOException
	 */
    public static MindyData parseResults(CSMicroarraySet maSet, File resultsFile) throws IOException {
        List<MindyData.MindyResultRow> rows = new ArrayList<MindyData.MindyResultRow>();
        BufferedReader in = new BufferedReader(new FileReader(resultsFile));

        //read in the header 
        in.readLine();
        
        String line = in.readLine();
        while (line != null) {
            String[] tokens = line.split("\t");
            DSGeneMarker transFac = maSet.getMarkers().get(tokens[0]);
            DSGeneMarker hub = maSet.getMarkers().get(tokens[1]);
            DSGeneMarker target = maSet.getMarkers().get(tokens[2]);
            float score = Float.valueOf(tokens[3]);
            float pvalue = Float.valueOf(tokens[4]);
            rows.add(new MindyData.MindyResultRow(hub, transFac, target, score, pvalue));

            line = in.readLine();
        }
        return new MindyData(maSet, rows, 0);
    }
}
