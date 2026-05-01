package com.hit.dao;

import com.hit.dm.Campus;

import java.io.IOException;

/**
 * Read-side DAO for campus data. Implementations decide where the campus
 * comes from (a JSON file on disk, a hard-coded fixture, a future database…).
 */
public interface ICampusDAO {

    /**
     * Loads the campus.
     *
     * @throws IOException if the underlying source can't be read
     */
    Campus load() throws IOException;
}
