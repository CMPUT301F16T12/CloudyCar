package com.cloudycrew.cloudycar.userprofile;

import com.cloudycrew.cloudycar.ViewController;
import com.cloudycrew.cloudycar.controllers.UserController;
import com.cloudycrew.cloudycar.models.User;
import com.cloudycrew.cloudycar.scheduling.ISchedulerProvider;
import com.cloudycrew.cloudycar.utils.ObservableUtils;

/**
 * Created by George on 2016-11-05.
 */

public class UserProfileController extends ViewController<IUserProfileView> {
    private UserController userController;
    private ISchedulerProvider schedulerProvider;

    public UserProfileController(UserController userController, ISchedulerProvider schedulerProvider) {
        this.userController = userController;
        this.schedulerProvider = schedulerProvider;
    }
    
    public void loadUser(String username) {
        dispatchDisplayLoading();

        ObservableUtils.fromFunction(userController::getUser, username)
                       .subscribeOn(schedulerProvider.ioScheduler())
                       .observeOn(schedulerProvider.mainThreadScheduler())
                       .subscribe(this::dispatchDisplayUser);
    }

    private void dispatchDisplayLoading() {
        if (getView() != null) {
            getView().displayLoading();
        }
    }

    private void dispatchDisplayUser(User user) {
        if (getView() != null) {
            getView().displayUser(user);
        }
    }
}
