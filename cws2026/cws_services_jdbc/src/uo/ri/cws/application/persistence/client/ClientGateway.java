package uo.ri.cws.application.persistence.client;

import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface ClientGateway extends Gateway<ClientGateway.ClientRecord> {

    Optional<ClientRecord> findByNif(String nif) throws PersistenceException;

    Optional<ClientRecord> findOtherByNif(String nif, String id)
            throws PersistenceException;

    List<ClientRecord> findRecommendedBy(String recommenderNif)
            throws PersistenceException;

    boolean hasVehicles(String clientId) throws PersistenceException;

    boolean hasPaidInvoices(String clientId) throws PersistenceException;

    class ClientRecord extends Gateway.BaseRecord {
        public String nif;
        public String name;
        public String surname;
        public String street;
        public String city;
        public String country;
        public String postalCode;
        public String phone;
        public String email;
    }
}
