package com.senseicoder.mastercookbook.features.login.view;

public interface LoginView {
    void handleEmailValidation(boolean isValid);
    void handlePasswordValidation(boolean isValid);

    void handleLoginSuccess();
    void handleLoginFailure(String errorMessage);

    void hideValidationErrors();

    void showLoadingDialog();
    void hideLoadingDialog();
}
