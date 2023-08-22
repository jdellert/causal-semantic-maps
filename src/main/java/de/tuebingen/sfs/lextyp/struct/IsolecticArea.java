package de.tuebingen.sfs.lextyp.struct;

import java.util.Set;

public class IsolecticArea 
{
	String lang;
	String lemma;
	Set<String> concepts;
	
	public IsolecticArea(String lang, String lemma, Set<String> concepts)
	{
		this.lang = lang;
		this.lemma = lemma;
		this.concepts = concepts;
	}
	
	public String getLang()
	{
		return lang;
	}
	
	public String getLemma()
	{
		return lemma;
	}
	
	public Set<String> getConcepts()
	{
		return concepts;
	}
	
	public int hashCode()
	{
		return lang.hashCode() * lemma.hashCode();
	}
	
	public boolean equals(Object o)
	{
		if (o instanceof IsolecticArea)
		{
			return equals((IsolecticArea) o);
		}
		return false;
	}
	
	public boolean equals(IsolecticArea otherArea)
	{
		return lang.equals(otherArea.getLang()) && lemma.equals(otherArea.getLemma())
				&& concepts.containsAll(otherArea.getConcepts()) && otherArea.getConcepts().containsAll(concepts);
	}
	
	public String toString()
	{
		return lang + "\t" + lemma + "\t" + concepts;
	}
}
