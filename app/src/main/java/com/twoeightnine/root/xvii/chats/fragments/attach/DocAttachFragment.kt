package com.twoeightnine.root.xvii.chats.fragments.attach

import com.twoeightnine.root.xvii.App
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.chats.adapters.attachments.DocAttachmentsAdapter
import com.twoeightnine.root.xvii.managers.Session
import com.twoeightnine.root.xvii.model.Attachment
import com.twoeightnine.root.xvii.model.Doc
import com.twoeightnine.root.xvii.utils.showError
import com.twoeightnine.root.xvii.utils.subscribeSmart
import kotlinx.android.synthetic.main.fragment_attachments_doc.*

class DocAttachFragment : BaseAttachFragment<Doc>() {

    companion object {
        fun newInstance(listener: ((Attachment) -> Unit)?): DocAttachFragment {
            val frag = DocAttachFragment()
            frag.listener = listener
            return frag
        }
    }

    override fun getLayout() = R.layout.fragment_attachments_doc

    override fun initAdapter() {
        App.appComponent?.inject(this)
        adapter = DocAttachmentsAdapter({ load() }, { listener?.invoke(Attachment(it)) })
        lvDocs.adapter = adapter
    }

    fun load() {
        api.getDocs(count, adapter.count)
                .subscribeSmart({
                    response ->
                    adapter.stopLoading(response.items)
                }, {
                    error ->
                    showError(activity, error)
                    adapter.isLoading = false
                })
    }
}