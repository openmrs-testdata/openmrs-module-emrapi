package org.openmrs.module.emrapi.db;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.module.emrapi.EmrApiContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EmrEncounterDAOComponentTest extends EmrApiContextSensitiveTest {

    @Autowired
    private EmrEncounterDAO emrEncounterDAO;

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ObsService obsService;

    @Autowired
    private EncounterService encounterService;

    @Before
    public void beforeAllTests() throws Exception {
        executeDataSet("emrEncounterDAOComponentTestDataset.xml");
    }

    @Test
    public void getEncountersByObsValueText_shouldFetchEncounterByObsTextValue() {
        Concept concept = conceptService.getConcept(19);
        List<Encounter> encounters = emrEncounterDAO.getEncountersByObsValueText(concept, "some test value", null, false);
        assertThat(encounters.size(), is(1));
        assertThat(encounters.get(0).getId(),is(1000));
    }

    @Test
    public void getEncountersByObsValueText_shouldReturnEmptyListIfNoObsWithThatValue() {
        Concept concept = conceptService.getConcept(19);
        List<Encounter> encounters = emrEncounterDAO.getEncountersByObsValueText(concept, "some bogus value", null, false);
        assertThat(encounters.size(), is(0));
    }

    @Test
    public void getEncountersByObsValueText_shouldReturnEmptyListIfNoObsWithThatValueForSelectedConcept() {
        Concept concept = conceptService.getConcept(18);  // not the concept that has the PB and J obs
        List<Encounter> encounters = emrEncounterDAO.getEncountersByObsValueText(concept, "some test value", null, false);
        assertThat(encounters.size(), is(0));
    }

    @Test
    public void getEncountersByObsValueText_shouldFindMatchEvenIfNoConceptSpecified() {
        List<Encounter> encounters = emrEncounterDAO.getEncountersByObsValueText(null, "some test value", null, false);
        assertThat(encounters.size(), is(1));
        assertThat(encounters.get(0).getId(),is(1000));
    }

    @Test
    public void getEncountersByObsValueText_shouldIncludeVoidedObsIfIncludeVoidedTrue() {
        List<Encounter> encounters = emrEncounterDAO.getEncountersByObsValueText(null, "some test value", null, true);
        assertThat(encounters.size(), is(2));
    }

    @Test
    public void getEncountersByObsValueText_shouldExcludeEncountersIfNotOfProperType() {
        EncounterType encounterType = encounterService.getEncounterType(1);
        List<Encounter> encounters = emrEncounterDAO.getEncountersByObsValueText(null, "some test value", encounterType, false);
        assertThat(encounters.size(), is(0));
    }

    @Test
    public void getEncountersByObsValueText_shouldIncludeEncountersOfProperType() {
        EncounterType encounterType = encounterService.getEncounterType(2);
        List<Encounter> encounters = emrEncounterDAO.getEncountersByObsValueText(null, "some test value", encounterType, false);
        assertThat(encounters.size(), is(1));
        assertThat(encounters.get(0).getId(),is(1000));
    }

    @Test
    public void getEncountersByObsValueText_shouldNotReturnTheSameEncounterTwice() {
        List<Encounter> encounters = emrEncounterDAO.getEncountersByObsValueText(null, "duplicate", null, false);
        assertThat(encounters.size(), is(1));
        assertThat(encounters.get(0).getId(),is(1000));
    }

    @Test
    public void getEncountersByObsValueText_shouldCorrectlyIncludePatient() {
        Concept concept = conceptService.getConcept(19);
        Patient patient = patientService.getPatient(7);
        List<Encounter> encounters = emrEncounterDAO.getEncountersByObsValueText(patient, concept, "some test value", null, false);
        assertThat(encounters.size(), is(1));
        assertThat(encounters.get(0).getId(),is(1000));
    }

    @Test
    public void getEncountersByObsValueText_shouldCorrectlyExcludePatient() {
        Concept concept = conceptService.getConcept(19);
        Patient patient = patientService.getPatient(6);
        List<Encounter> encounters = emrEncounterDAO.getEncountersByObsValueText(patient, concept, "some test value", null, false);
        assertThat(encounters.size(), is(0));
    }
}
