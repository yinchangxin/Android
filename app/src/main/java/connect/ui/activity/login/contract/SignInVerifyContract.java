package connect.ui.activity.login.contract;

import android.app.Activity;
import android.text.TextWatcher;

import java.util.Timer;

import connect.ui.activity.login.bean.UserBean;
import connect.ui.base.BasePresenter;
import connect.ui.base.BaseView;

/**
 * Created by Administrator on 2017/4/14 0014.
 */

public interface SignInVerifyContract {

    interface View extends BaseView<SignInVerifyContract.Presenter> {
        Activity getActivity();

        String getCode();

        void setVoiceVisi();

        void changeTime(int time,Timer timer);

        void goinCodeLogin(UserBean userBean);

        void goinRandomSend(String phone,String token);
    }

    interface Presenter extends BasePresenter {
        TextWatcher getEditChange();

        void requestVerifyCode();

        void reSendCode(int type);

        void miniuteReplay();

        void showChangeText();

        void requestBindMobile(String type);
    }

}
