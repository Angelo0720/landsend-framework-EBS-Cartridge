def containerFolder = "${PROJECT_NAME}/Environment_Cloning"

buildPipelineView(containerFolder + '/Provision_Cloned_Environments_Pipeline') {
    title('Provision Cloned Environments')
    displayedBuilds(5)
    selectedJob('Set_Provisioning_Parameters')
    showPipelineParameters()
    refreshFrequency(60)
	consoleOutputLinkStyle(OutputStyle.NewWindow)
	showPipelineParametersInHeaders()
}