package com.sap.piper

import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import org.junit.rules.RuleChain

import util.Rules

import com.lesfurets.jenkins.unit.BasePipelineTest

import com.sap.piper.FileUtils

import hudson.AbortException


class FileUtilsTest extends BasePipelineTest {

    @ClassRule
    public static TemporaryFolder tmp = new TemporaryFolder()

    private ExpectedException thrown = new ExpectedException()

    @Rule
    public RuleChain rules = Rules.getCommonRules(this)
                .around(thrown)

    private static emptyDir
    private static notEmptyDir
    private static file

    private static script

    @BeforeClass
    static void createTestFiles() {

        emptyDir = tmp.newFolder('emptyDir').getAbsolutePath()
        notEmptyDir = tmp.newFolder('notEmptyDir').getAbsolutePath()
        file = tmp.newFile('notEmptyDir/file').getAbsolutePath()
    }

    @Before
    void setup() {

        script = loadScript('commonPipelineEnvironment.groovy').commonPipelineEnvironment
    }


    @Test
    void nullValidateDirectoryTest() {

        thrown.expect(IllegalArgumentException)
        thrown.expectMessage("The parameter 'dir' can not be null or empty.")

        FileUtils.validateDirectory(script, null)
    }

    @Test
    void emptyValidateDirectoryTest() {

        thrown.expect(IllegalArgumentException)
        thrown.expectMessage("The parameter 'dir' can not be null or empty.")

        FileUtils.validateDirectory(script, '')
    }

    @Test
    void doestNotExistValidateDirectoryTest() {

        def dir = new File("$emptyDir", 'test').getAbsolutePath()

        helper.registerAllowedMethod('sh', [Map], { Map m -> validateDirectory(dir) })

        thrown.expect(AbortException)
        thrown.expectMessage("'$dir' does not exist.")

        FileUtils.validateDirectory(script, dir)
    }

    @Test
    void isNotDirectoryValidateDirectoryTest() {

        helper.registerAllowedMethod('sh', [Map], { Map m -> validateDirectory(file) })

        thrown.expect(AbortException)
        thrown.expectMessage("'$file' is not a directory.")

        FileUtils.validateDirectory(script, file)
    }

    @Test
    void validateDirectoryTest() {

        helper.registerAllowedMethod('sh', [Map], { Map m -> validateDirectory(notEmptyDir) })

        FileUtils.validateDirectory(script, notEmptyDir)
    }

    @Test
    void emptyDirValidateDirectoryIsNotEmptyTest() {

        helper.registerAllowedMethod('sh', [Map], { Map m -> validateDirectoryIsNotEmpty(emptyDir) })

        thrown.expect(AbortException)
        thrown.expectMessage("'$emptyDir' is empty.")

        FileUtils.validateDirectoryIsNotEmpty(script, emptyDir)
    }

    @Test
    void validateDirectoryIsNotEmptyTest() {

        helper.registerAllowedMethod('sh', [Map], { Map m -> validateDirectoryIsNotEmpty(notEmptyDir) })

        FileUtils.validateDirectoryIsNotEmpty(script, notEmptyDir)
    }

    @Test
    void validateFileNoFilePathTest() {

        thrown.expect(IllegalArgumentException)
        thrown.expectMessage("The parameter 'filePath' can not be null or empty.")

        FileUtils.validateFile(script, null)
    }

    @Test
    void validateFileEmptyFilePathTest() {

        thrown.expect(IllegalArgumentException)
        thrown.expectMessage("The parameter 'filePath' can not be null or empty.")

        FileUtils.validateFile(script, '')
    }

    @Test
    void validateFileDoesNotExistFileTest() {

        def path = new File("$emptyDir", 'test').getAbsolutePath()

        helper.registerAllowedMethod('sh', [Map], { Map m -> validateFile(path) })

        thrown.expect(AbortException)
        thrown.expectMessage("'$path' does not exist.")

        FileUtils.validateFile(script, path)
    }

    @Test
    void validateFileTest() {

        helper.registerAllowedMethod('sh', [Map], { Map m -> validateFile(file) })

        FileUtils.validateFile(script, file)
    }


    private validateDirectory(dir) {
        if (!dir) throw new IllegalArgumentException("The parameter 'dir' can not be null or empty.")
        def file = new File(dir)
        if (!file.exists()) throw new AbortException("'${file.getAbsolutePath()}' does not exist.")
        if (!file.isDirectory()) throw new AbortException("'${file.getAbsolutePath()}' is not a directory.")
        return 1
    }

    private validateDirectoryIsNotEmpty(dir) {
        validateDirectory(dir)
        def file = new File(dir)
        if (file.list().size() == 0) throw new AbortException("'${file.getAbsolutePath()}' is empty.")
        return 1
    }

    private validateFile(filePath) {
        if (!filePath) throw new IllegalArgumentException("The parameter 'filePath' can not be null or empty.")
        def file = new File(filePath)
        if (!file.exists()) throw new AbortException("'${file.getAbsolutePath()}' does not exist.")
        return 1
    }
}
