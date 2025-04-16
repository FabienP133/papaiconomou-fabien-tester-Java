package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
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


import static junit.framework.Assert.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){

    }

    @Test
    public void testParkingACar() throws Exception { //la méthode vérifie que l'appel de processincomveh se déroule correctement et qu'un ticket est créé dans la bdd
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); //saisie de la plaque
        when(inputReaderUtil.readSelection()).thenReturn(1); //la je dis que le type de véhicule est une voiture
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF"); //je récupère le ticket pour la plaque ABCDEF
        ParkingSpot spot = ticket.getParkingSpot();
        assertFalse("La place de parking doit être occupée", spot.isAvailable());
        //TODO: check that a ticket is actually saved in DB and Parking table is updated with availability
    }

    @Test
    public void testParkingLotExit() throws Exception {
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull("La date de sortie doit être renseignée", ticket.getOutTime());
        //TODO: check that the fare generated and out time are populated correctly in the database
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        String vehicleRegNumber = "ABCDEF";
        Ticket ticket1 = new Ticket();
        ticket1.setVehicleRegNumber(vehicleRegNumber);
        ticket1.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        Date inTime1 = new Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000);
        ticket1.setInTime(inTime1);
        Date outTime1 = new Date(inTime1.getTime() + 60 * 60 * 1000);
        ticket1.setOutTime(outTime1);
        ticketDAO.saveTicket(ticket1);

        Ticket ticket2 = new Ticket();
        ticket2.setVehicleRegNumber(vehicleRegNumber);
        ticket2.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        Date inTime2 = new Date(System.currentTimeMillis() - 60 * 60 * 1000);
        ticket2.setInTime(inTime2);
        Date outTime2 = new Date (inTime2.getTime() + 60 * 60 * 1000);
        ticket2.setOutTime(outTime2);
        ticketDAO.saveTicket(ticket2);

        parkingService.processExitingVehicle();

        Ticket updatedTicket = ticketDAO.getTicket(vehicleRegNumber);
        assertNotNull("Le ticket n'est pas null", updatedTicket);
        assertNotNull("La date de sortie doit être renseignée", updatedTicket.getOutTime());

        double expectedFare = Fare.CAR_RATE_PER_HOUR * 0.95;
        assertEquals("Le tarif pour un client récurent = 95% du plein tarif", expectedFare);

    }



}
