package org.erlide.engine.model.root

interface IProjectConfigurator {
    def String getName()
    def String getLocation()

    def ErlangProjectProperties getConfiguration()
    def void setConfiguration(ErlangProjectProperties data)
}
