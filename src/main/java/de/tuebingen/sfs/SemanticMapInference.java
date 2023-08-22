package de.tuebingen.sfs;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class SemanticMapInference 
{
	
	public static Options defineOptions() {
		Options options = new Options();
		
    	Option allSamples = new Option("a", "allSamples", false, "Output maps resulting from each sample.");
    	options.addOption(allSamples);
    	
    	Option bootstrap = new Option("b", "bootstrap", false, "Add bootstrapping on the language level.");
    	options.addOption(bootstrap);
    	//TODO: add optional specification of the number of bootstrap samples
    	
    	Option directionality = new Option("d", "directionality", false, "Use causal inference to infer arrows.");
    	options.addOption(directionality);
    	//TODO: add optional specification of strategy for directionality inference
    	
    	Option randomOrder = new Option("r", "randomOrder", false, "Randomize link deletion order.");
    	options.addOption(randomOrder);
    	
        Option concepts = Option.builder("c").longOpt("concepts")
                .argName("conceptFile")
                .hasArg()
                .required(false)
                .desc("Specify concepts. (default: all)").build();
        options.addOption(concepts);
    	
        Option input = Option.builder("i").longOpt("input")
                .argName("isolecticSetsFile")
                .hasArg()
                .required(true)
                .desc("Specify input file (= isolectic sets).").build();
        options.addOption(input);
        
        Option output = Option.builder("o").longOpt("output")
                .argName("outputDotFile")
                .hasArg()
                .required(true)
                .desc("Specify output file (= semantic map).").build();
        options.addOption(output);
        
        Option logfile = Option.builder("l").longOpt("logfile")
                .argName("logFile")
                .hasArg()
                .required(false)
                .desc("Specify logfile (textual output).").build();
        options.addOption(logfile);
        
        return options;
	}
	
    public static void main( String[] args )
    {
    	Options options = defineOptions();
 
        // define parser
        CommandLine cmd;
        CommandLineParser parser = new DefaultParser();
        HelpFormatter helper = new HelpFormatter();
    	
    	try {
    	    cmd = parser.parse(options, args);
    	    if(cmd.hasOption("b")) {
    	    System.out.println("Using bootstrapping on the language value.");
    	    }

    	    if (cmd.hasOption("c")) {
    	    String opt_config = cmd.getOptionValue("config");
    	    System.out.println("Config set to " + opt_config);
    	    }
    	} catch (ParseException e) {
    	    System.out.println(e.getMessage());
    	    helper.printHelp(" ", options);
    	    System.exit(0);
    	}
    	
    }
}
