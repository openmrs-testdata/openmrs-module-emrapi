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

import org.apache.commons.lang.time.DateUtils;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction;
import org.openmrs.module.emrapi.encounter.exception.EncounterMatcherNotFoundException;
import org.openmrs.module.emrapi.encounter.matcher.BaseEncounterMatcher;
import org.openmrs.module.emrapi.encounter.matcher.DefaultEncounterMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.openmrs.module.emrapi.utils.GeneralUtils.getCurrentDateIfNull;

@Transactional
@Component (value = "emrEncounterServiceTarget")
public class EmrEncounterServiceImpl extends BaseOpenmrsService implements EmrEncounterService {

    private final EncounterTransactionMapper encounterTransactionMapper;
    private PatientService patientService;
    private VisitService visitService;
    private EncounterService encounterService;
    private EncounterObservationServiceHelper encounterObservationServiceHelper;
    private EncounterDispositionServiceHelper encounterDispositionServiceHelper;
    private EncounterProviderServiceHelper encounterProviderServiceHelper;
    private EmrOrderService emrOrderService;
    private LocationService locationService;
    private ProviderService providerService;
    private AdministrationService administrationService;

    private Map<String, BaseEncounterMatcher> encounterMatcherMap = new HashMap<String, BaseEncounterMatcher>();

    @Autowired(required = false)
    public EmrEncounterServiceImpl(PatientService patientService, VisitService visitService, EncounterService encounterService,
                                   LocationService locationService, ProviderService providerService,
                                   @Qualifier(value = "adminService")AdministrationService administrationService,
                                   EncounterObservationServiceHelper encounterObservationServiceHelper,
                                   EncounterDispositionServiceHelper encounterDispositionServiceHelper,
                                   EncounterTransactionMapper encounterTransactionMapper,
                                   EncounterProviderServiceHelper encounterProviderServiceHelper,
                                   EmrOrderService emrOrderService) {
        this.patientService = patientService;
        this.visitService = visitService;
        this.encounterService = encounterService;
        this.encounterObservationServiceHelper = encounterObservationServiceHelper;
        this.locationService = locationService;
        this.providerService = providerService;
        this.administrationService = administrationService;
        this.encounterDispositionServiceHelper = encounterDispositionServiceHelper;
        this.encounterTransactionMapper = encounterTransactionMapper;
        this.encounterProviderServiceHelper = encounterProviderServiceHelper;
        this.emrOrderService = emrOrderService;
    }

    @Autowired(required = false)
    public EmrEncounterServiceImpl(PatientService patientService, VisitService visitService, EncounterService encounterService,
                                   LocationService locationService, ProviderService providerService,
                                   @Qualifier(value = "adminService")AdministrationService administrationService,
                                   EncounterObservationServiceHelper encounterObservationServiceHelper,
                                   EncounterDispositionServiceHelper encounterDispositionServiceHelper,
                                   EncounterTransactionMapper encounterTransactionMapper,
                                   EncounterProviderServiceHelper encounterProviderServiceHelper) {
        this(patientService, visitService, encounterService, locationService, providerService, administrationService, encounterObservationServiceHelper, encounterDispositionServiceHelper, encounterTransactionMapper, encounterProviderServiceHelper, null);
    }

    @Override
    public void onStartup() {
        try {
            super.onStartup();
            List<BaseEncounterMatcher> encounterMatchers = Context.getRegisteredComponents(BaseEncounterMatcher.class);
            for (BaseEncounterMatcher encounterMatcher : encounterMatchers) {
                encounterMatcherMap.put(encounterMatcher.getClass().getCanonicalName(), encounterMatcher);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public EncounterTransaction save(EncounterTransaction encounterTransaction) {
        Patient patient = patientService.getPatientByUuid(encounterTransaction.getPatientUuid());
        Visit visit = findOrCreateVisit(encounterTransaction, patient);
        Encounter encounter = findOrCreateEncounter(encounterTransaction, patient, visit);

        encounterObservationServiceHelper.update(encounter, encounterTransaction.getObservations());
        encounterObservationServiceHelper.updateDiagnoses(encounter, encounterTransaction.getDiagnoses());
        encounterDispositionServiceHelper.update(encounter, encounterTransaction.getDisposition());
        encounterProviderServiceHelper.update(encounter, encounterTransaction.getProviders());

        visitService.saveVisit(visit);

        if (emrOrderService != null) {
            emrOrderService.save(encounterTransaction.getDrugOrders(), encounter);
        }

        return new EncounterTransaction(visit.getUuid(), encounter.getUuid());
    }

    @Override
    public EncounterTransaction getActiveEncounter(ActiveEncounterParameters activeEncounterParameters) {
        Patient patient = patientService.getPatientByUuid(activeEncounterParameters.getPatientUuid());
        EncounterType encounterType = encounterService.getEncounterTypeByUuid(activeEncounterParameters.getEncounterTypeUuid());

        Provider provider = null;
        HashSet<Provider> providers = new HashSet<Provider>();
        if(activeEncounterParameters.getProviderUuid() != null)
            provider = providerService.getProviderByUuid(activeEncounterParameters.getProviderUuid());
            providers.add(provider);

        EncounterParameters encounterParameters = EncounterParameters.instance().
                            setPatient(patient).setEncounterType(encounterType).setProviders(providers);

        Visit visit = getActiveVisit(patient);

        if (visit == null) {
            return new EncounterTransaction();
        }

        Encounter encounter = findEncounter(visit, encounterParameters);

        if (encounter == null){
            encounter = newEncounter(visit, encounterParameters);
        }

        return encounterTransactionMapper.map(encounter, activeEncounterParameters.getIncludeAll());
    }

    private Encounter newEncounter(Visit visit, EncounterParameters encounterParameters) {
        Encounter encounter;
        encounter = new Encounter();
        encounter.setVisit(visit);
        encounter.setPatient(encounterParameters.getPatient());
        encounter.setEncounterType(encounterParameters.getEncounterType());
        return encounter;
    }

    @Override
    public List<EncounterTransaction> find(EncounterSearchParameters encounterSearchParameters) {
        Visit visit = visitService.getVisitByUuid(encounterSearchParameters.getVisitUuid());
        if (visit == null) return new ArrayList<EncounterTransaction>();

        return getEncounterTransactions(
                getEncountersForDate(encounterSearchParameters.getEncounterDateAsDate(), visit),
                encounterSearchParameters.getIncludeAll());
    }

    private List<EncounterTransaction> getEncounterTransactions(List<Encounter> encounters, boolean includeAll) {
        List<EncounterTransaction> encounterTransactions = new ArrayList<EncounterTransaction>();
        for (Encounter encounter : encounters) {
            encounterTransactions.add(encounterTransactionMapper.map(encounter, includeAll));
        }
        return encounterTransactions;
    }

    private ArrayList<Encounter> getEncountersForDate(Date encounterDate, Visit visit) {
        if (encounterDate == null) return new ArrayList<Encounter>(visit.getEncounters());
        ArrayList<Encounter> encounters = new ArrayList<Encounter>();
        for (Encounter encounter : visit.getEncounters()) {
            if (DateUtils.isSameDay(encounter.getEncounterDatetime(), encounterDate)) {
                encounters.add(encounter);
            }
        }
        return encounters;
    }

    private Visit getActiveVisit(Patient patient) {
        List<Visit> activeVisitsByPatient = visitService.getActiveVisitsByPatient(patient);
        return activeVisitsByPatient != null && !activeVisitsByPatient.isEmpty() ? activeVisitsByPatient.get(0) : null;
    }

    private Encounter findOrCreateEncounter(EncounterTransaction encounterTransaction, Patient patient, Visit visit) {

        EncounterType encounterType = encounterService.getEncounterTypeByUuid(encounterTransaction.getEncounterTypeUuid());
        Location location = locationService.getLocationByUuid(encounterTransaction.getLocationUuid());
        Date encounterDateTime = getCurrentDateIfNull(encounterTransaction.getEncounterDateTime());
        Set<Provider> providers = getProviders(encounterTransaction.getProviders());

        EncounterParameters encounterParameters = EncounterParameters.instance()
                .setLocation(location).setEncounterType(encounterType)
                .setProviders(providers).setEncounterDateTime(encounterDateTime)
                .setPatient(patient);

        Encounter encounter = findEncounter(visit, encounterParameters);

        if (encounter == null) {
            encounter = new Encounter();
            encounter.setPatient(patient);
            encounter.setEncounterType(encounterType);
            String encounterUuid = encounterTransaction.getEncounterUuid() == null ?UUID.randomUUID().toString():encounterTransaction.getEncounterUuid();
            encounter.setUuid(encounterUuid);
            encounter.setObs(new HashSet<Obs>());
            encounter.setEncounterDatetime(encounterDateTime);
            visit.addEncounter(encounter);
        }
        return encounter;
    }

    private Encounter findEncounter(Visit visit, EncounterParameters encounterParameters) {

        String matcherClass = administrationService.getGlobalProperty("emr.encounterMatcher");
        BaseEncounterMatcher encounterMatcher = isNotEmpty(matcherClass)? encounterMatcherMap.get(matcherClass) : new DefaultEncounterMatcher();
        if (encounterMatcher == null) {
            throw new EncounterMatcherNotFoundException();
        }
        return encounterMatcher.findEncounter(visit, encounterParameters);
    }

    private Set<Provider> getProviders(Set<EncounterTransaction.Provider> encounteProviders) {

        if (encounteProviders == null){
            return Collections.EMPTY_SET;
        }

        Set<Provider> providers = new HashSet<Provider>();

        for (EncounterTransaction.Provider encounterProvider : encounteProviders) {
            Provider provider = providerService.getProviderByUuid(encounterProvider.getUuid());
            providers.add(provider);
        }
        return providers;
    }

    private Visit findOrCreateVisit(EncounterTransaction encounterTransaction, Patient patient) {

        // return the visit that was explicitly asked for in the EncounterTransaction Object
        if(encounterTransaction.getVisitUuid() != null && !encounterTransaction.getVisitUuid().isEmpty()){
            return visitService.getVisitByUuid(encounterTransaction.getVisitUuid());
        }

        Visit activeVisit = getActiveVisit(patient);
        if (activeVisit != null){
            return activeVisit;
        }

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setVisitType(visitService.getVisitTypeByUuid(encounterTransaction.getVisitTypeUuid()));
        visit.setStartDatetime(getCurrentDateIfNull(encounterTransaction.getEncounterDateTime()));
        visit.setEncounters(new HashSet<Encounter>());
        visit.setUuid(UUID.randomUUID().toString());
        return visit;
    }

}
