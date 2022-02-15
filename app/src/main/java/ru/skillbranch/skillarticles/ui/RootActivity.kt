package ru.skillbranch.skillarticles.ui

import android.app.SearchManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.ui.custom.ArticleSubmenu
import ru.skillbranch.skillarticles.ui.custom.Bottombar
import ru.skillbranch.skillarticles.ui.custom.CheckableImageView
import ru.skillbranch.skillarticles.viewmodels.ArticleState
import ru.skillbranch.skillarticles.viewmodels.ArticleViewModel
import ru.skillbranch.skillarticles.viewmodels.Notify
import ru.skillbranch.skillarticles.viewmodels.ViewModelFactory

class RootActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private lateinit var coordinatorContainer: CoordinatorLayout

    private lateinit var toolbar: MaterialToolbar
    private var searchView: SearchView? = null

    private lateinit var textContent: TextView

    private lateinit var submenu: ArticleSubmenu
    private lateinit var btnTextUp: CheckableImageView
    private lateinit var btnTextDown: CheckableImageView
    private lateinit var switchMode: SwitchMaterial

    private lateinit var bottombar: Bottombar
    private lateinit var btnLike: CheckableImageView
    private lateinit var btnBookmark: CheckableImageView
    private lateinit var btnShare: ImageView
    private lateinit var btnSettings: CheckableImageView

    private lateinit var viewModel: ArticleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)
        setupViews()
        setupToolbar()
        setupBottombar()
        setupSubmenu()

        val vmFactory = ViewModelFactory("0")
        viewModel = ViewModelProvider(this, vmFactory)[ArticleViewModel::class.java]
        viewModel.observeState(this) {
            renderUi(it)
        }
        viewModel.observeNotifications(this) {
            renderNotification(it)
        }
    }

    private fun setupViews() {
        coordinatorContainer = findViewById(R.id.coordinator_container)
        toolbar = findViewById(R.id.toolbar)
        textContent = findViewById(R.id.tvTextContent)
        submenu = findViewById(R.id.submenu)
        btnTextUp = findViewById(R.id.btn_text_up)
        btnTextDown = findViewById(R.id.btn_text_down)
        switchMode = findViewById(R.id.switch_mode)
        bottombar = findViewById(R.id.bottombar)
        btnLike = findViewById(R.id.btn_like)
        btnBookmark = findViewById(R.id.btn_bookmark)
        btnShare = findViewById(R.id.btn_share)
        btnSettings = findViewById(R.id.btn_settings)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val logo = if (toolbar.childCount > 2) toolbar.getChildAt(2) as ImageView else null
        logo?.scaleType = ImageView.ScaleType.CENTER_CROP
        val lp = logo?.layoutParams as? Toolbar.LayoutParams
        lp?.let {
            it.width = this.dpToIntPx(40)
            lp.height = this.dpToIntPx(40)
            it.marginEnd = this.dpToIntPx(16)
            logo.layoutParams = it
        }
    }

    private fun setupSubmenu() {
        btnTextUp.setOnClickListener {
            viewModel.handleUpText()
        }
        btnTextDown.setOnClickListener {
            viewModel.handleDownText()
        }
        switchMode.setOnClickListener {
            viewModel.handleNightMode()
        }
    }

    private fun setupBottombar() {
        btnLike.setOnClickListener {
            viewModel.handleLike()
        }
        btnBookmark.setOnClickListener {
            viewModel.handleBookmark()
        }
        btnShare.setOnClickListener {
            viewModel.handleShare()
        }
        btnSettings.setOnClickListener {
            viewModel.handleToggleMenu()
        }
    }

    private fun renderUi(state: ArticleState) {
        btnSettings.isChecked = state.isShowMenu
        if (state.isShowMenu) {
            submenu.open()
        } else {
            submenu.close()
        }

        btnLike.isChecked = state.isLike
        btnBookmark.isChecked = state.isBookmark

        switchMode.isChecked = state.isDarkMode
        delegate.localNightMode = if (state.isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        if (state.isBigText) {
            textContent.textSize = 18f
            btnTextUp.isChecked = true
            btnTextDown.isChecked = false
        } else {
            textContent.textSize = 14f
            btnTextUp.isChecked = false
            btnTextDown.isChecked = true
        }

        textContent.text = if (state.isLoadingContent) "loading" else state.content.first() as String

        toolbar.title = state.title ?: "loading"
        toolbar.subtitle = state.category ?: "loading"
        if (state.categoryIcon != null) {
            toolbar.logo = getDrawable(state.categoryIcon as Int)
        }
    }

    private fun renderNotification(notify: Notify) {
        val snackbar = Snackbar.make(coordinatorContainer, notify.message, Snackbar.LENGTH_LONG)
            .setAnchorView(bottombar)


        when (notify) {
            is Notify.TextMessage -> {
                // nothing
            }
            is Notify.ActionMessage -> {
                snackbar.setActionTextColor(getColor(R.color.color_accent_dark))
                snackbar.setAction(notify.actionLabel) {
                    notify.actionHandler()
                }
            }
            is Notify.ErrorMessage -> {
                with(snackbar) {
                    setBackgroundTint(getColor(R.color.design_default_color_error))
                    setTextColor(getColor(android.R.color.white))
                    setActionTextColor(getColor(android.R.color.white))
                    setAction(notify.errLabel) {
                        notify.errHandler?.invoke()
                    }
                }
            }
        }

        snackbar.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_root, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        searchView = searchItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)

        searchView?.isIconified = !viewModel.currentState.isSearch
        searchView?.setQuery(viewModel.currentState.searchQuery, false)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        viewModel.handleSearch(query)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        viewModel.handleSearch(newText)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(QUERY_TEXT_KEY, viewModel.currentState.searchQuery)
        outState.putBoolean(SEARCH_MODE_KEY, !(searchView?.isIconified ?: true))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewModel.handleSearch(savedInstanceState.getString(QUERY_TEXT_KEY))
        viewModel.handleSearchMode(savedInstanceState.getBoolean(SEARCH_MODE_KEY, false))
    }

    companion object {
        const val SEARCH_MODE_KEY = "search_mode"
        const val QUERY_TEXT_KEY = "query_text"
    }
}