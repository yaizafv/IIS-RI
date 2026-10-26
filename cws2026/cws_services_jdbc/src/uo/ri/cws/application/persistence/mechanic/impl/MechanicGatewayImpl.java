package uo.ri.cws.application.persistence.mechanic.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.mechanic.MechanicGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.exception.PersistenceException;
import uo.ri.util.jdbc.Queries;

public class MechanicGatewayImpl implements MechanicGateway {

    private final MechanicRecordAssembler assembler = new MechanicRecordAssembler();

    @Override
    public void add(MechanicRecord record) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TMECHANICS_ADD"))) {
            pst.setString(1, record.id);
            pst.setString(2, record.nif);
            pst.setString(3, record.name);
            pst.setString(4, record.surname);
            pst.setLong(5, record.version);
            pst.setTimestamp(6, Timestamp.valueOf(record.createdAt));
            pst.setTimestamp(7, Timestamp.valueOf(record.updatedAt));
            pst.setString(8, record.entityState);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void remove(String id) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TMECHANICS_REMOVE"))) {
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void update(MechanicRecord record) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TMECHANICS_UPDATE"))) {
            pst.setString(1, record.name);
            pst.setString(2, record.surname);
            pst.setString(3, record.nif);
            pst.setTimestamp(4, Timestamp.valueOf(record.updatedAt));
            pst.setString(5, record.id);
            pst.setLong(6, record.version);
            if (pst.executeUpdate() != 1) {
                throw new PersistenceException(
                    new SQLException("Mechanic was not updated because its version has changed"));
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Optional<MechanicRecord> findById(String id) throws PersistenceException {
        return findOne("TMECHANICS_FIND_BY_ID", id);
    }

    @Override
    public List<MechanicRecord> findAll() throws PersistenceException {
        try (
            PreparedStatement pst = Jdbc.getCurrentConnection()
                .prepareStatement(Queries.getSQLSentence("TMECHANICS_FIND_ALL"));
            ResultSet rs = pst.executeQuery()) {
            return assembler.toMechanicRecordList(rs);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Optional<MechanicRecord> findByNif(String nif) throws PersistenceException {
        return findOne("TMECHANICS_FIND_BY_NIF", nif);
    }

    private Optional<MechanicRecord> findOne(String key, String value) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence(key))) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                return Optional.ofNullable(assembler.toRecord(rs));
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}