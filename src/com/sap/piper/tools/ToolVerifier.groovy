package com.sap.piper.tools

import com.sap.piper.FileUtils
import com.sap.piper.Version

import hudson.AbortException


class ToolVerifier implements Serializable {

    def static verifyToolHome(tool, script, configuration, environment) {

        def home = ToolUtils.getToolHome(tool, script, configuration, environment)
        if (home) { 
            script.echo "Verifying $tool.name home."
            FileUtils.validateDirectoryIsNotEmpty(home)
        }
        return home
    }

    def static verifyToolExecutable(tool, script, configuration, environment) {

        def home = verifyToolHome(tool, script, configuration, environment)
        def executable = ToolUtils.getToolExecutable(tool, script, home)
        if (home) {
            script.echo "Verifying $tool.name executable."
            FileUtils.validateFile(executable)
        }
        return executable
    }

    def static verifyToolVersion(tool, script, configuration, environment) {

        def executable = verifyToolExecutable(tool, script, configuration, environment)
        if (tool.name == 'SAP Multitarget Application Archive Builder') executable = "$environment.JAVA_HOME/bin/java -jar $executable"

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
        script.echo "$tool.name version ${version.toString()} is installed."
    }
}
