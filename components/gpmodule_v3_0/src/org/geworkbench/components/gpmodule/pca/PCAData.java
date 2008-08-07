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
package org.geworkbench.components.gpmodule.pca;

import org.tigr.util.FloatMatrix;
import org.tigr.microarray.mev.cluster.gui.impl.pca.ValuesViewer;
import org.genepattern.io.odf.OdfParser;
import org.genepattern.io.odf.OdfHandler;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.io.FileInputStream;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.text.DecimalFormat;

/**
 * @author: Marc-Danie Nazaire
 */
public class PCAData
{
    private FloatMatrix S_matrix;
    private FloatMatrix T_matrix;
    private FloatMatrix U_matrix;
    private int numPCs;
    private HashMap eigenValues;
    private HashMap percentVar;
    private HashMap eigenVectors;
    private String variables;

    public PCAData(List files, String variables)
    {
        try
        {
            this.variables = variables;
            initMatrices(files);
            extractPCAValuesViewerResults();
            extractEigenVectorResults();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void initMatrices(List<String> files) throws Exception
    {
        for(String file : files)
        {
            OdfObject odfObject = new OdfObject(file);
            float[][] data = odfObject.getData();

            if(file.contains("s.odf"))
                S_matrix = new FloatMatrix(data);
            if(file.contains("t.odf"))
                T_matrix = new FloatMatrix(data);
            if(file.contains("u.odf"))
                U_matrix = new FloatMatrix(data);
        }
    }

    private void extractPCAValuesViewerResults() throws Exception
    {
        ValuesViewer valuesViewerPanel = new ValuesViewer(S_matrix);
        eigenValues = new HashMap();
        percentVar = new HashMap();

        JTextArea content = (JTextArea)valuesViewerPanel.getContentComponent();
        String textData = content.getText();

        int prinCompIndex = textData.lastIndexOf("Principal Component");

        String[] numPCsLine = textData.substring(prinCompIndex, textData.indexOf("\t", prinCompIndex)).split(" ");
        numPCs = Integer.parseInt(numPCsLine[2]);

        PlainDocument document = (PlainDocument)content.getDocument();
        for(int i = 0; i < numPCs; i++)
        {
            int start = document.getDefaultRootElement().getElement(i).getStartOffset();
            int length = document.getDefaultRootElement().getElement(i).getEndOffset() - start;

            String line = document.getText(start, length);
            String[] lineSplit = line.split("\t");

            Integer id = new Integer(Integer.parseInt(lineSplit[0].substring(lineSplit[0].lastIndexOf(" ")+1, lineSplit[0].length())));
            Double eigenValue = Double.valueOf(lineSplit[1]);
            String var = String.valueOf(Double.parseDouble(lineSplit[2].replace("%", "")) + "%");

            eigenValues.put(id, eigenValue);
            percentVar.put(id, var);
        }
    }

    private void extractEigenVectorResults() throws Exception
    {
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(2);
        decimalFormat.setMinimumFractionDigits(2);
        eigenVectors = new HashMap();

        for(int c = 0; c < T_matrix.getColumnDimension(); c++)
        {
            List eigenVector = new ArrayList();
            for(int r = 0; r < T_matrix.getRowDimension(); r++)
            {
                eigenVector.add(decimalFormat.format(T_matrix.get(r, c)));
            }

            eigenVectors.put(new Integer(c+1), eigenVector);
        }
    }

    private class OdfObject
    {
        MyOdfHandler handler;
        public OdfObject(String file) throws Exception
        {
            FileInputStream fis = null;
            try
            {
                fis = new FileInputStream(file);
                OdfParser parser = new OdfParser();
                handler = new MyOdfHandler();
                parser.setHandler(handler);
                parser.parse(fis);
            }
            catch(Exception e)
            {
                throw e;
            }
        }

        public int getColumnCount()
        {
            return handler.numColumns;
        }

        public int getRowCount()
        {
            return handler.numRows;
        }

        public float getValueAt(int row, int col)
        {
            return Float.valueOf(handler.data[row][col]).floatValue();
        }

        public float[][] getData()
        {
            return handler.data;
        }

    }

    private class MyOdfHandler implements OdfHandler
    {
        int numRows;
        int numColumns;
        float[][] data = null;

        public MyOdfHandler(){}

        public void endHeader(){}

        public void header(String key, String[] values){}
        
        public void header(String key, String value)
        {
            if (key.equalsIgnoreCase("DataLines"))
                numRows = Integer.parseInt(value);

             if(key.equalsIgnoreCase("Columns"))
                numColumns = Integer.parseInt(value);
        }

        public void data(int row, int column, String d)
        {
            if(data == null)
            {
                data = new float[numRows][numColumns];
            }
                        
            data[row][column] = Float.valueOf(d).floatValue();
        }
    }

    public int getNumPCs()
    {
        return numPCs;
    }

    public HashMap getEigenValues()
    {
        return eigenValues;
    }

    public HashMap getPercentVars()
    {
        return percentVar;
    }

    public HashMap getEigenVectors()
    {
        return eigenVectors;
    }

    public FloatMatrix getUMatrix()
    {
        return U_matrix;
    }

    public String getVariables()
    {
        return variables;
    }
}
