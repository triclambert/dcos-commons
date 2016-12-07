package com.mesosphere.sdk.hdfs.scheduler;

import org.apache.curator.test.TestingServer;
import org.apache.mesos.SchedulerDriver;
import org.apache.mesos.config.ConfigStore;
import org.apache.mesos.config.ConfigurationUpdater;
import org.apache.mesos.offer.OfferRequirementProvider;
import org.apache.mesos.scheduler.DefaultScheduler;
import org.apache.mesos.specification.DefaultServiceSpec;
import org.apache.mesos.specification.ServiceSpec;
import org.apache.mesos.specification.yaml.YAMLServiceSpecFactory;
import org.apache.mesos.state.StateStore;
import org.apache.mesos.state.StateStoreCache;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.Collections;

import static org.apache.mesos.specification.yaml.YAMLServiceSpecFactory.generateRawSpecFromYAML;

public class HdfsServiceSpecTest {
    @ClassRule
    public static final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Mock
    private SchedulerDriver mockSchedulerDriver;

    @BeforeClass
    public static void beforeAll() {
        environmentVariables.set("EXECUTOR_URI", "");
        environmentVariables.set("LIBMESOS_URI", "");
        environmentVariables.set("PORT0", "8080");
        environmentVariables.set("SERVICE_NAME", "hdfs");
        environmentVariables.set("SERVICE_PRINCIPAL", "principal");
        environmentVariables.set("JOURNAL_CPUS", "1.0");
        environmentVariables.set("JOURNAL_MEM", "1024");
        environmentVariables.set("JOURNAL_DISK", "1024");
        environmentVariables.set("JOURNAL_DISK_TYPE", "MOUNT");
        environmentVariables.set("NAME_CPUS", "1.0");
        environmentVariables.set("NAME_MEM", "1024");
        environmentVariables.set("NAME_DISK", "1024");
        environmentVariables.set("NAME_DISK_TYPE", "MOUNT");
        environmentVariables.set("DATA_COUNT", "3");
    }

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOneTimePlanDeserialization() throws Exception {
        testDeserialization("hdfs_svc.yml");
    }

    @Test
    public void testOneTimePlanValidation() throws Exception {
        testValidation("hdfs_svc.yml");
    }

    private void testDeserialization(String yamlFileName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(yamlFileName).getFile());

        DefaultServiceSpec serviceSpec = YAMLServiceSpecFactory
                .generateServiceSpec(generateRawSpecFromYAML(file));
        Assert.assertNotNull(serviceSpec);
        Assert.assertEquals(8080, serviceSpec.getApiPort());
        DefaultServiceSpec.getFactory(serviceSpec, Collections.emptyList());
    }

    private void testValidation(String yamlFileName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(yamlFileName).getFile());
        DefaultServiceSpec serviceSpec = YAMLServiceSpecFactory
                .generateServiceSpec(generateRawSpecFromYAML(file));

        TestingServer testingServer = new TestingServer();
        StateStoreCache.resetInstanceForTests();
        StateStore stateStore = DefaultScheduler.createStateStore(
                serviceSpec,
                testingServer.getConnectString());
        ConfigStore<ServiceSpec> configStore = DefaultScheduler.createConfigStore(
                serviceSpec,
                testingServer.getConnectString(),
                Collections.emptyList());

        ConfigurationUpdater.UpdateResult configUpdateResult = DefaultScheduler
                .updateConfig(serviceSpec, stateStore, configStore);

        OfferRequirementProvider offerRequirementProvider = DefaultScheduler
                .createOfferRequirementProvider(stateStore, configUpdateResult.targetId);

        DefaultScheduler.create(serviceSpec, stateStore, configStore, offerRequirementProvider);
    }
}
