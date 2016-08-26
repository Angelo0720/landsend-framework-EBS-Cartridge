def containerFolder = "${PROJECT_NAME}/Environment_PreCloning"

buildPipelineView(containerFolder + '/PreClone_Environments_Pipeline') {
    title('PreClone Environments and backup Filesystem')
    displayedBuilds(5)
    selectedJob('Set_PreCloning_Source_Parameters')
    showPipelineParameters()
    refreshFrequency(60)
	consoleOutputLinkStyle(OutputStyle.NewWindow)
	showPipelineParametersInHeaders()
}