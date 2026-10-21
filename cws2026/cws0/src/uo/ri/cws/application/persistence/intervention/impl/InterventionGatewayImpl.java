package uo.ri.cws.application.persistence.intervention.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.intervention.InterventionGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.util.exception.PersistenceException;

public class InterventionGatewayImpl implements InterventionGateway {
    
    private final InterventionRecordAssembler assembler = new InterventionRecordAssembler();

    @Override
    public void add(InterventionRecord t) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TINTERVENTIONS_ADD"))) {
            pst.setString(1, t.id);
            pst.setLong(2, t.version);
            pst.setTimestamp(3, t.createdAt == null ? null : Timestamp.valueOf(t.createdAt));
            pst.setTimestamp(4, t.updatedAt == null ? null : Timestamp.valueOf(t.updatedAt));
            pst.setString(5, t.entityState);
            pst.setString(6, t.mechanicId);
            pst.setString(7, t.workorderId);
            pst.setTimestamp(8, t.date == null ? null : new Timestamp(t.date.getTime()));
            pst.setInt(9, t.minutes);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void remove(String id) throws PersistenceException {
        executeUpdate("TINTERVENTIONS_REMOVE", id);
    }

    @Override
    public void update(InterventionRecord t) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TINTERVENTIONS_UPDATE"))) {
            pst.setString(1, t.mechanicId);
            pst.setString(2, t.workorderId);
            pst.setTimestamp(3, t.date == null ? null : new Timestamp(t.date.getTime()));
            pst.setInt(4, t.minutes);
            pst.setTimestamp(5, t.updatedAt == null ? null : Timestamp.valueOf(t.updatedAt));
            pst.setString(6, t.id);
            pst.setLong(7, t.version);
            if (pst.executeUpdate() != 1) {
                throw new PersistenceException(new SQLException(
                        "Intervention was not updated because its version has changed"));
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Optional<InterventionRecord> findById(String id) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TINTERVENTIONS_FIND_BY_ID"))) {
            pst.setString(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                return Optional.ofNullable(assembler.toRecord(rs));
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public List<InterventionRecord> findAll() throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TINTERVENTIONS_FIND_ALL"));
             ResultSet rs = pst.executeQuery()) {
            return assembler.toInterventionRecordList(rs);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public List<InterventionRecord> findByMechanicId(String id) throws PersistenceException {
        Connection c = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            c = Jdbc.getCurrentConnection();
            pst = c.prepareStatement(
                Queries.getSQLSentence("TINTERVENTIONS_FIND_BY_MECHANIC_ID"));
            pst.setString(1, id);
            rs = pst.executeQuery();
            return assembler.toInterventionRecordList(rs);

        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignored) {}
            try {
                if (pst != null) {
                    pst.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    private void executeUpdate(String queryKey, String id) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence(queryKey))) {
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

}
