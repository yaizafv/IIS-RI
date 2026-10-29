package uo.ri.cws.application.persistence.intervention;

import java.time.LocalDateTime;
import java.util.List;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface InterventionGateway extends Gateway<InterventionGateway.InterventionRecord> {

    List<InterventionRecord> findByMechanicId(String id) throws PersistenceException;

    public static class InterventionRecord extends Gateway.BaseRecord {
        public String id;
        public long version;

        public String workorderId;
        public String mechanicId;
        public LocalDateTime date;
        public int minutes;
    }

}
