package de.tuebingen.sfs.lextyp.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import de.tuebingen.sfs.lextyp.struct.IsolecticArea;
import de.tuebingen.sfs.util.struct.ComparableTriple;
import de.tuebingen.sfs.util.struct.Triple;

public class IsolecticAreaProcessing {
	
	public static Map<String, Integer> countConcepts(Set<IsolecticArea> isolecticAreas) {
		Map<String, Integer> conceptCounts = new TreeMap<String, Integer>();
		for (IsolecticArea area : isolecticAreas) {
			for (String concept : area.getConcepts()) {
				Integer conceptCount = conceptCounts.get(concept);
				if (conceptCount == null)
					conceptCount = 1;
				conceptCounts.put(concept, conceptCount + 1);
			}
		}
		return conceptCounts;
	}

	public static void filterConceptsByMinOccurrence(Set<String> concepts, Set<IsolecticArea> isolecticAreas,
			int minConceptOccurrences) {
		Map<String, Integer> conceptCounts = countConcepts(isolecticAreas);
		for (Entry<String, Integer> count : conceptCounts.entrySet()) {
			if (count.getValue() >= minConceptOccurrences) {
				concepts.add(count.getKey());
			}
		}
	}
	
	public static List<Set<Set<Triple<String, String, String>>>> isolecticAreasToSamplePartitions(Set<IsolecticArea> isolecticAreas){
		return isolecticAreasToSamplePartitions(isolecticAreas, null);
	}
	
	public static List<Set<Set<Triple<String, String, String>>>> isolecticAreasToSamplePartitions(Set<IsolecticArea> isolecticAreas, List<String> langIDs)
	{
		if (langIDs != null)
		{
	        Map<String,Set<Set<Triple<String, String, String>>>> samplePartitionsPerLang = new TreeMap<String,Set<Set<Triple<String, String, String>>>>();
	        for (String lang : langIDs)
	        {
	        	samplePartitionsPerLang.put(lang, new HashSet<Set<Triple<String, String, String>>>());
	        }
			
	        for (IsolecticArea area : isolecticAreas)
	        {
	        	Set<Triple<String, String, String>> pairSet = new TreeSet<Triple<String, String, String>>();
	        	for (String concept : area.getConcepts())
	        	{
	        		pairSet.add(new ComparableTriple<String,String, String>(concept, concept, concept));
	        	}
				samplePartitionsPerLang.get(area.getLang()).add(pairSet);
			}
	        
	        List<Set<Set<Triple<String, String, String>>>> samplePartitions = new ArrayList<Set<Set<Triple<String, String, String>>>>(langIDs.size());
			for (String lang : langIDs)
			{
				samplePartitions.add(samplePartitionsPerLang.get(lang));
			}
			return samplePartitions;
		}
		else
		{
			Map<String,Set<Set<Triple<String, String, String>>>> samplePartitionsPerLang = new TreeMap<String,Set<Set<Triple<String, String, String>>>>();

	        for (IsolecticArea area : isolecticAreas)
	        {
	        	Set<Set<Triple<String, String, String>>> samplePartitionForLang = samplePartitionsPerLang.get(area.getLang());
	        	if (samplePartitionForLang == null) {
	        		samplePartitionForLang = new HashSet<Set<Triple<String, String, String>>>();
	        		samplePartitionsPerLang.put(area.getLang(), samplePartitionForLang);
	        	}
	        	
	        	Set<Triple<String, String, String>> tripleSet = new TreeSet<Triple<String, String, String>>();
	        	for (String concept : area.getConcepts())
	        	{
	        		tripleSet.add(new ComparableTriple<String,String, String>(concept, area.getLang(), area.getLemma()));
	        	}

				samplePartitionForLang.add(tripleSet);
			}
			List<Set<Set<Triple<String, String, String>>>> samplePartitions = new ArrayList<Set<Set<Triple<String, String, String>>>>(samplePartitionsPerLang.values());
	        return samplePartitions;
		}
	}
	
	public static List<Set<Set<Triple<String, String, String>>>> isolecticAreasToCompleteSamplePartitions(Set<IsolecticArea> isolecticAreas, Set<String> relevantConcepts) {
		return isolecticAreasToCompleteSamplePartitions(isolecticAreas, relevantConcepts, 0);
	}
	
	public static List<Set<Set<Triple<String, String, String>>>> isolecticAreasToCompleteSamplePartitions(Set<IsolecticArea> isolecticAreas, Set<String> relevantConcepts, int maxNumGapsPerLang) {
		Map<String,Set<Set<Triple<String, String, String>>>> samplePartitionsPerLang = new TreeMap<String,Set<Set<Triple<String, String, String>>>>();
		Map<String,Set<String>> coveredConceptsPerLang = new TreeMap<String,Set<String>>();
        for (IsolecticArea area : isolecticAreas)
        {
        	Set<Set<Triple<String, String, String>>> samplePartitionForLang = samplePartitionsPerLang.get(area.getLang());
        	if (samplePartitionForLang == null) {
        		samplePartitionForLang = new HashSet<Set<Triple<String, String, String>>>();
        		samplePartitionsPerLang.put(area.getLang(), samplePartitionForLang);
        		coveredConceptsPerLang.put(area.getLang(), new TreeSet<String>());
        	}
        	
        	Set<Triple<String, String, String>> tripleSet = new TreeSet<Triple<String, String, String>>();
        	for (String concept : area.getConcepts())
        	{
        		tripleSet.add(new ComparableTriple<String,String, String>(concept, area.getLang(), area.getLemma()));
        		coveredConceptsPerLang.get(area.getLang()).add(concept);
        	}

			samplePartitionForLang.add(tripleSet);
		}
		List<Set<Set<Triple<String, String, String>>>> samplePartitions = new ArrayList<Set<Set<Triple<String, String, String>>>>();
		for (String lang : samplePartitionsPerLang.keySet()) {
			Set<String> coveredConcepts = coveredConceptsPerLang.get(lang);
			Set<String> conceptsWithoutData = new TreeSet<String>();
			for (String relevantConcept : relevantConcepts) {
				if (!coveredConcepts.contains(relevantConcept)) {
					conceptsWithoutData.add(relevantConcept);
				}
			}
			if (conceptsWithoutData.size() == 0) {
				System.out.println("  " + lang + " data included, all concepts are covered.");
				samplePartitions.add(samplePartitionsPerLang.get(lang));
			} else if (conceptsWithoutData.size() <= maxNumGapsPerLang) {
				System.out.println("  " + lang + " data included, number of gaps is " + conceptsWithoutData.size());
				samplePartitions.add(samplePartitionsPerLang.get(lang));
			} else if (conceptsWithoutData.size() <= 5) {
				System.out.println("  " + lang + " data discarded due to gaps for " + conceptsWithoutData.size() + " concepts: " + String.join(", ", conceptsWithoutData));
			} else {
				System.out.println("  " + lang + " data discarded due to gaps for " + conceptsWithoutData.size() + " concepts.");
			}
		}
		return samplePartitions;
	}
}
