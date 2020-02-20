/*
 *  Copyright 2020. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.filewatcher.processors;
/*
 * @author Aditya Jagtiani
 */

import com.appdynamics.extensions.filewatcher.config.FileMetric;
import com.appdynamics.extensions.filewatcher.config.PathToProcess;
import com.appdynamics.extensions.filewatcher.helpers.GlobPathMatcher;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.stream.Stream;

import static com.appdynamics.extensions.filewatcher.util.FileWatcherUtil.getFormattedDisplayName;

public class CustomFileVisitor extends SimpleFileVisitor<Path> {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(CustomFileVisitor.class);
    private WatchService watchService;
    private Map<WatchKey, Path> keys;
    private GlobPathMatcher globPathMatcher;
    private PathToProcess pathToProcess;
    private Map<String, FileMetric> fileMetrics;
    private String baseDirectory;

    public CustomFileVisitor(String baseDirectory, WatchService watchService, Map<WatchKey, Path> keys,
                             GlobPathMatcher globPathMatcher, PathToProcess pathToProcess,
                             Map<String, FileMetric> fileMetrics) {
        this.baseDirectory = baseDirectory;
        this.watchService = watchService;
        this.keys = keys;
        this.globPathMatcher = globPathMatcher;
        this.pathToProcess = pathToProcess;
        this.fileMetrics = fileMetrics;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        if (pathToProcess.getIgnoreHiddenFiles() && path.toFile().isHidden()) {
            LOGGER.info("Skipping directory {}. Ignore hidden files = true & the path to this file is hidden.",
                    path.getFileName());
            return FileVisitResult.CONTINUE;
        }
        if (globPathMatcher.getMatcher().matches(path)) {
            LOGGER.info("Match found for entered path {}", path);
            FileMetric fileMetric = new FileMetric();
            setBasicFileAttributesForFile(path, basicFileAttributes, fileMetric);

            if(!keys.containsValue(path)) {
                LOGGER.debug("Now registering path {} with the Watch Service", path.getFileName());
                registerPath(path);
            }

            fileMetrics.put(getFormattedDisplayName(pathToProcess.getDisplayName(), path, baseDirectory),
                    generateDirectoryMetrics(path, basicFileAttributes));
        }
        return FileVisitResult.CONTINUE;
    }

    private void registerPath(Path path) throws IOException {
        WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        keys.put(key, path);
    }

    private FileMetric generateDirectoryMetrics(Path path, BasicFileAttributes basicFileAttributes) {
        FileMetric fileMetric = new FileMetric();
        int fileCount = 0;
        long oldestFile = 0L;
        File[] filesInDir = path.toFile().listFiles();

        if (filesInDir != null && filesInDir.length > 0) {
            oldestFile = filesInDir[0].lastModified();
            for (File f : filesInDir) {
                if (pathToProcess.getIgnoreHiddenFiles()) {
                    if (!f.isHidden()) {
                        if(f.isFile() || !pathToProcess.getExcludeSubdirectoryCount()) {
                            fileCount++;
                        }
                        if (f.lastModified() < oldestFile) {
                            oldestFile = f.lastModified();
                        }
                    }
                    else {
                        LOGGER.info("Skipping directory {} as it is hidden", f);
                    }
                } else {
                    fileCount++;
                    if (f.lastModified() < oldestFile) {
                        oldestFile = f.lastModified();
                    }
                }
            }
        }
        fileMetric.setNumberOfFiles(fileCount);
        long currentTimeInMillis = System.currentTimeMillis();
        long oldestFileAge = -1;
        if (oldestFile > 0 && oldestFile < currentTimeInMillis) {
            oldestFileAge = (currentTimeInMillis - oldestFile) / 1000;
        }
        fileMetric.setAvailable(path.toFile().exists());
        fileMetric.setOldestFileAge(oldestFileAge);

        LOGGER.info("For directory {}, Size = {}, File Count = {} & Oldest File Age = {} ms", path.getFileName(),
                fileMetric.getFileSize(), fileMetric.getNumberOfFiles(), fileMetric.getOldestFileAge());
        return fileMetric;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        if (pathToProcess.getIgnoreHiddenFiles() && path.toFile().isHidden()) {
            LOGGER.debug("Skipping file {} as it is hidden " + path.getFileName());
            return FileVisitResult.CONTINUE;
        }

        if (globPathMatcher.getMatcher().matches(path)) {
            LOGGER.debug("Found match for entered path" + path);
            FileMetric fileMetric = new FileMetric();
            setBasicFileAttributesForFile(path, basicFileAttributes, fileMetric);
            fileMetric.setNumberOfFiles(-1); fileMetric.setOldestFileAge(-1);
            fileMetric.setAvailable(path.toFile().exists());
            fileMetric.setNumberOfLines(getNumberOfLinesFromFile(path));
            LOGGER.info("For file {}, Availability = {}, File Size = {} & Last Modified Time = {} ms, Number of Lines " +
                            "= {}", path.getFileName(), fileMetric.getAvailable(), fileMetric.getFileSize(),
                    fileMetric.getNumberOfFiles(), fileMetric.getNumberOfLines());
            fileMetrics.put(getFormattedDisplayName(pathToProcess.getDisplayName(), path, baseDirectory), fileMetric);
        }
        return FileVisitResult.CONTINUE;
    }

    private void setBasicFileAttributesForFile(Path path, BasicFileAttributes basicFileAttributes, FileMetric fileMetric) {
        if (basicFileAttributes != null) {
            fileMetric.setLastModifiedTime(basicFileAttributes.lastModifiedTime().toMillis());
            fileMetric.setFileSize(String.valueOf(basicFileAttributes.size()));
        } else {
            LOGGER.debug("Couldn't find basic file attributes for {}", path.toString());
        }
    }

    private int getNumberOfLinesFromFile(Path file) throws IOException {
        try (Stream<String> fileStream = Files.lines(file)) {
            return (int) fileStream.count();
        }
    }
}