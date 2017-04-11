package com.leaves.app.shareme.contract;

import com.leaves.app.shareme.BasePresenter;
import com.leaves.app.shareme.BaseView;
import com.leaves.app.shareme.bean.Media;

import java.util.List;

/**
 * Created by Leaves on 2017/4/11.
 */

public interface AudioListContract {
    interface Presenter extends BasePresenter {

    }

    interface View extends BaseView {
        void setData(List<Media> medias);
    }
}
