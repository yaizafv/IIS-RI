package uo.ri.cws.application.persistence.util.jdbc;

import uo.ri.util.jdbc.PropertyFile;

public class Queries {
    private static final PropertyFile config = new PropertyFile(
            "queries.properties");

    public static String getSQLSentence(String key) {
        return config.getRequired(key);
    }
}