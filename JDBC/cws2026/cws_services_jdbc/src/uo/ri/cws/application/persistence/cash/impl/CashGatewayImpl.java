package uo.ri.cws.application.persistence.cash.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.cash.CashGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.exception.NotYetImplementedException;
import uo.ri.util.exception.PersistenceException;
import uo.ri.util.jdbc.Queries;

public class CashGatewayImpl implements CashGateway {

    @Override
    public void add(CashRecord record) throws PersistenceException {
        execute("TCASHES_ADD", record.id);
    }

    @Override
    public void remove(String id) throws PersistenceException {
        execute("TCASHES_DELETE", id);
    }

    @Override
    public void update(CashRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public Optional<CashRecord> findById(String id) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<CashRecord> findAll() throws PersistenceException {
        throw new NotYetImplementedException();
    }

    private static void execute(String queryKey, String id) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(Queries.getSQLSentence(queryKey))) {
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
