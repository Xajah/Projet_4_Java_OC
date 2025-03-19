package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {


    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false); // Pas de réduction par défaut
    }


    public void calculateFare(Ticket ticket, boolean discount) {

        // Validation des heures d'entrée et de sortie
        if (ticket.getOutTime() == null || ticket.getOutTime().before(ticket.getInTime())) {
            throw new IllegalArgumentException("Out time provided is incorrect: " + ticket.getOutTime());
        }

        // Calcul de la durée en millisecondes
        long inTimeMillis = ticket.getInTime().getTime();
        long outTimeMillis = ticket.getOutTime().getTime();
        double durationMillis = outTimeMillis - inTimeMillis;
        double durationMinutes = durationMillis / (1000 * 60); // Conversion ms -> minutes

        // Gratuité si la durée <= 30 minutes
        if (durationMinutes <= 30) {
            ticket.setPrice(0); // Gratuit
            return;
        }

        // Calcul de la durée en heures
        double durationHours = durationMillis / (1000 * 60 * 60); // Conversion ms -> heures

        // Calcul du tarif en fonction du type de parking
        double totalFare;
        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                totalFare = durationHours * Fare.CAR_RATE_PER_HOUR; // Tarif pour voiture
                break;
            }
            case BIKE: {
                totalFare = durationHours * Fare.BIKE_RATE_PER_HOUR; // Tarif pour moto
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown Parking Type");
            }
        }

        // Appliquer la réduction de 5 % si `discount` est true
        if (discount) {
            totalFare *= 0.95;
        }

        ticket.setPrice(totalFare); // Enregistrer le tarif final dans le ticket
    }
}