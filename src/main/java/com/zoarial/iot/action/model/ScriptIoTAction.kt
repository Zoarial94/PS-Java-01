package com.zoarial.iot.action.model

import com.zoarial.jpa.converters.PathConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import lombok.AccessLevel
import lombok.Getter
import lombok.NoArgsConstructor
import lombok.Setter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@Entity
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
open class ScriptIoTAction constructor(name: String? = null, uuid: UUID? = null, securityLevel: Byte? = null, encrypted: Boolean? = null, local: Boolean? = null, arguments: Byte? = null,
                                                     @Convert(converter = PathConverter::class) @Column(nullable = false) var path: Path? = null) : IoTAction(name, uuid, securityLevel, encrypted, local, arguments) {

    init {
        // For security reasons, have script actions disabled by default
        // TODO: this might be redundant
        super.disable()
    }

    override val isValid: Boolean
        get() = isValidFile(path)

    override fun privExecute(args: IoTActionArgumentList): String {
        return try {
            val stringBuilder = StringBuilder()
            val p = Runtime.getRuntime().exec(path!!.toFile().absolutePath)
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
                stringBuilder.append('\n')
            }
            if (stringBuilder.isEmpty()) {
                return "Script returned nothing."
            }
            stringBuilder.deleteCharAt(stringBuilder.length - 1)
            stringBuilder.toString()
        } catch (ex: Exception) {
            ex.printStackTrace()
            "Failed to execute $name."
        }
    }

    companion object {
        // TODO: Check file digest (sha1, MD5, etc) for integrity
        // TODO: Check for file owner
        fun isValidFile(path: Path?): Boolean {
            return Files.exists(path!!) &&
                    Files.isRegularFile(path) &&
                    Files.isReadable(path) &&
                    Files.isExecutable(path) &&
                    !Files.isSymbolicLink(path)
        }
    }
}