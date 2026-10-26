package uo.ri.cws.application.persistence.vehicle.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.jdbc.Queries;
import uo.ri.cws.application.persistence.vehicle.VehicleGateway;
import uo.ri.util.exception.NotYetImplementedException;
import uo.ri.util.exception.PersistenceException;

public class VehicleGatewayImpl implements VehicleGateway {
    
    private final VehicleRecordAssembler assembler = new VehicleRecordAssembler();

    @Override
    public void add(VehicleRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public void remove(String id) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public void update(VehicleRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public Optional<VehicleRecord> findById(String id) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<VehicleRecord> findAll() throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<VehicleRecord> findByClientId(String clientId) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TVEHICLES_FIND_BY_CLIENT_ID"))) {
            pst.setString(1, clientId);
            try (ResultSet rs = pst.executeQuery()) {
                List<VehicleRecord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(assembler.toRecord(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

}
