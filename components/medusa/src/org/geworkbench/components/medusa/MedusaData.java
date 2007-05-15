package org.geworkbench.components.medusa;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.bison.datastructure.biocollections.microarrays.CSMicroarraySet;

/**
 * 
 * @author keshav
 * @version $Id: MedusaData.java,v 1.3 2007-05-15 20:38:37 keshav Exp $
 */
public class MedusaData implements Serializable {
	private Log log = LogFactory.getLog(this.getClass());

	private CSMicroarraySet arraySet;

	// private List<RulesBean> rulesBeans = null; TODO - get from Medusa team

	private List<String> regulators = null;

	private List<String> targets = null;

	/**
	 * 
	 * @param arraySet
	 * @param regulators
	 * @param targets
	 */
	public MedusaData(CSMicroarraySet arraySet, List<String> regulators,
			List<String> targets) {
		this.arraySet = arraySet;
		this.regulators = regulators;
		this.targets = targets;
	}

	public CSMicroarraySet getArraySet() {
		return arraySet;
	}

	public void setArraySet(CSMicroarraySet arraySet) {
		this.arraySet = arraySet;
	}

}
