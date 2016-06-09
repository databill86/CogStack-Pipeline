/* 
 * Copyright 2016 King's College London, Richard Jackson <richgjackson@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.kcl.it;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import uk.ac.kcl.scheduling.SingleJobLauncher;

@RunWith(SpringJUnit4ClassRunner.class)
@ComponentScan("uk.ac.kcl.it")
@TestPropertySource({
        "classpath:basicPKprofiles.properties",
        "classpath:jms.properties",
        "classpath:deidentification.properties",
        "classpath:postgres_db.properties",
        "classpath:elasticsearch.properties",
        "classpath:jobAndStep_PK_partition_without_scheduling.properties",
        "classpath:postgres_test_config_basic.properties",
        "classpath:gate.properties"})
@ContextConfiguration(classes = {
        PostGresTestUtils.class,
        SingleJobLauncher.class,
        TestUtils.class},
        loader = AnnotationConfigContextLoader.class)
public class PostGresIntegrationTestsBasicPKPartitionWithoutScheduling {

    final static Logger logger = Logger.getLogger(PostGresIntegrationTestsBasicPKPartitionWithoutScheduling.class);

    @Autowired
    SingleJobLauncher jobLauncher;

    @Autowired
    PostGresTestUtils postGresTestUtils;

    @Autowired
    TestUtils testUtils;

    @Before
    public void init(){
        postGresTestUtils.initJobRepository();
        postGresTestUtils.createBasicInputTable();
        postGresTestUtils.createBasicOutputTable();
        testUtils.insertDataIntoBasicTable("tblInputDocs");
    }
    @Test
    @DirtiesContext
    public void postgresBasicPipelineTest() {
        jobLauncher.launchJob();
    }

}
