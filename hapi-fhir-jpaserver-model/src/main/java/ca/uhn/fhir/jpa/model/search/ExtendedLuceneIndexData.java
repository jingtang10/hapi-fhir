package ca.uhn.fhir.jpa.model.search;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum;
import ca.uhn.fhir.rest.param.TokenParam;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects our lucene extended indexing data.
 *
 */
public class ExtendedLuceneIndexData {
	private static final Logger ourLog = LoggerFactory.getLogger(ExtendedLuceneIndexData.class);

	final FhirContext myFhirContext;
	final SetMultimap<String, String> mySearchParamStrings = HashMultimap.create();
	final SetMultimap<String, TokenParam> mySearchParamTokens = HashMultimap.create();

	// wip mb add the Resource - do we already have it as json somewhere?

	public ExtendedLuceneIndexData(FhirContext theFhirContext) {
		this.myFhirContext = theFhirContext;
	}

	public void writeIndexElements(DocumentElement theDocument) {
		HibernateSearchIndexWriter indexWriter = HibernateSearchIndexWriter.forRoot(myFhirContext, theDocument);

		// WIP Use RestSearchParameterTypeEnum to define templates.
		mySearchParamStrings.forEach(indexWriter::writeStringIndex);
		mySearchParamTokens.forEach(indexWriter::writeTokenIndex);
	}

	public void addStringIndexData(String theSpName, String theText) {
		mySearchParamStrings.put(theSpName, theText);
	}

	public void addTokenIndexData(String theSpName, String theSystem,  String theValue) {
		mySearchParamTokens.put(theSpName, new TokenParam(theSystem, theValue));
	}
}
