package nl.bos;

import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import lombok.extern.java.Log;
import nl.bos.controllers.JobEditorPane;

@Log
public class JobMonitor implements Runnable {
    private final JobEditorPane jobEditorPane;
    private final MyJobObject currentJob;
    private Repository repository = Repository.getInstance();
    private volatile boolean running;

    public JobMonitor(MyJobObject currentJob, JobEditorPane jobEditorPane) {
        this.running = true;
        this.currentJob = currentJob;
        this.jobEditorPane = jobEditorPane;
    }

    @Override
    public void run() {
        while (running) {
            log.info("Monitor...");

            try {
                IDfPersistentObject job = repository.getSession().getObject(new DfId(currentJob.getObjectId()));
                jobEditorPane.updateFields(job);
            } catch (DfException e) {
                log.finest(e.getMessage());
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.finest(e.getMessage());
            }
        }
    }

    public synchronized void stop() {
        running = false;
    }
}
