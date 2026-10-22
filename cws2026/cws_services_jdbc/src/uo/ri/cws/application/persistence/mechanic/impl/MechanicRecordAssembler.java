package uo.ri.cws.application.persistence.mechanic.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import uo.ri.cws.application.persistence.mechanic.MechanicGateway.MechanicRecord;
import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;

public class MechanicRecordAssembler extends BaseRecordAssembler<MechanicRecord> {

    public MechanicRecordAssembler() {
    }

    @Override
    protected MechanicRecord createRecord() {
        return new MechanicRecord();
    }

    @Override
    public MechanicRecord toRecord(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        return toCurrentRecord(rs);
    }

    private MechanicRecord toCurrentRecord(ResultSet rs) throws SQLException {
        var record = super.toRecord(rs);
        record.nif = rs.getString("nif");
        record.name = rs.getString("name");
        record.surname = rs.getString("surname");

        return record;
    }

    public List<MechanicRecord> toMechanicRecordList(ResultSet rs)
        throws SQLException {
        List<MechanicRecord> res = new ArrayList<>();
        while (rs.next()) {
            res.add(toCurrentRecord(rs));
        }
        return res;
    }
}
