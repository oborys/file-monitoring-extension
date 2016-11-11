package com.appdynamics.extensions.filewatcher;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.filewatcher.config.Configuration;
import com.appdynamics.extensions.filewatcher.config.FileToProcess;
import com.appdynamics.extensions.filewatcher.pathmatcher.GlobPathMatcher;
import com.appdynamics.extensions.filewatcher.pathmatcher.factory.PathMatcherFactory;
import com.appdynamics.extensions.filewatcher.pathmatcher.factory.PathMatcherFactory.PathMatcherTypes;
import com.appdynamics.extensions.filewatcher.pathmatcher.visitors.FilePathVisitor;
import com.google.common.collect.Maps;

/**
 * Created by abhi.pandey on 9/17/14.
 */
public class FileProcessor {

    protected final Logger logger = Logger.getLogger(FileProcessor.class.getName());
    private Map<String, String> filesToProcessMap = Maps.newHashMap();
    private String metricSeparator;

    public void setMetricSeparator(String metricSeparator) {
        this.metricSeparator = metricSeparator;
    }

    public FileMetric getFileMetric(String filePath, boolean ignoreHiddenFiles) {
        FileMetric fileMetric;

        File file = new File(filePath);
        if (file.exists()) {
            fileMetric = new FileMetric();
            fileMetric.setTimeStamp(String.valueOf(file.lastModified()));
            if (file.isFile()) {
                fileMetric.setFileSize(String.valueOf(file.length()));
            } else {
                fileMetric.setFileSize(String.valueOf(directorySize(file, ignoreHiddenFiles)));
            }
            if (file.isDirectory()) {
                fileMetric.setNumberOfFiles(countOfFilesInDirectory(file, ignoreHiddenFiles));
                fileMetric.setOldestFileAge(getOldestFileAge(file));
            } else {
                fileMetric.setNumberOfFiles(-1);
                fileMetric.setOldestFileAge(-1);
            }

        } else {
            logger.error("no file exist at path:  " + filePath);
            return null;
        }
        return fileMetric;
    }

    private long getOldestFileAge(File file) {
        if (file.listFiles().length == 0) {
            logger.debug("directory- " + file.getAbsolutePath() + " has no files");
            return -1;
        }

        File[] files = file.listFiles();
        long oldestFileLastModifiedTimeStamp = files[0].lastModified();

        for (int i = 1; i < files.length; i++) {
            if (files[i].lastModified() < oldestFileLastModifiedTimeStamp) {
                oldestFileLastModifiedTimeStamp = files[i].lastModified();
            }
        }
        long currentTimeInMillis = System.currentTimeMillis();
        long oldestFileAge = -1;
        if (oldestFileLastModifiedTimeStamp < currentTimeInMillis) {
            oldestFileAge = (currentTimeInMillis - oldestFileLastModifiedTimeStamp) / 1000;
        }
        return oldestFileAge;
    }

    private long directorySize(File file, boolean ignoreHiddenFiles) {
        if (file.listFiles() == null) {
            logger.debug("directorySize called on a non directory.");
            return file.length();
        }
        long size = 0;
        if (ignoreHiddenFiles) {
            for (File f : file.listFiles()) {
                if (!f.isHidden()) {
                    if (f.isDirectory()) {
                        size += directorySize(f, ignoreHiddenFiles);
                    } else {
                        size += f.length();
                    }
                }
            }
        } else {
            for (File f : file.listFiles()) {
                if (f.isDirectory()) {
                    size += directorySize(f, ignoreHiddenFiles);
                } else {
                    size += f.length();
                }
            }
        }
        return size;
    }

    private int countOfFilesInDirectory(File file, boolean ignoreHiddenFiles) {
        int count = 0;
        if (file.listFiles() == null) {
            logger.debug("directory- " + file.getAbsolutePath() + " has no files");
            return count;
        }
        for (File f : file.listFiles()) {
            if (ignoreHiddenFiles) {
                if (!f.isHidden()) {
                    count++;
                }
            } else {
                count++;
            }
        }
        return count;
    }

    public Map<String, String> processDisplayName(Configuration conf,List<FileToProcess> files, boolean isDirectoryDetailsRequired) {

//        for (FileToProcess fileToProcess : files) {
//            File file = new File(fileToProcess.getPath());
//            if (file.exists()) {
//                String displayName = fileToProcess.getDisplayName();
//
//                if (!Strings.isNullOrEmpty(displayName)) {
//                    if (isDirectoryDetailsRequired && file.isDirectory()) {
//                        List<FileToProcess> directoryFiles = new ArrayList<FileToProcess>();
//
//                        for (File f : file.listFiles()) {
//                            if (!pathOfFiles.contains(f.getAbsolutePath())) {
//                                FileToProcess fp = new FileToProcess();
//                                fp.setPath(f.getAbsolutePath());
//                                fp.setDisplayName(displayName.concat(metricSeparator).concat(f.getName()));
//                                directoryFiles.add(fp);
//                            }
//                        }
//                        processDisplayName(directoryFiles, isDirectoryDetailsRequired);
//                    }
//                }
//                System.out.println(fileToProcess.getPath() + " " + displayName);
//                filesToProcessMap.put(fileToProcess.getPath(), displayName);
//            }else{
//                logger.error("File doesn't exist "+ file.getAbsolutePath());
//            }
//        }
//        
//        return filesToProcessMap;
    	
    	for(FileToProcess fileToProcess : files){
		GlobPathMatcher globPathMatcher = (GlobPathMatcher) PathMatcherFactory.getPathMatcher(PathMatcherTypes.GLOB, fileToProcess, conf);
		try {
			filesToProcessMap.putAll(FilePathVisitor.walkFilesByGlobMatcher(fileToProcess, globPathMatcher));
		} catch (IOException e) {
			logger.error("Error in walking file to process path " + e);
		}
		
	}	
    return filesToProcessMap;
    	
    }


}
