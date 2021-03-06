package com.cloudycrew.cloudycar.users;

import com.cloudycrew.cloudycar.elasticsearch.IElasticSearchService;
import com.cloudycrew.cloudycar.email.Email;
import com.cloudycrew.cloudycar.models.phonenumbers.PhoneNumber;
import com.cloudycrew.cloudycar.models.User;
import java.util.List;

/**
 * Created by Ryan on 2016-11-08.
 *
 * The service for fetching, creating, and updating users. Constructs queries through the elastic
 * search service to do this and also uses user preferences to update the currently logged in user
 * locally.
 */

public class UserService implements IUserService
{
    private IElasticSearchService<User> elasticSearchService;
    private IUserPreferences userPrefs;

    public UserService(IElasticSearchService<User> elasticSearchService, IUserPreferences preferences) {
        this.elasticSearchService = elasticSearchService;
        this.userPrefs = preferences;
    }

    @Override
    public User getUser(String username) {

        String query = "{\n" +
                       "    \"query\": {\n" +
                       "        \"filtered\" : {\n" +
                       "            \"filter\" : {\n" +
                       "                \"term\" : { \"username\" : \"" + username.toLowerCase() + "\" }\n" +
                       "            }\n" +
                       "        }\n" +
                       "    }\n" +
                       "}";
        List<User> userlist = elasticSearchService.search(query);
        if (userlist.size() > 0) {
            return userlist.get(0);
        }
        else {
            throw new UserDoesNotExistException();
        }
    }

    @Override
    public void createUser(User user){
        try {
            this.getUser(user.getUsername());
        }
        catch (UserDoesNotExistException e) {
            if(!user.verifyContactInformation()) {
                throw new IncompleteUserException();
            }
            elasticSearchService.create(user);
            return;
        }
        throw new DuplicateUserException();
    }

    @Override
    public User getCurrentUser() {
        if (userPrefs.getUserName().equals("")) {
            throw new UserDoesNotExistException();
        } else {
            return userPrefs.getUser();
        }
    }

    @Override
    public void setCurrentUser(User user) {
        userPrefs.saveUser(user);
    }

    @Override
    public void updateCurrentUser(User user) {
        userPrefs.saveUser(user);
        this.updateUser(user);
    }

    @Override
    public void updateUser(User user) {
        elasticSearchService.update(user);
    }
}
