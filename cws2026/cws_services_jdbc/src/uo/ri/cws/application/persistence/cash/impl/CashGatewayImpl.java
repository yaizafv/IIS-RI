package uo.ri.cws.application.persistence.cash.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.cash.CashGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.util.exception.NotYetImplementedException;
import uo.ri.util.exception.PersistenceException;

public class CashGatewayImpl implements CashGateway {

    @Override
    public void add(CashRecord record) {
        execute("TCASHES_ADD", record.id);
    }

    @Override
    public void remove(String id) {
        execute("TCASHES_DELETE", id);
    }

    @Override
    public void update(CashRecord record) {
        throw new NotYetImplementedException();
    }

    @Override
    public Optional<CashRecord> findById(String id) {
        throw new NotYetImplementedException();
    }

    @Override
    public List<CashRecord> findAll() {
        throw new NotYetImplementedException();
    }

    @Override
    public void removeForClient(String clientId) {
        execute("TCASHES_DELETE_FOR_CLIENT", clientId);
    }

    private static void execute(String key, String value) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(Queries.getSQLSentence(key))) {
            pst.setString(1, value);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

}
