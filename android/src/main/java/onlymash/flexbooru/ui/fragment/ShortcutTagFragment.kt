/*
 * Copyright (C) 2020. by onlymash <im@fiepi.me>, All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package onlymash.flexbooru.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import onlymash.flexbooru.R
import onlymash.flexbooru.common.Keys
import onlymash.flexbooru.common.Settings.activatedBooruUid
import onlymash.flexbooru.common.Values.BOORU_TYPE_DAN
import onlymash.flexbooru.common.Values.BOORU_TYPE_SANKAKU
import onlymash.flexbooru.common.Values.Tags
import onlymash.flexbooru.data.database.BooruManager
import onlymash.flexbooru.data.database.HistoryManager
import onlymash.flexbooru.data.database.TagFilterManager
import onlymash.flexbooru.data.database.dao.BooruDao
import onlymash.flexbooru.data.database.dao.PostDao
import onlymash.flexbooru.data.model.common.*
import onlymash.flexbooru.extension.copyText
import onlymash.flexbooru.ui.activity.SearchActivity
import onlymash.flexbooru.ui.viewmodel.BooruViewModel
import onlymash.flexbooru.ui.viewmodel.ShortcutViewModel
import onlymash.flexbooru.ui.viewmodel.getBooruViewModel
import onlymash.flexbooru.ui.viewmodel.getShortcutViewModel
import org.kodein.di.erased.instance

class ShortcutTagFragment : BaseBottomSheetDialogFragment() {
    companion object {
        private const val KEYWORD = "key"
        fun create(postId: Int, query: String): ShortcutTagFragment {
            return ShortcutTagFragment().apply {
                arguments = Bundle().apply {
                    putInt(Keys.POST_ID, postId)
                    putString(KEYWORD, query)
                }
            }
        }
    }

    private lateinit var behavior: BottomSheetBehavior<View>
    private lateinit var booruViewModel: BooruViewModel
    private val booruDao by instance<BooruDao>()
    private val postDao by instance<PostDao>()

    private var postId = -1
    private var post: Post? = null

    private lateinit var booru: Booru

    private lateinit var shortcutViewModel: ShortcutViewModel

    //
    private var keyword: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.apply {
            postId = getInt(Keys.POST_ID, -1)
            keyword = getString(KEYWORD, "")
        }
        val booru = BooruManager.getBooruByUid(activatedBooruUid)
        if (booru == null) {
            dismiss()
            return
        }
        this.booru = booru

        booruViewModel = getBooruViewModel(booruDao)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val view = View.inflate(context, R.layout.fragment_bottom_sheet_tag, null)
        dialog.setContentView(view)
        view.findViewById<Toolbar>(R.id.toolbar).apply {
            setTitle(R.string.title_tags)
            setOnClickListener {
                dismiss()
            }
        }
        behavior = BottomSheetBehavior.from(view.parent as View)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

        })
        val tagListAdapter = TagListAdapter(keyword)
        view.findViewById<RecyclerView>(R.id.tags_list).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = tagListAdapter
        }
        shortcutViewModel = getShortcutViewModel(postDao, booru.uid, postId)
        shortcutViewModel.post.observe(this, Observer { post ->
            this.post = post
            tagListAdapter.notifyDataSetChanged()
        })
        return dialog
    }

    override fun onStart() {
        super.onStart()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    inner class TagListAdapter(keyword: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount(): Int = post?.tags?.size ?: 0

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val tag = post?.tags?.get(position)
            (holder as TagListViewHolder).bind(tag, keyword)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return TagListViewHolder(
                LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tag_browse, parent, false))
        }
    }

    inner class TagListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val dot: AppCompatImageView = itemView.findViewById(R.id.dot)
        private val tagName: AppCompatTextView = itemView.findViewById(R.id.tag_name)
        private val tagExclude: AppCompatImageView = itemView.findViewById(R.id.tag_exclude)
        private val tagInclude: AppCompatImageView = itemView.findViewById(R.id.tag_include)

        private var tag: TagBase? = null

        init {
            TooltipCompat.setTooltipText(tagExclude, tagExclude.contentDescription)
            TooltipCompat.setTooltipText(tagInclude, tagInclude.contentDescription)
            itemView.setOnClickListener {
                val query = tag?.name ?: return@setOnClickListener
                HistoryManager.createHistory(History(
                    booruUid = booru.uid,
                    query = query
                ))
                SearchActivity.startSearch(itemView.context, query)
                dismiss()
            }
            itemView.setOnLongClickListener {
                itemView.context.copyText(tagName.text)
                true
            }
            tagExclude.setOnClickListener {
                val padding = resources.getDimensionPixelSize(R.dimen.spacing_mlarge)
                val layout = FrameLayout(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(padding, padding / 2, padding, 0)
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Add to blacklist tag for ${tagName.text} ?")
                    .setView(layout)
                    .setPositiveButton(R.string.dialog_yes) { _, _ ->
                        tag?.let {
                            TagFilterManager.createTagFilter(
                                TagFilter(
                                    booruUid = booru.uid,
                                    name = "-${it.name}",
                                    type = it.category
                                )
                            )
                        }
                        if (booru.blacklists.add(tagName.text.toString())) {
                            booruViewModel.updateBooru(booru)
                        }

                        Toast.makeText(requireContext(), "Success", Toast.LENGTH_SHORT).show()

                    }
                    .setNegativeButton(R.string.dialog_no, null)
                    .create()
                    .show()

            }
            tagInclude.setOnClickListener {
                tag?.let {
                    TagFilterManager.createTagFilter(
                        TagFilter(
                            booruUid = booru.uid,
                            name = it.name,
                            type = it.category
                        )
                    )
                }
                HistoryManager.createHistory(
                    History(
                        booruUid = booru.uid,
                        query = "$keyword ${tagName.text.toString()}"
                    )
                )
                SearchActivity.startSearch(requireContext(), "$keyword ${tagName.text.toString()}")
            }
        }

        fun bind(tag: TagBase?, keyword: String) {
            this.tag = tag ?: return
            tagName.text = tag.name
            when (booru.type) {
                BOORU_TYPE_DAN -> {
                    when (tag.category) {
                        Tags.TYPE_GENERAL -> dot.setColorFilter(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.tag_type_general
                            )
                        )
                        Tags.TYPE_ARTIST -> dot.setColorFilter(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.tag_type_artist
                            )
                        )
                        Tags.TYPE_COPYRIGHT -> dot.setColorFilter(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.tag_type_copyright
                            )
                        )
                        Tags.TYPE_CHARACTER -> dot.setColorFilter(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.tag_type_character
                            )
                        )
                        Tags.TYPE_META -> dot.setColorFilter(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.tag_type_meta
                            )
                        )
                        else -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_unknown))
                    }
                }
                BOORU_TYPE_SANKAKU -> {
                    when (tag.category) {
                        Tags.TYPE_GENERAL -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_general))
                        Tags.TYPE_ARTIST -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_artist))
                        Tags.TYPE_COPYRIGHT -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_copyright))
                        Tags.TYPE_CHARACTER -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_character))
                        Tags.TYPE_META_SANKAKU -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_meta))
                        Tags.TYPE_GENRE -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_genre))
                        Tags.TYPE_MEDIUM -> dot.setColorFilter(ContextCompat.getColor(itemView.context, R.color.tag_type_medium))
                        Tags.TYPE_STUDIO -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_studio))
                        else -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_unknown))
                    }
                }
                else -> {
                    dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_unknown))
                }
            }
        }
    }
}