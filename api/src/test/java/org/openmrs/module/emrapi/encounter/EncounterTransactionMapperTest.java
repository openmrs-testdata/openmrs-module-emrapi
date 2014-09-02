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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.Encounter;
import org.openmrs.module.emrapi.encounter.builder.EncounterBuilder;
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction;

import static org.mockito.MockitoAnnotations.initMocks;

public class EncounterTransactionMapperTest {
    @Mock
    private EncounterObservationsMapper encounterObservationsMapper;
    @Mock
    private EncounterProviderMapper encounterProviderMapper;
    @Mock
    private EmrOrderService emrOrderService;

    private EncounterTransactionMapper encounterTransactionMapper;

    @Before
    public void setUp() {
        initMocks(this);
        encounterTransactionMapper = new EncounterTransactionMapper(encounterObservationsMapper, encounterProviderMapper, emrOrderService);
    }

    @Test
    public void shouldMap() throws Exception {
        Encounter encounter = new EncounterBuilder().build();
        boolean includeAll = false;

        EncounterTransaction encounterTransaction = encounterTransactionMapper.map(encounter, includeAll);

        Assert.assertEquals(encounter.getUuid(), encounterTransaction.getEncounterUuid());
        Assert.assertEquals(encounter.getVisit().getUuid(), encounterTransaction.getVisitUuid());
        Assert.assertEquals(encounter.getPatient().getUuid(), encounterTransaction.getPatientUuid());
        Assert.assertEquals(encounter.getEncounterType().getUuid(), encounterTransaction.getEncounterTypeUuid());
        Assert.assertEquals(encounter.getLocation().getUuid(), encounterTransaction.getLocationUuid());
        Assert.assertEquals(encounter.getVisit().getVisitType().getUuid(), encounterTransaction.getVisitTypeUuid());
    }
}
