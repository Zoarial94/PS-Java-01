package com.zoarial.iot.dto;

import lombok.Value;
import me.zoarial.NetworkArbiter.annotations.ObjectElement;
import me.zoarial.NetworkArbiter.annotations.ZoarialNetworkObject;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public enum ZoarialDTO {;

    public enum V1 {;

        // Request to complete action
        @ZoarialNetworkObject
        public static class Action {
            final static String[] order = {"uuid", "otp", "args"};
            UUID uuid;
            Optional<Integer> otp;
            Optional<ArrayList<String>> args;
        }

        // Request to update info about an action
        @ZoarialNetworkObject
        public static class UpdateActionSecurityLevel {
            final static String[] order = {"uuid", "property", "level"};
            UUID uuid;
            String property;
            byte level;
        }

        @ZoarialNetworkObject
        public static class UpdateActionEncrypt {
            final static String[] order = {"uuid", "property", "encrypt"};
            UUID uuid;
            String property;
            boolean encrypt;
        }

        @ZoarialNetworkObject
        public static class UpdateActionLocal {
            final static String[] order = {"uuid", "property", "local"};
            UUID uuid;
            String property;
            boolean local;
        }

        @ZoarialNetworkObject
        public static class UpdateActionDescription {
            final static String[] order = {"uuid", "property", "description"};
            UUID uuid;
            String property;
            String description;
        }

        // Get info about a single action
        @ZoarialNetworkObject
        public static class InfoAction {
            final static String[] order = {"action"};
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
