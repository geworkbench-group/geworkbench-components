package org.geworkbench.components.filtering;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.geworkbench.analysis.AbstractSaveableParameterPanel;
import org.geworkbench.bison.model.analysis.ParamValidationResults;
import org.geworkbench.events.listeners.ParameterActionListener;

/**
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: First Genetic Trust Inc.</p>
 * @author First Genetic Trust
 * @version 1.0
 */

/**
 * The parameters panel for the <code>MissingValuesFilter</code>. Prompts
 * the user to enter a number X. Markers whose value is missing in more than X
 * microarrays will be removed.
 * 
 * @author yc2480
 * @version $ID$
 */
public class MissingValuesFilterPanel extends AbstractSaveableParameterPanel implements Serializable {
    private JLabel maxMissingLabel = new JLabel("<html><p>Maximum number of</p><p>missing arrays</p></html>");
    private JFormattedTextField maxMissingValue = new JFormattedTextField();

    private static class SerialInstance implements Serializable {
        Integer value;

        public SerialInstance(Integer value) {
            this.value = value;
        }

        Object readResolve() throws ObjectStreamException {
            MissingValuesFilterPanel panel = new MissingValuesFilterPanel();
            panel.maxMissingValue.setValue(value);
            panel.revalidate();
            return panel;
        }
    }

    public MissingValuesFilterPanel() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void jbInit() throws Exception {
        JPanel container = new JPanel();
        this.setLayout(new FlowLayout());
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.add(Box.createHorizontalGlue());
        container.add(maxMissingLabel);
        container.add(maxMissingValue);
        container.setPreferredSize(new Dimension(220, 27));
        this.add(container);
        maxMissingValue.setValue(new Integer(0));
        maxMissingValue.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        ParameterActionListener parameterActionListener = new ParameterActionListener(this);
        maxMissingValue.addActionListener(parameterActionListener);
    }

    /**
     * Get the user-specifed maximum number of microarrays that a marker is allowed
     * to have a missing value so that it does not get filtered out.
     *
     * @return
     */
    public int getMaxMissingArrays() {
        return ((Number) maxMissingValue.getValue()).intValue();
    }

    /**
        * Set the user-specifed maximum number of microarrays that a marker is allowed
        * to have a missing value so that it does not get filtered out.
        *
        *
        */
       public void settMaxMissingArrays(int newValue) {
            maxMissingValue.setValue(newValue);
       }


    /**
     * Overrides the method from <code>AbstractSaveableParameterPanel</code>.
     * Provides an error message if the designated number of microarrays is a
     * negative number.
     *
     * @return
     */
    public ParamValidationResults validateParameters() {
        if (getMaxMissingArrays() < 0)
            return new ParamValidationResults(false, "The number of microarrays cannot be negative.");
        else
            return new ParamValidationResults(true, "No Error");
    }

//    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//        out.defaultWriteObject();
//    }
//
//    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//        in.defaultReadObject();
//        revalidate();
//    }

    public Object writeReplace() throws ObjectStreamException {
        return new SerialInstance((Integer) maxMissingValue.getValue());
    }

	/*
	 * (non-Javadoc)
	 * @see org.geworkbench.analysis.AbstractSaveableParameterPanel#setParameters(java.util.Map)
	 * Set inputed parameters to GUI.
	 */
    @Override
    public void setParameters(Map<Serializable, Serializable> parameters){
        Set<Map.Entry<Serializable, Serializable>> set = parameters.entrySet();
        for (Iterator<Map.Entry<Serializable, Serializable>> iterator = set.iterator(); iterator.hasNext();) {
        	Map.Entry<Serializable, Serializable> parameter = iterator.next();
			Object key = parameter.getKey();
			Object value = parameter.getValue();
			if (key.equals("maxMissingValue")){
	            this.maxMissingValue.setValue((Integer)value);
	            this.revalidate();

			}
		}
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.geworkbench.analysis.AbstractSaveableParameterPanel#getParameters()
	 */
    @Override
    public Map<Serializable, Serializable> getParameters() {
		Map<Serializable, Serializable> parameters = new HashMap<Serializable, Serializable>();
		parameters.put("maxMissingValue", (Integer) maxMissingValue.getValue());
		return parameters;
	}

}
