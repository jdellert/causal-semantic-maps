package de.tuebingen.sfs;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.tuebingen.sfs.causal.algorithms.PcStarAlgorithm;
import de.tuebingen.sfs.causal.data.CausalGraph;
import de.tuebingen.sfs.causal.data.CausalGraphOutput;
import de.tuebingen.sfs.causal.data.CausalGraphSummary;
import de.tuebingen.sfs.causal.heuristics.arrows.CausalArrowFinder;
import de.tuebingen.sfs.causal.heuristics.arrows.CausalArrowFinderPcDefault;
import de.tuebingen.sfs.causal.heuristics.separation.PartialCorrelationDiscreteUnitFlow;
import de.tuebingen.sfs.lextyp.data.IsolecticAreaProcessing;
import de.tuebingen.sfs.lextyp.io.IsolecticAreaReader;
import de.tuebingen.sfs.lextyp.struct.IsolecticArea;
import de.tuebingen.sfs.util.io.ListReader;
import de.tuebingen.sfs.util.struct.Triple;

public class SemanticMapInference {

	private static List<Set<Set<Triple<String, String, String>>>> resamplePartitionsLanguageLevel(
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

		Option directionality = new Option("d", "directionality", false,
				"Perform directionality inference to produce a diachronic semantic map.");
		options.addOption(directionality);
		// TODO: add optional specification of strategy for directionality inference

		Option randomOrder = new Option("r", "randomOrder", false, "Randomize link deletion order (for minimization).");
		options.addOption(randomOrder);

		Option minimalMapOutput = new Option("m", "minimalMap", false,
				"Output semantic map of minimal size among samples.");
		options.addOption(minimalMapOutput);

		Option concepts = Option.builder("c").longOpt("concepts").argName("conceptFile").hasArg().required(false)
				.desc("Specify concepts. (default: all)").build();
		options.addOption(concepts);

		Option languages = Option.builder("l").longOpt("languages").argName("languageFile").hasArg().required(false)
				.desc("Specify languages. (default: all)").build();
		options.addOption(languages);

		Option input = Option.builder("i").longOpt("input").argName("inputFile").hasArg().required(true)
				.desc("Specify input file (= isolectic sets).").build();
		options.addOption(input);

		Option output = Option.builder("vo").longOpt("visOutput").argName("outputPrefix").hasArg().required(false)
				.desc("Specify path and filename prefix for the visualized map(s).").build();
		options.addOption(output);

		Option logfile = Option.builder("log").longOpt("logfile").argName("logFile").hasArg().required(false)
				.desc("Specify logfile (textual output).").build();
		options.addOption(logfile);

		Option coordinates = Option.builder("vc").longOpt("coordinates").argName("coordFile").hasArg().required(false)
				.desc("Specify coordinates for visualization output.").build();
		options.addOption(coordinates);

		Option linkThreshold = Option.builder("lt").longOpt("linkThreshold").argName("threshold").hasArg()
				.required(false)
				.desc("Specify minimal number of colexifications for a link (recommended: 0 for perfect data, 3 for noisy databases).")
				.build();
		options.addOption(linkThreshold);

		Option gapThreshold = Option.builder("gt").longOpt("gapThreshold").argName("threshold").hasArg().required(false)
				.desc("Specify maximal number of missing concepts for a language (recommended: not more than 50% of languages).")
				.build();
		options.addOption(gapThreshold);

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
			String outputFilePath = null;
			String coordinatesFilePath = null;

			Set<String> concepts = new TreeSet<String>();
			String conceptFilePath = null;
			int minConceptOccurrences = 0;

			double linkThreshold = 0;
			int gapThreshold = -1;

			boolean directionality = false;

			int numSamples = 1;
			boolean bootstrapping = false;
			boolean minimizeSize = false;
			boolean randomLinkProcessingOrder = false;

			if (cmd.hasOption("i")) {
				inputFilePath = cmd.getOptionValue("input");
				System.out.println("Reading isolectic sets from input file: " + inputFilePath);
			}

			if (cmd.hasOption("vo")) {
				outputFilePath = cmd.getOptionValue("visOutput");
				System.out
						.println("Will write semantic map in DOT format to output files with prefix " + outputFilePath);
			}

			if (cmd.hasOption("vc")) {
				coordinatesFilePath = cmd.getOptionValue("coordinates");
				System.out.println("DOT files will include coordinates specified in file " + coordinatesFilePath);
			}

			if (cmd.hasOption("b")) {
				System.out.println("Will use bootstrapping on the language level to derive a consensus map.");
				bootstrapping = true;
				numSamples = 1000;
			}

			if (cmd.hasOption("m")) {
				System.out.println("Will vary link processing order and output the map of minimal size.");
				if (bootstrapping) {
					System.out.println(
							"WARNING: combining bootstrap (-b) and minimization (-m) is an atypical use case!");
				}
				minimizeSize = true;
				randomLinkProcessingOrder = true;
				numSamples = 1000;
			}

			if (cmd.hasOption("r")) {
				System.out.println("Will vary link processing order to explore the space of possible maps.");
				if (minimizeSize) {
					System.out.println(
							"WARNING: randomization already happens due to minimization mode, the additional -r flag does not change anything!");
				}
				randomLinkProcessingOrder = true;
			}

			if (cmd.hasOption("d")) {
				System.out.println("Will apply arrow inference in order to derive a diachronic semantic map.");
				directionality = true;
			}

			if (cmd.hasOption("c")) {
				conceptFilePath = cmd.getOptionValue("concepts");
				System.out.println("Analysis is limited to concepts specified in file: " + conceptFilePath);
			}

			if (cmd.hasOption("lt")) {
				linkThreshold = Double.parseDouble(cmd.getOptionValue("linkThreshold"));
				System.out.println("Will require at least " + linkThreshold
						+ " colexifications for a link (to correct for noisy input data).");
			}

			if (cmd.hasOption("gt")) {
				gapThreshold = Integer.parseInt(cmd.getOptionValue("gapThreshold"));
				System.out.println("Will discard data from languages with more than " + gapThreshold
						+ " relevant concepts missing.");
			}

			Set<IsolecticArea> isolecticAreas = IsolecticAreaReader.loadFromFile(inputFilePath);

			// if no concept file was provided, select all concepts which occur in a certain
			// number of isolectic sets (default: 0, i.e. no filtering)
			if (conceptFilePath == null) {
				IsolecticAreaProcessing.filterConceptsByMinOccurrence(concepts, isolecticAreas, minConceptOccurrences);
			} else {
				concepts.addAll(ListReader.listFromFile(conceptFilePath));
			}

			// load coordinates from the specified file (-vo argument)
			Map<String, Point2D.Double> coordinates = new TreeMap<String, Point2D.Double>();
			if (coordinatesFilePath != null) {
				List<String[]> entries = ListReader.arrayFromTSV(coordinatesFilePath);
				for (String[] entry : entries) {
					if (entry.length < 3)
						continue;
					coordinates.put(entry[0],
							new Point2D.Double(Double.parseDouble(entry[1]), Double.parseDouble(entry[2])));
				}
			}

			// selected/filtered concepts are the variables for causal inference
			String[] varNames = createVarNames(concepts);

			List<Set<Set<Triple<String, String, String>>>> samplePartitions = null;
			if (gapThreshold == -1) {
				samplePartitions = IsolecticAreaProcessing.isolecticAreasToSamplePartitions(isolecticAreas);
			} else {
				samplePartitions = IsolecticAreaProcessing.isolecticAreasToCompleteSamplePartitions(isolecticAreas,
						concepts, gapThreshold);
			}
			System.err.println("Extracted isolectic sets from " + samplePartitions.size() + " languages.");

			CausalGraphSummary sampleSummary = new CausalGraphSummary(varNames);

			int minMapSize = Integer.MAX_VALUE;
			CausalGraph minimalMap = null;

			double[][] thresholds = new double[concepts.size()][concepts.size()];
			for (double[] thresholdRow : thresholds) {
				Arrays.fill(thresholdRow, linkThreshold);
			}

			for (int k = 0; k < numSamples; k++) {
				List<Set<Set<Triple<String, String, String>>>> sample = samplePartitions;
				if (bootstrapping) {
					sample = resamplePartitionsLanguageLevel(samplePartitions);
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
								semanticMap.addLink(var1, var2);
								semanticMap.putArrow(var1, var2, false);
							}
						}
					}
				}

				// conditional independence criterion defined by discrete unit flow
				// (implementing the connected component criterion for isolectic sets)
				PartialCorrelationDiscreteUnitFlow corrMeasure = new PartialCorrelationDiscreteUnitFlow(sample,
						semanticMap, varNames, thresholds, false);

				// apply v-structure criteria from PC algorithm (stable and conservative
				// variant)
				CausalArrowFinder<List<Set<Set<Triple<String, String, String>>>>> arrowFinder = new CausalArrowFinderPcDefault<List<Set<Set<Triple<String, String, String>>>>>(
						sample, varNames, true, true);

				// run PC* algorithm to derive the semantic map (not assuming acyclicity)
				PcStarAlgorithm pcInstance = new PcStarAlgorithm(corrMeasure, null, varNames, semanticMap,
						concepts.size(), true, true, false, randomLinkProcessingOrder);
				pcInstance.runSkeletonInference();
				if (directionality) {
					pcInstance.runDirectionalityInference();
				} else {
					semanticMap.convertCirclesToLines();
				}

				if (numSamples == 1) {
					System.out.println("\nRESULT:");
					System.out.println("=======\n");
					semanticMap.printInTextFormat();

					// generate DOT file for visualizing the output (if specified)
					if (outputFilePath != null) {
						PrintStream out = new PrintStream(new FileOutputStream(new File(outputFilePath + "-map.dot")));
						CausalGraphOutput.outputToDotFormat(semanticMap, out, coordinates, 50);
						out.flush();
						out.close();
					}
				} else {
					sampleSummary.addGraph(semanticMap);
				}

				if (minimizeSize) {
					int mapSize = semanticMap.listAllLinks().size();
					System.out.println("Map size: " + mapSize);
					if (mapSize < minMapSize) {
						System.out.println("Reached new smallest map size with " + mapSize + " links!");
						minMapSize = mapSize;
						minimalMap = semanticMap;
					}
				}
			}

			// print and output minimal map
			if (minimizeSize) {
				System.out.println("\nMINIMAL MAP (among " + numSamples + " runs)");
				System.out.println("==============================\n");
				minimalMap.printInTextFormat();
				if (outputFilePath != null) {
					PrintStream out = new PrintStream(
							new FileOutputStream(new File(outputFilePath + "-minimal-map.dot")));
					CausalGraphOutput.outputToDotFormat(minimalMap, out, coordinates, 50);
					out.flush();
					out.close();
				}
			}

			// print and output summary in case numSamples > 1
			if (numSamples > 1) {
				System.out.println("\nSEMANTIC MAP CONSENSUS (based on " + numSamples + " runs)");
				System.out.println("============================================\n");
				sampleSummary.printInTextFormat();
				if (outputFilePath != null) {
					PrintStream out = new PrintStream(
							new FileOutputStream(new File(outputFilePath + "-consensus.dot")));
					CausalGraphOutput.outputToDotFormat(sampleSummary, out, coordinates, 0.25, 50);
					out.flush();
					out.close();
				}
			}

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
