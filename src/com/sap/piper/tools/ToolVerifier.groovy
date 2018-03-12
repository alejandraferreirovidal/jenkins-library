package com.sap.piper.tools

import com.sap.piper.EnvironmentUtils
import com.sap.piper.FileUtils
import com.sap.piper.Version

import hudson.AbortException


class ToolVerifier implements Serializable {

    def static verifyToolHome(tool, script, configuration) {

        def home = ToolUtils.getToolHome(tool, script, configuration, false)
        if (home) { 
            script.echo "Verifying $tool.name home '$home'."
            FileUtils.validateDirectoryIsNotEmpty(script, home)
            script.echo "Verification success. $tool.name home '$home' exists."
        }
    }

    def static verifyToolExecutable(tool, script, configuration) {

        def home = ToolUtils.getToolHome(tool, script, configuration, false)
        def executable = ToolUtils.getToolExecutable(tool, script, configuration, false)
        if (home) {
            script.echo "Verifying $tool.name executable '$executable'."
            FileUtils.validateFile(script, executable)
            script.echo "Verification success. $tool.name executable '$executable' exists."
        }
    }

    def static verifyToolVersion(tool, script, configuration) {

        def executable = ToolUtils.getToolExecutable(tool, script, configuration, false)
        if (tool.name == 'SAP Multitarget Application Archive Builder'){
            def javaHome = EnvironmentUtils.getEnvironmentVariable(script, 'JAVA_HOME')
            executable = "$javaHome/bin/java -jar $executable"
        }

        script.echo "Verifying $tool.name version $tool.version or compatible version."

        def toolVersion
        try {
          toolVersion = script.sh returnStdout: true, script: "$executable $tool.versionOption"
        } catch(AbortException e) {
          throw new AbortException("The verification of $tool.name failed. Please check '$executable': $e.message.")
        }
        def version = new Version(toolVersion)
        if (!version.isCompatibleVersion(new Version(tool.version))) {
          throw new AbortException("The installed version of $tool.name is ${version.toString()}. Please install version $tool.version or a compatible version.")
        }
        script.echo "Verification success. $tool.name version ${version.toString()} is installed."
    }
}
