package com.example.semaj.mymemoapp.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.example.semaj.mymemoapp.Utils;
import com.example.semaj.mymemoapp.data.Memo;
import com.example.semaj.mymemoapp.data.MemoDataSource;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

/**
 * Local Database data를 handling하는 class
 * DbHelper를 생성, 쿼리 기반으로 DB에서 data를 뽑아서 Memo객체로 만들어 반환
 * 혹은 Memo객체의 내용물을 DB에 저장
 * Callback 리스너를 RxJava로 대체 -  즉 RxJava가 callback구현
 * 저장하고 삭제시에도 제대로 수행되었는지를 확인하기 위해 콜백 수행
 */
//Singleton
public class LocalMemoDataSource implements MemoDataSource {

    private static LocalMemoDataSource INSTANCE = null;

    private MemoDbHelper mDbHelper; //db 헬퍼

    private SQLiteDatabase mDb;

    private LocalMemoDataSource(Context context) {
        this.mDbHelper = new MemoDbHelper(context.getApplicationContext());
        mDb = mDbHelper.getWritableDatabase();
    }

    public static LocalMemoDataSource getInstance(Context context){
        if (INSTANCE == null)
            INSTANCE = new LocalMemoDataSource(context);

        return INSTANCE;
    }

    @Override
    public Flowable<List<Memo>> getMemoList() {
        String[] projection = {
                MemoDbContract.Entry.ENTRY_ID,
                MemoDbContract.Entry.COLUMN_NAME_TITLE,
                MemoDbContract.Entry.COLUMN_NAME_CONTENT,
                MemoDbContract.Entry.COLUMN_NAME_DATE,
        };

        String sortOrder = MemoDbContract.Entry.COLUMN_NAME_DATE + " DESC";
        Cursor cursor = mDb.query(
                MemoDbContract.Entry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder);
        List<Memo> memoList = new ArrayList<>();
        while(cursor.moveToNext()){
            Long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(MemoDbContract.Entry.ENTRY_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(MemoDbContract.Entry.COLUMN_NAME_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(MemoDbContract.Entry.COLUMN_NAME_CONTENT));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(MemoDbContract.Entry.COLUMN_NAME_DATE));
            Date parsedDate;
            try {
                parsedDate = Utils.parseDate(date);
            } catch (ParseException e) {
                e.printStackTrace();
                parsedDate = new Date();
            }
            Memo memo = new Memo(itemId, title, content, parsedDate);
            memoList.add(memo);
        }
        cursor.close();
        return Flowable.fromIterable(memoList)
                .toList().toFlowable();
    }

    @Override
    public Flowable<Memo> getMemo(@NonNull Long memoId) {
        String[] projection = {
                MemoDbContract.Entry.ENTRY_ID,
                MemoDbContract.Entry.COLUMN_NAME_TITLE,
                MemoDbContract.Entry.COLUMN_NAME_CONTENT,
                MemoDbContract.Entry.COLUMN_NAME_DATE,
        };
        String selection = MemoDbContract.Entry.ENTRY_ID + " = ?";
        String []selectionArgs = {"" +memoId};

        Cursor cursor = mDb.query(
                MemoDbContract.Entry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null);

        if(cursor.moveToNext()) {
            Long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(MemoDbContract.Entry.ENTRY_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(MemoDbContract.Entry.COLUMN_NAME_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(MemoDbContract.Entry.COLUMN_NAME_CONTENT));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(MemoDbContract.Entry.COLUMN_NAME_DATE));

            Date parsedDate;
            try {
                parsedDate = Utils.parseDate(date);
            } catch (ParseException e) {
                e.printStackTrace();
                parsedDate = new Date();
            }
            Memo memo = new Memo(itemId, title, content, parsedDate);

            cursor.close();
            return Flowable.just(memo);
        }
        return Flowable.error(Exception::new);
    }

    @Override
    public Single<Memo> saveMemo(Memo memo) {
        ContentValues values = new ContentValues();
        values.put(MemoDbContract.Entry.ENTRY_ID, memo.getId());
        values.put(MemoDbContract.Entry.COLUMN_NAME_TITLE, memo.getTitle());
        values.put(MemoDbContract.Entry.COLUMN_NAME_CONTENT, memo.getContent());
        values.put(MemoDbContract.Entry.COLUMN_NAME_DATE, Utils.getDateString(memo.getDate()));
        long rowId = mDb.insertWithOnConflict(MemoDbContract.Entry.TABLE_NAME,null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if(rowId == -1)
            mDb.update(MemoDbContract.Entry.TABLE_NAME, values, MemoDbContract.Entry.ENTRY_ID+"=?", new String[]{String.valueOf(memo.getId())});

        return Single.just(memo);
    }

    @Override
    public Completable deleteMemo(Long memoId) {
        String selection = MemoDbContract.Entry.ENTRY_ID + " LIKE ?";
        String[] args = {String.valueOf(memoId)};
        int deletedRows = mDb.delete(MemoDbContract.Entry.TABLE_NAME, selection, args);
        return Completable.complete();
    }

    @Override
    public Completable deleteAllMemo() {
        int deleteRows = mDb.delete(MemoDbContract.Entry.TABLE_NAME, null, null);
        return Completable.complete();
    }

    @Override
    public Completable deleteMemos(ArrayList<Long> ids) {
        String selection = MemoDbContract.Entry.ENTRY_ID + " IN ("+new String(new char[ids.size()-1]).replace("\0","?,")+"?)";
        String[] args = new String[ids.size()];
        for(int i=0;i<ids.size();++i)
            args[i] = String.valueOf(ids.get(i));
        int deleteRows = mDb.delete(
            MemoDbContract.Entry.TABLE_NAME,
            selection,
            args);
        return Completable.complete();
    }
}
