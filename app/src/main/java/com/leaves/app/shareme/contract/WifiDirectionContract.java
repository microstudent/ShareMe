package com.leaves.app.shareme.contract;

import com.leaves.app.shareme.BasePresenter;
import com.leaves.app.shareme.BaseView;

/**
 * Created by Leaves on 2017/3/29.
 */

public interface WifiDirectionContract {
    interface View extends BaseView {
        void onStartDiscover();

        void onDeviceFound();
    }

    interface Presenter extends BasePresenter {
        void appendPassword(String number);
    }
}
