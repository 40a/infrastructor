package io.infrastructor.cli.handlers

import io.infrastructor.core.ApplicationProperties

import static io.infrastructor.core.logging.ConsoleLogger.blue

public class VersionHandler {
    
    def options() {
        [:]
    }
    
    def usage() { 
        ["infrastructor version"] 
    }
    
    def description() { 
        "Print version information."
    }

    def execute() { 
        println("version:    ${blue(ApplicationProperties.applicationVersion())}\n" +
                "revision:   ${blue(ApplicationProperties.buildRevision())}\n" + 
                "build date: ${blue(ApplicationProperties.buildDate())}")
    }
}

