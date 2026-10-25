package uo.ri.cws.application.persistence.card;

import java.time.YearMonth;
import java.util.Optional;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;
import uo.ri.util.exception.PersistenceException;

public interface CardGateway extends Gateway<CardGateway.CardRecord> {

    Optional<CardRecord> findByNumber(String number) throws PersistenceException;

    void removeForClient(String clientId) throws PersistenceException;

    class CardRecord extends PaymentMeanRecord {
        public String number;
        public String type;
        public YearMonth validThru;
    }
}
