package com.example.aidlclient;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.example.aidlserver.Book;
import com.example.aidlserver.IBookManager;
import com.example.aidlserver.IOnNewBookArrivedListener;

import java.util.List;

public class BookManagerActivity extends AppCompatActivity {

    private IBookManager mRemoteBookManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bindRemoteService();
            }
        });

    }

    private void bindRemoteService() {
        //跨进程绑定服务，不能通过new Intent(this, 服务名.class)的方式；可以通过指定包名和完整类名方式
        Intent intent = new Intent();
        intent.setClassName("com.example.aidlserver", "com.example.aidlserver.BookManagerService");
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        unbindService(mConnection);

        if (mRemoteBookManager != null && mRemoteBookManager.asBinder().isBinderAlive()){
            try {
                mRemoteBookManager.registerListener(mOnNewBookArrivedListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        super.onDestroy();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IBookManager bookManager = IBookManager.Stub.asInterface(service);
            try {

                mRemoteBookManager = bookManager;

                List<Book> bookList = bookManager.getBookList();
                Log.e("", "----bookList: " + bookList.toString());

                bookManager.addBook(new Book(6, "Android艺术探索"));

                List<Book> newList = bookManager.getBookList();
                Log.e("", "----newList: " + newList.toString());

                // 注册监听
                bookManager.registerListener(mOnNewBookArrivedListener);

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private IOnNewBookArrivedListener mOnNewBookArrivedListener = new IOnNewBookArrivedListener.Stub() {
        @Override
        public void onNewBookArrivedListener(Book newBook) throws RemoteException {
            // 此方法是在客户端的Binder线程池中执行，为了便于UI操作，通过Handler切换到客户端的祝线程
            Message msg = Message.obtain();
            msg.obj = newBook;
            mHandler.sendMessage(msg);
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Book newBook = (Book) msg.obj;
            Log.e("", "----receive new book: " + newBook);
        }
    };

}
