package uo.ri.cws.application.persistence.vehicle;

import java.util.List;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface VehicleGateway extends Gateway<VehicleGateway.VehicleRecord> {

    List<VehicleRecord> findByClientId(String clientId) throws PersistenceException;

    class VehicleRecord extends Gateway.BaseRecord {
        public String clientId;
        public String licensePlate;
        public String make;
        public String model;
    }
}
