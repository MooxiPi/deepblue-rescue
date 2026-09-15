package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;


    @Test
    void shouldRegisterTreatmentSuccessfully() {

        // ARRANGE

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Bahia Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org",
                true
        );

        CreateTreatmentRequest request =
                new CreateTreatmentRequest(
                        "AN-001",
                        "SPEC-001",
                        LocalDateTime.of(
                                2026, 8, 21, 9, 0
                        ),
                        TreatmentType.WOUND_CARE,
                        "Cleaning of left front flipper injury"
                );

        TreatmentResponse response =
                new TreatmentResponse(
                        null,
                        "AN-001",
                        "SPEC-001",
                        LocalDateTime.of(
                                2026, 8, 21, 9, 0
                        ),
                        TreatmentType.WOUND_CARE,
                        "Cleaning of left front flipper injury"
                );

        when(
                animalRepository.findByAnimalCode("AN-001")
        ).thenReturn(
                Optional.of(animal)
        );

        when(
                specialistRepository
                        .findByProfessionalCode("SPEC-001")
        ).thenReturn(
                Optional.of(specialist)
        );

        when(
                treatmentRepository.save(
                        any(Treatment.class)
                )
        ).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        when(
                mapper.toResponse(any(Treatment.class))
        ).thenReturn(response);


        // ACT

        TreatmentResponse result =
                service.register(request);


        // ASSERT

        assertThat(result)
                .isEqualTo(response);

        ArgumentCaptor<Treatment> captor =
                ArgumentCaptor.forClass(
                        Treatment.class
                );

        verify(treatmentRepository)
                .save(captor.capture());

        Treatment savedTreatment =
                captor.getValue();

        assertThat(savedTreatment.getAnimal())
                .isEqualTo(animal);

        assertThat(savedTreatment.getSpecialist())
                .isEqualTo(specialist);

        assertThat(savedTreatment.getType())
                .isEqualTo(
                        TreatmentType.WOUND_CARE
                );

        assertThat(savedTreatment.getPerformedAt())
                .isEqualTo(
                        LocalDateTime.of(
                                2026, 8, 21, 9, 0
                        )
                );

        verify(mapper)
                .toResponse(any(Treatment.class));
    }


    @Test
    void shouldRejectTreatmentWhenSpecialistIsInactive() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Bahia Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org",
                false
        );

        CreateTreatmentRequest request =
                new CreateTreatmentRequest(
                        "AN-001",
                        "SPEC-001",
                        LocalDateTime.of(
                                2026, 8, 21, 9, 0
                        ),
                        TreatmentType.WOUND_CARE,
                        "Treatment attempt"
                );

        when(
                animalRepository.findByAnimalCode("AN-001")
        ).thenReturn(
                Optional.of(animal)
        );

        when(
                specialistRepository
                        .findByProfessionalCode("SPEC-001")
        ).thenReturn(
                Optional.of(specialist)
        );

        assertThatThrownBy(
                () -> service.register(request)
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "not active"
                );

        verify(
                treatmentRepository,
                never()
        ).save(any());
    }


    @Test
    void shouldRejectTreatmentWhenCaseIsReleased() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Bahia Concha",
                RescueStatus.RELEASED
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org",
                true
        );

        CreateTreatmentRequest request =
                new CreateTreatmentRequest(
                        "AN-001",
                        "SPEC-001",
                        LocalDateTime.of(
                                2026, 8, 21, 9, 0
                        ),
                        TreatmentType.OBSERVATION,
                        "Post release observation"
                );

        when(
                animalRepository.findByAnimalCode("AN-001")
        ).thenReturn(
                Optional.of(animal)
        );

        when(
                specialistRepository
                        .findByProfessionalCode("SPEC-001")
        ).thenReturn(
                Optional.of(specialist)
        );

        assertThatThrownBy(
                () -> service.register(request)
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "RELEASED"
                );

        verify(
                treatmentRepository,
                never()
        ).save(any());
    }


    @Test
    void shouldRejectTreatmentBeforeRescueDate() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Bahia Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org",
                true
        );

        CreateTreatmentRequest request =
                new CreateTreatmentRequest(
                        "AN-001",
                        "SPEC-001",
                        LocalDateTime.of(
                                2026, 8, 15, 9, 0
                        ),
                        TreatmentType.OBSERVATION,
                        "Invalid treatment date"
                );

        when(
                animalRepository.findByAnimalCode("AN-001")
        ).thenReturn(
                Optional.of(animal)
        );

        when(
                specialistRepository
                        .findByProfessionalCode("SPEC-001")
        ).thenReturn(
                Optional.of(specialist)
        );

        assertThatThrownBy(
                () -> service.register(request)
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "before rescue date"
                );

        verify(
                treatmentRepository,
                never()
        ).save(any());
    }
}