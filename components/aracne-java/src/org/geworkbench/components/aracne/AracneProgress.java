package org.geworkbench.components.aracne;

import java.awt.HeadlessException;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.util.ProgressBar;

/**
 * @author mhall
 * @version $Id$
 */
public class AracneProgress implements Observer {
	private static Log log = LogFactory.getLog(AracneProgress.class);
	
    private Thread aracneThread;
    private ProgressBar progressBar = null;

    public AracneProgress(Thread thread) throws HeadlessException {  
        aracneThread = thread;
        progressBar = ProgressBar.create(ProgressBar.INDETERMINATE_TYPE);
		progressBar.addObserver(this);
		progressBar.setTitle("ARACNE");
		progressBar.setMessage("ARACNE Process Running");
		progressBar.start();
        
        aracneThread.start();
    }
    
    public void update(java.util.Observable ob, Object o) {
		log.debug("initiated close");
		aracneThread.stop();
		log.warn("Cancelling ARACNE Analysis.");
		
		stopProgress();
	}
    
    public void startProgress(){
    	progressBar.start();
    }
    
    public void stopProgress(){
    	progressBar.stop();
    }
}
