// IOnNewBookArrivedListener.aidl
package com.example.aidlserver;

import com.example.aidlserver.Book;

interface IOnNewBookArrivedListener {
    void onNewBookArrivedListener(in Book newBook);
}
