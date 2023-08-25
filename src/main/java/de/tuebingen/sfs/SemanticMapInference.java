package de.tuebingen.sfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.tuebingen.sfs.causal.algorithms.PcAlgorithm;
import de.tuebingen.sfs.causal.algorithms.PcStarAlgorithm;
import de.tuebingen.sfs.causal.data.CausalGraph;
import de.tuebingen.sfs.causal.data.CausalGraphSummary;
import de.tuebingen.sfs.causal.heuristics.arrows.CausalArrowFinder;
import de.tuebingen.sfs.causal.heuristics.arrows.CausalArrowFinderPcDefault;
import de.tuebingen.sfs.causal.heuristics.separation.PartialCorrelationDiscreteUnitFlow;
import de.tuebingen.sfs.lextyp.data.IsolecticAreaProcessing;
import de.tuebingen.sfs.lextyp.io.IsolecticAreaReader;
import de.tuebingen.sfs.lextyp.struct.IsolecticArea;
import de.tuebingen.sfs.util.struct.Triple;

public class SemanticMapInference {

	private static List<Set<Set<Triple<String, String, String>>>> resamplePartitionsConceptLevel(
			List<Set<Set<Triple<String, String, String>>>> samplePartitions) {
		int size = samplePartitions.size();
		List<Set<Set<Triple<String, String, String>>>> resample = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			resample.add(samplePartitions.get((int) (Math.random() * size)));
		}
		return resample;
	}

	private static String[] createVarNames(Set<String> concepts) {
		String[] varNames = new String[concepts.size()];
		int id = 0;
		for (String concept : concepts) {
			varNames[id] = concept;
			id++;
		}
		return varNames;
	}

	private static Options defineOptions() {
		Options options = new Options();

		Option allSamples = new Option("a", "allSamples", false, "Output maps resulting from each sample.");
		options.addOption(allSamples);

		Option bootstrap = new Option("b", "bootstrap", false, "Derive confidence values via bootstrapping.");
		options.addOption(bootstrap);
		// TODO: add optional specification of the number of bootstrap samples

		Option directionality = new Option("d", "directionality", false, "Use causal inference to infer arrows.");
		options.addOption(directionality);
		// TODO: add optional specification of strategy for directionality inference

		Option randomOrder = new Option("r", "randomOrder", false, "Randomize link deletion order (for minimization).");
		options.addOption(randomOrder);
		
		Option minimalMapOutput = new Option("m", "minimalMap", false, "Output semantic map of minimal size among samples.");
		options.addOption(minimalMapOutput);
		//TODO: this should be mutually exclusive with the bootstrap!

		Option concepts = Option.builder("c").longOpt("concepts").argName("conceptFile").hasArg().required(false)
				.desc("Specify concepts. (default: all)").build();
		options.addOption(concepts);

		Option input = Option.builder("i").longOpt("input").argName("isolecticSetsFile").hasArg().required(true)
				.desc("Specify input file (= isolectic sets).").build();
		options.addOption(input);

		Option output = Option.builder("o").longOpt("output").argName("outputDotFile").hasArg().required(true)
				.desc("Specify output file (= semantic map).").build();
		options.addOption(output);
		
		//TODO: make optional, simple text output in case no dot output is specified

		Option logfile = Option.builder("l").longOpt("logfile").argName("logFile").hasArg().required(false)
				.desc("Specify logfile (textual output).").build();
		options.addOption(logfile);

		// TODO: option p to specify positions (in x y coordinates)

		return options;
	}

	public static void main(String[] args) {
		Options options = defineOptions();

		// define parser
		CommandLine cmd;
		CommandLineParser parser = new DefaultParser();
		HelpFormatter helper = new HelpFormatter();

		try {
			cmd = parser.parse(options, args);

			String inputFilePath = "";
			String outputFilePath = "";

			Set<String> concepts = new TreeSet<String>();
			String conceptFilePath = null;
			int minConceptOccurrences = 0;
			
			boolean directionality = false;

			boolean bootstrapping = false;
			int numSamples = 1;

			if (cmd.hasOption("i")) {
				inputFilePath = cmd.getOptionValue("input");
				System.out.println("Reading isolectic sets from input file: " + inputFilePath);
			}

			if (cmd.hasOption("o")) {
				outputFilePath = cmd.getOptionValue("output");
				System.out.println("Will write semantic map in DOT format to output file: " + outputFilePath);
			}

			if (cmd.hasOption("b")) {
				System.out.println("Will use bootstrapping on the language level in order to derive confidence values.");
				bootstrapping = true;
				numSamples = 100;
			}
			
			if (cmd.hasOption("d")) {
				System.out.println("Will apply arrow inference in order to derive a diachronic semantic map.");
				directionality = true;
			}

			if (cmd.hasOption("c")) {
				conceptFilePath = cmd.getOptionValue("concepts");
				System.out.println("Analysis is limited to concepts specified in file: " + conceptFilePath);
			}

			Set<IsolecticArea> isolecticAreas = IsolecticAreaReader.loadFromFile(inputFilePath);
			//List<IsolecticArea> sample = new ArrayList<IsolecticArea>(isolecticAreas);
			//Collections.shuffle(sample);

			// if no concept file was provided, select all concepts which occur in a certain
			// number of isolectic sets (default: 0, i.e. no filtering)
			if (conceptFilePath == null) {
				IsolecticAreaProcessing.filterConceptsByMinOccurrence(concepts, isolecticAreas, minConceptOccurrences);
			} else {
				// TODO: load concept file
			}

			// selected/filtered concepts are the variables for causal inference
			String[] varNames = createVarNames(concepts);

			List<Set<Set<Triple<String, String, String>>>> samplePartitions = IsolecticAreaProcessing
					.isolecticAreasToSamplePartitions(isolecticAreas);
			System.err.println("Extracted isolectic sets from " + samplePartitions.size() + " languages.");

			CausalGraphSummary bootstrapSummary = new CausalGraphSummary(varNames);
			int minMapSize = Integer.MAX_VALUE;
			
			double[][] thresholds = new double[concepts.size()][concepts.size()];
			for (double[] thresholdRow : thresholds) {
				Arrays.fill(thresholdRow, 1.00);
				// 5.00 to let only links with some chance of directionality in hypergeometric
				// test survive
			}
			
			for (int k = 0; k < numSamples; k++) {
				List<Set<Set<Triple<String, String, String>>>> sample = samplePartitions;
				if (bootstrapping) {
					sample = resamplePartitionsConceptLevel(samplePartitions);
				}

				CausalGraph semanticMap = new CausalGraph(varNames, false);
				for (IsolecticArea area : isolecticAreas) {
					for (String concept1 : area.getConcepts()) {
						if (!concepts.contains(concept1))
							continue;
						int var1 = semanticMap.nameToVar.get(concept1);
						for (String concept2 : area.getConcepts()) {
							if (!concepts.contains(concept2))
								continue;
							int var2 = semanticMap.nameToVar.get(concept2);
							if (var1 != var2) {
								// System.err.println(area);
								semanticMap.addLink(var1, var2);
								semanticMap.putArrow(var1, var2, false);
							}
						}
					}
				}

				// conditional independence criterion defined by discrete unit flow
				// (implementing the connected component criterion for isolectic sets)
				PartialCorrelationDiscreteUnitFlow corrMeasure = new PartialCorrelationDiscreteUnitFlow(
						sample, semanticMap, varNames, thresholds, false);
				
				// apply v-structure criteria from PC algorithm (stable and conservative variant)
				CausalArrowFinder<List<Set<Set<Triple<String, String, String>>>>> arrowFinder = 
						new CausalArrowFinderPcDefault<List<Set<Set<Triple<String, String, String>>>>>(
						sample, varNames, true, true);

				// run PC algorithm to derive the semantic map
				PcStarAlgorithm pcInstance = new PcStarAlgorithm(corrMeasure, arrowFinder, varNames, semanticMap,
						concepts.size(), true, true, true);
				pcInstance.runSkeletonInference();
				if (directionality) pcInstance.runDirectionalityInference();
				
				if (numSamples == 1) {
					System.out.println("\nRESULT:");
					System.out.println("=======\n");
					semanticMap.printInTextFormat();
					
					//TODO: dot output to output file (if specified)
				} else {
					
				}
			}
			// TODO: print summary in case numSamples > 1
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			helper.printHelp(" ", options);
			System.exit(0);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}

	}
}
