/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class monitoring a {@link File} for changes and notifying all registered
 * {@link IFileChangeListener}.
 * 
 * @author Pascal Essiembre
 * @since 1.3.0
 * @deprecated Since 1.4.0, use 
 *     {@link com.norconex.commons.lang.file.FileMonitor}
 */
@Deprecated
public final class FileMonitor {

    private static final FileMonitor INSTANCE = new FileMonitor();

    private Timer timer;

    private Map<String, FileMonitorTask> timerEntries;

    /**
     * Gets the file monitor instance.
     * 
     * @return file monitor instance
     */
    public static FileMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * Constructor.
     */
    private FileMonitor() {
        timer = new Timer(true);
        timerEntries = new Hashtable<String, FileMonitorTask>();
    }

    /**
     * Adds a monitored file with a {@link IFileChangeListener}.
     * 
     * @param listener listener to notify when the file changed.
     * @param fileName name of the file to monitor.
     * @param period   polling period in milliseconds.
     * @throws FileNotFoundException error with the file
     */
    public void addFileChangeListener(IFileChangeListener listener,
            String fileName, long period) throws FileNotFoundException {
        addFileChangeListener(listener, new File(fileName), period);
    }

    /**
     * Adds a monitored file with a FileChangeListener.
     * 
     * @param listener listener to notify when the file changed.
     * @param file     the file to monitor.
     * @param period   polling period in milliseconds.
     * @throws FileNotFoundException error with the file
     */
    public void addFileChangeListener(IFileChangeListener listener, File file,
            long period) throws FileNotFoundException {
        removeFileChangeListener(listener, file);
        FileMonitorTask task = new FileMonitorTask(listener, file);
        timerEntries.put(file.toString() + listener.hashCode(), task);
        timer.schedule(task, period, period);
    }

    /**
     * Remove the listener from the notification list.
     * 
     * @param listener the listener to be removed.
     * @param fileName name of the file for which to remove the listener
     */
    public void removeFileChangeListener(IFileChangeListener listener,
            String fileName) {
        removeFileChangeListener(listener, new File(fileName));
    }

    /**
     * Remove the listener from the notification list.
     * 
     * @param listener the listener to be removed.
     * @param file     the file for which to remove the listener
     */
    public void removeFileChangeListener(
            IFileChangeListener listener, File file) {
        FileMonitorTask task = timerEntries.remove(file.toString()
                + listener.hashCode());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Fires notification that a file changed.
     * 
     * @param listener
     *            file change listener
     * @param file
     *            the file that changed
     */
    protected void fireFileChangeEvent(
            IFileChangeListener listener, File file) {
        listener.fileChanged(file);
    }

    /**
     * File monitoring task.
     */
    class FileMonitorTask extends TimerTask {
        private IFileChangeListener listener;
        private File monitoredFile;
        private long lastModified;

        public FileMonitorTask(IFileChangeListener listener, File file)
                throws FileNotFoundException {
            this.listener = listener;
            this.lastModified = 0;
            monitoredFile = file;
            // but is it on CLASSPATH?
            if (!monitoredFile.exists()) {
                URL fileURL = listener.getClass().getClassLoader()
                        .getResource(file.toString());
                if (fileURL != null) {
                    monitoredFile = new File(fileURL.getFile());
                } else {
                    throw new FileNotFoundException("File Not Found: " + file);
                }
            }
            this.lastModified = monitoredFile.lastModified();
        }

        public void run() {
            long fileLastModified = monitoredFile.lastModified();
            if (fileLastModified != this.lastModified) {
                this.lastModified = fileLastModified;
                fireFileChangeEvent(this.listener, monitoredFile);
            }
        }
    }
}