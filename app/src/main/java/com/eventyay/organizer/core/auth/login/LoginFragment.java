package com.eventyay.organizer.core.auth.login;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.eventyay.organizer.R;
import com.eventyay.organizer.common.mvp.view.BaseFragment;
import com.eventyay.organizer.core.auth.SharedViewModel;
import com.eventyay.organizer.core.auth.reset.ResetPasswordFragment;
import com.eventyay.organizer.core.auth.signup.SignUpFragment;
import com.eventyay.organizer.core.main.MainActivity;
import com.eventyay.organizer.databinding.LoginFragmentBinding;
import com.eventyay.organizer.ui.ViewUtils;

import java.util.ArrayList;
import java.util.Set;

import javax.inject.Inject;

import br.com.ilhasoft.support.validation.Validator;

import static com.eventyay.organizer.ui.ViewUtils.showView;

public class LoginFragment extends BaseFragment implements LoginView {

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private LoginViewModel loginFragmentViewModel;
    private LoginFragmentBinding binding;
    private Validator validator;
    private SharedViewModel sharedViewModel;

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.login_fragment, container, false);
        loginFragmentViewModel = ViewModelProviders.of(this, viewModelFactory).get(LoginViewModel.class);
        sharedViewModel = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);
        validator = new Validator(binding);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        loginFragmentViewModel.getLoginStatus().observe(this, (loginStatus) -> handleIntent());
        loginFragmentViewModel.getProgress().observe(this, this::showProgress);
        loginFragmentViewModel.getError().observe(this, this::showError);
        loginFragmentViewModel.getEmailList().observe(this, this::attachEmails);
        loginFragmentViewModel.getLogin().observe(this, login -> {
            binding.setLogin(login);
            sharedViewModel.getEmail().observe(this, email -> binding.getLogin().setEmail(email));
        });
        loginFragmentViewModel.getTokenSentAction().observe(this, aVoid -> {
            getFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_from_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.fragment_container, new ResetPasswordFragment())
                .addToBackStack(null)
                .commit();
        });

        binding.btnLogin.setOnClickListener(view -> {
            if (!validator.validate())
                return;

            ViewUtils.hideKeyboard(view);
            String url = binding.url.baseUrl.getText().toString().trim();
            loginFragmentViewModel.setBaseUrl(url, binding.url.overrideUrl.isChecked());
            loginFragmentViewModel.login();
        });
        binding.signUpLink.setOnClickListener(view -> openSignUpPage());
        binding.forgotPasswordLink.setOnClickListener(view -> clickForgotPassword());
    }

    public void handleIntent() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    @Override
    protected int getTitle() {
        return R.string.login;
    }

    @Override
    public void onStop() {
        super.onStop();
        binding.emailDropdown.setAdapter(null);
    }

    private void openSignUpPage() {
        sharedViewModel.setEmail(binding.getLogin().getEmail());
        getFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_from_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            .replace(R.id.fragment_container, new SignUpFragment())
            .addToBackStack(null)
            .commit();
    }

    private void clickForgotPassword() {
        if (!validator.validate(binding.emailDropdown))
            return;

        loginFragmentViewModel.requestToken();
        sharedViewModel.setEmail(binding.getLogin().getEmail());
    }

    @Override
    public void showError(String error) {
        ViewUtils.hideKeyboard(binding.getRoot());
        ViewUtils.showSnackbar(binding.getRoot(), error);
    }

    @Override
    public void onSuccess(String message) {
        startActivity(new Intent(getActivity(), MainActivity.class));
        getActivity().finish();
    }

    @Override
    public void showProgress(boolean show) {
        showView(binding.progressBar, show);
    }

    @Override
    public void attachEmails(Set<String> emails) {
        binding.emailDropdown.setAdapter(
            new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<>(emails))
        );
    }
}
