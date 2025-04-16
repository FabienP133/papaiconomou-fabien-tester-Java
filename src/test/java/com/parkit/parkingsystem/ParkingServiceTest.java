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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;

import static junit.framework.Assert.*;
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
        try {
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            lenient().when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

            lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicleTest(){
        parkingService.processExitingVehicle();
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }

    @Test
    //1
    public void testProcessIncomingVehicle() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); //j'ai repris la ligne 36 pour saisir la plaque
        when(inputReaderUtil.readSelection()).thenReturn(1);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true); //je dis qu'une place est dispo
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1); //je suis censé mettre quoi dedans ?
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true); // la jai simulé la sauvegarde d'un ticket
        parkingService.processIncomingVehicle();
    }

    //2
    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); //je resaisis la plaque
        Ticket ticket = new Ticket();
        Date inTime = new Date(System.currentTimeMillis() - (60*60*1000)); //1h
        Date outTime = new Date();
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false)); // la ca fait en sorte que la place se libere avec le false
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket); //la je recup le ticket
        when(ticketDAO.updateTicket(ticket)).thenReturn(false);  //la je fais en sorte que l'appel renvoie false
        parkingService.processExitingVehicle(); //la j'appelle la methode processexitingvehicle de la classe parkingservice

    }

    //3
    @Test
    public void testGetNextParkingNumberIfAvailable() {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        ParkingSpot availableSpot = parkingService.getNextParkingNumberIfAvailable(); //là j'appelle la méthode qui est testée
        assertEquals(1, availableSpot.getId());
        assertEquals(ParkingType.CAR, availableSpot.getParkingType());
        assertTrue(availableSpot.isAvailable());
    }

    //4
    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(0);
        ParkingSpot availableSpot = parkingService.getNextParkingNumberIfAvailable();
        assertNull(availableSpot);
    }

    //5
    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        when(inputReaderUtil.readSelection()).thenReturn(3);
        ParkingSpot availableSpot = parkingService.getNextParkingNumberIfAvailable();
        assertNull(availableSpot);
    }


}
