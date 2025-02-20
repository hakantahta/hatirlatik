package com.tht.hatirlatik.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

import androidx.appcompat.widget.AppCompatCheckBox;

public class TaskCheckBox extends AppCompatCheckBox {
    private boolean isUserAction = true;
    private OnCheckedChangeListener userListener;

    public TaskCheckBox(Context context) {
        super(context);
        init();
    }

    public TaskCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        super.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUserAction && userListener != null) {
                userListener.onCheckedChanged(buttonView, isChecked);
            }
        });
    }

    @Override
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.userListener = listener;
    }

    public void setCheckedProgrammatically(boolean checked) {
        isUserAction = false;
        setChecked(checked);
        isUserAction = true;
    }
} 