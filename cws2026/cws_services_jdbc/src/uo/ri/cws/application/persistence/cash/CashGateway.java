package uo.ri.cws.application.persistence.cash;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;

public interface CashGateway extends Gateway<CashGateway.CashRecord> {

    public class CashRecord extends PaymentMeanRecord {
    }
}
