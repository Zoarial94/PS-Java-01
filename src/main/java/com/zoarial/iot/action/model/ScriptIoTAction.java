package com.zoarial.iot.action.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@Entity

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScriptIoTAction extends IoTAction {
    @Column(nullable = false)
    @Convert(converter = com.zoarial.jpa.converters.PathConverter.class)
    Path path;
    boolean enabled = false;

    public ScriptIoTAction(String name, UUID uuid, byte level, boolean encrypted, boolean local, byte args, Path file) {
        super(name, uuid, level, encrypted, local, args);
        path = file;
    }

    // TODO: Check file digest (sha1, MD5, etc) for integrity
    public static boolean isValidFile(Path path) {
        return Files.exists(path) &&
                Files.isRegularFile(path) &&
                Files.isReadable(path) &&
                Files.isExecutable(path) &&
                !Files.isSymbolicLink(path);
    }

    @Override
    public boolean isValid() {
        return isValidFile(path);
    }

    @Override
    protected String privExecute(IoTActionArgumentList args) {
        // Make sure permission are still valid, then execute.
        if (!isValid()) {
            return "Unable to run script. Invalid.";
        } else if (!isEnabled()) {
            return "Unable to run script. Disabled.";
        }
        try {
            StringBuilder stringBuilder = new StringBuilder();
            Process p = Runtime.getRuntime().exec(path.toFile().getAbsolutePath());
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
            if (stringBuilder.length() == 0) {
                return "Script returned nothing.";
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            return stringBuilder.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Failed to execute " + getName() + ".";
        }
    }

    @Override
    public String toString() {
        return "ScriptIoTAction{" +
                "path=" + path +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScriptIoTAction)) return false;
        if (!super.equals(o)) return false;
        ScriptIoTAction that = (ScriptIoTAction) o;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), path);
    }
}
