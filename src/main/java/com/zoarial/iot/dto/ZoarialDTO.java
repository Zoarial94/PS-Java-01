package com.zoarial.iot.dto;

import lombok.Data;
import me.zoarial.networkArbiter.annotations.ZoarialNetworkObject;
import me.zoarial.networkArbiter.annotations.ZoarialObjectElement;

import java.util.UUID;

public enum ZoarialDTO {;

    public enum V1 {;
        public enum Request {;

            // Request to complete action
            @Data
            @ZoarialNetworkObject
            public static class Action {
                @ZoarialObjectElement(placement = 1)
                UUID uuid;
                @ZoarialObjectElement(placement = 2, optional = true)
                Short otp;
                @ZoarialObjectElement(placement = 3)
                String data;
            }

            // Request to update info about an action
            @Data
            @ZoarialNetworkObject
            public static class UpdateActionSecurityLevel {
                @ZoarialObjectElement(placement = 1)
                UUID uuid;
                @ZoarialObjectElement(placement = 2)
                String property;
                @ZoarialObjectElement(placement = 3)
                byte level;
            }

            @Data
            @ZoarialNetworkObject
            public static class UpdateActionEncrypt {
                @ZoarialObjectElement(placement = 1)
                UUID uuid;
                @ZoarialObjectElement(placement = 2)
                String property;
                @ZoarialObjectElement(placement = 3)
                boolean encrypt;
            }

            @Data
            @ZoarialNetworkObject
            public static class UpdateActionLocal {
                @ZoarialObjectElement(placement = 1)
                UUID uuid;
                @ZoarialObjectElement(placement = 2)
                String property;
                @ZoarialObjectElement(placement = 3)
                boolean local;
            }

            @Data
            @ZoarialNetworkObject
            public static class UpdateActionDescription {
                @ZoarialObjectElement(placement = 1)
                UUID uuid;
                @ZoarialObjectElement(placement = 2)
                String property;
                @ZoarialObjectElement(placement = 3)
                String description;
            }

            // Get info about a single action
            @Data
            @ZoarialNetworkObject
            public static class InfoAction {
                @ZoarialObjectElement(placement = 1)
                UUID action;
            }

            // Get a list of all actions
            @ZoarialNetworkObject
            public static class InfoActions {
            }

            // Get general information about a node
            @ZoarialNetworkObject
            public static class InfoGeneral {
            }

            // Get a list of known nodes
            @ZoarialNetworkObject
            public static class InfoNodes {
            }
        }
    }
}
