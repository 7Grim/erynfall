package com.osrs.auth.db;

import com.osrs.auth.config.AuthSettings;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class Db {

    private final AuthSettings settings;

    public Db(AuthSettings settings) {
        this.settings = settings;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(settings.dbUrl(), settings.dbUser(), settings.dbPassword());
    }
}
