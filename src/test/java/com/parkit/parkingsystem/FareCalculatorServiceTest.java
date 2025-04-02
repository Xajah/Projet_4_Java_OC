package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

public class FareCalculatorServiceTest {

    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;

    @BeforeAll
    static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    void setUpPerTest() {
        ticket = new Ticket();
    }

    /**
     * Tests paramétrés pour les calculs de tarifs standard.
     * Ceci regroupe les tests :
     * - calculateFareCar
     * - calculateFareBike
     * - calculateFareBikeWithLessThanOneHourParkingTime
     * - calculateFareCarWithLessThanOneHourParkingTime
     * - calculateFareCarWithMoreThanADayParkingTime
     * - calculateFareDurationUnderThirtyMinutesForCarShouldBeFree
     * - calculateFareDurationUnderThirtyMinutesForBikeShouldBeFree
     */
    @ParameterizedTest
    @CsvSource({
            // Cas gratuit (29 et 30 minutes) et payant (31 minutes)
            "CAR, 29, 0.0",    // Voiture, 29 minutes, gratuit
            "CAR, 30, 0.0",    // Voiture, 30 minutes, gratuit
            "CAR, 31, 0.775",  // Voiture, 31 minutes, payant (tarif horaire basé sur CAR_RATE_PER_HOUR)

            "BIKE, 29, 0.0",   // Moto, 29 minutes, gratuit
            "BIKE, 30, 0.0",   // Moto, 30 minutes, gratuit
            "BIKE, 31, 0.517", // Moto, 31 minutes, payant (tarif horaire basé sur BIKE_RATE_PER_HOUR)

            // Cas normaux
            "CAR, 60, 1.5",    // Voiture, 1 heure, tarif normal
            "BIKE, 60, 1.0",   // Moto, 1 heure, tarif normal

            // Cas avec moins d'une heure
            "CAR, 45, 1.125",  // Voiture, 45 minutes, 3/4 du tarif
            "BIKE, 45, 0.75",  // Moto, 45 minutes, 3/4 du tarif

            // Cas supérieurs à un jour
            "CAR, 1440, 36.0"  // Voiture, 24 heures, tarif journalier
    })
    public void calculateFareParameterized(String vehicleType, int durationInMinutes, double expectedFare) {
        // Configuration des temps d'entrée et de sortie
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (durationInMinutes * 60 * 1000));
        Date outTime = new Date();

        // Configuration du ticket
        ParkingType parkingType = ParkingType.valueOf(vehicleType); // Convertir le type
        ParkingSpot parkingSpot = new ParkingSpot(1, parkingType, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        // Calcul et vérification du tarif
        fareCalculatorService.calculateFare(ticket);
        assertEquals(expectedFare, ticket.getPrice(), 0.01,
                "Le tarif calculé est incorrect pour " + vehicleType + " avec une durée de " + durationInMinutes + " minutes.");
    }

    /**
     * Cas où le type de véhicule est inconnu.
     */
    @Test
    public void calculateFareUnkownType() {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000)); // 1h avant
        Date outTime = new Date();

        ParkingSpot parkingSpot = new ParkingSpot(1, null, false); // Type de véhicule non défini
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket),
                "Une exception doit être levée pour un type de véhicule inconnu.");
    }

    /**
     * Cas où l'heure d'entrée est dans le futur (incohérence).
     */
    @Test
    public void calculateFareBikeWithFutureInTime() {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() + (60 * 60 * 1000)); // 1h dans le futur
        Date outTime = new Date();

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket),
                "Une exception doit être levée si l'heure d'entrée est dans le futur.");
    }
    @Test
    public void calculateFareCarWithDiscount() {

        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000)); // il y a 1 heure
        Date outTime = new Date();

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        ticket.setParkingSpot(parkingSpot);
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);


        fareCalculatorService.calculateFare(ticket, true);


        assertEquals(1.425, ticket.getPrice(), 0.01);
    }

    @Test
    public void calculateFareBikeWithDiscount() {

        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000)); // il y a 1 heure
        Date outTime = new Date();

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);
        ticket.setParkingSpot(parkingSpot);
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);


        fareCalculatorService.calculateFare(ticket, true);


        assertEquals(0.95, ticket.getPrice(), 0.01);
    }
}
