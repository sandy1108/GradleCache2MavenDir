package com.wsgh.java.fragment;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class RunnerForConvertGradleCacheToLocalMaven {

    private static final String GRADLE_CACHE_HOME_DIR = "/Users/zhangyipeng/.gradle";
    private static final String GRADLE_CACHE_FILE_DIR = GRADLE_CACHE_HOME_DIR + "/caches/modules-2/files-2.1";
    private static final String MAVEN_OUTPUT_DIR = GRADLE_CACHE_FILE_DIR + "/000LocalMavenConverted";

    public static void main(String[] args) {
        File gradleCacheFilesDir = new File(GRADLE_CACHE_FILE_DIR);
        File[] gradleCacheDirs = gradleCacheFilesDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                if(file.isDirectory()){
                    return true;
                }
                return false;
            }
            
        });
        for (File depGroupDir : gradleCacheDirs) {
            // 找到缓存中的依赖库的每一个组目录
            String groupName = depGroupDir.getName();
            if (groupName.contains("000")) {
                // 做一个例外，防止自己的目录被拷贝走
                continue;
            }
            try {
                for (File depArtifactDir : depGroupDir.listFiles()) {
                    // 遍历组内的各种依赖包目录
                    String artifactName = depArtifactDir.getName();
                    if(depArtifactDir.isFile()){
                        System.out.println("FOR: skip File not Directory: " + depArtifactDir);
                        continue;
                    }
                    for (File depVersionDir : depArtifactDir.listFiles()) {
                        // 遍历找到依赖包的每一个版本目录
                        String depVersionName = depVersionDir.getName();
                        String outputDir = MAVEN_OUTPUT_DIR + File.separator + groupName.replace(".", File.separator)
                                + File.separator + artifactName + File.separator + depVersionName;
                        new File(outputDir).mkdirs();
                        findAndCopyDepLibs(depVersionDir, outputDir);        
                    }
                }
            } catch (Exception e) {
                //TODO: handle exception
                e.printStackTrace();
                throw e;
            }
        }
        System.out.println("Gradle远程依赖Cache转换为本地Maven目录的操作已完成。");
    }

    // 遍历目录内包括下级目录中所有的文件，并全部输出到指定目录
    private static void findAndCopyDepLibs(File destDir, String outputDir) {
        System.out.println("findAndCopyDepLibs: " + destDir.getAbsolutePath());
        for (File file : destDir.listFiles()) {
            if (file.isDirectory()) {
                findAndCopyDepLibs(file, outputDir);
            } else {
                File outputFile = new File(outputDir, file.getName());
                util_copyFileUsingFileStreams(file, outputFile);
            }
        }
    }

    // 暂无用途
    private static File findDepJarsAndAars(File destDir) {
        File finalDepFile = null;
        for (File file : destDir.listFiles()) {
            File depFile = null;
            if (file.isDirectory()) {
                depFile = findDepJarsAndAars(destDir);
                if(depFile != null){
                    // 如果找到了对应的文件，则直接结束。
                    finalDepFile = depFile;
                    break;
                }else{
                    continue;
                }
            } else {
                depFile = file;
                String fileName = depFile.getName();
                if (fileName.endsWith(".jar") || fileName.endsWith(".aar")) {
                    String[] nameParts = fileName.split(".");
                    if (!(nameParts[nameParts.length - 2].contains("-sources")
                        || nameParts[nameParts.length - 2].contains("-doc"))) {
                        // 只要jar包和aar包，只要不是sources和doc的包，就认为是真正的依赖包
                        finalDepFile = depFile;
                        break;
                    }
                }
            }
        }
        return finalDepFile;
    }

    private static void util_copyFileUsingFileStreams(File source, File dest) {
        InputStream input = null;
        OutputStream output = null;
        try {
            if (dest.exists()) {
                System.out.println("异常：发现同名文件： " + dest.getAbsolutePath());
            }
            input = new FileInputStream(source);
            output = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) != -1) {
                output.write(buf, 0, bytesRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                input.close();
                output.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}