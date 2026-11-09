package com.deepblue.rescue.config;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.MedicalRecord;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Carga datos de ejemplo para probar el frontend.
 * Solo se ejecuta con el perfil "dev" y solo si los datos aun no existen.
 */
@Component
@Profile("dev")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DemoDataLoader.class);

    private static final String CENTER_CODE = "CTR-CARIBE";

    private final RescueCenterRepository centerRepository;
    private final RescueCaseRepository caseRepository;
    private final SpecialistRepository specialistRepository;
    private final ExpertiseRepository expertiseRepository;
    private final TreatmentRepository treatmentRepository;

    public DemoDataLoader(
            RescueCenterRepository centerRepository,
            RescueCaseRepository caseRepository,
            SpecialistRepository specialistRepository,
            ExpertiseRepository expertiseRepository,
            TreatmentRepository treatmentRepository) {

        this.centerRepository = centerRepository;
        this.caseRepository = caseRepository;
        this.specialistRepository = specialistRepository;
        this.expertiseRepository = expertiseRepository;
        this.treatmentRepository = treatmentRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (centerRepository.findByCode(CENTER_CODE).isPresent()) {
            log.info("Demo data already loaded, skipping");
            return;
        }

        RescueCenter center = centerRepository.save(
                new RescueCenter(CENTER_CODE, "DeepBlue Caribbean", "Cartagena")
        );

        Specialist ana = specialist("SP-001", "Ana", "Torres",
                "ana.torres@deepblue.org", true, "Marine Reptiles", "Trauma");
        Specialist luis = specialist("SP-002", "Luis", "Mendoza",
                "luis.mendoza@deepblue.org", true, "Marine Mammals", "Rehabilitation");
        specialist("SP-003", "Carla", "Rojas",
                "carla.rojas@deepblue.org", false, "Toxicology");

        Animal turtle = rescueCase(center, "CASE-001", "2026-10-01",
                "Playa Blanca", RescueStatus.ADMITTED,
                "AN-001", "Tortuga carey", "Eretmochelys imbricata", AnimalSex.FEMALE);

        rescueCase(center, "CASE-002", "2026-10-03",
                "Isla Baru", RescueStatus.ADMITTED,
                "AN-002", "Pelicano pardo", "Pelecanus occidentalis", AnimalSex.MALE);

        Animal manatee = rescueCase(center, "CASE-003", "2026-09-20",
                "Cienaga de la Virgen", RescueStatus.UNDER_EVALUATION,
                "AN-003", "Manati antillano", "Trichechus manatus", AnimalSex.FEMALE);

        Animal greenTurtle = rescueCase(center, "CASE-004", "2026-09-05",
                "Bahia de Cartagena", RescueStatus.IN_REHABILITATION,
                "AN-004", "Tortuga verde", "Chelonia mydas", AnimalSex.UNKNOWN);

        rescueCase(center, "CASE-005", "2026-08-15",
                "Islas del Rosario", RescueStatus.READY_FOR_RELEASE,
                "AN-005", "Delfin nariz de botella", "Tursiops truncatus", AnimalSex.MALE);

        rescueCase(center, "CASE-006", "2026-07-02",
                "Punta Canoa", RescueStatus.RELEASED,
                "AN-006", "Tortuga caguama", "Caretta caretta", AnimalSex.FEMALE);

        treatment(turtle, ana, "2026-10-02T09:30", TreatmentType.WOUND_CARE,
                "Limpieza de herida en aleta delantera derecha");
        treatment(manatee, luis, "2026-09-21T11:00", TreatmentType.HYDRATION,
                "Hidratacion subcutanea por deshidratacion moderada");
        treatment(greenTurtle, ana, "2026-09-06T08:15", TreatmentType.SURGERY,
                "Retiro de anzuelo alojado en el esofago");
        treatment(greenTurtle, luis, "2026-09-20T16:45", TreatmentType.PHYSIOTHERAPY,
                "Sesion de natacion asistida en piscina de recuperacion");

        log.info("Demo data loaded: 6 rescue cases, 3 specialists, 4 treatments");
    }

    private Specialist specialist(
            String code, String firstName, String lastName,
            String email, boolean active, String... expertiseNames) {

        Specialist specialist =
                new Specialist(code, firstName, lastName, email, active);

        for (String name : expertiseNames) {
            expertiseRepository.findByNameIgnoreCase(name)
                    .ifPresent(specialist::addExpertise);
        }

        return specialistRepository.save(specialist);
    }

    private Animal rescueCase(
            RescueCenter center, String caseCode, String rescueDate,
            String location, RescueStatus status,
            String animalCode, String commonName,
            String scientificName, AnimalSex sex) {

        RescueCase rescueCase = new RescueCase(
                caseCode, LocalDate.parse(rescueDate), location, status
        );
        center.addCase(rescueCase);

        Animal animal = new Animal(animalCode, commonName, scientificName, sex);
        animal.assignMedicalRecord(new MedicalRecord(
                new BigDecimal("25.50"), "Stable", null, null
        ));
        rescueCase.assignAnimal(animal);

        caseRepository.save(rescueCase);
        return animal;
    }

    private void treatment(
            Animal animal, Specialist specialist, String performedAt,
            TreatmentType type, String description) {

        treatmentRepository.save(new Treatment(
                animal, specialist, LocalDateTime.parse(performedAt), type, description
        ));
    }
}
