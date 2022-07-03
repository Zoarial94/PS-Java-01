package com.zoarial.jpa.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.file.Path;
import java.nio.file.Paths;

@Converter
public class PathConverter implements AttributeConverter<Path, String> {

    @Override
    public String convertToDatabaseColumn(Path path) {
        return path.toFile().getAbsolutePath();
    }

    @Override
    public Path convertToEntityAttribute(String s) {
        return Paths.get(s);
    }
}
