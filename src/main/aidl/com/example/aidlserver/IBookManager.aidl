// IBookManager.aidl
package com.example.aidlserver;

// 实现Parcelable的对象、AIDL文件，需要手动导包；
// 同时，自定义的Parcelable对象，必须新建一个AIDL文件，声明其是parcelabel类型
import com.example.aidlserver.Book;
import com.example.aidlserver.IOnNewBookArrivedListener;

interface IBookManager {
    List<Book> getBookList();
    void addBook(in Book book);     // in表示客户端传给服务端的值，并且服务端对其的修改不会影响客户端
    void registerListener(IOnNewBookArrivedListener listener);
    void unregisterListener(IOnNewBookArrivedListener listener);
}
