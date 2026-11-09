package com.deepblue.rescue.controller;

import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.TreatmentEligibilityResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/animals")
public class AnimalController {

    private final AnimalService animalService;
    private final TreatmentService treatmentService;

    public AnimalController(
            AnimalService animalService,
            TreatmentService treatmentService) {

        this.animalService = animalService;
        this.treatmentService = treatmentService;
    }

    // GET /api/animals/{animalCode}
    @GetMapping("/{animalCode}")
    public AnimalResponse findByCode(
            @PathVariable String animalCode) {

        return animalService.findByCode(animalCode);
    }

    // GET /api/animals/{animalCode}/treatments
    @GetMapping("/{animalCode}/treatments")
    public List<TreatmentResponse> findTreatments(
            @PathVariable String animalCode) {

        return treatmentService.findByAnimalCode(animalCode);
    }

    // GET /api/animals/{animalCode}/treatment-eligibility
    @GetMapping("/{animalCode}/treatment-eligibility")
    public TreatmentEligibilityResponse treatmentEligibility(
            @PathVariable String animalCode) {

        return new TreatmentEligibilityResponse(
                animalCode,
                animalService.canReceiveTreatment(animalCode)
        );
    }
}
