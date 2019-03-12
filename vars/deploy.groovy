import com.sap.piper.ConfigurationHelper
import com.sap.piper.Utils

import groovy.transform.Field

import static com.sap.piper.Prerequisites.checkScript

@Field String STEP_NAME = getClass().getName()

@Field Set GENERAL_CONFIG_KEYS = [
    /** neo */
    'neo',
    /** cloudFoundry */
    'cloudFoundry',
    'deployUser',
    'deployTool',
    'deployType',
    'keepOldInstance',
    'dockerImage',
    'dockerWorkspace',
    'mtaDeployParameters',
    'mtaExtensionDescriptor',
    'mtaPath',
    'smokeTestScript',
    'smokeTestStatusCode',
    'stashContent'
]

@Field Set STEP_CONFIG_KEYS = GENERAL_CONFIG_KEYS.plus([
    /** Control the deploy technology, examples 'neo', 'cf'.*/
    'deployTo',
    /** neo */
    'dockerEnvVars',
    'dockerImage',
    'dockerOptions',
    'neoHome'
])

@Field Set PARAMETER_KEYS = STEP_CONFIG_KEYS.plus([
     /** neo */
    'source',
    'deployMode',
    'warAction'
])


void call(parameters = [:]) {
    handlePipelineStepErrors(stepName: STEP_NAME, stepParameters: parameters) {

        def script = checkScript(this, parameters) ?: this

        def utils = parameters.utils ?: new Utils()

        Map config = ConfigurationHelper.newInstance(this)
            .loadStepDefaults()
            .mixinGeneralConfig(script.commonPipelineEnvironment, GENERAL_CONFIG_KEYS)
            .mixinStepConfig(script.commonPipelineEnvironment, STEP_CONFIG_KEYS)
            .mixinStageConfig(script.commonPipelineEnvironment, parameters.stageName ?: env.STAGE_NAME, STEP_CONFIG_KEYS)
            .mixin(parameters, PARAMETER_KEYS)
            .use()

        utils.pushToSWA([
            step: STEP_NAME,
            stepParamKey1: 'deployTo',
            stepParam1: config.deployTo,
        ], config)

        switch (config.deployTo) {
            case 'cf':
                cloudFoundryDeploy parameters
                break
            default:
                neoDeploy parameters
        }
    }
}

