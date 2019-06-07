package org.onap.ccsdk.cds.blueprintsprocessor;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.onap.ccsdk.cds.blueprintsprocessor.functions.netconf.executor.core.NetconfSessionImplTest;
import org.onap.ccsdk.cds.blueprintsprocessor.functions.python.executor.ComponentRemotePythonExecutorTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ComponentRemotePythonExecutorTest.class,
        NetconfSessionImplTest.class
})
public class BugInterruptedOnCompletableFuture {
}
