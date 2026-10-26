package uo.ri.cws.application.persistence.card.impl;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.card.CardGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.jdbc.Queries;
import uo.ri.util.exception.PersistenceException;

public class CardGatewayImpl implements CardGateway {

    private final CardRecordAssembler assembler = new CardRecordAssembler();

    @Override
    public void add(CardRecord record) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TCREDITCARDS_ADD"))) {
            pst.setString(1, record.id);
            pst.setString(2, record.type);
            pst.setDate(3, Date.valueOf(record.validThru.atEndOfMonth()));
            pst.setString(4, record.number);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void remove(String id) {
        execute("TCREDITCARDS_DELETE", id);
    }

    @Override
    public void update(CardRecord record) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TCREDITCARDS_UPDATE"))) {
            pst.setString(1, record.type);
            pst.setDate(2, Date.valueOf(record.validThru.atEndOfMonth()));
            pst.setString(3, record.number);
            pst.setString(4, record.id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Optional<CardRecord> findById(String id) {
        return find("TCREDITCARDS_FIND_BY_ID", id);
    }

    @Override
    public Optional<CardRecord> findByNumber(String number) {
        return find("TCREDITCARDS_FIND_BY_NUMBER", number);
    }

    @Override
    public void removeForClient(String clientId) {
        execute("TCREDITCARDS_DELETE_FOR_CLIENT", clientId);
    }

    @Override
    public List<CardRecord> findAll() {
        throw new PersistenceException(new UnsupportedOperationException("Use findById or findByNumber"));
    }

    private Optional<CardRecord> find(String key, String value) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(Queries.getSQLSentence(key))) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                CardRecord record = assembler.toRecord(rs);
                return record == null ? Optional.empty() : Optional.of(record);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private static void execute(String key, String id) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(Queries.getSQLSentence(key))) {
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
