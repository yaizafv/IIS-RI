package uo.ri.cws.application.persistence.paymentmean;

import java.math.BigDecimal;
import java.util.List;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface PaymentMeanGateway extends Gateway<PaymentMeanGateway.PaymentMeanRecord> {

    List<PaymentMeanRecord> findByClientId(String clientId) throws PersistenceException;

    class PaymentMeanRecord extends Gateway.BaseRecord {
        public String clientId;
        public BigDecimal accumulated;
        public String dtype;
    }
}
