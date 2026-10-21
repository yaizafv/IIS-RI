package uo.ri.cws.application.persistence.cash;

import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface CashGateway extends Gateway<CashGateway.CashRecord> {

    void removeForClient(String clientId) throws PersistenceException;

    class CashRecord extends Gateway.BaseRecord {
    }
}
