package uo.ri.cws.application.persistence.vehicle.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;
import uo.ri.cws.application.persistence.vehicle.VehicleGateway.VehicleRecord;

public class VehicleRecordAssembler extends BaseRecordAssembler<VehicleRecord> {

    public VehicleRecordAssembler() {
    }

    @Override
    protected VehicleRecord createRecord() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public VehicleRecord toRecord(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        return toCurrentRecord(rs);
    }

    private VehicleRecord toCurrentRecord(ResultSet rs) throws SQLException {
        var record = super.toRecord(rs);
        record.id = rs.getString("id");
        record.version = rs.getLong("version");
        record.createdAt = rs.getTimestamp("createdAt").toLocalDateTime();
        record.updatedAt = rs.getTimestamp("updatedAt").toLocalDateTime();
        record.entityState = rs.getString("entityState");
        record.clientId = rs.getString("client_id");
        record.licensePlate = rs.getString("licensePlate");
        record.make = rs.getString("make");
        record.model = rs.getString("model");
        
        return record;
    }

}
