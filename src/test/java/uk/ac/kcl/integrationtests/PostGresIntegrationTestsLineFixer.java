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
package uk.ac.kcl.integrationtests;

import uk.ac.kcl.batch.JobConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobParametersNotFoundException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import uk.ac.kcl.batch.BatchConfigurer;

/**
 *
 * @author rich
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:postgres_test_config.properties")
//@ContextConfiguration(locations = {"classpath:testApplicationContext.xml"})
@ContextConfiguration(classes = {
    JobConfiguration.class,
    BatchConfigurer.class},
        loader = AnnotationConfigContextLoader.class)
public class PostGresIntegrationTestsLineFixer {

    final static Logger logger = Logger.getLogger(PostGresIntegrationTestsLineFixer.class);

    @Autowired
    @Qualifier("sourceDataSource")
    public DataSource sourceDataSource;

    @Autowired
    @Qualifier("targetDataSource")
    public DataSource jdbcTargetDocumentFinder;

    private  Server server1;
    private  Server server2;
    private JdbcTemplate sourceTemplate;
    private JdbcTemplate targetTemplate;
    private ResourceDatabasePopulator rdp = new ResourceDatabasePopulator();
    private Resource dropTablesResource;
    private Resource makeTablesResource;


    public void initHSQLDBs() throws IOException, ServerAcl.AclFormatException {
 
        HsqlProperties p1 = new HsqlProperties();
        p1.setProperty("server.database.0", "mem:hsqldb");
        p1.setProperty("server.dbname.0", "test");
        p1.setProperty("server.port", "9001");
        p1.setProperty("server.remote_open", "true");
        server1 = new Server();
        server1.setProperties(p1);
        server1.setLogWriter(null);
        server1.setErrWriter(null);
        server1.start();

        HsqlProperties p2 = new HsqlProperties();
        p2.setProperty("server.database.0", "mem:hsqldb");
        p2.setProperty("server.dbname.0", "test2");
        p2.setProperty("server.port", "9002");
        p2.setProperty("server.remote_open", "true");
        server2 = new Server();
        server2.setProperties(p2);
        server2.setLogWriter(null);
        server2.setErrWriter(null);
        server2.start();

        //yodieconfig
        //Properties prop = System.getProperties();
        //prop.setProperty("at.ofai.gate.modularpipelines.configFile", "/home/rich/gate-apps/yodie/yodie-pipeline/main-bio/main-bio.config.yaml");        
    }

    public void destroyHSQLDBs() {
        server1.stop();
        server2.stop();
    }

    @Before
    public void initTemplates() {
        sourceTemplate = new JdbcTemplate(sourceDataSource);
        targetTemplate = new JdbcTemplate(jdbcTargetDocumentFinder);
    }

    @After
    public void dropDb() {
        //sourceTemplate.execute("DROP TABLE tblInputDocs");
        //targetTemplate.execute("DROP TABLE tblOutputDocs");
    }


    @Autowired
    JobOperator jobOperator;

    @Ignore
    @Test
    public void postgresGatePipelineTest() {
        initPostgresGateTable();
        initPostGresJobRepository();
        insertTestXHTMLForGate(sourceDataSource, false);

        try {
            jobOperator.startNextInstance("gateJob");
        } catch (NoSuchJobException | JobParametersNotFoundException | JobRestartException | JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException | UnexpectedJobExecutionException | JobParametersInvalidException ex) {
            java.util.logging.Logger.getLogger(PostGresIntegrationTestsLineFixer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Ignore
    @Test
    public void postgresDBLineFixerPipelineTest() {
        
        initPostGresJobRepository();
        initPostgresMultiLineTextTable();
        insertTestLinesForDBLineFixer(sourceDataSource);

        try {
            jobOperator.startNextInstance("dbLineFixerJob");
        } catch (NoSuchJobException | JobParametersNotFoundException | JobRestartException | JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException | UnexpectedJobExecutionException | JobParametersInvalidException ex) {
            java.util.logging.Logger.getLogger(PostGresIntegrationTestsLineFixer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
    @Ignore
    @Test
    public void sqlserverDBLineFixerPipelineTest() {
        
        initSqlServerJobRepository();
        initSqlServerMultiLineTextTable();
        insertTestLinesForDBLineFixer(sourceDataSource);

        try {
            jobOperator.startNextInstance("dbLineFixerJob");
        } catch (NoSuchJobException | JobParametersNotFoundException | JobRestartException | JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException | UnexpectedJobExecutionException | JobParametersInvalidException ex) {
            java.util.logging.Logger.getLogger(PostGresIntegrationTestsLineFixer.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
    
    @Test
    public void hsqlDBLineFixerPipelineTest() throws IOException, ServerAcl.AclFormatException{
        initHSQLDBs();
        initHSQLJobRepository();
        initHsqlMultiLineTextTable();
        insertTestLinesForDBLineFixer(sourceDataSource);

        try {
            jobOperator.startNextInstance("dbLineFixerJob");
        } catch (NoSuchJobException | JobParametersNotFoundException | JobRestartException | JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException | UnexpectedJobExecutionException | JobParametersInvalidException ex) {
            java.util.logging.Logger.getLogger(PostGresIntegrationTestsLineFixer.class.getName()).log(Level.SEVERE, null, ex);
        }
        destroyHSQLDBs();
    }            
    
    @Test
    public void hsqlDBGatePipelineTest() throws IOException, ServerAcl.AclFormatException{
        initHSQLDBs();
        initHSQLJobRepository();
        initHsqlMultiLineTextTable();
        insertTestLinesForDBLineFixer(sourceDataSource);

        try {
            jobOperator.startNextInstance("dbLineFixerJob");
        } catch (NoSuchJobException | JobParametersNotFoundException | JobRestartException | JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException | UnexpectedJobExecutionException | JobParametersInvalidException ex) {
            java.util.logging.Logger.getLogger(PostGresIntegrationTestsLineFixer.class.getName()).log(Level.SEVERE, null, ex);
        }
        destroyHSQLDBs();
    }       
    
    private void initMsSqlServerGateTable() {
        sourceTemplate.execute("IF OBJECT_ID('dbo.tblInputDocs', 'U') IS NOT NULL DROP TABLE dbo.tblInputDocs");        
        sourceTemplate.execute("CREATE TABLE tblInputDocs"
                + " (ID INT IDENTITY(1,1) PRIMARY KEY"
                + ", srcColumnFieldName VARCHAR(100) "
                + ", srcTableName VARCHAR(100) "
                + ", primaryKeyFieldName VARCHAR(100) "
                + ", primaryKeyFieldValue VARCHAR(100) "
                + ", binaryFieldName VARCHAR(100) "
                + ", updateTime VARCHAR(100) "
                + ", xhtml VARCHAR(max))");

        targetTemplate.execute("IF OBJECT_ID('dbo.tblOutputDocs', 'U') IS NOT NULL DROP TABLE dbo.tblOutputDocs");        
        targetTemplate.execute("CREATE TABLE tblOutputDocs "
                + " (ID INT IDENTITY(1,1) PRIMARY KEY"
                + ", srcColumnFieldName VARCHAR(100) "
                + ", srcTableName VARCHAR(100) "
                + ", primaryKeyFieldName VARCHAR(100) "
                + ", primaryKeyFieldValue VARCHAR(100) "
                + ", binaryFieldName VARCHAR(100) "
                + ", updateTime VARCHAR(100) "
                + ", gateJSON VARCHAR(max) )");
    }
    
    private void initSqlServerJobRepository(){
        dropTablesResource = new ClassPathResource("org/springframework/batch/core/schema-drop-sqlserver.sql");
        makeTablesResource = new ClassPathResource("org/springframework/batch/core/schema-sqlserver.sql");
        rdp.addScript(dropTablesResource);
        rdp.addScript(makeTablesResource);
        rdp.execute(jdbcTargetDocumentFinder);     
    }    

    private void initHSQLGateTable() {
////        //forhsql
        sourceTemplate.execute("DROP TABLE IF EXISTS tblInputDocs");
        sourceTemplate.execute("CREATE TABLE tblInputDocs"
                + " (ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY"
                + ", srcColumnFieldName VARCHAR(100) "
                + ", srcTableName VARCHAR(100) "
                + ", primaryKeyFieldName VARCHAR(100) "
                + ", primaryKeyFieldValue VARCHAR(100) "
                + ", binaryFieldName VARCHAR(100) "
                + ", updateTime VARCHAR(100) "
                + ", xhtml VARCHAR(1500000))");

        //forHsql
        targetTemplate.execute("DROP TABLE IF EXISTS tblOutputDocs");
        targetTemplate.execute("CREATE TABLE tblOutputDocs "
                + " (ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY"
                + ", srcColumnFieldName VARCHAR(100) "
                + ", srcTableName VARCHAR(100) "
                + ", primaryKeyFieldName VARCHAR(100) "
                + ", primaryKeyFieldValue VARCHAR(100) "
                + ", binaryFieldName VARCHAR(100) "
                + ", updateTime VARCHAR(100) "
                + ", gateJSON VARCHAR(1500000) )");
    }
    
    private void initHsqlMultiLineTextTable() {
        sourceTemplate.execute("DROP TABLE tblInputDocs IF EXISTS");
        sourceTemplate.execute("CREATE TABLE tblInputDocs"
                + "( ID  BIGINT IDENTITY PRIMARY KEY"
                + ", DOC_ID INTEGER "
                + ", LINE_ID INTEGER "
                + ", LINE_TEXT LONGVARCHAR )"
                );

        targetTemplate.execute("DROP TABLE   tblOutputDocs IF EXISTS");
        targetTemplate.execute("CREATE TABLE tblOutputDocs "
                + "( ID  BIGINT IDENTITY PRIMARY KEY"
                + ", DOC_ID INTEGER"
                + ", LINE_TEXT_CONCAT LONGVARCHAR )");

    }    

    private void initHSQLJobRepository(){
        dropTablesResource = new ClassPathResource("org/springframework/batch/core/schema-drop-hsqldb.sql");
        makeTablesResource = new ClassPathResource("org/springframework/batch/core/schema-hsqldb.sql");
        rdp.addScript(dropTablesResource);
        rdp.addScript(makeTablesResource);
        rdp.execute(jdbcTargetDocumentFinder);        
    }
    
    private void initPostgresGateTable() {
////        for postgres
        sourceTemplate.execute("DROP TABLE IF EXISTS tblInputDocs");
        sourceTemplate.execute("CREATE TABLE tblInputDocs"
                + "( ID  SERIAL PRIMARY KEY"
                + ", srcColumnFieldName text "
                + ", srcTableName text "
                + ", primaryKeyFieldName text "
                + ", primaryKeyFieldValue text "
                + ", binaryFieldName text "
                + ", updateTime text "
                + ", xhtml text )");

        targetTemplate.execute("DROP TABLE IF EXISTS tblOutputDocs");
        targetTemplate.execute("CREATE TABLE tblOutputDocs "
                + "( ID  SERIAL PRIMARY KEY"
                + ", srcColumnFieldName text "
                + ", srcTableName text "
                + ", primaryKeyFieldName text "
                + ", primaryKeyFieldValue text "
                + ", binaryFieldName text "
                + ", updateTime text "
                + ", gatejson text )");


    }
    
    private void initPostgresMultiLineTextTable(){
        sourceTemplate.execute("DROP TABLE IF EXISTS tblInputDocs");
        sourceTemplate.execute("CREATE TABLE tblInputDocs"
                + "( ID  SERIAL PRIMARY KEY"
                + ", DOC_ID integer "
                + ", LINE_ID integer "
                + ", LINE_TEXT text )"
                );

        targetTemplate.execute("DROP TABLE IF EXISTS  tblOutputDocs");
        targetTemplate.execute("CREATE TABLE tblOutputDocs "
                + "( ID  SERIAL PRIMARY KEY"
                + ", DOC_ID integer"
                + ", LINE_TEXT_CONCAT text )");
    }
    
    private void initSqlServerMultiLineTextTable(){
        sourceTemplate.execute("IF OBJECT_ID('dbo.tblInputDocs', 'U') IS NOT NULL DROP TABLE dbo.tblInputDocs");        
        sourceTemplate.execute("CREATE TABLE dbo.tblInputDocs"
                + "( ID  bigint IDENTITY(1,1) PRIMARY KEY"
                + ", DOC_ID bigint "
                + ", LINE_ID bigint "
                + ", LINE_TEXT VARCHAR(MAX) )"
                );

        targetTemplate.execute("IF OBJECT_ID('dbo.tblOutputDocs', 'U') IS NOT NULL DROP TABLE dbo.tblOutputDocs");        
        targetTemplate.execute("CREATE TABLE dbo.tblOutputDocs "
                + "( ID bigint IDENTITY(1,1) PRIMARY KEY"
                + ", DOC_ID bigint"
                + ", LINE_TEXT_CONCAT VARCHAR(MAX) )");
    }    
    
    private void initPostGresJobRepository(){
        dropTablesResource = new ClassPathResource("org/springframework/batch/core/schema-drop-postgresql.sql");
        makeTablesResource = new ClassPathResource("org/springframework/batch/core/schema-postgresql.sql");
        rdp.addScript(dropTablesResource);
        rdp.addScript(makeTablesResource);
        rdp.execute(jdbcTargetDocumentFinder);        
    }
      

    
    private void insertTestLinesForDBLineFixer(DataSource ds){
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
        int docCount = 10;
        int lineCountIncrementer = 1;
        String sql = "INSERT INTO dbo.tblInputDocs "
                + "( DOC_ID"
                + ", LINE_ID"
                + ", LINE_TEXT"
                + ") VALUES (?,?,?)";        
        for (int i = 0; i <= docCount; i++) {
            for(int j = 0;j < lineCountIncrementer; j++){
                String text = "This is DOC_ID:" + i + " and LINE_ID:" + j ;
                jdbcTemplate.update(sql, i,j,text);
            }
            lineCountIncrementer++;
            if(lineCountIncrementer % 50 == 0){
                lineCountIncrementer = 0;
            }
        }                
    }
    private void insertTestXHTMLForGate(DataSource ds, boolean includeGateBreaker) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
        int docCount = 10;
        byte[] bytes = null;
        try {
            bytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("xhtml_test"));
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(PostGresIntegrationTestsLineFixer.class.getName()).log(Level.SEVERE, null, ex);
        }
        String xhtmlString = new String(bytes, StandardCharsets.UTF_8);

        String sql = "INSERT INTO tblInputDocs "
                + "( srcColumnFieldName"
                + ", srcTableName"
                + ", primaryKeyFieldName"
                + ", primaryKeyFieldValue"
                + ", updateTime"
                + ", xhtml"
                + ") VALUES (?,?,?,?,?,?)";
        for (int ii = 0; ii < docCount; ii++) {
            jdbcTemplate.update(sql, "fictionalColumnFieldName","fictionalTableName","fictionalPrimaryKeyFieldName", ii,null,  xhtmlString);
            
        }
        //see what happens with a really long document...
        if (includeGateBreaker) {
            try {
                bytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("gate_breaker.txt"));
                xhtmlString = new String(bytes, StandardCharsets.UTF_8);
                jdbcTemplate.update(sql, "fictionalColumnFieldName", "fictionalTableName", "fictionalPrimaryKeyFieldName", docCount, null, xhtmlString);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(PostGresIntegrationTestsLineFixer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}