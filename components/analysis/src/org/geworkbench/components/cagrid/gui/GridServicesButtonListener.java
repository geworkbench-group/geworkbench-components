package org.geworkbench.components.cagrid.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.util.ProgressBar;
import org.geworkbench.util.Util;

import com.jgoodies.forms.builder.DefaultFormBuilder;

import edu.columbia.geworkbench.cagrid.discovery.client.DiscoveryServiceUtil;
import gov.nih.nci.cagrid.metadata.MetadataUtils;
import gov.nih.nci.cagrid.metadata.ServiceMetadata;

/**
 * An action listener for the grid services button.
 * 
 * @author keshav
 * @version $Id: GridServicesButtonListener.java,v 1.1 2007/04/03 02:39:14
 *          keshav Exp $
 */
public class GridServicesButtonListener implements ActionListener {
	private Log log = LogFactory.getLog(this.getClass());

	IndexServiceSelectionButtonListener indexServiceSelectionButtonListener = null;

	IndexServiceLabelListener indexServiceLabelListener = null;

	public DispatcherLabelListener dispatcherLabelListener = null;
	
	DefaultFormBuilder urlServiceBuilder = null;

	String selectedAnalysisType = null;

	ButtonGroup servicesButtonGroup = null;
	
	String indexServerUrl = "";

	/**
	 * 
	 * @param indexServiceSelectionButtonListener
	 * @param indexServiceLabelListener
	 * @param urlServiceBuilder
	 */
	public GridServicesButtonListener(
			IndexServiceSelectionButtonListener indexServiceSelectionButtonListener,
			IndexServiceLabelListener indexServiceLabelListener,
			DispatcherLabelListener dispatcherLabelListener,
			DefaultFormBuilder urlServiceBuilder) {
		super();
		this.indexServiceSelectionButtonListener = indexServiceSelectionButtonListener;
		this.indexServiceLabelListener = indexServiceLabelListener;
		this.dispatcherLabelListener = dispatcherLabelListener;
		this.urlServiceBuilder = urlServiceBuilder;
		this.servicesButtonGroup = new ButtonGroup();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		Thread t = new Thread(new Runnable() {
			public void run() {
				ProgressBar pBar = Util.createProgressBar("Grid Services",
						"Retrieving Services");

				pBar.start();
				pBar.reset();

				indexServerUrl = indexServiceLabelListener.getHost();
				EndpointReferenceType[] services = null;
				try {
					services = DiscoveryServiceUtil.getServices(indexServerUrl,dispatcherLabelListener.getHost(),
							selectedAnalysisType);
				} catch (Exception e) {
					// JDialog jdialog = new ErrorDialog("");
					// jdialog.setVisible(true);
					// Util.centerWindow(jdialog);
					
					if (indexServerUrl == null){
						indexServerUrl = " ";
					}
					
					JOptionPane.showMessageDialog(null, "Cannot reach host: "
							+ indexServerUrl, "Error",
							JOptionPane.ERROR_MESSAGE);
					log.debug("Cannot reach host:  indexServerUrl=" + indexServerUrl 
								+ "\n dispatcherLabelListener.getHost()="+ dispatcherLabelListener.getHost()
								+ "\n Exception=" + e);
				}

				if (services == null) {
					// TODO clear panel if populated
				}

				else {
					for (EndpointReferenceType service : services) {

						ServiceMetadata commonMetadata;
						try {
							commonMetadata = MetadataUtils
									.getServiceMetadata(service);

							String url = DiscoveryServiceUtil.getUrl(service);
							String researchCenter = DiscoveryServiceUtil
									.getResearchCenterName(commonMetadata);
							String description = DiscoveryServiceUtil
									.getDescription(commonMetadata);

							JRadioButton button = new JRadioButton();
							button
									.addActionListener(indexServiceSelectionButtonListener);
							button.setActionCommand(url);
							servicesButtonGroup.add(button);

							/* check if we've already seen this service */
							if (!indexServiceSelectionButtonListener
									.getSeenServices().containsKey(url)) {
								indexServiceSelectionButtonListener
										.getSeenServices().put(url, service);

								urlServiceBuilder.append(button);
								urlServiceBuilder.append(new JLabel(url));
								urlServiceBuilder.append(new JLabel(
										researchCenter));
								urlServiceBuilder
										.append(new JLabel(description));
								urlServiceBuilder.nextLine();
							}

						} catch (Exception e1) {
							throw new RuntimeException(e1);
						}
					}
				}

				pBar.stop();

				urlServiceBuilder.getPanel().revalidate();
				indexServiceSelectionButtonListener.getServiceDetailsBuilder()
						.getPanel().revalidate();

			}
		});
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();

	}

	/**
	 * 
	 * @return {@link ButtonGroup}
	 */
	public ButtonGroup getServicesButtonGroup() {
		return this.servicesButtonGroup;
	}

	/**
	 * 
	 * @param selectedAnalysisType
	 */
	public void setSelectedAnalysisType(String selectedAnalysisType) {
		this.selectedAnalysisType = selectedAnalysisType;
	}
	
	public String getIndexServerUrl(){
		return indexServerUrl;
	}
	
}
