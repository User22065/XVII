package com.twoeightnine.root.xvii.accounts.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.twoeightnine.root.xvii.accounts.models.Account
import com.twoeightnine.root.xvii.managers.Session
import com.twoeightnine.root.xvii.model.LongPollServer
import com.twoeightnine.root.xvii.network.ApiService
import com.twoeightnine.root.xvii.utils.CacheHelper
import io.realm.Realm
import io.realm.RealmQuery
import javax.inject.Inject

class AccountsViewModel(private val api: ApiService) : ViewModel() {

    private val accountsLiveData = MutableLiveData<ArrayList<Account>>()

    fun getAccounts() = accountsLiveData as LiveData<ArrayList<Account>>

    fun loadAccounts() {
        val realm = Realm.getDefaultInstance()
        val accounts = RealmQuery
                .createQuery(realm, Account::class.java)
                .findAll()
        accountsLiveData.value = ArrayList(accounts)
    }

    fun switchTo(account: Account) {
        with(account) {
            Session.token = token
            Session.uid = uid
            Session.fullName = name
            Session.photo = photo
            Session.longPoll = LongPollServer("", "", 0) //resetting
        }
        CacheHelper.deleteAllMessagesAsync()
    }

    fun deleteAccount(account: Account) {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        realm.where(Account::class.java)
                .equalTo(Account.UID, account.uid)
                .findFirst()
                .deleteFromRealm()
        realm.commitTransaction()

        val accounts = accountsLiveData.value ?: return
        accounts.remove(account)
        accountsLiveData.value = accounts
    }

    class Factory @Inject constructor(private val api: ApiService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) = AccountsViewModel(api) as T
    }
}