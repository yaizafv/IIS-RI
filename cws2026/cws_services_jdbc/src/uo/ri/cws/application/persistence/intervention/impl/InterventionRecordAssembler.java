package uo.ri.cws.application.persistence.intervention.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import uo.ri.cws.application.persistence.intervention.InterventionGateway.InterventionRecord;
import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;

public class InterventionRecordAssembler extends BaseRecordAssembler<InterventionRecord> {

    public InterventionRecordAssembler() {
        super(InterventionRecord::new);
    }

    @Override
    public InterventionRecord toRecord(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        return toCurrentRecord(rs);
    }

    private InterventionRecord toCurrentRecord(ResultSet rs) throws SQLException {
        var record = super.toRecord(rs);

        record.mechanicId = rs.getString("mechanic_id");
        record.workorderId = rs.getString("workorder_id");
        record.date = rs.getTimestamp("date") == null
                ? null : rs.getTimestamp("date").toLocalDateTime();
        record.minutes = rs.getInt("minutes");

        return record;
    }
    
    public List<InterventionRecord> toInterventionRecordList(ResultSet rs) throws SQLException {
        List<InterventionRecord> res = new ArrayList<>();
        while (rs.next()) {
            res.add(toCurrentRecord(rs));
        }
        return res;
    }

}
