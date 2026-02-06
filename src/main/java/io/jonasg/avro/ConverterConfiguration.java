package io.jonasg.avro;

import java.nio.file.Path;

public record ConverterConfiguration(Path avdlPath, Path avscPath, boolean maintainDirectoryStructure) {
}
