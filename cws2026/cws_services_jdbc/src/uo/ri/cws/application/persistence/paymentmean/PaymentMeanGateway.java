package uo.ri.cws.application.persistence.paymentmean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface PaymentMeanGateway extends Gateway<PaymentMeanGateway.PaymentMeanRecord> {

    @Override
    Optional<PaymentMeanRecord> findById(String id) throws PersistenceException;

    List<PaymentMeanRecord> findByClientId(String clientId) throws PersistenceException;

    boolean existsClient(String clientId) throws PersistenceException;

    boolean hasCharges(String id) throws PersistenceException;

    void addAccumulated(String id, BigDecimal amount) throws PersistenceException;

    void removeForClient(String clientId) throws PersistenceException;

    class PaymentMeanRecord extends Gateway.BaseRecord {
        public String clientId;
        public BigDecimal accumulated;
        public String dtype;
    }
}
