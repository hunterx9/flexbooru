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

import android.animation.ValueAnimator
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.annotation.FloatRange
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import onlymash.flexbooru.R
import onlymash.flexbooru.common.Settings.AUTO_HIDE_BOTTOM_BAR_KEY
import onlymash.flexbooru.common.Settings.activatedBooruUid
import onlymash.flexbooru.common.Settings.autoHideBottomBar
import onlymash.flexbooru.common.Values.BOORU_TYPE_DAN
import onlymash.flexbooru.common.Values.BOORU_TYPE_DAN1
import onlymash.flexbooru.common.Values.BOORU_TYPE_MOE
import onlymash.flexbooru.common.Values.BOORU_TYPE_SHIMMIE
import onlymash.flexbooru.data.action.ActionTag
import onlymash.flexbooru.data.api.BooruApis
import onlymash.flexbooru.data.database.MuzeiManager
import onlymash.flexbooru.data.database.dao.BooruDao
import onlymash.flexbooru.data.database.dao.HistoryDao
import onlymash.flexbooru.data.database.dao.PostDao
import onlymash.flexbooru.data.model.common.Booru
import onlymash.flexbooru.data.model.common.Muzei
import onlymash.flexbooru.data.repository.suggestion.SuggestionRepositoryImpl
import onlymash.flexbooru.ui.activity.MainActivity
import onlymash.flexbooru.ui.activity.SearchActivity
import onlymash.flexbooru.ui.viewmodel.*
import onlymash.flexbooru.widget.searchbar.SearchBar
import onlymash.flexbooru.widget.searchbar.SearchBarMover
import org.kodein.di.erased.instance

abstract class SearchBarFragment : BaseFragment(), SearchBar.Helper,
    SearchBar.StateListener, SearchBarMover.Helper, ActionMode.Callback,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val sp by instance<SharedPreferences>()
    val booruApis by instance<BooruApis>()
    private val booruDao by instance<BooruDao>()
    private var actionTag: ActionTag? = null

    private lateinit var booruViewModel: BooruViewModel

    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var suggestionViewModel: SuggestionViewModel

    private lateinit var searchBar: SearchBar
    private lateinit var searchBarMover: SearchBarMover
    private lateinit var leftDrawable: DrawerArrowDrawable
    internal lateinit var mainList: RecyclerView
    internal lateinit var searchLayout: CoordinatorLayout
    internal lateinit var swipeRefresh: SwipeRefreshLayout
    internal lateinit var progressBar: ProgressBar
    internal lateinit var progressBarHorizontal: ProgressBar
    private lateinit var container: CoordinatorLayout
    private lateinit var fabToListTop: FloatingActionButton
    private var systemUiBottomSize = 0
    private var systemUiTopSize = 0
    private val historyDao by instance<HistoryDao>()
    private val postDao by instance<PostDao>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        booruViewModel = getBooruViewModel(booruDao)
        historyViewModel = getHistoryViewModel(historyDao, postDao)
        suggestionViewModel = getSuggestionViewModel(SuggestionRepositoryImpl(booruApis))
        return inflater.inflate(R.layout.fragment_searchbar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.searchbar_fragment_container)
        mainList = view.findViewById(R.id.list)
        searchBar = view.findViewById(R.id.search_bar)
        searchLayout = view.findViewById(R.id.search_layout)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        progressBar = view.findViewById(R.id.progress_bar)
        progressBarHorizontal = view.findViewById(R.id.progress_bar_horizontal)
        fabToListTop = view.findViewById(R.id.action_to_top)
        container.setOnApplyWindowInsetsListener { _, insets ->
            systemUiTopSize = insets.systemWindowInsetTop
            systemUiBottomSize = insets.systemWindowInsetBottom
            (searchBar.layoutParams as CoordinatorLayout.LayoutParams).topMargin =
                resources.getDimensionPixelSize(R.dimen.search_bar_vertical_margin) + systemUiTopSize
            (fabToListTop.layoutParams as CoordinatorLayout.LayoutParams).updateMargins(
                bottom = systemUiBottomSize + resources.getDimensionPixelSize(R.dimen.margin_normal)
            )
            setupMainListPadding()
            setupSwipeRefreshOffset()
            insets
        }
        setupFabToListTop()
        setupSwipeRefreshColor()
        initSearchBar()
        booruViewModel.booru.observe(viewLifecycleOwner, Observer {
            actionTag = if (it == null) {
                null
            } else {
                ActionTag(
                    booru = it,
                    limit = 6,
                    order = "count"
                )
            }
            onBooruLoaded(it)
        })
        booruViewModel.loadBooru(activatedBooruUid)
        suggestionViewModel.suggestions.observe(viewLifecycleOwner, Observer {
            searchBar.updateSuggestions(it)
        })
        sp.registerOnSharedPreferenceChangeListener(this)
    }

    private fun setupFabToListTop() {
        if (activity is SearchActivity) {
            fabToListTop.isVisible = true
            (fabToListTop.layoutParams as CoordinatorLayout.LayoutParams).behavior =
                HideBottomViewOnScrollBehavior<FloatingActionButton>()
            fabToListTop.setOnClickListener {
                toListTop()
            }
        }
    }

    private fun setupSwipeRefreshColor() {
        swipeRefresh.setColorSchemeResources(
            R.color.blue,
            R.color.purple,
            R.color.green,
            R.color.orange,
            R.color.red
        )
    }

    private fun setupSwipeRefreshOffset() {
        val start = resources.getDimensionPixelSize(R.dimen.swipe_refresh_layout_offset_start) + systemUiTopSize
        val end = resources.getDimensionPixelSize(R.dimen.swipe_refresh_layout_offset_end) + systemUiTopSize
        swipeRefresh.setProgressViewOffset(false, start, end)
    }

    private fun initSearchBar() {
        leftDrawable = DrawerArrowDrawable(context)
        searchBar.setLeftDrawable(leftDrawable)
        searchBar.setHelper(this)
        searchBar.setStateListener(this)
        searchBar.setEditTextHint(getSearchBarHint())
        searchBarMover = SearchBarMover(this, searchBar, mainList)
        searchBar.setEditTextSelectionModeCallback(this)
    }

    private fun setupMainListPadding() {
        val paddingTop = systemUiTopSize + resources.getDimensionPixelSize(R.dimen.header_item_height)
        if (activity is MainActivity) {
            val paddingBottom = systemUiBottomSize + resources.getDimensionPixelSize(R.dimen.nav_bar_height)
            searchLayout.updatePadding(top = paddingTop, bottom = paddingBottom)
            val paddingBottomAuto = if (autoHideBottomBar) systemUiBottomSize else paddingBottom
            mainList.updatePadding(top = paddingTop, bottom = paddingBottomAuto)
            progressBarHorizontal.updatePadding(bottom = paddingBottomAuto)
        } else {
            searchLayout.updatePadding(top = paddingTop, bottom = systemUiBottomSize)
            mainList.updatePadding(top = paddingTop, bottom = systemUiBottomSize)
            progressBarHorizontal.updatePadding(bottom = systemUiBottomSize)
        }
    }

    fun setLeftDrawableProgress(@FloatRange(from = 0.0, to = 1.0) progress: Float) {
        leftDrawable.progress = progress
    }

    abstract fun getSearchBarHint(): CharSequence

    fun setSearchBarMenu(menuResId: Int) {
        activity?.menuInflater?.let {
            searchBar.setMenu(menuResId, it)
        }
    }

    fun setSearchBarTitle(title: CharSequence) {
        searchBar.setTitle(title)
    }

    fun setSearchBarText(text: CharSequence) {
        searchBar.setEditText(text)
    }

    fun getSearchBarLeftButton(): ImageButton {
        return searchBar.getLeftButton()
    }

    fun getEditQuery(): String = searchBar.getQueryText()

    val currentState: Int
        get() =  searchBar.currentState

    private fun forceShowNavBar() {
        val activity = activity
        if (activity is MainActivity) {
            activity.forceShowNavBar()
        }
    }

    private fun forceShowSearchBar() {
        searchBarMover.showSearchBar()
    }

    fun toExpandState() {
        forceShowNavBar()
        searchBar.toExpandState()
    }

    fun toNormalState() {
        searchBar.toNormalState()
        forceShowSearchBar()
        forceShowNavBar()
    }

    fun clearSearchBarText() {
        searchBar.clearText()
    }

    private fun toggleArrowLeftDrawable() {
        toggleArrow(leftDrawable)
    }

    private fun toggleArrow(drawerArrow: DrawerArrowDrawable) {
        if (drawerArrow.progress == 0f) {
            ValueAnimator.ofFloat(0f, 1f)
        } else {
            ValueAnimator.ofFloat(1f, 0f)
        }.apply {
            addUpdateListener { animation ->
                drawerArrow.progress = animation.animatedValue as Float
            }
            interpolator = DecelerateInterpolator()
            duration = 300
            start()
        }
    }

    abstract fun onBooruLoaded(booru: Booru?)


    override fun onApplySearch(query: String) {

    }

    override fun onClickTitle() {
        historyViewModel.loadHistory(activatedBooruUid).observe(this, Observer {
            val suggestions = arrayListOf<String>()
            it.forEach { history ->
                suggestions.add(history.query)
            }
            searchBar.updateSuggestions(suggestions)
        })
    }

    override fun onEditTextBackPressed() {

    }

    override fun onFetchSuggestion(query: String) {
        val action = actionTag ?: return
        if (action.booru.type == BOORU_TYPE_SHIMMIE) return
        if (searchBar.currentState == SearchBar.STATE_SEARCH) {
            action.query = when (action.booru.type) {
                BOORU_TYPE_MOE,
                BOORU_TYPE_DAN,
                BOORU_TYPE_DAN1 -> "$query*"
                else -> query
            }
            suggestionViewModel.fetchSuggestions(action)
        }
    }

    override fun onLeftButtonClick() {
        val activity = activity
        if (activity is MainActivity) {
            activity.openDrawer()
        } else {
            activity?.finish()
        }
    }

    override fun onMenuItemClick(menuItem: MenuItem) {

    }

    override fun onStateChange(newState: Int, oldState: Int, animation: Boolean) {
        if (activity is MainActivity) {
            toggleArrowLeftDrawable()
        } else {
            fabToListTop.isVisible = newState != SearchBar.STATE_EXPAND
        }
    }

    override val validRecyclerView: RecyclerView
        get() = mainList

    override val isForceShowSearchBar: Boolean
        get() = (searchBar.currentState == SearchBar.STATE_SEARCH) ||
                (searchBar.currentState == SearchBar.STATE_EXPAND)

    override fun isValidView(recyclerView: RecyclerView): Boolean =
        searchBar.currentState == SearchBar.STATE_NORMAL &&
                recyclerView == mainList

    open fun onBackPressed(): Boolean = true

    fun toListTop() {
        val itemCount = mainList.adapter?.itemCount
        if (itemCount != null && itemCount > 0) {
            mainList.scrollToPosition(0)
        }
        toNormalState()
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.add(
            MUZEI_MENU_GROUP_ID,
            MUZEI_MENU_ITEM_ID,
            MUZEI_MENU_ORDER,
            R.string.action_add_to_muzei)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        if (item?.itemId == MUZEI_MENU_ITEM_ID) {
            val query = searchBar.getSelectedText()
            val muzei = Muzei(
                booruUid = activatedBooruUid,
                query = query
            )
            MuzeiManager.createMuzei(muzei)
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {

    }

    override fun onDestroy() {
        super.onDestroy()
        sp.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == AUTO_HIDE_BOTTOM_BAR_KEY) {
            setupMainListPadding()
        }
    }

    companion object {
        private const val MUZEI_MENU_GROUP_ID = 101
        private const val MUZEI_MENU_ITEM_ID = 102
        private const val MUZEI_MENU_ORDER = 0
    }
}