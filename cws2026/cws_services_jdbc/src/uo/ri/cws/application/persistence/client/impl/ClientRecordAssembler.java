package uo.ri.cws.application.persistence.client.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import uo.ri.cws.application.persistence.client.ClientGateway.ClientRecord;
import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;

public class ClientRecordAssembler extends BaseRecordAssembler<ClientRecord> {

    @Override
    protected ClientRecord createRecord() {
        return new ClientRecord();
    }

    @Override
    public ClientRecord toRecord(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        return toCurrentRecord(rs);
    }

    @Override
    public List<ClientRecord> toRecordList(ResultSet rs) throws SQLException {
        List<ClientRecord> result = new ArrayList<>();
        while (rs.next()) {
            result.add(toCurrentRecord(rs));
        }
        return result;
    }

    ClientRecord toCurrentRecord(ResultSet rs) throws SQLException {
        ClientRecord record = super.toRecord(rs);
        record.nif = rs.getString("nif");
        record.name = rs.getString("name");
        record.surname = rs.getString("surname");
        record.street = rs.getString("street");
        record.city = rs.getString("city");
        record.country = rs.getString("country");
        record.postalCode = rs.getString("postalCode");
        record.phone = rs.getString("phone");
        record.email = rs.getString("email");
        return record;
    }
}
