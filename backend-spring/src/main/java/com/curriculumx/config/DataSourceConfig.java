package com.curriculumx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.File;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");

        File dbFile = new File("data/curriculum_records.db");
        if (!dbFile.exists()) {
            File parentDb = new File("../data/curriculum_records.db");
            if (parentDb.exists()) {
                dbFile = parentDb;
            }
        }
        if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }

        dataSource.setUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        System.out.println("📦 Connected to SQLite Database at: " + dbFile.getAbsolutePath());
        return dataSource;
    }
}
