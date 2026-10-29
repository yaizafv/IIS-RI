package uo.ri.cws.application.persistence.vehicle.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;
import uo.ri.cws.application.persistence.vehicle.VehicleGateway.VehicleRecord;

public class VehicleRecordAssembler extends BaseRecordAssembler<VehicleRecord> {

    @Override
    protected VehicleRecord createRecord() {
        return new VehicleRecord();
    }

    @Override
    public VehicleRecord toRecord(ResultSet rs) throws SQLException {
        VehicleRecord record = super.toRecord(rs);
        record.clientId = rs.getString("client_id");
        record.licensePlate = rs.getString("licensePlate");
        record.make = rs.getString("make");
        record.model = rs.getString("model");
        return record;
    }
}
