package uo.ri.cws.application.persistence.util.assembler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import uo.ri.cws.application.persistence.Gateway.BaseRecord;

public abstract class BaseRecordAssembler<T extends BaseRecord> {

    protected abstract T createRecord();

    public List<T> toRecordList(ResultSet rs) throws SQLException {
        List<T> res = new ArrayList<>();
        while (rs.next()) {
            res.add(toRecord(rs));
        }
        return res;
    }

    public T toRecord(ResultSet m) throws SQLException {
        T dto = createRecord();
        dto.id = m.getString("id");
        dto.version = m.getLong("version");
        dto.entityState = m.getString("entityState");
        if (m.getTimestamp("createdAt") != null) {
            dto.createdAt = m.getTimestamp("createdAt").toLocalDateTime();
        }
        if (m.getTimestamp("updatedAt") != null) {
            dto.updatedAt = m.getTimestamp("updatedAt").toLocalDateTime();
        }
        return dto;
    }

}
