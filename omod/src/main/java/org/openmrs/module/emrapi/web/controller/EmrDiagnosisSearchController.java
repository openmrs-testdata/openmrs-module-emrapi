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

package org.openmrs.module.emrapi.web.controller;

import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.module.emrapi.diagnosis.Diagnosis;
import org.openmrs.module.emrapi.diagnosis.DiagnosisService;
import org.openmrs.module.emrapi.encounter.DateMapper;
import org.openmrs.module.emrapi.encounter.DiagnosisMapper;
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction;
import org.openmrs.module.emrapi.web.exception.InvalidInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping(method = RequestMethod.GET, value = "/rest/emrapi/diagnosis")
public class EmrDiagnosisSearchController {
    @Autowired
    private DiagnosisService diagnosisService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private DiagnosisMapper diagnosisMapper;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<EncounterTransaction.Diagnosis> search(@RequestParam("patientUuid") String patientUuid, @RequestParam(value = "fromDate", required = false) String date) throws Exception {
        Patient patient = patientService.getPatientByUuid(patientUuid);
        Date fromDate;
        try {
            fromDate = date!=null ? new DateMapper().toDate(date) : new Date(0);
        } catch (Exception e) {
            throw new InvalidInputException("Date format needs to be 'yyyy-MM-dd'. Incorrect Date:" + date + ".", e);
        }
        List<Diagnosis> pastDiagnoses = diagnosisService.getDiagnoses(patient, fromDate);
        return diagnosisMapper.convert(pastDiagnoses);
    }
}

