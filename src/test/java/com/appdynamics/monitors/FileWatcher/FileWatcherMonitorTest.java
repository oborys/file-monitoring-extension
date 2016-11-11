package com.appdynamics.monitors.FileWatcher;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.appdynamics.extensions.filewatcher.FileWatcherMonitor;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

/**
 * Created by abhi.pandey on 9/4/14.
 */
public class FileWatcherMonitorTest {

    private static final String CONFIG_ARG = "config-file";

    private FileWatcherMonitor testClass;

    @Before
    public void init() throws Exception {
        testClass = new FileWatcherMonitor();
    }


    @Test(expected = TaskExecutionException.class)
    public void testWithNullArgsShouldResultInException() throws Exception {
        testClass.execute(null, null);
    }

    @Test(expected = TaskExecutionException.class)
    public void testWithEmptyArgsShouldResultInException() throws Exception {
        testClass.execute(new HashMap<String, String>(), null);
    }

    @Ignore
    @Test
    public void testFileWatcherExtension() throws TaskExecutionException {

        Map<String, String> taskArgs = new HashMap();
        taskArgs.put(CONFIG_ARG, "src/test/resources/conf/config.yml");
        try {
            testClass.execute(taskArgs, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
