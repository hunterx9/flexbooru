/*
 * Copyright (C) 2019. by onlymash <im@fiepi.me>, All rights reserved
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

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.refreshable_list.*
import onlymash.flexbooru.R
import onlymash.flexbooru.common.Constants
import onlymash.flexbooru.common.Settings
import onlymash.flexbooru.database.UserManager
import onlymash.flexbooru.entity.common.Booru
import onlymash.flexbooru.entity.common.User
import onlymash.flexbooru.entity.tag.SearchTag
import onlymash.flexbooru.entity.tag.TagBase
import onlymash.flexbooru.repository.NetworkState
import onlymash.flexbooru.repository.tag.TagRepository
import onlymash.flexbooru.repository.tag.TagRepositoryImpl
import onlymash.flexbooru.ui.activity.MainActivity
import onlymash.flexbooru.ui.activity.SearchActivity
import onlymash.flexbooru.ui.adapter.TagAdapter
import onlymash.flexbooru.ui.viewholder.TagViewHolder
import onlymash.flexbooru.ui.viewmodel.TagViewModel
import onlymash.flexbooru.widget.search.SearchBar

class TagFragment : ListFragment() {

    companion object {
        private const val TAG = "TagFragment"

        private const val TYPE_ALL = ""
        private const val TYPE_GENERAL = "0"
        private const val TYPE_ARTIST = "1"
        private const val TYPE_COPYRIGHT = "3"
        private const val TYPE_CHARACTER = "4"
        private const val TYPE_CIRCLE = "5"
        private const val TYPE_FAULTS = "6"
        private const val TYPE_META = "5"
        private const val TYPE_MODEL = "5"
        private const val TYPE_PHOTO_SET = "6"
        private const val TYPE_META_SANKAKU = "9"
        private const val TYPE_STUDIO = "2"
        private const val TYPE_GENRE = "5"
        private const val TYPE_MEDIUM = "8"

        private const val ORDER_DATE = "date"
        private const val ORDER_NAME = "name"
        private const val ORDER_COUNT = "count"

        @JvmStatic
        fun newInstance(booru: Booru, user: User?) =
            TagFragment().apply {
                arguments = Bundle().apply {
                    putString(Constants.SCHEME_KEY, booru.scheme)
                    putString(Constants.HOST_KEY, booru.host)
                    putInt(Constants.TYPE_KEY, booru.type)
                    if (user != null) {
                        putInt(Constants.USER_ID_KEY, user.id)
                        putString(Constants.USERNAME_KEY, user.name)
                        putString(
                            Constants.AUTH_KEY,
                            when (booru.type) {
                                Constants.TYPE_DANBOORU,
                                Constants.TYPE_GELBOORU-> user.apiKey
                                else -> user.passwordHash
                            }
                        )
                    } else {
                        putString(Constants.USERNAME_KEY, "")
                        putString(Constants.AUTH_KEY, "")
                    }
                }
            }
    }

    private var type = -1
    private lateinit var search: SearchTag

    override val stateChangeListener: SearchBar.StateChangeListener
        get() = object : SearchBar.StateChangeListener {
            override fun onStateChange(newState: Int, oldState: Int, animation: Boolean) {
                toggleArrowLeftDrawable()
            }
        }
    override val searchBarHelper: SearchBarHelper
        get() = object : SearchBarHelper {
            override fun onMenuItemClick(menuItem: MenuItem) {
                when (menuItem.itemId) {
                    R.id.action_tag_order_date -> {
                        search.order = ORDER_DATE
                        refresh()
                    }
                    R.id.action_tag_order_name -> {
                        search.order = ORDER_NAME
                        refresh()
                    }
                    R.id.action_tag_order_count -> {
                        search.order = ORDER_COUNT
                        refresh()
                    }
                    R.id.action_tag_type_all -> {
                        search.type = TYPE_ALL
                        refresh()
                    }
                    R.id.action_tag_type_general -> {
                        search.type = TYPE_GENERAL
                        refresh()
                    }
                    R.id.action_tag_type_artist -> {
                        search.type = TYPE_ARTIST
                        refresh()
                    }
                    R.id.action_tag_type_copyright -> {
                        search.type = TYPE_COPYRIGHT
                        refresh()
                    }
                    R.id.action_tag_type_character -> {
                        search.type = TYPE_CHARACTER
                        refresh()
                    }
                    R.id.action_tag_type_circle -> {
                        search.type = TYPE_CIRCLE
                        refresh()
                    }
                    R.id.action_tag_type_faults -> {
                        search.type = TYPE_FAULTS
                        refresh()
                    }
                    R.id.action_tag_type_meta -> {
                        search.type = TYPE_META
                        refresh()
                    }
                    R.id.action_tag_type_model -> {
                        search.type = TYPE_MODEL
                        refresh()
                    }
                    R.id.action_tag_type_photo_set -> {
                        search.type = TYPE_PHOTO_SET
                        refresh()
                    }
                    R.id.action_tag_type_meta_sankaku -> {
                        search.type = TYPE_META_SANKAKU
                        refresh()
                    }
                    R.id.action_tag_type_genre -> {
                        search.type = TYPE_GENRE
                        refresh()
                    }
                    R.id.action_tag_type_studio -> {
                        search.type = TYPE_STUDIO
                        refresh()
                    }
                    R.id.action_tag_type_medium -> {
                        search.type = TYPE_MEDIUM
                        refresh()
                    }
                }
            }

            override fun onApplySearch(query: String) {
                search.name = query
                refresh()
            }
        }

    private fun refresh() {
        when (type) {
            Constants.TYPE_DANBOORU -> {
                swipe_refresh.isRefreshing = true
                tagViewModel.show(search)
                tagViewModel.refreshDan()
            }
            Constants.TYPE_MOEBOORU -> {
                swipe_refresh.isRefreshing = true
                tagViewModel.show(search)
                tagViewModel.refreshMoe()
            }
            Constants.TYPE_DANBOORU_ONE -> {
                swipe_refresh.isRefreshing = true
                tagViewModel.show(search)
                tagViewModel.refreshDanOne()
            }
            Constants.TYPE_GELBOORU -> {
                swipe_refresh.isRefreshing = true
                tagViewModel.show(search)
                tagViewModel.refreshGel()
            }
            Constants.TYPE_SANKAKU -> {
                swipe_refresh.isRefreshing = true
                tagViewModel.show(search)
                tagViewModel.refreshSankaku()
            }
            Constants.TYPE_IDOL -> {
                swipe_refresh.isRefreshing = true
                tagViewModel.show(search)
                tagViewModel.refreshIdol()
            }
        }
    }

    private lateinit var tagViewModel: TagViewModel
    private lateinit var tagAdapter: TagAdapter

    private val itemListener = object : TagViewHolder.ItemListener {
        override fun onClickItem(keyword: String) {
            SearchActivity.startActivity(requireContext(), keyword)
        }
    }

    private val userListener = object : UserManager.Listener {
        override fun onAdd(user: User) {
            updateUserInfoAndRefresh(user)
        }
        override fun onDelete(user: User) {
            if (user.booruUid != Settings.activeBooruUid) return
            search.username = ""
            search.auth_key = ""
            when (type) {
                Constants.TYPE_DANBOORU -> {
                    tagViewModel.apply {
                        show(search)
                        refreshDan()
                    }
                }
                Constants.TYPE_MOEBOORU -> {
                    tagViewModel.apply {
                        show(search)
                        refreshMoe()
                    }
                }
                Constants.TYPE_DANBOORU_ONE -> {
                    tagViewModel.apply {
                        show(search)
                        refreshDanOne()
                    }
                }
                Constants.TYPE_GELBOORU -> {
                    tagViewModel.apply {
                        show(search)
                        refreshGel()
                    }
                }
                Constants.TYPE_SANKAKU -> {
                    tagViewModel.apply {
                        show(search)
                        refreshSankaku()
                    }
                }
            }
        }
        override fun onUpdate(user: User) {
            updateUserInfoAndRefresh(user)
        }
    }

    private fun updateUserInfoAndRefresh(user: User) {
        when (type) {
            Constants.TYPE_DANBOORU -> {
                search.username = user.name
                search.auth_key = user.apiKey ?: ""
                tagViewModel.apply {
                    show(search)
                    refreshDan()
                }
            }
            Constants.TYPE_MOEBOORU -> {
                search.username = user.name
                search.auth_key = user.passwordHash ?: ""
                tagViewModel.apply {
                    show(search)
                    refreshMoe()
                }
            }
            Constants.TYPE_DANBOORU_ONE -> {
                search.username = user.name
                search.auth_key = user.passwordHash ?: ""
                tagViewModel.apply {
                    show(search)
                    refreshDanOne()
                }
            }
            Constants.TYPE_GELBOORU -> {
                search.username = user.name
                search.auth_key = user.apiKey ?: ""
                tagViewModel.apply {
                    show(search)
                    refreshGel()
                }
            }
            Constants.TYPE_SANKAKU -> {
                search.username = user.name
                search.auth_key = user.passwordHash ?: ""
                tagViewModel.apply {
                    show(search)
                    refreshSankaku()
                }
            }
        }
    }

    private val navigationListener = object : MainActivity.NavigationListener {
        override fun onClickPosition(position: Int) {
            if (position == 3) {
                list.smoothScrollToPosition(0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arg = arguments ?: throw RuntimeException("arg is null")
        type = arg.getInt(Constants.TYPE_KEY, Constants.TYPE_UNKNOWN)
        if (isUnsupported) return
        search = SearchTag(
            scheme = arg.getString(Constants.SCHEME_KEY, ""),
            host = arg.getString(Constants.HOST_KEY, ""),
            name = "",
            order = ORDER_DATE,
            type = TYPE_ALL,
            username = arg.getString(Constants.USERNAME_KEY, ""),
            auth_key = arg.getString(Constants.AUTH_KEY, ""),
            limit = Settings.pageLimit
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchBar.setTitle(R.string.title_tags)
        searchBar.setEditTextHint(getString(R.string.search_bar_hint_search_tags))
        if (isUnsupported) {
            list.visibility = View.GONE
            swipe_refresh.visibility = View.GONE
            notSupported.visibility = View.VISIBLE
        }
        tagViewModel = getTagViewModel(
            TagRepositoryImpl(
                danbooruApi = danApi,
                danbooruOneApi = danOneApi,
                moebooruApi = moeApi,
                gelbooruApi = gelApi,
                sankakuApi = sankakuApi,
                idolApi = idolApi,
                networkExecutor = ioExecutor
            )
        )
        tagAdapter = TagAdapter(
            listener = itemListener,
            retryCallback = { retry() }
        )
        list.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = tagAdapter
        }
        when (type) {
            Constants.TYPE_DANBOORU -> {
                searchBar.setMenu(R.menu.tag_dan, requireActivity().menuInflater)
                tagViewModel.tagsDan.observe(viewLifecycleOwner, Observer { tags ->
                    @Suppress("UNCHECKED_CAST")
                    tagAdapter.submitList(tags as PagedList<TagBase>)
                })
                tagViewModel.networkStateDan.observe(viewLifecycleOwner, Observer { networkState ->
                    tagAdapter.setNetworkState(networkState)
                })
                initSwipeToRefreshDan()
            }
            Constants.TYPE_MOEBOORU -> {
                searchBar.setMenu(R.menu.tag_moe, requireActivity().menuInflater)
                tagViewModel.tagsMoe.observe(viewLifecycleOwner, Observer { tags ->
                    @Suppress("UNCHECKED_CAST")
                    tagAdapter.submitList(tags as PagedList<TagBase>)
                })
                tagViewModel.networkStateMoe.observe(viewLifecycleOwner, Observer { networkState ->
                    tagAdapter.setNetworkState(networkState)
                })
                initSwipeToRefreshMoe()
            }
            Constants.TYPE_DANBOORU_ONE -> {
                searchBar.setMenu(R.menu.tag_dan_one, requireActivity().menuInflater)
                tagViewModel.tagsDanOne.observe(viewLifecycleOwner, Observer { tags ->
                    @Suppress("UNCHECKED_CAST")
                    tagAdapter.submitList(tags as PagedList<TagBase>)
                })
                tagViewModel.networkStateDanOne.observe(viewLifecycleOwner, Observer { networkState ->
                    tagAdapter.setNetworkState(networkState)
                })
                initSwipeToRefreshDanOne()
            }
            Constants.TYPE_GELBOORU -> {
                searchBar.setMenu(R.menu.tag_gel, requireActivity().menuInflater)
                tagViewModel.tagsGel.observe(viewLifecycleOwner, Observer { tags ->
                    @Suppress("UNCHECKED_CAST")
                    tagAdapter.submitList(tags as PagedList<TagBase>)
                })
                tagViewModel.networkStateGel.observe(viewLifecycleOwner, Observer { networkState ->
                    tagAdapter.setNetworkState(networkState)
                })
                initSwipeToRefreshGel()
            }
            Constants.TYPE_SANKAKU -> {
                searchBar.setMenu(R.menu.tag_sankaku, requireActivity().menuInflater)
                tagViewModel.tagsSankaku.observe(viewLifecycleOwner, Observer { tags ->
                    @Suppress("UNCHECKED_CAST")
                    tagAdapter.submitList(tags as PagedList<TagBase>)
                })
                tagViewModel.networkStateSankaku.observe(viewLifecycleOwner, Observer { networkState ->
                    tagAdapter.setNetworkState(networkState)
                })
                initSwipeToRefreshSankaku()
            }
            Constants.TYPE_IDOL -> {
                searchBar.setMenu(R.menu.tag_sankaku, requireActivity().menuInflater)
                tagViewModel.tagsIdol.observe(this, Observer { tags ->
                    @Suppress("UNCHECKED_CAST")
                    tagAdapter.submitList(tags as PagedList<TagBase>)
                })
                tagViewModel.networkStateIdol.observe(this, Observer { networkState ->
                    tagAdapter.setNetworkState(networkState)
                })
                initSwipeToRefreshIdol()
            }
        }
        tagViewModel.show(search = search)
        UserManager.listeners.add(userListener)
        (requireActivity() as MainActivity).addNavigationListener(navigationListener)
    }

    override fun retry() {
        when (type) {
            Constants.TYPE_DANBOORU -> tagViewModel.retryDan()
            Constants.TYPE_MOEBOORU -> tagViewModel.retryMoe()
            Constants.TYPE_DANBOORU_ONE -> tagViewModel.retryDanOne()
            Constants.TYPE_GELBOORU -> tagViewModel.retryGel()
            Constants.TYPE_SANKAKU -> tagViewModel.retrySankaku()
            Constants.TYPE_IDOL -> tagViewModel.retryIdol()
        }
    }

    private fun initSwipeToRefreshDan() {
        tagViewModel.refreshStateDan.observe(viewLifecycleOwner, Observer<NetworkState> {
            if (it != NetworkState.LOADING) {
                swipe_refresh.isRefreshing = false
            }
        })
        swipe_refresh.setOnRefreshListener { tagViewModel.refreshDan() }
    }

    private fun initSwipeToRefreshDanOne() {
        tagViewModel.refreshStateDanOne.observe(viewLifecycleOwner, Observer<NetworkState> {
            if (it != NetworkState.LOADING) {
                swipe_refresh.isRefreshing = false
            }
        })
        swipe_refresh.setOnRefreshListener { tagViewModel.refreshDanOne() }
    }

    private fun initSwipeToRefreshMoe() {
        tagViewModel.refreshStateMoe.observe(viewLifecycleOwner, Observer<NetworkState> {
            if (it != NetworkState.LOADING) {
                swipe_refresh.isRefreshing = false
            }
        })
        swipe_refresh.setOnRefreshListener { tagViewModel.refreshMoe() }
    }

    private fun initSwipeToRefreshGel() {
        tagViewModel.refreshStateGel.observe(viewLifecycleOwner, Observer<NetworkState> {
            if (it != NetworkState.LOADING) {
                swipe_refresh.isRefreshing = false
            }
        })
        swipe_refresh.setOnRefreshListener { tagViewModel.refreshGel() }
    }

    private fun initSwipeToRefreshSankaku() {
        tagViewModel.refreshStateSankaku.observe(viewLifecycleOwner, Observer<NetworkState> {
            if (it != NetworkState.LOADING) {
                swipe_refresh.isRefreshing = false
            }
        })
        swipe_refresh.setOnRefreshListener { tagViewModel.refreshSankaku() }
    }

    private fun initSwipeToRefreshIdol() {
        tagViewModel.refreshStateIdol.observe(this, Observer<NetworkState> {
            if (it != NetworkState.LOADING) {
                swipe_refresh.isRefreshing = false
            }
        })
        swipe_refresh.setOnRefreshListener { tagViewModel.refreshIdol() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getTagViewModel(repo: TagRepository): TagViewModel {
        return ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return TagViewModel(repo) as T
            }
        })[TagViewModel::class.java]
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isUnsupported) return
        UserManager.listeners.remove(userListener)
        (requireActivity() as MainActivity).removeNavigationListener(navigationListener)
    }

    override val isUnsupported: Boolean
        get() = type == -1
}