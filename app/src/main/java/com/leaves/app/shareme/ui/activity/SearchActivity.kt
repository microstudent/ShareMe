package com.leaves.app.shareme.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import com.leaves.app.shareme.R

class SearchActivity : BaseActivity() {

    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        ButterKnife.bind(this)
        setSupportActionBar(toolbar)

        title = ""
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        var searchItem = menu.findItem(R.id.action_search)
        var searchView = searchItem.actionView as SearchView

        searchView.setOnSearchClickListener {
            //            doSearch("keyword")
        }
        searchView.setIconifiedByDefault(false)
        searchView.isIconified = false
        searchView.onActionViewExpanded()

        searchView.isSubmitButtonEnabled = true
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Toast.makeText(this@SearchActivity, query, Toast.LENGTH_SHORT)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        searchView.queryHint = getString(R.string.search_hint)


        return true
    }
}
