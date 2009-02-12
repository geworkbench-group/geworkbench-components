package org.geworkbench.components.normalization;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.geworkbench.analysis.AbstractSaveableParameterPanel;
import org.geworkbench.events.listeners.ParameterActionListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Columbia University</p>
 * @author non attributable
 */

/**
 * Parameters panels used by the <code>QuantileNormalizer</code>.
 * @author unknown, yc2480
 * @version $ID$
 */
public class QuantileNormalizerPanel extends AbstractSaveableParameterPanel implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 4689662471445840601L;
	
	final String MARKER_OPTION = "Mean profile marker";
    final String MICROARRAY_OPTION = "Mean microarray value";
    private GridLayout gridLayout1 = new GridLayout();
    private JLabel averagingTypeLabel = new JLabel("Averaging method");
    private JComboBox averagingTypeSelection = new JComboBox(new String[]{MARKER_OPTION, MICROARRAY_OPTION});

    private static class SerialInstance implements Serializable {

        /**
		 * 
		 */
		private static final long serialVersionUID = 2525140046457874959L;
		private int averaging;

        public SerialInstance(int averaging) {
            this.averaging = averaging;
        }

        Object readResolve() throws ObjectStreamException {
            QuantileNormalizerPanel panel = new QuantileNormalizerPanel();
            panel.averagingTypeSelection.setSelectedIndex(averaging);
            return panel;
        }

    }

    public Object writeReplace() throws ObjectStreamException {
        return new SerialInstance(averagingTypeSelection.getSelectedIndex());
    }
    
	/*
	 * (non-Javadoc)
	 * @see org.geworkbench.analysis.AbstractSaveableParameterPanel#setParameters(java.util.Map)
	 * Set inputed parameters to GUI.
	 */
    @Override
    public void setParameters(Map<Serializable, Serializable> parameters){
        if (parameters==null){
        	return;
        }
        Set<Map.Entry<Serializable, Serializable>> set = parameters.entrySet();
        for (Iterator<Map.Entry<Serializable, Serializable>> iterator = set.iterator(); iterator.hasNext();) {
        	Map.Entry<Serializable, Serializable> parameter = iterator.next();
			Object key = parameter.getKey();
			Object value = parameter.getValue();
			if (key.equals("averagingTypeSelection")){
				this.averagingTypeSelection.setSelectedIndex((Integer)value);
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
		parameters.put("averagingTypeSelection", averagingTypeSelection.getSelectedIndex());
		return parameters;
	}
    
    public QuantileNormalizerPanel() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void jbInit() throws Exception {
        FormLayout layout = new FormLayout(
                "right:max(40dlu;pref), 8dlu, max(60dlu;pref)",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("Quantile Parameters");

        builder.append("Missing values averaging method", averagingTypeSelection);
        this.add(builder.getPanel(), BorderLayout.CENTER);
        ParameterActionListener parameterActionListener = new ParameterActionListener(this);
        averagingTypeSelection.addActionListener(parameterActionListener);
    }

    /**
     * Return the user-specified parameter that designates if a missing value for
     * a marker X within a microarray Y will be replaced by the mean value of the
     * marker X across all micorarrays in the set, or with the mean value of all
     * markers within Y.
     *
     * @return <code>MARKER_PROFILE_MEAN</code> or <code>MICROARRAY_MEAN</code>.
     */
    public int getAveragingType() {
        if (averagingTypeSelection.getSelectedItem().equals(MARKER_OPTION))
            return MissingValueNormalizer.MARKER_PROFILE_MEAN;
        else
            return MissingValueNormalizer.MICROARRAY_MEAN;
    }

    public void setAveragingType(String type){
        if(type==null){
            return;
        }
        if(type.equalsIgnoreCase(MARKER_OPTION)){
         averagingTypeSelection.setSelectedItem(MARKER_OPTION);
        }
        if(type.equalsIgnoreCase(MICROARRAY_OPTION)){
         averagingTypeSelection.setSelectedItem(MICROARRAY_OPTION);
        }
    };


    public void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    public void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        revalidate();
    }

}

