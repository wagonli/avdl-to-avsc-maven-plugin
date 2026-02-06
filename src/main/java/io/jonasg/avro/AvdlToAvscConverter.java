package io.jonasg.avro;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;

public class AvdlToAvscConverter {

    private final IdlToSchemataTool idlToSchemataTool = new IdlToSchemataTool();
    ;

    private final Log log;

    public AvdlToAvscConverter(Log log) {
        this.log = log;
    }

    public void convert(ConverterConfiguration configuration) {
        if (!configuration.avdlPath().toFile().exists()) {
            log.error("Avdl directory does not exist: " + configuration.avdlPath());
            return;
        }

        try (Stream<Path> stream = walk(configuration.avdlPath())) {
            stream
                    .filter(this::onAvdlExtension)
                    .forEach(avdlFile -> {
                        var relativeAvdlPath = configuration.avdlPath().relativize(avdlFile.getParent());
                        var outputDirectory = configuration.maintainDirectoryStructure()
                                ? configuration.avscPath().resolve(relativeAvdlPath)
                                : configuration.avscPath();
                        avdlToAvsc(avdlFile, outputDirectory.toFile());
                    });

        } catch (IOException e) {
            log.error("Error walking avdl directory", e);
        }
    }

    boolean onAvdlExtension(Path path) {
        var file = path.toFile();
        var fileName = file.getName();
        return Objects.equals("avdl", FilenameUtils.getExtension(fileName));
    }

    void avdlToAvsc(Path avdlFilePath, File outputDirectory) {
        List<String> toolsArgs = new ArrayList<>();
        toolsArgs.add(avdlFilePath.toFile().getAbsolutePath());
        toolsArgs.add(outputDirectory.getAbsolutePath());

        try {
            idlToSchemataTool.run(System.in, System.out, System.err, toolsArgs);
            log.info("Converted " + avdlFilePath);
        } catch (Exception ex) {
            log.error("Error converting " + avdlFilePath + " to " + outputDirectory, ex);
            throw new RuntimeException(ex);
        }
    }

}
