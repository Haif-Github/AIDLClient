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

        findViewById(R.id.tv2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 调用远程服务端的方法，有可能是耗时操作，所以最好放在子线程中
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (mRemoteBookManager != null) {
                                List<Book> bookList = mRemoteBookManager.getBookList();
                                Log.e("", "---getBookList: " + bookList);
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
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

        if (mRemoteBookManager != null && mRemoteBookManager.asBinder().isBinderAlive()) {
            try {
                mRemoteBookManager.unregisterListener(mOnNewBookArrivedListener);
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

                // 注册监听
                mRemoteBookManager.registerListener(mOnNewBookArrivedListener);

                // 设置DeathRecipient监听Binder意外死亡，这运行在Binder线程池（还有一种方法，就是在onServiceDisconnected中重连）
//                mRemoteBookManager.asBinder().linkToDeath(mDeathRecipient, 0);

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 当服务意外停止时，需要重连服务，这是运行在主线程的（还有一种重连服务的方法，设置DeathRecipient监听，这运行在Binder线程池）
            bindRemoteService();
        }
    };

    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            if (mRemoteBookManager != null) {
                // 解除死亡通知，如果Binder死亡了，不会再触发binderDied方法
                mRemoteBookManager.asBinder().unlinkToDeath(mDeathRecipient, 0);
                mRemoteBookManager = null;
                // 重新启动服务
                bindRemoteService();
            }
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
