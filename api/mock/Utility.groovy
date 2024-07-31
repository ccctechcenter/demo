package api.mock

import org.springframework.boot.actuate.health.Status
import org.springframework.core.env.Environment

Environment environment

def getSisVersion(){
    def versions = [:]
    versions["data"] = "1.0.0"

    return ["Mock Versions":versions]
}

def getSisConnectionStatus(){
    return Status.UP
}