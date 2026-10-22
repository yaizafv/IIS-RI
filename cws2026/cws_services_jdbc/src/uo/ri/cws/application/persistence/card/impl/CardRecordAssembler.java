package uo.ri.cws.application.persistence.card.impl;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;

import uo.ri.cws.application.persistence.card.CardGateway.CardRecord;

public class CardRecordAssembler {

    public CardRecord toRecord(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        return toCurrentRecord(rs);
    }

    private CardRecord toCurrentRecord(ResultSet rs) throws SQLException {
        CardRecord record = new CardRecord();
        record.id = rs.getString("id");
        record.number = rs.getString("number");
        record.type = rs.getString("type");
        Date date = rs.getDate("validThru");
        record.validThru = date == null ? null : YearMonth.from(date.toLocalDate());
        return record;
    }
}
