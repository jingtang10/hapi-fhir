package ca.uhn.fhir.jpa.term;

/*-
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2021 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.apache.commons.lang3.StringUtils;

import static org.hl7.fhir.common.hapi.validation.support.ValidationConstants.LOINC_GENERIC_VALUESET_URL;
import static org.hl7.fhir.common.hapi.validation.support.ValidationConstants.LOINC_GENERIC_VALUESET_URL_PLUS_SLASH;
import static org.hl7.fhir.common.hapi.validation.support.ValidationConstants.LOINC_LOW;

public class TermReadSvcUtil {

	public static boolean mustReturnEmptyValueSet(String theUrl) {
		if (! theUrl.startsWith(LOINC_GENERIC_VALUESET_URL))   return true;

		if (! theUrl.startsWith(LOINC_GENERIC_VALUESET_URL_PLUS_SLASH)) {
			throw new InternalErrorException("Don't know how to extract ValueSet's ForcedId from url: " + theUrl);
		}

		String forcedId = theUrl.substring(LOINC_GENERIC_VALUESET_URL_PLUS_SLASH.length());
		return StringUtils.isBlank(forcedId);
	}


	public static boolean isLoincNotGenericUnversionedValueSet(String theUrl) {
		boolean isLoincCodeSystem = StringUtils.containsIgnoreCase(theUrl, LOINC_LOW);
		boolean isNoVersion = ! theUrl.contains("|");
		boolean isNotLoincGenericValueSet = ! theUrl.equals(LOINC_GENERIC_VALUESET_URL);

		return isLoincCodeSystem && isNoVersion && isNotLoincGenericValueSet;
	}


	public static boolean isLoincNotGenericUnversionedCodeSystem(String theUrl) {
		boolean isLoincCodeSystem = StringUtils.containsIgnoreCase(theUrl, LOINC_LOW);
		boolean isNoVersion = ! theUrl.contains("|");

		return isLoincCodeSystem && isNoVersion;
	}



}
