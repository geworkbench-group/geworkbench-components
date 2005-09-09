package org.geworkbench.components.alignment.panels;

import java.beans.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import java.util.HashMap;
import org.geworkbench.engine.config.VisualPlugin;
import org.geworkbench.engine.config.events.EventSource;
import org.geworkbench.engine.config.MenuListener;
import org.geworkbench.events.SequenceDiscoveryTableEvent;
import org.geworkbench.components.alignment.synteny.DotMatrixViewWidgetPanel;
import org.biojava.bio.seq.db.SequenceDB;
import org.geworkbench.events.SequencePanelEvent;
import org.geworkbench.engine.parsers.FileFormat;
import org.geworkbench.engine.parsers.sequences.SequenceFileFormat;
import org.geworkbench.util.PropertiesMonitor;
import org.geworkbench.bison.datastructure.biocollections.DataSet;
import org.geworkbench.builtin.projects.ProjectSelection;

/**
 * <p>SequenceViewAppComponent controls all notification and communication for SequenceViewWidget</p>
 * <p>Loads FASTA file </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: Califano Lab </p>
 * @author
 * @version 1.0
 */
public class DotMatrixViewAppComponent
    extends EventSource
    implements VisualPlugin,
    MenuListener, PropertyChangeListener {

//  SequenceViewWidgetByXQ sViewWidget;

  DotMatrixViewWidgetPanel dmVW;
  EventListenerList listenerList = new EventListenerList();
//  JMenuItem jOpenFASTAItem = new JMenuItem();
//  JMenuItem jOpenFileItem = new JMenuItem();

  //This registers listeners for menu items.
  HashMap listeners = new HashMap();
  SequencePanelEvent spe = null;
  ActionListener listener = null;
  SequenceDB sequenceDB = null;

  public void DotMatrixAppComponent() {

//    sViewWidget = new SequenceViewWidgetByXQ();
//    sViewWidget.addPropertyChangeListener(this);

//    jOpenFASTAItem.setText("FASTA File");
    listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {

        jOpenFASTAItem_actionPerformed(e);
      }
    };

    listeners.put("File.Open.FASTA File", listener);
//    jOpenFASTAItem.addActionListener(listener);
    //sViewWidget.add(jOpenFASTAItem);
//    jOpenFileItem.setText("File");

    listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {

        jOpenFASTAItem_actionPerformed(e);
      }
    };
    listeners.put("File.Open.File", listener);
//    jOpenFASTAItem.addActionListener(listener);
  }

  public void sequenceDiscoveryTableRowSelected(SequenceDiscoveryTableEvent e) {
//    sViewWidget.patternSelectionHasChanged(e);
  }

  public Component getComponent() {
    return dmVW;
  }

  public ActionListener getActionListener(String var) {

    return (ActionListener) getListeners().get(var);

  } //implementation of core.config.MenuListener interface

  public void propertyChange(PropertyChangeEvent e) {
    String propertyName = e.getPropertyName();
  }

  void jOpenFASTAItem_actionPerformed(ActionEvent e) {
    String defPath = PropertiesMonitor.getPropertiesMonitor().getDefPath();
    JFileChooser fc = new JFileChooser(defPath);
    FileFormat format = new SequenceFileFormat();
    FileFilter filter = format.getFileFilter();
    fc.setFileFilter(filter);
    fc.setDialogTitle("Open FASTA file");
//    int choice = fc.showOpenDialog(sViewWidget.getParent());
//    if (choice == JFileChooser.APPROVE_OPTION) {
//      PropertiesMonitor.getPropertiesMonitor().setDefPath(
//          fc.getCurrentDirectory().getAbsolutePath());
//      sequenceDB = SequenceDB.getSequenceDB(fc.getSelectedFile());
//      if (sequenceDB != null) {
//        sViewWidget.setSequenceDB(sequenceDB);
//      }
//    }
  }

  public HashMap getListeners() {
    return listeners;
  }
}
