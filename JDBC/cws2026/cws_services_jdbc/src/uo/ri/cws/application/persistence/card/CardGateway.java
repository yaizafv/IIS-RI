package uo.ri.cws.application.persistence.card;

import java.time.YearMonth;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;

public interface CardGateway extends Gateway<CardGateway.CardRecord> {

    class CardRecord extends PaymentMeanRecord {
        public String number;
        public String type;
        public YearMonth validThru;
    }
}
