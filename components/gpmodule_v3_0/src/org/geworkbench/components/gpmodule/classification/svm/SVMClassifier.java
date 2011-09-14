/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2010) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/
package org.geworkbench.components.gpmodule.classification.svm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.webservice.Parameter;
import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMicroarray;
import org.geworkbench.bison.datastructure.complex.panels.DSPanel;
import org.geworkbench.components.gpmodule.GPDataset;
import org.geworkbench.components.gpmodule.classification.PredictionModel;
import org.geworkbench.components.gpmodule.classification.VisualGPClassifier;

/**
 * @author Marc-Danie Nazaire
 * @version $Id$
 */
public class SVMClassifier extends VisualGPClassifier
{
	private static final long serialVersionUID = 8241480979307707499L;

	public SVMClassifier(DSDataSet<?> parent, String label, String[] classifications, PredictionModel model,
                         GPDataset dataset, DSPanel<DSMicroarray> casePanel, DSPanel<DSMicroarray> controlPanel)
    {
        super("SVM", parent, label, classifications, dataset, model, casePanel, controlPanel);
    }

    public Parameter[] buildParametersList(List<float[]> data, List<String> arrayNames, String[] classLabels)
    {
        File testData = createTestGCTFile("SVMTest_Data", data, arrayNames);
        File testCLSData = createTestCLSFile("SVMTest_Cls", data.size(), classLabels);

        List<Parameter> parameters = new ArrayList<Parameter>();

        parameters.add(new Parameter("saved.model.filename", predModel.getPredModelFile().getAbsolutePath()));
        parameters.add(new Parameter("test.data.filename", testData.getAbsolutePath()));
        parameters.add(new Parameter("test.cls.filename", testCLSData.getAbsolutePath()));
        parameters.add(new Parameter("pred.results.output.file", predModel.getPredModelFile().getName() + "pred"));

        return (Parameter[])parameters.toArray(new Parameter[0]);
    }
}