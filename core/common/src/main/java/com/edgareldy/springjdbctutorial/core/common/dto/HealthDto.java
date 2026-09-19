package com.edgareldy.springjdbctutorial.core.common.dto;

import java.util.Objects;

/**
 * Business shape of the health check result.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class HealthDto {

    private String status;
    private String database;

    public HealthDto() {
    }

    public HealthDto(String status, String database) {
        this.status = status;
        this.database = database;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HealthDto that = (HealthDto) o;
        return Objects.equals(status, that.status) && Objects.equals(database, that.database);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, database);
    }

    @Override
    public String toString() {
        return "HealthDto{status='" + status + "', database='" + database + "'}";
    }
}
