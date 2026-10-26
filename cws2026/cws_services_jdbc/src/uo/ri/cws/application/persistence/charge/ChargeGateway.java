package uo.ri.cws.application.persistence.charge;

import java.math.BigDecimal;
import java.util.List;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface ChargeGateway extends Gateway<ChargeGateway.ChargeRecord> {

    List<ChargeRecord> findByPaymentMeanId(String paymentMeanId) throws PersistenceException;

    class ChargeRecord extends Gateway.BaseRecord {
        public BigDecimal charge;
        public String invoiceId;
        public String paymentMeanId;
    }
}
