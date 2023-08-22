package de.tuebingen.sfs.lextyp.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.tuebingen.sfs.lextyp.struct.IsolecticArea;
import de.tuebingen.sfs.util.io.ListReader;

public class IsolecticAreaReader {
	
	public static Set<IsolecticArea> loadFromFile(String fileName)
			throws FileNotFoundException, IOException {
		return loadFromFileWithSubstitutions(fileName, null);
	}

	public static Set<IsolecticArea> loadFromFileWithSubstitutions(String fileName, Map<String, String> substitutions)
			throws FileNotFoundException, IOException {
		Set<IsolecticArea> areas = new HashSet<IsolecticArea>();
		List<String[]> areaLines = ListReader.arrayFromTSV(fileName);
		for (String[] areaLine : areaLines) {
			if (areaLine.length > 2 && areaLine[2].length() > 2)
			{
				String lang = areaLine[0];
				String lemma = areaLine[1];
				String conceptsList = areaLine[2].substring(1,areaLine[2].length() - 1);
				Set<String> concepts = new TreeSet<String>();
				for (String concept : conceptsList.split(", ")) {
					if (substitutions != null && substitutions.containsKey(concept)) {
						concept = substitutions.get(concept);
					}
					concepts.add(concept);
				}
				concepts.remove("");
				IsolecticArea area = new IsolecticArea(lang, lemma, concepts);
				areas.add(area);
			}
		}
		return areas;
	}
}
