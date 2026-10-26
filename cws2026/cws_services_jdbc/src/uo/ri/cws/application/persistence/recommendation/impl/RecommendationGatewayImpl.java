package uo.ri.cws.application.persistence.recommendation.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uo.ri.cws.application.persistence.recommendation.RecommendationGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.jdbc.Queries;
import uo.ri.util.exception.NotYetImplementedException;
import uo.ri.util.exception.PersistenceException;

public class RecommendationGatewayImpl implements RecommendationGateway {

    @Override
    public void add(String recommenderId, String recommendedId) {
        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TRECOMMENDATIONS_ADD"))) {
            pst.setString(1, UUID.randomUUID().toString());
            pst.setTimestamp(2, Timestamp.valueOf(now));
            pst.setTimestamp(3, Timestamp.valueOf(now));
            pst.setString(4, "ENABLED");
            pst.setBoolean(5, false);
            pst.setLong(6, 1);
            pst.setString(7, recommenderId);
            pst.setString(8, recommendedId);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void removeForClient(String clientId) {
        execute(clientId, clientId);
    }

    @Override
    public void add(RecommendationRecord record) {
        add(record.recommenderId, record.recommendedId);
    }

    @Override
    public void remove(String id) {
        throw new NotYetImplementedException();
    }

    @Override
    public void update(RecommendationRecord record) {
        throw new NotYetImplementedException();
    }

    @Override
    public Optional<RecommendationRecord> findById(String id) {
        throw new NotYetImplementedException();
    }

    @Override
    public List<RecommendationRecord> findAll() {
        throw new NotYetImplementedException();
    }

    private static void execute(String recommenderId, String recommendedId) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TRECOMMENDATIONS_DELETE_FOR_CLIENT"))) {
            pst.setString(1, recommenderId);
            pst.setString(2, recommendedId);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
