package com.twoeightnine.root.xvii.friends.fragments

import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.adapters.BaseAdapter
import com.twoeightnine.root.xvii.fragments.BaseFragment
import com.twoeightnine.root.xvii.friends.adapters.UsersAdapter
import com.twoeightnine.root.xvii.managers.Style
import com.twoeightnine.root.xvii.profile.fragments.ProfileFragment
import kotlinx.android.synthetic.main.fragment_friends_advanced.*

class FriendsAllFragment: BaseFragment(), BaseAdapter.OnMultiSelected {

    companion object {

        fun newInstance(loadMore: (Int) -> Unit, createCallback: (String) -> Unit): FriendsAllFragment {
            val frag = FriendsAllFragment()
            frag.loadMore = loadMore
            frag.createChat = createCallback
            return frag
        }

    }

    lateinit var adapter: UsersAdapter

    private var multiSelectMode = false

    var loadMore: ((Int) -> Unit)? = null
    var createChat: ((String) -> Unit)? = null

    override fun bindViews(view: View) {
        super.bindViews(view)
        initAdapter()
        initFabs()
        Style.forFAB(fabCreateChat)
        Style.forFAB(fabDoneCreating)
    }

    private fun initFabs() {
        fabDoneCreating.hide()
        fabCreateChat.show()
        fabCreateChat.setOnClickListener { enableMultiSelect() }
        fabDoneCreating.setOnClickListener { create() }
    }

    private fun initAdapter() {
        adapter = UsersAdapter(safeActivity, { loadMore?.invoke(it) }, ::onClick)
        adapter.multiListener = this
        rvUsers.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
        rvUsers.adapter = adapter
    }

    private fun onClick(position: Int) {
        val user = adapter.items[position]
        if (multiSelectMode) {
            adapter.multiSelect(user.id)
            adapter.notifyItemChanged(position)
        } else {
            rootActivity.loadFragment(ProfileFragment.newInstance(user.id))
        }
    }

    private fun enableMultiSelect() {
        updateTitle(getString(R.string.new_dialog))
        multiSelectMode = true
        fabCreateChat.hide()
    }

    private fun disableMultiSelect() {
        updateTitle(getString(R.string.fiends))
        multiSelectMode = false
        fabCreateChat.show()
        fabDoneCreating.hide()
    }

    private fun create() {
        createChat?.invoke(adapter.multiSelect)
        adapter.clearMultiSelect()
        adapter.notifyMultiSelect()
    }

    override fun onNonEmpty() {
        if (adapter.multiSelectRaw.size > 1 && !fabDoneCreating.isShown) {
            fabDoneCreating.show()
        }
        if (fabDoneCreating.isShown && adapter.multiSelectRaw.size <= 1) {
            fabDoneCreating.hide()
        }
    }

    override fun onEmpty() {
        disableMultiSelect()
    }

    override fun onBackPressed(): Boolean {
        return if (multiSelectMode) {
            adapter.clearMultiSelect()
            true
        } else {
            false
        }
    }

    override fun getLayout() = R.layout.fragment_friends_advanced
}