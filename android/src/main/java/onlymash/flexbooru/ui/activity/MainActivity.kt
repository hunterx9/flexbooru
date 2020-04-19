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

package onlymash.flexbooru.ui.activity

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.annotation.NavigationRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.mikepenz.materialdrawer.util.addItemAtPosition
import com.mikepenz.materialdrawer.util.addItems
import com.mikepenz.materialdrawer.util.getDrawerItem
import com.mikepenz.materialdrawer.util.removeItems
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import onlymash.flexbooru.R
import onlymash.flexbooru.common.Settings.AUTO_HIDE_BOTTOM_BAR_KEY
import onlymash.flexbooru.common.Settings.BOORU_UID_ACTIVATED_KEY
import onlymash.flexbooru.common.Settings.NIGHT_THEME_KEY
import onlymash.flexbooru.common.Settings.ORDER_SUCCESS_KEY
import onlymash.flexbooru.common.Settings.activatedBooruUid
import onlymash.flexbooru.common.Settings.autoHideBottomBar
import onlymash.flexbooru.common.Settings.isOrderSuccess
import onlymash.flexbooru.common.Values.BOORU_TYPE_DAN
import onlymash.flexbooru.common.Values.BOORU_TYPE_DAN1
import onlymash.flexbooru.common.Values.BOORU_TYPE_GEL
import onlymash.flexbooru.common.Values.BOORU_TYPE_MOE
import onlymash.flexbooru.common.Values.BOORU_TYPE_SANKAKU
import onlymash.flexbooru.common.Values.BOORU_TYPE_SHIMMIE
import onlymash.flexbooru.common.Values.BOORU_TYPE_UNKNOWN
import onlymash.flexbooru.data.database.dao.BooruDao
import onlymash.flexbooru.data.model.common.Booru
import onlymash.flexbooru.extension.setup
import onlymash.flexbooru.extension.setupInsets
import onlymash.flexbooru.ui.fragment.SearchBarFragment
import onlymash.flexbooru.ui.helper.isNightEnable
import onlymash.flexbooru.ui.viewmodel.BooruViewModel
import onlymash.flexbooru.ui.viewmodel.getBooruViewModel
import org.kodein.di.erased.instance

class MainActivity : PathActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val BOORUS_LIMIT = 3
        private const val HEADER_ITEM_ID_BOORU_MANAGE = -100L
        private const val DRAWER_ITEM_ID_ACCOUNT = 1L
        private const val DRAWER_ITEM_ID_COMMENTS = 2L
        private const val DRAWER_ITEM_ID_HISTORY = 3L
        private const val DRAWER_ITEM_ID_TAG_BLACKLIST = 4L
        private const val DRAWER_ITEM_ID_MUZEI = 5L
        private const val DRAWER_ITEM_ID_SAUCE_NAO = 6L
        private const val DRAWER_ITEM_ID_WHAT_ANIME = 7L
        private const val DRAWER_ITEM_ID_SETTINGS = 8L
        private const val DRAWER_ITEM_ID_ABOUT = 9L
        private const val DRAWER_ITEM_ID_PURCHASE = 10L
        private const val DRAWER_ITEM_ID_PURCHASE_POSITION = 8
    }

    private var currentBooru: Booru? = null
    private var boorus: MutableList<Booru> = mutableListOf()

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var drawerSliderView: MaterialDrawerSliderView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var headerView: AccountHeaderView
    private lateinit var profileSettingDrawerItem: ProfileSettingDrawerItem

    private val sp by instance<SharedPreferences>()
    private val booruDao by instance<BooruDao>()

    private lateinit var booruViewModel: BooruViewModel

    private lateinit var navController: NavController

    private val drawerItemClickListener: ((v: View?, item: IDrawerItem<*>, position: Int) -> Boolean) = { _: View?, item: IDrawerItem<*>, _: Int ->
        when (item.identifier) {
            DRAWER_ITEM_ID_ACCOUNT -> {
                currentBooru?.let {
                    if (it.type != BOORU_TYPE_SHIMMIE) {
                        if (it.user == null) {
                            toActivity(AccountConfigActivity::class.java)
                        } else {
                            toActivity(AccountActivity::class.java)
                        }
                    } else {
                        notSupportedToast()
                    }
                }
            }
            DRAWER_ITEM_ID_COMMENTS -> {
                if (currentBooru?.type != BOORU_TYPE_SHIMMIE) {
                    toActivity(CommentActivity::class.java )
                } else {
                    notSupportedToast()
                }
            }
            DRAWER_ITEM_ID_HISTORY -> {
                if (currentBooru != null) {
                    toActivity(HistoryActivity::class.java)
                }
            }
            DRAWER_ITEM_ID_TAG_BLACKLIST -> {
                if (currentBooru?.type ?: BOORU_TYPE_UNKNOWN
                    in intArrayOf(
                        BOORU_TYPE_SANKAKU,
                        BOORU_TYPE_MOE,
                        BOORU_TYPE_DAN,
                        BOORU_TYPE_DAN1,
                        BOORU_TYPE_GEL
                    )
                ) {
                    toActivity(TagBlacklistActivity::class.java)
                } else {
                    notSupportedToast()
                }
            }
            DRAWER_ITEM_ID_MUZEI -> toActivity(MuzeiActivity::class.java)
            DRAWER_ITEM_ID_SETTINGS -> toActivity(SettingsActivity::class.java)
            DRAWER_ITEM_ID_SAUCE_NAO -> toActivity(SauceNaoActivity::class.java)
            DRAWER_ITEM_ID_WHAT_ANIME -> toActivity(WhatAnimeActivity::class.java)
            DRAWER_ITEM_ID_ABOUT -> toActivity(AboutActivity::class.java)
            DRAWER_ITEM_ID_PURCHASE -> toActivity(PurchaseActivity::class.java)
        }
        false
    }

    private fun toActivity(activityCls: Class<*>) {
        startActivity(Intent(this, activityCls))
    }

    private fun notSupportedToast() {
        Toast.makeText(this, getString(R.string.msg_not_supported), Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val windowHeight = resources.displayMetrics.heightPixels
            val gestureWidth = resources.getDimensionPixelSize(R.dimen.gesture_exclusion_width)
            val gestureHeight = resources.getDimensionPixelSize(R.dimen.gesture_exclusion_height)
            val gestureOffset = resources.getDimensionPixelSize(R.dimen.gesture_exclusion_offset)
            window.decorView.systemGestureExclusionRects = listOf(Rect(0, windowHeight - gestureHeight - gestureOffset, gestureWidth, windowHeight - gestureOffset))
        }
        setContentView(R.layout.activity_main)
        bottomNavigationView = findViewById(R.id.navigation)
        drawerLayout = findViewById(R.id.drawer_layout)
        drawerSliderView = findViewById(R.id.slider)
        navController = findNavController(R.id.nav_host_fragment)
        setupNavigationBarBehavior()
        bottomNavigationView.setup(navController) {
            toListTop()
        }
        booruViewModel = getBooruViewModel(booruDao)
        if (!booruViewModel.isNotEmpty()) {
            activatedBooruUid = createDefaultBooru()
        }
        sp.registerOnSharedPreferenceChangeListener(this)
        setupDrawer()
        booruViewModel.loadBoorus().observe(this, Observer {
            boorus.clear()
            boorus.addAll(it)
            initDrawerHeader()
        })
        booruViewModel.booru.observe(this, Observer { booru: Booru? ->
            if (booru != null) {
                currentBooru = booru
                setupNavigationMenu(booru.type)
            } else {
                navController.graph = navController.navInflater.inflate(R.navigation.main_navigation)
            }
        })
        if (savedInstanceState == null) {
            booruViewModel.loadBooru(activatedBooruUid)
        }
        if (!isOrderSuccess) {
            drawerSliderView.addItemAtPosition(
                DRAWER_ITEM_ID_PURCHASE_POSITION,
                PrimaryDrawerItem().apply {
                    name = StringHolder(R.string.purchase_title)
                    icon = createImageHolder(R.drawable.ic_payment_24dp)
                    isSelectable = false
                    isIconTinted = true
                    identifier = DRAWER_ITEM_ID_PURCHASE
                }
            )
        }
        setupInsets { insets ->
            drawerSliderView.recyclerView.updatePadding(bottom = insets.systemWindowInsetBottom)
            drawerSliderView.stickyFooterView?.updatePadding(bottom = insets.systemWindowInsetBottom)
        }
        checkUpdate()
    }

    private fun setupNavigationMenu(booruType: Int) {
        when (booruType) {
            BOORU_TYPE_SANKAKU -> setupNavigationMenu(4, R.menu.navigation_sankaku, R.navigation.main_navigation_sankaku)
            BOORU_TYPE_GEL -> setupNavigationMenu(2, R.menu.navigation_gel, R.navigation.main_navigation_gel)
            BOORU_TYPE_SHIMMIE -> setupNavigationMenu(1, R.menu.navigation_shimmie, R.navigation.main_navigation_shimmie)
            else -> setupNavigationMenu(5, R.menu.navigation, R.navigation.main_navigation)
        }
    }

    private fun setupNavigationMenu(menuSize: Int, @MenuRes menuRes: Int, @NavigationRes navRes: Int) {
        if (bottomNavigationView.menu.size() != menuSize) {
            bottomNavigationView.menu.clear()
            bottomNavigationView.inflateMenu(menuRes)
        }
        navController.graph = navController.navInflater.inflate(navRes)
        val currentId = navController.currentDestination?.id ?: R.id.nav_posts
        if (currentId != bottomNavigationView.selectedItemId) {
            bottomNavigationView.selectedItemId = currentId
        }
        forceShowNavBar()
    }

    private fun setupDrawer() {
        profileSettingDrawerItem = ProfileSettingDrawerItem().apply {
            name = StringHolder(R.string.title_manage_boorus)
            identifier = HEADER_ITEM_ID_BOORU_MANAGE
            icon = createImageHolder(R.drawable.ic_settings_outline_24dp)
            isIconTinted = true
        }
        headerView = AccountHeaderView(this).apply {
            attachToSliderView(drawerSliderView)
            addProfile(profileSettingDrawerItem, profiles?.size ?: 0)
            onAccountHeaderListener = { _: View?, profile: IProfile, _: Boolean ->
                when (val uid = profile.identifier) {
                    HEADER_ITEM_ID_BOORU_MANAGE -> startActivity(Intent(this@MainActivity, BooruActivity::class.java))
                    else -> activatedBooruUid = uid
                }
                false
            }
        }
        drawerSliderView.apply {
            addItems(
                PrimaryDrawerItem().apply {
                    name = StringHolder(R.string.title_account)
                    icon = createImageHolder(R.drawable.ic_account_circle_outline_24dp)
                    isSelectable = false
                    isIconTinted = true
                    identifier = DRAWER_ITEM_ID_ACCOUNT
                },
                PrimaryDrawerItem().apply {
                    name = StringHolder(R.string.title_comments)
                    icon = createImageHolder(R.drawable.ic_comment_outline_24dp)
                    isSelectable = false
                    isIconTinted = true
                    identifier = DRAWER_ITEM_ID_COMMENTS
                },
                PrimaryDrawerItem().apply {
                    name = StringHolder(R.string.title_history)
                    icon = createImageHolder(R.drawable.ic_history_24dp)
                    isSelectable = false
                    isIconTinted = true
                    identifier = DRAWER_ITEM_ID_HISTORY
                },
                PrimaryDrawerItem().apply {
                    name = StringHolder(R.string.title_tag_blacklist)
                    icon = createImageHolder(R.drawable.ic_visibility_off_outline_24dp)
                    isSelectable = false
                    isIconTinted = true
                    identifier = DRAWER_ITEM_ID_TAG_BLACKLIST
                },
                PrimaryDrawerItem().apply {
                    name = StringHolder(R.string.title_muzei)
                    icon = createImageHolder(R.drawable.ic_muzei_24dp)
                    isSelectable = false
                    isIconTinted = true
                    identifier = DRAWER_ITEM_ID_MUZEI
                },
                PrimaryDrawerItem().apply {
                    name = StringHolder(R.string.title_sauce_nao)
                    icon = createImageHolder(R.drawable.ic_search_24dp)
                    isSelectable = false
                    isIconTinted = true
                    identifier = DRAWER_ITEM_ID_SAUCE_NAO
                },
                PrimaryDrawerItem().apply {
                    name = StringHolder(R.string.title_what_anime)
                    icon = createImageHolder(R.drawable.ic_youtube_searched_for_24dp)
                    isSelectable = false
                    isIconTinted = true
                    identifier = DRAWER_ITEM_ID_WHAT_ANIME
                }
            )
            stickyDrawerItems = arrayListOf(
                PrimaryDrawerItem().apply {
                    name = StringHolder(R.string.title_settings)
                    icon = createImageHolder(R.drawable.ic_settings_outline_24dp)
                    isSelectable = false
                    isIconTinted = true
                    identifier = DRAWER_ITEM_ID_SETTINGS
                },
                PrimaryDrawerItem().apply {
                    name = StringHolder(R.string.title_about)
                    icon = createImageHolder(R.drawable.ic_info_outline_24dp)
                    isSelectable = false
                    isIconTinted = true
                    identifier = DRAWER_ITEM_ID_ABOUT
                }
            )
            stickyFooterShadow = false
            stickyFooterDivider = true
            setSelection(-3L)
            onDrawerItemClickListener = drawerItemClickListener
            tintNavigationBar = false
        }
    }

    private fun createImageHolder(@DrawableRes resId: Int): ImageHolder =
        ImageHolder(ResourcesCompat.getDrawable(resources, resId, theme))

    private fun createDefaultBooru(): Long {
        booruViewModel.createBooru(
            Booru(
                name = "Sankaku",
                scheme = "https",
                host = "capi-v2.sankakucomplex.com",
                hashSalt = "choujin-steiner--your-password--",
                type = BOORU_TYPE_SANKAKU
            )

        )

        booruViewModel.createBooru(
            Booru(
                name = "safebooru",
                scheme = "https",
                host = "safebooru.donmai.us",
                hashSalt = "",
                type = BOORU_TYPE_DAN
            )

        )

        booruViewModel.createBooru(
            Booru(
                name = "Danbooru",
                scheme = "https",
                host = "danbooru.donmai.us",
                hashSalt = "onlymash--your-password--",
                type = BOORU_TYPE_DAN
            )
        )

        booruViewModel.createBooru(
            Booru(
                name = "rule34.xxx",
                scheme = "https",
                host = "rule34.xxx",
                hashSalt = "",
                type = BOORU_TYPE_GEL
            )
        )
        booruViewModel.createBooru(
            Booru(
                name = "Gelbooru",
                scheme = "https",
                host = "gelbooru.com",
                hashSalt = "",
                type = BOORU_TYPE_GEL
            )
        )
        booruViewModel.createBooru(
            Booru(
                name = "e926.net",
                scheme = "https",
                host = "e926.net",
                hashSalt = "--your-password--",
                type = BOORU_TYPE_DAN
            )
        )
        booruViewModel.createBooru(
            Booru(
                name = "lolibooru",
                scheme = "http",
                host = "lolibooru.moe",
                hashSalt = "--your-password--",
                type = BOORU_TYPE_MOE
            )
        )
        booruViewModel.createBooru(
            Booru(
                name = "3dbooru",
                scheme = "http",
                host = "behoimi.org",
                hashSalt = "meganekko-heaven--your-password--",
                type = BOORU_TYPE_DAN1
            )

        )
        booruViewModel.createBooru(
            Booru(
                name = "hypnohub",
                scheme = "https",
                host = "hypnohub.net",
                hashSalt = "--your-password--",
                type = BOORU_TYPE_MOE
            )

        )
        booruViewModel.createBooru(
            Booru(
                name = "e621.net",
                scheme = "https",
                host = "e621.net",
                hashSalt = "--your-password--",
                type = BOORU_TYPE_DAN
            )

        )
        booruViewModel.createBooru(
            Booru(
                name = "yande.re",
                scheme = "https",
                host = "yande.re",
                hashSalt = "So-I-Heard-You-Like-Mupkids-?--your-password--",
                type = BOORU_TYPE_MOE
            )

        )
        booruViewModel.createBooru(
            Booru(
                name = "Konachan",
                scheme = "https",
                host = "konachan.com",
                hashSalt = "So-I-Heard-You-Like-Mupkids-?--your-password--",
                type = BOORU_TYPE_MOE
            )

        )

        booruViewModel.createBooru(
            Booru(
                name = "Sakugabooru",
                scheme = "https",
                host = "www.sakugabooru.com",
                hashSalt = "So-I-Heard-You-Like-Mupkids-?--your-password--",
                type = BOORU_TYPE_MOE
            )

        )
        booruViewModel.createBooru(
            Booru(
                name = "TBIB",
                scheme = "https",
                host = "tbib.org",
                hashSalt = "",
                type = BOORU_TYPE_GEL
            )
        )

        booruViewModel.createBooru(
            Booru(
                name = "Dante's Archive",
                scheme = "https",
                host = "cow.zone",
                hashSalt = "er@!\$rjiajd0\$!dkaopc350!Y%)--your-password--",
                type = BOORU_TYPE_SHIMMIE
            )

        )

        booruViewModel.createBooru(
            Booru(
                name = "EvBooru",
                scheme = "https",
                host = "evbooru.com",
                hashSalt = "choujin-steiner--your-password--",
                type = BOORU_TYPE_MOE
            )

        )

        booruViewModel.createBooru(
            Booru(
                name = "AllTheFallen",
                scheme = "https",
                host = "booru.allthefallen.moe",
                hashSalt = "choujin-steiner--your-password--",
                type = BOORU_TYPE_DAN
            )

        )
        return booruViewModel.createBooru(
            Booru(
                name = "Sample",
                scheme = "https",
                host = "moe.fiepi.com",
                hashSalt = "onlymash--your-password--",
                type = BOORU_TYPE_MOE
            )

        )
    }

    private fun checkUpdate() {
//        GlobalScope.launch {
//            AppUpdaterApi.checkUpdate()
//        }
//        if (BuildConfig.VERSION_CODE < latestVersionCode) {
//            AlertDialog.Builder(this)
//                .setTitle(R.string.update_found_update)
//                .setMessage(getString(R.string.update_version, latestVersionName))
//                .setPositiveButton(R.string.dialog_update) { _, _ ->
//                    if (isGoogleSign && isAvailableOnStore) {
//                        openAppInMarket(applicationContext.packageName)
//                    } else {
//                        launchUrl(latestVersionUrl)
//                    }
//                    finish()
//                }
//                .setNegativeButton(R.string.dialog_exit) { _, _ ->
//                    finish()
//                }
//                .create().apply {
//                    setCancelable(false)
//                    setCanceledOnTouchOutside(false)
//                    show()
//                }
//        }
    }

    private fun initDrawerHeader() {
        val success = isOrderSuccess
        val size = boorus.size
        var uid = activatedBooruUid
        var i = -1
        headerView.clear()
        boorus.forEachIndexed { index, booru ->
            if (!success && index >= BOORUS_LIMIT) {
                return@forEachIndexed
            }
            if (i == -1 && booru.uid == uid) {
                i = index
            }
            var host = booru.host
            if (booru.type == BOORU_TYPE_SANKAKU && host.startsWith("capi-v2.")) {
                host = host.replaceFirst("capi-v2.", "beta.")
            }
            headerView.addProfile(
                ProfileDrawerItem().apply {
                    icon = ImageHolder(
                        Uri.parse(
                            String.format(
                                "%s://%s/favicon.ico",
                                booru.scheme,
                                host
                            )
                        )
                    )
                    name = StringHolder(booru.name)
//                    description = StringHolder(String.format("%s://%s", booru.scheme, booru.host))
                    description = StringHolder(booru.name)

                    identifier = booru.uid
                },
                index
            )
        }
        headerView.addProfile(
            profileSettingDrawerItem,
            if (success || size < BOORUS_LIMIT) size else BOORUS_LIMIT
        )
        if (size == 0) {
            activatedBooruUid = -1
            return
        }
        if (i == -1) {
            uid = boorus[0].uid
            activatedBooruUid = uid
        }
        headerView.setActiveProfile(uid)
    }

    override fun onDestroy() {
        sp.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            NIGHT_THEME_KEY -> {
                if (resources.configuration.isNightEnable()) {
                    recreate()
                }
            }
            BOORU_UID_ACTIVATED_KEY -> {
                val identifier = headerView.activeProfile?.identifier ?: -1
                val uid = activatedBooruUid
                if (uid >= 0 && uid != identifier) {
                    headerView.setActiveProfile(identifier = uid, fireOnProfileChanged = false)
                }
                booruViewModel.loadBooru(uid)
            }
            ORDER_SUCCESS_KEY -> {
                if (isOrderSuccess) {
                    drawerSliderView.removeItems(DRAWER_ITEM_ID_PURCHASE)
                } else {
                    if (drawerSliderView.getDrawerItem(DRAWER_ITEM_ID_PURCHASE) == null) {
                        drawerSliderView.addItemAtPosition(
                            DRAWER_ITEM_ID_PURCHASE_POSITION,
                            PrimaryDrawerItem().apply {
                                name = StringHolder(R.string.purchase_title)
                                icon = createImageHolder(R.drawable.ic_payment_24dp)
                                isSelectable = false
                                isIconTinted = true
                                identifier = DRAWER_ITEM_ID_PURCHASE
                            }
                        )
                    }
                }
                if (boorus.size > BOORUS_LIMIT) {
                    initDrawerHeader()
                }
            }
            AUTO_HIDE_BOTTOM_BAR_KEY -> setupNavigationBarBehavior()
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else if (isFragmentCanBack()) {
            val startId = navController.graph.startDestination
            if (navController.currentDestination?.id == startId) {
                super.onBackPressed()
            } else {
                bottomNavigationView.selectedItemId = startId
            }
        }
    }

    private fun isFragmentCanBack(): Boolean {
        val currentFragment = getCurrentFragment()
        if (currentFragment is SearchBarFragment) {
            return currentFragment.onBackPressed()
        }
        return true
    }

    private fun toListTop() {
        val currentFragment = getCurrentFragment()
        if (currentFragment is SearchBarFragment) {
            currentFragment.toListTop()
        }
    }

    private fun getCurrentFragment(): Fragment? {
        val fragments = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                as? NavHostFragment)?.childFragmentManager?.fragments
        if (fragments.isNullOrEmpty()) {
            return null
        }
        return fragments.last()
    }

    fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun setupNavigationBarBehavior() {
        val layoutParams = bottomNavigationView.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = layoutParams.behavior
        layoutParams.behavior =
            if (autoHideBottomBar) {
                HideBottomViewOnScrollBehavior<BottomNavigationView>()
            } else {
                if (behavior is HideBottomViewOnScrollBehavior) {
                    behavior.slideUp(bottomNavigationView)
                }
                null
            }
    }

    fun forceShowNavBar() {
        val behavior = (bottomNavigationView.layoutParams as CoordinatorLayout.LayoutParams).behavior
        if (behavior is HideBottomViewOnScrollBehavior) {
            behavior.slideUp(bottomNavigationView)
        }
    }
}
