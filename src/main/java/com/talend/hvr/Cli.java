package com.talend.hvr;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Hashtable;

public class Cli {

    private static final Logger logger = Logger.getLogger(Cli.class);
    private static final Options options = new Options();
    private static CommandLine cmd = null;
    private static ArrayList<String> regions = new ArrayList<String>();
    private static final String _VERSION = "1.3.0";

    static {
        options.addOption("h", "help", false, "show help.");
        options.addOption("w", "wait", false, "Will block any other commands from executing until job is completed");
        options.addOption("t", "token", true, "Talend Cloud Token");
        options.addOption("te", "tokenenv", true, "Talend Cloud Token in Environment Variable");
        //options.addOption("j", "job", true, "The Talend Cloud Job to Execute");
        options.addOption("r","region", true, "Talend Cloud Region [AWS_USA_EAST | AWS_EMEA | AWS_APAC | AZURE_USA_WEST]");
        options.addOption("e","environment",true, "The environment the job is in. If not used name will be `default`");
        options.addOption("cv","contextvariables", true, "Context Variables to pass. EX: name1=value1;name2=value2");
        options.addOption("hm","hvrmanifest", true, "Directory of HVR manifest file(s)");
        options.addOption("tm","tlndmanifest", true, "Path to Talend manifest");
        options.addOption("v","version", false,"product version");

        regions.add("AWS_USA_EAST");
        regions.add("AZURE_USA_WEST");
        regions.add("AWS_EMEA");
        regions.add("AWS_APAC");
    }

    public static String getCliValue(String option)
    {
        try {
            if (cmd.hasOption(option)) {
                return cmd.getOptionValue(option);
            }
            else
                return null;
        } catch(NullPointerException e)
        {
            return null;
        }
    }

    public static boolean hasCliValue(String option)
    {
        return cmd.hasOption(option);
    }

    public static void parse(String[] argv) {


        try {
            boolean hasPwd = true;
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, argv);
            if (cmd.hasOption("v"))
            {
                System.out.println("Version: " + _VERSION);
                System.exit(0);
            }
            if (cmd.hasOption("h")) {
                help();
            }

            if (cmd.hasOption("t"))
            {
                logger.debug("Using cli argument -t");
            }

            if (cmd.hasOption("te"))
            {
                logger.debug("Using cli argument -te");
            }

            /*
            if (cmd.hasOption("j"))
            {
                logger.debug("Using cli argument -j");
            } else {
                System.err.println("Job parameter is required!");
                help();
            }
            */

            if (cmd.hasOption("r") && !regions.contains(cmd.getOptionValue("r")))
            {
                System.err.println("Region not valid: " + cmd.getOptionValue("r"));
                help();
            }

            if (cmd.hasOption("e")) {
                logger.debug("Using cli argument -e");
            }
            if (cmd.hasOption("cv")) {
                logger.debug("Using cli argument -cv");
            }

            if (!cmd.hasOption("tm") && !cmd.hasOption("hm"))
            {
                logger.error("Both -tm and -hm parameters must be used");
                System.err.println("Both -tm and -hm parameters must be used");
                help();
            }

            if ((cmd.hasOption("hm") && !cmd.hasOption("tm")) || (cmd.hasOption("tm") && !cmd.hasOption("hm")))
            {
                logger.error("Both -tm and -hm parameters must be used");
                System.err.println("Both -tm and -hm parameters must be used");
                help();
            }



            if (cmd.hasOption("w")) {
                logger.debug("Using cli argument -w");
                if (cmd.hasOption("s"))
                {
                    logger.error("Use of both cli arguments -s and -w is not permitted");
                    System.err.print("Use of both cli arguments -s and -w is not permitted");
                    help();
                }
            }



        } catch (ParseException e) {
            logger.fatal("Failed to parse command line properties", e);
            help();
        }

    }

    private static void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("tcli", options);
        System.exit(0);
    }

    private Cli() {
    } // Disabled constructor
}