package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    public void setUpPerTest() {
        // Reduit au setup minimal pour resoudre les probleme de stubbing
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }

    @Test
    public void processExitingVehicleTest() throws Exception {
        // Arrange
        Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setInTime(new Date());
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket); // Récupération du ticket
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(1); // Client non récurrent
        when(ticketDAO.updateTicket(ticket)).thenReturn(true); // Mise à jour OK
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true); // Libération OK

        // Act
        parkingService.processExitingVehicle();

        // Assert
        verify(ticketDAO, times(1)).getTicket("ABCDEF"); // Vérification de l'accès au ticket
        verify(ticketDAO, times(1)).updateTicket(ticket); // Vérification de la mise à jour
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class)); // Vérification de la libération de la place
    }

    @Test
    public void testProcessIncomingVehicle() throws Exception {
        // Arrange
        when(inputReaderUtil.readSelection()).thenReturn(1); // Choix 'CAR'
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1); // Place disponible
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // Plaque saisie
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true); // Sauvegarde ticket OK

        // Act
        parkingService.processIncomingVehicle();

        // Assert
        verify(inputReaderUtil, times(1)).readSelection();
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR); // Vérification de la recherche de place
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class)); // Vérification de la réservation de place
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class)); // Vérification de la sauvegarde du ticket

    }

    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        // Arrange
        Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setInTime(new Date());
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // Plaque saisie
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket); // Récupération ticket valide
        when(ticketDAO.updateTicket(ticket)).thenReturn(false); // Mise à jour échoue

        // Act
        parkingService.processExitingVehicle();

        // Assert
        verify(ticketDAO, times(1)).getTicket("ABCDEF"); // Vérif que le ticket est récupéré
        verify(ticketDAO, times(1)).updateTicket(ticket); // Mise à jour tentée
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class)); // Pas de libération de place
    }

    @Test
    public void testGetNextParkingNumberIfAvailable() {
        // Arrange
        when(inputReaderUtil.readSelection()).thenReturn(1); // Choix 'CAR'
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1); // Place dispo

        // Act
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // Assert
        assertNotNull(parkingSpot); // Une place est trouvée
        assertEquals(1, parkingSpot.getId()); // Vérification de l'ID de la place
        assertTrue(parkingSpot.isAvailable()); // Vérif que la place est disponible
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        // Arrange
        when(inputReaderUtil.readSelection()).thenReturn(1); // Choix 'CAR'
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(-1); // Aucune place dispo

        // Act
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // Assert
        assertNull(parkingSpot); // Vérifie que la méthode retourne null
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        // Arrange
        when(inputReaderUtil.readSelection()).thenReturn(-1); // Mauvaise saisie

        // Act
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        // Assert
        assertNull(result); // Vérifie que la saisie incorrecte retourne null
    }
}
