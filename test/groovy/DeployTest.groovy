import com.sap.piper.JenkinsUtils
import com.sap.piper.Utils

import hudson.AbortException

import org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException

import org.junit.Assert
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder

import util.BasePiperTest
import util.CommandLineMatcher
import util.JenkinsCredentialsRule
import util.JenkinsLockRule
import util.JenkinsLoggingRule
import util.JenkinsReadYamlRule
import util.JenkinsShellCallRule
import util.JenkinsShellCallRule.Type
import util.JenkinsStepRule
import util.JenkinsWithEnvRule
import util.JenkinsWriteFileRule
import util.JenkinsDockerExecuteRule
import util.JenkinsFileExistsRule
import util.Rules

import static org.junit.Assert.assertThat

import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.containsString


class DeployTest extends BasePiperTest {

    @ClassRule
    public static TemporaryFolder tmp = new TemporaryFolder()

    private ExpectedException thrown = new ExpectedException().none()
    private JenkinsLoggingRule loggingRule = new JenkinsLoggingRule(this)
    private JenkinsShellCallRule shellRule = new JenkinsShellCallRule(this)
    private JenkinsStepRule stepRule = new JenkinsStepRule(this)
    private JenkinsLockRule lockRule = new JenkinsLockRule(this)
    private JenkinsReadYamlRule readYamlRule = new JenkinsReadYamlRule(this)
    private JenkinsWriteFileRule writeFileRule = new JenkinsWriteFileRule(this)
    private JenkinsDockerExecuteRule dockerExecuteRule = new JenkinsDockerExecuteRule(this)
    private JenkinsFileExistsRule fileExistsRule = new JenkinsFileExistsRule(this, ['file.mtar', 'file.war', 'file.properties'])


    class JenkinsUtilsMock extends JenkinsUtils {
        def isJobStartedByUser() {
            return true
        }
    }

    @Rule
    public RuleChain ruleChain = Rules
        .getCommonRules(this)
        .around(readYamlRule)
        .around(thrown)
        .around(loggingRule)
        .around(shellRule)
        .around(new JenkinsCredentialsRule(this)
        .withCredentials('myCredentialsId', 'anonymous', '********')
        .withCredentials('CI_CREDENTIALS_ID', 'defaultUser', '********')
        .withCredentials('test_cfCredentialsId', 'test_cf', '********'))
        .around(stepRule)
        .around(lockRule)
        .around(new JenkinsWithEnvRule(this))
        .around(writeFileRule)
        .around(dockerExecuteRule)
        .around(fileExistsRule)


    @Before
    void init() {

        helper.registerAllowedMethod('pwd', [], { return "${tmp.getRoot()}" })
        mockShellCommands()

        nullScript.commonPipelineEnvironment.configuration = [
            general: [
                neo: [
                    host: 'test.deploy.host.com',
                    account: 'trialuser123'
                ],
                cloudFoundry: [
                    appName:'testAppName',
                    credentialsId: 'test_cfCredentialsId',
                    manifest: 'test.yml',
                    org: 'testOrg',
                    space: 'testSpace'
                ]
            ],
            steps: [
                neoDeploy: [
                    neoHome: '/config/neo'
                ],
                cloudFoundryDeploy: [
                    deployTool: 'cf_native',
                    deployType: 'blue-green',
                    keepOldInstance: true
                ]
            ]
        ]
    }


    @Test
    void neoDeployWithParametersTest() {

        stepRule.step.deploy(
            script: nullScript,
            source: 'file.mtar',
            neo:[credentialsId: 'myCredentialsId'],
            neoHome: '/param/neo'
        )
    }


    @Test
    void neoDeployWithCustomStepConfigurationTest() {

        stepRule.step.deploy(
            script: nullScript,
            source: 'file.mtar'
        )
    }


    @Test
    void cfDeployWithParametersTest() {

        readYamlRule.registerYaml('test.yml', "applications: [[]]")

        stepRule.step.deploy([
            script: nullScript,
            deployTo: 'cf',
            juStabUtils: utils,
            jenkinsUtilsStub: new JenkinsUtilsMock(),
            cloudFoundry: [
                appName: 'testAppName',
                credentialsId: 'test_cfCredentialsId',
                manifest: 'test.yml',
                org: 'testOrg',
                space: 'testSpace'
            ],
            deployTool: 'cf_native',
            deployType: 'blue-green',
            keepOldInstance: true
        ])

        assertThat(dockerExecuteRule.dockerParams, hasEntry('dockerImage', 's4sdk/docker-cf-cli'))
        assertThat(dockerExecuteRule.dockerParams, hasEntry('dockerWorkspace', '/home/piper'))

        assertThat(shellRule.shell, hasItem(containsString('cf login -u "test_cf" -p \'********\' -a https://api.cf.eu10.hana.ondemand.com -o "testOrg" -s "testSpace"')))
        assertThat(shellRule.shell, hasItem(containsString("cf blue-green-deploy testAppName -f 'test.yml'")))
        assertThat(shellRule.shell, hasItem(containsString("cf stop testAppName-old &>")))
        assertThat(shellRule.shell, hasItem(containsString("cf logout")))
    }


    @Test
    void cfDeployWithConfigurationTest() {

       readYamlRule.registerYaml('test.yml', "applications: [[]]")

        stepRule.step.deploy([
            script: nullScript,
            deployTo: 'cf',
            juStabUtils: utils,
            jenkinsUtilsStub: new JenkinsUtilsMock()
        ])

        assertThat(dockerExecuteRule.dockerParams, hasEntry('dockerImage', 's4sdk/docker-cf-cli'))
        assertThat(dockerExecuteRule.dockerParams, hasEntry('dockerWorkspace', '/home/piper'))

        assertThat(shellRule.shell, hasItem(containsString('cf login -u "test_cf" -p \'********\' -a https://api.cf.eu10.hana.ondemand.com -o "testOrg" -s "testSpace"')))
        assertThat(shellRule.shell, hasItem(containsString("cf blue-green-deploy testAppName -f 'test.yml'")))
        assertThat(shellRule.shell, hasItem(containsString("cf stop testAppName-old &>")))
        assertThat(shellRule.shell, hasItem(containsString("cf logout")))
    }


    private mockShellCommands() {
        String javaVersion = '''openjdk version \"1.8.0_121\"
                    OpenJDK Runtime Environment (build 1.8.0_121-8u121-b13-1~bpo8+1-b13)
                    OpenJDK 64-Bit Server VM (build 25.121-b13, mixed mode)'''
        shellRule.setReturnValue(Type.REGEX, '.*java -version.*', javaVersion)

        String neoVersion = '''SAP Cloud Platform Console Client
                    SDK version    : 3.39.10
                    Runtime        : neo-java-web'''
        shellRule.setReturnValue(Type.REGEX, '.*neo.sh version.*', neoVersion)

        shellRule.setReturnValue(Type.REGEX, '.*JAVA_HOME.*', '/opt/java')
        shellRule.setReturnValue(Type.REGEX, '.*NEO_HOME.*', '/opt/neo')
        shellRule.setReturnValue(Type.REGEX, '.*which java.*', 0)
        shellRule.setReturnValue(Type.REGEX, '.*which neo.*', 0)
    }
}
