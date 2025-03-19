package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) {

        if (ticket.getOutTime() == null || ticket.getOutTime().before(ticket.getInTime())) {
            throw new IllegalArgumentException("Out time provided is incorrect: " + ticket.getOutTime());
        }

        // Calculate time difference in milliseconds
        long inTimeMillis = ticket.getInTime().getTime();
        long outTimeMillis = ticket.getOutTime().getTime();
        double durationMillis = outTimeMillis - inTimeMillis;
        double durationMinutes = durationMillis / (1000 * 60); // ms -> seconds -> minutes

        if (durationMinutes <= 30) {
            ticket.setPrice(0); // Free parking if duration is <= 30 minutes
            return;
        }
        // Convert duration to hours
        double durationHours = durationMillis / (1000 * 60 * 60); // ms -> seconds -> minutes -> hours

        // Calculate fare based on parking type
        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                ticket.setPrice(durationHours * Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                ticket.setPrice(durationHours * Fare.BIKE_RATE_PER_HOUR);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown Parking Type");
            }
        }
    }

}