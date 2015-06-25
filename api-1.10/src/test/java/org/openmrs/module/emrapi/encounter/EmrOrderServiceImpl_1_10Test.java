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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.TestOrder;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.module.emrapi.encounter.builder.DrugOrderBuilder;
import org.openmrs.module.emrapi.encounter.builder.TestOrderBuilder;
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction;
import org.openmrs.module.emrapi.encounter.mapper.OpenMRSDrugOrderMapper;
import org.openmrs.module.emrapi.encounter.mapper.OpenMRSTestOrderMapper;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

public class EmrOrderServiceImpl_1_10Test {

    @Mock
    private EncounterService encounterService;

    @Mock
    private OpenMRSDrugOrderMapper openMRSDrugOrderMapper;

    @Mock
    private OpenMRSTestOrderMapper openMRSTestOrderMapper;

    @Mock
    private OrderService orderService;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void shouldSaveNewDrugOrdersInTheSequenceOfOrdering() throws ParseException {
        EmrOrderServiceImpl_1_10 emrOrderService = new EmrOrderServiceImpl_1_10(openMRSDrugOrderMapper, encounterService, openMRSTestOrderMapper);
        EncounterTransaction.DrugOrder drugOrder1 = new DrugOrderBuilder().withDrugUuid("drug-uuid1").build();
        EncounterTransaction.DrugOrder drugOrder2 = new DrugOrderBuilder().withDrugUuid("drug-uuid2").build();
        DrugOrder mappedDrugOrder1 = new DrugOrder();
        DrugOrder mappedDrugOrder2 = new DrugOrder();
        Encounter encounter = new Encounter();
        when(openMRSDrugOrderMapper.map(drugOrder1, encounter)).thenReturn(mappedDrugOrder1);
        when(openMRSDrugOrderMapper.map(drugOrder2, encounter)).thenReturn(mappedDrugOrder2);

        emrOrderService.save(Arrays.asList(drugOrder1, drugOrder2), encounter);

        ArgumentCaptor<Encounter> encounterArgumentCaptor = ArgumentCaptor.forClass(Encounter.class);
        verify(encounterService).saveEncounter(encounterArgumentCaptor.capture());
        Encounter savedEncounter = encounterArgumentCaptor.getValue();
        ArrayList<Order> savedOrders = new ArrayList<Order>(savedEncounter.getOrders());
        assertThat(savedOrders.size(), is(2));
        assertThat((DrugOrder)savedOrders.get(0), is(sameInstance(mappedDrugOrder1)));
        assertThat((DrugOrder)savedOrders.get(1), is(sameInstance(mappedDrugOrder2)));
    }

    @Test
    public void shouldSaveNewDrugOrdersInTheSequenceOfOrderingToAnEncounterWithExistingOrders() throws ParseException {
        EmrOrderServiceImpl_1_10 emrOrderService = new EmrOrderServiceImpl_1_10(openMRSDrugOrderMapper, encounterService, openMRSTestOrderMapper);
        EncounterTransaction.DrugOrder drugOrder3 = new DrugOrderBuilder().withDrugUuid("drug-uuid3").build();
        EncounterTransaction.DrugOrder drugOrder4 = new DrugOrderBuilder().withDrugUuid("drug-uuid4").build();
        DrugOrder existingDrugOrder1 = new DrugOrder();
        DrugOrder existingDrugOrder2 = new DrugOrder();
        DrugOrder mappedDrugOrder3 = new DrugOrder();
        DrugOrder mappedDrugOrder4 = new DrugOrder();
        Encounter encounter = new Encounter();
        encounter.addOrder(existingDrugOrder1);
        encounter.addOrder(existingDrugOrder2);
        when(openMRSDrugOrderMapper.map(drugOrder3, encounter)).thenReturn(mappedDrugOrder3);
        when(openMRSDrugOrderMapper.map(drugOrder4, encounter)).thenReturn(mappedDrugOrder4);

        emrOrderService.save(Arrays.asList(drugOrder3, drugOrder4), encounter);

        ArgumentCaptor<Encounter> encounterArgumentCaptor = ArgumentCaptor.forClass(Encounter.class);
        verify(encounterService).saveEncounter(encounterArgumentCaptor.capture());
        Encounter savedEncounter = encounterArgumentCaptor.getValue();
        ArrayList<Order> savedOrders = new ArrayList<Order>(savedEncounter.getOrders());
        assertThat(savedOrders.size(), is(4));
        assertThat((DrugOrder)savedOrders.get(2), is(sameInstance(mappedDrugOrder3)));
        assertThat((DrugOrder)savedOrders.get(3), is(sameInstance(mappedDrugOrder4)));
    }

    @Test
    public void shouldSaveTestOrders() throws ParseException {
        EmrOrderServiceImpl_1_10 emrOrderService = new EmrOrderServiceImpl_1_10(openMRSDrugOrderMapper, encounterService, openMRSTestOrderMapper);
        EncounterTransaction.TestOrder testOrder1 = new TestOrderBuilder().withConceptUuid("concept-uuid1").withComment("Comment").build();
        EncounterTransaction.TestOrder testOrder2 = new TestOrderBuilder().withConceptUuid("concept-uuid2").withComment("Comment").build();

        TestOrder mappedTestOrder1 = new TestOrder();
        Concept concept = new Concept();
        concept.setUuid("concept-uuid1");
        mappedTestOrder1.setConcept(concept);
        mappedTestOrder1.setCommentToFulfiller("Comment");

        TestOrder mappedTestOrder2 = new TestOrder();
        concept = new Concept();
        concept.setUuid("concept-uuid2");
        mappedTestOrder2.setConcept(concept);
        mappedTestOrder2.setCommentToFulfiller("Comment");

        Encounter encounter = new Encounter();
        when(openMRSTestOrderMapper.map(testOrder1,encounter)).thenReturn(mappedTestOrder1);
        when(openMRSTestOrderMapper.map(testOrder2,encounter)).thenReturn(mappedTestOrder2);

        emrOrderService.saveTestOrders(Arrays.asList(testOrder1, testOrder2), encounter);

        ArgumentCaptor<Encounter> encounterArgumentCaptor = ArgumentCaptor.forClass(Encounter.class);
        verify(encounterService).saveEncounter(encounterArgumentCaptor.capture());
        Encounter savedEncounter = encounterArgumentCaptor.getValue();
        ArrayList<Order> savedOrders = new ArrayList<Order>(savedEncounter.getOrders());
        assertThat(savedOrders.size(), is(2));
        assertTrue(existsInOrdersList(mappedTestOrder1, savedOrders));
        assertTrue(existsInOrdersList(mappedTestOrder2, savedOrders));
    }

    @Test
    public void shouldSaveTestOrdersToEncounterWithExistingOrders() throws ParseException {
        EmrOrderServiceImpl_1_10 emrOrderService = new EmrOrderServiceImpl_1_10(openMRSDrugOrderMapper, encounterService, openMRSTestOrderMapper);
        EncounterTransaction.TestOrder testOrder1 = new TestOrderBuilder().withConceptUuid("concept-uuid1").withComment("Comment").build();
        EncounterTransaction.TestOrder testOrder2 = new TestOrderBuilder().withConceptUuid("concept-uuid2").withComment("Comment").build();

        TestOrder mappedTestOrder1 = new TestOrder();
        Concept concept = new Concept();
        concept.setUuid("concept-uuid1");
        mappedTestOrder1.setConcept(concept);
        mappedTestOrder1.setCommentToFulfiller("Comment");


        TestOrder mappedTestOrder2 = new TestOrder();
        concept = new Concept();
        concept.setUuid("concept-uuid2");
        mappedTestOrder2.setConcept(concept);
        mappedTestOrder2.setCommentToFulfiller("Comment");


        TestOrder existingTestOrder1 = new TestOrder();
        TestOrder existingTestOrder2 = new TestOrder();

        Encounter encounter = new Encounter();
        encounter.addOrder(existingTestOrder1);
        encounter.addOrder(existingTestOrder2);

        when(openMRSTestOrderMapper.map(testOrder1,encounter)).thenReturn(mappedTestOrder1);
        when(openMRSTestOrderMapper.map(testOrder2,encounter)).thenReturn(mappedTestOrder2);

        emrOrderService.saveTestOrders(Arrays.asList(testOrder1, testOrder2), encounter);

        ArgumentCaptor<Encounter> encounterArgumentCaptor = ArgumentCaptor.forClass(Encounter.class);
        verify(encounterService).saveEncounter(encounterArgumentCaptor.capture());
        Encounter savedEncounter = encounterArgumentCaptor.getValue();
        ArrayList<Order> savedOrders = new ArrayList<Order>(savedEncounter.getOrders());
        assertThat(savedOrders.size(), is(4));
        assertTrue(existsInOrdersList(mappedTestOrder1, savedOrders));
        assertTrue(existsInOrdersList(mappedTestOrder2, savedOrders));
    }

    private boolean existsInOrdersList(TestOrder testOrder, ArrayList<Order> orderArrayList) {
        for(Order order : orderArrayList) {
            if(order.getConcept()!=null && order.getConcept().getUuid().equals(testOrder.getConcept().getUuid()) &&
                    order.getCommentToFulfiller().equals(testOrder.getCommentToFulfiller()))
                return true;
        }
        return false;
    }
}