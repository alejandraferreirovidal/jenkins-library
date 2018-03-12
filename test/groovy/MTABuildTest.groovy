import hudson.AbortException
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.parser.ParserException

import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Ignore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder

import com.lesfurets.jenkins.unit.BasePipelineTest

import util.JenkinsLoggingRule
import util.JenkinsShellCallRule
import util.JenkinsStepRule
import util.JenkinsEnvironmentRule
import util.Rules

public class MtaBuildTest extends BasePipelineTest {

    @ClassRule
    public static TemporaryFolder tmp = new TemporaryFolder()

    private ExpectedException thrown = new ExpectedException()
    private JenkinsLoggingRule jlr = new JenkinsLoggingRule(this)
    private JenkinsShellCallRule jscr = new JenkinsShellCallRule(this)
    private JenkinsStepRule jsr = new JenkinsStepRule(this)
    private JenkinsEnvironmentRule jer = new JenkinsEnvironmentRule(this)

    @Rule
    public RuleChain ruleChain = Rules
        .getCommonRules(this)
        .around(thrown)
        .around(jlr)
        .around(jscr)
        .around(jsr)
        .around(jer)

    private static currentDir
    private static newDir
    private static mtaYaml


    @BeforeClass
    static void createTestFiles() {

        currentDir = "${tmp.getRoot()}"
        tmp.newFolder('bin')
        tmp.newFolder('bin', 'java')
        mtaYaml = tmp.newFile('mta.yaml')
        newDir = "$currentDir/newDir"
        tmp.newFolder('newDir')
        tmp.newFile('newDir/mta.yaml') << defaultMtaYaml()
    }

    @Before
    void init() {
        mtaYaml.text = defaultMtaYaml()

        helper.registerAllowedMethod('pwd', [], { currentDir } )
        helper.registerAllowedMethod('sh', [Map], { Map m -> getVersionWithoutEnvVars(m) })

        binding.setVariable('PATH', '/usr/bin')
    }


    @Test
    void environmentPathTest() {

        jsr.step.call(buildTarget: 'NEO')

        assert jscr.shell.find { c -> c.contains('PATH=./node_modules/.bin:/usr/bin')}
    }


    @Test
    void sedTest() {

        jsr.step.call(buildTarget: 'NEO')

        assert jscr.shell.find { c -> c =~ /sed -ie "s\/\\\$\{timestamp\}\/`date \+%Y%m%d%H%M%S`\/g" ".*\/mta.yaml"$/}
    }


    @Test
    void mtarFilePathFromCommonPipelineEnviromentTest() {

        jsr.step.call(script: [commonPipelineEnvironment: jer.env],
                      buildTarget: 'NEO')

        def mtarFilePath = jer.env.getMtarFilePath()

        assert mtarFilePath == "$currentDir/com.mycompany.northwind.mtar"
    }


    @Test
    void mtaBuildWithSurroundingDirTest() {

        helper.registerAllowedMethod('pwd', [], { newDir } )

        def mtarFilePath = jsr.step.call(buildTarget: 'NEO')

        assert jscr.shell.find { c -> c =~ /sed -ie "s\/\\\$\{timestamp\}\/`date \+%Y%m%d%H%M%S`\/g" ".*\/newDir\/mta.yaml"$/}

        assert mtarFilePath == "$newDir/com.mycompany.northwind.mtar"
    }


    @Test
    void mtaJarLocationNotSetTest() {

        jsr.step.call(buildTarget: 'NEO')

        assert jscr.shell.find { c -> c.contains(' -jar mta.jar --mtar ')}

        assert jlr.log.contains("SAP Multitarget Application Archive Builder expected on PATH or current working directory.")
        assert jlr.log.contains("Using SAP Multitarget Application Archive Builder executable 'mta.jar'.")
    }


    @Test
    void mtaJarLocationAsParameterTest() {

        jsr.step.call(mtaJarLocation: "/param/mta", buildTarget: 'NEO')

        assert jscr.shell.find { c -> c.contains("-jar /param/mta/mta.jar --mtar")}

        assert jlr.log.contains("SAP Multitarget Application Archive Builder home '/param/mta' retrieved from configuration.")
        assert jlr.log.contains("Using SAP Multitarget Application Archive Builder executable '/param/mta/mta.jar'.")
    }


    @Test
    void noMtaPresentTest() {

        mtaYaml.delete()
        thrown.expect(FileNotFoundException)

        jsr.step.call(buildTarget: 'NEO')
    }


    @Test
    void badMtaTest() {

        thrown.expect(ParserException)
        thrown.expectMessage('while parsing a block mapping')

        mtaYaml.text = badMtaYaml()

        jsr.step.call(buildTarget: 'NEO')
    }


    @Test
    void noIdInMtaTest() {

        thrown.expect(AbortException)
        thrown.expectMessage("Property 'ID' not found in mta.yaml file at: '")

        mtaYaml.text = noIdMtaYaml()

        jsr.step.call(buildTarget: 'NEO')
    }


    @Test
    void mtaJarLocationFromEnvironmentTest() {

        helper.registerAllowedMethod('sh', [Map], { Map m -> getVersionWithEnvVars(m) })

        jsr.step.call(buildTarget: 'NEO')

        assert jscr.shell.find { c -> c.contains("-jar /env/mta/mta.jar --mtar")}
        assert jlr.log.contains("SAP Multitarget Application Archive Builder home '/env/mta' retrieved from environment.")
        assert jlr.log.contains("Using SAP Multitarget Application Archive Builder executable '/env/mta/mta.jar'.")
    }


    @Test
    void mtaJarLocationFromCustomStepConfigurationTest() {

        jer.env.configuration = [steps:[mtaBuild:[mtaJarLocation: "/config/mta"]]]

        jsr.step.call(script: [commonPipelineEnvironment: jer.env],
                      buildTarget: 'NEO')

        assert jscr.shell.find(){ c -> c.contains("-jar /config/mta/mta.jar --mtar")}
        assert jlr.log.contains("SAP Multitarget Application Archive Builder home '/config/mta' retrieved from configuration.")
        assert jlr.log.contains("Using SAP Multitarget Application Archive Builder executable '/config/mta/mta.jar'.")
    }


    @Test
    void buildTargetFromParametersTest() {

        jsr.step.call(buildTarget: 'NEO')

        assert jscr.shell.find { c -> c.contains('java -jar mta.jar --mtar com.mycompany.northwind.mtar --build-target=NEO build')}
    }


    @Test
    void buildTargetFromCustomStepConfigurationTest() {

        jer.env.configuration = [steps:[mtaBuild:[buildTarget: 'NEO']]]

        jsr.step.call(script: [commonPipelineEnvironment: jer.env])

        assert jscr.shell.find(){ c -> c.contains('java -jar mta.jar --mtar com.mycompany.northwind.mtar --build-target=NEO build')}
    }


    @Test
    void buildTargetFromDefaultStepConfigurationTest() {

        jer.env.defaultConfiguration = [steps:[mtaBuild:[buildTarget: 'NEO']]]

        jsr.step.call(script: [commonPipelineEnvironment: jer.env])

        assert jscr.shell.find { c -> c.contains('java -jar mta.jar --mtar com.mycompany.northwind.mtar --build-target=NEO build')}
    }


    private static defaultMtaYaml() {
        return  '''
                _schema-version: "2.0.0"
                ID: "com.mycompany.northwind"
                version: 1.0.0

                parameters:
                  hcp-deployer-version: "1.0.0"

                modules:
                  - name: "fiorinorthwind"
                    type: html5
                    path: .
                    parameters:
                       version: 1.0.0-${timestamp}
                    build-parameters:
                      builder: grunt
                build-result: dist
                '''
    }

    private badMtaYaml() {
        return  '''
                _schema-version: "2.0.0
                ID: "com.mycompany.northwind"
                version: 1.0.0

                parameters:
                  hcp-deployer-version: "1.0.0"

                modules:
                  - name: "fiorinorthwind"
                    type: html5
                    path: .
                    parameters:
                       version: 1.0.0-${timestamp}
                    build-parameters:
                      builder: grunt
                build-result: dist
                '''
    }

    private noIdMtaYaml() {
        return  '''
                _schema-version: "2.0.0"
                version: 1.0.0

                parameters:
                  hcp-deployer-version: "1.0.0"

                modules:
                  - name: "fiorinorthwind"
                    type: html5
                    path: .
                    parameters:
                       version: 1.0.0-${timestamp}
                    build-parameters:
                      builder: grunt
                build-result: dist
                '''
    }

    private getVersionWithEnvVars(Map m) {

        if(m.script.contains('java -version')) {
            return '''openjdk version \"1.8.0_121\"
                    OpenJDK Runtime Environment (build 1.8.0_121-8u121-b13-1~bpo8+1-b13)
                    OpenJDK 64-Bit Server VM (build 25.121-b13, mixed mode)'''
        } else if(m.script.contains('mta.jar -v')) {
            return '1.0.6'
        } else {
            return getEnvVars(m)
        }
    }

    private getVersionWithoutEnvVars(Map m) {

        if(m.script.contains('java -version')) {
            return '''openjdk version \"1.8.0_121\"
                    OpenJDK Runtime Environment (build 1.8.0_121-8u121-b13-1~bpo8+1-b13)
                    OpenJDK 64-Bit Server VM (build 25.121-b13, mixed mode)'''
        } else if(m.script.contains('mta.jar -v')) {
            return '1.0.6'
        } else {
            return getNoEnvVars(m)
        }
    }

    private getEnvVars(Map m) {

        if(m.script.contains('JAVA_HOME')) {
            return '/env/java'
        } else if(m.script.contains('MTA_JAR_LOCATION')) {
            return '/env/mta'
        } else {
            return 1
        }
    }

    private getNoEnvVars(Map m) {

        if(m.script.contains('JAVA_HOME')) {
            return ''
        } else if(m.script.contains('MTA_JAR_LOCATION')) {
            return ''
        } else {
            return 1
        }
    }
}
