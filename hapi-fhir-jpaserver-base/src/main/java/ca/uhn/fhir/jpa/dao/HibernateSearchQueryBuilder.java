package ca.uhn.fhir.jpa.dao;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.search.HapiLuceneAnalysisConfigurer;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class HibernateSearchQueryBuilder {
	private static final Logger ourLog = LoggerFactory.getLogger(HibernateSearchQueryBuilder.class);

	final FhirContext myFhirContext;
	final SearchPredicateFactory myPredicateFactory;
	final BooleanPredicateClausesStep<?> myRootClause;

	public HibernateSearchQueryBuilder(FhirContext myFhirContext, BooleanPredicateClausesStep<?> myRootClause, SearchPredicateFactory myPredicateFactory) {
		this.myFhirContext = myFhirContext;
		this.myRootClause = myRootClause;
		this.myPredicateFactory = myPredicateFactory;
	}


	@Nonnull
	private Set<String> extractOrStringParams(List<? extends IQueryParameterType> nextAnd) {
		Set<String> terms = new HashSet<>();
		for (IQueryParameterType nextOr : nextAnd) {
			String nextValueTrimmed;
			if (nextOr instanceof StringParam) {
				StringParam nextOrString = (StringParam) nextOr;
				nextValueTrimmed = StringUtils.defaultString(nextOrString.getValue()).trim();
			} else if (nextOr instanceof TokenParam) {
				TokenParam nextOrToken = (TokenParam) nextOr;
				nextValueTrimmed = nextOrToken.getValue();
			} else {
				throw new IllegalArgumentException("Unsupported full-text param type: " + nextOr.getClass());
			}
			if (isNotBlank(nextValueTrimmed)) {
				terms.add(nextValueTrimmed);
			}
		}
		return terms;
	}


	/**
	 * Provide an OR wrapper around a list of predicates.
	 * Returns the sole predicate if it solo, or wrap as a bool/should for OR semantics.
	 *
	 * @param theOrList a list containing at least 1 predicate
	 * @return a predicate providing or-sematics over the list.
	 */
	private PredicateFinalStep orPredicateOrSingle(List<? extends PredicateFinalStep> theOrList) {
		PredicateFinalStep finalClause;
		if (theOrList.size() == 1) {
			finalClause = theOrList.get(0);
		} else {
			BooleanPredicateClausesStep<?> orClause = myPredicateFactory.bool();
			theOrList.forEach(orClause::should);
			finalClause = orClause;
		}
		return finalClause;
	}

	public void addTokenUnmodifiedSearch(String theSearchParamName, List<List<IQueryParameterType>> theAndOrTerms) {
		if (CollectionUtils.isEmpty(theAndOrTerms)) {
			return;
		}
		for (List<? extends IQueryParameterType> nextAnd : theAndOrTerms) {
			String indexFieldPrefix = "sp." + theSearchParamName + ".token";

			List<? extends PredicateFinalStep> clauses = nextAnd.stream().map(orTerm -> {
				// wip can this be untrue?
				TokenParam token = (TokenParam) orTerm;
				if (StringUtils.isBlank(token.getSystem())) {
					// bare value
					return myPredicateFactory.match().field(indexFieldPrefix + ".code").matching(token.getValue());
				} else if (StringUtils.isBlank(token.getValue())) {
					// system without value
					return myPredicateFactory.match().field(indexFieldPrefix + ".system").matching(token.getSystem());
				} else {
					// system + value
					return myPredicateFactory.match().field(indexFieldPrefix + ".code-system").matching(token.getValueAsQueryToken(this.myFhirContext));
				}
			}).collect(Collectors.toList());

			PredicateFinalStep finalClause = orPredicateOrSingle(clauses);
			myRootClause.must(finalClause);
		}

	}

	public void addStringTextSearch(String nextParam, List<List<IQueryParameterType>> tokenTextAndOrTerms) {
		if (CollectionUtils.isEmpty(tokenTextAndOrTerms)) {
			return;
		}
		String fieldName;
		// we store some as legacy direct-mapped hibernate search fields
		// wip mb maybe start indexing to sp._text.string.text too?
		switch (nextParam) {
			case Constants.PARAM_CONTENT:
				fieldName = "myContentText";
				break;
			case Constants.PARAM_TEXT:
				fieldName = "myNarrativeText";
				break;
			default:
				fieldName = "sp." + nextParam + ".string.text";
				break;
		}

		for (List<? extends IQueryParameterType> nextAnd : tokenTextAndOrTerms) {
			Set<String> terms = extractOrStringParams(nextAnd);
			//fixme GGG: MB, did you mean for this to say >= 1?
			if (terms.size() == 1) {
				String query = terms.stream()
					.map(s -> "(" + s + ")")
					.collect(Collectors.joining(" OR "));
				myRootClause.must(myPredicateFactory
					.simpleQueryString()
					.field(fieldName)
					.boost(4.0f)
					.matching(query)
					.analyzer(HapiLuceneAnalysisConfigurer.STANDARD_ANALYZER)
					.defaultOperator(BooleanOperator.AND)); // term value may contain multiple tokens.  Require all of them to be present.
			} else if (terms.size() > 1) {
				String joinedTerms = StringUtils.join(terms, ' ');
				myRootClause.must(myPredicateFactory.match().field(fieldName).matching(joinedTerms));
			} else {
				ourLog.debug("No Terms found in query parameter {}", nextAnd);
			}
		}
	}
}
