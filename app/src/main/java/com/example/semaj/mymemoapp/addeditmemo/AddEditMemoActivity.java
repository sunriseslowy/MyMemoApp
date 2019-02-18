package com.example.semaj.mymemoapp.addeditmemo;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.example.semaj.mymemoapp.R;
import com.example.semaj.mymemoapp.Utils;
import com.example.semaj.mymemoapp.data.MemoRepository;
import com.example.semaj.mymemoapp.data.local.LocalMemoDataSource;

public class AddEditMemoActivity extends AppCompatActivity {

    public static final int REQUEST_ADD_MEMO = 0x11;
    public static final int REQUEST_EDIT_MEMO = 0x12;
    private AddEditMemoPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_memo);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        AddEditMemoFragment fragment = (AddEditMemoFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);

        long memoId = getIntent().getLongExtra(AddEditMemoFragment.ARG_MEMO_ID, -1);

        if(fragment == null){
            fragment = AddEditMemoFragment.newInstance(memoId);
            Utils.addFragmentToActivity(getSupportFragmentManager(),fragment,R.id.content_frame);
        }

        mPresenter = new AddEditMemoPresenter(
                memoId,
                MemoRepository.getInstance(LocalMemoDataSource.getInstance(getApplicationContext())),
                fragment,
                memoId != -1
        );

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
