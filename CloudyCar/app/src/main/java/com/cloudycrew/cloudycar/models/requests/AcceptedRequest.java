package com.cloudycrew.cloudycar.models.requests;

import com.cloudycrew.cloudycar.models.User;

/**
 * Created by George on 2016-10-13.
 */

public class AcceptedRequest extends Request {
    private User driver;

    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
    }
}
