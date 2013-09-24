/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.emrapi.encounter.matcher;

import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Visit;
import org.openmrs.module.emrapi.encounter.EncounterParameters;

/**
 * Find {@link org.openmrs.Encounter} from a {@link org.openmrs.Visit} by matching {@link org.openmrs.EncounterType}.
 */
public class DefaultEncounterMatcher implements BaseEncounterMatcher {

    @Override
    public Encounter findEncounter(Visit visit, EncounterParameters encounterParameters) {
        EncounterType encounterType = encounterParameters.getEncounterType();

        if (encounterType == null){
            throw new IllegalArgumentException("Encounter Type not found");
        }

        for (Encounter encounter : visit.getEncounters()) {
            if (encounterType.equals(encounter.getEncounterType())) {
                return encounter;
            }
        }
        return null;
    }
}
