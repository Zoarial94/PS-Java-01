package com.zoarial.iot.action.model;

import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;

@Entity
@NoArgsConstructor
public class ExternalIoTAction extends IoTAction {
    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    protected String privExecute(IoTActionArgumentList args) {
        // TODO: do something similar to JavaIoTAction where a socket is queried when run
        return "This is an external action. You shouldn't be seeing this.";
    }
}
