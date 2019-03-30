package com.algoideas.androidserialport.activity;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.algoideas.androidserialport.bean.MessageBean;
import java.util.ArrayList;

/**
 * author： deemons
 * date:    2018/5/26
 * desc:
 */
public interface MainContract {

    interface IView {
        void setOpen(boolean isOpen);

        void setLeftData(ArrayList<MultiItemEntity> list);

        void addData(MessageBean messageBean);

        void showPermissionDialog();

        String getEditText();
    }

    interface IPresenter {

    }

}
