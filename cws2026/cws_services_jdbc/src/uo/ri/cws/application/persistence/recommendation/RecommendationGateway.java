package uo.ri.cws.application.persistence.recommendation;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface RecommendationGateway
        extends Gateway<RecommendationGateway.RecommendationRecord> {

    void add(String recommenderId, String recommendedId)
            throws PersistenceException;

    void removeForClient(String clientId) throws PersistenceException;

    class RecommendationRecord extends Gateway.BaseRecord {
        public String recommenderId;
        public String recommendedId;
        public boolean usedForVoucher;
    }
}
