package com.zoarial.jpa.converters

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.nio.file.Path
import java.nio.file.Paths

@Converter
class PathConverter : AttributeConverter<Path, String> {
    override fun convertToDatabaseColumn(path: Path): String {
        return path.toFile().absolutePath
    }

    override fun convertToEntityAttribute(s: String): Path {
        return Paths.get(s)
    }
}