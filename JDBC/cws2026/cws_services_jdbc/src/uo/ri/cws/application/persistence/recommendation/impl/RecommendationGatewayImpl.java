package uo.ri.cws.application.persistence.recommendation.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.recommendation.RecommendationGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.exception.NotYetImplementedException;
import uo.ri.util.exception.PersistenceException;
import uo.ri.util.jdbc.Queries;

public class RecommendationGatewayImpl implements RecommendationGateway {

    private final RecommendationRecordAssembler assembler = new RecommendationRecordAssembler();

    @Override
    public void add(RecommendationRecord record) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TRECOMMENDATIONS_ADD"))) {
            pst.setString(1, record.id);
            pst.setTimestamp(2, Timestamp.valueOf(record.createdAt));
            pst.setTimestamp(3, Timestamp.valueOf(record.updatedAt));
            pst.setString(4, record.entityState);
            pst.setBoolean(5, record.usedForVoucher);
            pst.setLong(6, record.version);
            pst.setString(7, record.recommenderId);
            pst.setString(8, record.recommendedId);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void remove(String id) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TRECOMMENDATIONS_DELETE"))) {
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void update(RecommendationRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public Optional<RecommendationRecord> findById(String id) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<RecommendationRecord> findAll() throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<RecommendationRecord> findByClientId(String clientId) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TRECOMMENDATIONS_FIND_BY_CLIENT_ID"))) {
            pst.setString(1, clientId);
            pst.setString(2, clientId);
            try (ResultSet rs = pst.executeQuery()) {
                return assembler.toRecordList(rs);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
