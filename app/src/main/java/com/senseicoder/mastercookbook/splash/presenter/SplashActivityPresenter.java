package com.senseicoder.mastercookbook.splash.presenter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.senseicoder.mastercookbook.db.remote.callbacks.GetUserByIdOrAddUserCallback;
import com.senseicoder.mastercookbook.model.DTOs.UserDTO;
import com.senseicoder.mastercookbook.model.repositories.DataRepository;
import com.senseicoder.mastercookbook.splash.view.SplashActivityView;

public class SplashActivityPresenter {

    SplashActivityView splashActivityView;
    DataRepository dataRepository;

    public SplashActivityPresenter(SplashActivityView splashActivityView, DataRepository dataRepository) {
        this.splashActivityView = splashActivityView;
        this.dataRepository = dataRepository;
    }

    public void checkIfUserIsLoggedIn() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            dataRepository.getUserByIdOrAddUser(new UserDTO(
                    null,
                    null
                    , null,
                    user.getUid()
            ), new GetUserByIdOrAddUserCallback() {
                @Override
                public void onGetUserByIdOrAddUserCallbackSuccess(UserDTO userDTO) {
                    dataRepository.setCurrentUser(userDTO);
                    splashActivityView.handleCheckCurrentUser(true);
                }
                @Override
                public void onGetUserByIdOrAddUserCallbackailure(String message) {
                    splashActivityView.showRetryDialog();
                }
            });
        }else{
            splashActivityView.handleCheckCurrentUser(false);
        }
    }

    public void onRetryConfirmed() {
        checkIfUserIsLoggedIn();
    }

    public void onRetryCancelled() {
        splashActivityView.handleCheckCurrentUser(false);
    }
}
