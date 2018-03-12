package com.sap.piper

import hudson.AbortException


class EnvironmentUtils implements Serializable {

    def static isEnvironmentVariable(script, variable) {
        def exists
        try {
          exists = script.sh returnStdout: true, script: "echo $variable"
        } catch(AbortException e) {
          throw new AbortException("The verification of the environment variable '$variable' failed. Reason: $e.message.")
        }
        if (exists) return true
        else return false
    }

    def static getEnvironmentVariable(script, variable) {
        try {
          def value = script.sh returnStdout: true, script: "echo $variable"
          return value
        } catch(AbortException e) {
          throw new AbortException("There was an error requesting the environment variable '$variable'. Reason: $e.message.")
        }
    }
}
