package org.openmrs.module.emrapi.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.CodedOrFreeText;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ConditionService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emrapi.conditionslist.Condition;
import org.openmrs.module.emrapi.conditionslist.ConditionHistory;
import org.openmrs.module.emrapi.conditionslist.ConditionListConstants;
import org.openmrs.module.emrapi.conditionslist.DateConverter;
import org.openmrs.module.emrapi.conditionslist.contract.ConditionHistoryMapper;
import org.openmrs.module.emrapi.conditionslist.contract.ConditionMapper;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.openmrs.util.LocaleUtility.getDefaultLocale;

/**
 * This class specifies data manipulation methods on a Condition.
 */
@Controller
@RequestMapping(value = "/rest/emrapi")
public class ConditionController extends BaseRestController {

	ConditionMapper conditionMapper = new ConditionMapper();

	ConditionHistoryMapper conditionHistoryMapper = new ConditionHistoryMapper(conditionMapper);

	ConditionService conditionService;

	PatientService patientService;

	ConceptService conceptService;

	/**
	 * Constructor to instantiate the ConditionController.
	 *
	 * @param conditionService - the condition service
	 * @param patientService - the patient service
	 * @param conceptService - the concept service
	 */
	@Autowired
	public ConditionController(ConditionService conditionService, PatientService patientService,
			ConceptService conceptService) {
		this.conditionService = conditionService;
		this.patientService = patientService;
		this.conceptService = conceptService;
	}

	/**
	 * Method to return a concept name
	 *
	 * @param names - a collection of concept name objects
	 * @param name - name of a concept
	 *
	 * @return null or currentName
	 */
	private ConceptName getName(Collection<ConceptName> names, String name) {
		if (name == null) {
			return null;
		}

		for (ConceptName currentName : names) {
			if (name.equalsIgnoreCase(currentName.getName())) {
				return currentName;
			}
		}

		return null;
	}

	/**
	 * Method to convert clinical status of core condition objects into status of condition objects of the emrapi module
	 *
	 * @param clinicalStatus - a ConditionClinicalStatus object
	 *
	 * @return convertedStatus
	 */
	private Condition.Status convertClinicalStatus(ConditionClinicalStatus clinicalStatus) {
		Condition.Status convertedStatus = Condition.Status.ACTIVE;

		if (clinicalStatus == ConditionClinicalStatus.ACTIVE) {
			convertedStatus = Condition.Status.ACTIVE;
		} else if (clinicalStatus == ConditionClinicalStatus.INACTIVE) {
			convertedStatus = Condition.Status.INACTIVE;
		} else if (clinicalStatus == ConditionClinicalStatus.HISTORY_OF) {
			convertedStatus = Condition.Status.HISTORY_OF;
		}

		return convertedStatus;
	}

	/**
	 * Method to convert Condition.Status object of the emrapi module into ConditionClinicalStatus object of the core module
	 *
	 * @param status - a Condition.Status object
	 *
	 * @return convertedStatus
	 */
	private ConditionClinicalStatus convertConditionListStatus(Condition.Status status) {
		ConditionClinicalStatus convertedStatus = ConditionClinicalStatus.ACTIVE;

		if (status == Condition.Status.ACTIVE) {
			convertedStatus = ConditionClinicalStatus.ACTIVE;
		} else if (status == Condition.Status.INACTIVE) {
			convertedStatus = ConditionClinicalStatus.INACTIVE;
		} else if (status == Condition.Status.HISTORY_OF) {
			convertedStatus = ConditionClinicalStatus.HISTORY_OF;
		}

		return convertedStatus;
	}

	/**
	 * Method to convert an org.openmrs.Concept object into an
	 * org.openmrs.module.emrapi.conditionslist.contract.Concept object
	 *
	 * @param coreConcept - a Condition.Status object
	 *
	 * @return concept
	 */
	private org.openmrs.module.emrapi.conditionslist.contract.Concept convertCoreConceptToEmrapiConcept(Concept coreConcept) {
		ConceptName fullySpecifiedName = coreConcept.getFullySpecifiedName(Context.getLocale());
		if (fullySpecifiedName == null) {
			fullySpecifiedName = coreConcept.getFullySpecifiedName(getDefaultLocale());
		}
		if (fullySpecifiedName == null) {
			fullySpecifiedName = coreConcept.getFullySpecifiedName(new Locale("en"));
		}
		org.openmrs.module.emrapi.conditionslist.contract.Concept concept =
				new org.openmrs.module.emrapi.conditionslist.contract.Concept(coreConcept.getUuid(),
						fullySpecifiedName.getName());
		ConceptName shortName = coreConcept.getShortNameInLocale(Context.getLocale());

		if (shortName != null) {
			concept.setShortName(shortName.getName());
		}
		return concept;
	}

	/**
	 * Method to convert a list of org.openmrs.Condition objects into a list of
	 * org.openmrs.module.emrapi.conditionslist.Condition objects
	 *
	 * @param coreConditions - a list of org.openmrs.Condition objects
	 *
	 * @return conditions
	 */
	private List<Condition> convertCoreConditionsToEmrapiConditions(List<org.openmrs.Condition> coreConditions) {
		List<Condition> conditions = new ArrayList<Condition>();
		for (org.openmrs.Condition coreCondition : coreConditions) {
			Condition condition = new Condition();
			Concept concept;

			if (coreCondition.getCondition().getCoded() != null) {
				concept = conceptService.getConceptByUuid(coreCondition.getCondition().getCoded().getUuid());
			} else {
				concept = new Concept();
				concept.setFullySpecifiedName(new ConceptName(coreCondition.getCondition().getNonCoded(), Context.getLocale()));
			}

			condition.setId(coreCondition.getId());
			condition.setUuid(coreCondition.getUuid());
			condition.setAdditionalDetail(coreCondition.getAdditionalDetail());
			condition.setConcept(concept);
			condition.setPatient(coreCondition.getPatient());

			if (coreCondition.getCondition().getNonCoded() != null) {
				condition.setConditionNonCoded(coreCondition.getCondition().getNonCoded());
			}

			if (coreCondition.getOnsetDate() != null) {
				condition.setOnsetDate(coreCondition.getOnsetDate());
			}
			condition.setVoided(coreCondition.getVoided());
			condition.setVoidReason(coreCondition.getVoidReason());
			condition.setEndDate(coreCondition.getEndDate());
			condition.setCreator(coreCondition.getCreator());
			condition.setDateCreated(coreCondition.getDateCreated());
			condition.setStatus(convertClinicalStatus(coreCondition.getClinicalStatus()));
			if (coreCondition.getPreviousVersion() != null) {
				condition.setPreviousCondition(convertCoreConditionToEmrapiCondition(coreCondition.getPreviousVersion()));
			}

			conditions.add(condition);
		}
		return conditions;
	}

	/**
	 * Method to convert a list of core active conditions into a list of condition history objects
	 *
	 * @param coreConditions - a list of org.openmrs.Condition objects
	 *
	 * @return conditionHistory
	 */
	private List<ConditionHistory> convertHistory(List<org.openmrs.Condition> coreConditions) {
		List<Condition> convertedConditions = convertCoreConditionsToEmrapiConditions(coreConditions);
		Map<String, ConditionHistory> allConditions = new LinkedHashMap<String, ConditionHistory>();

		for (Condition condition : convertedConditions) {
			Concept concept = condition.getConcept();

			String nonCodedConceptUuid = Context.getAdministrationService().getGlobalProperty(
					ConditionListConstants.GLOBAL_PROPERTY_NON_CODED_UUID);

			String key = concept.getUuid().equals(nonCodedConceptUuid) ?
					condition.getConditionNonCoded() :
					concept.getUuid();
			ConditionHistory conditionHistory = allConditions.get(key);

			if (conditionHistory != null) {
				conditionHistory.getConditions().add(condition);
			} else {
				conditionHistory = new ConditionHistory();
				List<Condition> conditions = new ArrayList<Condition>();
				conditions.add(condition);
				conditionHistory.setConditions(conditions);
				conditionHistory.setCondition(condition.getConcept());
				if (concept.getUuid().equals(nonCodedConceptUuid)) {
					conditionHistory.setNonCodedCondition(condition.getConditionNonCoded());
				}
			}
			allConditions.put(key, conditionHistory);
		}
		return new ArrayList<ConditionHistory>(allConditions.values());
	}

	/**
	 * Method to convert an org.openmrs.module.emrapi.conditionslist.contract.Condition object into an org.openmrs.Condition
	 * object
	 *
	 * @param condition - an org.openmrs.module.emrapi.conditionslist.contract.Condition object
	 *
	 * @return cond
	 */
	private org.openmrs.Condition convertEmrapiContractConditionToCoreCondition(org.openmrs.module.emrapi.conditionslist.contract.Condition condition) {

		org.openmrs.Condition openmrsCondition = null;

		Patient patient = Context.getPatientService().getPatientByUuid(condition.getPatientUuid());

		if (condition.getUuid() != null) {
			openmrsCondition = conditionService.getConditionByUuid(condition.getUuid());
		}
		if (openmrsCondition == null) {
			openmrsCondition = new org.openmrs.Condition();
			openmrsCondition.setUuid(condition.getUuid());
		}

		// Map coded conditions
		CodedOrFreeText codedOrFreeText = new CodedOrFreeText();
		if (condition.getConcept() != null) {
			if (condition.getConcept().getUuid() != null) {
				codedOrFreeText.setCoded(conceptService.getConceptByUuid(condition.getConcept().getUuid()));
			}
			if (condition.getConcept().getName() != null && codedOrFreeText.getCoded() != null) {
				codedOrFreeText.setSpecificName(getName(codedOrFreeText.getCoded().getNames(), condition.getConcept().getName()));
			}
		}

		// Map non-coded conditions
		if (codedOrFreeText.getCoded() == null) {
			if (!isEmpty(condition.getConditionNonCoded())) {
				String gpName = ConditionListConstants.GLOBAL_PROPERTY_NON_CODED_UUID;
				String nonCodedConditionConcept = Context.getAdministrationService().getGlobalProperty(gpName);
				if (StringUtils.isBlank(nonCodedConditionConcept)) {
					throw new IllegalStateException("Configuration Required: " + gpName);
				}
				codedOrFreeText.setCoded(conceptService.getConceptByUuid(nonCodedConditionConcept));
			}
		}
		codedOrFreeText.setNonCoded(condition.getConditionNonCoded());

		openmrsCondition.setCondition(codedOrFreeText);
		openmrsCondition.setAdditionalDetail(condition.getAdditionalDetail());
		openmrsCondition.setClinicalStatus(convertConditionListStatus(condition.getStatus()));
		openmrsCondition.setCondition(codedOrFreeText);
		openmrsCondition.setPatient(patient);
		openmrsCondition.setOnsetDate(DateConverter.deserialize(condition.getOnSetDate()));
		openmrsCondition.setEndDate(DateConverter.deserialize(condition.getEndDate()));
		openmrsCondition.setVoided(condition.getVoided());
		openmrsCondition.setVoidReason(condition.getVoidReason());

		return openmrsCondition;
	}

	/**
	 * Method to convert an org.openmrs.Condition object into an org.openmrs.module.emrapi.conditionslist.contract.Condition
	 * object
	 *
	 * @param coreCondition -a core condition object
	 *
	 * @return contractCondition
	 */
	private org.openmrs.module.emrapi.conditionslist.contract.Condition convertCoreConditionToEmrapiContractCondition(org.openmrs.Condition coreCondition) {
		org.openmrs.module.emrapi.conditionslist.contract.Concept concept = new org.openmrs.module.emrapi.conditionslist.contract.Concept();
		if (coreCondition.getCondition().getCoded() != null) {
			concept = convertCoreConceptToEmrapiConcept(coreCondition.getCondition().getCoded());
		}
		org.openmrs.module.emrapi.conditionslist.contract.Condition contractCondition = new org.openmrs.module.emrapi.conditionslist.contract.Condition();
		CodedOrFreeText codedOrFreeText = coreCondition.getCondition();

		contractCondition.setUuid(coreCondition.getUuid());
		contractCondition.setAdditionalDetail(coreCondition.getAdditionalDetail());
		contractCondition.setStatus(convertClinicalStatus(coreCondition.getClinicalStatus()));
		contractCondition.setConcept(concept);
		contractCondition.setPatientUuid(coreCondition.getPatient().getUuid());
		contractCondition.setConditionNonCoded(codedOrFreeText.getNonCoded());
		contractCondition.setOnSetDate(DateConverter.serialize(coreCondition.getOnsetDate()));
		contractCondition.setVoided(coreCondition.getVoided());
		contractCondition.setVoidReason(coreCondition.getVoidReason());
		contractCondition.setEndDate(DateConverter.serialize(coreCondition.getEndDate()));
		contractCondition.setCreator(coreCondition.getCreator().getUuid());
		contractCondition.setDateCreated(coreCondition.getDateCreated());
		if (coreCondition.getPreviousVersion() != null) {
			contractCondition.setPreviousConditionUuid(convertCoreConditionToEmrapiCondition(coreCondition.getPreviousVersion()).getUuid());
		}

		return contractCondition;
	}

	/**
	 * Method to convert an org.openmrs.Condition object into an org.openmrs.module.emrapi.conditionslist.Condition
	 * object
	 *
	 * @param coreCondition - an org.openmrs.Condition object
	 *
	 * @return cListCondition
	 */
	private org.openmrs.module.emrapi.conditionslist.Condition convertCoreConditionToEmrapiCondition(org.openmrs.Condition coreCondition) {
		org.openmrs.module.emrapi.conditionslist.Condition cListCondition = new org.openmrs.module.emrapi.conditionslist.Condition();
		Concept concept;

		if (coreCondition.getCondition().getCoded() != null) {
			concept = conceptService.getConceptByUuid(coreCondition.getCondition().getCoded().getUuid());

			if(coreCondition.getCondition().getSpecificName() == null) {
				coreCondition.getCondition().setSpecificName(coreCondition.getCondition().getCoded().getName(Context.getLocale()));
			}
		} else {
			concept = new Concept();
		}

		cListCondition.setUuid(coreCondition.getUuid());
		cListCondition.setConcept(concept);
		cListCondition.setAdditionalDetail(coreCondition.getAdditionalDetail());
		cListCondition.setPatient(coreCondition.getPatient());
		cListCondition.setConditionNonCoded(coreCondition.getCondition().getNonCoded());
		cListCondition.setOnsetDate(coreCondition.getOnsetDate());
		cListCondition.setVoided(coreCondition.getVoided());
		cListCondition.setVoidReason(coreCondition.getVoidReason());
		cListCondition.setEndDate(coreCondition.getEndDate());
		cListCondition.setCreator(coreCondition.getCreator());
		cListCondition.setDateCreated(coreCondition.getDateCreated());
		cListCondition.setStatus(convertClinicalStatus(coreCondition.getClinicalStatus()));
		if (coreCondition.getPreviousVersion() != null) {
			cListCondition.setPreviousCondition(convertCoreConditionToEmrapiCondition(coreCondition.getPreviousVersion()));
		}

		return cListCondition;
	}

	/**
	 * Gets a list of active conditions.
	 *
	 * @param patientUuid - the uuid of a patient
	 * @return a list of active conditions
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/conditionhistory")
	@ResponseBody
	public List<org.openmrs.module.emrapi.conditionslist.contract.ConditionHistory> getConditionHistory(@RequestParam("patientUuid") String patientUuid) {
		List<org.openmrs.Condition> Conditions = conditionService.getAllConditions(patientService.getPatientByUuid(patientUuid));
		return conditionHistoryMapper.map(convertHistory(Conditions));
	}
	
    @RequestMapping(method = RequestMethod.GET, value = "/condition")
	@ResponseBody
	public List<org.openmrs.module.emrapi.conditionslist.contract.Condition> getCondition(@RequestParam("conditionUuid") String conditionUuid) {
		org.openmrs.Condition condition = conditionService.getConditionByUuid(conditionUuid);
        List<org.openmrs.Condition> conditionList =new ArrayList<org.openmrs.Condition>();
		conditionList.add(condition);
		List<org.openmrs.module.emrapi.conditionslist.contract.Condition> result = new ArrayList<org.openmrs.module.emrapi.conditionslist.contract.Condition>();
		result.add(conditionMapper.map(convertCoreConditionsToEmrapiConditions(conditionList).get(0)));
		return result;
	}

	/**
	 * Saves a condition.
	 *
	 * @param conditions - a list of conditions to be saved
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/condition")
	@ResponseBody
	public List<org.openmrs.module.emrapi.conditionslist.contract.Condition> save(@RequestBody org.openmrs.module.emrapi.conditionslist.contract.Condition[] conditions) {

		List<org.openmrs.module.emrapi.conditionslist.contract.Condition> savedConditions = new ArrayList<org.openmrs.module.emrapi.conditionslist.contract.Condition>();

		for (org.openmrs.module.emrapi.conditionslist.contract.Condition condition : conditions) {
			org.openmrs.Condition savedCondition = conditionService.saveCondition(
					convertEmrapiContractConditionToCoreCondition(condition));
			savedConditions.add(convertCoreConditionToEmrapiContractCondition(savedCondition));
		}
		return savedConditions;
	}

}

