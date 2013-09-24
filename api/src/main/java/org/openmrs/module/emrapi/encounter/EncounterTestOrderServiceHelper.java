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
package org.openmrs.module.emrapi.encounter;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.ConceptService;
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction;
import org.openmrs.module.emrapi.encounter.exception.ConceptNotFoundException;

import java.util.List;

/**
 * Add/update/delete a {@link org.openmrs.TestOrder} on an {@link org.openmrs.Encounter}.
 */
public class EncounterTestOrderServiceHelper {

    private ConceptService conceptService;

    public EncounterTestOrderServiceHelper(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    public void update(Encounter encounter, List<EncounterTransaction.TestOrder> testOrders) {
        for (EncounterTransaction.TestOrder testOrder : testOrders) {

            Order order = getMatchingOrder(encounter, testOrder);

            if (testOrder.isVoided()) {
                order.setVoided(true);
                order.setVoidReason(testOrder.getVoidReason());
                return;
            }

            if (order == null) {
                order = new org.openmrs.TestOrder();
                order.setEncounter(encounter);
                order.setPatient(encounter.getPatient());
                encounter.addOrder(order);
            }

            Concept newConcept = conceptService.getConceptByUuid(testOrder.getConceptUuid());
            if (newConcept == null) {
                throw new ConceptNotFoundException("Test order concept does not exist" + testOrder.getConceptUuid());
            }
            order.setConcept(newConcept);
            order.setInstructions(testOrder.getInstructions());
        }
    }

    private Order getMatchingOrder(Encounter encounter, EncounterTransaction.TestOrder testOrder) {
        for (Order o : encounter.getOrders()) {
            if (StringUtils.equals(o.getUuid(), testOrder.getUuid())) {
                return o;
            }
        }
        return null;
    }
}