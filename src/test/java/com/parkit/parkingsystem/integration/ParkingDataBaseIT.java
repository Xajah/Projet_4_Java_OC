package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    private ParkingService parkingService;

    @BeforeAll
    public static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // Plaque d'immatriculation
        dataBasePrepareService.clearDataBaseEntries();

        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }

    @AfterAll
    public static void tearDown() {
        // Pas d'opérations spécifiques pour le teardown ici
    }
    @Test
    public void testParkingACar() {
        when(inputReaderUtil.readSelection()).thenReturn(1);  // 1 = Car
// Act
        parkingService.processIncomingVehicle();

// Assert
// Vérifie qu'un ticket a bien été sauvegardé
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket);
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertNotNull(ticket.getInTime());
        assertNull(ticket.getOutTime());

// Vérifie que la place est mise à jour (non disponible)
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertFalse(parkingSpotDAO.getNextAvailableSlot(parkingSpot.getParkingType()) == parkingSpot.getId());
    }

    /**
     * Vérifie que le prix et l'heure de sortie sont renseignés lors de la sortie d'un véhicule.
     */




    /**
     * Vérifie qu'un ticket est enregistré et qu'une place est rendue indisponible.
     */
    @Test
    public void testParkingLotExit() {
        // Arrange : Demander la prochaine place de parking disponible
        ParkingType parkingType = ParkingType.CAR; //On simule une voiture
        int nextAvailableSpotId = parkingSpotDAO.getNextAvailableSlot(parkingType); // Récupère la prochaine place libre
        assertTrue(nextAvailableSpotId > 0, "Aucune place de parking disponible");

        // Créer l'objet ParkingSpot pour cette place
        ParkingSpot parkingSpot = new ParkingSpot(nextAvailableSpotId, parkingType, false); // Place occupée
        boolean isParkingUpdated = parkingSpotDAO.updateParking(parkingSpot); // Rendre cette place "indisponible"
        assertTrue(isParkingUpdated, "La mise à jour de la disponibilité de la place a échoué");

        //Créer un ticket avec les caractéristiques précises pour le test
        Ticket testTicket = new Ticket();
        testTicket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false)); // Place de parking occupée
        testTicket.setVehicleRegNumber("ABCDEF"); // Immatriculation du véhicule
        testTicket.setPrice(0); // Prix initial


        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 heure avant maintenant
        testTicket.setInTime(inTime); // Configure le InTime pour la simulation d'entrée
        testTicket.setOutTime(null); // Pas encore de OutTime au départ

        // Sauvegarder le ticket dans la base de données
        boolean isSaved = ticketDAO.saveTicket(testTicket);
        // Valider que la sauvegarde a bien eu lieu
        assertTrue(isSaved, "The test ticket was not saved in the database");

        //  Simuler la sortie via ParkingService
        parkingService.processExitingVehicle();

        // Vérification données mises à jour dans la base après la sortie du V
        Ticket ticketInDb = ticketDAO.getTicket("ABCDEF"); // Récupérer le ticket mis à jour
        assertEquals(1, parkingSpotDAO.getNextAvailableSlot(ticketInDb.getParkingSpot().getParkingType()));

        double hourlyRate = ticketInDb.getParkingSpot().getParkingType() == ParkingType.CAR ? 1.5 : 1.0;
        assertEquals(hourlyRate, ticketInDb.getPrice(), 0.01, "The calculated price for the ticket is incorrect");
        // Vérifier que la place de parking associée est maintenant libérée

    }


    /**
     * récurrent bénéficie de 5% de remise.
     */
    @Test
    public void testParkingLotExitRecurringUser () throws  Exception {
        // Arrange

        // Simule un premier passage
        ParkingType parkingType = ParkingType.CAR; //On simule une voiture
        int nextAvailableSpotId = parkingSpotDAO.getNextAvailableSlot(parkingType); // Récupère la prochaine place libre
        assertTrue(nextAvailableSpotId > 0, "Aucune place de parking disponible");

        // Créer l'objet ParkingSpot pour cette place
        ParkingSpot parkingSpot = new ParkingSpot(nextAvailableSpotId, parkingType, false); // Place occupée
        boolean isParkingUpdated = parkingSpotDAO.updateParking(parkingSpot); // Rendre cette place "indisponible"
        assertTrue(isParkingUpdated, "La mise à jour de la disponibilité de la place a échoué");

        //Créer un ticket avec les caractéristiques précises pour le test
        Ticket testTicket = new Ticket();
        testTicket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false)); // Place de parking occupée
        testTicket.setVehicleRegNumber("ABCDEF"); // Immatriculation du véhicule
        testTicket.setPrice(0); // Prix initial


        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 heure avant maintenant
        testTicket.setInTime(inTime); // Configure le InTime pour la simulation d'entrée
        testTicket.setOutTime(null); // Pas encore de OutTime au départ

        // Sauvegarder le ticket dans la base de données
        boolean isSaved = ticketDAO.saveTicket(testTicket);
        // Valider que la sauvegarde a bien eu lieu
        assertTrue(isSaved, "The test ticket was not saved in the database");
        parkingService.processExitingVehicle();

        // Simule un deuxième passage du même utilisateur
        ParkingType parkingType2 = ParkingType.CAR; //On simule une voiture
        int nextAvailableSpotId2 = parkingSpotDAO.getNextAvailableSlot(parkingType); // Récupère la prochaine place libre
        assertTrue(nextAvailableSpotId2 > 0, "Aucune place de parking disponible");

        // Créer l'objet ParkingSpot pour cette place
        ParkingSpot parkingSpot2 = new ParkingSpot(nextAvailableSpotId, parkingType, false); // Place occupée
        boolean isParkingUpdated2 = parkingSpotDAO.updateParking(parkingSpot2); // Rendre cette place "indisponible"
        assertTrue(isParkingUpdated2, "La mise à jour de la disponibilité de la place a échoué");

        //Créer un ticket avec les caractéristiques précises pour le test
        Ticket testTicket2 = new Ticket();
        testTicket2.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false)); // Place de parking occupée
        testTicket2.setVehicleRegNumber("ABCDEF"); // Immatriculation du véhicule
        testTicket2.setPrice(0); // Prix initial


        Date inTime2 = new Date();
        inTime2.setTime(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 heure avant maintenant
        testTicket2.setInTime(inTime); // Configure le InTime pour la simulation d'entrée
        testTicket2.setOutTime(null); // Pas encore de OutTime au départ

        // Sauvegarder le ticket dans la base de données
        boolean isSaved2 = ticketDAO.saveTicket(testTicket);
        // Valider que la sauvegarde a bien eu lieu
        assertTrue(isSaved2, "The test ticket was not saved in the database");
        parkingService.processExitingVehicle();

        // Assert
        // Vérifie que la remise de 5% a été appliquée
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket.getOutTime());
        assertEquals(1.43, ticket.getPrice(), 0.01); // À 5% près
    }
}