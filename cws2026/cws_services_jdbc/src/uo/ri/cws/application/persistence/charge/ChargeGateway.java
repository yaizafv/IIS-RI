package uo.ri.cws.application.persistence.charge;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface ChargeGateway extends Gateway<ChargeGateway.ChargeRecord> {

    class ChargeRecord extends Gateway.BaseRecord {
        public BigDecimal charge;
        public String invoiceId;
        public String paymentMeanId;
        public LocalDateTime date;
    }

    void add(String invoiceId, String paymentMeanId, BigDecimal amount)
            throws PersistenceException;

    void removeForClient(String clientId) throws PersistenceException;
}
