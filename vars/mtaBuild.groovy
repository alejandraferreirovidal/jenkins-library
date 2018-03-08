import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import com.sap.piper.tools.Tool
import com.sap.piper.tools.ToolVerifier
import com.sap.piper.tools.ToolUtils


def call(Map parameters = [:]) {

    def stepName = 'mtaBuild'

    Set parameterKeys = [
        'buildTarget',
        'mtaJarLocation'
    ]

    Set stepConfigurationKeys = [
        'buildTarget',
        'mtaJarLocation'
    ]

    handlePipelineStepErrors (stepName: stepName, stepParameters: parameters) {

        final script = parameters?.script ?: [commonPipelineEnvironment: commonPipelineEnvironment]

        prepareDefaultValues script: script

        final Map configuration = ConfigurationMerger.merge(
                                      script, stepName,
                                      parameters, parameterKeys,
                                      null,
                                      stepConfigurationKeys)

        def mta = new Tool('SAP Multitarget Application Archive Builder', 'MTA_JAR_LOCATION', 'mtaJarLocation', '/', 'mta.jar', '1.0.6', '-v')

        ToolVerifier.verifyToolVersion(mta, this, configuration, env)

        def java = new Tool('Java', 'JAVA_HOME', '', '/bin/', 'java', '1.8.0', '-version 2>&1')

        ToolVerifier.verifyToolVersion(java, this, configuration, env)

        def mtaYaml = readYaml file: "${pwd()}/mta.yaml"

        //[Q]: Why not yaml.dump()? [A]: This reformats the whole file.
        sh "sed -ie \"s/\\\${timestamp}/`date +%Y%m%d%H%M%S`/g\" \"${pwd()}/mta.yaml\""

        def id = mtaYaml.ID
        if (!id) {
            error "Property 'ID' not found in mta.yaml file at: '${pwd()}'"
        }

        def mtarFileName = "${id}.mtar"
        def mtaJar = ToolUtils.getToolExecutable(mta, this, configuration, env)
        def buildTarget = configuration.buildTarget

        sh  """#!/bin/bash
            export PATH=./node_modules/.bin:${PATH}
            java -jar ${mtaJar} --mtar ${mtarFileName} --build-target=${buildTarget} build
            """

        def mtarFilePath = "${pwd()}/${mtarFileName}"
        script?.commonPipelineEnvironment?.setMtarFilePath(mtarFilePath)

        return mtarFilePath
    }
}
