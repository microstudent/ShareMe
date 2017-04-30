package com.leaves.app.shareme.contract;

import com.leaves.app.shareme.BasePresenter;
import com.leaves.app.shareme.BaseView;

/**
 * Created by Leaves on 2017/4/19.
 */

public interface MainActivityContract {
    interface View extends BaseView{

        void onSearching();

        void setupFragment(int mode);
    }

    interface Presenter extends BasePresenter {
        void onDialpadClick(int position, String number);

        boolean isServer();

        void cancelSearch();
    }
}
