package uo.ri.cws.application.persistence.cash;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;
import uo.ri.util.exception.PersistenceException;

public interface CashGateway extends Gateway<CashGateway.CashRecord> {

    void removeForClient(String clientId) throws PersistenceException;

    public class CashRecord extends PaymentMeanRecord {
    }
}
