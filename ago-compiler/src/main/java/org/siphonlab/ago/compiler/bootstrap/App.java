package org.siphonlab.ago.compiler.bootstrap;

import org.apache.commons.cli.*;
import org.apache.commons.io.file.PathUtils;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.ClassFile;
import org.siphonlab.ago.compiler.Compiler;
import org.siphonlab.ago.compiler.Unit;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.ZipInputStream;

/**
 *
 * Supported syntax:
 *   -i <file> [<file> ...]   one or more input files (mandatory)
 *   -o <dir>                 output directory (mandatory)
 *   -agocp                   class path
 *   -h | --help              display help
 */
public class App {

    public static void main(String[] args) {
        // 1. Define the command‑line options
        Options options = buildOptions();

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                formatter.printHelp(
                        "java -cp ago-compiler.jar [-agocp classpath] -i ago_files -o output_directory|output.agopkg",
                        options,
                        true);
                System.exit(0);
            }

            String[] inputFiles = cmd.getOptionValues("i");
            if (inputFiles == null || inputFiles.length == 0) {
                throw new ParseException("-i must be followed by at least one file.");
            }
            for (String inputFile : inputFiles) {
                var f = new File(inputFile);
                if(!f.exists()){
                    throw new ParseException("'%s' not found, the absolute path '%s'".formatted(inputFile, f.getAbsolutePath()));
                }
            }

            String outputDir = cmd.getOptionValue("o");
            if (outputDir == null) {
                outputDir = "./";
            }

            String[] classPaths = cmd.getOptionValues("agocp");
            if(classPaths != null){
                for (String classPath : classPaths) {
                    var f = new File(classPath);
                    if(!f.exists()){
                        throw new ParseException("'%s' not found, the absolute path '%s'".formatted(classPath, f.getAbsolutePath()));
                    }
                }
            }

            // 4. Demo logic – just print what we got
            System.out.println("Input files: " + Arrays.toString(inputFiles));
            System.out.println("Output: " + outputDir);
            System.out.println("Class paths: " + Arrays.toString(classPaths));

            // Create the output directory if it does not exist
            File out = new File(outputDir);
            if (!out.exists() && !outputDir.endsWith(".agopkg")) {
                boolean created = out.mkdirs();
                if (!created) {
                    System.err.println("Error: could not create directory " + outputDir);
                    System.exit(1);
                }
            }

            compile(Arrays.stream(inputFiles).map(File::new).toArray(File[]::new), out, classPaths);

        } catch (ParseException e) {
            System.err.println("Argument error: " + e.getMessage());
            formatter.printHelp(
                    "java -cp ago-compiler.jar [-agocp classpath] -i ago_files -o output_directory",
                    options,
                    true);
            System.exit(1);
        } catch (CompilationError | IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void compile(File[] inputFiles, File out, String[] classPaths) throws IOException, CompilationError {
        Compiler compiler = new Compiler();
        Collection<ClassDef> rtClasses;
        AgoClassLoader agoClassLoader = new AgoClassLoader();
        Unit[] units;
        if(classPaths != null && classPaths.length > 0) {
            for (String classPath : classPaths) {
                if (classPath.endsWith(".agopkg")) {
                    agoClassLoader.loadClasses(new ZipInputStream(new FileInputStream(classPath)));
                } else {
                    agoClassLoader.loadClasses(classPath);
                }
            }
            rtClasses = compiler.load(agoClassLoader);
            units = compiler.compile(inputFiles, rtClasses.toArray(new ClassDef[0]));
        } else {
            units = compiler.compile(inputFiles);
        }

        if(out.getName().endsWith(".agopkg")) {
            ClassFile.createPackage(units, new FileOutputStream(out));
        } else {
            ClassFile.saveToDirectory(units, out.getAbsolutePath());
        }
    }

    /**
     * Build and return the Options object.
     */
    private static Options buildOptions() {
        Options opts = new Options();

        Option cpOption = Option.builder("agocp")
                .longOpt("agoclasspath")
                .hasArgs()
                .valueSeparator(' ')   // optional – split by space (default)
                .desc("ago class path")
                .required(false)       // we check later manually
                .build();

        Option iOption = Option.builder("i")
                .longOpt("input")
                .hasArgs()
                .valueSeparator(' ')   // optional – split by space (default)
                .desc("List of input files (at least one).")
                .required(false)       // we check later manually
                .build();

        Option oOption = Option.builder("o")
                .longOpt("output")
                .hasArg()
                .desc("Output directory or ago package file(extension '.agopkg').")
                .required(false)
                .build();

        Option hOption = Option.builder("h")
                .longOpt("help")
                .desc("Display help.")
                .required(false)
                .build();

        opts.addOption(cpOption);
        opts.addOption(iOption);
        opts.addOption(oOption);
        opts.addOption(hOption);

        return opts;
    }
}
