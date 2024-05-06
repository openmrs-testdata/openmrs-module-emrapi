package org.openmrs.module.emrapi.conditionslist.contract;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.emrapi.conditionslist.Condition.Status.INACTIVE;

import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emrapi.conditionslist.ConditionListConstants;
import org.openmrs.module.emrapi.conditionslist.DateConverter;
import org.openmrs.util.LocaleUtility;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {Context.class, LocaleUtility.class})
public class ConditionMapperTest {
	
	@Mock
	public PatientService patientService;
	
	@Mock
	public ConceptService conceptService;
	
	@Mock
	public AdministrationService administrationService;
	
	private ConditionMapper conditionMapper;
	
	@Before
	public void before() {
		initMocks(this);
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(LocaleUtility.class);
		when(Context.getConceptService()).thenReturn(conceptService);
		when(Context.getPatientService()).thenReturn(patientService);
		when(Context.getAdministrationService()).thenReturn(administrationService);
		when(Context.getLocale()).thenReturn(Locale.ENGLISH);
		
		String nonCodedUuid = "nonCodedUuid";
		Concept nonCodedConcept = new Concept();
		nonCodedConcept.setUuid(nonCodedUuid);
		when(administrationService.getGlobalProperty(ConditionListConstants.GLOBAL_PROPERTY_NON_CODED_UUID)).thenReturn(
				nonCodedUuid);
		when(conceptService.getConceptByUuid(nonCodedUuid)).thenReturn(nonCodedConcept);
		
		conditionMapper = new ConditionMapper();
	}
	
	@Test
	public void shouldMapOpenmrsConditionToContractCondition() throws Exception {
		String uuid = "13a1234-asdf23-ad23425as-sas90";
		Date today = new Date();
		String conceptUuid = "10924-1294124-1284u12-12841";
		String patientUuid = "13a1234-asdf23-ad2354-sas23";
		String additionalDetail = "some notes";
		String endReasonUuid = "end-reason-uuid-288a-asdf";
		
		org.openmrs.module.emrapi.conditionslist.Condition prevOpenmrsCondition = new org.openmrs.module.emrapi.conditionslist.Condition();
		org.openmrs.module.emrapi.conditionslist.Condition openmrsCondition = new org.openmrs.module.emrapi.conditionslist.Condition();
		openmrsCondition.setDateCreated(new Date());
		openmrsCondition.setPreviousCondition(prevOpenmrsCondition);
		openmrsCondition.setOnsetDate(today);
		openmrsCondition.setUuid(uuid);
		
		Concept concept = new Concept();
		concept.setFullySpecifiedName(new ConceptName("dog bite", Locale.ENGLISH));
		Concept endReason = new Concept();
		endReason.setFullySpecifiedName(new ConceptName("end", Locale.ENGLISH));
		endReason.setUuid(endReasonUuid);
		openmrsCondition.setEndReason(endReason);
		
		concept.setUuid(conceptUuid);
		Patient patient = new Patient();
		patient.setUuid(patientUuid);
		openmrsCondition.setConcept(concept);
		openmrsCondition.setPatient(patient);
		openmrsCondition.setStatus(INACTIVE);
		openmrsCondition.setAdditionalDetail(additionalDetail);
		
		User creator = new User();
		creator.setUsername("CREATOR");
		openmrsCondition.setCreator(creator);
		
		Condition condition = conditionMapper.map(openmrsCondition);
		
		assertEquals(uuid, condition.getUuid());
		assertEquals(prevOpenmrsCondition.getUuid(), condition.getPreviousConditionUuid());
		assertEquals(conceptUuid, condition.getConcept().getUuid());
		assertEquals(patientUuid, condition.getPatientUuid());
		assertEquals(openmrsCondition.getDateCreated(), condition.getDateCreated());
		assertEquals(today, DateConverter.deserialize(condition.getOnSetDate()));
		assertEquals(additionalDetail, condition.getAdditionalDetail());
		
		assertEquals(null, condition.getEndDate());
		assertEquals(endReasonUuid, condition.getEndReason().getUuid());
		assertEquals(INACTIVE, condition.getStatus());
		
		assertEquals("(CREATOR)", condition.getCreator());
	}
	
	@Test
	public void shouldMapContractConditionToOpenmrsCondition() throws Exception {
		String uuid = "13a1234-asdf23-ad23425as-sas90";
		Date today = new Date();
		String conceptUuid = "10924-1294124-1284u12-12841";
		String patientUuid = "13a1234-asdf23-ad2354-sas23";
		String additionalDetail = "some notes";
		String endReasonUuid = "end-reason-uuid-288a-asdf";
		
		Condition condition = new Condition();
		condition.setOnSetDate(DateConverter.serialize(today));
		condition.setUuid(uuid);
		
		Concept concept = new Concept();
		Concept endReason = new Concept();
		endReason.setUuid(endReasonUuid);
		condition.setEndReason(new org.openmrs.module.emrapi.conditionslist.contract.Concept(endReasonUuid, "somename"));
		condition.setEndDate(DateConverter.serialize(today));
		
		concept.setUuid(conceptUuid);
		Patient patient = new Patient();
		patient.setUuid(patientUuid);
		condition.setConcept(new org.openmrs.module.emrapi.conditionslist.contract.Concept(conceptUuid, "somename"));
		condition.setPatientUuid(patientUuid);
		condition.setStatus(INACTIVE);
		condition.setAdditionalDetail(additionalDetail);
		
		when(patientService.getPatientByUuid(patientUuid)).thenReturn(patient);
		when(conceptService.getConceptByUuid(conceptUuid)).thenReturn(concept);
		when(conceptService.getConceptByUuid(endReasonUuid)).thenReturn(endReason);
		
		org.openmrs.module.emrapi.conditionslist.Condition openmrsCondition = conditionMapper.map(condition);
		
		assertEquals(uuid, openmrsCondition.getUuid());
		assertEquals(conceptUuid, openmrsCondition.getConcept().getUuid());
		assertEquals(patientUuid, openmrsCondition.getPatient().getUuid());
		assertEquals(today, openmrsCondition.getOnsetDate());
		assertEquals(additionalDetail, openmrsCondition.getAdditionalDetail());
		
		assertEquals(today, openmrsCondition.getEndDate());
		assertEquals(endReasonUuid, openmrsCondition.getEndReason().getUuid());
		assertEquals(INACTIVE, openmrsCondition.getStatus());
		
	}

	@Test
	public void shouldUseDefaultLocaleWhenConceptNameNotInUseLocale() throws Exception{
		String patientUuid = "13a1234-asdf23-ad2354-sas23";
		Concept concept = new Concept();
		String conceptUuid = "conceptUuid";
		ConceptName conceptNameInUK = new ConceptName("Name in Uk", Locale.UK);
		concept.setUuid(conceptUuid);
		concept.setFullySpecifiedName(conceptNameInUK);

		org.openmrs.module.emrapi.conditionslist.Condition openmrsCondition = new org.openmrs.module.emrapi.conditionslist.Condition();
		openmrsCondition.setConcept(concept);
		
		Patient patient = new Patient();
		patient.setUuid(patientUuid);
		openmrsCondition.setPatient(patient);
		
		User creator = new User();
		creator.setUsername("CREATOR");
		openmrsCondition.setCreator(creator);
		
		when(Context.getLocale()).thenReturn(Locale.FRANCE);
		when(LocaleUtility.getDefaultLocale()).thenReturn(Locale.UK);
		
		Condition condition = conditionMapper.map(openmrsCondition);
		
		assertEquals(condition.getConcept().getName(),"Name in Uk");
	}
}