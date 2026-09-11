package com.deepblue.rescue;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.MedicalRecord;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.MedicalRecordRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;



    @Test
    void inheritedRepositoryMethodsWork() {
        long before = rescueCenterRepository.count();

        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCenter saved = rescueCenterRepository.save(center);

        assertThat(saved.getId()).isNotNull();
        assertThat(rescueCenterRepository.findById(saved.getId())).isPresent();
        assertThat(rescueCenterRepository.existsById(saved.getId())).isTrue();
        assertThat(rescueCenterRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void oneToManyCenterCasesWorks() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta")
        );

        RescueCase first = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 1),
                "Taganga",
                RescueStatus.ADMITTED
        );
        RescueCase second = new RescueCase(
                "RES-002",
                LocalDate.of(2026, 8, 2),
                "Bahia Concha",
                RescueStatus.UNDER_EVALUATION
        );

        center.addCase(first);
        center.addCase(second);
        rescueCaseRepository.saveAllAndFlush(List.of(first, second));

        List<RescueCase> cases = rescueCaseRepository.findByRescueCenterCode("DB-CAR");

        assertThat(cases).hasSize(2);
        assertThat(cases).allMatch(rescueCase -> rescueCase.getRescueCenter().getId().equals(center.getId()));
    }

    @Test
    void oneToOneCaseAnimalWorks() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta")
        );

        RescueCase rescueCase = new RescueCase(
                "RES-2026-001",
                LocalDate.of(2026, 8, 18),
                "Bahia Concha",
                RescueStatus.ADMITTED
        );
        center.addCase(rescueCase);

        Animal animal = new Animal(
                "AN-2026-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );
        rescueCase.assignAnimal(animal);

        rescueCaseRepository.saveAndFlush(rescueCase);

        assertThat(rescueCase.getAnimal()).isSameAs(animal);
        assertThat(animal.getRescueCase()).isSameAs(rescueCase);
        assertThat(animal.getId()).isNotNull();
    }

    @Test
    void oneToOneAnimalMedicalRecordWorksWithCascade() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta")
        );

        RescueCase rescueCase = new RescueCase(
                "RES-2026-002",
                LocalDate.of(2026, 8, 19),
                "Rodadero",
                RescueStatus.UNDER_EVALUATION
        );
        center.addCase(rescueCase);

        Animal animal = new Animal(
                "AN-2026-002",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.UNKNOWN
        );
        MedicalRecord record = new MedicalRecord(
                new BigDecimal("28.40"),
                "STABLE",
                "Left front flipper injury",
                null
        );

        animal.assignMedicalRecord(record);
        rescueCase.assignAnimal(animal);
        rescueCaseRepository.saveAndFlush(rescueCase);

        assertThat(animal.getId()).isNotNull();
        assertThat(record.getId()).isNotNull();
        assertThat(record.getAnimal()).isSameAs(animal);
        assertThat(medicalRecordRepository.findById(record.getId())).isPresent();
    }

    @Test
    void manyToManySpecialistExpertiseWorks() {
        Expertise trauma = expertise("Trauma");
        Expertise rehabilitation = expertise("Rehabilitation");

        Specialist elena = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org",
                true
        );
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist saved = specialistRepository.saveAndFlush(elena);
        Long id = saved.getId();

        entityManager.clear();

        Specialist reloaded = specialistRepository.findById(id).orElseThrow();
        assertThat(reloaded.getExpertiseAreas()).hasSize(2);
    }

    @Test
    void queryMethodByStatusReturnsOnlyMatchingCases() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta")
        );

        RescueCase first = new RescueCase("RES-001", LocalDate.of(2026, 8, 1), "A", RescueStatus.IN_REHABILITATION);
        RescueCase second = new RescueCase("RES-002", LocalDate.of(2026, 8, 2), "B", RescueStatus.READY_FOR_RELEASE);
        RescueCase third = new RescueCase("RES-003", LocalDate.of(2026, 8, 3), "C", RescueStatus.IN_REHABILITATION);

        center.addCase(first);
        center.addCase(second);
        center.addCase(third);
        rescueCaseRepository.saveAllAndFlush(List.of(first, second, third));

        List<RescueCase> result = rescueCaseRepository
                .findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(RescueCase::getCaseCode)
                .containsExactly("RES-001", "RES-003");
    }

    @Test
    void queryMethodNavigatesAnimalCaseAndCenter() {
        RescueCenter caribbean = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );
        RescueCenter pacific = rescueCenterRepository.save(
                new RescueCenter("DB-PAC", "DeepBlue Pacific", "Buenaventura")
        );

        RescueCase caribbeanCase = new RescueCase("RES-CAR", LocalDate.of(2026, 8, 1), "A", RescueStatus.ADMITTED);
        RescueCase pacificCase = new RescueCase("RES-PAC", LocalDate.of(2026, 8, 2), "B", RescueStatus.ADMITTED);
        caribbean.addCase(caribbeanCase);
        pacific.addCase(pacificCase);

        caribbeanCase.assignAnimal(new Animal("AN-CAR", "Turtle", "Chelonia mydas", AnimalSex.UNKNOWN));
        pacificCase.assignAnimal(new Animal("AN-PAC", "Dolphin", "Tursiops truncatus", AnimalSex.UNKNOWN));

        rescueCaseRepository.saveAllAndFlush(List.of(caribbeanCase, pacificCase));

        List<Animal> animals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");

        assertThat(animals).extracting(Animal::getAnimalCode)
                .containsExactly("AN-CAR");
    }

    @Test
    void jpqlFindsActiveSpecialistsByExpertise() {
        Expertise trauma = expertise("Trauma");
        Expertise rehabilitation = expertise("Rehabilitation");
        Expertise mammals = expertise("Marine Mammals");
        Expertise birds = expertise("Marine Birds");

        Specialist elena = new Specialist("SPEC-E", "Elena", "Vargas", "elena@test.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist mateo = new Specialist("SPEC-M", "Mateo", "Lopez", "mateo@test.org", true);
        mateo.addExpertise(mammals);
        mateo.addExpertise(rehabilitation);

        Specialist sofia = new Specialist("SPEC-S", "Sofia", "Ruiz", "sofia@test.org", true);
        sofia.addExpertise(birds);
        sofia.addExpertise(trauma);

        specialistRepository.saveAllAndFlush(List.of(elena, mateo, sofia));

        List<Specialist> result = specialistRepository.findActiveByExpertise("trauma");

        assertThat(result).extracting(Specialist::getFirstName)
                .containsExactly("Sofia", "Elena");
    }

    @Test
    void treatmentQueryMethodOrdersChronologically() {
        ScenarioData data = basicScenario("ORD");

        Specialist elena = specialistRepository.save(
                new Specialist("SPEC-ORD", "Elena", "Vargas", "ord@deepblue.org", true)
        );

        Treatment third = new Treatment(data.animal, elena, LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "Third");
        Treatment first = new Treatment(data.animal, elena, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "First");
        Treatment second = new Treatment(data.animal, elena, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "Second");

        treatmentRepository.saveAllAndFlush(List.of(third, first, second));

        List<Treatment> treatments = treatmentRepository
                .findByAnimalIdOrderByPerformedAtAsc(data.animal.getId());

        assertThat(treatments).extracting(Treatment::getDescription)
                .containsExactly("First", "Second", "Third");
    }

    @Test
    void jpqlFindsTreatmentsInsideDateInterval() {
        ScenarioData data = basicScenario("DATE");
        Specialist specialist = specialistRepository.save(
                new Specialist("SPEC-DATE", "Mateo", "Lopez", "date@deepblue.org", true)
        );

        treatmentRepository.saveAllAndFlush(List.of(
                new Treatment(data.animal, specialist, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "August 1"),
                new Treatment(data.animal, specialist, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "August 10"),
                new Treatment(data.animal, specialist, LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "August 20")
        ));

        List<Treatment> result = treatmentRepository.findBetweenDates(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 23, 59)
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDescription()).isEqualTo("August 10");
    }

    @Test
    void uniqueAnimalCodeIsEnforcedByPostgres() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-UNQ", "Unique Center", "Santa Marta")
        );

        RescueCase firstCase = new RescueCase("RES-UNQ-1", LocalDate.of(2026, 8, 1), "A", RescueStatus.ADMITTED);
        RescueCase secondCase = new RescueCase("RES-UNQ-2", LocalDate.of(2026, 8, 2), "B", RescueStatus.ADMITTED);
        center.addCase(firstCase);
        center.addCase(secondCase);

        firstCase.assignAnimal(new Animal("AN-100", "Turtle", "Chelonia mydas", AnimalSex.UNKNOWN));
        secondCase.assignAnimal(new Animal("AN-100", "Turtle", "Chelonia mydas", AnimalSex.UNKNOWN));

        rescueCaseRepository.saveAndFlush(firstCase);

        assertThatThrownBy(() -> rescueCaseRepository.saveAndFlush(secondCase))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void foreignKeyConstraintIsEnforcedByPostgres() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                insert into rescue_cases
                    (case_code, rescue_date, rescue_location, status, rescue_center_id)
                values
                    ('RES-BAD-FK', '2026-08-01', 'Test', 'ADMITTED', 999999)
                """
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void statusCheckConstraintIsEnforcedByPostgres() {
        RescueCenter center = rescueCenterRepository.saveAndFlush(
                new RescueCenter("DB-CHK", "Check Center", "Santa Marta")
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                insert into rescue_cases
                    (case_code, rescue_date, rescue_location, status, rescue_center_id)
                values
                    (?, '2026-08-01', 'Test', 'INVALID_STATUS', ?)
                """,
                "RES-BAD-CHECK",
                center.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void integratedScenarioAndFinalChallengeWork() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );

        RescueCase rescueCase = new RescueCase(
                "RES-2026-100",
                LocalDate.of(2026, 8, 18),
                "Bahia Concha",
                RescueStatus.IN_REHABILITATION
        );
        center.addCase(rescueCase);

        Animal animal = new Animal(
                "AN-2026-100",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );
        animal.setTrackingDeviceCode("GPS-100");
        animal.assignMedicalRecord(new MedicalRecord(
                new BigDecimal("27.80"),
                "STABLE",
                "Injury caused by fishing net",
                "Possible plastic ingestion"
        ));
        rescueCase.assignAnimal(animal);
        rescueCaseRepository.saveAndFlush(rescueCase);

        Specialist elena = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org",
                true
        );
        elena.addExpertise(expertise("Marine Reptiles"));
        elena.addExpertise(expertise("Trauma"));
        elena.addExpertise(expertise("Rehabilitation"));
        specialistRepository.saveAndFlush(elena);

        Treatment first = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 18, 14, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper"
        );
        Treatment second = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 19, 9, 30),
                TreatmentType.HYDRATION,
                "Subcutaneous fluid therapy"
        );
        treatmentRepository.saveAllAndFlush(List.of(first, second));

        assertThat(rescueCaseRepository.findByCaseCode("RES-2026-100")).isPresent();
        assertThat(rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION))
                .extracting(RescueCase::getCaseCode)
                .contains("RES-2026-100");
        assertThat(animalRepository.findByRescueCaseRescueCenterCode("DB-CAR"))
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");
        assertThat(animalRepository.findByCommonNameContainingIgnoreCase("turtle"))
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");
        assertThat(specialistRepository.findActiveByExpertise("Trauma"))
                .extracting(Specialist::getProfessionalCode)
                .contains("SPEC-001");
        assertThat(treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId()))
                .extracting(Treatment::getType)
                .containsExactly(TreatmentType.WOUND_CARE, TreatmentType.HYDRATION);
        assertThat(treatmentRepository.findBySpecialistExpertise("rehabilitation"))
                .hasSize(2);
        assertThat(treatmentRepository.findBetweenDates(
                LocalDateTime.of(2026, 8, 19, 0, 0),
                LocalDateTime.of(2026, 8, 19, 23, 59)
        )).hasSize(1);
        assertThat(treatmentRepository.findByCenterCode("DB-CAR"))
                .hasSize(2);

        List<Animal> finalChallenge = animalRepository.findByStatusAndTreatmentExpertise(
                RescueStatus.IN_REHABILITATION,
                "trauma"
        );
        assertThat(finalChallenge).extracting(Animal::getAnimalCode)
                .containsExactly("AN-2026-100");
    }

    private Expertise expertise(String name) {
        return expertiseRepository.findByNameIgnoreCase(name).orElseThrow();
    }

    private ScenarioData basicScenario(String suffix) {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-" + suffix, "Center " + suffix, "Santa Marta")
        );
        RescueCase rescueCase = new RescueCase(
                "RES-" + suffix,
                LocalDate.of(2026, 8, 1),
                "Santa Marta",
                RescueStatus.IN_REHABILITATION
        );
        center.addCase(rescueCase);
        Animal animal = new Animal(
                "AN-" + suffix,
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.UNKNOWN
        );
        rescueCase.assignAnimal(animal);
        rescueCaseRepository.saveAndFlush(rescueCase);
        return new ScenarioData(center, rescueCase, animal);
    }

    private record ScenarioData(RescueCenter center, RescueCase rescueCase, Animal animal) {
    }
}
