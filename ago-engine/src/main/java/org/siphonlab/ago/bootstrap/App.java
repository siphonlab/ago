package org.siphonlab.ago.bootstrap;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.classloader.AgoClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipInputStream;

public class App {

    public static void main(String[] args) {
        Options options = new Options();

        Option iOpt = Option.builder("i")
                .hasArgs()
                .argName("files")
                .desc("Input .agoc files (one or more)")
                .required(false)
                .build();
        options.addOption(iOpt);

        Option eOpt = Option.builder("e")
                .hasArg()
                .argName("entry")
                .desc("Entry point, default is 'main#'")
                .required(false)
                .build();
        options.addOption(eOpt);

        Option classPathOpt = Option.builder("agocp")
                .longOpt("agoclasspath")
                .hasArgs()
                .argName("agocp")
                .desc("ago class paths")
                .required(true)
                .build();
        options.addOption(classPathOpt);

        Option helpOpt = new Option("h", "help", false, "Show help");
        options.addOption(helpOpt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                formatter.printHelp(
                        "java -cp ago-engine.jar -agocp classpath... -i files... [-e entry]",
                        options);
                System.exit(0);
            }

            String[] inputFiles = cmd.getOptionValues("i");
            String entryPoint = cmd.getOptionValue("e");
            String[] classPaths = cmd.getOptionValues("agocp");

            if (inputFiles == null || inputFiles.length == 0) {
                if(classPaths == null || classPaths.length == 0) {
                    throw new ParseException("both input files and classpaths not provided");
                }
            } else {
                for (String inputFile : inputFiles) {
                    var f = new File(inputFile);
                    if (!f.exists()) {
                        throw new ParseException("'%s' not found, the absolute path '%s'".formatted(inputFile, f.getAbsolutePath()));
                    }
                }
            }

            if(classPaths != null){
                for (String classPath : classPaths) {
                    var f = new File(classPath);
                    if(!f.exists()){
                        throw new ParseException("'%s' not found, the absolute path '%s'".formatted(classPath, f.getAbsolutePath()));
                    }
                }
            }

            if(StringUtils.isEmpty(entryPoint)) entryPoint = "main#";

            run(inputFiles == null ? null : Arrays.stream(inputFiles).map(File::new).toArray(File[]::new), entryPoint, classPaths);

        } catch (ParseException ex) {
            System.err.println("Error parsing command line: " + ex.getMessage());
            formatter.printHelp(
                    "java -cp ago-engine.jar [options] -i files... -e entry",
                    options);
            System.exit(1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(File[] inputFiles, String entryPoint, String[] classPaths) throws IOException {
        AgoEngine engine = new AgoEngine();
        AgoClassLoader agoClassLoader = new AgoClassLoader();

        if(classPaths != null) {
            for (String classPath : classPaths) {
                if (classPath.endsWith(".agopkg")) {
                    agoClassLoader.loadClasses(new ZipInputStream(new FileInputStream(classPath)));
                } else {
                    agoClassLoader.loadClasses(classPath);
                }
            }
        }
        if(inputFiles != null)
            agoClassLoader.loadClasses(inputFiles);

        engine.load(agoClassLoader);

        engine.run(entryPoint);
    }
}

