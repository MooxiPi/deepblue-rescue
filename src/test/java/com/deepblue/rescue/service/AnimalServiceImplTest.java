package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository repository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;


    @Test
    void shouldAllowTreatmentWhenAnimalIsInRehabilitation() {

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

        when(repository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        boolean result =
                service.canReceiveTreatment("AN-001");

        assertThat(result).isTrue();
    }


    @Test
    void shouldNotAllowTreatmentWhenAnimalIsReleased() {

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

        when(repository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        boolean result =
                service.canReceiveTreatment("AN-001");

        assertThat(result).isFalse();
    }


    @Test
    void shouldThrowExceptionWhenAnimalDoesNotExist() {

        when(repository.findByAnimalCode("AN-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.canReceiveTreatment("AN-999")
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-999");
    }


    @Test
    void shouldFindAnimalByCode() {

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

        AnimalResponse response =
                new AnimalResponse(
                        null,
                        "AN-001",
                        "Green Sea Turtle",
                        "Chelonia mydas",
                        AnimalSex.FEMALE,
                        "RES-001",
                        RescueStatus.IN_REHABILITATION
                );

        when(repository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        when(mapper.toResponse(animal))
                .thenReturn(response);

        AnimalResponse result =
                service.findByCode("AN-001");

        assertThat(result)
                .isEqualTo(response);
    }
}