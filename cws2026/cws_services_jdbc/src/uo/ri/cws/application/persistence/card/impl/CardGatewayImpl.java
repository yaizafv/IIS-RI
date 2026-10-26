package uo.ri.cws.application.persistence.card.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.card.CardGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.exception.NotYetImplementedException;
import uo.ri.util.exception.PersistenceException;
import uo.ri.util.jdbc.Queries;

public class CardGatewayImpl implements CardGateway {

    @Override
    public void add(CardRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public void remove(String id) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TCREDITCARDS_DELETE"))) {
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void update(CardRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public Optional<CardRecord> findById(String id) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<CardRecord> findAll() throws PersistenceException {
        throw new NotYetImplementedException();
    }
}
