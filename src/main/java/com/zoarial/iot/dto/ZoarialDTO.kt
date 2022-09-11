package com.zoarial.iot.dto

import lombok.Data
import me.zoarial.networkArbiter.annotations.ZoarialNetworkObject
import me.zoarial.networkArbiter.annotations.ZoarialObjectElement
import java.util.*

enum class ZoarialDTO {
    ;

    enum class V1 {
        ;

        enum class Request {
            ;

            // Request to complete action
            @Data
            @ZoarialNetworkObject
            class Action {
                @ZoarialObjectElement(placement = 1)
                var uuid: UUID? = null

                @ZoarialObjectElement(placement = 2, optional = true)
                var otp: Short? = null

                @ZoarialObjectElement(placement = 3)
                var data: String? = null
            }

            // Request to update info about an action
            @Data
            @ZoarialNetworkObject
            class UpdateActionSecurityLevel {
                @ZoarialObjectElement(placement = 1)
                var uuid: UUID? = null

                @ZoarialObjectElement(placement = 2)
                var property: String? = null

                @ZoarialObjectElement(placement = 3)
                var level: Byte = 0
            }

            @Data
            @ZoarialNetworkObject
            class UpdateActionEncrypt {
                @ZoarialObjectElement(placement = 1)
                var uuid: UUID? = null

                @ZoarialObjectElement(placement = 2)
                var property: String? = null

                @ZoarialObjectElement(placement = 3)
                var encrypt = false
            }

            @Data
            @ZoarialNetworkObject
            class UpdateActionLocal {
                @ZoarialObjectElement(placement = 1)
                var uuid: UUID? = null

                @ZoarialObjectElement(placement = 2)
                var property: String? = null

                @ZoarialObjectElement(placement = 3)
                var local = false
            }

            @Data
            @ZoarialNetworkObject
            class UpdateActionDescription {
                @ZoarialObjectElement(placement = 1)
                var uuid: UUID? = null

                @ZoarialObjectElement(placement = 2)
                var property: String? = null

                @ZoarialObjectElement(placement = 3)
                var description: String? = null
            }

            // Get info about a single action
            @Data
            @ZoarialNetworkObject
            class InfoAction {
                @ZoarialObjectElement(placement = 1)
                var action: UUID? = null
            }

            // Get a list of all actions
            @ZoarialNetworkObject
            class InfoActions {
                @ZoarialObjectElement(placement = 1)
                var sessionId: Long? = null
            }

            // Get general information about a node
            @ZoarialNetworkObject
            class InfoGeneral

            // Get a list of known nodes
            @ZoarialNetworkObject
            class InfoNodes
        }
    }
}