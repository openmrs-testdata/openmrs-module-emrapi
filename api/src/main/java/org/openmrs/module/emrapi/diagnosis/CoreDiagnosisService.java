package org.openmrs.module.emrapi.diagnosis;

import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * class implementing the DiagnosisService while delegating calls to the core module
 * */
public class CoreDiagnosisService {

	private EmrDiagnosisDAO emrDiagnosisDAO;

	public void setEmrDiagnosisDAO(EmrDiagnosisDAO emrDiagnosisDAO) {
		this.emrDiagnosisDAO = emrDiagnosisDAO;
	}

	public List<Diagnosis> getDiagnoses(Patient patient, Date fromDate) {
		return DiagnosisUtils.convert(Context.getDiagnosisService().getDiagnoses(patient, fromDate));
	}

	public 	List<Diagnosis> getUniqueDiagnoses(Patient patient, Date fromDate){
		return DiagnosisUtils.convert(Context.getDiagnosisService().getUniqueDiagnoses(patient, fromDate));
	}

	public List<Diagnosis> getPrimaryDiagnoses(Encounter encounter){
		return DiagnosisUtils.convert(Context.getDiagnosisService().getPrimaryDiagnoses(encounter));
	}

	public boolean hasDiagnosis(Encounter encounter, Diagnosis diagnosis){
		return true;
	}

	public List<Obs> codeNonCodedDiagnosis(Obs nonCodedObs, List<Diagnosis> diagnoses){
		return null;
	}

	public List<Obs> getDiagnosesAsObs(Visit visit, DiagnosisMetadata diagnosisMetadata, Boolean primaryOnly, Boolean confirmedOnly) {
		List<org.openmrs.Diagnosis> diagnoses = emrDiagnosisDAO.getDiagnoses(visit, primaryOnly, confirmedOnly);
		List<Obs> diagnosisList = new ArrayList<Obs>();
		for (Diagnosis diagnosis : DiagnosisUtils.convert(diagnoses)) {
			diagnosisList.add(diagnosisMetadata.buildDiagnosisObsGroup(diagnosis));
		}
		return diagnosisList;
	}
}
